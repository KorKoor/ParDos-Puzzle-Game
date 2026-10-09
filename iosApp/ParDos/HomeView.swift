import SwiftUI

/// Inicio: perfil, jugar, reto diario, lo de hoy (regalo, cofre, ruleta, misiones, pase, liga), eventos y oferta.
struct HomeView: View {
    @EnvironmentObject var model: AppModel
    @State private var now = Date()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                profileCard
                playButton
                dailyCard
                todayStrip
                if let state = model.state {
                    MissionsCard(missions: state.missions, perfectDays: state.perfectDays)
                    ForEach(state.events) { event in
                        EventBanner(event: event)
                    }
                    ForEach(state.eventSkins) { info in
                        EventSkinCard(info: info)
                    }
                    OfferCard(offer: state.offer)
                    chestsCard(state)
                    streakCard(state)
                    if let goal = state.nextGoal {
                        goalCard(goal)
                    }
                }
                Text("Versión de prueba para iPhone · la misma lógica y reglas que en Android")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.35))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }
            .padding(.horizontal, 18)
            .padding(.top, 26)
            .padding(.bottom, 20)
        }
        .onReceive(timer) { value in now = value }
    }

    // MARK: Perfil

    private var profileCard: some View {
        let state = model.state
        let name = state?.name ?? "Jugador Zen"
        let level = state?.playerLevel ?? 1
        let xp = state?.xp ?? 0
        let need = max(1, state?.xpNext ?? 100)
        let fraction = min(1.0, Double(xp) / Double(need))
        return VStack(spacing: 10) {
            ZStack(alignment: .topLeading) {
                BannerView(id: state?.banner ?? 1, height: 96)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                HStack(spacing: 12) {
                    Button(action: { model.tab = .profile }) {
                        AvatarView(id: state?.avatar ?? 1, size: 62)
                            .overlay(Circle().stroke(Color.white, lineWidth: 3))
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Text(name)
                            .font(.system(size: 19, weight: .black, design: .rounded))
                            .foregroundColor(bannerInk)
                            .lineLimit(1)
                        HStack(spacing: 6) {
                            Text("NIVEL \(level) · " + (state?.rank ?? "Novato").uppercased())
                                .font(.system(size: 10, weight: .heavy))
                                .kerning(1.5)
                                .foregroundColor(bannerInk.opacity(0.8))
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule().fill(Color.black.opacity(0.18))
                                    Capsule().fill(Theme.gold).frame(width: max(6, geo.size.width * CGFloat(fraction)))
                                }
                            }
                            .frame(height: 7)
                        }
                    }
                    Spacer(minLength: 0)
                    Button(action: { model.sheet = .settings }) {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Theme.ink)
                            .frame(width: 38, height: 38)
                            .background(Circle().fill(Color.white.opacity(0.92)))
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 17)
            }
            CurrencyBar()
        }
    }

    private var bannerInk: Color {
        guard let item = model.banner(model.state?.banner ?? 1) else { return Theme.ink }
        return Color(hex: UInt32(item.ink))
    }

    // MARK: Jugar

    private var playButton: some View {
        Button(action: { model.start(model.unlocked) }) {
            HStack(spacing: 14) {
                Image(systemName: "play.fill")
                    .font(.system(size: 26, weight: .bold))
                VStack(alignment: .leading, spacing: 2) {
                    Text("JUGAR")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .kerning(3)
                    Text(Theme.halloween ? "Nivel \(model.unlocked) · ¡BUU!" : "Nivel \(model.unlocked)")
                        .font(.system(size: 13, weight: .bold))
                        .opacity(0.85)
                }
                Spacer()
                if Theme.halloween {
                    HStack(spacing: -6) {
                        FloatingSprite(name: "ico_ghost", size: 40, tilt: -8, phase: 0.2)
                        FloatingSprite(name: "ico_pumpkin", size: 50, tilt: 6, phase: 0.5)
                    }
                } else {
                    FloatingSprite(name: "ico_star", size: 46, tilt: 8, phase: 0.3)
                }
            }
            .foregroundColor(.white)
            .padding(.horizontal, 24)
            .frame(height: 96)
            .background(
                RoundedRectangle(cornerRadius: 30, style: .continuous)
                    .fill(LinearGradient(colors: [Theme.accent, Theme.accentDark], startPoint: .top, endPoint: .bottom))
            )
            .shadow(color: Theme.accentDark.opacity(0.5), radius: 0, x: 0, y: 6)
        }
    }

    private var dailyCard: some View {
        Button(action: { model.startDaily() }) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(kindColor(model.dailyCard?.kind ?? "ZEN"))
                        .frame(width: 46, height: 46)
                    Image(systemName: kindSymbol(model.dailyCard?.kind ?? "ZEN"))
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("RETO DIARIO")
                        .font(.system(size: 10, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.gold)
                    Text(model.dailyCard?.kindLabel ?? "Reto")
                        .font(.system(size: 17, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                    Text(model.dailyCard?.goal ?? "")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Color.white.opacity(0.7))
                        .lineLimit(1)
                }
                Spacer()
                if model.dailyDone {
                    HStack(spacing: 2) {
                        ForEach(0..<3, id: \.self) { i in
                            Image(systemName: "star.fill")
                                .font(.system(size: 13))
                                .foregroundColor(i < model.dailyStars ? Theme.gold : Color.white.opacity(0.2))
                        }
                    }
                } else {
                    Image(systemName: "chevron.right")
                        .foregroundColor(Color.white.opacity(0.6))
                }
            }
            .padding(.horizontal, 16)
            .frame(height: 74)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Theme.ink))
            .shadow(color: Theme.ink.opacity(0.25), radius: 0, x: 0, y: 4)
        }
    }

    // MARK: Lo de hoy

    private var todayStrip: some View {
        let state = model.state
        let remaining = model.freeChestRemaining(now: now)
        let chestReady = (state?.freeChest.ready ?? false) || remaining == 0
        let giftReady = state?.dailyReward.claimable ?? false
        let freeSpins = state?.wheel.freeLeft ?? 0
        let missions = state?.missions ?? []
        let claimedCount = missions.filter { $0.claimed }.count
        let readyMissions = missions.filter { $0.done && !$0.claimed }.count
        let season = state?.season
        return VStack(spacing: 8) {
            SectionTitle(text: "Hoy")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    TodayTile(
                        title: "Regalo", detail: giftReady ? "¡Reclamar!" : "Vuelve mañana", hot: giftReady,
                        icon: AnyView(SpriteImage(name: "ico_gift", size: 38))
                    ) {
                        if giftReady { model.push(.dailyGift) } else { model.showToast("Ya reclamaste el regalo de hoy") }
                    }
                    TodayTile(
                        title: "Cofre gratis", detail: chestReady ? "¡Listo!" : formatClock(ms: remaining), hot: chestReady,
                        icon: AnyView(ChestIcon(type: state?.freeChest.type ?? "COMMON", size: 40))
                    ) {
                        if chestReady { model.claimFreeChest() } else { model.showToast("Falta \(formatClock(ms: remaining)) para el próximo cofre") }
                    }
                    TodayTile(
                        title: "Ruleta", detail: freeSpins > 0 ? "1 giro gratis" : "Vuelve mañana", hot: freeSpins > 0,
                        icon: AnyView(SpriteImage(name: "ico_party", size: 38))
                    ) { model.sheet = .wheel }
                    TodayTile(
                        title: "Misiones", detail: "\(claimedCount)/\(missions.count) cobradas", hot: readyMissions > 0,
                        icon: AnyView(SpriteImage(name: "ico_fire", size: 38))
                    ) { model.sheet = .missions }
                    TodayTile(
                        title: "Pase", detail: "Nivel \(season?.tier ?? 0)/30", hot: (season?.claimable ?? 0) > 0,
                        icon: AnyView(SpriteImage(name: "ico_crown", size: 38))
                    ) { model.sheet = .season }
                    TodayTile(
                        title: "Liga", detail: state?.league.name ?? "Bronce", hot: state?.league.pending != nil,
                        icon: AnyView(SpriteImage(name: "ico_star", size: 38))
                    ) { model.sheet = .league }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 2)
            }
        }
    }

    // MARK: Cofres, racha y meta

    private func chestsCard(_ state: MetaState) -> some View {
        VStack(spacing: 10) {
            SectionTitle(text: "Tus cofres", detail: state.totalChests == 0 ? "ninguno" : "\(state.totalChests)")
            HStack(spacing: 8) {
                chestSlot("COMMON", state.chests.COMMON)
                chestSlot("RARE", state.chests.RARE)
                chestSlot("EPIC", state.chests.EPIC)
            }
            Text("Se consiguen con el cofre gratis, las misiones, el pase y los capítulos. Dentro hay piezas del álbum.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.45))
                .multilineTextAlignment(.center)
        }
        .padding(14)
        .card()
    }

    private func chestSlot(_ type: String, _ count: Int) -> some View {
        Button(action: { if count > 0 { model.openChest(type) } else { model.showToast("No tienes cofres de este tipo") } }) {
            VStack(spacing: 4) {
                ZStack(alignment: .topTrailing) {
                    ChestIcon(type: type, size: 46)
                        .opacity(count > 0 ? 1 : 0.35)
                    if count > 0 { CountBadge(count: count).offset(x: 6, y: -4) }
                }
                Text(count > 0 ? "ABRIR" : chestName(type).replacingOccurrences(of: "Cofre ", with: ""))
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(1)
                    .foregroundColor(count > 0 ? Theme.accent : Theme.ink.opacity(0.35))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(rarityColor(type == "COMMON" ? "COMMON" : type).opacity(0.10)))
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func streakCard(_ state: MetaState) -> some View {
        VStack(spacing: 10) {
            HStack {
                Image(systemName: "flame.fill").foregroundColor(Theme.energy)
                Text("\(state.streak) \(state.streak == 1 ? "día" : "días") de racha")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Image(systemName: "shield.fill").foregroundColor(Color(hex: 0x4E8FA6))
                Text("\(state.freezes)")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
            HStack(spacing: 6) {
                ForEach(0..<state.cycle.count, id: \.self) { i in
                    let day = state.cycle[i]
                    let isToday = i + 1 == state.dailyReward.day
                    VStack(spacing: 2) {
                        Text("\(i + 1)")
                            .font(.system(size: 9, weight: .heavy))
                            .foregroundColor(Theme.ink.opacity(0.4))
                        if day.isChest {
                            ChestIcon(type: "RARE", size: 24)
                        } else {
                            CoinIcon(size: 20)
                        }
                        Text(day.isChest ? "Cofre" : "\(day.coins)")
                            .font(.system(size: 9, weight: .heavy, design: .rounded))
                            .foregroundColor(Theme.ink)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                    .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(isToday ? Theme.gold.opacity(0.25) : Theme.ink.opacity(0.04)))
                    .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(isToday ? Theme.gold : Color.clear, lineWidth: 2))
                }
            }
            Text("Un escudo de racha cubre un día que no puedas jugar. Se compran en la tienda.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.45))
                .multilineTextAlignment(.center)
        }
        .padding(14)
        .card()
    }

    private func goalCard(_ goal: NextGoalInfo) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "flag.checkered")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Theme.accent)
            VStack(alignment: .leading, spacing: 3) {
                Text("SIGUIENTE META")
                    .font(.system(size: 9, weight: .heavy))
                    .kerning(2)
                    .foregroundColor(Theme.ink.opacity(0.4))
                Text(goal.title)
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text(goal.detail)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.55))
            }
            Spacer()
        }
        .padding(14)
        .card()
    }
}

