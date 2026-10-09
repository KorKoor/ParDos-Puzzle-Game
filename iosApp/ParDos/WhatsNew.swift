import SwiftUI

/// Novedades de la versión de iPhone: se enseñan una sola vez (a quien ya jugaba antes) y la bienvenida de Noche de brujas.
struct WhatsNewView: View {
    @EnvironmentObject var model: AppModel

    private let items: [(String, String, String)] = [
        ("bag.fill", "Tienda y cofres", "Skins, efectos, ayudas, cofres y packs de gemas con precios de App Store."),
        ("rectangle.stack.fill", "Álbum de 320 piezas", "Abre cofres, vende repetidas, crea piezas y hazlas brillantes."),
        ("crown.fill", "Pase, ligas y prestigio", "Misiones, pase de temporada, liga semanal, 82 logros y rangos."),
        ("flag.checkered", "Más modos", "Torre infinita, Carrera, Duelo, duelo a distancia con código y Studio."),
        ("bell.fill", "Avisos", "Te avisamos del cofre gratis, el regalo del día y tu racha.")
    ]

    var body: some View {
        PopupFrame(title: "Novedades de ParDos") {
            VStack(alignment: .leading, spacing: 10) {
                ForEach(0..<items.count, id: \.self) { i in
                    HStack(spacing: 12) {
                        Image(systemName: items[i].0)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 38, height: 38)
                            .background(Circle().fill(Theme.accent))
                        VStack(alignment: .leading, spacing: 1) {
                            Text(items[i].1).font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                            Text(items[i].2).font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.55))
                        }
                    }
                }
            }
            BigButton(title: "¡A JUGAR!") { model.dismissCelebration() }
        }
    }
}

extension AppModel {
    /// Bienvenida de Noche de brujas (una vez por año) y novedades para quien ya jugaba.
    func showWelcomeMessages() {
        let year = Calendar.current.component(.year, from: Date())
        let hKey = "halloween_welcome_" + String(year)
        if Theme.halloween && !defaults.bool(forKey: hKey) {
            defaults.set(true, forKey: hKey)
            push(.info("¡Noche de brujas!", "Hasta el 2 de noviembre: skin de Halloween, mapa embrujado y monedas extra en la semana de la fiesta. ¡Buu!", "moon.stars.fill"))
        }
        let version = 2
        let seen = defaults.integer(forKey: "whats_new_ios")
        if seen < version {
            defaults.set(version, forKey: "whats_new_ios")
            if (state?.levelsWon ?? 0) > 0 || (state?.unlocked ?? 1) > 1 {
                push(.whatsNew)
            }
        }
    }
}
