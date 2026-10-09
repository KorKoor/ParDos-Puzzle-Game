import Foundation
import StoreKit

/// Compras reales con StoreKit 2. En el .ipa que se instala con Sideloadly (sin cuenta de pago) la App Store no devuelve productos;
/// entonces la app usa las compras de prueba (gratis). Con una cuenta de desarrollador y los productos de `docs/APPSTORE_PRODUCTOS.md`
/// creados en App Store Connect, el mismo código cobra de verdad y muestra el precio en la moneda local.
final class StoreManager {
    static let shared = StoreManager()

    private(set) var products: [String: Product] = [:]
    private var listener: Task<Void, Never>?
    private let doneKey = "storekit_done_transactions"

    var prices: [String: String] {
        var map: [String: String] = [:]
        for (id, product) in products { map[id] = product.displayPrice }
        return map
    }

    func load(ids: [String]) async {
        do {
            let list = try await Product.products(for: ids)
            var map: [String: Product] = [:]
            for product in list { map[product.id] = product }
            products = map
        } catch {
            products = [:]
        }
    }

    /// Compra un producto. Devuelve `true` si la App Store confirmó el pago.
    func buy(_ id: String) async -> Bool {
        guard let product = products[id] else { return false }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                switch verification {
                case .verified(let transaction):
                    markDone(transaction.id)
                    await transaction.finish()
                    return true
                case .unverified:
                    return false
                }
            default:
                return false
            }
        } catch {
            return false
        }
    }

    private func isDone(_ id: UInt64) -> Bool {
        let list = UserDefaults.standard.array(forKey: doneKey) as? [String] ?? []
        return list.contains(String(id))
    }

    private func markDone(_ id: UInt64) {
        var list = UserDefaults.standard.array(forKey: doneKey) as? [String] ?? []
        list.append(String(id))
        if list.count > 200 { list.removeFirst(list.count - 200) }
        UserDefaults.standard.set(list, forKey: doneKey)
    }

    /// Compras que llegan fuera de la app (por ejemplo, aprobadas por un padre): se entregan al abrir.
    func listen(deliver: @escaping (String) -> Void) {
        listener?.cancel()
        listener = Task {
            for await update in Transaction.updates {
                if case .verified(let transaction) = update {
                    let fresh = !self.isDone(transaction.id)
                    self.markDone(transaction.id)
                    await transaction.finish()
                    if fresh {
                        let productID = transaction.productID
                        await MainActor.run { deliver(productID) }
                    }
                }
            }
        }
    }

    /// Productos de una sola vez que el jugador ya compró (para "Restaurar compras").
    func ownedOneTimeProducts() async -> [String] {
        var out: [String] = []
        for await entitlement in Transaction.currentEntitlements {
            if case .verified(let transaction) = entitlement {
                out.append(transaction.productID)
            }
        }
        return out
    }
}