// MARK: - Piezas de Inicio

struct TodayTile: View {
    let title: String
    let detail: String
    let hot: Bool
    let icon: AnyView
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                ZStack(alignment: .topTrailing) {
                    icon.frame(height: 42)
                    if hot {
                        Circle()
                            .fill(Color(hex: 0xE0475B))
                            .frame(width: 11, height: 11)
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            .offset(x: 8, y: -2)
                    }
                }
                Text(title)
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                    .lineLimit(1)
                Text(detail)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(hot ? Theme.accent : Theme.ink.opacity(0.45))
                    .lineLimit(1)
            }
            .frame(width: 104)
            .padding(.vertical, 12)
            .card(radius: 20)
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(hot ? Theme.accent.opacity(0.7) : Color.clear, lineWidth: 2))
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct EventBanner: View {
    let event: EventInfo

    var body: some View {
        let multiplier = event.coinMult > 1.0 ? "Monedas ×\(trim(event.coinMult))" : "Experiencia ×\(trim(event.xpMult))"
        return HStack(spacing: 12) {
            Image(systemName: "sparkles")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
            VStack(alignment: .leading, spacing: 2) {
                Text(event.name.uppercased())
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .kerning(1)
                    .foregroundColor(.white)
                Text("\(multiplier) · \(event.daysLeft == 0 ? "termina hoy" : "quedan \(event.daysLeft) días")")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Color.white.opacity(0.85))
            }
            Spacer()
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(LinearGradient(colors: [Theme.energy, Theme.accent], startPoint: .leading, endPoint: .trailing))
        )
    }

    private func trim(_ value: Double) -> String {
        if value == value.rounded() { return "\(Int(value))" }
        return String(format: "%.1f", value)
    }
}

