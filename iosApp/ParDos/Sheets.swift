import SwiftUI

private func sheetHandle() -> some View {
    Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
}

// MARK: - Ruleta

struct WheelSlicePath: Shape {
    let index: Int
    let count: Int

    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2
        let step = 2.0 * Double.pi / Double(count)
        let start = Double(index) * step - Double.pi / 2
        var p = Path()
        p.move(to: center)
        let segments = 14
        for i in 0...segments {
            let angle = start + step * Double(i) / Double(segments)
            p.addLine(to: CGPoint(x: center.x + radius * CGFloat(cos(angle)), y: center.y + radius * CGFloat(sin(angle))))
        }
        p.closeSubpath()
        return p
    }
}

struct WheelSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var rotation: Double = 0
    @State private var spinning = false
    @State private var resultText: String = ""

    private let colors: [Color] = [
        Color(hex: 0xFFB347), Color(hex: 0xB27BFF), Color(hex: 0x7DE0A6), Color(hex: 0xFF7A59),
        Color(hex: 0x6EC6FF), Color(hex: 0xFFD36E), Color(hex: 0xF58FC1), Color(hex: 0x9AD65A)
    ]

    var body: some View {
        let slices = model.wheelSlices
        let freeLeft = model.state?.wheel.freeLeft ?? 0
        return VStack(spacing: 16) {
            sheetHandle()
            Text("Ruleta diaria")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            ZStack(alignment: .top) {
                wheel(slices)
                    .rotationEffect(.degrees(rotation))
                Image(systemName: "arrowtriangle.down.fill")
                    .font(.system(size: 34))
                    .foregroundColor(Theme.ink)
                    .offset(y: -18)
            }
            .padding(.top, 20)
            if !resultText.isEmpty {
                Text(resultText)
                    .font(.system(size: 18, weight: .black, design: .rounded))
                    .foregroundColor(Theme.accent)
            }
            BigButton(title: spinning ? "GIRANDO…" : (freeLeft > 0 ? "GIRAR GRATIS" : "VUELVE MAÑANA"), enabled: freeLeft > 0 && !spinning) {
                spin(slices)
            }
            .padding(.horizontal, 40)
            Text("1 giro gratis al día. Puede tocarte monedas, gemas, un cofre o puntos del pase.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.45))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 30)
            Spacer(minLength: 0)
        }
        .background(Theme.cream.ignoresSafeArea())
    }

    private func wheel(_ slices: [WheelSliceInfo]) -> some View {
        let size: CGFloat = 270
        let count = max(1, slices.count)
        return ZStack {
            ForEach(0..<slices.count, id: \.self) { i in
                WheelSlicePath(index: i, count: count)
                    .fill(colors[i % colors.count])
                WheelSlicePath(index: i, count: count)
                    .stroke(Color.white, lineWidth: 2)
            }
            ForEach(0..<slices.count, id: \.self) { i in
                sliceLabel(slices[i], size: size)
                    .rotationEffect(.degrees((Double(i) + 0.5) * 360.0 / Double(count)))
            }
            Circle().fill(Color.white).frame(width: 44, height: 44)
            Circle().stroke(Theme.ink.opacity(0.2), lineWidth: 3).frame(width: 44, height: 44)
        }
        .frame(width: size, height: size)
        .overlay(Circle().stroke(Theme.ink, lineWidth: 6))
    }

    private func sliceLabel(_ slice: WheelSliceInfo, size: CGFloat) -> some View {
        VStack(spacing: 2) {
            sliceIcon(slice)
            Text(slice.label)
                .font(.system(size: 13, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
        }
        .frame(width: 70)
        .padding(.top, 16)
        .frame(width: size, height: size, alignment: .top)
    }

    @ViewBuilder
    private func sliceIcon(_ slice: WheelSliceInfo) -> some View {
        switch slice.kind {
        case "COINS": CoinIcon(size: 26)
        case "GEMS": GemIcon(size: 26)
        case "CHEST": ChestIcon(type: slice.chest ?? "COMMON", size: 30)
        default: Image(systemName: "crown.fill").font(.system(size: 20)).foregroundColor(Theme.gold)
        }
    }

    private func spin(_ slices: [WheelSliceInfo]) {
        guard !spinning, let index = model.spinWheel(), index < slices.count else { return }
        spinning = true
        resultText = ""
        let count = Double(max(1, slices.count))
        let centerAngle = (Double(index) + 0.5) * 360.0 / count
        let current = rotation.truncatingRemainder(dividingBy: 360.0)
        let target = rotation - current + 360.0 * 5.0 + (360.0 - centerAngle)
        withAnimation(.easeOut(duration: 4.0)) {
            rotation = target
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.2) {
            spinning = false
            resultText = prizeText(slices[index])
        }
    }

    private func prizeText(_ slice: WheelSliceInfo) -> String {
        switch slice.kind {
        case "COINS": return "¡+\(slice.amount) monedas!"
        case "GEMS": return "¡+\(slice.amount) gemas!"
        case "CHEST": return "¡Un \(chestName(slice.chest ?? "COMMON").lowercased())!"
        default: return "¡+\(slice.amount) puntos del pase!"
        }
    }
}

// MARK: - Pase de temporada

struct RewardChips: View {
    @EnvironmentObject var model: AppModel
    let reward: SeasonRewardInfo

    var body: some View {
        VStack(spacing: 3) {
            if reward.coins > 0 { chip { CoinIcon(size: 14); Text("\(reward.coins)") } }
            if reward.gems > 0 { chip { GemIcon(size: 14); Text("\(reward.gems)") } }
            if let chest = reward.chest { chip { ChestIcon(type: chest, size: 18); Text(chestName(chest).replacingOccurrences(of: "Cofre ", with: "")) } }
            if let skin = reward.skin { chip { Image(systemName: "paintpalette.fill").font(.system(size: 11)); Text(model.skin(skin)?.name ?? "Skin") } }
            if reward.avatar != 0 { chip { Image(systemName: "person.crop.circle.fill").font(.system(size: 11)); Text(model.avatar(reward.avatar)?.name ?? "Avatar") } }
            if reward.banner != 0 { chip { Image(systemName: "photo.fill").font(.system(size: 11)); Text(model.banner(reward.banner)?.name ?? "Banner") } }
            if reward.fx != nil { chip { Image(systemName: "sparkles").font(.system(size: 11)); Text("Efecto") } }
            if reward.freezes > 0 { chip { Image(systemName: "shield.fill").font(.system(size: 11)); Text("×\(reward.freezes)") } }
            if reward.undos > 0 { chip { Image(systemName: "arrow.uturn.backward").font(.system(size: 11)); Text("×\(reward.undos)") } }
            if reward.tokens > 0 { chip { Image(systemName: "arrow.left.arrow.right").font(.system(size: 11)); Text("×\(reward.tokens)") } }
        }
    }

    private func chip<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        HStack(spacing: 3) { content() }
            .font(.system(size: 10, weight: .heavy, design: .rounded))
            .foregroundColor(Theme.ink)
            .lineLimit(1)
    }
}

struct SeasonSheet: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack(spacing: 0) {
            sheetHandle()
            if let season = model.state?.season {
                header(season)
                ScrollView(showsIndicators: false) {
                    LazyVStack(spacing: 8) {
                        ForEach(model.tiers) { tier in
                            tierRow(tier, season)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Theme.cream.ignoresSafeArea())
        .onAppear { model.loadTiers() }
    }

    private func header(_ season: SeasonInfo) -> some View {
        let fraction = Double(season.pointsInTier) / Double(max(1, model.eco?.pointsPerTier ?? 100))
        return VStack(spacing: 10) {
            Text("Pase de \(season.name)")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text("Quedan \(season.daysLeft) días · Nivel \(season.tier) de 30")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(Theme.ink.opacity(0.55))
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.1))
                    Capsule().fill(LinearGradient(colors: [Theme.accent, Theme.gold], startPoint: .leading, endPoint: .trailing))
                        .frame(width: max(8, geo.size.width * CGFloat(fraction)))
                }
            }
            .frame(height: 10)
            Text("\(season.pointsInTier)/\(model.eco?.pointsPerTier ?? 100) puntos para el siguiente nivel · ganas puntos jugando, con misiones, ruleta y cofres")
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.45))
                .multilineTextAlignment(.center)
            HStack(spacing: 10) {
                BigButton(title: season.claimable > 0 ? "COBRAR TODO (\(season.claimable))" : "NADA POR COBRAR", enabled: season.claimable > 0) {
                    model.claimAllTiers()
                }
                if season.tier < 30 {
                    BigButton(title: "SUBIR · \(season.skipCost)💎", color: Theme.energy, enabled: model.gems >= season.skipCost) {
                        model.buySeasonTier()
                    }
                }
            }
            if !season.premium {
                Button(action: { model.unlockPremium() }) {
                    HStack(spacing: 8) {
                        SpriteImage(name: "ico_crown", size: 26)
                        Text("Pase premium · " + (model.store?.specials.first(where: { $0.id == "season_pass" })?.price ?? "$4.99") + " (prueba: gratis)")
                            .font(.system(size: 12, weight: .heavy, design: .rounded))
                    }
                    .foregroundColor(Theme.ink)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(Theme.gold.opacity(0.3)))
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
    }

    private func tierRow(_ tier: SeasonTierInfo, _ season: SeasonInfo) -> some View {
        let reached = tier.tier <= season.tier
        return HStack(spacing: 8) {
            Text("\(tier.tier)")
                .font(.system(size: 15, weight: .black, design: .rounded))
                .foregroundColor(reached ? .white : Theme.ink.opacity(0.4))
                .frame(width: 34, height: 34)
                .background(Circle().fill(reached ? Theme.accent : Theme.ink.opacity(0.08)))
            rewardCell(tier.free, tier: tier.tier, premium: false, season: season, reached: reached)
            rewardCell(tier.premium, tier: tier.tier, premium: true, season: season, reached: reached)
        }
    }

    private func rewardCell(_ reward: SeasonRewardInfo, tier: Int, premium: Bool, season: SeasonInfo, reached: Bool) -> some View {
        let key = (premium ? "p" : "f") + "\(tier)"
        let claimed = season.claimed.contains(key)
        let locked = premium && !season.premium
        let canClaim = reached && !claimed && !locked
        return Button(action: { if canClaim { model.claimTier(tier, premium: premium) } }) {
            VStack(spacing: 3) {
                HStack(spacing: 4) {
                    Text(premium ? "PREMIUM" : "GRATIS")
                        .font(.system(size: 8, weight: .heavy))
                        .kerning(1)
                        .foregroundColor(premium ? Theme.gold : Theme.ink.opacity(0.4))
                    if locked { Image(systemName: "lock.fill").font(.system(size: 8)).foregroundColor(Theme.ink.opacity(0.4)) }
                    if claimed { Image(systemName: "checkmark.circle.fill").font(.system(size: 10)).foregroundColor(Theme.accent) }
                }
                RewardChips(reward: reward)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(cellFill(premium: premium, canClaim: canClaim, claimed: claimed)))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(canClaim ? Theme.accent : Color.clear, lineWidth: 2))
            .opacity(claimed ? 0.55 : (locked ? 0.7 : 1))
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func cellFill(premium: Bool, canClaim: Bool, claimed: Bool) -> Color {
        if canClaim { return Theme.accent.opacity(0.14) }
        if premium { return Theme.gold.opacity(0.12) }
        return Color.white
    }
}

