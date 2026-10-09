import SwiftUI

/// Dibujo suelto que se repite de fondo en las cartas de cada serie.
private func motifIcon(_ motif: String) -> String {
    switch motif {
    case "DOTS": return "fx.snow_dot"
    case "RAYS": return "fx.sparkle"
    case "WAVES": return "fx.ring"
    case "STARS": return "fx.star_soft"
    case "LEAVES": return "fx.leaf"
    case "SNOW": return "fx.snowflake"
    case "DIAMONDS": return "fx.diamond"
    case "BUBBLES": return "fx.bubble"
    case "HILLS": return "fx.cloud"
    case "EMBERS": return "fx.ember"
    case "RAIN": return "fx.raindrop"
    case "HEARTS": return "fx.heart_small"
    case "CONFETTI": return "fx.confetti_a"
    case "STRIPES": return "fx.spark"
    case "CRYSTALS": return "fx.crystal"
    default: return "fx.snow_dot"
    }
}

private func luminance(_ rgb: Int) -> Double {
    let r = Double((rgb >> 16) & 0xFF)
    let g = Double((rgb >> 8) & 0xFF)
    let b = Double(rgb & 0xFF)
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
}

private func rarityStars(_ rarity: String) -> Int {
    switch rarity {
    case "RARE": return 2
    case "EPIC": return 3
    case "LEGENDARY": return 4
    default: return 1
    }
}

/// Carta de una pieza del álbum: fondo de su serie, motivo, emoji grande, marco de su rareza. Sin tenerla, una silueta.
struct PieceCard: View {
    let piece: AlbumPiece
    let series: AlbumSeries?
    var owned: Bool = true
    var foil: Bool = false
    var copies: Int = 1
    var isNew: Bool = false
    var width: CGFloat = 96

    private var height: CGFloat { width * 4.0 / 3.0 }
    private var top: Color { Color(hex: UInt32(series?.top ?? 0xD9F2E3)) }
    private var bottom: Color { Color(hex: UInt32(series?.bottom ?? 0x9ED8B5)) }
    private var accent: Color { Color(hex: UInt32(series?.accent ?? 0x3E8A5A)) }
    private var darkBackground: Bool { luminance((series?.top ?? 0xD9F2E3)) + luminance((series?.bottom ?? 0x9ED8B5)) < 0.9 }
    private var textColor: Color { darkBackground ? Color.white : Color(hex: 0x3D2F22) }
    private var starColor: Color { piece.rarity == "COMMON" ? textColor.opacity(0.5) : rarityColor(piece.rarity) }

