import SwiftUI

/// Lo que sale volando cuando dos fichas se juntan, según el efecto de fusión que lleve puesto el jugador (cosmético).
struct MergeBurst: View {
    let fx: String
    let size: CGFloat
    let value: Int
    @State private var t: Double = 0

    private var count: Int {
        var steps = 0
        var v = 2
        while v < value && steps < 14 {
            v *= 2
            steps += 1
        }
        return min(26, max(8, 8 + steps * 2))
    }

    private var glyph: String {
        switch fx {
        case "sparks": return "✦"
        case "bubbles": return "○"
        case "petals": return "❀"
        case "hearts": return "♥"
        case "confetti": return "▪︎"
        case "stars": return "★"
        case "lightning": return "⚡︎"
        case "fireworks": return "✺"
        default: return ""
        }
    }

    private func color(_ i: Int) -> Color {
        switch fx {
        case "sparks": return Color(hex: 0xFFC94A)
        case "bubbles": return Color(hex: 0x7FD6F5)
        case "petals": return Color(hex: 0xF4A8BC)
        case "hearts": return Color(hex: 0xE0475B)
        case "stars": return Color(hex: 0xFFD36E)
        case "lightning": return Color(hex: 0xB27BFF)
        default:
            let palette: [Color] = [Color(hex: 0xE8772E), Color(hex: 0x9B4FC9), Color(hex: 0xE0A93B), Color(hex: 0x6B9E86), Color(hex: 0xE07A5F), Color(hex: 0x4E8FA6)]
            return palette[i % palette.count]
        }
    }

    var body: some View {
        ZStack {
            if fx == "ripple" {
                Circle()
                    .stroke(Color(hex: 0x7FD6F5).opacity(1 - t), lineWidth: 3)
                    .frame(width: size * (0.5 + 1.2 * CGFloat(t)), height: size * (0.5 + 1.2 * CGFloat(t)))
            } else if !glyph.isEmpty {
                ForEach(0..<count, id: \.self) { i in
                    let angle = Double(i) / Double(count) * 2.0 * Double.pi + Double(i % 3) * 0.2
                    let reach = Double(size) * (0.55 + 0.5 * Double((i * 7) % 5) / 4.0)
                    Text(glyph)
                        .font(.system(size: size * (fx == "confetti" ? 0.2 : 0.26), weight: .black))
                        .foregroundColor(color(i))
                        .offset(x: CGFloat(cos(angle) * reach * t), y: CGFloat(sin(angle) * reach * t) - (fx == "bubbles" || fx == "hearts" ? CGFloat(t) * size * 0.3 : 0))
                        .rotationEffect(.degrees(fx == "confetti" || fx == "petals" ? t * 220.0 : 0))
                        .opacity(1.0 - t)
                        .scaleEffect(1.0 - 0.4 * CGFloat(t))
                }
            }
        }
        .frame(width: size, height: size)
        .allowsHitTesting(false)
        .onAppear {
            withAnimation(.easeOut(duration: 0.7)) {
                t = 1
            }
        }
    }
}

/// El "+N" que sube y se desvanece donde se juntaron dos fichas.
struct FloatingScore: View {
    let value: Int
    let size: CGFloat
    @State private var rise: CGFloat = 0
    @State private var fade: Double = 1

    var body: some View {
        Text("+" + String(value))
            .font(.system(size: size * 0.3, weight: .black, design: .rounded))
            .foregroundColor(Color.white)
            .shadow(color: Color.black.opacity(0.55), radius: 2, x: 0, y: 1)
            .offset(y: rise)
            .opacity(fade)
            .allowsHitTesting(false)
            .onAppear {
                withAnimation(.easeOut(duration: 0.8)) {
                    rise = -size * 0.9
                    fade = 0
                }
            }
    }
}
