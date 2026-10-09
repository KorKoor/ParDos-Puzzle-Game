import SwiftUI

private func motifGlyph(_ motif: String) -> String {
    switch motif {
    case "DOTS": return "•"
    case "RAYS": return "✧"
    case "WAVES": return "≈"
    case "STARS": return "✦"
    case "LEAVES": return "🍃"
    case "SNOW": return "❄︎"
    case "DIAMONDS": return "◆"
    case "BUBBLES": return "○"
    case "HILLS": return "▲"
    case "EMBERS": return "•"
    case "RAIN": return "╱"
    case "HEARTS": return "♥"
    case "CONFETTI": return "▪︎"
    case "STRIPES": return "▮"
    case "CRYSTALS": return "◇"
    default: return "▦"
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
                    Text(piece.glyph).font(.system(size: width * 0.40))
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

    private var lockedFace: some View {
        ZStack {
            LinearGradient(colors: [Color(hex: 0xD5D1CA), Color(hex: 0xB8B3AB)], startPoint: .top, endPoint: .bottom)
            VStack(spacing: 2) {
                Text("\(piece.n)")
                    .font(.system(size: width * 0.12, weight: .heavy, design: .rounded))
                    .foregroundColor(Color.white.opacity(0.8))
                Text("?")
                    .font(.system(size: width * 0.5, weight: .black, design: .rounded))
                    .foregroundColor(Color.white.opacity(0.7))
            }
        }
    }

    @ViewBuilder
    private var pattern: some View {
        let glyph = motifGlyph(series?.motif ?? "DOTS")
        Canvas { context, size in
            for i in 0..<14 {
                let x = size.width * Double((i * 37) % 100) / 100.0
                let y = size.height * Double((i * 53) % 100) / 100.0
                var layer = context
                layer.opacity = 0.22
                layer.translateBy(x: x, y: y)
                layer.rotate(by: .radians(Double(i) * 0.8))
                layer.draw(Text(glyph).font(.system(size: 13.0)).foregroundColor(accent), at: .zero)
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
