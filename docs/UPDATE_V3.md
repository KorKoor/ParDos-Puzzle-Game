# ParDos — Actualización v3

Guía técnica de lo que cambió en la rama `update-v3` y cómo trabajar con ella.

## Cómo compilar

- **JDK 21** (Android Studio trae JDK 25, que este Gradle/Kotlin no soporta). Apunta `JAVA_HOME` a un JDK 21 o configúralo en
  *Settings → Build → Gradle → Gradle JDK*.
- `google-services.json` va en `app/` (está en `.gitignore`). La variante debug usa `app/src/debug/google-services.json`
  (con el paquete `com.korkoor.pardos.debug`, también ignorado) para poder instalarse junto a la app de Play Store.
- Pruebas de la lógica compartida: `./gradlew :shared:testDebugUnitTest`

## Arquitectura

```
shared/   (Kotlin Multiplatform: Android + iOS)   <- lógica pura, sin Android
  domain/logic      GameEngine (con semilla), ProgressionEngine, DailyChallenge, RaceRules, DuelRules
  domain/events     EventCalendar (eventos programados sin servidor)
  domain/model      BoardState, TileModel, GameMode, LevelInfo, LevelRepository, DailyMission
  domain/rewards    DailyRewards, StreakCalculator, CoinRewards, ChapterRewards
  domain/shop       ShopCatalog, CoinShop, TileSkins (catálogo + inventario)
  domain/social     Leaderboard, WeekCalendar, FriendCode
  domain/collection Collection (48 piezas, cofres con garantía, crear con esencia)
  domain/economy    Economy (TODOS los precios y premios en un solo archivo)
  domain/retention  FreeChest, DailyWheel, SeasonPass, WeeklyMissions, PiggyBank, Leagues, ReminderPlanner
  domain/social     (+ ShareText: texto para compartir resultados e invitaciones)
app/      (Android)
  data/local        EconomyManager, CollectionManager, RewardsManager, DailyRewardManager, LevelProgressStore, ProfileManager, MissionManager, RetentionManager
  data/auth         AuthManager (Google + Firebase Auth)
  data/billing      BillingManager (Google Play Billing 8)
  ui/design         PardosDesign.kt  <- colores, formas y componentes base (ÚNICA fuente de verdad)
  ui/menu           MenuScreen/MenuHome, LevelSelectorScreen (mapa), ModelSelectionScreen, DailyRewardDialog
  ui/game           GameScreen, GameHud, GameOverlays, AchievementScreen, components/ (tablero, fichas, poderes)
  ui/shop           ShopScreen
  ui/profile        ProfileScreen, FriendsScreen
  ui/rewards        TodayStrip (cofre/ruleta/pase/hucha), WheelScreen, LeagueCard, RetentionSection (diálogos de bienvenida)
  ui/season         SeasonScreen (pase de temporada)
  ui/studio         StudioScreen (skin de pago editable)
```

Regla: **toda regla de juego nueva (puntos, premios, progresión) va en `shared/` con su prueba**, y la UI solo la muestra.
Eso es lo que permitirá reutilizarla en iOS.

## Modos y sistemas de juego

- **Motor determinista:** `GameEngine(boardSize, random)`. Con una semilla, los mismos movimientos dan el mismo tablero.
  Lo usan el reto diario (igual para todos los del mismo día local) y el duelo.
- **Eventos programados:** `EventCalendar.activeOn(díaLocal)`. Fin de semana dorado (monedas x2), Miércoles de
  experiencia (XP x2) y Semana festival (monedas x1.5 y estrellas dobles en el ranking). No se acumulan: aplica el mejor.
- **Skins de fichas:** `TileSkin` (Gelatina, Mate, Madera, Cristal, Neón). El dibujo está en
  `ui/game/components/TileSkinStyle.kt`; se compran y equipan en la tienda.
- **Modo Carrera:** `RaceRules`. Etapas encadenadas (3x3 → 6x6), el reloj sube con cada etapa superada (máx. 180 s).
- **Duelo local:** `DuelRules`. Misma semilla para los dos jugadores, 60 s cada uno, se pasan el teléfono.
- **Cofres de capítulo:** `ChapterRewards` (cada 20 niveles del mapa).
- **Skins con temática (12):** cada `TileSkin` trae un `SkinStyle` con paleta de fichas, acabado, fondo, colores de texto
  y partículas. Añadir una skin = añadir una entrada en `TileSkins.kt` (no hay que tocar la UI).
- **Colección:** 48 piezas en 6 series (4 comunes, 2 raras, 1 épica y 1 legendaria por serie). Cofres común/raro/épico con
  garantía por cofre y por acumulación (`ChestRules.PITY_*`). Las repetidas dan esencia; la esencia crea piezas.
- **Economía:** todo en `domain/economy/Economy.kt` y `domain/shop/ShopOffers.kt`, con pruebas de balance. Fuentes:
  niveles, misiones, regalo diario, logros (pagan según rareza), hitos de racha, cofres de capítulo, x2 con anuncio,
  eventos. Sumideros: skins, cofres, consumibles (Deshacer, escudo de racha), oferta diaria con descuento.
- **Multijugador** vive en `ui/social/MultiplayerHub` (duelo local, amigos, reto diario), fuera de "Elige tu ritmo".

## Retención (RetentionManager + domain/retention)

Todo vive en `RetentionManager` (SharedPreferences `pardos_retention`, StateFlow compartidos) y las reglas puras en `shared`.

