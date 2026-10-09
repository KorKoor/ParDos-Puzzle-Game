import SwiftUI

/// Muestra el primer aviso pendiente (regalo, cofre, skin nueva...) encima de todo.
struct CelebrationOverlay: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        if let current = model.queue.first {
            ZStack {
                Color.black.opacity(0.55).ignoresSafeArea()
                content(current)
            }
            .transition(.opacity)
            .zIndex(20)
        }
    }

    @ViewBuilder
    private func content(_ item: Celebration) -> some View {
        switch item {
        case .dailyGift:
            DailyGiftView()
        case .comeback(let info):
            ComebackView(info: info)
        case .milestone(let info):
            MilestoneView(info: info)
        case .repair(let info):
            RepairView(info: info)
        case .chest(let type, let drops):
            ChestOpenView(type: type, drops: drops)
        case .reveal(let skinID):
            SkinRevealView(skinID: skinID)
        case .leagueResult(let pending):
            InfoPopup(title: "Liga: \(pending.to)", text: "Ganaste \(pending.coins) monedas la semana pasada.", symbol: "trophy.fill")
        case .info(let title, let text, let symbol):
            InfoPopup(title: title, text: text, symbol: symbol)
        case .notifPrimer:
            NotifPrimerView()
        case .whatsNew:
            WhatsNewView()
        case .profileSetup:
            ProfileSetupView()
        }
    }
}

/// Marco común de los avisos.
struct PopupFrame<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 14) {
            Text(loc(title))
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
                .multilineTextAlignment(.center)
            content
        }
        .padding(22)
        .frame(maxWidth: 340)
        .background(RoundedRectangle(cornerRadius: 30, style: .continuous).fill(Theme.cream))
        .padding(24)
    }
}

struct InfoPopup: View {
    @EnvironmentObject var model: AppModel
    let title: String
    let text: String
    let symbol: String

    var body: some View {
        PopupFrame(title: title) {
            Image(systemName: symbol)
                .font(.system(size: 44, weight: .bold))
                .foregroundColor(Theme.gold)
            Text(loc(text))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.7))
                .multilineTextAlignment(.center)
            BigButton(title: "¡GENIAL!") { model.dismissCelebration() }
        }
    }
}

// MARK: - Regalo diario

struct DailyGiftView: View {
    @EnvironmentObject var model: AppModel
    @State private var claimedCoins: Int? = nil
    @State private var claimedGems: Int = 0
    @State private var doubled = false

