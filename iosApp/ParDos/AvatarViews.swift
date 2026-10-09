import SwiftUI

// Avatares y banners del perfil. En Android se dibujan a mano con Canvas; aquí se componen con emojis y degradados
// a partir de los mismos datos (animal, accesorio, variante, marco, patrón y colores).

private let classicEmojis = ["🦊", "🐱", "🐼", "🐰", "🐻", "🐸", "🦉", "🐧", "🐨", "🦝"]

func animalEmoji(_ animal: String) -> String {
    switch animal {
    case "FOX": return "🦊"
    case "CAT": return "🐱"
    case "PANDA": return "🐼"
    case "BUNNY": return "🐰"
    case "BEAR": return "🐻"
    case "FROG": return "🐸"
    case "OWL": return "🦉"
    case "PENGUIN": return "🐧"
    case "KOALA": return "🐨"
    case "RACCOON": return "🦝"
    case "CHICK": return "🐥"
    case "UNICORN": return "🦄"
    case "DRAGON": return "🐲"
    case "AXOLOTL": return "🦎"
    case "CAPYBARA": return "🦫"
    case "TIGER": return "🐯"
    case "LION": return "🦁"
    case "WOLF": return "🐺"
    case "SHEEP": return "🐑"
    case "HEDGEHOG": return "🦔"
    case "TURTLE": return "🐢"
    case "PIG": return "🐷"
    case "MONKEY": return "🐵"
    case "HAMSTER": return "🐹"
    case "DEER": return "🦌"
    case "COW": return "🐮"
    case "DUCK": return "🦆"
    case "BAT": return "🦇"
    case "GHOST": return "👻"
    case "PUMPKIN": return "🎃"
    case "ROBOT": return "🤖"
    case "ALIEN": return "👽"
    case "DINO": return "🦖"
    case "PHOENIX": return "🦅"
    default: return "🙂"
    }
}

func accessoryEmoji(_ accessory: String) -> String? {
    switch accessory {
    case "CROWN": return "👑"
    case "HELMET": return "⛑️"
    case "HEADBAND": return "🎀"
    case "MOON": return "🌙"
    case "STAR": return "⭐️"
    case "SCARF": return "🧣"
    case "FLOWERS": return "🌸"
    case "BOW": return "🎀"
    case "GLASSES": return "👓"
    case "WIZARD": return "🪄"
    case "HEADPHONES": return "🎧"
    case "FLOWER_CROWN": return "🌼"
    case "WITCH": return "🧙"
    case "PIRATE": return "🏴‍☠️"
    case "CHEF": return "🍳"
    case "SANTA": return "🎅"
    case "DEVIL": return "😈"
    case "HALO": return "😇"
    case "NINJA": return "🥷"
    case "TOPHAT": return "🎩"
    case "CAP": return "🧢"
    case "SUNGLASSES": return "😎"
    case "CAPE": return "🦇"
    case "PARTY": return "🥳"
    default: return nil
    }
}

private func variantColors(_ variant: String) -> [Color] {
    switch variant {
    case "GOLD": return [Color(hex: 0xFFE9A0), Color(hex: 0xE0A93B)]
    case "MIDNIGHT": return [Color(hex: 0x4A4F9A), Color(hex: 0x1B1E4A)]
    case "JADE": return [Color(hex: 0xBDEBD0), Color(hex: 0x4FB58A)]
    case "RAINBOW": return [Color(hex: 0xFF9AA2), Color(hex: 0xFFE08A), Color(hex: 0x9AE0B5), Color(hex: 0x9AB8FF)]
    case "SAKURA": return [Color(hex: 0xFFD6E5), Color(hex: 0xF29BB5)]
    case "FROST": return [Color(hex: 0xDDF1FF), Color(hex: 0x8EC9F0)]
    case "EMBER": return [Color(hex: 0xFFC27A), Color(hex: 0xE0502B)]
    case "SHADOW": return [Color(hex: 0x6B5F82), Color(hex: 0x2C2540)]
    case "CANDY": return [Color(hex: 0xFFD1E8), Color(hex: 0xB59BFF)]
    case "GALAXY": return [Color(hex: 0x6A44B8), Color(hex: 0x1B0F4A)]
    case "PLATINUM": return [Color(hex: 0xF2F4F8), Color(hex: 0xA9B3C6)]
    default: return [Color(hex: 0xFFEFD2), Color(hex: 0xF4CC95)]
    }
}

