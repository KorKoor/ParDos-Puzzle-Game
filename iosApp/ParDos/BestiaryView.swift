import SwiftUI

// Bestiario: todos los jefes y guardianes con su emblema animado, su frase y los niveles donde se les enfrenta.

struct BestiaryEntry: Decodable, Identifiable {
    let name: String
    let motif: String
    let taunt: String
    let levels: [Int]
    let first: Int
    let defeated: Bool
    let guardian: Bool
    var id: String { name }
}

struct BestiaryData: Decodable {
    let bosses: [BestiaryEntry]
    let guardians: [BestiaryEntry]
    let pantheon: [BestiaryEntry]?
    let defeated: Int
    let total: Int
}

extension AppModel {
    func loadBestiary() -> BestiaryData? {
        return decodeJSON(BestiaryData.self, meta.bestiary())
    }
}

/// Paleta de cada motivo (fondo oscuro, color principal, brillo).
func motifPalette(_ motif: String) -> (bg: Color, main: Color, glow: Color) {
    switch motif {
    case "STORM": return (Color(hex: 0x1D2A4A), Color(hex: 0x7FA8FF), Color(hex: 0xFFE27A))
    case "CLOCK": return (Color(hex: 0x3A2A1C), Color(hex: 0xE9B949), Color(hex: 0xFFF1C2))
    case "GOLD": return (Color(hex: 0x3B2D0E), Color(hex: 0xF2C14E), Color(hex: 0xFFF6C9))
    case "STONE": return (Color(hex: 0x2B2B33), Color(hex: 0x9A9AA8), Color(hex: 0xD8D8E6))
    case "CRYSTAL": return (Color(hex: 0x16303A), Color(hex: 0x6FD6E8), Color(hex: 0xE3FBFF))
    case "EYE": return (Color(hex: 0x2A1740), Color(hex: 0xB07CFF), Color(hex: 0xFFB3F0))
    case "GEARS": return (Color(hex: 0x2A2A22), Color(hex: 0xC9A15C), Color(hex: 0xFFE2A8))
    case "MAZE": return (Color(hex: 0x1D2F2A), Color(hex: 0x6FCF97), Color(hex: 0xD6FFE8))
    case "FIRE": return (Color(hex: 0x40160F), Color(hex: 0xFF7A3D), Color(hex: 0xFFE08A))
    default: return (Color(hex: 0x2A2430), Color(hex: 0xC27A5A), Color(hex: 0xFFD9C2)) // WALL
    }
}

/// Emblema animado del jefe: una escena corta por motivo (se dibuja con trazos, sin imágenes ni emojis).
struct BossEmblemView: View {
    let motif: String
    var size: CGFloat = 96
    var locked: Bool = false

    var body: some View {
        let pal = motifPalette(motif)
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            Canvas { ctx, sz in
                EmblemPainter.paint(&ctx, sz, motif, t, locked, pal)
            }
            .frame(width: size, height: size)
            .background(
                Circle().fill(LinearGradient(colors: [pal.bg, pal.bg.opacity(0.85)], startPoint: .top, endPoint: .bottom))
            )
            .clipShape(Circle())
            .overlay(Circle().stroke((locked ? Color.gray : pal.main).opacity(0.55), lineWidth: 2))
            .shadow(color: (locked ? Color.clear : pal.main).opacity(0.35), radius: 10, y: 4)
        }
    }
}

