# Animación de la mega actualización de Halloween (Lottie)

`halloween_mega_update.json` — animación vectorial en formato **Lottie** (JSON), **1920×1080 (16:9), 60 fps**, 180 cuadros (3 s) en bucle sin costura y **menos de 200 KB** (hoy ~75 KB). El generador falla si se sale de esas medidas.
Sin imágenes ni emojis. Lleva fondo nocturno propio (no queda transparente ni blanco) y el título PARDOS dibujado con trazos (no usa fuentes, así se ve igual en cualquier visor).

Escena: luna con halo que respira, tres murciélagos que vuelan en bucle aleteando, calabaza que flota con la cara encendida (luz que parpadea),
telarañas en las esquinas, estrellas que titilan y una colina con lápidas. Colores del evento: morado noche, naranja calabaza y dorado.

## Cómo usarla
- **Android (Jetpack Compose):** añadir `com.airbnb.android:lottie-compose` y `LottieAnimation(composition, iterations = LottieConstants.IterateForever)`
  con `rememberLottieComposition(LottieCompositionSpec.Asset("halloween_mega_update.json"))` (copiar el archivo a `app/src/main/assets/`).
- **iPhone:** paquete `lottie-ios` con `LottieAnimationView(name:)`.
- **Web / pruebas:** `lottie-web` o https://lottiefiles.com/preview.

Se regenera con `python tools/lottie/make_halloween.py` (los colores, tiempos y posiciones están arriba del script).

## Imagen principal

`imagen_principal_halloween.jpg` (y `.png`) — imagen fija 1920×1080 (16:9) sacada de la animación (cuadro 48, con las alas abiertas). Lo importante (título PARDOS, calabaza y luna) está dentro de la zona segura; las telarañas de las esquinas son solo decoración. Se regenera renderizando ese cuadro con lottie-web a 1920×1080.