- **Cofre gratis** cada 4 h (cada 5.º es raro); se salta con anuncio (3/día) o gemas (`FreeChest`).
- **Ruleta diaria**: 1 giro gratis + 2 con anuncio; la casilla premiada se decide en `DailyWheel.pick`.
- **Pase de temporada** mensual: 30 niveles, vía gratis + premium (producto `season_pass`), skin exclusiva en el nivel 30.
  Saltar de nivel cuesta gemas **proporcionales a lo que falta** (10–40, `SeasonPass.skipCostGems`).
- **Misiones semanales** (4 por semana, iguales para todos) + cofre raro y gemas por completarlas.
- **Hucha** de gemas: se llena al ganar (tope 300); romperla es el producto `piggy_break`.
- **Ligas semanales** (`Leagues`): Bronce → Plata → Oro → Zafiro → Rubí → Diamante. Cuentan las estrellas ganadas en la
  semana (las mismas de la misión semanal). Al cerrar la semana (lunes) se sube / mantiene / baja y se deja un resultado
  pendiente (`pardos_retention.league_pending`) que el jugador reclama en un diálogo. No hay rivales: es una escalera
  personal, funciona offline y en iOS. Las semanas sin jugar solo pueden bajar (máx. 3 ligas).
- **Extras**: primera victoria del día, premios por subir de nivel, regalo de regreso (3+ días), bonus de misiones diarias.
- **Notificaciones** (`ReminderPlanner`, probado): poderes listos, cofre gratis, ruleta, regalo diario, racha en riesgo,
  premios del pase, pase por terminar, semanales por vencer, hucha casi llena, liga en juego (últimos 2 días), regresos.
  Nunca entre 21:30 y 9:00.

## Experiencia de usuario

- **Ajustes** (`ui/settings/SettingsScreen.kt`, engranaje en el perfil): efectos de sonido, música, vibración y recordatorios.
  Se guardan en `SettingsManager` (`pardos_settings`) y lo respetan `SoundManager`, `GameAudioManager`, la vibración
  (`rememberGameHaptics()` en `ui/design/Haptics.kt`: úsalo en vez de `LocalHapticFeedback.current`) y
  `ZenNotificationManager`. Apagar la música tiene efecto al instante.
- **Siguiente meta** (`domain/retention/NextGoal.kt`): en el resumen de victoria se muestra UNA meta cercana (cofre listo >
  liga a punto de subir > semanal casi hecha > nivel del pase cerca > reto diario sin jugar > cuenta atrás del cofre).
- **Recuperar racha** (`domain/rewards/StreakRepair.kt`): si se pierde una racha de 3+ días faltando 1–2 días, al abrir la app
  se ofrece recuperarla ese mismo día por gemas (2 por día de racha, 10–60) o con un anuncio (1 por semana). El hito de
  racha que se alcance al recuperar se paga igual. La oferta vive en `pardos_profile` (`repair_*`).

## Fiestas, skins de evento y skins secretas

- **11 fiestas fijas** (`EventCalendar.seasonal`, todas monedas x1.5, nunca se solapan): San Valentín (12–14 feb), Primavera
  (20 mar–3 abr), Verano (21 jun–5 jul), Fiestas patrias (15–16 sep), Noche de brujas (25–31 oct), Día de Muertos (1–2 nov),
  Navidad (18–26 dic) y Año nuevo (31 dic–2 ene). `EventCalendar.upcoming()` lista las que empiezan pronto.
- **Cada fiesta trae una skin exclusiva** (`EventSkins`, fuente `SkinSource.EVENT`): se gana con 3 victorias durante la fiesta o
  con 100 gemas mientras dura. Si no se consigue, vuelve el año siguiente. El progreso es por edición (`ev_<TIPO>_<díaInicio>`).
  El menú muestra el banner con "Skin X · 1/3" y al tocarlo abre `EventSkinDialog` (vista previa con fondo y partículas reales).
- **8 skins secretas** (`HiddenSkins`, fuente `SkinSource.HIDDEN`): en la tienda aparecen como "SECRETA" con una pista poética que
  no revela el número. Se entregan solas al cumplir la condición y salen en el resumen de victoria y en un diálogo al volver
  al menú (`RetentionManager.checkHiddenSkins`, `takePendingReveals`):
  Búho Nocturno (3 victorias de 0:00 a 4:59), Amanecer (3 victorias de 5:00 a 7:59), Fénix (racha de 30), Corona (ficha 2048),
  Prisma (álbum completo), Jade Imperial (30 días distintos), Estrella Fugaz (300 estrellas), Obsidiana (200 victorias).
  Los jugadores antiguos cuentan con su progreso previo (estrellas y niveles de campaña ya ganados).
- **10 efectos de partículas nuevos** en `AmbientParticles.kt` (corazones, murciélagos, cempasúchil, flores, confeti, confeti
  tricolor, fuegos artificiales, copos de nieve, destellos, meteoros). Añadir uno = un valor en `ParticleKind` (siempre al final)
  y su dibujo. Studio también los ofrece.
- Avisos: "¡Llega <fiesta>!" (10:00 del día de inicio, hasta 3) y "Última oportunidad" (último y penúltimo día si falta la skin).
- **Probar fiestas sin esperar** (solo builds debug): `adb shell am start -n com.korkoor.pardos.debug/com.korkoor.pardos.MainActivity --ei debug_day_offset 19`
  adelanta el calendario 19 días (`LocalDay.debugOffsetDays`, solo en memoria).

## Multijugador: duelo a distancia por código

