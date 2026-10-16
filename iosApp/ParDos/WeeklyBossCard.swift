import SwiftUI

struct WeeklyBossInfo: Decodable {
    let active: Bool
    let level: Int?
    let title: String?
    let motif: String?
    let taunt: String?
    let claimed: Bool?
    let coins: Int?
}

/// Jefe de la semana: uno de los jefes que ya venciste, otra vez. La primera victoria de la semana da monedas y un banner.
struct WeeklyBossCard: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        if let info = model.loadWeeklyBoss(), info.active, let level = info.level, let motif = info.motif {
            let claimed = info.claimed ?? false
            let pal = motifPalette(motif)
            Button(action: {
                if let card = model.card(level) { model.sheet = .level(card) }
            }) {
                HStack(spacing: 14) {
                    BossEmblemView(motif: motif, size: 74)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(loc("JEFE DE LA SEMANA"))
                            .font(.system(size: 10, weight: .heavy))
                            .kerning(2)
                            .foregroundColor(pal.glow.opacity(0.9))
                        Text(info.title ?? "")
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                        Text("\u{201C}\(info.taunt ?? "")\u{201D}")
                            .font(.system(size: 12, weight: .medium, design: .serif))
                            .foregroundColor(.white.opacity(0.7))
                            .lineLimit(2)
                        Text(claimed ? loc("Ya lo venciste esta semana") : "\(loc("Véncelo de nuevo")): +\(info.coins ?? 0) \(loc("monedas")) + banner")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundColor(claimed ? Color.white.opacity(0.5) : pal.glow)
                            .padding(.top, 2)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .black))
                        .foregroundColor(.white.opacity(0.6))
                }
                .padding(16)
                .background(
                    ZStack {
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .fill(LinearGradient(colors: [pal.bg, pal.bg.opacity(0.75)], startPoint: .topLeading, endPoint: .bottomTrailing))
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .stroke(pal.main.opacity(0.6), lineWidth: 1.5)
                    }
                )
                .shimmer(radius: 24)
                .shadow(color: pal.bg.opacity(0.4), radius: 14, y: 8)
            }
            .buttonStyle(PlainButtonStyle())
        }
    }
}