struct AvatarView: View {
    @EnvironmentObject var model: AppModel
    let id: Int
    var size: CGFloat = 48

    var body: some View {
        let item = model.avatar(id)
        let animal = item?.animal
        let emoji: String = animal != nil ? animalEmoji(animal ?? "") : classicEmojis[max(0, min(id - 1, classicEmojis.count - 1))]
        let accessory: String? = item != nil ? accessoryEmoji(item?.accessory ?? "NONE") : nil
        let colors = variantColors(item?.variant ?? "NORMAL")
        let frame = item?.frame ?? "NONE"
        return ZStack {
            Circle()
                .fill(LinearGradient(colors: colors, startPoint: .top, endPoint: .bottom))
            Text(emoji)
                .font(.system(size: size * 0.56))
                .offset(y: size * 0.04)
            if let accessory = accessory {
                Text(accessory)
                    .font(.system(size: size * 0.30))
                    .offset(y: -size * 0.33)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(frameRing(frame))
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

private func patternGlyph(_ pattern: String) -> String {
    switch pattern {
    case "PETALS": return "❀"
    case "LEAVES", "FOREST": return "🍃"
    case "BUBBLES": return "○"
    case "SNOW": return "❄︎"
    case "EMBERS": return "•"
    case "BATS": return "🦇"
    case "PUMPKINS": return "🎃"
    case "HEARTS": return "♥"
    case "FIREWORKS": return "✺"
    case "STARS", "GALAXY": return "✦"
    case "LANTERNS": return "🏮"
    case "CONFETTI": return "▪︎"
    case "RAIN": return "╱"
    case "CRYSTALS", "DIAMONDS": return "◆"
    case "CLOUDS": return "☁︎"
    case "ZEN": return "◌"
    case "CIRCUIT": return "┼"
    case "AURORA": return "∿"
    case "SUNSET": return "☀︎"
    case "MOUNTAINS", "HILLS", "SKYLINE": return "▲"
    case "WAVES": return "≈"
    case "RAYS": return "✧"
    default: return "•"
    }
}

/// Fondo de la tarjeta de jugador: degradado y un dibujo repetido del color de acento.
struct BannerView: View {
    @EnvironmentObject var model: AppModel
    let id: Int
    var height: CGFloat = 90

    var body: some View {
        let item = model.banner(id)
        let top = Color(hex: UInt32(item?.top ?? 0xF3EFE6))
        let bottom = Color(hex: UInt32(item?.bottom ?? 0xE8E0D0))
        let accent = Color(hex: UInt32(item?.accent ?? 0xB8A58A))
        let glyph = patternGlyph(item?.pattern ?? "DOTS")
        return ZStack {
            LinearGradient(colors: [top, bottom], startPoint: .top, endPoint: .bottom)
            Canvas { context, size in
                let columns = 9
                let rows = 3
                for r in 0..<rows {
                    for c in 0..<columns {
                        let index = r * columns + c
                        let x = size.width * (Double(c) + 0.5 + (r % 2 == 0 ? 0.0 : 0.5)) / Double(columns)
                        let y = size.height * (Double(r) + 0.5) / Double(rows)
                        let wobble = 1.0 + 0.4 * sin(Double(index) * 1.7)
                        var layer = context
                        layer.opacity = 0.35
                        layer.translateBy(x: x, y: y)
                        layer.rotate(by: .radians(Double(index) * 0.7))
                        layer.draw(Text(glyph).font(.system(size: 13.0 * wobble)).foregroundColor(accent), at: .zero)
                    }
                }
            }
        }
        .frame(height: height)
        .clipped()
    }
}