`domain/logic/RemoteDuel.kt` (con pruebas). Sin servidor ni cuenta: el reto es un código `PD1-<semilla>-<puntos>-<NOMBRE>-<control>`
(base 36) que viaja dentro de un mensaje normal.
- **Lanzar** (`RemoteRole.CREATOR`): juegas 60 s con una semilla nueva → sale tu código y un botón de compartir.
- **Aceptar** (`RemoteRole.CHALLENGED`): pegas el mensaje en "Tengo un código" (o el Hub lo detecta solo en el portapapeles) y
  juegas el MISMO tablero; se compara el puntaje (ganar / empatar / perder), con premios en `Economy.REMOTE_DUEL_*`.
- Cada código paga y cuenta en las estadísticas solo la primera vez (`RemoteDuelManager`); lanzar retos paga hasta 3 al día.
- Hub (`ui/social/MultiplayerHub.kt`): reto detectado, campo para pegar, estadísticas (ganados/perdidos/racha) e historial.
- El control de 2 caracteres detecta errores de tecleo y ediciones torpes del puntaje; NO es seguridad real (no hay servidor).
  Si algún día hay torneos con premios gordos, hará falta validar en servidor (Firestore + Auth).

## Iconos de poderes y mapa de campaña

- `ui/design/PowerIcons.kt`: 5 iconos dibujados en Canvas (varita, fusión, escoba, enlace, rebobinar) con degradados, brillo y
  destellos animados. `PowerUpBar.kt`: botones con respiración cuando están listos y anillo de recarga real; corona si eres VIP.
- `ui/menu/MapArt.kt`: 12 mundos (uno por capítulo, `chapterThemes`) con paisaje propio (dunas, pinos, río, colinas, nubes, lavanda,
  linternas, montañas, cerezos, luna, bambú), nodos de gema 3D, retos como fichas, onda de luz y etiqueta JUGAR en el nivel actual,
  camino con borde, banners con avance, cofres dibujados. Añadir un capítulo = una fila en `chapterThemes`.
- El título "PARDOS" del menú y de la pantalla de inicio NO se tocó: el logo de fichas de colores se probó y se descartó
  (el usuario prefiere el título original; ver [[feedback]] en la memoria).

## Iconos cozy y mapa vivo (v2)

- **Iconos cozy** (`ui/design/CozyIcons.kt`): 16 ilustraciones dibujadas (estrella con carita, moneda, gema, llama, copa, cofre, corazón,
  regalo, rayo, reloj, escudo, hucha, destellos, confeti, dado, corona). El componente `Icon` de `ui.design` REEMPLAZA al de Material
  en toda la app: reconoce por nombre los iconos de Material que tienen versión cozy (`Star`, `MonetizationOn`, `Diamond`, …) y
  dibuja esa; los demás pasan al `Icon` de Material. Un tinte casi transparente o blanco se dibuja como silueta plana (estados
  bloqueados, botones de color). Al crear un archivo nuevo, importa `com.korkoor.pardos.ui.design.Icon` (no el de material3).
  `CozyText` cambia ◆ ● ★ ❤ ✨ ⭐ por iconos en línea. Un solo reloj (`CozyClockProvider`, en `MainActivity`) mueve todos los destellos.
- **Mapa** (`MapWorld.kt`, `MapArt.kt`, `LevelSelectorScreen.kt`): `MapBackdrop` pinta el cielo de cada capítulo (con transición),
  sol o luna, nubes que cruzan, relieve con parallax y partículas del mundo (arena, hojas, burbujas, nieve, luciérnagas, brasas,
  pétalos, estrellas). `ChapterLandmark` dibuja un monumento por capítulo sobre su banner (jardín zen, cabaña con humo, puente,
  casa de té, faro con luz, molino, puesto de mercado, pico con bandera, torii, pirámides, velero, pagoda). El nivel actual muestra
  tu avatar con su número; las estrellas saltan al aparecer; una lucecita recorre el camino hecho; al desbloquear un nivel hay
  rebote y estallido de destellos (`pardos_map.seen_current`). La cabecera cambia a texto claro en capítulos nocturnos.

## Cofres del tesoro (dibujados)

`ui/design/TreasureChest.kt`: un solo cofre dibujado a mano (tablones, remaches, correas de metal, tapa abovedada, cerradura con gema)
que reemplaza a los iconos planos en el mapa, la tienda, la colección y los diálogos del cofre gratis. Material por rareza
(`ChestType`): madera cálida (común), azul y plata (raro), morado y oro (épico). Estados `ChestState`: `LOCKED` (gris con candado),
`READY` (se sacude y brilla) y `OPEN` (tapa que se abre con resorte, monedas y gema). Usa el reloj compartido de `CozyClockProvider`.

## Skins y Studio

26 skins con temática completa. **Studio** (`skin_studio`) es la skin de pago editable (acabado, 2 tonos, intensidad, fondo,
partículas); se puede probar gratis, equiparla exige la compra. Lógica en `domain/shop/StudioSkin.kt`.

## Compartir

`ShareText` arma el texto de victoria (reto diario con fecha, estrellas, ficha, movimientos, tiempo, racha) y la invitación
con código de amigo. El botón de compartir está en el resumen de victoria y en Amigos.

## Eventos de calendario

