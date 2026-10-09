import SwiftUI

// Avatares y banners del perfil. Son dibujos vectoriales (carpeta Art, ver Art.swift): el animal, su accesorio, la escena de fondo y la
// paleta de su variante salen de los mismos datos que en Android. Nada de emojis.

/// Los 10 avatares clásicos (ids 1 a 10) son estos animales con su paleta normal.
private let classicAnimals = ["FOX", "CAT", "PANDA", "BUNNY", "BEAR", "FROG", "OWL", "PENGUIN", "KOALA", "RACCOON"]

enum AvatarArt {
    static func animal(id: Int, item: AvatarItem?) -> String {
        if let name = item?.animal, !name.isEmpty { return name }
        return classicAnimals[max(0, min(id - 1, classicAnimals.count - 1))]
    }

    static func palette(animal: String, variant: String) -> ArtPalette {
        let library = ArtLibrary.shared
        if let found = library.palette("avatar.\(animal).\(variant)") { return found }
        return library.palette("avatar.\(animal).NORMAL") ?? [:]
    }

    /// Pinta el avatar completo (fondo, escena, animal y accesorio) en un rectángulo.
    static func draw(_ context: GraphicsContext, rect: CGRect, animal: String, variant: String, accessory: String, scene: String, blink: Bool) {
        let pal = palette(animal: animal, variant: variant)
        let library = ArtLibrary.shared
        let top = ArtColor.slot("bg1").resolve(pal, [:]).color
        let bottom = ArtColor.slot("bg2").resolve(pal, [:]).color
        context.fill(
            Path(rect),
            with: GraphicsContext.Shading.linearGradient(
                Gradient(colors: [top, bottom]),
                startPoint: CGPoint(x: rect.midX, y: rect.minY),
                endPoint: CGPoint(x: rect.midX, y: rect.maxY)
            )
        )
        var sceneId = ""
        if scene != "AUTO" && !scene.isEmpty {
            sceneId = "scene.\(scene)"
        } else if variant == "MIDNIGHT" {
            sceneId = "scene.AUTO_MIDNIGHT"
        } else if variant == "GOLD" {
            sceneId = "scene.AUTO_GOLD"
        }
        if !sceneId.isEmpty { library.draw(sceneId, context, in: rect, palette: pal) }
        if accessory == "CAPE" { library.draw("acc.CAPE.back", context, in: rect, palette: pal) }
        library.draw("animal.\(animal)", context, in: rect, palette: pal, blink: blink)
        if accessory != "NONE" && !accessory.isEmpty { library.draw("acc.\(accessory)", context, in: rect, palette: pal) }
    }
}

struct AvatarView: View {
    @EnvironmentObject var model: AppModel
    @ObservedObject private var custom = CustomAvatarStore.shared
    let id: Int
    var size: CGFloat = 48
    /// true cuando es el avatar del jugador de este iPhone (puede ser su sticker propio).
    var mine: Bool = false
    var animate: Bool = true
    @State private var blinking = false

