import SwiftUI

func tierColor(_ tier: String) -> Color {
    switch tier {
    case "SILVER": return Color(hex: 0xA9B3C6)
    case "GOLD": return Color(hex: 0xE0A93B)
    case "DIAMOND": return Color(hex: 0x4FC3F7)
    default: return Color(hex: 0xB9792F)
    }
}

func tierName(_ tier: String) -> String {
    switch tier {
    case "SILVER": return "Plata"
    case "GOLD": return "Oro"
    case "DIAMOND": return "Diamante"
    default: return "Bronce"
    }
}

// MARK: - Aviso de logro

/// Letrero que baja desde arriba cuando se desbloquea un logro.
struct AchievementBanner: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack {
            if let ach = model.achBanner {
                HStack(spacing: 12) {
                    Image(systemName: "trophy.fill")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(rarityColor(ach.rarity))
                        .frame(width: 44, height: 44)
                        .background(Circle().fill(Color.white))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("LOGRO DESBLOQUEADO")
                            .font(.system(size: 9, weight: .heavy))
                            .kerning(2)
                            .foregroundColor(Color.white.opacity(0.7))
                        Text(ach.title)
                            .font(.system(size: 16, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                        HStack(spacing: 8) {
                            HStack(spacing: 3) { CoinIcon(size: 13); Text("+\(ach.coins)") }
                            if ach.gems > 0 { HStack(spacing: 3) { GemIcon(size: 13); Text("+\(ach.gems)") } }
                            if ach.chest != nil { ChestIcon(type: ach.chest ?? "COMMON", size: 18) }
                        }
                        .font(.system(size: 11, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                    }
                    Spacer()
                }
                .padding(12)
                .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Theme.ink.opacity(0.95)))
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .transition(.move(edge: .top).combined(with: .opacity))
            }
            Spacer()
        }
        .allowsHitTesting(false)
        .animation(.spring(response: 0.4, dampingFraction: 0.8), value: model.achBanner?.id)
        .zIndex(25)
    }
}

// MARK: - Logros

struct AchievementsSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var data: AchListData?
    @State private var filter: String = "ALL"

    private let filters: [(String, String)] = [("ALL", "Todos"), ("DONE", "Logrados"), ("TODO", "Pendientes")]

    var body: some View {
        VStack(spacing: 12) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            Text("Logros")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            if let data = data {
                summary(data)
                filterRow
                ScrollView(showsIndicators: false) {
                    LazyVStack(spacing: 8) {
                        ForEach(visible(data)) { item in
                            row(item)
                        }
                    }
                    .padding(.bottom, 24)
                }
            }
        }
        .padding(.horizontal, 18)
        .background(Theme.cream.ignoresSafeArea())
        .onAppear { data = model.loadAchievements() }
    }

    private func visible(_ data: AchListData) -> [AchItem] {
        switch filter {
        case "DONE": return data.list.filter { $0.unlocked }
        case "TODO": return data.list.filter { !$0.unlocked }
        default: return data.list
        }
    }

    private func summary(_ data: AchListData) -> some View {
        let fraction = Double(data.done) / Double(max(1, data.total))
        return VStack(spacing: 8) {
            HStack {
                Text("\(data.done) / \(data.total)")
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Text("Cada logro paga monedas; los épicos y legendarios, también un cofre")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .multilineTextAlignment(.trailing)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.08))
                    Capsule().fill(LinearGradient(colors: [Theme.accent, Theme.gold], startPoint: .leading, endPoint: .trailing))
                        .frame(width: max(8, geo.size.width * CGFloat(fraction)))
                }
            }
            .frame(height: 9)
        }
        .padding(14)
        .card()
    }

    private var filterRow: some View {
        HStack(spacing: 8) {
            ForEach(0..<filters.count, id: \.self) { i in
                let item = filters[i]
                Button(action: { filter = item.0 }) {
                    Text(item.1)
                        .font(.system(size: 12, weight: .heavy, design: .rounded))
                        .foregroundColor(filter == item.0 ? .white : Theme.ink.opacity(0.55))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(Capsule().fill(filter == item.0 ? Theme.accent : Color.white))
                }
            }
            Spacer()
        }
    }

    private func row(_ item: AchItem) -> some View {
        HStack(spacing: 12) {
            Image(systemName: item.unlocked ? "trophy.fill" : "lock.fill")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(item.unlocked ? tierColor(item.tier) : Theme.ink.opacity(0.25))
                .frame(width: 42, height: 42)
                .background(Circle().fill(item.unlocked ? tierColor(item.tier).opacity(0.16) : Theme.ink.opacity(0.06)))
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(item.unlocked ? 1 : 0.6))
                Text(item.desc)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .lineLimit(2)
            }
            Spacer(minLength: 4)
            VStack(alignment: .trailing, spacing: 2) {
                Text(tierName(item.tier).uppercased())
                    .font(.system(size: 8, weight: .heavy))
                    .kerning(1)
                    .foregroundColor(tierColor(item.tier))
                HStack(spacing: 3) { CoinIcon(size: 12); Text("\(item.coins)") }
                    .font(.system(size: 10, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.6))
                if item.gems > 0 {
                    HStack(spacing: 3) { GemIcon(size: 12); Text("\(item.gems)") }
                        .font(.system(size: 10, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.ink.opacity(0.6))
                }
            }
        }
        .padding(12)
        .card(radius: 18)
    }
}

