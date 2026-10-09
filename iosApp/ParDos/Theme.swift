import SwiftUI

extension Color {
    /// Color a partir de un número hexadecimal 0xRRGGBB.
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }
}

/// Colores de la app. En Noche de brujas (1 de octubre – 2 de noviembre, igual que en Android) se visten de Halloween.
enum Theme {
    static let halloween: Bool = {
        let c = Calendar.current.dateComponents([.month, .day], from: Date())
        let month = c.month ?? 0
        let day = c.day ?? 0
        return month == 10 || (month == 11 && day <= 2)
    }()

    static var ink: Color { halloween ? Color(hex: 0x33204D) : Color(hex: 0x3D405B) }
    static var accent: Color { halloween ? Color(hex: 0xE8772E) : Color(hex: 0x6B9E86) }
    static var accentDark: Color { halloween ? Color(hex: 0xC85F1B) : Color(hex: 0x5A8C74) }
    static var energy: Color { halloween ? Color(hex: 0x9B4FC9) : Color(hex: 0xE07A5F) }
    static var paper: Color { halloween ? Color(hex: 0xEFE6F6) : Color(hex: 0xF3EFE6) }
    static var cream: Color { halloween ? Color(hex: 0xFFF7EE) : Color(hex: 0xFFFBF5) }
    static let gold = Color(hex: 0xE0A93B)

    static var background: LinearGradient {
        LinearGradient(colors: [paper, cream], startPoint: .top, endPoint: .bottom)
    }
}

/// Colores de las fichas (los mismos que en Android).
func tileColor(_ value: Int) -> Color {
    switch value {
    case 2: return Color(hex: 0xF5ECDF)
    case 4: return Color(hex: 0xF2DFC2)
    case 8: return Color(hex: 0xF2CC8F)
    case 16: return Color(hex: 0xEDB27A)
    case 32: return Color(hex: 0xE5906A)
    case 64: return Color(hex: 0xD9694C)
    case 128: return Color(hex: 0x81B29A)
    case 256: return Color(hex: 0x5FA08A)
    case 512: return Color(hex: 0x4E8FA6)
    case 1024: return Color(hex: 0x7A74E0)
    case 2048: return Color(hex: 0x3D405B)
    default: return Color(hex: 0x9B4FC9)
    }
}

func tileTextColor(_ value: Int) -> Color {
    if value <= 16 { return Color(hex: 0x5C4F44) }
    if value >= 2048 { return Color(hex: 0xF2CC8F) }
    return Color(hex: 0xFFFBF5)
}

/// Color propio de cada tipo de nivel.
func kindColor(_ kind: String) -> Color {
    switch kind {
    case "ZEN": return Theme.accent
    case "SCORE": return Theme.gold
    case "FOURS": return Theme.energy
    case "HEADSTART": return Color(hex: 0x4E8FA6)
    case "STONES": return Color(hex: 0x7C869B)
    case "SPRINT": return Color(hex: 0xD9694C)
    case "TWINS": return Color(hex: 0x8E6BD6)
    case "CLOCK": return Color(hex: 0xE0782F)
    case "BOSS": return Color(hex: 0xB4413C)
    case "HEAVY": return Color(hex: 0x8D6E63)
    case "LADDER": return Color(hex: 0x2E9E8F)
    case "MARATHON": return Color(hex: 0x4F7CAC)
    case "COMBO": return Color(hex: 0xE0568B)
    case "TWIST": return Color(hex: 0x5C6BC0)
    case "STORM": return Color(hex: 0x3C8DAD)
    case "HARVEST": return Color(hex: 0x7A9A3C)
    case "DOUBLE": return Color(hex: 0xCC7A00)
    default: return Theme.accent
    }
}

/// Icono (SF Symbols) de cada tipo de nivel.
func kindSymbol(_ kind: String) -> String {
    switch kind {
    case "ZEN": return "leaf.fill"
    case "SCORE": return "star.fill"
    case "FOURS": return "4.square.fill"
    case "HEADSTART": return "bolt.fill"
    case "STONES": return "triangle.fill"
    case "SPRINT": return "speedometer"
    case "TWINS": return "square.on.square"
    case "CLOCK": return "timer"
    case "BOSS": return "shield.fill"
    case "HEAVY": return "scalemass.fill"
    case "LADDER": return "list.number"
    case "MARATHON": return "figure.walk"
    case "COMBO": return "flame.fill"
    case "TWIST": return "arrow.triangle.2.circlepath"
    case "STORM": return "cloud.bolt.fill"
    case "HARVEST": return "leaf.arrow.triangle.circlepath"
    case "DOUBLE": return "2.square.fill"
    default: return "circle.fill"
    }
}