Además de fin de semana dorado / miércoles de XP / semana festival, hay fiestas con fecha fija (`EventCalendar.seasonal`):
Noche de brujas (25–31 oct), Día de Muertos (1–2 nov), Navidad (18–26 dic), Año nuevo (31 dic–2 ene) y San Valentín
(12–14 feb), todas con monedas x1.5. No se acumulan: aplica el mayor multiplicador. Añadir una = una fila en `seasonal`,
sus textos en `strings.xml` (es/en) y su estilo en `EventBanner` (`MenuHome.kt`).

## Consumibles

Deshacer y **Tiempo extra** (+20 s en modos con reloj; chip junto al reloj, no se permite en duelo). Se compran en la tienda
con monedas. Tiempo extra suma a `elapsedTime` y a `maxTime` a la vez para no alterar el tiempo usado.

## Convenciones importantes

- **Todos los tiempos del estado del juego están en milisegundos** (`maxTime`, `elapsedTime`, `GameMode.timeLimit`).
- Los días de racha/regalo/ranking son **días locales** (`LocalDay.today()`), no UTC.
- Colores: usar `ui/design` (`Navy`, `Sage`, `Terracotta`, `Gold`…). Para botones/selección sobre un tema usar
  `GameTheme.actionColor`; el `accentColor` del tema es un pastel solo para fondos.

## Anuncios

Se usan los **IDs de prueba de Google** hasta publicar. Antes de lanzar, restaurar en
`AndroidManifest.xml` (App ID) y `AdManager.kt` (unidad de recompensa) los IDs reales.

## Compras (Play Console)

Crear 8 *productos administrados* con estos IDs exactos (ver `ShopCatalog`):
`gems_small` (100 gemas), `gems_medium` (550), `gems_large` (1200), `vip_forever`, `starter_pack`, `skin_studio`,
`season_pass` (consumible, se vuelve a comprar cada temporada) y `piggy_break` (consumible).
Las compras solo se pueden probar con la app subida a una pista de pruebas y una cuenta de probador.

## Pendiente / decisiones abiertas

- **Seguridad de Firestore:** los perfiles usan el `ANDROID_ID` como id de documento sin Firebase Auth, por lo que las
  reglas deben ser abiertas. Recomendado: Firebase Auth + reglas `request.auth.uid == userId`. Hasta entonces no se
  añaden regalos/retos entre amigos (requieren escribir en el perfil de otro jugador).
- **iOS:** falta mover la UI a Compose Multiplatform y el almacenamiento/Firebase/Billing a implementaciones
  multiplataforma. Solo se puede compilar en un Mac con Xcode.
- Modo horizontal de varias pantallas sin verificar en dispositivo.

## Banners de perfil, más avatares, pase rediseñado y mapa (última tanda)

- **Banners** (`shared/.../shop/Banners.kt`, dibujo en `ui/profile/BannerArt.kt`, selector en `BannerSelector.kt`): 21 banners
  (2 gratis, 13 de tienda por monedas/gemas, 6 exclusivos del pase que rotan). El elegido vive en `UserProfile.bannerId`
  (se sincroniza con Firestore) y se ve en el perfil, la tarjeta del menú y las filas del ranking de amigos.
  Dueños: `EconomyManager.ownedBanners / buyBanner / grantBanner`. Precios en `Economy.BANNER_*`.
- **Avatares**: 8 animales nuevos (ajolote, capibara, tigre, león, lobo, oveja, erizo, tortuga), 5 accesorios nuevos
  (lazo, gafas, mago, auriculares, corona de flores) y **marcos** por valor (`AvatarDef.frame`, `AvatarFramed`):
  simple, plata, oro y prisma giratorio para los de temporada. 24 de tienda + 10 de temporada (rotan en 5 pares).
- **Pase** (`ui/season/SeasonScreen.kt`) rediseñado: colores de la skin exclusiva del mes, anillo de progreso, "premios estrella"
  (skin, avatares, banners, cofres con su dibujo real), riel con nodos, barra fija "Reclamar todo" y "Subir nivel".
  El pase ahora también da un banner gratis (nivel 9) y uno premium (nivel 22) cada mes.
- **Mapa**: cabecera en tarjeta translúcida, selector de capítulos (tocar el título), amigos que aparecen en su nivel,
  niebla en niveles bloqueados lejanos y "cofre del capítulo en N niveles" en la vista previa.
- Pruebas nuevas: `BannersTest`, ampliado `AvatarsTest`.

## Avisos al máximo + Halloween (icono e inicio)

- **Avisos** (`notifications/PardosNotifier.kt`): 4 canales (Premios y cofres, Racha y regresos, Fiestas y ligas, Partida) para que el jugador silencie
  solo una categoría; icono propio de una tinta (`ic_stat_pardos`); texto largo expandible; **al tocar un aviso se abre la pantalla que toca**
  (cofre → diálogo del cofre, ruleta, pase, tienda, amigos/liga, hucha). Qué avisar y cuándo sigue decidiéndolo `ReminderPlanner` (shared).
- **Voz de Halloween** (`shared/.../SeasonalCopy.kt`, con pruebas): del 1 de octubre al 2 de noviembre los avisos cambian de texto.
- **Ajustes → Avisos → "Enviar aviso de prueba"**: pide el permiso si falta y muestra un aviso real al instante.
- **Icono de Halloween**: calabaza tierna con sombrero de bruja, mejillas rosas y carita feliz, luna y murciélagos (`drawable/ic_halloween_*.xml`), con versión monocromática
  para los iconos temáticos de Android 13+. El icono clásico quedó guardado en `docs/icon-classic/` para volver a ponerlo
  (copiar esas carpetas a `app/src/main/res/` y borrar `mipmap-anydpi-v24`/`v26` nuevos).