// MARK: - Prestigio

struct PrestigeSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var data: PrestigeData?
    @State private var group: String = "Colección"

    private let groups = ["Colección", "Campaña", "Torre", "Maestría", "Social", "Trofeos"]

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                Text("Prestigio")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                if let data = data {
                    rankCard(data)
                    trophyCard(data)
                    platinumCard(data)
                    milestonesCard(data)
                    titlesCard(data)
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 30)
        }
        .background(Theme.cream.ignoresSafeArea())
        .onAppear { data = model.loadPrestige() }
    }

    private func rankCard(_ data: PrestigeData) -> some View {
        VStack(spacing: 10) {
            Image(systemName: "crown.fill")
                .font(.system(size: 34))
                .foregroundColor(Theme.gold)
            Text(data.rank.uppercased())
                .font(.system(size: 26, weight: .black, design: .rounded))
                .kerning(2)
                .foregroundColor(Theme.ink)
            Text("\(data.score) puntos de prestigio")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Theme.ink.opacity(0.55))
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.08))
                    Capsule().fill(LinearGradient(colors: [Theme.accent, Theme.gold], startPoint: .leading, endPoint: .trailing))
                        .frame(width: max(8, geo.size.width * CGFloat(data.progress)))
                }
            }
            .frame(height: 10)
            if let next = data.nextRank {
                Text("Faltan \(data.pointsToNext) puntos para \(next)")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.accent)
            } else {
                Text("¡Rango máximo!")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.accent)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(data.ranks) { row in
                        VStack(spacing: 3) {
                            Text(row.title)
                                .font(.system(size: 11, weight: .black, design: .rounded))
                                .foregroundColor(row.title == data.rank ? .white : Theme.ink)
                            Text("\(row.min)")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(row.title == data.rank ? Color.white.opacity(0.85) : Theme.ink.opacity(0.45))
                            if row.coins > 0 {
                                Text("+\(row.coins) monedas · \(row.gems) gemas")
                                    .font(.system(size: 8, weight: .heavy))
                                    .foregroundColor(row.title == data.rank ? Color.white.opacity(0.85) : Theme.ink.opacity(0.45))
                            }
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(row.title == data.rank ? Theme.accent : Theme.ink.opacity(0.06)))
                    }
                }
            }
        }
        .padding(16)
        .card()
    }

    private func trophyCard(_ data: PrestigeData) -> some View {
        VStack(spacing: 10) {
            HStack {
                Text("Trofeos")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Button(action: { model.switchSheet(.achievements) }) {
                    Text("Ver logros")
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundColor(Theme.accent)
                }
            }
            HStack(spacing: 8) {
                ForEach(data.tiers) { tier in
                    VStack(spacing: 3) {
                        Image(systemName: "trophy.fill").foregroundColor(tierColor(tier.tier))
                        Text("\(tier.count)")
                            .font(.system(size: 18, weight: .black, design: .rounded))
                            .foregroundColor(Theme.ink)
                        Text(tier.label)
                            .font(.system(size: 9, weight: .heavy))
                            .foregroundColor(Theme.ink.opacity(0.45))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(tierColor(tier.tier).opacity(0.12)))
                }
            }
            Text("\(data.trophies) de \(data.trophiesTotal) logros")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.5))
        }
        .padding(14)
        .card()
    }

    private func platinumCard(_ data: PrestigeData) -> some View {
        let fraction = Double(data.platDone) / Double(max(1, data.platTotal))
        return VStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "star.circle.fill").font(.system(size: 26)).foregroundColor(Color(hex: 0x9AA3B5))
                VStack(alignment: .leading, spacing: 2) {
                    Text(data.platinum ? "¡PLATINO CONSEGUIDO!" : "El Platino")
                        .font(.system(size: 15, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text("Todos los logros y todos los hitos · +\(data.platCoins) monedas y +\(data.platGems) gemas")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.08))
                    Capsule().fill(Color(hex: 0x9AA3B5)).frame(width: max(8, geo.size.width * CGFloat(fraction)))
                }
            }
            .frame(height: 8)
            Text("\(data.platDone) / \(data.platTotal)")
                .font(.system(size: 11, weight: .heavy))
                .foregroundColor(Theme.ink.opacity(0.5))
        }
        .padding(14)
        .card()
    }

    private func milestonesCard(_ data: PrestigeData) -> some View {
        let list = data.milestones.filter { $0.group == group }
        return VStack(spacing: 10) {
            HStack {
                Text("Hitos")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Text("\(data.milestonesDone)/\(data.milestonesTotal)")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<groups.count, id: \.self) { i in
                        Button(action: { group = groups[i] }) {
                            Text(groups[i])
                                .font(.system(size: 12, weight: .heavy, design: .rounded))
                                .foregroundColor(group == groups[i] ? .white : Theme.ink.opacity(0.55))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Capsule().fill(group == groups[i] ? Theme.ink : Theme.ink.opacity(0.06)))
                        }
                    }
                }
            }
            ForEach(list) { item in
                milestoneRow(item)
            }
        }
        .padding(14)
        .card()
    }

    private func milestoneRow(_ item: MilestoneItem) -> some View {
        let fraction = Double(item.progress) / Double(max(1, item.target))
        return HStack(spacing: 10) {
            Image(systemName: item.done ? "checkmark.seal.fill" : "seal")
                .font(.system(size: 20))
                .foregroundColor(item.done ? Theme.accent : Theme.ink.opacity(0.3))
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(item.done ? 0.55 : 1))
                Text(item.desc)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Theme.ink.opacity(0.08))
                        Capsule().fill(item.done ? Theme.accent : Theme.gold).frame(width: max(4, geo.size.width * CGFloat(fraction)))
                    }
                }
                .frame(height: 5)
            }
            VStack(alignment: .trailing, spacing: 2) {
                Text("\(item.progress)/\(item.target)")
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundColor(Theme.ink.opacity(0.5))
                HStack(spacing: 3) { CoinIcon(size: 11); Text("\(item.coins)") }
                    .font(.system(size: 9, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.55))
            }
        }
    }

    private func titlesCard(_ data: PrestigeData) -> some View {
        VStack(spacing: 10) {
            HStack {
                Text("Títulos")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Text("Luces: \(data.equippedTitle)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Theme.accent)
            }
            ForEach(data.titles) { item in
                titleRow(item)
            }
        }
        .padding(14)
        .card()
    }

    private func titleRow(_ item: TitleItem) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .foregroundColor(rarityColor(item.rarity))
                Text(item.how)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .lineLimit(2)
            }
            Spacer()
            if item.equipped {
                Text("PUESTO").font(.system(size: 10, weight: .heavy)).foregroundColor(Theme.accent)
            } else if item.owned {
                Button(action: {
                    model.equipTitle(item.id)
                    data = model.loadPrestige()
                }) {
                    Text("USAR")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(Theme.accent))
                }
            } else if item.price > 0 {
                Button(action: {
                    model.buyTitle(item.id)
                    data = model.loadPrestige()
                }) {
                    HStack(spacing: 3) {
                        GemIcon(size: 13)
                        Text("\(item.price)")
                    }
                    .font(.system(size: 11, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(model.gems >= item.price ? Theme.accent : Color.gray.opacity(0.5)))
                }
            } else {
                Image(systemName: "lock.fill").font(.system(size: 11)).foregroundColor(Theme.ink.opacity(0.3))
            }
        }
    }
}
