# iPhone v4: sin emojis, con sonido y más motivos para volver

Todo esto está hecho en el código pero **no se ha compilado ni subido** (falta un Mac; el compilador de Swift solo corre en GitHub Actions).
Cuando pidas la build, si aparece un error de compilación se arregla en la siguiente vuelta.

## Arte sin emojis

- Los dibujos se hacen en Python (`iosApp/tools/art`), se ven como PNG antes de usarlos y se guardan como JSON en `iosApp/ParDos/Art/art_*.json`.
  `Art.swift` los lee y los pinta con `Canvas`. Guía de estilo y cómo crear más: `iosApp/tools/art/GUIA.md`.
- Hechos (256 dibujos): 34 animales de avatar (con parpadeo y las 12 variantes de color), 24 accesorios, 14 escenas de fondo,
  30 banners con capa animada, 34 partículas, 24 emblemas de capítulo (día y noche de brujas), 64 adornos del mapa,
  23 iconos "cozy" y de poderes, rangos de prestigio, medallas y cofres abiertos/cerrados.
- Pantallas ya conectadas: avatares y banners (animados), mapa (emblema por capítulo, camino con cinta punteada, adornos por mundo),
  partículas de fusión y de fondo, motivos de las cartas, escudo de cada serie del álbum.
- **Las 320 piezas del álbum están ilustradas** (flores, animales, comida, vehículos, monumentos...), cada una con su dibujo vectorial; las que aún no tienes se ven como silueta. Los escudos de las series usan la pieza legendaria de cada serie. Los dibujos están en `iosApp/tools/art/packs/pieces_*.py` y se ven con `python build.py pieces_N` y `python sheet.py pieces_N`.
- Banners: los 30 patrones tienen dibujo propio (base + capa animada, y una segunda capa para los que titilan). Las siluetas y partículas genéricas quedan solo como respaldo.
- Avatar propio: en Perfil, "Mi propio sticker": un emoji o Memoji de Apple (teclado de emojis), una foto o una imagen pegada,
  con movimiento (flotar, respirar, bailar, rebotar, latido, girar, brillar) y fondo a elegir. Solo se guarda en ese iPhone.

## Sonido

- 115 efectos y 5 músicas de fondo generados por código (`iosApp/tools/audio`: `python make_sfx.py`, `python make_music.py`).
  Los efectos son `sfx_*.mp3` y la lista de nombres se escribe sola en `Sfx.swift`.
- Música por pantalla: menú, mapa, partida, tienda/álbum y Noche de brujas. Entra y sale con fundido y se baja en las fanfarrias.
- Efectos en: toques (tic global), pestañas, fusiones (cambian con el tamaño de la ficha y los combos), poderes, victoria y estrellas,
  derrota, cofres (común, raro, épico, legendario), cartas, ruleta (giro, freno y premio), misiones, pase, liga, calendario, compras.
- Vibraciones (Taptic) acompañan los sonidos. En Ajustes: interruptores, dos volúmenes y el ahorro de energía.
- Nadie pudo "oír" los sonidos aquí: se comprobaron con medidas (volumen, recortes, graves) y hay que escucharlos en el teléfono.

## Más motivos para volver (módulo compartido, con 11 pruebas nuevas)

- **Hora feliz:** cada día hay una ventana (60 min, 90 en fin de semana, entre las 11:00 y las 22:00) con monedas x2 en las victorias.
  Tarjeta en Inicio y aviso 5 minutos antes.
- **Calendario del mes:** casilla diaria con premios crecientes (gemas los días 5/10/15/20/25, cofres el 7/14/21 y gran premio el último día,
  cofre extra a las 10 y 20 casillas). Los días perdidos se recuperan (el primero del mes gratis, luego 3 gemas). Aviso a las 19:00 si no se cobró.
  Si el reloj del teléfono retrocede a otro mes, el calendario se queda quieto (no se cobra dos veces).
- Pendiente de Android: estas dos funciones ya existen en `:shared`, pero la app de Android todavía no tiene pantalla para ellas.

## Interfaz

- Tarjetas con relieve de juguete, brillo superior y sombra suave; botones grandes con degradado y que se hunden al tocar;
  barra de pestañas con indicador; títulos de sección con barrita.

## Batería y calentamiento

- iPhone: `PowerMonitor` mira el modo de bajo consumo, el calor del teléfono y "Reducir movimiento". Con ahorro o calor bajan los
  fotogramas y las partículas, y con mucho calor se detienen los adornos. Las animaciones también se paran con la app en segundo plano.
