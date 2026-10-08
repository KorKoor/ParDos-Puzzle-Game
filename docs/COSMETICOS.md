# Cosméticos: avatares, banners y Pase

Todo se dibuja con Canvas dentro de la app (sin imágenes descargadas): no pesa en el APK, no tiene problemas de licencia y se anima.
El catálogo son datos en `shared` (`Avatars.kt`, `Banners.kt`, `SeasonPass.kt`); añadir uno = añadir una línea.

## Avatares: 114 (antes 44)
- 10 clásicos (imágenes) + 71 de tienda + 10 + 15 exclusivos del Pase + 8 de prestigio.
- 13 animales nuevos: cerdito, monito, hámster, ciervo, vaca, patito, murciélago, fantasma, calabaza, robot, alienígena, dino y fénix.
- 12 accesorios nuevos: bruja, pirata, chef, Santa, diablillo, halo, ninja, chistera, gorra, gafas de sol, capa y gorro de fiesta.
- 7 variantes de color nuevas: sakura, hielo, brasa, sombra, caramelo, galaxia y platino.
- 13 escenas de fondo animadas: destellos, rayos, estrellas, nubes, corazones, nieve, noche de brujas, pétalos, burbujas, confeti, aurora y llamas.
- Todos los animales ya tenían brillo y sombra de barbilla propios; los ojos llevan doble destello.
- Marco **Mítico** (arcoíris que gira rápido) para los avatares de prestigio.
- Rareza visible (común, raro, épico, legendario) según cómo se consiguen.
- Colección "Noche de brujas": 11 avatares en la tienda con su filtro propio.

## Banners: 61 (antes 21)
- 16 patrones nuevos: murciélagos, calabazas, montañas, bosque, nubes, corazones, fuegos artificiales, galaxia, jardín zen, caramelo, circuito, farolillos, confeti, lluvia (con tormenta), atardecer en el mar y cristales.
- 20 nuevos en la tienda (10 con monedas, 10 con gemas), 12 exclusivos del Pase y 8 de prestigio.

## Pase de temporada (30 niveles)
- Premios más grandes: gratis 25+4×nivel monedas (antes 20+3×), premium 60+9×nivel (antes 40+6×); más gemas, más deshacer, y cofres épicos premium en los niveles 10, 20, 25 y 30.
- Cosméticos por temporada: **7 avatares y 7 banners** (antes 2 y 2): gratis 3 avatares y 2 banners; premium 4 avatares y 5 banners, más la skin y el efecto exclusivos.
- Los extras rotan cada mes entre 3 sets, así que el Pase no se repite durante 3 meses.
- "Premios estrella" ahora lista todo lo especial del mes, y la tarjeta premium enseña los avatares y banners que desbloquea.

## Prestigio
Cada rango desbloquea un avatar y un banner (y el Platino, uno más). Se conceden solos y salen en la celebración de subida de rango.

## Selectores
Filtros (Todos, Tuyos, Tienda, Halloween, Pase, Prestigio, Clásicos), contador de colección (`29 / 114`), rareza, y qué hace falta para conseguir cada uno.

## Revisión visual
En builds de depuración: `adb shell am start -n com.korkoor.pardos.debug/com.korkoor.pardos.MainActivity --ei debug_gallery 1 --ei debug_page 0`
(1 = avatares, 20 por página · 2 = banners, 7 por página).
