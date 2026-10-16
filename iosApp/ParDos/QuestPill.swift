import SwiftUI

/// Reto bonus del nivel: opcional, no cambia las estrellas. Si se cumple al ganar, suma unas monedas.
struct QuestPill: View {
    let title: String
    let hint: String
    let coins: Int
    let done: Bool

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                Circle().fill(done ? Color(hex: 0x4F8F72) : Theme.gold.opacity(0.22)).frame(width: 30, height: 30)
                Image(systemName: done ? "checkmark" : "target")
                    .font(.system(size: 13, weight: .black))
                    .foregroundColor(done ? .white : Theme.gold)
            }
            VStack(alignment: .leading, spacing: 1) {
                Text("\(loc("RETO BONUS")) \u{00B7} \(loc(title))")
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(1.2)
                    .foregroundColor(Theme.ink.opacity(0.55))
                Text(loc(hint))
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Theme.ink)
                    .lineLimit(2)
            }
            Spacer(minLength: 4)
            HStack(spacing: 3) {
                Text("+\(coins)")
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(done ? Color(hex: 0x4F8F72) : Theme.ink.opacity(0.7))
                CoinIcon(size: 14)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Color.white.opacity(0.85)))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(done ? Color(hex: 0x4F8F72).opacity(0.6) : Theme.gold.opacity(0.35), lineWidth: 1))
        .padding(.horizontal, 14)
    }
}