- **Inicio de Halloween** (`ui/menu/HalloweenSplash.kt`): noche con luna, murciélagos, fantasmas, árbol seco, lápidas y la calabaza
  parpadeando como vela. Sale del 1 de octubre al 2 de noviembre; el resto del año, el inicio crema de siempre.
- Android 12+: la pantalla de inicio del sistema también usa el fondo de noche y la calabaza (`values-v31/themes.xml`).
- Pendiente de tu lado: en Play Console el icono de la ficha (512 px) se sube aparte; el icono nuevo solo cambia dentro de la app instalada.

## Firebase: consultas optimizadas (ronda 2)
Objetivo: que un jugador activo gaste ~1 escritura y casi 0 lecturas por sesión (plan gratis: 20 000 escrituras / 50 000 lecturas al día).

- **Caché de amigos por amigo y en disco** (`pardos_social_cache`): cada amigo tiene su propia vida útil (`SyncPolicy.friendTtlMs`): 15 min si juega hoy, 6 h si lleva días, 24 h si lleva semanas. Solo se leen los vencidos; reiniciar la app o abrir Perfil/Amigos/Mapa no vuelve a leer nada. El botón de actualizar fuerza la lectura, con 1 min mínimo entre refrescos.
- **Colección recordada**: cada amigo guarda si vive en `players` o en `users`; ya no se consulta `users` por los que están en `players`. Los amigos que no existen se recuerdan 24 h.
- **Sin red no se encolan escrituras** (el SDK las reenviaba todas al reconectar), no hay dos subidas a la vez, y tras un fallo se espera 1, 2, 4… min hasta 6 h (`uploadBackoffMs`) en vez de insistir cada minuto.
- **Consulta de la nube al arrancar cada 3 días** (antes cada día) y solo se restaura si la nube va por delante: guardar un perfil idéntico provocaba una subida inútil.
- **Agregar amigos**: tope de 100 amigos y máximo 12 búsquedas por hora (1,5 s entre una y otra) antes de gastar una lectura.
- **Consultas con `limit(10)`** y reglas (`firestore.rules`): en `players` solo se permite `get` o consultas con `limit <= 10`, así nadie puede barrer la colección. *Las reglas siguen sin publicarse: `firebase deploy --only firestore:rules --project pardos-b9b21`.*
- Ya estaba: subida con espera de 45 s, huella del contenido (no sube si no cambió), subida al salir de la app, 1 sola escritura por sesión.
- Recomendado en la consola (no es código): **Firebase App Check** (Play Integrity) y alertas de presupuesto/uso en Firestore.

## Novedades tras actualizar
- `shared/.../retention/WhatsNew.kt` (con pruebas en `WhatsNewTest`): lista corta (máx. 5) de lo nuevo y la regla `shouldShow`: se enseña **una sola vez**, solo desde la versión 16 y **nunca a una instalación nueva** (un jugador nuevo no necesita saber qué cambió).
- UI: `ui/rewards/WhatsNewDialog.kt`, enganchado en `RetentionSection` con la prioridad más baja (primero van reparar racha, regreso, subida de nivel, liga y skins). La versión vista se guarda en `pardos_settings` (`whats_new_seen`).
- Para la próxima actualización: sube `SINCE_VERSION`, cambia los textos de `items` y listo.
- `.gitignore` ahora ignora `*.aab`, `*.pem` y `app/release/` para no subir el bundle firmado ni el certificado de subida.
- Verificado en el teléfono: sale una vez, se cierra con "¡A jugar!" y no vuelve a aparecer al reabrir (`whats_new_seen=16`).

## Menú en horizontal
- El dock (Perfil / Multi / Álbum / Logros) estaba enterrado a mitad del scroll de la columna derecha. Ahora vive en la columna izquierda, debajo del título, siempre a la vista (esa columna también hace scroll en pantallas bajas). Probado en el A15 en horizontal: Perfil y Logros abren bien.

## Skins oscuras: contraste (revisión en el teléfono)
Probado con la skin oculta Obsidiana (fondo casi negro):
- **Mosaicos de "Tu día"** (Cofre / Ruleta / Pase): los activos tenían un fondo translúcido y las etiquetas oscuras se perdían sobre el fondo negro. Ahora llevan un fondo blanco debajo del tinte.
- **Barra de estado**: la hora y los iconos salían oscuros sobre fondo oscuro. Nuevo `ui/design/SystemBars.kt` (`DarkSystemBars` + `isDarkBackground`): con fondo oscuro los iconos pasan a claros en el menú y en el juego, y al salir vuelven a oscuros (antes se "restauraba" el valor anterior, que el splash de Halloween había dejado en claro, y las pantallas claras heredaban iconos blancos).
- Eslogan "SUMA Y RELÁJATE" más legible en fondos oscuros.
- Emojis sueltos que quedaban a la vista: 🥇 (perfil sin amigos) → corona cozy, ✏️ (editar nombre) → icono de lápiz, ☕ del botón de apoyo → quitado.

