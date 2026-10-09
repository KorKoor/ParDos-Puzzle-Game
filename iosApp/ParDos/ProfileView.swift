import SwiftUI

private enum ProfilePick: String, CaseIterable {
    case avatars = "Avatares"
    case banners = "Banners"
}

private enum OwnFilter: String, CaseIterable {
    case mine = "Míos"
    case shop = "Tienda"
    case pass = "Pase"
    case prestige = "Prestigio"
}

/// Perfil: tarjeta de jugador, estadísticas, vitrina y elección de avatar y banner.
struct ProfileView: View {
    @EnvironmentObject var model: AppModel
    @State private var pick: ProfilePick = .avatars
    @State private var filter: OwnFilter = .mine
    @State private var nameDraft: String = ""
    @State private var editing = false

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                if let state = model.state {
                    card(state)
                    stats(state)
                    progressButtons(state)
                    showcase
                    gallery(state)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 26)
            .padding(.bottom, 24)
        }
    }

    // MARK: Tarjeta

    private func card(_ state: MetaState) -> some View {
        let item = model.banner(state.banner)
        let ink = item != nil ? Color(hex: UInt32(item?.ink ?? 0x3D405B)) : Theme.ink
        let fraction = min(1.0, Double(state.xp) / Double(max(1, state.xpNext)))
        return ZStack(alignment: .bottomLeading) {
            BannerView(id: state.banner, height: 180)
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            HStack(alignment: .bottom, spacing: 14) {
                AvatarView(id: state.avatar, size: 84)
                    .overlay(Circle().stroke(Color.white, lineWidth: 3))
                VStack(alignment: .leading, spacing: 6) {
                    if editing {
                        HStack {
                            TextField("Tu nombre", text: $nameDraft)
                                .font(.system(size: 18, weight: .black, design: .rounded))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color.white.opacity(0.9)))
                            Button(action: {
                                model.setName(nameDraft)
                                editing = false
                            }) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 26))
                                    .foregroundColor(Theme.accent)
                            }
                        }
                    } else {
                        HStack(spacing: 8) {
                            Text(state.name)
                                .font(.system(size: 22, weight: .black, design: .rounded))
                                .foregroundColor(ink)
                                .lineLimit(1)
                            Button(action: {
                                nameDraft = state.name
                                editing = true
                            }) {
                                Image(systemName: "pencil.circle.fill")
                                    .font(.system(size: 20))
                                    .foregroundColor(ink.opacity(0.8))
                            }
                        }
                    }
                    Text(state.title.uppercased())
                        .font(.system(size: 9, weight: .heavy))
                        .kerning(1.5)
                        .foregroundColor(ink.opacity(0.85))
                    HStack(spacing: 6) {
                        Text("NIVEL \(state.playerLevel)")
                            .font(.system(size: 10, weight: .heavy))
                            .kerning(1.5)
                            .foregroundColor(ink.opacity(0.85))
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color.black.opacity(0.18))
                                Capsule().fill(Theme.gold).frame(width: max(6, geo.size.width * CGFloat(fraction)))
                            }
                        }
                        .frame(height: 7)
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: Logros y prestigio

    private func progressButtons(_ state: MetaState) -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                bigTile("trophy.fill", Theme.gold, "Logros", "82 por conseguir") { model.sheet = .achievements }
                bigTile("crown.fill", Color(hex: 0x8E6BD6), "Prestigio", "\(state.rank) · \(state.prestige) pts") { model.sheet = .prestige }
            }
            HStack(spacing: 10) {
                bigTile("person.2.fill", Theme.energy, "Amigos", "Ranking con tarjetas") { model.sheet = .friends }
                bigTile("chart.bar.fill", Color(hex: 0x4E8FA6), "Récords", "Tus mejores marcas") { model.sheet = .records }
            }
        }
    }

    private func bigTile(_ symbol: String, _ color: Color, _ title: String, _ detail: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(color))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.system(size: 15, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text(detail).font(.system(size: 10, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5)).lineLimit(1)
                }
                Spacer(minLength: 0)
            }
            .padding(12)
            .card(radius: 20)
        }
        .buttonStyle(PlainButtonStyle())
    }

    // MARK: Estadísticas

    private func stats(_ state: MetaState) -> some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
            statTile("star.fill", Theme.gold, "\(state.totalStars)", "Estrellas")
            statTile("flag.fill", Theme.accent, "\(state.levelsWon)", "Niveles")
            statTile("flame.fill", Theme.energy, "\(state.bestStreak)", "Mejor racha")
            statTile("bolt.fill", Color(hex: 0x4E8FA6), "\(state.winStreak)", "Victorias seguidas")
            statTile("rectangle.stack.fill", Color(hex: 0xB57CF0), "\(model.album?.owned ?? 0)", "Piezas")
            statTile("percent", Color(hex: 0x2E9E8F), "+\(state.coinPercent)%", "Bono monedas")
        }
    }

    private func statTile(_ symbol: String, _ color: Color, _ value: String, _ title: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: symbol).font(.system(size: 16, weight: .bold)).foregroundColor(color)
            Text(value).font(.system(size: 18, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
            Text(title).font(.system(size: 9, weight: .heavy)).foregroundColor(Theme.ink.opacity(0.45)).lineLimit(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .card(radius: 18)
    }

    // MARK: Vitrina

    private var showcase: some View {
        let ids = model.album?.showcase ?? []
        let slots = model.album?.slots ?? 3
        let maxSlots = model.album?.maxSlots ?? 9
        return VStack(spacing: 10) {
            SectionTitle(text: "Mi vitrina", detail: "\(ids.count)/\(slots)")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<slots, id: \.self) { i in
                        if i < ids.count, let piece = model.piece(ids[i]) {
                            Button(action: { model.setShowcase(ids.filter { $0 != piece.id }) }) {
                                PieceCard(piece: piece, series: model.series(piece.series), owned: true, foil: model.album?.foil.contains(piece.id) ?? false, width: 78)
                            }
                            .buttonStyle(PlainButtonStyle())
                        } else {
                            Button(action: { model.tab = .album }) {
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(Theme.ink.opacity(0.2), style: StrokeStyle(lineWidth: 2, dash: [5]))
                                    .frame(width: 78, height: 104)
                                    .overlay(Image(systemName: "plus").foregroundColor(Theme.ink.opacity(0.3)))
                            }
                        }
                    }
                }
                .padding(.vertical, 3)
            }
            if slots < maxSlots, let cost = model.album?.slotCost, cost > 0 {
                Button(action: { model.unlockShowcaseSlot() }) {
                    HStack(spacing: 6) {
                        Text("Abrir otro hueco")
                        GemIcon(size: 16)
                        Text("\(cost)")
                    }
                    .font(.system(size: 12, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.accent)
                }
            }
        }
        .padding(14)
        .card()
    }

    // MARK: Avatares y banners

    private func gallery(_ state: MetaState) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                ForEach(ProfilePick.allCases, id: \.self) { item in
                    Button(action: { pick = item }) {
                        Text(item.rawValue)
                            .font(.system(size: 14, weight: .black, design: .rounded))
                            .foregroundColor(pick == item ? .white : Theme.ink.opacity(0.6))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Capsule().fill(pick == item ? Theme.accent : Color.white))
                    }
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(OwnFilter.allCases, id: \.self) { item in
                        Button(action: { filter = item }) {
                            Text(item.rawValue)
                                .font(.system(size: 12, weight: .heavy, design: .rounded))
                                .foregroundColor(filter == item ? .white : Theme.ink.opacity(0.55))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Capsule().fill(filter == item ? Theme.ink : Color.white))
                        }
                    }
                }
            }
            if pick == .avatars {
                avatarGrid(state)
            } else {
                bannerGrid(state)
            }
        }
    }

    private func avatarOwned(_ item: AvatarItem, _ state: MetaState) -> Bool {
        item.source == "FREE" || state.ownedAvatars.contains(item.id)
    }

    private func avatarGrid(_ state: MetaState) -> some View {
        let list = model.avatars.filter { item in
            switch filter {
            case .mine: return avatarOwned(item, state)
            case .shop: return item.source == "SHOP"
            case .pass: return item.source == "SEASON"
            case .prestige: return item.source == "PRESTIGE"
            }
        }
        return LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            ForEach(list) { item in
                let owned = avatarOwned(item, state)
                Button(action: {
                    if owned { model.setAvatar(item.id) } else if item.source == "SHOP" { model.buyAvatar(item.id) } else { model.showToast(item.source == "SEASON" ? "Es un premio del pase de temporada" : "Se desbloquea con un rango de prestigio") }
                }) {
                    VStack(spacing: 4) {
                        AvatarView(id: item.id, size: 62)
                            .opacity(owned ? 1 : 0.55)
                            .overlay(Circle().stroke(state.avatar == item.id ? Theme.accent : Color.clear, lineWidth: 3).padding(-3))
                        if owned {
                            Text(state.avatar == item.id ? "PUESTO" : " ")
                                .font(.system(size: 8, weight: .heavy))
                                .foregroundColor(Theme.accent)
                        } else if item.source == "SHOP" {
                            HStack(spacing: 2) { CoinIcon(size: 11); Text("\(item.coin)") }
                                .font(.system(size: 9, weight: .heavy, design: .rounded))
                                .foregroundColor(Theme.ink)
                        } else {
                            Image(systemName: "lock.fill").font(.system(size: 10)).foregroundColor(Theme.ink.opacity(0.4))
                        }
                    }
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(12)
        .card()
    }

    private func bannerOwned(_ item: BannerItem, _ state: MetaState) -> Bool {
        item.source == "FREE" || state.ownedBanners.contains(item.id)
    }

    private func bannerGrid(_ state: MetaState) -> some View {
        let list = model.banners.filter { item in
            switch filter {
            case .mine: return bannerOwned(item, state)
            case .shop: return item.source == "SHOP"
            case .pass: return item.source == "SEASON"
            case .prestige: return item.source == "PRESTIGE"
            }
        }
        return LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
            ForEach(list) { item in
                let owned = bannerOwned(item, state)
                Button(action: {
                    if owned { model.setBanner(item.id) } else if item.source == "SHOP" { model.buyBanner(item.id) } else { model.showToast(item.source == "SEASON" ? "Es un premio del pase de temporada" : "Se desbloquea con un rango de prestigio") }
                }) {
                    ZStack(alignment: .bottomLeading) {
                        BannerView(id: item.id, height: 64)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .opacity(owned ? 1 : 0.55)
                        HStack(spacing: 4) {
                            Text(item.name)
                                .font(.system(size: 11, weight: .black, design: .rounded))
                                .foregroundColor(Color(hex: UInt32(item.ink)))
                                .lineLimit(1)
                            Spacer()
                            if !owned && item.source == "SHOP" {
                                if item.coin > 0 { CoinIcon(size: 12); Text("\(item.coin)").font(.system(size: 10, weight: .heavy)).foregroundColor(Color(hex: UInt32(item.ink))) }
                                if item.gem > 0 { GemIcon(size: 12); Text("\(item.gem)").font(.system(size: 10, weight: .heavy)).foregroundColor(Color(hex: UInt32(item.ink))) }
                            } else if !owned {
                                Image(systemName: "lock.fill").font(.system(size: 10)).foregroundColor(Color(hex: UInt32(item.ink)))
                            } else if state.banner == item.id {
                                Image(systemName: "checkmark.circle.fill").foregroundColor(Theme.accent)
                            }
                        }
                        .padding(8)
                    }
                    .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(state.banner == item.id ? Theme.accent : Color.clear, lineWidth: 3))
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(12)
        .card()
    }
}
