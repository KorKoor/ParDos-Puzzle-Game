# Cumplimiento antes de publicar (Play Console + Firebase)

## 1. Política de privacidad ✅ publicada
URL: **https://www.korwork.org/ParDos-Puzzle-Game/** (ES + EN, incluye cómo pedir la eliminación de datos).
Fuente: rama `gh-pages` del repo. Contacto público en la página: carlosghta9999@gmail.com.
- Play Console → **Política y programas → Contenido de la app → Política de privacidad** → pegar la URL → Guardar.
- Pegar la misma URL como *URL para eliminar cuenta/datos* (Seguridad de los datos): `https://www.korwork.org/ParDos-Puzzle-Game/#eliminar-datos`.

## 2. Seguridad de los datos (Contenido de la app → Seguridad de los datos)
**¿Recopila o comparte datos?** Sí. **¿Cifrados en tránsito?** Sí. **¿Pueden los usuarios pedir que se eliminen?** Sí (URL de arriba).

| Categoría → tipo | Recopilado | Compartido | Obligatorio | Finalidad |
|---|---|---|---|---|
| Información personal → **Nombre** (apodo) | Sí | Sí (otros jugadores amigos) | Opcional (cuenta) | Funciones de la app, Personalización |
| Información personal → **Dirección de correo** (cuenta de Google) | Sí | No | Opcional | Gestión de cuentas |
| Información personal → **ID de usuario** (UID de Firebase / ANDROID_ID) | Sí | No | Obligatorio | Funciones de la app, Gestión de cuentas |
| Actividad en la app → **Interacciones con la app** (nivel, XP, rachas, estrellas, récords) | Sí | Sí (amigos) | Obligatorio | Funciones de la app |
| Info y rendimiento de la app → *no aplica* (no hay Analytics ni Crashlytics) | No | — | — | — |
| Dispositivo u otros ID → **ID de publicidad** (AdMob) | Sí | Sí (Google AdMob) | Opcional (anuncios, con consentimiento donde la ley lo exige) | Publicidad o marketing |

- Ubicación, contactos, fotos, audio, archivos, mensajes y datos financieros: **No**.
- Declara también el permiso `AD_ID` en *Contenido de la app → ID de publicidad* (**Sí**, publicidad).
- Público objetivo: la política dice "no dirigido a menores de 13". Elige *Contenido de la app → Público objetivo* acorde (13+/todos con anuncios según tu decisión; si marcas niños, aplican las reglas de Familias y AdMob).

> ✅ **Formulario ya rellenado y guardado el 8-oct-2026** (no enviado a revisión). Declarado: Nombre, Correo, ID de usuario, Interacciones en la app e ID de dispositivo/publicidad (este último compartido con AdMob). Las compras no se declaran: las gestiona Google Play Billing.

## 3. Productos de compra
Ver `docs/PLAY_PRODUCTOS.md` (8 productos con IDs, precios y descripciones). Requiere perfil de pagos activo ("Monetiza con Play → Comenzar") y haber subido una compilación con Billing a una pista de pruebas.

## 4. Huella SHA-1 de firma de Play → Firebase
1. Play Console → app → **Probar y lanzar → Configuración → Integridad de la app → Firma de apps** → copiar **SHA-1 del certificado de la clave de firma de la app** (y también el de la *clave de subida*).
2. Firebase Console → proyecto `pardos-b9b21` → **Configuración del proyecto → Tus apps → com.korkoor.pardos → Agregar huella digital** → pegar cada SHA-1 → Guardar.
3. Descargar de nuevo `google-services.json` y reemplazar `app/google-services.json` (el archivo no se sube a git).
Sin esto, "Continuar con Google" falla en la versión instalada desde Play.

## 5. AdMob ✅ hecho en código
`app/build.gradle.kts`: **debug** usa IDs de prueba de Google; **release** usa los reales
(app `ca-app-pub-3851960142449906~8749596168`, recompensado `ca-app-pub-3851960142449906/7125882091`).
Antes de publicar: en AdMob vincula la app con su ficha de Play y revisa `app-ads.txt` si lo pide.

---
## Estado verificado el 8-oct-2026 (navegador integrado)
- **ID de publicidad ✅** ya estaba declarado desde el 21-ene: *Sí*, usos "Estadísticas" y "Publicidad o marketing". Coincide con el permiso `AD_ID` del manifiesto. Sin cambios.
- **Público objetivo ✅** ya estaba en 13-15, 16-17 y mayores de 18 (sin menores de 13). Coincide con la política de privacidad. Sin cambios.
- **SHA-1 de la clave de CARGA ✅** `6C:28:27:EA:BD:58:2C:F8:A5:FD:69:C4:55:E7:18:16:BF:BE:25:42` agregada en Firebase (`pardos-b9b21` → app `com.korkoor.pardos`).
- **SHA-1 de la clave de FIRMA DE LA APP ✅** `5B:E4:C7:12:80:DE:DA:9D:17:C7:0B:F3:F8:CB:6A:8F:CB:FA:DA:53` y su **SHA-256** `EC:9E:A4:A9:F3:A5:A7:F0:D2:8D:ED:1B:EB:0F:AD:6C:D6:CA:39:59:83:03:4C:29:C1:D1:3F:2A:A3:FB:5C:B5` agregadas en Firebase (8-oct-2026). Total en Firebase: 2 SHA-1 + 1 SHA-256. (El botón de copiar de Play no funciona en el navegador integrado; el valor se leyó interceptando `navigator.clipboard.writeText`.)
- Después de agregar huellas: descargar de nuevo `google-services.json` y reemplazar `app/google-services.json` (no se sube a git).
