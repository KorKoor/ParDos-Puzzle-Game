# Productos de la App Store (iPhone)

Los precios de iPhone salen de `shared/.../domain/shop/IosStore.kt`. Apple solo permite **escalones de precio** (0,99 · 1,99 ·
2,99 · 4,99 · 6,99 · 9,99 · 19,99 · 49,99 · 99,99 dólares...), y en cada país App Store Connect convierte el escalón a la moneda
local. Por eso la escalera de gemas es distinta a la de Google Play: se añadieron **$4,99** y **$99,99**, y cada pack da más
gemas por dólar que el anterior (lo comprueba `IosStoreTest`).

Crear en App Store Connect → *Monetización → Compras dentro de la app*, con estos IDs exactos:

| ID | Nombre | Tipo | Precio | Qué da |
|---|---|---|---|---|
| `gems_tiny` | Chispa | Consumible | $0.99 | 45 gemas (**primera compra x2**) |
| `gems_small` | Puñado | Consumible | $1.99 | 100 gemas (primera compra x2) |
| `gems_pocket` | Bolsita | Consumible | $4.99 | 270 gemas (primera compra x2) |
| `gems_medium` | Bolsa — *mejor valor* | Consumible | $9.99 | 580 gemas (primera compra x2) |
| `gems_large` | Baúl | Consumible | $19.99 | 1.250 gemas (primera compra x2) |
| `gems_huge` | Tesoro | Consumible | $49.99 | 3.600 gemas (primera compra x2) |
| `gems_vault` | Bóveda | Consumible | $99.99 | 8.000 gemas (primera compra x2) |
| `starter_pack` | Pack inicial | No consumible | $2.99 | 300 gemas, 3 cofres raros, skin Cerezo, efecto Corazones |
| `season_pass` | Pase premium | Consumible (cada temporada) | $4.99 | vía premium del pase del mes |
| `vip_forever` | VIP para siempre | No consumible | $6.99 | +20 % monedas, 5 gemas diarias |
| `piggy_break` | Romper la hucha | Consumible | $2.99 | las gemas guardadas en la hucha |

## Por qué es distinto a Android

- **Sin anuncios con premio.** En iPhone lo que en Android se logra viendo un anuncio se resuelve con monedas o gemas:
  Escoba y Unir cuestan 80 monedas (`Economy.MANUAL_POWER_PRICE_COINS`), saltar la espera del cofre cuesta gemas, la ruleta
  tiene un giro gratis al día.
- **Los mismos IDs que en Google Play** para los cinco packs que ya existen, así el perfil en la nube sigue valiendo.
- Los importes son referencia en dólares; la tienda real mostrará el precio local que devuelve StoreKit.

## Estado de la versión de prueba

El `.ipa` que se instala con Sideloadly **no tiene StoreKit**: los botones de precio "compran" gratis para poder ver la tienda
llena. Para cobrar de verdad hace falta una cuenta de desarrollador de pago, crear estos productos y conectar StoreKit 2
(la lógica de entrega ya está en `MetaSession.testBuyProduct`: se cambia por la confirmación de la tienda).
