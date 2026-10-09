import StoreKit
import UIKit

extension AppModel {
    /// Después de 12 niveles ganados se pide (una sola vez) que valoren la app. iOS decide si muestra el aviso.
    func maybeAskForReview() {
        if defaults.bool(forKey: "review_asked") { return }
        if (state?.levelsWon ?? 0) < 12 { return }
        defaults.set(true, forKey: "review_asked")
        if let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene {
            SKStoreReviewController.requestReview(in: scene)
        }
    }
}
