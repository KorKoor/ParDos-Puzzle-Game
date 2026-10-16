import SwiftUI

/// Frenesí: al llegar a FLOW, las siguientes jugadas dan el doble de puntos. Cápsula dorada que late mientras dura.
struct FrenzyBadge: View {
    let left: Int

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            let pulse = 0.5 + 0.5 * sin(t * 6)
            HStack(spacing: 6) {
                Image(systemName: "flame.fill")
                    .font(.system(size: 13, weight: .black))
                Text("\(loc("FRENESÍ")) x2")
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .kerning(1)
                HStack(spacing: 3) {
                    ForEach(0..<3, id: \.self) { i in
                        Circle()
                            .fill(Color.white.opacity(i < left ? 1 : 0.28))
                            .frame(width: 6, height: 6)
                    }
                }
            }
            .foregroundColor(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background(
                Capsule().fill(LinearGradient(colors: [Color(hex: 0xFF8A1F), Color(hex: 0xE0A93B)], startPoint: .leading, endPoint: .trailing))
            )
            .overlay(Capsule().stroke(Color.white.opacity(0.5 + 0.3 * pulse), lineWidth: 1.5))
            .shadow(color: Color(hex: 0xFF8A1F).opacity(0.35 + 0.3 * pulse), radius: 6 + 6 * pulse, y: 2)
            .scaleEffect(1 + 0.03 * pulse)
        }
    }
}
