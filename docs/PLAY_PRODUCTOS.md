# Productos de Play Console (ParDos · com.korkoor.pardos)

Play Console → *Monetizar con Play → Productos → Productos integrados en la aplicación* → **Crear producto**.
Los IDs deben ser **exactamente** estos (el código los usa en `ShopCatalog`). Precios en USD aprobados el 2026-10-07; Play
los convierte a cada país. Límites de Play: nombre ≤ 55 caracteres, descripción ≤ 80.

| ID del producto | Nombre | Descripción | Precio | Cómo lo trata la app |
|---|---|---|---|---|
| `gems_tiny` | 45 gemas | Una chispa de gemas para un capricho. | $0.99 | consumible (45; **primera compra x2 = 90**) |
| `gems_small` | 100 gemas | Un puñado de gemas para cofres y extras. | $1.99 | consumible (100; primera compra x2) |
| `gems_medium` | 550 gemas | Más gemas por tu dinero: ideal para cofres raros. | $9.99 | consumible (550; primera compra x2) |
| `gems_large` | 1200 gemas | El mejor valor en gemas para completar tu álbum. | $19.99 | consumible (1200; primera compra x2) |
| `gems_huge` | 3500 gemas | El tesoro completo: el mayor ahorro en gemas. | $49.99 | consumible (3500; primera compra x2) |
| `vip_forever` | VIP para siempre | Poderes sin anuncios y ventajas VIP, una sola vez. | $6.99 | única vez |
| `starter_pack` | Pack inicial | 300 gemas, 3 cofres raros, skin Cerezo y efecto Corazones. Una vez. | $2.99 | única vez |
| `skin_studio` | Studio: diseña tu skin | Crea tus propias fichas: colores, acabado, fondo y partículas. | $3.99 | única vez |
| `season_pass` | Pase de temporada premium | Doble de premios y la skin exclusiva de la temporada. | $4.99 | consumible (se compra cada temporada) |
| `piggy_break` | Romper la hucha | Recibe todas las gemas guardadas en tu hucha. | $2.99 | consumible |

Pasos por producto: ID → Nombre → Descripción → *Establecer precio* (USD) → Guardar → **Activar**.
Después: subir una compilación a una pista de pruebas (interna) y añadir tu cuenta como probador de licencias para poder
comprar en pruebas (las compras de prueba no cuestan dinero).

**Notas de la actualización de monetización (2026-10-08)**
- Hay **10 productos** (antes 8): se añaden `gems_tiny` ($0.99) y `gems_huge` ($49.99).
- La **primera compra de cada pack de gemas da el doble** (lo decide la app; en Play el producto es el mismo).
- Gemas por dólar: 45 / 50 / 55 / 60 / 70 → los packs grandes dan hasta +54 % extra (la tienda lo muestra).
- Los precios en la app salen de Play: mientras un producto no exista en Play Console, su tarjeta dice "Pronto".