    var body: some View {
        PopupFrame(title: "Regalo de hoy") {
            if let state = model.state {
                Text("Día \(state.dailyReward.day) de tu racha")
                    .font(.system(size: 13, weight: .heavy))
                    .kerning(1.5)
                    .foregroundColor(Theme.ink.opacity(0.5))
                cycleRow(state)
                VStack(spacing: 6) {
                    HStack(spacing: 18) {
                        HStack(spacing: 6) {
                            CoinIcon(size: 30)
                            Text("\(state.dailyReward.coins)")
                        }
                        if state.dailyReward.gems > 0 {
                            HStack(spacing: 6) {
                                GemIcon(size: 30)
                                Text("\(state.dailyReward.gems)")
                            }
                        }
                    }
                    .font(.system(size: 26, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                    if state.dailyReward.isChest {
                        Text("¡Día de cofre! Sigue la racha para mejores premios.")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(Theme.ink.opacity(0.55))
                    }
                }
                if let coins = claimedCoins {
                    if doubled {
                        Text("¡Duplicado! +\(coins) monedas" + (claimedGems > 0 ? " y +\(claimedGems) gemas" : ""))
                            .font(.system(size: 14, weight: .black, design: .rounded))
                            .foregroundColor(Theme.accent)
                            .multilineTextAlignment(.center)
                    } else if state.ads?.doubleGift ?? false {
                        WatchAdButton(
                            title: "DUPLICAR EL REGALO",
                            subtitle: "+\(coins) monedas" + (claimedGems > 0 ? " y +\(claimedGems) gemas" : "") + " viendo un anuncio corto",
                            tag: "x2",
                            color: Color(hex: 0x8E6BD6)
                        ) {
                            model.adDoubleGift()
                            doubled = true
                        }
                    }
                    BigButton(title: "LISTO") {
                        model.dismissCelebration()
                    }
                } else {
                    BigButton(title: "RECLAMAR") {
                        claimedCoins = state.dailyReward.coins
                        claimedGems = state.dailyReward.gems
                        model.claimDailyGift()
                    }
                }
            }
        }
    }

    private func cycleRow(_ state: MetaState) -> some View {
        HStack(spacing: 4) {
            ForEach(0..<state.cycle.count, id: \.self) { i in
                let isToday = i + 1 == state.dailyReward.day
                let past = i + 1 < state.dailyReward.day
                VStack(spacing: 2) {
                    if state.cycle[i].isChest {
                        ChestIcon(type: "RARE", size: 22)
                    } else {
                        CoinIcon(size: 18)
                    }
                    Text("\(i + 1)")
                        .font(.system(size: 9, weight: .heavy))
                        .foregroundColor(Theme.ink.opacity(0.5))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(isToday ? Theme.gold.opacity(0.3) : Theme.ink.opacity(0.05)))
                .opacity(past ? 0.45 : 1)
            }
        }
    }
}

struct ComebackView: View {
    @EnvironmentObject var model: AppModel
    let info: ComebackInfo

    var body: some View {
        PopupFrame(title: "¡Qué bueno verte!") {
            SpriteImage(name: "ico_gift", size: 70)
            Text("Te echamos de menos. Aquí tienes un regalo por volver:")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.65))
                .multilineTextAlignment(.center)
            HStack(spacing: 14) {
                HStack(spacing: 4) { CoinIcon(size: 24); Text("\(info.coins)") }
                HStack(spacing: 4) { ChestIcon(type: info.chest, size: 30); Text("1") }
                if info.freezes > 0 {
                    HStack(spacing: 4) {
                        Image(systemName: "shield.fill").foregroundColor(Color(hex: 0x4E8FA6))
                        Text("\(info.freezes)")
                    }
                }
            }
            .font(.system(size: 20, weight: .black, design: .rounded))
            .foregroundColor(Theme.ink)
            BigButton(title: "¡GRACIAS!") { model.dismissCelebration() }
        }
    }
}

struct MilestoneView: View {
    @EnvironmentObject var model: AppModel
    let info: StreakMilestoneInfo

    var body: some View {
        PopupFrame(title: "¡\(info.days) días de racha!") {
            Image(systemName: "flame.fill")
                .font(.system(size: 54))
                .foregroundColor(Theme.energy)
            Text("Premio por tu constancia")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.6))
            HStack(spacing: 14) {
                HStack(spacing: 4) { CoinIcon(size: 24); Text("\(info.coins)") }
                if info.gems > 0 { HStack(spacing: 4) { GemIcon(size: 24); Text("\(info.gems)") } }
                if let chest = info.chest { ChestIcon(type: chest, size: 32) }
            }
            .font(.system(size: 20, weight: .black, design: .rounded))
            .foregroundColor(Theme.ink)
            BigButton(title: "¡SIGO!") { model.dismissCelebration() }
        }
    }
}

struct RepairView: View {
    @EnvironmentObject var model: AppModel
    let info: RepairInfo

    var body: some View {
        PopupFrame(title: "¡Tu racha se apagó!") {
            Image(systemName: "flame")
                .font(.system(size: 54))
                .foregroundColor(Theme.ink.opacity(0.4))
            Text("Perdiste una racha de \(info.lost) días por poco. Puedes recuperarla hoy.")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.65))
                .multilineTextAlignment(.center)
            BigButton(title: "RECUPERAR · \(info.cost) GEMAS", color: Theme.energy, enabled: model.gems >= info.cost) {
                model.repairStreak()
                model.dismissCelebration()
            }
            WatchAdButton(
                title: "RECUPERAR VIENDO UN ANUNCIO",
                subtitle: "Gratis · sin gastar gemas",
                tag: "GRATIS",
                color: Color(hex: 0x8E6BD6)
            ) {
                model.adRepairStreak()
            }
            Button(action: {
                model.declineRepair()
                model.dismissCelebration()
            }) {
                Text("No, gracias")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.55))
            }
        }
    }
}