## Monetización amable (2026-10-08)
Todo cosmético o de comodidad: **nada se necesita para pasar los niveles**. Precios y números en `Economy.kt`; lógica en `:shared` con pruebas (`MonetizationTest`, `MergeEffectsTest`).
- **Packs de gemas**: 5 (`gems_tiny` 45 · `gems_small` 100 · `gems_medium` 550 · `gems_large` 1200 · `gems_huge` 3500). La tienda enseña el "+% extra" real de cada uno frente al más barato y marca "MEJOR VALOR". **Primera compra de cada pack = doble de gemas** (`GemPacks`, flag `purchased_<id>` en `EconomyManager`).
- **VIP con ventajas diarias** (`VipPerks`): +20 % de monedas por victoria y **5 gemas cada día** (botón "Regalo VIP de hoy" en Destacado).
- **Impulso de monedas** (`CoinBoost`): 20 gemas → +50 % de monedas en las próximas 5 victorias (acumulable hasta 15). Es un sumidero útil para las gemas. `EconomyManager.applyWinBonuses()` aplica VIP + impulso una vez por victoria (campaña y carrera); un VIP con impulso nunca pasa de ~2x.
- **Efectos de fusión** (`MergeFx`, 10 efectos): Clásico (gratis), Chispas, Burbujas (700 ●), Pétalos, Confeti (1500 ●), Estrellas, Ondas (70 ◆), Fuegos artificiales (140 ◆); **Corazones** viene en el Pack inicial y **Rayo** es premio del pase. Se dibujan en `ui/game/components/MergeFxCanvas.kt` (más partículas cuanto mayor la ficha) y se compran en la pestaña nueva **Efectos** de la tienda, con vista previa animada.
- Pack inicial: ahora incluye también el efecto Corazones.
- **Pase de temporada**: nivel 25 gratis regala el efecto Chispas (una muestra) y el nivel 26 premium regala Rayo (exclusivo). Se entregan con `RetentionManager.claim` → `economy.grantFx`.
- **Álbum ampliado a 9 series / 72 piezas** (nuevas: Mar Profundo, Fiesta, Noche Mágica). Bono de monedas hasta **+45 %**.
- **Piezas Brillantes** (`FoilRules`): cualquier pieza que tengas se puede hacer Brillante con esencia (el doble de lo que cuesta crearla). Luce un borde dorado con destello y cada 6 brillantes suman +1 % de monedas (se aplica en campaña y carrera vía `AlbumBonus.combine(…, foil)`).
- **Esencia por gemas** (`ShardShop`): 25 gemas → 100 de esencia, hasta 4 packs al día. Fila nueva "Esencia" en el Álbum.
- **Abrir cofres con más emoción**: pausa de suspense antes de las piezas épicas/legendarias, vibración por pieza (más fuerte en raras+), estallido de partículas al revelar (chispas / estrellas / fuegos artificiales según rareza), título "¡Pieza épica!" / "¡LEGENDARIA!" y brillo pulsante en las legendarias.

## Identidad visual: "juguete de gelatina" (2026-10-08)
Todo el juego comparte ahora un lenguaje propio en `ui/design/Jelly.kt`, `ToySwitch.kt` y `SystemBars.kt`:
- **`JellyCard` / `JellyRow` / `JellyColumn` / `JellySurface`**: superficies con labio sólido debajo (cálido en claros, oscuro en colores), brillo interior arriba y borde fino. Se hunden al pulsar con rebote. `JellySurface` es un sustituto directo de `material3.Surface` (se cambió en 25 archivos), `ToyAlertDialog` de `AlertDialog`.
- **`PrimaryButton`** ahora es un botón 3D (degradado, brillo de plástico, labio oscuro, se hunde). `PardosCard`, `PardosBackButton`, `IconTile`, `SectionLabel` (sello girado + puntitos) y `PardosTopBar` (ojito terracota) usan el mismo lenguaje.
- **`pardosBackdrop()`** sustituye a `.background(ScreenBackground)`: degradado cálido + mantel de cuadros muy tenue + tres manchas de color que respiran. Acepta un degradado propio (Records y modos de juego lo usan con el color del tema).
- **Movimiento**: `staggerIn` (entrada escalonada, repetible con `key`), `popIn` (diálogos), `breathing`, `wobble`; contadores de monedas/gemas que corren y dan un saltito al subir; `ToySwitch` en Ajustes.
- **Menú**: botón JUGAR con tres fichas flotantes (2, 4, 8), mosaicos de "Tu día", dock y píldoras con labio.
- **Splash**: el de Halloween se rehízo (casa embrujada con ventanas que parpadean, niebla, luciérnagas, araña colgando, iconos 3D, textos legibles); el normal pasó de "un círculo gris" a una escena con el logo flotando, fichas de juguete y las letras de PARDOS cayendo una a una (`ic_logo_classic.xml` = el logo sin disfraz).
- **Ruleta**: iconos cozy en las casillas, bombillas que corren por el aro, vibración por casilla, estallido al ganar, tarjeta "Qué puedes ganar".
- **Récords**: medallas de podio (oro con trofeo, plata, bronce) y tarjetas con labio. **Perfil**: insignias como medallas (bloqueadas = ranura con candado).
- **Iconos 3D descargados** (Fluent Emoji, MIT, 28 PNG en `res/drawable-nodpi/ico_*.png`): ver `docs/CREDITOS.md`.

### Controles y sensaciones (misma ronda)
- **`ToyButton` / `ToyTextButton` / `ToyTextField`** (`ui/design/ToyControls.kt`): sustitutos directos de `Button`, `TextButton` y `OutlinedTextField`. Botón 3D con labio, botón de texto en píldora suave y campo "hundido" que enciende su borde con el foco. Se cambiaron en todas las pantallas menos las de accesibilidad (`Accessible*`), que se dejan con los controles estándar a propósito para no romper TalkBack.
- **Tic al tocar**: todo lo que sea `JellyCard` pulsable y el `ToySwitch` dan una vibración muy suave (respeta el interruptor de Vibración de Ajustes).
- **Diálogo del regalo diario**: llama 3D que respira, mosaicos de día con labio y moneda/cofre cozy, botón de reclamar de juguete. **Banners de eventos** con iconos 3D (calabaza, calavera, árbol, fuegos, cupido, tulipán, playa, globo…).
- Emojis: en pantalla ya no queda ninguno (se dibujan iconos); solo permanecen en texto de notificaciones y en el mensaje de compartir, donde no se pueden dibujar imágenes.