// MARK: - Liga

struct LeagueSheet: View {
    @EnvironmentObject var model: AppModel

    private let ladder: [(String, Int, Int)] = [
        ("Bronce", 20, 0), ("Plata", 30, 8), ("Oro", 40, 12), ("Zafiro", 55, 16), ("Rubí", 70, 22), ("Diamante", 85, 28)
    ]

    var body: some View {
        VStack(spacing: 14) {
            sheetHandle()
            if let league = model.state?.league {
                Text("Liga \(league.name)")
                    .font(.system(size: 26, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                if let pending = league.pending {
                    pendingCard(pending)
                }
                progressCard(league)
                ladderCard(league)
                Text("Cada semana cuentan las estrellas que ganes. Al cerrar la semana subes, te quedas o bajas, y cobras monedas por cada estrella.")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.45))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 30)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .background(Theme.cream.ignoresSafeArea())
    }

    private func pendingCard(_ pending: LeaguePending) -> some View {
        let title: String
        switch pending.outcome {
        case "PROMOTED": title = "¡Subiste a \(pending.to)!"
        case "DEMOTED": title = "Bajaste a \(pending.to)"
        case "TOP_HELD": title = "¡Sigues en la cima!"
        default: title = "Te quedas en \(pending.to)"
        }
        return VStack(spacing: 8) {
            Text(title)
                .font(.system(size: 17, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text("Semana pasada: \(pending.stars) estrellas")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.55))
            HStack(spacing: 12) {
                HStack(spacing: 4) { CoinIcon(size: 18); Text("\(pending.coins)") }
                if pending.gems > 0 { HStack(spacing: 4) { GemIcon(size: 18); Text("\(pending.gems)") } }
                if let chest = pending.chest { ChestIcon(type: chest, size: 24) }
            }
            .font(.system(size: 15, weight: .black, design: .rounded))
            .foregroundColor(Theme.ink)
            BigButton(title: "COBRAR PREMIO") { model.claimLeague() }
        }
        .padding(14)
        .card()
    }

    private func progressCard(_ league: LeagueInfo) -> some View {
        let fraction = min(1.0, Double(league.weekStars) / Double(max(1, league.promote)))
        return VStack(spacing: 8) {
            HStack {
                Text("ESTA SEMANA")
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(2)
                    .foregroundColor(Theme.ink.opacity(0.4))
                Spacer()
                Text("quedan \(league.weekDaysLeft) días")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.45))
            }
            HStack(spacing: 6) {
                Image(systemName: "star.fill").foregroundColor(Theme.gold)
                Text("\(league.weekStars) / \(league.promote)")
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                if let next = league.next {
                    Text("para subir a \(next)")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(Theme.accent)
                }
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.08))
                    Capsule().fill(Theme.gold).frame(width: max(8, geo.size.width * CGFloat(fraction)))
                    Rectangle().fill(Color(hex: 0xE0475B)).frame(width: 2, height: 14)
                        .offset(x: geo.size.width * CGFloat(min(1.0, Double(league.keep) / Double(max(1, league.promote)))))
                }
            }
            .frame(height: 10)
            if league.keep > 0 {
                Text("Necesitas al menos \(league.keep) estrellas para no bajar (marca roja).")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
        }
        .padding(14)
        .card()
    }

    private func ladderCard(_ league: LeagueInfo) -> some View {
        VStack(spacing: 6) {
            ForEach(0..<ladder.count, id: \.self) { i in
                let row = ladder[ladder.count - 1 - i]
                let current = row.0 == league.name
                HStack {
                    Image(systemName: current ? "chevron.right.circle.fill" : "circle")
                        .foregroundColor(current ? Theme.accent : Theme.ink.opacity(0.2))
                    Text(row.0)
                        .font(.system(size: 14, weight: current ? .black : .bold, design: .rounded))
                        .foregroundColor(Theme.ink.opacity(current ? 1 : 0.55))
                    Spacer()
                    Text("sube con \(row.1)★")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.4))
                }
            }
        }
        .padding(14)
        .card()
    }
}