    var body: some View {
        ZStack {
            if owned {
                ownedFace
            } else {
                lockedFace
            }
        }
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: width * 0.12, style: .continuous))
        .overlay(frame)
        .shadow(color: Color.black.opacity(0.14), radius: 0, x: 0, y: 3)
    }

    private var ownedFace: some View {
        ZStack {
            LinearGradient(colors: [top, bottom], startPoint: .top, endPoint: .bottom)
            pattern
            if foil {
                AngularGradient(colors: [Color(hex: 0xFF9AA2).opacity(0.35), Color(hex: 0xFFE08A).opacity(0.35), Color(hex: 0x9AE0B5).opacity(0.35), Color(hex: 0x9AB8FF).opacity(0.35), Color(hex: 0xFF9AA2).opacity(0.35)], center: .center)
            }
            VStack(spacing: 0) {
                HStack {
                    Text("\(piece.n)")
                        .font(.system(size: width * 0.11, weight: .heavy, design: .rounded))
                        .foregroundColor(textColor.opacity(0.7))
                    Spacer()
                    HStack(spacing: 1) {
                        ForEach(0..<rarityStars(piece.rarity), id: \.self) { _ in
                            Image(systemName: "star.fill").font(.system(size: width * 0.08))
                        }
                    }
                    .foregroundColor(starColor)
                }
                .padding(.horizontal, width * 0.08)
                .padding(.top, width * 0.07)
                Spacer(minLength: 0)
                ZStack {
                    Circle().fill(Color.white.opacity(darkBackground ? 0.16 : 0.45)).frame(width: width * 0.62, height: width * 0.62)
                    pieceArt
                }
                Spacer(minLength: 0)
                Text(piece.name)
                    .font(.system(size: width * 0.115, weight: .black, design: .rounded))
                    .foregroundColor(textColor)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.7)
                    .padding(.horizontal, width * 0.06)
                    .padding(.bottom, width * 0.08)
            }
            if copies > 1 {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Text("×\(copies)")
                            .font(.system(size: width * 0.12, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Capsule().fill(accent))
                    }
                    .padding(6)
                }
                .padding(.bottom, width * 0.22)
            }
            if isNew {
                VStack {
                    HStack {
                        Spacer()
                        Text("NUEVA")
                            .font(.system(size: width * 0.095, weight: .black))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Capsule().fill(Color(hex: 0xE0475B)))
                    }
                    .padding(.top, width * 0.2)
                    .padding(.trailing, 4)
                    Spacer()
                }
            }
        }
    }

    /// La ilustración de la pieza; si faltara, una insignia tallada.
    @ViewBuilder
    private var pieceArt: some View {
        if ArtLibrary.shared.has("piece.\(piece.id)") {
            ArtView(id: "piece.\(piece.id)")
                .frame(width: width * 0.6, height: width * 0.6)
        } else {
            PieceEmblem(n: piece.n, rarity: piece.rarity, accent: accent, dark: darkBackground)
                .frame(width: width * 0.58, height: width * 0.58)
        }
    }

    private var lockedFace: some View {
        ZStack {
            LinearGradient(colors: [Color(hex: 0xD5D1CA), Color(hex: 0xB8B3AB)], startPoint: .top, endPoint: .bottom)
            VStack(spacing: 2) {
                Text("\(piece.n)")
                    .font(.system(size: width * 0.12, weight: .heavy, design: .rounded))
                    .foregroundColor(Color.white.opacity(0.8))
                if ArtLibrary.shared.has("piece.\(piece.id)") {
                    ArtView(id: "piece.\(piece.id)")
                        .colorMultiply(Color(white: 0.3))
                        .opacity(0.5)
                        .frame(width: width * 0.6, height: width * 0.6)
                } else {
                    Text("?")
                        .font(.system(size: width * 0.5, weight: .black, design: .rounded))
                        .foregroundColor(Color.white.opacity(0.7))
                }
            }
        }
    }

    @ViewBuilder
    private var pattern: some View {
        let icon = motifIcon(series?.motif ?? "DOTS")
        let color = ArtColor.from(accent)
        let pal: ArtPalette = ["c": color, "c2": ArtColor.mix(color, ArtColor.hex(0xFFFFFF), 0.5)]
        Canvas { context, size in
            for i in 0..<9 {
                let x = size.width * Double((i * 37) % 100) / 100.0
                let y = size.height * Double((i * 53) % 100) / 100.0
                let side = 15.0 + Double((i * 5) % 3) * 4.0
                let rect = CGRect(x: x - side / 2, y: y - side / 2, width: side, height: side)
                ArtLibrary.shared.draw(icon, context, in: rect, palette: pal, opacity: 0.3, rotation: Double(i) * 0.8)
            }
        }
    }

    @ViewBuilder
    private var frame: some View {
        let r = piece.rarity
        if !owned {
            RoundedRectangle(cornerRadius: width * 0.12, style: .continuous).stroke(Color.white.opacity(0.6), lineWidth: 2)
        } else if r == "LEGENDARY" {
            RoundedRectangle(cornerRadius: width * 0.12, style: .continuous)
                .stroke(LinearGradient(colors: [Color(hex: 0xFFF1B5), Color(hex: 0xC98A1B), Color(hex: 0xFFFAD6), Color(hex: 0xC98A1B)], startPoint: .topLeading, endPoint: .bottomTrailing), lineWidth: 3.5)
        } else if r == "EPIC" {
            RoundedRectangle(cornerRadius: width * 0.12, style: .continuous)
                .stroke(LinearGradient(colors: [Color(hex: 0xD9B8FF), Color(hex: 0x8A4FE0)], startPoint: .top, endPoint: .bottom), lineWidth: 3)
        } else if r == "RARE" {
            RoundedRectangle(cornerRadius: width * 0.12, style: .continuous)
                .stroke(LinearGradient(colors: [Color(hex: 0x9BD0FF), Color(hex: 0x3F8FE0)], startPoint: .top, endPoint: .bottom), lineWidth: 2.5)
        } else {
            RoundedRectangle(cornerRadius: width * 0.12, style: .continuous).stroke(Color(hex: 0xB9B2A0), lineWidth: 2)
        }
    }
}

