import SwiftUI
import UIKit
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

/// Anuncios con premio (AdMob). Funciona igual que en Android: un anuncio corto a cambio de algo (duplicar el regalo, un giro extra,
/// gemas gratis, seguir jugando...). Para quien tiene VIP no se muestra nada y el premio es directo.
/// IMPORTANTE: aquí van los identificadores de PRUEBA de Google (siempre muestran anuncios de ejemplo). Antes de publicar en la App Store
/// hay que crear la app en AdMob y poner el identificador real en project.yml (GADApplicationIdentifier) y en `rewardedUnitID`.
final class AdManager: NSObject, ObservableObject {
    static let shared = AdManager()
    static let rewardedUnitID = "ca-app-pub-3940256099942544/1712485313"

    @Published private(set) var ready = false
    private var pendingReward: (() -> Void)?
    private var earned = false
    private var retries = 0

    static func topController() -> UIViewController? {
        var root: UIViewController? = nil
        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            for window in windowScene.windows where window.isKeyWindow {
                root = window.rootViewController
            }
        }
        var top = root
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }

    private func finish(rewarded: Bool) {
        let reward = pendingReward
        pendingReward = nil
        SoundManager.shared.setMusicPaused(false)
        if rewarded { reward?() }
    }

#if canImport(GoogleMobileAds)
    private var ad: GADRewardedAd?
    private var loading = false

    func start() {
        GADMobileAds.sharedInstance().start { [weak self] _ in
            DispatchQueue.main.async { self?.load() }
        }
    }

    private func load() {
        if loading || ad != nil { return }
        loading = true
        GADRewardedAd.load(withAdUnitID: AdManager.rewardedUnitID, request: GADRequest()) { [weak self] loaded, _ in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.loading = false
                if let loaded = loaded {
                    loaded.fullScreenContentDelegate = self
                    self.ad = loaded
                    self.ready = true
                    self.retries = 0
                } else {
                    self.ready = false
                    self.retries += 1
                    let wait = min(60.0, 4.0 * pow(2.0, Double(min(self.retries, 4))))
                    DispatchQueue.main.asyncAfter(deadline: .now() + wait) { self.load() }
                }
            }
        }
    }

    /// Muestra el anuncio; `onReward` se ejecuta solo si se vio hasta el final.
    func show(onReward: @escaping () -> Void, onUnavailable: @escaping () -> Void) {
        guard let loaded = ad, let presenter = AdManager.topController() else {
            onUnavailable()
            load()
            return
        }
        pendingReward = onReward
        earned = false
        SoundManager.shared.setMusicPaused(true)
        loaded.present(fromRootViewController: presenter) { [weak self] in
            self?.earned = true
        }
    }
#else
    func start() {
        ready = true
    }

    /// Sin el SDK de anuncios (compilación de prueba) el premio se da directo.
    func show(onReward: @escaping () -> Void, onUnavailable: @escaping () -> Void) {
        onReward()
    }
#endif
}

#if canImport(GoogleMobileAds)
extension AdManager: GADFullScreenContentDelegate {
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        self.ad = nil
        ready = false
        finish(rewarded: earned)
        earned = false
        load()
    }

    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        self.ad = nil
        ready = false
        pendingReward = nil
        SoundManager.shared.setMusicPaused(false)
        load()
    }
}
#endif

// MARK: - Acciones con anuncio

extension AppModel {
    var isVip: Bool { state?.vip ?? false }

    /// Enseña un anuncio (salvo VIP) y, si se ve completo, ejecuta `reward`.
    func withAd(_ reward: @escaping () -> Void) {
        if isVip {
            reward()
            return
        }
        AdManager.shared.show(onReward: reward, onUnavailable: { [weak self] in
            self?.showToast("El anuncio todavía no está listo. Prueba en un momento.")
        })
    }

    func adFreeGems() { withAd { self.run(sound: .gem) { $0.adFreeGems() } } }
    func adToken() { withAd { self.run(sound: .token) { $0.adToken() } } }
    func adSeasonBoost() { withAd { self.run(sound: .tier_up) { $0.adSeasonBoost() } } }
    func adRepairStreak() {
        withAd {
            if self.run(sound: .streak_saved, { $0.adRepairStreak() }) { self.dismissCelebration() }
        }
    }
    func adSkipChest() {
        withAd {
            if self.run(sound: .unlock, { $0.adSkipFreeChest() }) { self.claimFreeChest() }
        }
    }
    func adDoubleGift() { withAd { self.run(sound: .claim_all) { $0.adDoubleGift() } } }
}

// MARK: - Botón

/// Botón de "ver un anuncio y ganar": con VIP dice que es gratis y sin anuncio.
struct WatchAdButton: View {
    @EnvironmentObject var model: AppModel
    @ObservedObject private var ads = AdManager.shared
    let title: String
    let subtitle: String
    var tag: String? = nil
    var color: Color = Color(hex: 0x8E6BD6)
    let action: () -> Void

    var body: some View {
        let vip = model.isVip
        return Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: vip ? "crown.fill" : "play.rectangle.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                VStack(alignment: .leading, spacing: 1) {
                    Text(loc(title))
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                    Text(loc(vip ? "VIP: gratis, sin anuncio" : subtitle))
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(Color.white.opacity(0.88))
                }
                Spacer(minLength: 4)
                if let tag = tag {
                    Text(tag)
                        .font(.system(size: 12, weight: .black, design: .rounded))
                        .foregroundColor(color)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(Color.white))
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(LinearGradient(colors: [color.shade(0.14), color], startPoint: .top, endPoint: .bottom))
            )
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder(Color.white.opacity(0.3), lineWidth: 1))
            .shadow(color: color.shade(-0.3).opacity(0.5), radius: 0, x: 0, y: 3)
            .opacity(vip || ads.ready ? 1 : 0.6)
        }
        .buttonStyle(ToyPressStyle())
    }
}

/// Tarjeta de Inicio: gemas gratis por anuncio (con tope diario).
struct FreeGemsCard: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        if let ads = model.state?.ads, ads.gems > 0 {
            WatchAdButton(
                title: "GEMAS GRATIS",
                subtitle: "Ver un anuncio · quedan \(ads.gems) hoy",
                tag: "+\(ads.gemsAmount)",
                color: Color(hex: 0x3FA7E8)
            ) {
                model.adFreeGems()
            }
        }
    }
}
