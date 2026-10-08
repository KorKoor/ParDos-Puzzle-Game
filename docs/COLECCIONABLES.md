# Coleccionables, repetidas e intercambio

El álbum pasó de 72 a **320 cartas únicas** en **32 series de 10**. Todo es dato en `shared` (`Catalog.kt`, `AlbumEconomy.kt`) con tests.

## Las cartas
- Cada carta es única: su propio emoji de arte, nombre en español e inglés, **descripción**, número dentro de la serie y un tono de fondo propio.
- Cada serie tiene paleta, motivo animado de fondo (hojas, olas, estrellas, brasas, lluvia, corazones, confeti, cristales…) y una **mejora** temática.
- Rareza por posición: 4 comunes, 3 raras, 2 épicas y 1 legendaria por serie (128 / 96 / 64 / 32 en total).
- El marco cambia con la rareza (beige, azul, morado con destellos y dorado giratorio para las legendarias). Las Brillantes llevan un barrido holográfico.
- Sin tenerla se ve una silueta misteriosa con su "?".
- Si un teléfono antiguo no sabe dibujar un emoji, la carta usa el de su serie (nunca sale un cuadrito vacío).
- Las 72 piezas anteriores conservan sus ids; cada serie vieja ganó 2 piezas nuevas (9 y 10).

## Repetidas
- Las cartas repetidas ya **no se convierten solas en esencia**: se guardan como copias. La primera copia queda en el álbum y nunca se puede vender ni cambiar.
- Con una repetida puedes: **vender** (común 12, rara 40, épica 130, legendaria 420 monedas), **reciclar** en esencia, o **cambiarla** con un amigo.
- Pestaña "Repetidas": valor total, venta masiva (comunes, o comunes y raras) y las cartas con sus copias.
- Con 320 cartas aparecen más repetidas (60 % de preferencia por una carta nueva, antes 75 %), y de ahí sale el valor de intercambiar.

## Intercambio con amigos
- Se paga con **fichas de intercambio**: común 1 · rara 2 · épica 4 · legendaria 8. Solo se cambia por una carta de la misma rareza.
- Cómo ganar fichas: 1 al día al entrar, 1 con las tres misiones del día, 1 por misión semanal, 3 al subir de rango, 10 con el Platino, premios del Pase y mejoras de las cartas. También 1 por 15 gemas (máx. 3 al día).
- Tu perfil publica qué cartas tienes y cuáles te sobran (80 letras hexadecimales, un bit por carta). La app calcula los cambios que les convienen a los dos y los sugiere primero por rareza.
- La propuesta aparta tu carta y gasta las fichas al enviarla. Si la rechazan, la cancelas o caduca (7 días), recuperas las dos cosas. Quien acepta entrega una repetida suya y se queda la tuya. Máximo 5 propuestas pendientes.
- Va por Firestore (`trades/{id}`). **Hay que publicar las reglas nuevas**: `firebase deploy --only firestore:rules --project pardos-b9b21`.

## Mejoras por rareza (Épicas y Legendarias)
Cada carta épica y legendaria da una mejora permanente según el tipo de su serie; la versión Brillante la duplica.

| Mejora | Épica | Legendaria | Tope |
|---|---|---|---|
| Monedas | +0,3 % | +0,6 % | 15 % |
| Experiencia | +0,5 % | +1 % | 20 % |
| Venta de repetidas | +1 % | +2 % | 40 % |
| Carta extra en cofres (suerte) | +0,5 % | +1 % | 25 % |
| Ficha extra al día | +2 % | +4 % | 50 % |

Las mejoras de monedas suman al bono del álbum (ahora tope 60 %). El bono de piezas y brillantes se reajustó para el álbum grande: +1 % cada 40 piezas, +1 % por serie completa, +8 % por el álbum entero, +1 % cada 16 brillantes.

## Vitrina del perfil
Hasta 9 cartas exhibidas (3 al inicio; los huecos 4 al 9 cuestan 30, 50, 80, 120, 160 y 220 gemas). Tus amigos ven las tres primeras en su lista de Amigos.

## Más cosas en qué gastar (sin trampas)
- **Sobre de serie**: 3 cartas de la serie que tú elijas por 900 monedas o 45 gemas; si te falta alguna, siempre trae al menos una nueva.
- Fichas de intercambio con gemas, hasta 3 al día.
- Huecos de vitrina, esencia, brillantes, títulos, avatares y banners.
- Las probabilidades de los cofres no cambian y son las de siempre; no hay temporizadores ni escasez inventada.

## Pantalla del Álbum
Cabecera con anillo de progreso, bono de monedas desglosado y mejoras activas; tarjeta de **pistas** ("Te falta 1 carta para cerrar Mesa de Té") con sobre de serie, crear con esencia o pedirla a un amigo; filtros (todas, casi listas, incompletas, completas); detalle con descripción, mejora y acciones; apertura de cofres con las repetidas guardadas.

## Revisión visual
En builds de depuración: `adb shell am start -n com.korkoor.pardos.debug/com.korkoor.pardos.MainActivity --ei debug_gallery 3 --ei debug_page N` (12 cartas por página).
