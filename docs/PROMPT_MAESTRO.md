# PROMPT MAESTRO — continuar ParDos en otro chat

Pega todo lo de abajo como primer mensaje del chat nuevo.

---

Vas a continuar el trabajo en **ParDos: Math Zen Puzzle**, mi juego Android estilo 2048 (`com.korkoor.pardos`). Hablo español; responde siempre en español. Tienes libertad creativa total y permiso total sobre mi PC; yo no soy quien programa, así que explícame en simple qué hiciste y qué me toca a mí.

## Dónde está todo
- Proyecto: `C:\Users\carlo\Documents\Android\ParDos-Puzzle-Game`, rama `update-v3` (solo local, NO subida a GitHub). Repo original: https://github.com/KorKoor/ParDos-Puzzle-Game.git
- Lee primero: `docs/UPDATE_V3.md` (arquitectura, convenciones, productos de Play) y la memoria en `C:\Users\carlo\.claude\projects\C--Users-carlo-Documents-Android\memory\` (`MEMORY.md` y `build-setup.md`).
- Las carpetas dicen `com/example/pardos` pero los paquetes son `com.korkoor.pardos.*`.

## Entorno de compilación y pruebas
- JDK 21: `export JAVA_HOME="/c/Users/carlo/.jdks/jdk-21.0.12.1+1"` (el JDK 25 rompe Gradle).
- Pruebas de la lógica compartida: `./gradlew :shared:testDebugUnitTest --console=plain -q` (201 pruebas, todas pasan).
- APK: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- Teléfono Samsung A15 (serial `R5CX936T4CT`). adb: `~/AppData/Local/Android/Sdk/platform-tools/adb.exe`. Usa `export MSYS_NO_PATHCONV=1`. App de pruebas: `com.korkoor.pardos.debug` (el sufijo `.debug` hace que conviva con mi app de Play). Actividad: `com.korkoor.pardos.MainActivity`.
- Capturas 1080x2340. Para inyectar estado: `adb push` a `/data/local/tmp` + `run-as com.korkoor.pardos.debug cp ... shared_prefs/`. Scripts útiles en la carpeta scratchpad de la sesión (`setskin.sh <idSkin>`, `fb.js` para Firebase por REST).
- Para editar archivos con caracteres especiales usa la herramienta Write + scripts de Python (los heredocs de bash con comillas triples fallan).
- Las animaciones del teléfono ya están activadas; no las toques.

## Reglas que me importan
1. **Anuncios: SOLO IDs de prueba de Google** hasta que yo diga que voy a publicar. No hay banner inferior (lo quité a propósito). IDs reales para el día de publicar: app `ca-app-pub-3851960142449906~8749596168`, rewarded `/7125882091`.
2. Mantener mis colores e identidad visual (Navy, Sage, Terracotta, Gold, Sand, GemBlue, Violet, Cream, Paper) y modernizar. Sistema de diseño en `ui/design/PardosDesign.kt`.
3. Tiempos del estado de juego en **milisegundos**; los días son **días locales** (`LocalDay.today()`), no UTC.
4. La lógica pura (reglas, datos, números) va en el módulo KMP `:shared` con pruebas; la app solo tiene UI y managers Android. Meta a futuro: iOS/App Store.
5. Todos los precios y premios viven en `shared/.../domain/economy/Economy.kt` (y objetos de `domain/retention`).
6. No hagas commit, push ni nada hacia fuera sin que lo pida. Nunca metas `amdin-key.json` (clave de admin de Firebase) en git; está ignorada.
7. Las compras reales van al final; no las pruebes con dinero.

## Qué ya existe (resumen)
- Motor con semilla determinista, retos diarios iguales para todos, modos: Clásico (campaña con mapa y capítulos), Desafío, Zen, Rápido, Tablas, Carrera, Duelo local, Personalizado.
- Perfil, XP y nivel, racha con escudos, recompensas diarias de 7 días, logros pagados por rareza, eventos programados (fin de semana dorado, miércoles XP, semana festival), amigos por código y ranking semanal, inicio de sesión con Google (Credential Manager), Firebase (proyecto `pardos-b9b21`). **Ya publiqué las reglas de Firestore.**
- Economía: monedas, gemas, esencia; Álbum de 48 coleccionables en 6 series con cofres (común/raro/épico), garantías, esencia y crafteo; tienda con pestañas, oferta del día, pack inicial, VIP, consumibles (Deshacer, escudo de racha).
- Skins con temática completa (fichas + fondo + partículas).

## Lo último que se hizo (SIN COMMIT TODAVÍA, 201 pruebas pasan)
**Verificado en el teléfono con capturas** (2026-10-07): franja "Tu día", ruleta (giro y premio), pase (reclamar, saltar nivel), cofre gratis,
hucha, Studio (editor), tienda, semanales, resumen de victoria con extras, compartir.
Correcciones: botón deshabilitado con sombra rara, "1 premios", salto de nivel del pase ahora proporcional (10–40 gemas), el menú mostraba
"Nivel 3" aunque el progreso real era 21 (el perfil de la nube pisaba el nivel de campaña: ahora `healCampaignLevel` en `MainActivity` lo sube
al progreso real), botón "SIGUIENTE" que se partía en dos líneas.
**Funciones nuevas:**
- **Ligas semanales** (Bronce→Diamante) en `shared/.../retention/Leagues.kt` + `RetentionManager` + `ui/rewards/LeagueCard.kt`
  (tarjeta en el menú, diálogo de reglas y diálogo de resultado semanal con premio). Probado el ascenso inyectando 25★ en la semana pasada.
- **Compartir resultado** (`ShareText`): botón en el resumen de victoria (reto diario con fecha) y texto de invitación con código de amigo.
- **Tiempo extra** (consumable): compra x3 en la tienda, chip "+20s · N" junto al reloj en modos con reloj (verificado: 01:30→01:50).
- **Eventos de calendario**: Noche de brujas, Día de Muertos, Navidad, Año nuevo, San Valentín (monedas x1.5). Con pruebas.
- **Ajustes** (sonido, música, vibración, avisos) con engranaje en el perfil; **Siguiente meta** en el resumen de victoria;
  **Recuperar racha** (gemas o anuncio, el mismo día) — los tres verificados en el teléfono.
- **Fiestas y skins**: 11 fiestas del calendario (San Valentín, Primavera, Verano, Fiestas patrias, Noche de brujas, Muertos, Navidad,
  Año nuevo...), 8 skins de fiesta ganables jugando y 8 skins SECRETAS con pistas; 10 partículas nuevas. Todo verificado en el
  teléfono con capturas (usar `--ei debug_day_offset N` para probar fechas). Ver `docs/UPDATE_V3.md`.
- **Duelo a distancia por código** (multijugador sin servidor), **iconos de poderes** dibujados a mano y **mapa de campaña con 12 mundos**
  — verificados en el teléfono. IMPORTANTE: el usuario RECHAZÓ un logo nuevo de fichas de colores para "PARDOS"; el título original
  (letras en tinta con rebote) es su identidad y NO se toca sin que lo pida.
- **Iconos cozy** (reemplazan a los de Material en toda la app, ver UPDATE_V3) y **mapa vivo**: cielo/sol/luna/nubes/parallax/partículas por
  capítulo, 12 monumentos, avatar en el nivel actual, animación de desbloqueo. Verificados con capturas.
- PENDIENTE DEL USUARIO en Play Console (panel integrado del chat): para crear los productos hay que elegir/crear el PERFIL DE PAGOS
  (cuenta de comerciante de Google Payments); es decisión financiera suya. Después se pueden crear los 8 productos (ver docs/PLAY_PRODUCTOS.md).
  URL de la app: .../developers/8884739635851588625/app/4976287900309150551/one-time-products
- **Aviso de liga** en `ReminderPlanner` (últimos 2 días de la semana si hay riesgo o ascenso cerca).
Lo último (eventos de calendario) compila y pasa pruebas pero NO está instalado en el teléfono (se desconectó); reinstalar y probar el banner.

## Siguientes pasos (en este orden)
1. Reinstalar el APK en el teléfono y revisar: modo horizontal y skins oscuras (pendiente de pasadas visuales), tarjeta de liga con skin oscura.
2. Hacer commit (mensaje en español, rama `update-v3`) cuando el usuario lo apruebe.
3. Más social (requiere Firestore + Auth; reglas ya publicadas): retos entre amigos, regalos, torneo semanal real con rivales (las ligas actuales son personales).
4. Pulir economía con datos de pruebas (ruleta, pase, semanales, ligas).
5. Futuro: Compose Multiplatform + iOS (necesita Mac/Xcode), subir la rama a GitHub como respaldo (preguntar antes).

## Pendientes que dependen de MÍ (recuérdamelos al final)
- Crear en Play Console los 8 productos de arriba y añadir la SHA-1 de firma de Play a Firebase (app `com.korkoor.pardos`).
- Activar autenticación anónima en Firebase si la quiero; probar el inicio de sesión con Google completo.
- Cambiar a los IDs reales de AdMob solo al publicar.
- Mover `amdin-key.json` fuera de la carpeta del proyecto.

Empieza leyendo `docs/UPDATE_V3.md` y la memoria, haz `git status` para confirmar que los cambios de esta sesión siguen sin commit, y continúa por el paso 1.
