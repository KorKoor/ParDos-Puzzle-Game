import SwiftUI
#if canImport(FirebaseCore)
import FirebaseCore
#endif

/// Arranque de Firebase. Se inicia una sola vez al abrir la app.
/// La configuración (`GoogleService-Info.plist`) no está en el repositorio: se pone al compilar.
/// Si falta o el paquete no está, la app funciona igual, solo sin la parte en línea.
enum CloudBootstrap {
    private(set) static var isReady = false

    static func start() {
        #if canImport(FirebaseCore)
        if isReady { return }
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return }
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        isReady = FirebaseApp.app() != nil
        #endif
    }
}

/// Lo que pide Firebase: iniciarlo cuando la app termina de abrir.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        CloudBootstrap.start()
        return true
    }
}
