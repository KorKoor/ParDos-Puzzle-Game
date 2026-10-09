# ParDos en iPhone: .ipa con GitHub Actions + Sideloadly

Esto genera un `.ipa` **sin firmar** en la nube (GitHub Actions, en un Mac virtual) para instalarlo en tu iPhone con
[Sideloadly](https://sideloadly.io), que lo firma con tu Apple ID. No necesitas Mac ni cuenta de desarrollador de pago.

## Qué hay (y qué no)

La app de Android es Jetpack Compose y no se puede compilar para iPhone. Para poder probar en iOS hay una **app nativa mínima
en SwiftUI** (`iosApp/`) que usa la **misma lógica del juego** que Android: el módulo `:shared` (Kotlin) se compila a un
`Shared.xcframework`, y SwiftUI solo dibuja.

| | Sí está en la versión de iPhone | No está (solo Android por ahora) |
|---|---|---|
| Juego | motor, 2.400 niveles con todas sus reglas (piedras, tormentas, giros, jefes con fase 2, metas de puntos/escalera/combo/cosecha...), estrellas, deshacer, reloj | modos Multi-Mates, Carrera, Duelo, Zen, Studio, Torre |
| Ayudas | tutorial del nivel 1 con la mano 3D, pista si te quedas parado 7 s (niveles 1-15), botón Pista, tarjeta que explica cada regla nueva y cada jefe la primera vez | poderes (Limpiar, Fusión, Escoba) |
| Meta | mapa de campaña (12 capítulos con cartel y estrellas), **reto diario** (mismo tablero para todos, 3 estrellas) y **racha** de días, progreso guardado en el teléfono | tienda, cofres, álbum, pase, misiones, ligas, perfil, amigos, anuncios, Google/Firebase |
| Ambiente | Noche de brujas en octubre: fondo con calabazas/fantasmas/murciélagos flotando, telarañas, luces, confeti al ganar, portada, sonidos y música, vibración | |
| Ajustes | sonido, música, vibración y borrar progreso | |

Es una **versión de prueba**: sirve para sentir el juego y los niveles en iPhone y detectar problemas, no para publicar.

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