/// Dibuja cada escena de jefe. Va en una función aparte para que el compilador no tarde una eternidad en revisar un cierre gigante.
enum EmblemPainter {
    static func paint(_ ctx: inout GraphicsContext, _ sz: CGSize, _ motif: String, _ t: Double, _ locked: Bool, _ pal: (bg: Color, main: Color, glow: Color)) {
                let w = sz.width, h = sz.height
                let c = CGPoint(x: w / 2, y: h / 2)
                let main = locked ? Color(hex: 0x4A4A55) : pal.main
                let glow = locked ? Color(hex: 0x6A6A75) : pal.glow
                // resplandor que respira
                let pulse = 0.5 + 0.5 * sin(t * 2.0)
                let halo = Path(ellipseIn: CGRect(x: c.x - w * 0.46, y: c.y - h * 0.46, width: w * 0.92, height: h * 0.92))
                ctx.fill(halo, with: .radialGradient(Gradient(colors: [glow.opacity(0.28 + 0.12 * pulse), .clear]), center: c, startRadius: 0, endRadius: w * 0.5))
                let lw = max(2, w * 0.045)
                let style = StrokeStyle(lineWidth: lw, lineCap: .round, lineJoin: .round)
                switch motif {
                case "STORM":
                    var cloud = Path()
                    cloud.addEllipse(in: CGRect(x: w * 0.18, y: h * 0.28, width: w * 0.42, height: h * 0.30))
                    cloud.addEllipse(in: CGRect(x: w * 0.38, y: h * 0.22, width: w * 0.44, height: h * 0.36))
                    cloud.addEllipse(in: CGRect(x: w * 0.28, y: h * 0.36, width: w * 0.46, height: h * 0.24))
                    ctx.fill(cloud, with: .color(main.opacity(0.9)))
                    let flash = max(0, sin(t * 3.4))
                    var bolt = Path()
                    bolt.move(to: CGPoint(x: w * 0.54, y: h * 0.55)); bolt.addLine(to: CGPoint(x: w * 0.44, y: h * 0.72))
                    bolt.addLine(to: CGPoint(x: w * 0.52, y: h * 0.72)); bolt.addLine(to: CGPoint(x: w * 0.46, y: h * 0.90))
                    ctx.stroke(bolt, with: .color(glow.opacity(0.35 + 0.65 * flash)), style: style)
                case "CLOCK":
                    let r = w * 0.34
                    ctx.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(main), style: style)
                    for i in 0..<12 {
                        let a = Double(i) / 12 * .pi * 2
                        var tick = Path()
                        tick.move(to: CGPoint(x: c.x + CGFloat(cos(a)) * r * 0.82, y: c.y + CGFloat(sin(a)) * r * 0.82))
                        tick.addLine(to: CGPoint(x: c.x + CGFloat(cos(a)) * r * 0.94, y: c.y + CGFloat(sin(a)) * r * 0.94))
                        ctx.stroke(tick, with: .color(main.opacity(0.7)), style: StrokeStyle(lineWidth: lw * 0.5, lineCap: .round))
                    }
                    let a1 = t * 0.9, a2 = t * 0.075
                    var hand = Path()
                    hand.move(to: c); hand.addLine(to: CGPoint(x: c.x + CGFloat(cos(a1)) * r * 0.78, y: c.y + CGFloat(sin(a1)) * r * 0.78))
                    ctx.stroke(hand, with: .color(glow), style: style)
                    var hand2 = Path()
                    hand2.move(to: c); hand2.addLine(to: CGPoint(x: c.x + CGFloat(cos(a2)) * r * 0.5, y: c.y + CGFloat(sin(a2)) * r * 0.5))
                    ctx.stroke(hand2, with: .color(main), style: style)
                case "GOLD":
                    for i in 0..<3 {
                        let bob = sin(t * 2 + Double(i)) * h * 0.015
                        let rect = CGRect(x: w * (0.20 + 0.18 * CGFloat(i)), y: h * (0.50 - 0.12 * CGFloat(i)) + bob, width: w * 0.36, height: h * 0.24)
                        ctx.fill(Path(roundedRect: rect, cornerRadius: w * 0.05), with: .color(main.opacity(0.75 + 0.08 * Double(i))))
                        ctx.stroke(Path(roundedRect: rect, cornerRadius: w * 0.05), with: .color(glow.opacity(0.8)), style: StrokeStyle(lineWidth: lw * 0.4))
                    }
                    let sp = 0.5 + 0.5 * sin(t * 4)
                    ctx.fill(Path(ellipseIn: CGRect(x: w * 0.70, y: h * 0.20, width: w * 0.09 * (0.6 + sp), height: w * 0.09 * (0.6 + sp))), with: .color(glow))
                case "STONE":
                    let rocks: [(CGFloat, CGFloat, CGFloat)] = [(0.30, 0.58, 0.30), (0.56, 0.52, 0.34), (0.44, 0.34, 0.26)]
                    for (n, rk) in rocks.enumerated() {
                        let wob = sin(t * 1.6 + Double(n)) * 1.5
                        var p = Path()
                        let cx = w * rk.0, cy = h * rk.1 + CGFloat(wob), r = w * rk.2 / 2
                        for k in 0..<7 {
                            let a = Double(k) / 7 * .pi * 2
                            let rr = r * (0.82 + 0.18 * CGFloat(((k * 5 + n * 3) % 4)) / 3)
                            let pt = CGPoint(x: cx + CGFloat(cos(a)) * rr, y: cy + CGFloat(sin(a)) * rr)
                            if k == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                        }
                        p.closeSubpath()
                        ctx.fill(p, with: .color(main.opacity(0.55 + 0.15 * Double(n))))
                        ctx.stroke(p, with: .color(glow.opacity(0.6)), style: StrokeStyle(lineWidth: lw * 0.5, lineJoin: .round))
                    }
                case "CRYSTAL":
                    for k in 0..<3 {
                        let x = w * (0.32 + 0.18 * CGFloat(k))
                        let hh = h * (0.46 - 0.08 * abs(CGFloat(k) - 1))
                        var p = Path()
                        p.move(to: CGPoint(x: x, y: h * 0.80)); p.addLine(to: CGPoint(x: x - w * 0.08, y: h * 0.80 - hh * 0.6))
                        p.addLine(to: CGPoint(x: x, y: h * 0.80 - hh)); p.addLine(to: CGPoint(x: x + w * 0.08, y: h * 0.80 - hh * 0.6)); p.closeSubpath()
                        ctx.fill(p, with: .color(main.opacity(0.55)))
                        ctx.stroke(p, with: .color(glow), style: StrokeStyle(lineWidth: lw * 0.5, lineJoin: .round))
                    }
                    let glint = (t * 0.7).truncatingRemainder(dividingBy: 1)
                    ctx.fill(Path(ellipseIn: CGRect(x: w * (0.25 + 0.5 * glint), y: h * 0.30, width: w * 0.06, height: w * 0.06)), with: .color(glow))
                case "EYE":
                    var lid = Path()
                    lid.move(to: CGPoint(x: w * 0.14, y: c.y))
                    lid.addQuadCurve(to: CGPoint(x: w * 0.86, y: c.y), control: CGPoint(x: c.x, y: h * 0.14))
                    lid.addQuadCurve(to: CGPoint(x: w * 0.14, y: c.y), control: CGPoint(x: c.x, y: h * 0.86))
                    ctx.fill(lid, with: .color(main.opacity(0.25)))
                    ctx.stroke(lid, with: .color(main), style: style)
                    let look = sin(t * 0.9) * w * 0.07
                    let blinkWave: Double = sin(t * 1.3 - 1.2) - 0.96
                    let blinkAmount: Double = max(0.0, blinkWave) * 25.0
                    let blink: Double = max(0.08, 1.0 - 0.92 * blinkAmount)
                    let ir = w * 0.15
                    ctx.fill(Path(ellipseIn: CGRect(x: c.x - ir + look, y: c.y - ir * blink, width: ir * 2, height: ir * 2 * blink)), with: .color(glow))
                    ctx.fill(Path(ellipseIn: CGRect(x: c.x - ir * 0.4 + look, y: c.y - ir * 0.4 * blink, width: ir * 0.8, height: ir * 0.8 * blink)), with: .color(pal.bg))
                case "GEARS":
                    func gear(_ gc: CGPoint, _ r: CGFloat, _ teeth: Int, _ ang: Double, _ col: Color) {
                        var p = Path()
                        let steps = teeth * 2
                        for k in 0..<steps {
                            let a = ang + Double(k) / Double(steps) * .pi * 2
                            let rr = k % 2 == 0 ? r : r * 0.78
                            let pt = CGPoint(x: gc.x + CGFloat(cos(a)) * rr, y: gc.y + CGFloat(sin(a)) * rr)
                            if k == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                        }
                        p.closeSubpath()
                        ctx.fill(p, with: .color(col.opacity(0.7)))
                        ctx.stroke(p, with: .color(glow.opacity(0.8)), style: StrokeStyle(lineWidth: lw * 0.4, lineJoin: .round))
                        ctx.fill(Path(ellipseIn: CGRect(x: gc.x - r * 0.25, y: gc.y - r * 0.25, width: r * 0.5, height: r * 0.5)), with: .color(pal.bg))
                    }
                    gear(CGPoint(x: w * 0.40, y: h * 0.54), w * 0.24, 8, t * 0.8, main)
                    gear(CGPoint(x: w * 0.68, y: h * 0.38), w * 0.16, 6, -t * 1.07 + 0.3, glow)
                case "MAZE":
                    let g = 4, cell = w * 0.17, ox = c.x - cell * 2, oy = c.y - cell * 2
                    for i in 0...g {
                        var v = Path(); v.move(to: CGPoint(x: ox + cell * CGFloat(i), y: oy + (i % 2 == 0 ? 0 : cell * 0.5)))
                        v.addLine(to: CGPoint(x: ox + cell * CGFloat(i), y: oy + cell * CGFloat(g) - (i % 3 == 0 ? cell * 0.5 : 0)))
                        ctx.stroke(v, with: .color(main.opacity(0.8)), style: StrokeStyle(lineWidth: lw * 0.6, lineCap: .round))
                        var hz = Path(); hz.move(to: CGPoint(x: ox + (i % 2 == 0 ? 0 : cell * 0.5), y: oy + cell * CGFloat(i)))
                        hz.addLine(to: CGPoint(x: ox + cell * CGFloat(g) - (i % 3 == 1 ? cell * 0.5 : 0), y: oy + cell * CGFloat(i)))
                        ctx.stroke(hz, with: .color(main.opacity(0.8)), style: StrokeStyle(lineWidth: lw * 0.6, lineCap: .round))
                    }
                    let k = (t * 0.6).truncatingRemainder(dividingBy: 1)
                    let ballX: CGFloat = ox + cell * CGFloat(0.35 + 3.0 * k)
                    let ballY: CGFloat = oy + cell * CGFloat(0.35 + abs(sin(k * Double.pi * 2)))
                    let ball = CGRect(x: ballX, y: ballY, width: cell * 0.3, height: cell * 0.3)
                    ctx.fill(Path(ellipseIn: ball), with: .color(glow))
                case "FIRE":
                    for k in 0..<3 {
                        let sway = sin(t * 3 + Double(k) * 1.7) * w * 0.03
                        let bx = w * (0.34 + 0.16 * CGFloat(k)) + CGFloat(sway)
                        let fh = h * (0.50 - 0.10 * abs(CGFloat(k) - 1)) * (0.92 + 0.08 * CGFloat(sin(t * 5 + Double(k))))
                        var p = Path()
                        p.move(to: CGPoint(x: bx, y: h * 0.82))
                        p.addQuadCurve(to: CGPoint(x: bx, y: h * 0.82 - fh), control: CGPoint(x: bx - w * 0.17, y: h * 0.60))
                        p.addQuadCurve(to: CGPoint(x: bx, y: h * 0.82), control: CGPoint(x: bx + w * 0.17, y: h * 0.60))
                        ctx.fill(p, with: .color((k == 1 ? glow : main).opacity(0.85)))
                    }
                default: // WALL
                    for row in 0..<4 {
                        for col in 0..<3 {
                            let off = row % 2 == 0 ? 0 : w * 0.09
                            let r = CGRect(x: w * 0.12 + CGFloat(col) * w * 0.26 + off, y: h * 0.22 + CGFloat(row) * h * 0.14, width: w * 0.24, height: h * 0.12)
                            ctx.fill(Path(roundedRect: r, cornerRadius: 3), with: .color(main.opacity(0.45 + 0.1 * Double((row + col) % 3))))
                        }
                    }
                    let shake = sin(t * 8) * (sin(t * 0.7) > 0.9 ? 2 : 0)
                    ctx.stroke(Path(roundedRect: CGRect(x: w * 0.12 + CGFloat(shake), y: h * 0.20, width: w * 0.78, height: h * 0.60), cornerRadius: 6), with: .color(glow.opacity(0.7)), style: StrokeStyle(lineWidth: lw * 0.5))
                }
    }
}