// MARK: - Insignias (sin emojis)

private func lighter(_ c: ArtRGBA, _ amount: Double) -> Color {
    return Color(.sRGB, red: c.r + (1 - c.r) * amount, green: c.g + (1 - c.g) * amount, blue: c.b + (1 - c.b) * amount, opacity: 1)
}

private func darker(_ c: ArtRGBA, _ amount: Double) -> Color {
    return Color(.sRGB, red: c.r * (1 - amount), green: c.g * (1 - amount), blue: c.b * (1 - amount), opacity: 1)
}

/// Piedra tallada que representa a una pieza del álbum: la forma depende del número de la pieza (1 a 10) y el adorno de su rareza.
struct PieceEmblem: View {
    let n: Int
    let rarity: String
    let accent: Color
    let dark: Bool

    var body: some View {
        Canvas { context, size in
            let side = min(size.width, size.height)
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let r = side * 0.40
            let base = ArtColor.from(accent).resolve([:], [:])
            let top = lighter(base, dark ? 0.55 : 0.35)
            let deep = darker(base, 0.18)
            let edge = darker(base, 0.45)
            let outline = PieceEmblem.outline(n, center: center, radius: r)

            if rarity == "LEGENDARY" {
                for k in 0..<12 {
                    let angle = Double(k) / 12.0 * 2.0 * Double.pi
                    var ray = Path()
                    ray.move(to: center)
                    ray.addLine(to: CGPoint(x: center.x + CGFloat(cos(angle - 0.09)) * side * 0.5, y: center.y + CGFloat(sin(angle - 0.09)) * side * 0.5))
                    ray.addLine(to: CGPoint(x: center.x + CGFloat(cos(angle + 0.09)) * side * 0.5, y: center.y + CGFloat(sin(angle + 0.09)) * side * 0.5))
                    ray.closeSubpath()
                    context.fill(ray, with: .color(Color(hex: 0xFFD36E).opacity(0.4)))
                }
            }
            context.fill(
                Path(ellipseIn: CGRect(x: center.x - r * 0.8, y: center.y + r * 0.98, width: r * 1.6, height: r * 0.26)),
                with: .color(Color.black.opacity(0.14))
            )
            context.fill(
                outline,
                with: GraphicsContext.Shading.linearGradient(
                    Gradient(colors: [top, deep]),
                    startPoint: CGPoint(x: center.x - r * 0.4, y: center.y - r),
                    endPoint: CGPoint(x: center.x + r * 0.4, y: center.y + r)
                )
            )
            if rarity != "COMMON" {
                context.stroke(outline, with: .color(rarityColor(rarity)), style: StrokeStyle(lineWidth: side * 0.05, lineJoin: .round))
            } else {
                context.stroke(outline, with: .color(edge), style: StrokeStyle(lineWidth: side * 0.03, lineJoin: .round))
            }
            let inner = PieceEmblem.outline(n, center: center, radius: r * 0.64)
            context.fill(inner, with: .color(Color.white.opacity(0.2)))
            context.stroke(inner, with: .color(Color.white.opacity(0.5)), style: StrokeStyle(lineWidth: side * 0.02, lineJoin: .round))
            for k in 0..<6 {
                let angle = Double(k) / 6.0 * 2.0 * Double.pi + 0.5
                var spoke = Path()
                spoke.move(to: CGPoint(x: center.x + CGFloat(cos(angle)) * r * 0.2, y: center.y + CGFloat(sin(angle)) * r * 0.2))
                spoke.addLine(to: CGPoint(x: center.x + CGFloat(cos(angle)) * r * 0.62, y: center.y + CGFloat(sin(angle)) * r * 0.62))
                context.stroke(spoke, with: .color(Color.white.opacity(0.28)), lineWidth: side * 0.012)
            }
            context.fill(
                Path(ellipseIn: CGRect(x: center.x - r * 0.62, y: center.y - r * 0.78, width: r * 0.7, height: r * 0.34)),
                with: .color(Color.white.opacity(0.5))
            )
            if rarity == "EPIC" || rarity == "LEGENDARY" {
                let pal: ArtPalette = ["c": ArtColor.hex(0xFFFFFF), "c2": ArtColor.hex(0xFFE9A0)]
                ArtLibrary.shared.draw("fx.sparkle", context, in: CGRect(x: center.x + r * 0.55, y: center.y - r * 1.15, width: side * 0.28, height: side * 0.28), palette: pal)
                ArtLibrary.shared.draw("fx.sparkle", context, in: CGRect(x: center.x - r * 1.2, y: center.y + r * 0.1, width: side * 0.2, height: side * 0.2), palette: pal)
            }
        }
        .accessibilityHidden(true)
    }

