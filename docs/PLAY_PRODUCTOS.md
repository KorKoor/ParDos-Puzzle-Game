# Productos de Play Console (ParDos · com.korkoor.pardos)

Play Console → *Monetizar con Play → Productos → Productos integrados en la aplicación* → **Crear producto**.
Los IDs deben ser **exactamente** estos (el código los usa en `ShopCatalog`). Precios en USD aprobados el 2026-10-07; Play
los convierte a cada país. Límites de Play: nombre ≤ 55 caracteres, descripción ≤ 80.

| ID del producto | Nombre | Descripción | Precio | Cómo lo trata la app |
|---|---|---|---|---|
| `gems_small` | 100 gemas | Un puñado de gemas para cofres y extras. | $1.99 | consumible (suma 100 gemas) |
| `gems_medium` | 550 gemas | Más gemas por tu dinero: ideal para cofres raros. | $9.99 | consumible (550) |
| `gems_large` | 1200 gemas | El mejor valor en gemas para completar tu álbum. | $19.99 | consumible (1200) |
| `vip_forever` | VIP para siempre | Poderes sin anuncios y ventajas VIP, una sola vez. | $6.99 | única vez |
| `starter_pack` | Pack inicial | 300 gemas, 3 cofres raros y la skin Cerezo. Solo una vez. | $2.99 | única vez |
| `skin_studio` | Studio: diseña tu skin | Crea tus propias fichas: colores, acabado, fondo y partículas. | $3.99 | única vez |
| `season_pass` | Pase de temporada premium | Doble de premios y la skin exclusiva de la temporada. | $4.99 | consumible (se compra cada temporada) |
| `piggy_break` | Romper la hucha | Recibe todas las gemas guardadas en tu hucha. | $2.99 | consumible |

Pasos por producto: ID → Nombre → Descripción → *Establecer precio* (USD) → Guardar → **Activar**.
Después: subir una compilación a una pista de pruebas (interna) y añadir tu cuenta como probador de licencias para poder
comprar en pruebas (las compras de prueba no cuestan dinero).
