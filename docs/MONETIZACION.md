# Monetización de ParDos

Principio: **ganar dinero sin espantar a quien juega**. Los anuncios que el jugador *pide* (con premio) son la fuente principal; los de pantalla completa son pocos, solo en pausas naturales y se pueden quitar para siempre con el VIP. Nada de cuentas atrás falsas, falsa escasez ni botones que parecen otra cosa.

## 1. Anuncios

### Con premio (los pide el jugador) — AdMob rewarded
| Dónde | Premio | Tope diario | VIP |
|---|---|---|---|
| Resumen de victoria → **DUPLICAR MONEDAS** (botón grande violeta que respira, con lo que se gana escrito) | + las monedas ganadas (máx. 300) | una vez por partida | gratis, sin anuncio |
| Al perder → **CONTINUAR** | segunda oportunidad (o movimientos extra) | una por partida | gratis |
| Tienda (Destacado y Gemas) y menú → **GEMAS GRATIS** | 3 ◆ | 3 al día (9 ◆ máx.; el VIP ya recibe sus 5 ◆ diarias) | no se ofrece |
| Al abrir un cofre → **UNA CARTA MÁS** | una carta con las probabilidades de ese cofre (sin garantías) | 3 al día | sin anuncio, con el mismo tope |
| Álbum → Intercambio → **FICHA GRATIS** | 1 ficha de intercambio | 2 al día | sin anuncio, con el mismo tope |
| Pase de temporada → **IMPULSO DEL PASE** | 40 puntos de pase | 2 al día | sin anuncio, con el mismo tope |
| En la partida → **DESHACER** (cuando ya no te quedan, justo después de una jugada) | deshacer la última jugada, sin gastar tus "Deshacer" | 2 por nivel | sin anuncio, con el mismo tope |
| Ruleta, cofre gratis, reparar racha, poderes manuales | (ya existían) | (los suyos) | gratis |

El VIP se salta el anuncio, **no el tope diario** (así la carta extra, la ficha y el impulso del pase no se pueden encadenar sin límite). Todos usan el mismo botón (`WatchAdButton`): muestra una ruedita mientras el anuncio se prepara y, si se toca antes de tiempo, avisa con un mensaje claro ("El anuncio todavía no está listo…") en vez de no hacer nada. Si no hay anuncio por falta de conexión o de oferta, se reintenta solo con espera creciente (4 s → 60 s).

### De pantalla completa (intersticiales) — AdMob interstitial
Reglas en `shared/.../domain/shop/AdPolicy.kt` (con pruebas en `AdPolicyTest`):

| Regla | Valor |
|---|---|
| Solo **tras ganar** un nivel de **campaña** y al pulsar SIGUIENTE (nunca tras perder, nunca en Torre/diario/modos sueltos) | — |
| Nivel mínimo de campaña | 8 |
| Antigüedad mínima de la instalación | 20 min |
| Victorias entre un anuncio y el siguiente | 3 |
| Separación mínima entre cualquier anuncio | 3 min |
| Tras ver un anuncio con premio | 4 min de calma |
| Máximo por día | 6 |
| Con **VIP** | nunca |
| Si toca anuncio | se apaga el avance automático (el jugador pasa tocando SIGUIENTE; nada aparece solo) |
| Si no hay anuncio cargado | el juego sigue al momento (jamás bloquea) |

Para ajustar la frecuencia basta cambiar esas constantes (`AdPolicy.WINS_BETWEEN`, `MIN_GAP_MS`, `MAX_PER_DAY`…). Cualquier cambio queda cubierto por las pruebas.

### Consentimiento y privacidad (UMP)
- Al abrir la app se consulta el SDK de mensajes de usuario de Google (UMP). Donde la ley lo exige (Espacio Económico Europeo, Reino Unido y algunos estados de EE. UU.) se muestra el formulario **antes** de pedir anuncios; en el resto no aparece nada.
- **Ajustes → Privacidad**: enlace a la política y, cuando corresponde, "Anuncios y privacidad" para cambiar la decisión.
- Los anuncios solo se piden cuando el consentimiento lo permite.

## 2. Compras dentro de la app
Catálogo y precios de referencia: `docs/PLAY_PRODUCTOS.md` (8 productos).

| Producto | Qué recibe | Cómo se le enseña |
|---|---|---|
| **VIP (para siempre)** | sin anuncios (ni con premio ni de pantalla completa), +20 % de monedas, 5 ◆ al día, poderes y x2 gratis | Tarjeta del menú (desde el nivel 15), tienda → Destacado |
| **Pack inicial** (una vez) | 300 ◆ + 3 cofres raros + skin Cerezo + efecto Corazones | Tarjeta del menú (desde el nivel 4, hasta que lo compra), tienda |
| **Packs de gemas** (45 / 100 / 550 / 1200 / 3500) | gemas; la **primera compra de cada pack da el doble** | Tienda → Gemas, y **al perder sin gemas suficientes**: "te faltan N ◆ · pack pequeño por $X" sin salir de la partida |
| **Pase de temporada premium** | vía premium del mes con skin exclusiva | Menú (pase), tienda, pantalla del pase |
| **Hucha** | rompe y entrega las gemas acumuladas | Menú, tienda |
| **Studio** | editor de skin propia | Tienda |