    static func outline(_ n: Int, center c: CGPoint, radius r: CGFloat) -> Path {
        var p = Path()
        switch (n - 1) % 10 {
        case 0:
            p.addEllipse(in: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2))
        case 1:
            polygon(&p, c, r, 6, -Double.pi / 2)
        case 2:
            p.move(to: CGPoint(x: c.x, y: c.y - r * 1.1))
            p.addLine(to: CGPoint(x: c.x + r * 0.85, y: c.y))
            p.addLine(to: CGPoint(x: c.x, y: c.y + r * 1.1))
            p.addLine(to: CGPoint(x: c.x - r * 0.85, y: c.y))
            p.closeSubpath()
        case 3:
            p.addRoundedRect(in: CGRect(x: c.x - r * 0.92, y: c.y - r * 0.92, width: r * 1.84, height: r * 1.84), cornerSize: CGSize(width: r * 0.5, height: r * 0.5))
        case 4:
            star(&p, c, r * 1.05, r * 0.5, 5)
        case 5:
            let steps = 72
            for k in 0...steps {
                let angle = Double(k) / Double(steps) * 2.0 * Double.pi
                let radius = Double(r) * (0.8 + 0.2 * cos(6.0 * angle))
                let pt = CGPoint(x: c.x + CGFloat(cos(angle) * radius), y: c.y + CGFloat(sin(angle) * radius))
                if k == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
            }
            p.closeSubpath()
        case 6:
            p.move(to: CGPoint(x: c.x - r * 0.8, y: c.y - r * 0.85))
            p.addLine(to: CGPoint(x: c.x + r * 0.8, y: c.y - r * 0.85))
            p.addLine(to: CGPoint(x: c.x + r * 0.8, y: c.y + r * 0.1))
            p.addCurve(to: CGPoint(x: c.x, y: c.y + r), control1: CGPoint(x: c.x + r * 0.8, y: c.y + r * 0.6), control2: CGPoint(x: c.x + r * 0.3, y: c.y + r * 0.85))
            p.addCurve(to: CGPoint(x: c.x - r * 0.8, y: c.y + r * 0.1), control1: CGPoint(x: c.x - r * 0.3, y: c.y + r * 0.85), control2: CGPoint(x: c.x - r * 0.8, y: c.y + r * 0.6))
            p.closeSubpath()
        case 7:
            star(&p, c, r * 1.08, r * 0.8, 16)
        case 8:
            p.move(to: CGPoint(x: c.x, y: c.y - r))
            p.addCurve(to: CGPoint(x: c.x + r * 0.85, y: c.y + r * 0.4), control1: CGPoint(x: c.x + r * 0.2, y: c.y - r * 0.55), control2: CGPoint(x: c.x + r * 0.85, y: c.y + r * 0.05))
            p.addCurve(to: CGPoint(x: c.x, y: c.y + r), control1: CGPoint(x: c.x + r * 0.85, y: c.y + r * 0.85), control2: CGPoint(x: c.x + r * 0.45, y: c.y + r))
            p.addCurve(to: CGPoint(x: c.x - r * 0.85, y: c.y + r * 0.4), control1: CGPoint(x: c.x - r * 0.45, y: c.y + r), control2: CGPoint(x: c.x - r * 0.85, y: c.y + r * 0.85))
            p.addCurve(to: CGPoint(x: c.x, y: c.y - r), control1: CGPoint(x: c.x - r * 0.85, y: c.y + r * 0.05), control2: CGPoint(x: c.x - r * 0.2, y: c.y - r * 0.55))
            p.closeSubpath()
        default:
            let k: CGFloat = r * 0.85
            p.move(to: CGPoint(x: c.x, y: c.y + k))
            p.addCurve(to: CGPoint(x: c.x, y: c.y - k * 0.35), control1: CGPoint(x: c.x - k * 1.35, y: c.y + k * 0.05), control2: CGPoint(x: c.x - k, y: c.y - k * 1.05))
            p.addCurve(to: CGPoint(x: c.x, y: c.y + k), control1: CGPoint(x: c.x + k, y: c.y - k * 1.05), control2: CGPoint(x: c.x + k * 1.35, y: c.y + k * 0.05))
            p.closeSubpath()
        }
        return p
    }

    private static func polygon(_ p: inout Path, _ c: CGPoint, _ r: CGFloat, _ sides: Int, _ start: Double) {
        for k in 0..<sides {
            let angle = start + Double(k) / Double(sides) * 2.0 * Double.pi
            let pt = CGPoint(x: c.x + CGFloat(cos(angle)) * r, y: c.y + CGFloat(sin(angle)) * r)
            if k == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
        }
        p.closeSubpath()
    }

    private static func star(_ p: inout Path, _ c: CGPoint, _ outer: CGFloat, _ inner: CGFloat, _ points: Int) {
        let total = points * 2
        for k in 0..<total {
            let radius = k % 2 == 0 ? outer : inner
            let angle = -Double.pi / 2 + Double(k) / Double(total) * 2.0 * Double.pi
            let pt = CGPoint(x: c.x + CGFloat(cos(angle)) * radius, y: c.y + CGFloat(sin(angle)) * radius)
            if k == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
        }
        p.closeSubpath()
    }
}

/// Escudo redondo de una serie del álbum (sustituye al emoji de la serie).
struct SeriesCrest: View {
    let series: AlbumSeries
    var size: CGFloat = 28

    var body: some View {
        let accent = Color(hex: UInt32(truncatingIfNeeded: series.accent))
        let top = Color(hex: UInt32(truncatingIfNeeded: series.top))
        let bottom = Color(hex: UInt32(truncatingIfNeeded: series.bottom))
        return ZStack {
            Circle().fill(LinearGradient(colors: [top, bottom], startPoint: .top, endPoint: .bottom))
            Circle().stroke(accent, lineWidth: max(1.5, size * 0.07))
            if ArtLibrary.shared.has("piece.\(series.id)_8") {
                ArtView(id: "piece.\(series.id)_8")
                    .frame(width: size * 0.78, height: size * 0.78)
            } else {
                PieceEmblem(n: 5, rarity: "COMMON", accent: accent, dark: false)
                    .frame(width: size * 0.7, height: size * 0.7)
            }
        }
        .frame(width: size, height: size)
    }
}
