# ParDos en iPhone: .ipa con GitHub Actions + Sideloadly

Esto genera un `.ipa` **sin firmar** en la nube (GitHub Actions, en un Mac virtual) para instalarlo en tu iPhone con
[Sideloadly](https://sideloadly.io), que lo firma con tu Apple ID. No necesitas Mac ni cuenta de desarrollador de pago.

## Qué hay (y qué no)

La app de Android es Jetpack Compose y no se puede compilar para iPhone. Para iOS hay una **app nativa en SwiftUI** (`iosApp/`) que
usa la **misma lógica** que Android: el módulo `:shared` (Kotlin) se compila a un `Shared.xcframework` y SwiftUI solo dibuja.
Hay dos "cerebros" compartidos: `GameSession` (la partida) y `MetaSession` (todo lo que la rodea: monedas, tienda, cofres, álbum,
misiones, pase, liga, logros, prestigio). Los dos tienen pruebas en Windows y hablan con Swift en JSON.

| Área | En iPhone |
|---|---|
| Juego | 2.400 niveles con todas sus reglas, jefes con fase 2, estrellas, reloj, deshacer (con inventario), pista, tutorial con mano 3D, "flow", ayuda tras perder varias veces |
| Poderes | Limpiar y Fusión (espera de 15 min), Escoba y Unir (80 monedas: en iPhone no hay anuncios con premio), Tiempo extra |
| Modos | Campaña, reto diario, Torre infinita (3 corazones), Carrera, Duelo local, Duelo a distancia por código (compatible con Android), Tablas, Partida libre con atajos Zen y Rápido |
| Economía | monedas, gemas, esencia, fichas, racha de días con escudos, regalo diario, cofre gratis cada 4 h, ruleta, hucha, primera victoria del día |
| Tienda | skins (42, con eventos y secretas), efectos de fusión, ayudas, cofres, ofertas del día, Studio (editor de tu skin) y **packs de gemas con precios de App Store** (ver `docs/APPSTORE_PRODUCTOS.md`) |
| Álbum | 32 series / 320 piezas, cofres con garantías, repetidas (vender/reciclar), crear piezas, brillantes, vitrina, recompensas de serie y de álbum, mejoras |
| Metas | misiones diarias y semanales, días perfectos, pase de temporada (30 niveles, gratis y premium), liga semanal, eventos de calendario |
| Perfil | 150 avatares y 60 banners (se componen con emojis y degradados), títulos, 82 logros, prestigio (8 rangos, ~75 hitos, Platino), récords |
| Ambiente | Noche de brujas en octubre, partículas y fondos de cada skin, efectos de fusión, sonidos y música, vibración, avisos del teléfono (notificaciones locales) |
| Más | copia de seguridad en texto (importante con Sideloadly), VoiceOver, avisos del teléfono, StoreKit 2 listo para la App Store, iPad, segunda oportunidad con gemas, perfil inicial, novedades |
| Amigos | **sin cuenta**: compartes una tarjeta (código `PF1-…`) por WhatsApp, quien la pega te agrega y se arma un ranking por prestigio con rival de arriba y de abajo |
| No está | amigos/ranking/intercambios en línea con Firebase y Google (necesitan decidir cómo conectar iOS al mismo proyecto), anuncios, inglés |

Es una **versión de prueba**: sirve para sentir el juego y la economía en iPhone, no para publicar.

## Pasos

1. **Sube el código a GitHub** (el repositorio ya apunta a `KorKoor/ParDos-Puzzle-Game`). Necesitas subir estas carpetas/archivos
   nuevos: `.github/workflows/ios-ipa.yml`, `iosApp/`, `shared/` y `docs/IOS.md`.
2. **Lanza la compilación** (cualquiera de las tres):
   - Pestaña **Actions** → *iOS .ipa (sin firmar, para Sideloadly)* → **Run workflow**. (El botón solo aparece si el archivo
     del workflow ya está en la rama principal del repositorio, normalmente `main`.)
   - O sube la rama: `git push origin HEAD:ios-build`
   - O una etiqueta: `git tag ios-1 && git push origin ios-1`
3. Espera unos **15–25 minutos** la primera vez (descarga el compilador de Kotlin/Native; las siguientes veces va más rápido).
4. Cuando termine en verde, abre la ejecución y baja **ParDos-ipa** (en *Artifacts*, abajo). Es un `.zip` con `ParDos-unsigned.ipa`.
   Si falla, baja **xcodebuild-log** y pásamelo: lo arreglo.
5. **Sideloadly** (en Windows o Mac): conecta el iPhone por cable, arrastra `ParDos-unsigned.ipa`, escribe tu Apple ID y pulsa *Start*.
6. En el iPhone: **Ajustes → General → VPN y gestión de dispositivos →** tu Apple ID → *Confiar*. Si pide **Modo desarrollador**
   (iOS 16+): Ajustes → Privacidad y seguridad → Modo desarrollador → activar y reiniciar.

Con un Apple ID gratuito la app **caduca a los 7 días** (hay que volver a instalarla con Sideloadly) y puedes tener 3 apps así a la vez.

## Cómo está montado

- `shared/build.gradle.kts` registra el `XCFramework("Shared")` (estático, para que quede dentro del ejecutable).
- `shared/.../domain/session/GameSession.kt` es la partida completa sin interfaz (con pruebas en `GameSessionTest`); entrega a
  Swift enteros y JSON. `GameSessionTest` también comprueba que el JSON trae todas las claves que lee `iosApp/ParDos/Models.swift`.
- `iosApp/project.yml` es el proyecto de Xcode (lo genera XcodeGen en la nube: no hace falta tener `.xcodeproj`).
- `iosApp/ParDos/`: `AppModel` (progreso en UserDefaults), `SoundManager` (mp3 en `Sounds/`), `Decor` (adornos y animaciones),
  `Chapters` (temas de capítulo), `Theme` (paleta; `Theme.halloween` se activa en octubre).
- `iosApp/tools/check_swift_contract.py` comprueba que **todo el JSON real** que entrega Kotlin se puede leer con las estructuras `Decodable` de Swift (claves y tipos). Es la red de seguridad contra pantallas vacías; corre en CI antes de compilar el Swift.
- `iosApp/tools/check_swift_syntax.py` revisa la sintaxis de todos los `.swift` sin Mac (tree-sitter). El workflow lo corre primero en
  Linux (job `swift-syntax`), así un error tonto falla en segundos y no tras 20 minutos de Mac. No comprueba tipos.
- El workflow: pruebas de la sesión y el motor → `./gradlew :shared:assembleSharedReleaseXCFramework` → `xcodegen generate` →
  `xcodebuild` sin firmar → empaqueta `Payload/ParDos.app` en `ParDos-unsigned.ipa`.

## Cambiar el aspecto o añadir pantallas

La lógica va en `:shared` (se prueba en Windows con `./gradlew :shared:testDebugUnitTest`); la interfaz está en
`iosApp/ParDos/*.swift` (Theme, Models, AppModel, MenuAndMapViews, GameViews, BoardView, Decor, Chapters, SoundManager). Para que algo nuevo cruce a Swift,
añade la clave al JSON de `GameSession.snapshot()` y al struct de `Models.swift` (y a la lista de claves de `GameSessionTest`).
Reglas para el Swift: solo APIs de iOS 15 (`kerning`, no `tracking`), expresiones cortas, nada de `if let` con comas largas en vistas.

## Aviso

El módulo compartido sí se compiló para iOS en Windows (como librería), pero **la parte Swift y el empaquetado no se han podido
compilar ni probar fuera de GitHub** (hace falta un Mac). Si la primera ejecución falla, es normal: me pasas el registro y se corrige.
