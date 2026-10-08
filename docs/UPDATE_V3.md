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
app/      (Android)
  data/local        EconomyManager, CollectionManager, RewardsManager, DailyRewardManager, LevelProgressStore, ProfileManager, MissionManager
  data/auth         AuthManager (Google + Firebase Auth)
  data/billing      BillingManager (Google Play Billing 8)
  ui/design         PardosDesign.kt  <- colores, formas y componentes base (ÚNICA fuente de verdad)
  ui/menu           MenuScreen/MenuHome, LevelSelectorScreen (mapa), ModelSelectionScreen, DailyRewardDialog
  ui/game           GameScreen, GameHud, GameOverlays, AchievementScreen, components/ (tablero, fichas, poderes)
  ui/shop           ShopScreen
  ui/profile        ProfileScreen, FriendsScreen
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

## Convenciones importantes

- **Todos los tiempos del estado del juego están en milisegundos** (`maxTime`, `elapsedTime`, `GameMode.timeLimit`).
- Los días de racha/regalo/ranking son **días locales** (`LocalDay.today()`), no UTC.
- Colores: usar `ui/design` (`Navy`, `Sage`, `Terracotta`, `Gold`…). Para botones/selección sobre un tema usar
  `GameTheme.actionColor`; el `accentColor` del tema es un pastel solo para fondos.

## Anuncios

Se usan los **IDs de prueba de Google** hasta publicar. Antes de lanzar, restaurar en
`AndroidManifest.xml` (App ID) y `AdManager.kt` (unidad de recompensa) los IDs reales.

## Compras (Play Console)

Crear 4 *productos administrados* con estos IDs exactos (ver `ShopCatalog`):
`gems_small` (100 gemas), `gems_medium` (550), `gems_large` (1200), `vip_forever` y `starter_pack`.
Las compras solo se pueden probar con la app subida a una pista de pruebas y una cuenta de probador.

## Pendiente / decisiones abiertas

- **Seguridad de Firestore:** los perfiles usan el `ANDROID_ID` como id de documento sin Firebase Auth, por lo que las
  reglas deben ser abiertas. Recomendado: Firebase Auth + reglas `request.auth.uid == userId`. Hasta entonces no se
  añaden regalos/retos entre amigos (requieren escribir en el perfil de otro jugador).
- **iOS:** falta mover la UI a Compose Multiplatform y el almacenamiento/Firebase/Billing a implementaciones
  multiplataforma. Solo se puede compilar en un Mac con Xcode.
- Modo horizontal de varias pantallas sin verificar en dispositivo.
