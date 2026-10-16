# Publicar ParDos 3.0 (versionCode 23)

## 1. Firmar y generar el paquete
La clave de subida **nunca** va en el repositorio. Pon esto en `C:\Users\carlo\.gradle\gradle.properties` (fuera del proyecto):

```
pardos.storeFile=C:/ruta/a/tu-clave-de-subida.jks
pardos.storePassword=...
pardos.keyAlias=...
pardos.keyPassword=...
```

Luego:

```
./gradlew :shared:testDebugUnitTest :app:testDebugUnitTest :app:bundleRelease
```

El paquete queda en `app/build/outputs/bundle/release/app-release.aab`. Comprueba la firma con
`keytool -printcert -jarfile app-release.aab`: la huella SHA-1 debe ser
`25:20:FC:F3:99:B3:DB:D1:9B:D0:C8:1F:67:AB:8C:32:92:05:C4:26` (la clave de subida registrada en Play Console).
Si no coincide, Play rechazará el paquete.

## 2. Avisar a quien no está actualizado
La app ya usa la API oficial de actualizaciones de Google Play (`InAppUpdate`):
- **Flexible** (por defecto): ventana de Play, se descarga jugando y se instala al terminar; se ofrece como mucho una vez al día.
- **Inmediata**: si al crear la versión en Play Console pones **Prioridad de actualización 4 o 5**, aparece a pantalla completa y hay que actualizar para seguir.

Solo funciona con la app instalada desde Play (en pruebas internas o producción). Recomendado: prioridad 2 para esta versión; sube a 4 solo si algo crítico lo exige.
Importante: los usuarios que ya tengan una versión **anterior a esta** no tienen este código, así que a ellos les avisa Play con su actualización automática normal; desde la 3.0 en adelante, la app misma avisa.

## 3. Reseñas
Tras **25 minutos reales de juego** (sumando sesiones), más nivel 12, 2 días desde la instalación y un momento feliz (jefe vencido o 3 estrellas en racha), se pide una reseña con la ventana oficial de Google Play. Nunca junto a un anuncio, nunca al perder, máximo 3 veces y 60 días entre cada una.

## 4. Tamaño de la interfaz
El tamaño de los elementos del juego viene al **80 %** por defecto (se puede cambiar en Ajustes entre 80 % y 110 %). Quien ya lo cambió a mano conserva su valor.

## 5. Notas de la versión
**Español**
```
ParDos 3.0
• Nuevos jefes, guardianes y un bestiario para coleccionar
• Calma, Frenesí y el reto semanal de jefes
• Misiones secundarias y ofertas de tiempo limitado, sin trucos
• Interfaz más limpia y de aspecto más elegante, con tamaño ajustado al 80 %
• Arreglo: ahora siempre puedes salir de un nivel
• Mejoras de rendimiento y estabilidad
```
**English**
```
ParDos 3.0
• New bosses, guardians and a bestiary to collect
• Calm, Frenzy and the weekly boss challenge
• Side quests and honest limited-time offers
• Cleaner, more elegant interface, sized at 80% by default
• Fix: you can always leave a level now
• Performance and stability improvements
```

## 6. Lista de comprobación
- [ ] `versionCode` 23 / `versionName` 3.0 (hecho)
- [ ] Tests en verde (hecho)
- [ ] `.aab` firmado con la clave de subida y huella verificada
- [ ] Prueba interna en Play Console antes de producción (ver que sale el aviso de actualización en un teléfono con la 2.0)
- [ ] Notas de la versión pegadas (arriba) y prioridad de actualización elegida
- [ ] Publicar el proyecto de Play Games con los logros (`tools/pgs/out/logros.zip`)
- [ ] Despliegue gradual: 10 % → 50 % → 100 % mirando fallos en Android vitals
