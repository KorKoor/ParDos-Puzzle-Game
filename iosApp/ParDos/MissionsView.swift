import SwiftUI

/// Misiones de hoy dentro de Inicio.
struct MissionsCard: View {
    @EnvironmentObject var model: AppModel
    let missions: [MissionInfo]
    let perfectDays: Int

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                Image(systemName: "checklist")
                    .foregroundColor(Theme.accent)
                Text("Misiones de hoy")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                if perfectDays > 0 {
                    HStack(spacing: 3) {
                        Image(systemName: "checkmark.seal.fill").foregroundColor(Theme.gold)
                        Text("\(perfectDays)")
                            .font(.system(size: 13, weight: .heavy, design: .rounded))
                            .foregroundColor(Theme.ink)
                    }
                }
                Button(action: { model.sheet = .missions }) {
                    Text("Ver todas")
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundColor(Theme.accent)
                }
            }
            ForEach(missions) { mission in
                MissionRow(mission: mission)
            }
            Text("Cobra las tres para ganar un cofre, gemas y una ficha de intercambio.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.45))
                .multilineTextAlignment(.center)
        }
        .padding(14)
        .card()
    }
}

struct MissionRow: View {
    @EnvironmentObject var model: AppModel
    let mission: MissionInfo

    var body: some View {
        let fraction = min(1.0, Double(mission.progress) / Double(max(1, mission.target)))
        return HStack(spacing: 12) {
            ZStack {
                Circle().fill(mission.claimed ? Theme.accent.opacity(0.18) : Theme.ink.opacity(0.06))
                    .frame(width: 38, height: 38)
                Image(systemName: mission.claimed ? "checkmark" : symbol(mission.type))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(mission.claimed ? Theme.accent : Theme.ink.opacity(0.6))
            }
            VStack(alignment: .leading, spacing: 5) {
                Text(mission.desc)
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(mission.claimed ? 0.45 : 1))
                    .strikethrough(mission.claimed)
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Theme.ink.opacity(0.08))
                        Capsule().fill(mission.done ? Theme.accent : Theme.gold)
                            .frame(width: max(5, geo.size.width * CGFloat(fraction)))
                    }
                }
                .frame(height: 6)
            }
            if mission.claimed {
                EmptyView()
            } else if mission.done {
                Button(action: { model.claimMission(mission.id) }) {
                    HStack(spacing: 4) {
                        CoinIcon(size: 16)
                        Text("\(mission.coins)")
                    }
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(Theme.accent))
                }
            } else {
                Text("\(min(mission.progress, mission.target))/\(mission.target)")
                    .font(.system(size: 12, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
        }
    }

    private func symbol(_ type: String) -> String {
        switch type {
        case "PLAY_GAMES": return "gamecontroller.fill"
        case "WIN_LEVELS": return "flag.fill"
        case "MERGE_PAIRS": return "plus.square.on.square"
        case "REACH_BLOCK": return "square.stack.3d.up.fill"
        case "EARN_STARS": return "star.fill"
        case "WIN_UNDER_TIME": return "timer"
        case "WIN_NO_POWERUPS": return "hand.raised.fill"
        default: return "circle.fill"
        }
    }
}

struct WeeklyRow: View {
    @EnvironmentObject var model: AppModel
    let item: WeeklyInfo

    var body: some View {
        let fraction = min(1.0, Double(item.progress) / Double(max(1, item.target)))
        return HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 5) {
                Text(item.title)
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(item.claimed ? 0.45 : 1))
                    .strikethrough(item.claimed)
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Theme.ink.opacity(0.08))
                        Capsule().fill(item.done ? Theme.accent : Color(hex: 0x4E8FA6))
                            .frame(width: max(5, geo.size.width * CGFloat(fraction)))
                    }
                }
                .frame(height: 6)
            }
            if item.claimed {
                Image(systemName: "checkmark.circle.fill").foregroundColor(Theme.accent)
            } else if item.done {
                Button(action: { model.claimWeekly(item.id) }) {
                    HStack(spacing: 4) {
                        CoinIcon(size: 16)
                        Text("\(item.coins)")
                    }
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(Theme.accent))
                }
            } else {
                Text("\(min(item.progress, item.target))/\(item.target)")
                    .font(.system(size: 12, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
        }
    }
}

/// Hoja con las misiones del día y las de la semana.
struct MissionsSheet: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                Text("Misiones")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                if let state = model.state {
                    daily(state)
                    weekly(state)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 30)
        }
        .background(Theme.cream.ignoresSafeArea())
    }

    private func daily(_ state: MetaState) -> some View {
        VStack(spacing: 12) {
            SectionTitle(text: "De hoy", detail: "cambian cada día")
            ForEach(state.missions) { mission in
                MissionRow(mission: mission)
            }
            HStack(spacing: 6) {
                Image(systemName: "checkmark.seal.fill").foregroundColor(Theme.gold)
                Text(state.perfectDays > 0 ? "\(state.perfectDays) \(state.perfectDays == 1 ? "día perfecto" : "días perfectos") seguidos" : "Cobra las tres y empieza una racha de días perfectos")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.6))
            }
        }
        .padding(14)
        .card()
    }

    private func weekly(_ state: MetaState) -> some View {
        VStack(spacing: 12) {
            SectionTitle(text: "De la semana", detail: "metas más largas")
            ForEach(state.weekly) { item in
                WeeklyRow(item: item)
            }
            if state.weeklyBonusClaimed {
                Text("Premio semanal cobrado")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.accent)
            } else {
                Button(action: { model.claimWeeklyBonus() }) {
                    HStack(spacing: 8) {
                        ChestIcon(type: "RARE", size: 26)
                        Text(state.weeklyBonusReady ? "COBRAR COFRE RARO + 8 GEMAS" : "Completa las cuatro: cofre raro + 8 gemas")
                    }
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .foregroundColor(state.weeklyBonusReady ? .white : Theme.ink.opacity(0.5))
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(state.weeklyBonusReady ? Theme.accent : Theme.ink.opacity(0.07)))
                }
                .disabled(!state.weeklyBonusReady)
            }
        }
        .padding(14)
        .card()
    }
}
