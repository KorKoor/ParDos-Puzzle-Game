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
    @State private var showConfetti = false
    @State private var prizeShown = false

    private let colors: [Color] = [
        Color(hex: 0xFFB347), Color(hex: 0xB27BFF), Color(hex: 0x7DE0A6), Color(hex: 0xFF7A59),
        Color(hex: 0x6EC6FF), Color(hex: 0xFFD36E), Color(hex: 0xF58FC1), Color(hex: 0x9AD65A)
    ]

    var body: some View {
        let slices = model.wheelSlices
        let freeLeft = model.state?.wheel.freeLeft ?? 0
        return ZStack {
        VStack(spacing: 16) {
            sheetHandle()
            SheetTitle("Ruleta diaria", symbol: "sparkles")
            ZStack(alignment: .top) {
                Circle()
                    .fill(RadialGradient(colors: [Color(hex: 0xE9B949).opacity(0.35), .clear], center: .center, startRadius: 100, endRadius: 190))
                    .frame(width: 380, height: 380)
                    .allowsHitTesting(false)
                wheel(slices)
                    .rotationEffect(.degrees(rotation))
                    .shadow(color: Color.black.opacity(0.25), radius: 16, y: 10)
                Image(systemName: "arrowtriangle.down.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(LinearGradient(colors: [Color(hex: 0xFFF1B5), Color(hex: 0xE9B949), Color(hex: 0xB07A1E)], startPoint: .top, endPoint: .bottom))
                    .shadow(color: Color.black.opacity(0.35), radius: 4, y: 3)
                    .offset(y: -20)
            }
            .padding(.top, 20)
            if !resultText.isEmpty {
                Text(resultText)
                    .font(.system(size: 22, weight: .black, design: .serif))
                    .foregroundColor(Theme.accent)
                    .shadow(color: Theme.accent.opacity(0.4), radius: 8)
                    .scaleEffect(prizeShown ? 1 : 0.4)
                    .opacity(prizeShown ? 1 : 0)
            }
            BigButton(title: spinning ? "GIRANDO…" : (freeLeft > 0 ? "GIRAR GRATIS" : "VUELVE MAÑANA"), enabled: freeLeft > 0 && !spinning) {
                spin(slices)
            }
            .padding(.horizontal, 40)
            if freeLeft == 0, let left = model.state?.ads?.wheel, left > 0, !spinning {
                WatchAdButton(
                    title: "OTRO GIRO CON ANUNCIO",
                    subtitle: "Ver un anuncio · quedan \(left) hoy",
                    tag: "+1",
                    color: Color(hex: 0x8E6BD6)
                ) {
                    spin(slices, ad: true)
                }
                .padding(.horizontal, 30)
            }
            Text("1 giro gratis al día. Puede tocarte monedas, gemas, un cofre o puntos del pase.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.45))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 30)
            Spacer(minLength: 0)
        }
            if showConfetti { ConfettiView().allowsHitTesting(false) }
        }
        .background(LuxSheetBackground())
    }

    private func wheel(_ slices: [WheelSliceInfo]) -> some View {
        let size: CGFloat = 270
        let rim: CGFloat = size + 30
        let count = max(1, slices.count)
        let gold = LinearGradient(colors: [Color(hex: 0xFFF1B5), Color(hex: 0xE9B949), Color(hex: 0xB07A1E)], startPoint: .topLeading, endPoint: .bottomTrailing)
        return ZStack {
            Circle().fill(gold).frame(width: rim, height: rim)
            Circle().strokeBorder(Color.white.opacity(0.55), lineWidth: 1.5).frame(width: rim, height: rim)
            ForEach(0..<18, id: \.self) { i in
                let a = Double(i) / 18.0 * 2.0 * Double.pi
                Circle()
                    .fill(Color.white)
                    .frame(width: 7, height: 7)
                    .shadow(color: Color(hex: 0xFFE27A), radius: 3)
                    .offset(x: CGFloat(cos(a)) * (rim / 2 - 8), y: CGFloat(sin(a)) * (rim / 2 - 8))
            }
            ZStack {
                ForEach(0..<slices.count, id: \.self) { i in
                    WheelSlicePath(index: i, count: count)
                        .fill(LinearGradient(colors: [colors[i % colors.count].shade(0.2), colors[i % colors.count].shade(-0.08)], startPoint: .top, endPoint: .bottom))
                    WheelSlicePath(index: i, count: count)
                        .stroke(Color.white.opacity(0.9), lineWidth: 2)
                }
                ForEach(0..<slices.count, id: \.self) { i in
                    sliceLabel(slices[i], size: size)
                        .rotationEffect(.degrees((Double(i) + 0.5) * 360.0 / Double(count)))
                }
            }
            .frame(width: size, height: size)
            ZStack {
                Circle().fill(gold)
                Circle().fill(LinearGradient(colors: [Color.white.opacity(0.5), Color.white.opacity(0)], startPoint: .top, endPoint: .center)).padding(2)
                Circle().strokeBorder(Color.white.opacity(0.7), lineWidth: 2)
                Image(systemName: "star.fill").font(.system(size: 18, weight: .black)).foregroundColor(.white)
                    .shadow(color: Color.black.opacity(0.25), radius: 1, y: 1)
            }
            .frame(width: 56, height: 56)
            .shadow(color: Color.black.opacity(0.3), radius: 5, y: 3)
        }
        .frame(width: rim, height: rim)
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

    private func spin(_ slices: [WheelSliceInfo], ad: Bool = false) {
        guard !spinning, let index = model.spinWheel(ad: ad), index < slices.count else { return }
        spinning = true
        resultText = ""
        prizeShown = false
        showConfetti = false
        // clics de la ruleta: muchos al principio y cada vez más espaciados al frenar
        for i in 1...22 {
            let u = Double(i) / 22.0
            let when = 4.0 * (1.0 - pow(1.0 - u, 1.0 / 3.0))
            DispatchQueue.main.asyncAfter(deadline: .now() + when) { Haptics.tap() }
        }
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
            Haptics.success()
            withAnimation(.spring(response: 0.5, dampingFraction: 0.55)) { prizeShown = true }
            showConfetti = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { showConfetti = false }
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
    private let gold = Color(hex: 0xE9B949)

    private var total: Int { max(1, model.tiers.count > 0 ? model.tiers.count : 80) }

    var body: some View {
        ScrollViewReader { proxy in
            VStack(spacing: 0) {
                sheetHandle()
                if let season = model.state?.season {
                    header(season, proxy)
                    ScrollView(showsIndicators: false) {
                        LazyVStack(spacing: 0) {
                            ForEach(model.tiers) { tier in
                                VStack(spacing: 0) {
                                    if (tier.tier - 1) % 10 == 0 { groupHeader(tier.tier) }
                                    tierRow(tier, season)
                                        .id(tier.tier)
                                }
                            }
                            finale
                        }
                        .padding(.horizontal, 14)
                        .padding(.bottom, 28)
                    }
                }
            }
            .background(LuxSheetBackground())
            .onAppear {
                model.loadTiers()
                scrollToMine(proxy, delay: 0.4)
            }
        }
    }

    private func scrollToMine(_ proxy: ScrollViewProxy, delay: Double) {
        let target = max(1, (model.state?.season.tier ?? 1))
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            withAnimation(.easeInOut(duration: 0.5)) { proxy.scrollTo(target, anchor: .center) }
        }
    }

    // MARK: Cabecera

    private func header(_ season: SeasonInfo, _ proxy: ScrollViewProxy) -> some View {
        let perTier = Double(max(1, model.eco?.pointsPerTier ?? 100))
        let within = Double(season.pointsInTier) / perTier
        let overall = min(1.0, (Double(season.tier) + within) / Double(total))
        return VStack(spacing: 12) {
            SheetTitle("Pase de \(season.name)", symbol: "crown.fill")
            HStack(spacing: 16) {
                ZStack {
                    Circle().stroke(Theme.ink.opacity(0.08), lineWidth: 10)
                    Circle()
                        .trim(from: 0, to: CGFloat(overall))
                        .stroke(LinearGradient(colors: [Theme.accent, gold], startPoint: .topLeading, endPoint: .bottomTrailing), style: StrokeStyle(lineWidth: 10, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .animation(.spring(response: 0.8, dampingFraction: 0.8), value: overall)
                    VStack(spacing: 0) {
                        Text("\(season.tier)")
                            .font(.system(size: 36, weight: .black, design: .serif))
                            .foregroundColor(Theme.ink)
                        Text("\(loc("de")) \(total)")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundColor(Theme.ink.opacity(0.5))
                    }
                }
                .frame(width: 108, height: 108)
                .shadow(color: gold.opacity(0.35), radius: 14)
                VStack(alignment: .leading, spacing: 8) {
                    Text("\(loc("Quedan")) \(season.daysLeft) \(loc("días"))")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.ink)
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(Theme.ink.opacity(0.1))
                            LuxFill(LinearGradient(colors: [Theme.accent, gold], startPoint: .leading, endPoint: .trailing), width: max(8, geo.size.width * CGFloat(within)))
                        }
                    }
                    .frame(height: 10)
                    Text("\(season.pointsInTier)/\(Int(perTier)) \(loc("puntos para el siguiente nivel"))")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.5))
                    Button(action: { scrollToMine(proxy, delay: 0) }) {
                        HStack(spacing: 5) {
                            Image(systemName: "location.fill")
                            Text(loc("Ir a mi nivel"))
                        }
                        .font(.system(size: 11, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.ink)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(LuxCapsule())
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            if let ads = model.state?.ads, ads.season > 0 {
                WatchAdButton(
                    title: "IMPULSO DE TEMPORADA",
                    subtitle: "Ver un anuncio · quedan \(ads.season) hoy",
                    tag: "+\(ads.seasonPoints)",
                    color: Color(hex: 0xE8772E)
                ) {
                    model.adSeasonBoost()
                }
            }
            HStack(spacing: 10) {
                BigButton(title: season.claimable > 0 ? "COBRAR TODO (\(season.claimable))" : "NADA POR COBRAR", enabled: season.claimable > 0) {
                    model.claimAllTiers()
                }
                if season.tier < total {
                    BigButton(title: "SUBIR · \(season.skipCost) GEMAS", color: Theme.energy, enabled: model.gems >= season.skipCost) {
                        model.buySeasonTier()
                    }
                }
            }
            if !season.premium {
                Button(action: { model.buyProduct("season_pass") }) {
                    HStack(spacing: 8) {
                        SpriteImage(name: "ico_crown", size: 26)
                        Text("Pase premium · " + model.priceLabel("season_pass", model.store?.specials.first(where: { $0.id == "season_pass" })?.price ?? "$4.99"))
                            .font(.system(size: 13, weight: .heavy, design: .rounded))
                    }
                    .foregroundColor(Color(hex: 0x4A3208))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        ZStack {
                            Capsule().fill(LinearGradient(colors: [Color(hex: 0xFFF1B5), gold, Color(hex: 0xC48A22)], startPoint: .top, endPoint: .bottom))
                            Capsule().strokeBorder(Color.white.opacity(0.6), lineWidth: 1)
                        }
                    )
                    .shimmer(radius: 24, period: 3.6)
                    .pulsingGlow(gold, radius: 12, strength: 0.8)
                }
                .buttonStyle(ToyPressStyle())
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
    }

    // MARK: Pista de niveles

    private func groupHeader(_ first: Int) -> some View {
        let last = min(total, first + 9)
        return HStack(spacing: 10) {
            Rectangle().fill(LinearGradient(colors: [.clear, gold.opacity(0.7)], startPoint: .leading, endPoint: .trailing)).frame(height: 1)
            Text("\(loc("NIVELES")) \(first)–\(last)")
                .font(.system(size: 10, weight: .heavy))
                .kerning(3)
                .foregroundColor(Theme.ink.opacity(0.5))
            Rectangle().fill(LinearGradient(colors: [gold.opacity(0.7), .clear], startPoint: .leading, endPoint: .trailing)).frame(height: 1)
        }
        .padding(.vertical, 14)
    }

    private var finale: some View {
        VStack(spacing: 6) {
            Image(systemName: "crown.fill").font(.system(size: 26)).foregroundColor(gold)
                .shadow(color: gold.opacity(0.7), radius: 10)
            Text(loc("GRAN FINAL"))
                .font(.system(size: 12, weight: .heavy))
                .kerning(4)
                .foregroundColor(Theme.ink.opacity(0.55))
            Text("\(loc("Llegaste al nivel")) \(total)")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.4))
        }
        .padding(.top, 18)
    }

    private func tierRow(_ tier: SeasonTierInfo, _ season: SeasonInfo) -> some View {
        let reached = tier.tier <= season.tier
        let isCurrent = tier.tier == season.tier + 1
        let milestone = tier.tier % 5 == 0
        let big = tier.tier == 30 || tier.tier == total
        return HStack(alignment: .center, spacing: 6) {
            rewardCell(tier.free, tier: tier.tier, premium: false, season: season, reached: reached)
            spine(tier.tier, reached: reached, current: isCurrent, milestone: milestone, big: big)
            rewardCell(tier.premium, tier: tier.tier, premium: true, season: season, reached: reached)
        }
        .padding(.vertical, 4)
    }

    /// Columna central: la línea dorada que se enciende hasta tu nivel y el nodo de cada nivel (más grande cada 5, corona en 30 y en el final).
    private func spine(_ tier: Int, reached: Bool, current: Bool, milestone: Bool, big: Bool) -> some View {
        let size: CGFloat = big ? 50 : (milestone ? 42 : 34)
        let lineColor: LinearGradient = reached
            ? LinearGradient(colors: [gold, Color(hex: 0xC48A22)], startPoint: .top, endPoint: .bottom)
            : LinearGradient(colors: [Theme.ink.opacity(0.12), Theme.ink.opacity(0.12)], startPoint: .top, endPoint: .bottom)
        let fill: LinearGradient = reached
            ? LinearGradient(colors: [Theme.accent.shade(0.2), Theme.accent, Theme.accentDark], startPoint: .top, endPoint: .bottom)
            : LinearGradient(colors: [Color.white, Theme.paper], startPoint: .top, endPoint: .bottom)
        return ZStack {
            Rectangle().fill(lineColor).frame(width: 4).frame(maxHeight: .infinity)
            ZStack {
                Circle().fill(fill)
                Circle().strokeBorder(reached ? gold : Theme.ink.opacity(0.15), lineWidth: milestone ? 3 : 2)
                if big {
                    Image(systemName: "crown.fill").font(.system(size: 20, weight: .bold)).foregroundColor(reached ? .white : gold)
                } else {
                    Text("\(tier)")
                        .font(.system(size: milestone ? 16 : 14, weight: .black, design: .serif))
                        .foregroundColor(reached ? .white : Theme.ink.opacity(0.45))
                }
            }
            .frame(width: size, height: size)
            .shadow(color: reached ? gold.opacity(0.45) : Color.clear, radius: milestone ? 10 : 6)
            .overlay(
                Circle()
                    .stroke(Theme.accent.opacity(current ? 0.6 : 0), lineWidth: 3)
                    .frame(width: size + 10, height: size + 10)
            )
        }
        .frame(width: 52)
    }

    private func rewardCell(_ reward: SeasonRewardInfo, tier: Int, premium: Bool, season: SeasonInfo, reached: Bool) -> some View {
        let key = (premium ? "p" : "f") + "\(tier)"
        let claimed = season.claimed.contains(key)
        let locked = premium && !season.premium
        let canClaim = reached && !claimed && !locked
        let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
        return Button(action: { if canClaim { model.claimTier(tier, premium: premium) } }) {
            VStack(spacing: 4) {
                HStack(spacing: 4) {
                    if premium { Image(systemName: "crown.fill").font(.system(size: 8)).foregroundColor(gold) }
                    Text(premium ? "PREMIUM" : "GRATIS")
                        .font(.system(size: 8, weight: .heavy))
                        .kerning(1.5)
                        .foregroundColor(premium ? Color(hex: 0xB07A1E) : Theme.ink.opacity(0.4))
                    if locked { Image(systemName: "lock.fill").font(.system(size: 8)).foregroundColor(Theme.ink.opacity(0.4)) }
                    if claimed { Image(systemName: "checkmark.circle.fill").font(.system(size: 10)).foregroundColor(Theme.accent) }
                }
                RewardChips(reward: reward)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
            .padding(.horizontal, 4)
            .background(
                ZStack {
                    shape.fill(LinearGradient(colors: cellColors(premium: premium, canClaim: canClaim), startPoint: .top, endPoint: .bottom))
                    shape.strokeBorder(
                        LinearGradient(colors: [canClaim ? Theme.accent : (premium ? gold.opacity(0.7) : Color.white.opacity(0.9)), Theme.ink.opacity(0.06)], startPoint: .top, endPoint: .bottom),
                        lineWidth: canClaim ? 2 : 1
                    )
                }
            )
            .shadow(color: canClaim ? Theme.accent.opacity(0.4) : Theme.ink.opacity(0.06), radius: canClaim ? 10 : 4, x: 0, y: 3)
            .opacity(claimed ? 0.5 : (locked ? 0.72 : 1))
        }
        .buttonStyle(ToyPressStyle())
    }

    private func cellColors(premium: Bool, canClaim: Bool) -> [Color] {
        if canClaim { return [Theme.accent.opacity(0.22), Theme.accent.opacity(0.10)] }
        if premium { return [Color(hex: 0xFFF6DC), Color(hex: 0xFBEBC2)] }
        return [Color.white, Theme.cream]
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
                SheetTitle("Liga \(league.name)", symbol: "trophy.fill")
                if let pending = league.pending {
                    pendingCard(pending)
                }
                progressCard(league).rise(0.05)
                ladderCard(league).rise(0.12)
                Text("Cada semana cuentan las estrellas que ganes. Al cerrar la semana subes, te quedas o bajas, y cobras monedas por cada estrella.")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.45))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 30)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .background(LuxSheetBackground())
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
            ZStack {
                LightRays(color: Theme.gold).frame(width: 130, height: 130)
                LuxIcon("trophy.fill", tint: Theme.gold, size: 54)
            }
            .frame(height: 96)
            Text(loc(title))
                .font(.system(size: 20, weight: .black, design: .serif))
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
                    .font(.system(size: 26, weight: .black, design: .serif))
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
                    LuxFill(Theme.gold, width: max(8, geo.size.width * CGFloat(fraction)))
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

    private func leagueColor(_ name: String) -> Color {
        switch name {
        case "Bronce": return Color(hex: 0xB5733A)
        case "Plata": return Color(hex: 0xA9B0BE)
        case "Oro": return Color(hex: 0xE0A93B)
        case "Zafiro": return Color(hex: 0x3F6FE0)
        case "Rubí": return Color(hex: 0xD6366B)
        default: return Color(hex: 0x5FD0E8)
        }
    }

    private func ladderCard(_ league: LeagueInfo) -> some View {
        VStack(spacing: 6) {
            ForEach(0..<ladder.count, id: \.self) { i in
                let row = ladder[ladder.count - 1 - i]
                let current = row.0 == league.name
                let tint = leagueColor(row.0)
                HStack(spacing: 10) {
                    LuxIcon("shield.fill", tint: tint, size: current ? 32 : 26)
                        .opacity(current ? 1 : 0.7)
                    Text(row.0)
                        .font(.system(size: 14, weight: current ? .black : .bold, design: .rounded))
                        .foregroundColor(Theme.ink.opacity(current ? 1 : 0.55))
                    Spacer()
                    Text("sube con \(row.1) estrellas")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.4))
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(current ? tint.opacity(0.14) : Color.clear)
                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(current ? tint.opacity(0.5) : Color.clear, lineWidth: 1.2))
                )
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
            SheetTitle("Más ayudas", symbol: "bag.fill")
            CurrencyBar().padding(.horizontal, 20)
            if let eco = model.eco {
                helpRow("arrow.uturn.backward", "Deshacer ×3", "Vuelve a la jugada anterior", eco.undoPrice * 3, "tienes \(model.state?.undos ?? 0)") { model.buyUndos(3) }
                helpRow("timer", "Tiempo extra ×3", "+20 segundos en los niveles con reloj", eco.extraTimePrice * 3, "tienes \(model.state?.extraTimes ?? 0)") { model.buyExtraTimes(3) }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .background(LuxSheetBackground())
    }

    private func helpRow(_ symbol: String, _ title: String, _ detail: String, _ price: Int, _ have: String, _ action: @escaping () -> Void) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Theme.accent)
                .frame(width: 44, height: 44)
                .background(Circle().fill(Theme.accent.opacity(0.12)))
            VStack(alignment: .leading, spacing: 2) {
                Text(loc(title)).font(.system(size: 15, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
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