    var body: some View {
        let item = model.avatar(id)
        let frame = item?.frame ?? "NONE"
        return ZStack {
            if mine && custom.active {
                CustomAvatarFace(size: size, animated: animate)
            } else {
                artLayer(item)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(frameRing(frame))
        .task {
            await blinkLoop()
        }
    }

    private func artLayer(_ item: AvatarItem?) -> some View {
        let animal = AvatarArt.animal(id: id, item: item)
        let variant = item?.variant ?? "NORMAL"
        let accessory = item?.accessory ?? "NONE"
        let scene = item?.scene ?? "AUTO"
        let closed = blinking
        return Canvas { context, canvasSize in
            AvatarArt.draw(
                context,
                rect: CGRect(origin: CGPoint.zero, size: canvasSize),
                animal: animal,
                variant: variant,
                accessory: accessory,
                scene: scene,
                blink: closed
            )
        }
        .accessibilityHidden(true)
    }

    /// Parpadea cada pocos segundos (solo avatares grandes y si el teléfono no está en ahorro ni caliente).
    private func blinkLoop() async {
        if !animate || size < 70 { return }
        var round = 0
        while !Task.isCancelled {
            let wait = 2.4 + Double((id * 7 + round * 3) % 20) / 10.0
            try? await Task.sleep(nanoseconds: UInt64(wait * 1_000_000_000))
            if Task.isCancelled { break }
            if PowerMonitor.shared.decorativeMotion {
                await MainActor.run { blinking = true }
                try? await Task.sleep(nanoseconds: 150_000_000)
                await MainActor.run { blinking = false }
            }
            round += 1
        }
    }

    @ViewBuilder
    private func frameRing(_ frame: String) -> some View {
        switch frame {
        case "SOFT":
            Circle().stroke(Color.white, lineWidth: max(2, size * 0.05))
        case "SILVER":
            Circle().stroke(LinearGradient(colors: [Color(hex: 0xF2F4F8), Color(hex: 0x9AA3B5)], startPoint: .top, endPoint: .bottom), lineWidth: max(2.5, size * 0.07))
        case "GOLD":
            Circle().stroke(LinearGradient(colors: [Color(hex: 0xFFF1B5), Color(hex: 0xC98A1B)], startPoint: .top, endPoint: .bottom), lineWidth: max(3, size * 0.08))
        case "PRISM":
            Circle().stroke(AngularGradient(colors: [Color(hex: 0xFF9AA2), Color(hex: 0xFFE08A), Color(hex: 0x9AE0B5), Color(hex: 0x9AB8FF), Color(hex: 0xC7B6FF), Color(hex: 0xFF9AA2)], center: .center), lineWidth: max(3, size * 0.08))
        case "MYTHIC":
            Circle().stroke(AngularGradient(colors: [Color(hex: 0xFFE08A), Color(hex: 0xB57CF0), Color(hex: 0x6A2FB8), Color(hex: 0xFFE08A)], center: .center), lineWidth: max(3.5, size * 0.09))
                .shadow(color: Color(hex: 0xB57CF0).opacity(0.7), radius: 4, x: 0, y: 0)
        default:
            EmptyView()
        }
    }
}

// MARK: - Banners

/// Cómo se mueve la capa animada de cada patrón (la misma tabla que usa iosApp/tools/art/packs/banners.py).
private let bannerModes: [String: String] = [
    "CHECKER": "twinkle", "DOTS": "drift", "HILLS": "drift", "PETALS": "fall", "LEAVES": "fall",
    "WAVES": "sway", "BUBBLES": "rise", "SNOW": "fall", "DIAMONDS": "twinkle", "RAYS": "twinkle",
    "STARS": "twinkle", "AURORA": "drift", "SKYLINE": "twinkle", "EMBERS": "rise", "BATS": "drift",
    "PUMPKINS": "twinkle", "MOUNTAINS": "drift", "FOREST": "twinkle", "CLOUDS": "drift", "HEARTS": "rise",
    "FIREWORKS": "twinkle", "GALAXY": "twinkle", "ZEN": "drift", "STRIPES": "drift", "CIRCUIT": "drift",
    "LANTERNS": "sway", "CONFETTI": "fall", "RAIN": "fall", "SUNSET": "drift", "CRYSTALS": "twinkle"
]

/// Partículas que flotan en los patrones que no tienen dibujo propio.
private let bannerFallbackFx: [String: [String]] = [
    "DIAMONDS": ["fx.diamond"], "RAYS": ["fx.sparkle", "fx.glow"], "STARS": ["fx.star", "fx.sparkle"],
    "AURORA": ["fx.sparkle", "fx.star_soft"], "SKYLINE": ["fx.sparkle"], "EMBERS": ["fx.ember"],
    "BATS": ["fx.bat"], "PUMPKINS": ["fx.pumpkin_small", "fx.ember"], "MOUNTAINS": ["fx.cloud"],
    "FOREST": ["fx.leaf", "fx.leaf_round"], "CLOUDS": ["fx.cloud"], "HEARTS": ["fx.heart", "fx.heart_small"],
    "FIREWORKS": ["fx.firework", "fx.spark"], "GALAXY": ["fx.sparkle", "fx.star_soft", "fx.meteor"], "ZEN": ["fx.petal"],
    "STRIPES": ["fx.spark"], "CIRCUIT": ["fx.spark", "fx.ring"], "LANTERNS": ["fx.glow", "fx.sparkle"],
    "CONFETTI": ["fx.confetti_a", "fx.confetti_b", "fx.confetti_c"], "RAIN": ["fx.raindrop"],
    "SUNSET": ["fx.cloud", "fx.glow"], "CRYSTALS": ["fx.crystal", "fx.sparkle"]
]

/// Fondo de la tarjeta de jugador: degradado, dibujo del patrón y una capa que se mueve despacio.
struct BannerView: View {
    @EnvironmentObject var model: AppModel
    @ObservedObject private var power = PowerMonitor.shared
    let id: Int
    var height: CGFloat = 90
    var animate: Bool = true

    var body: some View {
        let item = model.banner(id)
        let pattern = item?.pattern ?? "DOTS"
        let top = Color(hex: UInt32(truncatingIfNeeded: item?.top ?? 0xF3EFE6))
        let bottom = Color(hex: UInt32(truncatingIfNeeded: item?.bottom ?? 0xE8E0D0))
        let pal = bannerPalette(item)
        let hasArt = ArtLibrary.shared.has("banner.\(pattern)")
        let mode = bannerModes[pattern] ?? "drift"
        let moving = animate && power.decorativeMotion
        return ZStack {
            LinearGradient(colors: [top, bottom], startPoint: .top, endPoint: .bottom)
            Canvas { context, size in
                if hasArt {
                    ArtLibrary.shared.draw("banner.\(pattern)", context, in: CGRect(origin: CGPoint.zero, size: size), palette: pal, fit: .fillBottom)
                } else {
                    drawFallbackBase(context, size, pattern, pal)
                }
            }
            if moving {
                TimelineView(.animation(minimumInterval: power.frameInterval, paused: false)) { timeline in
                    Canvas { context, size in
                        drawMotion(context, size, pattern, mode, hasArt, pal, timeline.date.timeIntervalSinceReferenceDate)
                    }
                }
            } else {
                Canvas { context, size in
                    drawMotion(context, size, pattern, mode, hasArt, pal, 3.0)
                }
            }
        }
        .frame(height: height)
        .clipped()
        .accessibilityHidden(true)
    }

    private func bannerPalette(_ item: BannerItem?) -> ArtPalette {
        let top = UInt32(truncatingIfNeeded: item?.top ?? 0xF3EFE6)
        let bottom = UInt32(truncatingIfNeeded: item?.bottom ?? 0xE8E0D0)
        let accent = UInt32(truncatingIfNeeded: item?.accent ?? 0xB8A58A)
        let ink = UInt32(truncatingIfNeeded: item?.ink ?? 0x3D405B)
        return [
            "top": ArtColor.hex(top),
            "bottom": ArtColor.hex(bottom),
            "accent": ArtColor.hex(accent),
            "ink": ArtColor.hex(ink),
            "c": ArtColor.hex(accent),
            "c2": ArtColor.mix(ArtColor.hex(accent), ArtColor.hex(0xFFFFFF), 0.55)
        ]
    }

    // MARK: capa que se mueve

    private func drawMotion(_ context: GraphicsContext, _ size: CGSize, _ pattern: String, _ mode: String, _ hasArt: Bool, _ pal: ArtPalette, _ t: Double) {
        let rect = CGRect(origin: CGPoint.zero, size: size)
        let library = ArtLibrary.shared
        if !hasArt {
            drawFallbackParticles(context, size, pattern, mode, pal, t)
            return
        }
        let scale = max(size.width / 300.0, size.height / 100.0)
        let artW = 300.0 * scale
        let artH = 100.0 * scale
        let fx = "banner.\(pattern).fx"
        switch mode {
        case "drift":
            let dx = CGFloat((t * 7.0).truncatingRemainder(dividingBy: 300.0)) * scale
            library.draw(fx, context, in: rect.offsetBy(dx: -dx, dy: 0), palette: pal, fit: .fillBottom)
            library.draw(fx, context, in: rect.offsetBy(dx: artW - dx, dy: 0), palette: pal, fit: .fillBottom)
        case "rise", "fall":
            let step = CGFloat((t * 6.0).truncatingRemainder(dividingBy: 100.0)) * scale
            let dy = mode == "rise" ? -step : step
            library.draw(fx, context, in: rect.offsetBy(dx: 0, dy: dy), palette: pal, fit: .fillBottom)
            library.draw(fx, context, in: rect.offsetBy(dx: 0, dy: dy + (mode == "rise" ? artH : -artH)), palette: pal, fit: .fillBottom)
        case "sway":
            let dx = CGFloat(sin(t * 0.7)) * 5.0 * scale
            library.draw(fx, context, in: rect.offsetBy(dx: dx, dy: 0), palette: pal, fit: .fillBottom)
        default:
            let blend = 0.5 + 0.5 * sin(t * 1.3)
            library.draw(fx, context, in: rect, palette: pal, fit: .fillBottom, opacity: blend)
            library.draw("banner.\(pattern).fx2", context, in: rect, palette: pal, fit: .fillBottom, opacity: 1 - blend)
        }
    }

    // MARK: patrones sin dibujo propio

    private func drawFallbackBase(_ context: GraphicsContext, _ size: CGSize, _ pattern: String, _ pal: ArtPalette) {
        let accent = ArtColor.slot("accent").resolve(pal, [:])
        let w = size.width
        let h = size.height
        func tone(_ a: Double) -> Color {
            return Color(.sRGB, red: accent.r, green: accent.g, blue: accent.b, opacity: a)
        }
        switch pattern {
        case "MOUNTAINS", "FOREST", "SKYLINE", "SUNSET", "ZEN", "AURORA", "CLOUDS", "LANTERNS", "PUMPKINS", "BATS":
            if pattern == "SUNSET" {
                context.fill(Path(ellipseIn: CGRect(x: w * 0.62, y: h * 0.38, width: h * 0.7, height: h * 0.7)), with: .color(tone(0.45)))
            }
            for layer in 0..<3 {
                var path = Path()
                let base = h * (0.62 + 0.14 * Double(layer))
                path.move(to: CGPoint(x: 0, y: h))
                path.addLine(to: CGPoint(x: 0, y: base))
                var x: CGFloat = 0
                var i = 0
                while x <= w + 20 {
                    let peak = (pattern == "SKYLINE") ? 0 : CGFloat(10 + ((i * 7 + layer * 5) % 14))
                    if pattern == "SKYLINE" {
                        let block = CGFloat(8 + ((i * 11 + layer * 3) % 18))
                        path.addLine(to: CGPoint(x: x, y: base - block))
                        path.addLine(to: CGPoint(x: x + 18, y: base - block))
                    } else if pattern == "ZEN" || pattern == "AURORA" || pattern == "CLOUDS" || pattern == "SUNSET" {
                        path.addQuadCurve(to: CGPoint(x: x + 44, y: base), control: CGPoint(x: x + 22, y: base - peak * 1.4))
                    } else {
                        path.addLine(to: CGPoint(x: x + 22, y: base - peak * 2.2))
                        path.addLine(to: CGPoint(x: x + 44, y: base))
                    }
                    x += (pattern == "SKYLINE") ? 18 : 44
                    i += 1
                }
                path.addLine(to: CGPoint(x: w, y: h))
                path.closeSubpath()
                context.fill(path, with: .color(tone(0.16 + 0.12 * Double(layer))))
            }
        case "STRIPES":
            var x: CGFloat = -h
            while x < w + h {
                var stripe = Path()
                stripe.move(to: CGPoint(x: x, y: h))
                stripe.addLine(to: CGPoint(x: x + 14, y: h))
                stripe.addLine(to: CGPoint(x: x + 14 + h, y: 0))
                stripe.addLine(to: CGPoint(x: x + h, y: 0))
                stripe.closeSubpath()
                context.fill(stripe, with: .color(tone(0.2)))
                x += 34
            }
        case "CIRCUIT":
            var row = 0
            var y: CGFloat = 14
            while y < h {
                var x = CGFloat((row * 23) % 40)
                while x < w {
                    let len = CGFloat(26 + (row * 7 + Int(x)) % 30)
                    var line = Path()
                    line.move(to: CGPoint(x: x, y: y))
                    line.addLine(to: CGPoint(x: x + len, y: y))
                    context.stroke(line, with: .color(tone(0.28)), lineWidth: 1.4)
                    context.fill(Path(ellipseIn: CGRect(x: x + len - 2.5, y: y - 2.5, width: 5, height: 5)), with: .color(tone(0.4)))
                    x += len + 18
                }
                y += 24
                row += 1
            }
        default:
            break
        }
    }

    private func drawFallbackParticles(_ context: GraphicsContext, _ size: CGSize, _ pattern: String, _ mode: String, _ pal: ArtPalette, _ t: Double) {
        guard let ids = bannerFallbackFx[pattern], !ids.isEmpty else { return }
        let library = ArtLibrary.shared
        let count = max(4, Int(Double(14) * power.particleScale))
        let w = size.width
        let h = size.height
        for i in 0..<count {
            let seed = Double(i) * 12.9898
            let fx = abs(sin(seed) * 43758.5453).truncatingRemainder(dividingBy: 1.0)
            let fy = abs(sin(seed * 1.7 + 3.1) * 24634.6345).truncatingRemainder(dividingBy: 1.0)
            let side = CGFloat(14.0 + 14.0 * fy)
            var x = CGFloat(fx) * w
            var y = CGFloat(fy) * h
            var alpha = 0.8
            switch mode {
            case "drift":
                x = CGFloat((Double(x) + t * 8.0).truncatingRemainder(dividingBy: Double(w + 40))) - 20
            case "rise":
                y = CGFloat(Double(h + 20) - (Double(h) - Double(y) + t * 10.0).truncatingRemainder(dividingBy: Double(h + 40)))
            case "fall":
                y = CGFloat((Double(y) + t * 12.0).truncatingRemainder(dividingBy: Double(h + 40))) - 20
            case "sway":
                x += CGFloat(sin(t * 0.8 + seed)) * 6
            default:
                alpha = 0.35 + 0.65 * (0.5 + 0.5 * sin(t * 1.6 + seed))
            }
            library.draw(ids[i % ids.count], context, in: CGRect(x: x - side / 2, y: y - side / 2, width: side, height: side), palette: pal, opacity: alpha)
        }
    }
}