### Icono, fondos y optimización (2026-10-08, noche)
- **Icono nuevo** (siempre el mismo, ya no vestido de Halloween): logo del tablero sobre el verde salvia de la marca con luz y mantel muy tenue (`ic_icon_bg.xml` / `ic_icon_fg.xml`, también como monocromo para iconos temáticos de Android 13+). La **pantalla de inicio del sistema** (Android 12+) ya no es negra con calabaza: papel cálido con el logo, y el splash de Halloween entra con un fundido en vez de un salto.
- **Fondo nuevo** (`pardosBackdrop`): se quitó el mantel de cuadros inclinado (anticuado) y ahora hay luces suaves, manchas de color que respiran y **fichas fantasma del juego** (2, 4, 8, 16...) subiendo despacio. Se pinta a ~30 fps solo en la fase de dibujo (medido: 0 % de fotogramas lentos). También vive en el menú, el juego y los modos (`PicnicBackgroundOptimized` ahora usa el mismo, encima del degradado de la skin).
- **Tarjetas de Logros** ahora opacas (antes eran translúcidas y dejaban ver el fondo); bloqueadas = ranura de papel, logradas = tarjeta con labio de color.
- **Color de acción vivo** (`Color.actionTone()`): los botones y selecciones de cada skin usan el mismo tono pero más saturado, en vez de oscurecerlo con negro (quedaba lodoso).
- **Optimización**: R8 + recorte de recursos en `release` (el APK de release pasó de ~35 MB de debug a ~9,8 MB), reglas en `proguard-rules.pro` (modelos de Firestore y enums se conservan). Se probó la versión minificada con 1500 eventos aleatorios sin errores (`./gradlew assembleDebug -PpardosMinifyDebug`). Las imágenes PNG (avatares e iconos 3D) pasaron a WebP sin pérdida: -0,5 MB.

## Retención: misiones y avisos (2026-10-08)
- **Misiones del día rehechas** (`DailyMissionPlan`, con pruebas): cada día lleva **una fácil, una media y una difícil, de tipos distintos** (antes eran 3 al azar y podían salir las tres iguales o las tres duras). Determinista por día local. La tarjeta nueva enseña anillo de progreso, cuenta atrás ("se renuevan en 11 h 44 min"), premio de cada misión (monedas + XP), medallas con iconos cozy, botón COBRAR que respira y el premio por cobrar las tres.
- **Racha de días perfectos** (`PerfectDays`): encadenar días con las tres misiones cobradas da premios en 3, 7, 14 y 30 días (3 / 8 / 15 / 40 gemas). La tarjeta enseña la racha, cuánto falta para el siguiente hito y puntitos de hitos.
- **Avisos nuevos** (`ReminderPlanner`, con pruebas; nunca de madrugada): misiones nuevas cada mañana (10:30), misiones listas para cobrar, racha perfecta en riesgo (19:00), reto diario sin jugar (13:00) y cofres sin abrir. Canal propio "Misiones y reto diario"; los avisos de cofres abren el Álbum.
- **Permiso de avisos con contexto** (`NotificationPrimer`): ya no se pide al abrir la app. Tras la primera victoria sale un diálogo que explica qué se avisará (cofre listo, racha en riesgo, misiones, fiestas); si dice "ahora no", se vuelve a preguntar a los 3 días, máximo 3 veces, y nunca a quien ya aceptó o los apagó.
- **Avisos sin emojis**: cada aviso lleva un icono 3D grande (calabaza, fantasma, murciélago, caramelo, corona, fuego...) en lugar de emojis en el texto.

## Niveles con reglas y variedad (2026-10-08)
- **Cada nivel ya no es solo "llega a X"**: hay 9 tipos (Zen, Puntos, Lluvia de 4, Con ventaja, Piedras, Sprint, Gemelas, Contrarreloj y Jefe) con tablero, meta y límites propios. Ver `docs/NIVELES.md`.
- **Piedras** que no se mueven ni reciben fichas y parten las filas (motor `GameEngine` con casillas bloqueadas; sin piedras es idéntico al anterior, comprobado contra una copia del algoritmo viejo).
- **Estrellas de verdad**: la campaña pagaba siempre 3★; ahora premian la eficiencia (movimientos o tiempo) y la tarjeta de victoria explica cómo conseguir las 3.
- **Mapa y tarjeta del nivel**: cada nodo lleva la insignia de su tipo, los jefes tienen forma y borde propios; la tarjeta enseña un plano del tablero con sus piedras y fichas, la meta, el límite y las 3 estrellas. En la partida, la cabecera muestra tipo, movimientos que quedan y piedras; la primera vez que sale cada regla (y en cada jefe) una tarjeta la explica.
- **Variedad y duración**: la ficha base sube hasta 1024 y ahí se queda (antes eran 2.330 niveles de 4096); desde ahí la dificultad viene de las reglas. Cada capítulo sigue un guion (Zen de bienvenida, jefes en el 10 y el 20, especiales en el 5 y el 15, nunca dos iguales seguidos). Capítulo 1 escrito a mano como tutorial.
- **Fácil de ampliar**: un nivel nuevo es una línea en `HandmadeLevels`; subir `TOTAL_LEVELS` alarga la campaña. `LevelValidator` y las pruebas de todo el catálogo + un bot jugador (`LevelFeasibilityTest`) avisan si un nivel es imposible o injusto.
- **Reto diario** con un tipo de regla por día de la semana.
- Si te quedas sin movimientos o sin tiempo, la pantalla de derrota lo dice y el anuncio de revivir da movimientos extra (sin limpiar el tablero).
- Solo debug: `--ei debug_level N` abre un nivel directo.

