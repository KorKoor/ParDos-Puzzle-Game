import SwiftUI

private enum SkinFilter: String, CaseIterable {
    case all = "Todas"
    case shop = "Tienda"
    case special = "Eventos y secretas"
    case mine = "Mías"
}

/// Tienda: oferta del día, cofres, skins de fichas, efectos de fusión, ayudas, gemas y esencia.
struct ShopView: View {
    @EnvironmentObject var model: AppModel
    @State private var filter: SkinFilter = .all

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 16) {
                header
                if let state = model.state {
                    OfferCard(offer: state.offer)
                    specials
                    gemsSection
                    chests
                    helps(state)
                    skinsSection(state)
                    fxSection(state)
                    essence
                    piggy(state)
                }
                Button(action: { model.restorePurchases() }) {
                    Text("Restaurar compras")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.accent)
                }
                Text(model.storeIsLive ? "Pagos seguros de la App Store." : "Versión de prueba: las compras con dinero real no cobran (con la App Store, el precio saldrá en tu moneda).")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.4))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }
            .padding(.horizontal, 18)
            .padding(.top, 26)
            .padding(.bottom, 24)
        }
    }

    private var header: some View {
        VStack(spacing: 10) {
            HStack {
                Text("Tienda")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
            }
            HStack(spacing: 8) {
                AmountPill(kind: .coin, amount: model.coins)
                AmountPill(kind: .gem, amount: model.gems)
                AmountPill(kind: .shard, amount: model.state?.shards ?? 0)
                AmountPill(kind: .token, amount: model.state?.tokens ?? 0)
                Spacer()
            }
        }
    }

    // MARK: Cofres

    private var chests: some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Cofres", detail: "traen piezas del álbum")
            HStack(spacing: 8) {
                if let eco = model.eco {
                    chestOffer("COMMON", coins: eco.commonChestCoins, gems: 0)
                    chestOffer("RARE", coins: eco.rareChestCoins, gems: eco.rareChestGems)
                    chestOffer("EPIC", coins: 0, gems: eco.epicChestGems)
                }
            }
        }
    }

    private func chestOffer(_ type: String, coins: Int, gems: Int) -> some View {
        VStack(spacing: 6) {
            ChestIcon(type: type, size: 52)
            Text(chestName(type).replacingOccurrences(of: "Cofre ", with: "").uppercased())
                .font(.system(size: 10, weight: .heavy))
                .kerning(1)
                .foregroundColor(rarityColor(type == "COMMON" ? "COMMON" : type))
            if coins > 0 {
                priceButton(coin: coins, gem: 0, enabled: model.coins >= coins) { model.buyChest(type, gems: false) }
            }
            if gems > 0 {
                priceButton(coin: 0, gem: gems, enabled: model.gems >= gems) { model.buyChest(type, gems: true) }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .padding(.horizontal, 6)
        .card(radius: 20)
    }

    private func priceButton(coin: Int, gem: Int, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if coin > 0 { CoinIcon(size: 15); Text("\(coin)") }
                if gem > 0 { GemIcon(size: 15); Text("\(gem)") }
            }
            .font(.system(size: 12, weight: .black, design: .rounded))
            .foregroundColor(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(Capsule().fill(enabled ? Theme.accent : Color.gray.opacity(0.5)))
        }
    }

    // MARK: Ayudas

    private func helps(_ state: MetaState) -> some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Ayudas")
            if let eco = model.eco {
                VStack(spacing: 0) {
                    helpRow("arrow.uturn.backward", "Deshacer ×3", "tienes \(state.undos)", eco.undoPrice * 3, false) { model.buyUndos(3) }
                    Divider()
                    helpRow("timer", "Tiempo extra ×3", "tienes \(state.extraTimes) · +20 s en niveles con reloj", eco.extraTimePrice * 3, false) { model.buyExtraTimes(3) }
                    Divider()
                    helpRow("shield.fill", "Escudo de racha", "tienes \(state.freezes) de \(eco.maxFreezes) · cubre un día sin jugar", eco.freezePrice, false) { model.buyFreeze() }
                    Divider()
                    helpRow("bolt.fill", "Impulso de monedas", state.boostWins > 0 ? "activo: \(state.boostWins) victorias +\(eco.boostPercent)%" : "+\(eco.boostPercent)% por \(eco.boostWins) victorias", eco.boostPrice, true) { model.buyBoost() }
                }
                .padding(.horizontal, 14)
                .card()
            }
        }
    }

    private func helpRow(_ symbol: String, _ title: String, _ detail: String, _ price: Int, _ gems: Bool, _ action: @escaping () -> Void) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(Theme.accent)
                .frame(width: 38, height: 38)
                .background(Circle().fill(Theme.accent.opacity(0.12)))
            VStack(alignment: .leading, spacing: 2) {
                Text(loc(title)).font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                Text(loc(detail)).font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5)).lineLimit(2)
            }
            Spacer()
            priceButton(coin: gems ? 0 : price, gem: gems ? price : 0, enabled: gems ? model.gems >= price : model.coins >= price, action: action)
        }
        .padding(.vertical, 10)
    }

    // MARK: Skins

    private func skinsSection(_ state: MetaState) -> some View {
        let list = filteredSkins(state)
        return VStack(spacing: 10) {
            SectionTitle(text: "Skins de fichas", detail: "\(state.ownedSkins.count)/\(model.skins.count)")
            studioCard(state)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(SkinFilter.allCases, id: \.self) { item in
                        Button(action: { filter = item }) {
                            Text(item.rawValue)
                                .font(.system(size: 12, weight: .heavy, design: .rounded))
                                .foregroundColor(filter == item ? .white : Theme.ink.opacity(0.6))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .background(Capsule().fill(filter == item ? Theme.accent : Color.white))
                        }
                    }
                }
            }
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                ForEach(list) { skin in
                    SkinCard(skin: skin, state: state)
                }
            }
        }
    }

    private func studioCard(_ state: MetaState) -> some View {
        let owned = state.ownedSkins.contains("studio")
        let price = model.priceLabel("skin_studio", model.store?.specials.first(where: { $0.id == "skin_studio" })?.price ?? "$3.99")
        return Button(action: { model.sheet = .studio }) {
            HStack(spacing: 12) {
                Image(systemName: "paintpalette.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 46, height: 46)
                    .background(Circle().fill(LinearGradient(colors: [Theme.energy, Theme.accent], startPoint: .topLeading, endPoint: .bottomTrailing)))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Studio · diseña tu skin").font(.system(size: 15, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text(owned ? "Abre el editor y cambia colores, acabado y fondo" : "Acabado, colores, fondo y partículas a tu gusto")
                        .font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
                Text(owned ? "EDITAR" : price)
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(Theme.accent))
            }
            .padding(12)
            .card()
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func filteredSkins(_ state: MetaState) -> [SkinItem] {
        switch filter {
        case .all: return model.skins.filter { $0.id != "studio" || state.ownedSkins.contains("studio") }
        case .shop: return model.skins.filter { $0.source == "SHOP" && $0.id != "studio" }
        case .special: return model.skins.filter { $0.source == "EVENT" || $0.source == "HIDDEN" }
        case .mine: return model.skins.filter { state.ownedSkins.contains($0.id) }
        }
    }

    // MARK: Efectos

    private func fxSection(_ state: MetaState) -> some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Efectos de fusión", detail: "solo cosméticos")
            VStack(spacing: 0) {
                ForEach(0..<model.fxs.count, id: \.self) { i in
                    fxRow(model.fxs[i], state)
                    if i < model.fxs.count - 1 { Divider() }
                }
            }
            .padding(.horizontal, 14)
            .card()
        }
    }

    private func fxRow(_ fx: FxItem, _ state: MetaState) -> some View {
        let owned = state.ownedFx.contains(fx.id)
        let equipped = state.equippedFx == fx.id
        return HStack(spacing: 12) {
            Image(systemName: "sparkles")
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(rarityColor(fx.rarity))
                .frame(width: 38, height: 38)
                .background(Circle().fill(rarityColor(fx.rarity).opacity(0.14)))
            VStack(alignment: .leading, spacing: 2) {
                Text(fx.name).font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                Text(fx.blurb).font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5)).lineLimit(2)
            }
            Spacer()
            if equipped {
                Text("PUESTO").font(.system(size: 11, weight: .heavy)).foregroundColor(Theme.accent)
            } else if owned {
                smallButton("USAR") { model.equipFx(fx.id) }
            } else if fx.buyable {
                priceButton(coin: fx.coin, gem: fx.gem, enabled: model.coins >= fx.coin && model.gems >= fx.gem) { model.buyFx(fx.id) }
            } else {
                Text(fx.source == "SEASON" ? "PASE" : "REGALO")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundColor(Theme.ink.opacity(0.4))
            }
        }
        .padding(.vertical, 10)
    }

    private func smallButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(loc(title))
                .font(.system(size: 12, weight: .black, design: .rounded))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(Capsule().fill(Theme.accent))
        }
    }

    // MARK: Gemas y esencia

    private var gemsSection: some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Gemas", detail: "primera compra de cada pack: ¡doble!")
            if let packs = model.store?.packs {
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                    ForEach(packs) { pack in
                        packCard(pack)
                    }
                }
            }
            HStack(spacing: 12) {
                GemIcon(size: 26)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Cambiar gemas por monedas").font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text("5 gemas = \(5 * (model.eco?.coinsPerGem ?? 40)) monedas").font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
                smallButton("CAMBIAR") { model.exchangeGems(5) }
            }
            .padding(14)
            .card()
        }
    }

    private func packCard(_ pack: StorePack) -> some View {
        let shown = pack.first ? pack.gems * 2 : pack.gems
        return VStack(spacing: 6) {
            ZStack(alignment: .top) {
                HStack(spacing: -8) {
                    GemIcon(size: 30)
                    GemIcon(size: 38)
                    if pack.gems >= 1000 { GemIcon(size: 30) }
                }
                .padding(.top, 14)
                if pack.best {
                    Text("MEJOR VALOR")
                        .font(.system(size: 8, weight: .black))
                        .kerning(1)
                        .foregroundColor(.white)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(Theme.energy))
                        .offset(y: -4)
                }
            }
            .frame(height: 56)
            Text("\(shown)")
                .font(.system(size: 22, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text(pack.name)
                .font(.system(size: 11, weight: .heavy))
                .foregroundColor(Theme.ink.opacity(0.5))
            Text(pack.first ? "¡Doble la 1.ª vez!" : (pack.bonus > 0 ? "+\(pack.bonus)% de bono" : "Pack básico"))
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(pack.first ? Theme.energy : Theme.ink.opacity(0.45))
            Button(action: { model.buyProduct(pack.id) }) {
                Text(model.priceLabel(pack.id, pack.price))
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(Capsule().fill(pack.best ? Theme.energy : Theme.accent))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .padding(.horizontal, 12)
        .card(radius: 20)
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(pack.best ? Theme.energy : Color.clear, lineWidth: 2.5))
    }

    private var specials: some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Ofertas especiales")
            if let items = model.store?.specials {
                ForEach(items) { item in
                    specialRow(item)
                }
            }
        }
    }

    private func specialRow(_ item: StoreSpecial) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Theme.gold.opacity(0.18))
                    .frame(width: 54, height: 54)
                specialIcon(item.id)
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(item.name)
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text(item.blurb)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.55))
                    .lineLimit(3)
            }
            Spacer(minLength: 4)
            if item.owned {
                Text("TUYO")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundColor(Theme.accent)
            } else {
                Button(action: { model.buyProduct(item.id) }) {
                    Text(model.priceLabel(item.id, item.price))
                        .font(.system(size: 13, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(Capsule().fill(item.available ? Theme.accent : Color.gray.opacity(0.5)))
                }
                .disabled(!item.available)
            }
        }
        .padding(12)
        .card()
    }

    @ViewBuilder
    private func specialIcon(_ id: String) -> some View {
        switch id {
        case "starter_pack": SpriteImage(name: "ico_gift", size: 40)
        case "season_pass": SpriteImage(name: "ico_crown", size: 40)
        case "vip_forever": SpriteImage(name: "ico_star", size: 40)
        default: Image(systemName: "dollarsign.circle.fill").font(.system(size: 34)).foregroundColor(Color(hex: 0xF29BB5))
        }
    }

    private var essence: some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Álbum")
            if let eco = model.eco {
                VStack(spacing: 0) {
                    HStack(spacing: 12) {
                        Image(systemName: "sparkle").font(.system(size: 20, weight: .bold)).foregroundColor(Color(hex: 0xB57CF0)).frame(width: 38)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Pack de esencia ×\(eco.shardPackShards)").font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                            Text("Sirve para crear piezas que te faltan · hoy \(model.album?.shardPacksToday ?? 0)/\(eco.shardPacksPerDay)").font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                        }
                        Spacer()
                        priceButton(coin: 0, gem: eco.shardPackGems, enabled: model.gems >= eco.shardPackGems) { model.buyShardPack() }
                    }
                    .padding(.vertical, 10)
                    Divider()
                    HStack(spacing: 12) {
                        Image(systemName: "arrow.left.arrow.right.circle.fill").font(.system(size: 20, weight: .bold)).foregroundColor(Color(hex: 0x2E9E8F)).frame(width: 38)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Ficha de intercambio").font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                            Text("Para cambiar repetidas con amigos · hoy \(model.album?.tokensToday ?? 0)/\(eco.tokensPerDay)").font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                        }
                        Spacer()
                        priceButton(coin: 0, gem: eco.tokenGems, enabled: model.gems >= eco.tokenGems) { model.buyTradeToken() }
                    }
                    .padding(.vertical, 10)
                }
                .padding(.horizontal, 14)
                .card()
            }
        }
    }

    private func piggy(_ state: MetaState) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "dollarsign.circle.fill").font(.system(size: 28)).foregroundColor(Color(hex: 0xF29BB5))
            VStack(alignment: .leading, spacing: 2) {
                Text("Hucha de gemas").font(.system(size: 14, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                Text("Guarda gemas cada vez que ganas (\(state.piggy)/300). Se rompe en «Ofertas especiales».").font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
            }
            Spacer()
            HStack(spacing: 3) { GemIcon(size: 16); Text("\(state.piggy)") }
                .font(.system(size: 13, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
        }
        .padding(14)
        .card()
    }
}

/// Tarjeta de una skin en la tienda.
struct SkinCard: View {
    @EnvironmentObject var model: AppModel
    let skin: SkinItem
    let state: MetaState

    var body: some View {
        let owned = state.ownedSkins.contains(skin.id)
        let equipped = state.equippedSkin == skin.id
        return VStack(spacing: 8) {
            SkinSwatch(skin: skin, size: 74)
            Text(skin.name)
                .font(.system(size: 14, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
                .lineLimit(1)
            Text(rarityName(skin.rarity).uppercased())
                .font(.system(size: 9, weight: .heavy))
                .kerning(1.5)
                .foregroundColor(rarityColor(skin.rarity))
            action(owned: owned, equipped: equipped)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .padding(.horizontal, 8)
        .card(radius: 20)
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(equipped ? Theme.accent : Color.clear, lineWidth: 2.5))
    }

    @ViewBuilder
    private func action(owned: Bool, equipped: Bool) -> some View {
        if equipped {
            Text("PUESTA")
                .font(.system(size: 11, weight: .heavy))
                .foregroundColor(Theme.accent)
                .frame(height: 30)
        } else if owned {
            Button(action: { model.equipSkin(skin.id) }) {
                Text("USAR")
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 7)
                    .background(Capsule().fill(Theme.accent))
            }
        } else if !skin.exclusive && (skin.coin > 0 || skin.gem > 0) {
            Button(action: { model.buySkin(skin.id) }) {
                HStack(spacing: 4) {
                    if skin.coin > 0 { CoinIcon(size: 15); Text("\(skin.coin)") }
                    if skin.gem > 0 { GemIcon(size: 15); Text("\(skin.gem)") }
                }
                .font(.system(size: 12, weight: .black, design: .rounded))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(Capsule().fill(model.coins >= skin.coin && model.gems >= skin.gem ? Theme.accent : Color.gray.opacity(0.5)))
            }
        } else {
            Text(lockText)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(Theme.ink.opacity(0.5))
                .multilineTextAlignment(.center)
                .frame(height: 30)
        }
    }

    private var lockText: String {
        if let hint = skin.hint { return hint }
        if let event = skin.event { return "Se gana en \(event)" }
        switch skin.source {
        case "ALBUM": return "Premio del álbum completo"
        case "SEASON": return "Premio del pase"
        case "PURCHASE": return "Compra especial"
        default: return "Exclusiva"
        }
    }
}