struct BestiarySheet: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let data = model.loadBestiary()
        return ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                Text("Bestiario")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                if let d = data {
                    Text("\(d.defeated) de \(d.total) vencidos")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(Theme.ink.opacity(0.55))
                    ProgressView(value: Double(d.defeated), total: Double(max(1, d.total)))
                        .tint(Theme.gold)
                        .padding(.horizontal, 30)
                    section("Jefes", d.bosses)
                    section("Guardianes", d.guardians)
                    if let p = d.pantheon { section("Panteón", p) }
                } else {
                    Text("No se pudo cargar").foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer(minLength: 30)
            }
            .padding(.horizontal, 16)
        }
        .background(Theme.background.ignoresSafeArea())
    }

    private func section(_ title: String, _ list: [BestiaryEntry]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(loc(title).uppercased())
                .font(.system(size: 11, weight: .heavy))
                .kerning(3)
                .foregroundColor(Theme.ink.opacity(0.45))
                .padding(.top, 8)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                ForEach(list) { e in cell(e) }
            }
        }
    }

    private func cell(_ e: BestiaryEntry) -> some View {
        VStack(spacing: 8) {
            BossEmblemView(motif: e.motif, size: 84, locked: !e.defeated)
            Text(e.defeated ? e.name : "???")
                .font(.system(size: 14, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(e.defeated ? "\u{201C}\(e.taunt)\u{201D}" : (e.levels.first.map { $0 > 100_000 } == true ? "Sala \(e.first)" : "Nivel \(e.first)"))
                .font(.system(size: 11, weight: .medium, design: e.defeated ? .serif : .default))
                .foregroundColor(Theme.ink.opacity(0.55))
                .multilineTextAlignment(.center)
                .lineLimit(3)
                .frame(minHeight: 42, alignment: .top)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .card(radius: 20)
    }
}
