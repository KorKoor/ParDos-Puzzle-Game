# 🧩 Pardos: Math Zen Puzzle
Una reimaginación "Juicy" y moderna del género 2048 con infraestructura en la nube.

Pardos es una experiencia táctil de alto rendimiento construida nativamente para Android. No solo desafía la agilidad matemática, sino que ofrece un ecosistema completo de progresión, competitividad social y persistencia de datos distribuida.

✨ Experiencia de Usuario (UX/UI) & Game Feel
Pardos aplica principios de Juice It or Lose It para maximizar el feedback sensorial:

🎨 Estética "Toy & Jelly": Fichas con iluminación especular, sombras dinámicas y animaciones basadas en físicas de resortes (spring physics).

🧩 Geometría Dinámica: Formas personalizadas dibujadas mediante Low-level Canvas (Soft Octagon, Squircle, Hexágonos).

📳 Sistema Háptico: Vibración de intensidad variable según la magnitud de la fusión y combos.

🌓 Adaptabilidad: Soporte nativo para orientación Landscape y Portrait con layouts diferenciados.

🎮 Modos de Juego y Mecánicas
🏆 Campaña: Más de 100 niveles con escalado procedimental de dificultad y tamaño de grid (3x3 hasta 6x6).

⚡ Desafío: Mecánica de "Time Attack" donde las fusiones exitosas inyectan segundos al reloj.

🧘 Zen: Modo "Infinito" optimizado para reducir la carga cognitiva.

✖️ Tablas: Gamificación educativa para el aprendizaje de tablas de multiplicar mediante fusiones de múltiplos.

🛠️ Ingeniería y Arquitectura
El proyecto implementa una arquitectura Clean Architecture + MVVM desacoplada, facilitando el testing y la mantenibilidad.

Stack Tecnológico Avanzado
Persistencia Híbrida: Uso de Room (KSP) para caché local de récords y Firebase Firestore para el perfil global.

Cloud Sync: Sistema de rescate de datos que sincroniza el progreso del usuario mediante el ID único del dispositivo, permitiendo la recuperación tras reinstalación.

Social Engine: Sistema de "Círculo Zen" (Amigos) mediante códigos únicos de invitación y Ranking en tiempo real.

Optimización R8: Configuración personalizada de reglas de ProGuard para ofuscación de código sin romper la serialización de datos de Firebase.

Gestión de Estado y Rendimiento
Reactividad: Flujos de datos asíncronos con StateFlow y gestión de ciclo de vida con collectAsStateWithLifecycle.

SoundPool Engine: Implementación de baja latencia para efectos de sonido, optimizando el uso de memoria RAM frente a MediaPlayer.

Ad Management: Estrategia de precarga (Pre-loading) de anuncios premiados para evitar interrupciones en el flujo de juego.

📂 Estructura del Proyecto
Plaintext
com.korkoor.pardos
├── data
│   ├── local            # Room DB, ProfileManager (SharedPreferences).
│   └── remote           # Firebase Firestore integracion.
├── domain
│   ├── logic            # GameEngine, ProgressionEngine (Lógica matemática).
│   ├── model            # Modelos @Keep (UserProfile, Record, TileModel).
│   └── achievements     # Sistema de detección de hitos.
├── ui
│   ├── game             # Pantallas de juego, ViewModels y animaciones.
│   ├── profile          # Gestión de Amigos, Ranking y Perfil Zen.
│   └── theme            # Sistema de temas dinámicos y paletas.
└── notifications        # ZenNotificationManager (Retención de usuarios).
🚀 Instalación y Compilación
Clona el repositorio: git clone https://github.com/TuUsuario/Pardos.git

Agrega tu archivo google-services.json en la carpeta /app.

Abre en Android Studio Ladybug o superior.

Sincroniza Gradle y ejecuta (Requiere soporte para KSP).~~~~