struct EventSkinCard: View {
    @EnvironmentObject var model: AppModel
    let info: EventSkinInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("SKIN DE \(info.name.uppercased())")
                    .font(.system(size: 11, weight: .heavy))
                    .kerning(1.5)
                    .foregroundColor(Theme.energy)
                Spacer()
                Text(info.daysLeft == 0 ? "último día" : "\(info.daysLeft) días")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.45))
            }
            if info.owned {
                Text("¡Ya la tienes! Equípala en la tienda.")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Theme.ink)
            } else {
                Text("Gana \(info.need) niveles durante la fiesta: llevas \(min(info.wins, info.need)).")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Theme.ink)
                HStack(spacing: 4) {
                    ForEach(0..<info.need, id: \.self) { i in
                        Capsule().fill(i < info.wins ? Theme.energy : Theme.ink.opacity(0.1)).frame(height: 8)
                    }
                }
                if let cost = model.eco?.eventSkinGems {
                    Button(action: { model.buyEventSkin(info.id) }) {
                        HStack(spacing: 6) {
                            Text("O cómprala ya")
                            GemIcon(size: 16)
                            Text("\(cost)")
                        }
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.energy)
                    }
                }
            }
        }
        .padding(14)
        .card()
    }
}

struct OfferCard: View {
    @EnvironmentObject var model: AppModel
    let offer: OfferInfo