## Tutorial, manos y Halloween (2026-10-08)
- **Tutorial del nivel 1 (solo la primera vez)**: ya no es una manita blanca que desaparece al primer movimiento. Un guion (`TutorialCoach`, con pruebas) enseña 1) deslizar, 2) que dos fichas iguales se suman (resalta las dos fichas con un halo dorado y la mano las empuja hacia donde toca), 3) repetirlo hasta tres fusiones. Una tarjeta ocupa el sitio de los poderes con el texto del paso y tres puntitos de avance; el tutorial termina solo (o tras 24 movimientos) y no vuelve a salir.
- **Mano nueva**: la mano 3D del resto de iconos (en vez de un icono de Material blanco): se acerca, se hunde al tocar con una onda, desliza dejando estela y flecha y se suelta, rotada hacia donde se mueve.
- **Pista por inactividad**: en los niveles 1 a 15, si pasan 7 segundos sin mover, la mano enseña una jugada útil (`MoveAdvisor`, con pruebas: prefiere la que más funde, respeta las piedras).
- **Pantalla de Halloween rehecha** (`HalloweenSplash`): escena en capas — guirnalda de luces con calabazas colgadas, luna enorme con murciélagos delante, casona con humo y ventanas encendidas en su colina, camino con farolillos, árbol con gato y farol, lápidas y dos montones de calabazas con luz propia a los lados (el centro queda libre para los créditos), niebla y viñeta. El título PARDOS conserva su tipografía y rebote. Se dibuja leyendo las animaciones dentro del lienzo (no recompone la pantalla en cada fotograma) y las barras del sistema cambian de color con el fundido del arranque.


## Noche de brujas en toda la app (2026-10-08)
- **Un interruptor de temporada** (`Season.halloween`, ui/design/Season.kt): activo del 1 de octubre al 2 de noviembre. Cambia solo, sin tocar nada, y el resto del año todo vuelve a ser como siempre.
- **Paleta**: los colores de marca se vuelven getters: acción en naranja calabaza, "energía" en morado, tinta de berenjena y fondos con un tinte lila (los verdes y azul-marino sueltos en el código se pasaron a esos colores).
- **Fondo de todas las pantallas** (`pardosBackdrop`): suben calabazas, fantasmas, murciélagos, dulces y calaveras en lugar de fichas con números; telarañas en las esquinas y una araña que cuelga y se balancea.
- **Menú**: cuerda de luces de colores en lo alto, botón JUGAR naranja con calabaza, fantasma y murciélago ("Nivel 47 · ¡BUU!"), lema "SUMA… SI TE ATREVES", los atajos Cofre/Ruleta/Pase/Hucha con sus iconos 3D (calabaza, fantasma, murciélago, caramelo) y telaraña en la esquina de todos los diálogos.
- **Mapa de campaña**: 12 mundos embrujados (Cementerio Sereno, Bosque Encantado, Pantano Brumoso, Calabazar, Faro Fantasma, Mansión Embrujada, Mercado de Brujas, Pico Aullante, Árbol de las Almas, Tumbas de Momias, Bahía del Barco Fantasma, Templo Maldito), todos de noche con luna de cosecha, murciélagos que cruzan el cielo, calabazas y lápidas junto al camino, y cuatro monumentos nuevos (verja con fantasma, bosque con búho y ojos, calabaza gigante, mansión).
- **Partida**: telarañas y bolitas de temporada de fondo, victoria "¡MONSTRUOSO!" / "¡BUEN TRUCO!", derrotas "¡Los fantasmas llenaron el tablero!", "¡Se acabaron los hechizos!", "¡Dieron las doce!".
- **Bienvenida** (una vez por año, solo a quien ya jugaba): "¡Llegó la fiesta más oscura!"; si ya tienes la skin Noche de brujas, ofrece ponértela con un toque.
- Las etiquetas de reglas del nivel (sprint, piedras...) ahora se leen también sobre skins oscuras.


## Contenido de niveles para ~1 mes y medio
6 tipos de nivel nuevos (Pesadas, Escalera, Del revés, Maratón, Tormenta, Combo), 4 giros de controles, tableros 6×6, jefes nuevos, más distribuciones de piedras y una dificultad que sigue subiendo hasta el nivel 2.400. Calendario de novedades, horas estimadas (~173 h) y cómo se probó en `docs/CONTENIDO_NIVELES.md`.

## Más contenido: Cosecha, Doble caída, Callejones, jefes con 2 fases y Torre infinita
Dos tipos de nivel más (17 en total), 4 callejones (una dirección prohibida), **jefes con segunda fase** (16 recetas), **Torre infinita** (pisos con corazones, cada 5 un jefe) y retos diarios en 3 semanas. Detalle y calendario en `docs/CONTENIDO_NIVELES.md`.