struct SkinRevealView: View {
    @EnvironmentObject var model: AppModel
    let skinID: String

    var body: some View {
        let skin = model.skin(skinID)
        return PopupFrame(title: "¡Skin desbloqueada!") {
            SkinSwatch(skin: skin, size: 110)
            Text(skin?.name ?? "Skin")
                .font(.system(size: 22, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text(rarityName(skin?.rarity ?? "COMMON"))
                .font(.system(size: 12, weight: .heavy))
                .kerning(2)
                .foregroundColor(rarityColor(skin?.rarity ?? "COMMON"))
            BigButton(title: "EQUIPAR AHORA") {
                model.equipSkin(skinID)
                model.dismissCelebration()
            }
            Button(action: { model.dismissCelebration() }) {
                Text("Luego")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.55))
            }
        }
    }
}

// MARK: - Apertura de cofres

struct ChestOpenView: View {
    @EnvironmentObject var model: AppModel
    let type: String
    let drops: [DropInfo]
    @State private var opened = false
    @State private var revealed = 0
    @State private var shake = false
    private let timer = Timer.publish(every: 0.55, on: .main, in: .common).autoconnect()

    private var columns: [GridItem] {
        [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
    }

    var body: some View {
        VStack(spacing: 16) {
            if !opened {
                Spacer()
                ChestIcon(type: type == "SERIES" ? "RARE" : type, size: 150)
                    .rotationEffect(.degrees(shake ? 5 : -5))
                    .onAppear {
                        withAnimation(Animation.easeInOut(duration: 0.12).repeatForever(autoreverses: true)) { shake = true }
                    }
                Text(type == "SERIES" ? "Sobre de serie" : chestName(type))
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text("Toca para abrir")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Color.white.opacity(0.7))
                Spacer()
            } else {
                Text("¡Mira lo que salió!")
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.top, 40)
                ScrollView(showsIndicators: false) {
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(0..<drops.count, id: \.self) { i in
                            dropCard(i)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 8)
                }
                if revealed >= drops.count {
                    summary
                    BigButton(title: "RECOGER") { model.dismissCelebration() }
                        .padding(.horizontal, 40)
                        .padding(.bottom, 30)
                } else {
                    Button(action: { revealed = drops.count }) {
                        Text("Mostrar todas")
                            .font(.system(size: 14, weight: .heavy, design: .rounded))
                            .foregroundColor(Color.white.opacity(0.8))
                    }
                    .padding(.bottom, 30)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(Rectangle())
        .onTapGesture {
            if !opened {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) { opened = true }
            }
        }
        .onReceive(timer) { _ in
            if opened && revealed < drops.count {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.65)) { revealed += 1 }
            }
        }
    }

    @ViewBuilder
    private func dropCard(_ index: Int) -> some View {
        let drop = drops[index]
        if index < revealed, let piece = model.piece(drop.id) {
            VStack(spacing: 6) {
                PieceCard(piece: piece, series: model.series(piece.series), owned: true, isNew: drop.new, width: 128)
                if !drop.new {
                    Text("Repetida · +\(drop.shards) esencia o vender")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(Color.white.opacity(0.75))
                        .multilineTextAlignment(.center)
                }
                if drop.bonus {
                    Text("¡Carta extra de suerte!")
                        .font(.system(size: 10, weight: .heavy))
                        .foregroundColor(Theme.gold)
                }
            }
            .transition(.scale)
        } else {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.white.opacity(0.12))
                .frame(width: 128, height: 171)
                .overlay(Text("?").font(.system(size: 50, weight: .black, design: .rounded)).foregroundColor(Color.white.opacity(0.4)))
        }
    }

    private var summary: some View {
        let fresh = drops.filter { $0.new }.count
        let text = fresh > 0 ? "\(fresh) \(fresh == 1 ? "pieza nueva" : "piezas nuevas") para tu álbum" : "Esta vez todo repetido: véndelas o recíclalas en el álbum"
        return Text(loc(text))
            .font(.system(size: 13, weight: .bold))
            .foregroundColor(Color.white.opacity(0.85))
            .multilineTextAlignment(.center)
            .padding(.horizontal, 30)
    }
}
