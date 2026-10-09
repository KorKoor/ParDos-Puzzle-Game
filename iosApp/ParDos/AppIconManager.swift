import UIKit

/// Icono de la app: el clásico (verde salvia) casi todo el año y el de Noche de brujas en octubre. Se puede fijar en Ajustes.
enum AppIconManager {
    /// "auto", "classic" o "halloween".
    static var preference: String {
        return UserDefaults.standard.string(forKey: "icon_pref") ?? "auto"
    }

    static func setPreference(_ value: String) {
        UserDefaults.standard.set(value, forKey: "icon_pref")
        apply()
    }

    static func apply() {
        guard UIApplication.shared.supportsAlternateIcons else { return }
        var wanted: String? = nil
        switch preference {
        case "halloween":
            wanted = "AppIconHalloween"
        case "classic":
            wanted = nil
        default:
            wanted = Theme.halloween ? "AppIconHalloween" : nil
        }
        if UIApplication.shared.alternateIconName == wanted { return }
        UIApplication.shared.setAlternateIconName(wanted) { _ in }
    }
}
