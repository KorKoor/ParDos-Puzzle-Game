# Ficha de Google Play — guía rápida

Todo lo de esta carpeta está listo para usarse. **Los textos (español e inglés) ya están guardados como BORRADOR en Play Console**;
solo falta subir las imágenes y enviar los cambios a revisión.

## 1. Subir las imágenes (2 minutos)

En Play Console: **Aumenta la cantidad de usuarios → Presencia en Play Store → Fichas de Play Store → Editar ficha predeterminada**
(idioma *Español (Latinoamérica)*). Arrastra los archivos de `graficos-halloween/` (hasta el 2 de noviembre) a cada hueco:

| Hueco de Play Console | Archivo | Medida |
|---|---|---|
| Ícono de la app | `icono_512.png` | 512 × 512 |
| Gráfico de funciones | `grafico_funciones_1024x500.png` | 1024 × 500 |
| Capturas de pantalla de teléfono (borra las 7 viejas y sube estas 8, en este orden) | `01_tablero.png` … `08_perfil.png` | 1080 × 1920 |
| (opcionales, para reemplazar las más flojas) | `09_extra_inicio.png`, `10_extra_ruleta.png` | 1080 × 1920 |

El orden importa: las **3 primeras capturas** son las que ve casi todo el mundo en los resultados de búsqueda
(tablero → campaña → skins).

Después de Halloween (3 de noviembre) cambia a `graficos-siempre/` (mismo orden y mismos nombres): son los mismos gráficos con la paleta
de siempre. El ícono de la tienda de esa versión es el clásico que ya tienes subido (no hace falta tocarlo si prefieres volver a él).

## 2. Enviar a revisión
Panel → **Descripción general de la publicación** → *Enviar los cambios para revisión*. Las fichas suelen aprobarse en pocas horas
(a veces 1–3 días).

## 3. Cosas importantes ANTES de publicar la nueva versión
- **Seguridad de los datos** (Contenido de la app → Seguridad de los datos) y **política de privacidad**: la ficha vieja decía
  "tu progreso se guarda en tu dispositivo, sin recopilar datos personales". Eso **ya no es cierto**: ahora hay inicio de sesión con Google,
  Firebase (cuenta, perfil, amigos, ranking), AdMob (ID de publicidad) y Google Play Billing. Hay que actualizar ambos o Google puede
  rechazar la versión.
- **Compras integradas**: en el panel aparece "Monetiza con Play → Comenzar": todavía no hay productos creados. La lista está en
  `docs/PLAY_PRODUCTOS.md`. La ficha dice "compras opcionales dentro de la app": se vuelve cierto en cuanto esa versión salga con productos.
- **AdMob**: antes de publicar, cambia los IDs de prueba por los reales (ver `docs/UPDATE_V3.md`).
- **Firebase**: agrega la huella SHA-1 de firma de Play a la app `com.korkoor.pardos` para que el inicio de sesión con Google funcione
  en la versión de la tienda.

## 4. Archivos
- `LISTADO_ES.md` / `LISTADO_EN.md`: los textos exactos (con conteo de caracteres).
- `ASO_Y_MARKETING.md`: palabras clave, plan de lanzamiento y cosas que probar.
- `graficos-halloween/` y `graficos-siempre/`: imágenes listas.

## 5. Video promocional
`video/ParDos_trailer_1080p.mp4` (1920×1080, 48 s, con música original sintetizada, sin derechos de autor). Usa gameplay real grabado en el teléfono.
Súbelo a YouTube (puede ser "No listado") y pega el enlace en Play Console → Ficha → **Video promocional**. Debe ser público o no listado, sin restricción de edad y con la monetización desactivada.

## 6. Capturas de tablet (7" y 10")
`graficos-halloween/tablet/` y `graficos-siempre/tablet/`: 8 capturas de 1080×1920 (9:16) hechas con la app real corriendo en pantalla de tablet.
En Play Console → Ficha → **Capturas de pantalla de tablet de 7 pulgadas** y **de 10 pulgadas**: sube las mismas 8 en ambos huecos (mismo orden: 01 → 08).
