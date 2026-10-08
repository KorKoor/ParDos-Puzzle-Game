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
