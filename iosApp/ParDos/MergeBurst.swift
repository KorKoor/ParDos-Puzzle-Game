import SwiftUI

/// Lo que sale volando cuando dos fichas se juntan, según el efecto de fusión que lleve puesto el jugador (cosmético).
struct MergeBurst: View {
    let fx: String
    let size: CGFloat
    let value: Int
    @State private var t: Double = 0
    @ObservedObject private var power = PowerMonitor.shared

    private var count: Int {
        var steps = 0
        var v = 2
        while v < value && steps < 14 {
            v *= 2
            steps += 1
        }
        let base = min(26, max(8, 8 + steps * 2))
        return max(4, Int(Double(base) * power.particleScale))
    }

    private var icons: [String] {
        switch fx {
        case "sparks": return ["fx.spark", "fx.sparkle"]
        case "bubbles": return ["fx.bubble"]
        case "petals": return ["fx.petal", "fx.petal_cherry"]
        case "hearts": return ["fx.heart", "fx.heart_small"]
        case "confetti": return ["fx.confetti_a", "fx.confetti_b", "fx.confetti_c"]
        case "stars": return ["fx.star", "fx.star_soft"]
        case "lightning": return ["fx.bolt"]
        case "fireworks": return ["fx.firework", "fx.spark"]
        default: return []
        }
    }

    private func tint(_ i: Int) -> UInt32 {
        switch fx {
        case "sparks": return 0xFFC94A
        case "bubbles": return 0x7FD6F5
        case "petals": return 0xF4A8BC
        case "hearts": return 0xE0475B
        case "stars": return 0xFFD36E
        case "lightning": return 0xB27BFF
        default:
            let palette: [UInt32] = [0xE8772E, 0x9B4FC9, 0xE0A93B, 0x6B9E86, 0xE07A5F, 0x4E8FA6]
            return palette[i % palette.count]
        }
    }

    private func particle(_ i: Int, _ ids: [String]) -> some View {
        let total = Double(count)
        let angle = Double(i) / total * 2.0 * Double.pi + Double(i % 3) * 0.2
        let reach = Double(size) * (0.55 + 0.5 * Double((i * 7) % 5) / 4.0)
        let side = size * (fx == "confetti" ? 0.24 : 0.3)
        let lift: CGFloat = (fx == "bubbles" || fx == "hearts") ? CGFloat(t) * size * 0.3 : 0
        let dx = CGFloat(cos(angle) * reach * t)
        let dy = CGFloat(sin(angle) * reach * t) - lift
        let spin: Double = (fx == "confetti" || fx == "petals") ? t * 220.0 : 0
        let color = ArtColor.hex(tint(i))
        let pal: ArtPalette = ["c": color, "c2": ArtColor.mix(color, ArtColor.hex(0xFFFFFF), 0.5)]
        return ArtView(id: ids[i % ids.count], palette: pal)
            .frame(width: side, height: side)
            .offset(x: dx, y: dy)
            .rotationEffect(.degrees(spin))
            .opacity(1.0 - t)
            .scaleEffect(1.0 - 0.4 * CGFloat(t))
    }

    var body: some View {
        let ids = icons
        return ZStack {
            if fx == "ripple" {
                Circle()
                    .stroke(Color(hex: 0x7FD6F5).opacity(1 - t), lineWidth: 3)
                    .frame(width: size * (0.5 + 1.2 * CGFloat(t)), height: size * (0.5 + 1.2 * CGFloat(t)))
            } else if !ids.isEmpty {
                ForEach(0..<count, id: \.self) { i in
                    particle(i, ids)
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