// MARK: - Faltan ayudas

struct LowFundsSheet: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack(spacing: 14) {
            sheetHandle()
            Text("Más ayudas")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            CurrencyBar().padding(.horizontal, 20)
            if let eco = model.eco {
                helpRow("arrow.uturn.backward", "Deshacer ×3", "Vuelve a la jugada anterior", eco.undoPrice * 3, "tienes \(model.state?.undos ?? 0)") { model.buyUndos(3) }
                helpRow("timer", "Tiempo extra ×3", "+20 segundos en los niveles con reloj", eco.extraTimePrice * 3, "tienes \(model.state?.extraTimes ?? 0)") { model.buyExtraTimes(3) }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .background(Theme.cream.ignoresSafeArea())
    }

    private func helpRow(_ symbol: String, _ title: String, _ detail: String, _ price: Int, _ have: String, _ action: @escaping () -> Void) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Theme.accent)
                .frame(width: 44, height: 44)
                .background(Circle().fill(Theme.accent.opacity(0.12)))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 15, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                Text("\(detail) · \(have)").font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
            }
            Spacer()
            Button(action: action) {
                HStack(spacing: 4) {
                    CoinIcon(size: 16)
                    Text("\(price)")
                }
                .font(.system(size: 13, weight: .black, design: .rounded))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(Capsule().fill(Theme.accent))
            }
        }
        .padding(14)
        .card()
    }
}