### Tarjeta de ofertas del menú (`PromoStrip`)
Una sola tarjeta que **rota en cada visita** entre: pack inicial · VIP · oferta del día · gemas gratis. Lo que el jugador ya tiene no se le vuelve a ofrecer (pack comprado, VIP activo, skin que ya posee: la oferta del día elige otra, ver `DailyOffers.forDayAvoiding`). Lógica pura y probada en `MenuPromo` (shared).

### Al perder (segunda oportunidad)
Tres vías, todas visibles: **anuncio** (gratis), **12 ◆** (`Economy.REVIVE_PRICE_GEMS`, mismo precio que en iPhone) o, si faltan gemas, **comprar el pack pequeño** desde ahí mismo. VIP continúa sin anuncio.

### Reseñas en Google Play
`RatePolicy` (shared, con pruebas) + `RatePrompt` (app): al pulsar SIGUIENTE tras una victoria **buena** —3 estrellas con 3 victorias seguidas, o un jefe vencido— se pide una reseña con la ventana nativa de Play (`com.google.android.play:review`). Condiciones: nivel de campaña ≥ 12, al menos 2 días desde la instalación, 60 días entre peticiones, máximo 3 en total y **nunca pegada a un anuncio**. Play tiene además su propio tope, así que a veces no enseña la ventana (se cuenta igualmente como pedida). Sin preguntas previas del tipo "¿te gusta el juego?": lo prohíbe la política de Play.

### Oferta del día fija
La oferta de la tienda y la del menú son **la misma y no cambian a mitad del día**: la primera vez que se pide cada día se elige (sin repetir una skin que ya tienes) y se guarda (`DailyOfferStore`). Así comprar la skin del día no hace aparecer otra con descuento en cadena; si ya la compró, la tarjeta del menú deja de enseñarla.

## 3. Lo que tienes que hacer tú (consolas)

1. **AdMob → Aplicaciones → ParDos → Bloques de anuncios → Añadir bloque de anuncios → Intersticial.** Copia su ID (`ca-app-pub-3851960142449906/XXXXXXXXXX`) y pégalo en `gradle.properties`:
   ```
   pardos.interstitialAdUnitId=ca-app-pub-3851960142449906/XXXXXXXXXX
   ```
   Mientras esté vacío, la versión de lanzamiento **no muestra intersticiales** (la de depuración usa los de prueba de Google).
2. **AdMob → Privacidad y mensajes → Mensaje de consentimiento (GDPR)** y, si quieres, **Opciones de privacidad de EE. UU.**: crea el mensaje para la app y publícalo. Sin mensaje publicado, el formulario no aparece (en la UE no se mostrarán anuncios personalizados).
3. **Play Console → Contenido de la app → Seguridad de los datos**: ya declaraba "ID de publicidad → compartido con AdMob". No cambia; solo conviene revisar que diga "anuncios" (no solo "con premio").
4. **Play Console → Productos** (si aún no): crear los 8 productos de `docs/PLAY_PRODUCTOS.md` con los mismos IDs.
5. Antes de publicar: probar en un dispositivo con la versión `release` instalada desde la pista interna, **sin tocar nunca los anuncios reales** (AdMob puede suspender la cuenta por clics propios). En depuración se usan siempre los anuncios de prueba.

## 4. Cómo probar
- Depuración: anuncios de prueba de Google; las ofertas y los topes diarios se guardan en `pardos_ads` (SharedPreferences).
- Forzar que toque un intersticial: borrar `pardos_ads` (`adb shell run-as com.korkoor.pardos.debug rm shared_prefs/pardos_ads.xml`) pone la antigüedad a 0 → hay que esperar 20 min; para probarlo ya, bajar temporalmente `AdPolicy.MIN_INSTALL_AGE_MS`.
- Formulario de consentimiento (UE): añadir `ConsentDebugSettings` con `DEBUG_GEOGRAPHY_EEA` y el ID del dispositivo (el SDK lo imprime en logcat).

## 5. Ideas para después (no hechas)
- Pase "sin anuncios" más barato que el VIP (solo quita intersticiales).
- Ofertas por temporada (p. ej. paquete de Navidad) reutilizando `MenuPromo`.
- Mediación de anuncios (AppLovin/Meta) cuando haya volumen, para subir el eCPM.
- Medir conversión de la tarjeta del menú (hoy no hay analítica, por privacidad).
