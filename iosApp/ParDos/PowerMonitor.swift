import SwiftUI
import UIKit

/// Cuánta animación se puede permitir la app ahora mismo. Se recorta con el modo de bajo consumo, cuando el teléfono se calienta,
/// cuando la app no está en primer plano o si la persona activó "Reducir movimiento". Menos animación = menos batería y menos calor.
final class PowerMonitor: ObservableObject {
    static let shared = PowerMonitor()

    enum Level: Int {
        case normal = 0
        case saver = 1
        case hot = 2
    }

    @Published private(set) var level: Level = .normal
    @Published private(set) var reduceMotion: Bool = false
    @Published private(set) var appActive: Bool = true

    private var tokens: [NSObjectProtocol] = []

    /// Ajuste de la persona: 0 automático, 1 siempre ahorrar, 2 nunca ahorrar (el calor del teléfono siempre manda).
    var mode: Int {
        return UserDefaults.standard.integer(forKey: "power_mode")
    }

    private init() {
        recompute()
        let center = NotificationCenter.default
        let names: [Notification.Name] = [
            Notification.Name.NSProcessInfoPowerStateDidChange,
            ProcessInfo.thermalStateDidChangeNotification,
            UIAccessibility.reduceMotionStatusDidChangeNotification,
            UIApplication.didBecomeActiveNotification,
            UIApplication.willResignActiveNotification,
            UIApplication.didEnterBackgroundNotification
        ]
        for name in names {
            let token = center.addObserver(forName: name, object: nil, queue: OperationQueue.main) { [weak self] _ in
                self?.recompute()
            }
            tokens.append(token)
        }
    }

    deinit {
        for token in tokens { NotificationCenter.default.removeObserver(token) }
    }

    func setMode(_ value: Int) {
        UserDefaults.standard.set(value, forKey: "power_mode")
        recompute()
    }

    func recompute() {
        let info = ProcessInfo.processInfo
        var next = Level.normal
        if mode == 1 {
            next = .saver
        } else if mode == 0 && info.isLowPowerModeEnabled {
            next = .saver
        }
        let thermal = info.thermalState
        if thermal == .serious || thermal == .critical {
            next = .hot
        }
        let motion = UIAccessibility.isReduceMotionEnabled
        let active = UIApplication.shared.applicationState == .active
        if next != level { level = next }
        if motion != reduceMotion { reduceMotion = motion }
        if active != appActive { appActive = active }
    }

    /// ¿Se pueden mover adornos que no hacen falta para jugar (fondos, brillos, avatares animados)?
    var decorativeMotion: Bool {
        return appActive && !reduceMotion && level != .hot
    }

    /// Efectos caros (halos, muchas partículas, desenfoques).
    var heavyEffects: Bool {
        return level == .normal && !reduceMotion
    }

    /// Multiplicador de cuántas partículas se dibujan.
    var particleScale: Double {
        if reduceMotion { return 0.2 }
        switch level {
        case .normal: return 1.0
        case .saver: return 0.5
        case .hot: return 0.15
        }
    }

    /// Segundos mínimos entre fotogramas de las animaciones decorativas continuas.
    var frameInterval: Double {
        switch level {
        case .normal: return 1.0 / 30.0
        case .saver: return 1.0 / 15.0
        case .hot: return 1.0 / 8.0
        }
    }

    var label: String {
        switch level {
        case .normal: return "Normal"
        case .saver: return "Ahorro"
        case .hot: return "Teléfono caliente"
        }
    }
}
