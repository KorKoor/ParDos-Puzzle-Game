# 🧩 Pardos: Math Zen Puzzle

> **Una reimaginación "Juicy" y moderna del género 2048.**

**Pardos** no es solo otro juego de unir números. Es una experiencia visual y táctil construida nativamente para Android con **Jetpack Compose**. Combina una estética "Toy/Jelly" con físicas de rebote, iluminación dinámica y un sistema de progresión matemática profundo.

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple?style=for-the-badge&logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=for-the-badge&logo=android)
![AdMob](https://img.shields.io/badge/Google_AdMob-Monetization-EA4335?style=for-the-badge&logo=google-ads)
![Room](https://img.shields.io/badge/Android_Room-Persistence-3DDC84?style=for-the-badge&logo=sqlite)
![KSP](https://img.shields.io/badge/KSP-Symbol_Processing-orange?style=for-the-badge)

---

## ✨ Experiencia de Usuario (UX/UI)

Lo que hace único a Pardos es su atención al detalle visual y la sensación de juego (**Game Feel**):

* **🎨 Estética "Toy & Jelly":**
    * Fichas con **efecto Gloss** (brillo especular) y sombras dinámicas que simulan botones de goma/caramelo.
    * Animaciones basadas en físicas (`spring` physics) con alto rebote para feedback satisfactorio.
    * Formas personalizadas dibujadas con `Canvas`: **Soft Octagon**, **Squircle**, Triángulos suaves, etc.
* **📳 Feedback Háptico Inmersivo:** Vibraciones sutiles al mover y fuertes al fusionar combos.
* **🔄 Adaptabilidad Total:**
    * Diseño responsivo que transiciona fluidamente entre modo **Vertical** y **Horizontal**.
    * Fondos con *Blur* dinámico en tiempo real.

## 🎮 Modos de Juego y Mecánicas

* **🏆 Campaña (Clásico):** Sistema de niveles incremental. El objetivo (meta de ficha) y el tamaño del tablero (3x3, 4x4, 5x5) escalan dinámicamente según tu progreso.
* **⚡ Desafío:** Tableros contrarreloj. Fusiona rápido para ganar segundos extra.
* **🧘 Zen:** Sin estrés, sin tiempo. Solo tú y las matemáticas.
* **✖️ Tablas:** Un modo educativo único donde practicas las tablas de multiplicar (bases x3, x4, x5...) fusionando múltiplos.

## 🛠️ Ingeniería y Arquitectura

El proyecto sigue una arquitectura **Clean Architecture + MVVM** estricta, optimizada para escalabilidad y rendimiento.

### Stack Tecnológico
* **Lenguaje:** Kotlin 2.1.0.
* **UI:** Jetpack Compose (Material 3).
* **Inyección de Dependencias:** Manual (patrón Singleton para `AdManager` y `GameEngine`).
* **Persistencia:** Room Database con **KSP** (migrado desde KAPT para compatibilidad con Kotlin 2.x).
* **Monetización:** **Google AdMob** (Formato *Rewarded Ads*) integrado nativamente para revivir y obtener Power-Ups.

### Detalles de Implementación Clave

1.  **Game Engine Personalizado:**
    * Lógica de matriz pura separada de la UI.
    * Algoritmos de fusión recursiva y detección de "Game Over" anticipada.
    * Generación procedimental de fichas basada en probabilidades dinámicas.

2.  **Gestión de Estado Reactiva:**
    * Uso intensivo de `StateFlow` y `combine` para actualizar la UI sin recomposiciones innecesarias.
    * `collectAsStateWithLifecycle` para manejo seguro de la memoria en Compose.

3.  **Sistema de Anuncios (AdManager):**
    * Implementación robusta de `RewardedAd`.
    * Callbacks para manejar la carga, visualización y recompensa de forma asíncrona.
    * Estrategia de precarga de anuncios para minimizar la latencia del usuario.

## 📂 Estructura del Proyecto

```text
com.example.pardos
├── data
│   └── local            # Room Database, DAOs y Entidades (Records).
├── domain
│   ├── logic            # GameEngine, ProgressionEngine (Matemáticas del juego).
│   ├── model            # Data Classes (BoardState, TileModel, GameMode).
│   └── achievements     # Sistema de logros desbloqueables.
├── ui
│   ├── game
│   │   ├── components   # BoardDisplay, AnimatedTile (Canvas logic).
│   │   ├── logic        # AdManager, GameTimerManager.
│   │   └── menu         # Pantallas de menú y overlays.
│   └── theme            # ThemeViewModel, Paletas de colores dinámicas.
└── MainActivity.kt      # Single Activity entry point.
🚀 Instalación y Compilación
Este proyecto utiliza Kotlin 2.1.0 y Gradle 8.x.

Clona el repositorio:

Bash
git clone [https://github.com/TuUsuario/Pardos.git](https://github.com/TuUsuario/Pardos.git)
Abre el proyecto en Android Studio Ladybug (o superior).

Importante: Asegúrate de tener configurado tu local.properties si planeas firmar la app, aunque para debug no es necesario.

Sincroniza Gradle (El proyecto usa KSP, la primera vez puede tardar un poco en generar el código de Room).

Ejecuta en un emulador o dispositivo físico.

🔮 Roadmap / Futuro
[ ] Integración de Google Play Games Services (Leaderboards en la nube).

[ ] Modo "Dark Mode" real sincronizado con el sistema.

[ ] Localización a más idiomas (actualmente ES/EN).
