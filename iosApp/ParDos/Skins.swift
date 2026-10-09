import SwiftUI
import UIKit

/// Cómo se ven las fichas y el fondo de la partida con la skin que lleva puesta el jugador (los mismos datos que en Android).
struct BoardStyle {
    let palette: [Color]?
    let darkText: Color
    let lightText: Color
    let lightFrom: Int
    let finish: String
    let bgTop: Color?
    let bgBottom: Color?
    let ink: Color?
    let accent: Color?
    let surface: Color?
    let particles: String
    let particleTint: Color

    init(skin: SkinItem?) {
        guard let skin = skin else {
            palette = nil
            darkText = Color(hex: 0x5C4F44)
            lightText = Color(hex: 0xFFFBF5)
            lightFrom = 3
            finish = "JELLY"
            bgTop = nil
            bgBottom = nil
            ink = nil
            accent = nil
            surface = nil
            particles = "NONE"
            particleTint = Color(hex: 0xF4B6C2)
            return
        }
        if let list = skin.palette {
            palette = list.map { Color(hex: UInt32($0)) }
        } else {
            palette = nil
        }
        darkText = Color(hex: UInt32(skin.darkText))
        lightText = Color(hex: UInt32(skin.lightText))
        lightFrom = skin.lightFrom
        finish = skin.finish
        if let top = skin.bgTop, let bottom = skin.bgBottom {
            bgTop = Color(hex: UInt32(top))
            bgBottom = Color(hex: UInt32(bottom))
        } else {
            bgTop = nil
            bgBottom = nil
        }
        ink = skin.ink.map { Color(hex: UInt32($0)) }
        accent = skin.accent.map { Color(hex: UInt32($0)) }
        surface = skin.surface.map { Color(hex: UInt32($0)) }
        particles = skin.particles
        particleTint = Color(hex: UInt32(skin.particleTint))
    }

    var changesTheme: Bool { bgTop != nil && bgBottom != nil }

    var background: LinearGradient {
        LinearGradient(colors: [bgTop ?? Theme.paper, bgBottom ?? Theme.cream], startPoint: .top, endPoint: .bottom)
    }

    /// 0 para la ficha 2, 1 para la 4, 2 para la 8...
    private func power(_ value: Int) -> Int {
        var p = 0
        var v = 2
        while v < value && p < 40 {
            v *= 2
            p += 1
        }
        return p
    }

    func fill(_ value: Int) -> Color {
        guard let list = palette, !list.isEmpty else { return tileColor(value) }
        let index = min(power(value), list.count - 1)
        return list[index]
    }

    func text(_ value: Int) -> Color {
        if palette == nil { return tileTextColor(value) }
        return power(value) >= lightFrom ? lightText : darkText
    }
}

// MARK: - Partículas del fondo

/// Adornos que flotan detrás del tablero según la skin (pétalos, nieve, burbujas, estrellas...). Son dibujos, no caracteres.
struct ParticlesView: View {
    let kind: String
    let tint: Color
    @ObservedObject private var power = PowerMonitor.shared

    var body: some View {
        if kind == "NONE" || !power.decorativeMotion {
            EmptyView()
        } else {
            TimelineView(.animation(minimumInterval: power.frameInterval, paused: false)) { timeline in
                Canvas { context, size in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    draw(context, size, t)
                }
            }
            .allowsHitTesting(false)
            .ignoresSafeArea()
        }
    }

    private var icons: [String] {
        switch kind {
        case "PETALS": return ["fx.petal", "fx.petal_cherry"]
        case "SNOW", "SNOWFLAKE": return ["fx.snowflake", "fx.snow_dot"]
        case "LEAVES": return ["fx.leaf", "fx.leaf_round"]
        case "FIREFLIES": return ["fx.glow"]
        case "BUBBLES": return ["fx.bubble"]
        case "STARS", "SPARKLES": return ["fx.sparkle", "fx.star_soft"]
        case "EMBERS": return ["fx.ember"]
        case "SPRINKLES", "CONFETTI", "FLAG_CONFETTI": return ["fx.confetti_a", "fx.confetti_b", "fx.confetti_c"]
        case "RAIN": return ["fx.raindrop"]
        case "SAND": return ["fx.snow_dot"]
        case "HEARTS": return ["fx.heart_small", "fx.heart"]
        case "BATS": return ["fx.bat"]
        case "MARIGOLD", "FLOWERS": return ["fx.marigold", "fx.flower"]
        case "FIREWORKS": return ["fx.firework", "fx.spark"]
        case "METEORS": return ["fx.meteor"]
        default: return ["fx.snow_dot"]
        }
    }

    /// Hacia dónde va: 1 = cae, -1 = sube.
    private var direction: Double {
        switch kind {
        case "BUBBLES", "EMBERS", "FIREFLIES", "HEARTS", "BATS", "STARS", "SPARKLES", "FIREWORKS": return -1
        default: return 1
        }
    }

    private func draw(_ context: GraphicsContext, _ size: CGSize, _ t: Double) {
        let count = max(4, Int(16.0 * power.particleScale))
        let ids = icons
        let color = ArtColor.from(tint)
        let pal: ArtPalette = ["c": color, "c2": ArtColor.mix(color, ArtColor.hex(0xFFFFFF), 0.5)]
        for i in 0..<count {
            let seed = Double(i)
            let speed = 0.03 + 0.02 * Double((i * 5) % 4)
            let progress = (t * speed + seed * 0.173).truncatingRemainder(dividingBy: 1.0)
            let x = size.width * (0.05 + 0.9 * Double((i * 41) % 100) / 100.0) + 16.0 * sin(t * 0.8 + seed)
            let y = direction > 0 ? size.height * progress : size.height * (1.0 - progress)
            let fade = sin(progress * Double.pi)
            let side = CGFloat(18.0 + 9.0 * Double((i * 3) % 3))
            let rect = CGRect(x: x - Double(side) / 2, y: y - Double(side) / 2, width: Double(side), height: Double(side))
            ArtLibrary.shared.draw(ids[i % ids.count], context, in: rect, palette: pal, opacity: 0.6 * fade, rotation: t * 0.6 + seed)
        }
    }
}
