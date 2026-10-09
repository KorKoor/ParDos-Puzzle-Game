import SwiftUI

/// Une la tienda de pago con StoreKit: si la App Store responde, se cobra de verdad; si no, son compras de prueba.
extension AppModel {
    var storeIsLive: Bool { !livePrices.isEmpty }

    var allProductIDs: [String] {
        var ids: [String] = []
        for pack in store?.packs ?? [] { ids.append(pack.id) }
        for item in store?.specials ?? [] { ids.append(item.id) }
        return ids
    }

    func startStore() {
        let ids = allProductIDs
        StoreManager.shared.listen { [weak self] productID in
            self?.deliverProduct(productID)
        }
        Task {
            await StoreManager.shared.load(ids: ids)
            let prices = StoreManager.shared.prices
            await MainActor.run { self.livePrices = prices }
        }
    }

    /// Precio que se enseña: el de la App Store (en tu moneda) o el de referencia en dólares.
    func priceLabel(_ id: String, _ fallback: String) -> String {
        return livePrices[id] ?? fallback
    }

    func buyProduct(_ id: String) {
        if livePrices[id] != nil {
            Task {
                let ok = await StoreManager.shared.buy(id)
                if ok {
                    await MainActor.run { self.deliverProduct(id) }
                }
            }
        } else {
            testBuyProduct(id)
        }
    }

    func deliverProduct(_ id: String) {
        run { $0.testBuyProduct(id: id) }
    }

    /// Recupera lo que ya compraste (VIP, pack inicial, Studio) al cambiar de iPhone.
    func restorePurchases() {
        Task {
            let owned = await StoreManager.shared.ownedOneTimeProducts()
            await MainActor.run {
                var restored = 0
                for id in owned where id == "vip_forever" || id == "starter_pack" || id == "skin_studio" {
                    if self.run({ $0.testBuyProduct(id: id) }) { restored += 1 }
                }
                self.showToast(restored > 0 ? "Compras restauradas" : "No hay compras que restaurar")
            }
        }
    }
}