- Android: un solo reloj para todos los adornos (30 fotogramas por segundo, menos con ahorro de batería o calor, y parado con la app en segundo plano),
  partículas y fondos del mapa colgados de ese reloj y con menos partículas en ahorro.
- Firebase: se revisó y ya estaba bien optimizado (subida agrupada cada 45–60 s, caché de amigos, lecturas por trozos, sin `get()` en las reglas);
  no hizo falta cambiarlo.

## Paridad con Android (ronda 2)

- **Inglés:** la app sigue el idioma del teléfono. Los textos del código están en español y `en.lproj/Localizable.strings`
  (se genera con `iosApp/tools/make_localizable.py` desde `translations_en.py`) los traduce; los nombres de series y cartas también
  vienen en inglés desde `:shared`. Lo que viene de la lógica compartida solo en español (descripciones de logros y misiones) se queda en español,
  igual que en Android. Para añadir textos: `python iosApp/tools/extract_strings.py` muestra los que faltan.
- **Anuncios con premio (AdMob):** `AdManager.swift` y los botones "ver un anuncio" en: duplicar el regalo diario, giro extra de la ruleta,
  abrir ya el cofre gratis, recuperar la racha sin gemas, usar Escoba/Unir sin monedas, seguir jugando tras perder, gemas gratis (3, hasta 3 veces al día)
  e impulso de temporada. Con VIP el premio es directo, sin anuncio. Las reglas y topes diarios están en `:shared` (`MetaAds.kt`, probadas).
  **Usa los identificadores de PRUEBA de Google** (`project.yml` → `GADApplicationIdentifier` y `rewardedUnitID` en `AdManager.swift`):
  antes de publicar hay que crear la app en AdMob y cambiarlos. No hay anuncios intersticiales entre niveles (Android sí los tiene; en iPhone se dejaron fuera a propósito).
- **Música adaptativa:** la música de partida son 3 capas (base, ritmo y melodía) que entran según el fluir, los combos, el tiempo que queda y los huecos del tablero.
- **Compartir como imagen:** el resultado de una partida se comparte como tarjeta bonita (iOS 16 o más) además del texto.
- **Vibraciones finas (Core Haptics):** el cofre acelera sus toques y termina con un golpe y destellos; los logros suben una escalera de toques.

## Lo que todavía no está (necesita decisión)

- **Amigos en línea, ranking compartido, intercambios de cartas y cuenta de Google:** necesitan registrar la app de iPhone en Firebase
  (bundle `com.korkoor.pardos.ios`), bajar `GoogleService-Info.plist` y publicar las reglas de Firestore (`firestore.rules`, preparadas y sin publicar).
  Sin eso el iPhone usa amigos por tarjeta (sin servidor).
- **Compras reales:** StoreKit 2 está listo; falta crear los productos en App Store Connect (`docs/APPSTORE_PRODUCTOS.md`) y una cuenta de desarrollador de pago.

## Ronda 3

- Las 320 piezas del álbum ilustradas (ver arriba), con silueta para las que faltan.
- La tarjeta de jugador (banner, avatar, rango y marcas) se comparte como imagen desde Amigos (iOS 16 o más).
- **Serie destacada del día:** cada día una serie del álbum es la protagonista (rotan las 32 sin repetir; en Noche de brujas sale Halloween un día sí y otro no) y el primer cofre que abres ese día trae una carta extra de ella. Hay una tarjeta arriba del Álbum. Lógica probada en `:shared` (`FeaturedSeries.kt`, `MetaFeatured.kt`).
- Cada capítulo del mapa muestra un monumento grande de su mundo en la cabecera.
- **Banners completos:** los 30 patrones (volcán, ciudad neón, calabazas, fuegos artificiales, galaxia, jardín zen, farolillos, cristales…) tienen arte vectorial propio.
  Se generan con `iosApp/tools/art/packs/banners.py` (`python build.py banners`) y se revisan con colores reales del catálogo con `python bannerprev.py PATRON ...`.
- **Icono de la app a elección:** clásico (salvia, el de Android) o Noche de brujas; en Automático cambia solo en octubre. Está en Ajustes. (`AppIconManager.swift`; el de Halloween es el icono alterno `AppIconHalloween`.)
- **Botones de dirección** (Ajustes) para jugar con una mano; se muestran solos con VoiceOver o Control por botón y VoiceOver puede leer el tablero, como en Android.
- `iosApp/tools/art/vd2png.py` dibuja los vectores de Android (VectorDrawable) como PNG: sirve para reutilizar iconos de Android en iPhone.