    var body: some View {
        let title: String = offer.kind == "skin" ? (model.skin(offer.id)?.name ?? "Skin") : chestName(offer.id)
        return HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Theme.energy.opacity(0.12))
                    .frame(width: 66, height: 66)
                if offer.kind == "skin" {
                    SkinSwatch(skin: model.skin(offer.id), size: 54)
                } else {
                    ChestIcon(type: offer.id, size: 54)
                }
            }
            VStack(alignment: .leading, spacing: 3) {
                Text("OFERTA DEL DÍA · -\(offer.discount)%")
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(1.5)
                    .foregroundColor(Theme.energy)
                Text(title)
                    .font(.system(size: 17, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                HStack(spacing: 4) {
                    if offer.coin > 0 { CoinIcon(size: 16); Text("\(offer.coin)") }
                    if offer.gem > 0 { GemIcon(size: 16); Text("\(offer.gem)") }
                }
                .font(.system(size: 13, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink)
            }
            Spacer()
            Button(action: { model.buyDailyOffer() }) {
                Text(offer.owned ? "TUYA" : "COMPRAR")
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(offer.owned ? Color.gray.opacity(0.5) : Theme.energy))
            }
            .disabled(offer.owned)
        }
        .padding(14)
        .card()
    }
}

/// Muestra de una skin: cuatro fichas con sus colores.
struct SkinSwatch: View {
    let skin: SkinItem?
    var size: CGFloat = 60

    var body: some View {
        let style = BoardStyle(skin: skin)
        let cell = size * 0.46
        return ZStack {
            if style.changesTheme {
                RoundedRectangle(cornerRadius: size * 0.2, style: .continuous).fill(style.background)
            }
            VStack(spacing: size * 0.04) {
                HStack(spacing: size * 0.04) {
                    swatchTile(style, 2, cell)
                    swatchTile(style, 8, cell)
                }
                HStack(spacing: size * 0.04) {
                    swatchTile(style, 32, cell)
                    swatchTile(style, 128, cell)
                }
            }
        }
        .frame(width: size, height: size)
    }

    private func swatchTile(_ style: BoardStyle, _ value: Int, _ cell: CGFloat) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: cell * 0.22, style: .continuous).fill(style.fill(value))
            Text("\(value)")
                .font(.system(size: cell * 0.42, weight: .black, design: .rounded))
                .foregroundColor(style.text(value))
                .minimumScaleFactor(0.5)
        }
        .frame(width: cell, height: cell)
    }
}
