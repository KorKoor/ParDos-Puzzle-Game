import SwiftUI

// Iconos y piezas de interfaz que se repiten por toda la app.

func rarityColor(_ rarity: String) -> Color {
    switch rarity {
    case "RARE": return Color(hex: 0x3F8FE0)
    case "EPIC": return Color(hex: 0x8A4FE0)
    case "LEGENDARY": return Color(hex: 0xE0A93B)
    default: return Color(hex: 0x9C9484)
    }
}

func rarityName(_ rarity: String) -> String {
    switch rarity {
    case "RARE": return "Rara"
    case "EPIC": return "Épica"
    case "LEGENDARY": return "Legendaria"
    default: return "Común"
    }
}

func chestName(_ type: String) -> String {
    switch type {
    case "RARE": return "Cofre raro"
    case "EPIC": return "Cofre épico"
    default: return "Cofre común"
    }
}

func formatNumber(_ n: Int) -> String {
    if n >= 10000 {
        let thousands = Double(n) / 1000.0
        return String(format: "%.1fk", thousands)
    }
    return "\(n)"
}

func formatClock(ms: Int) -> String {
    let total = max(0, ms) / 1000
    let h = total / 3600
    let m = (total % 3600) / 60
    let s = total % 60
    if h > 0 { return String(format: "%d:%02d:%02d", h, m, s) }
    return String(format: "%d:%02d", m, s)
}

struct CoinIcon: View {
    var size: CGFloat = 22

    var body: some View {
        ZStack {
            Circle()
                .fill(LinearGradient(colors: [Color(hex: 0xFFE08A), Color(hex: 0xE0A93B)], startPoint: .top, endPoint: .bottom))
            Circle()
                .stroke(Color(hex: 0xB8791A), lineWidth: size * 0.07)
            Circle()
                .stroke(Color(hex: 0xFFF1B5), lineWidth: size * 0.05)
                .padding(size * 0.17)
            Image(systemName: "star.fill")
                .font(.system(size: size * 0.42, weight: .black))
                .foregroundColor(Color(hex: 0xB8791A))
        }
        .frame(width: size, height: size)
    }
}

struct GemShape: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let h = rect.height
        var p = Path()
        p.move(to: CGPoint(x: w * 0.25, y: h * 0.10))
        p.addLine(to: CGPoint(x: w * 0.75, y: h * 0.10))
        p.addLine(to: CGPoint(x: w * 0.96, y: h * 0.38))
        p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.94))
        p.addLine(to: CGPoint(x: w * 0.04, y: h * 0.38))
        p.closeSubpath()
        return p
    }
}

struct GemFacets: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let h = rect.height
        var p = Path()
        p.move(to: CGPoint(x: w * 0.04, y: h * 0.38))
        p.addLine(to: CGPoint(x: w * 0.96, y: h * 0.38))
        p.move(to: CGPoint(x: w * 0.25, y: h * 0.10))
        p.addLine(to: CGPoint(x: w * 0.38, y: h * 0.38))
        p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.94))
        p.move(to: CGPoint(x: w * 0.75, y: h * 0.10))
        p.addLine(to: CGPoint(x: w * 0.62, y: h * 0.38))
        p.addLine(to: CGPoint(x: w * 0.50, y: h * 0.94))
        return p
    }
}

struct GemIcon: View {
    var size: CGFloat = 22

    var body: some View {
        ZStack {
            GemShape()
                .fill(LinearGradient(colors: [Color(hex: 0x9BE7FF), Color(hex: 0x3FA7E8)], startPoint: .top, endPoint: .bottom))
            GemFacets()
                .stroke(Color.white.opacity(0.65), lineWidth: max(1, size * 0.045))
            GemShape()
                .stroke(Color(hex: 0x2A78B8), lineWidth: max(1, size * 0.06))
        }
        .frame(width: size, height: size)
    }
}

struct ChestIcon: View {
    var type: String = "COMMON"
    var size: CGFloat = 44

    private var body1: Color {
        switch type {
        case "RARE": return Color(hex: 0x3F8FE0)
        case "EPIC": return Color(hex: 0x8A4FE0)
        default: return Color(hex: 0xB9792F)
        }
    }

    private var body2: Color {
        switch type {
        case "RARE": return Color(hex: 0x2A64A8)
        case "EPIC": return Color(hex: 0x5A2FA8)
        default: return Color(hex: 0x8A5418)
        }
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.12, style: .continuous)
                .fill(LinearGradient(colors: [body1, body2], startPoint: .top, endPoint: .bottom))
                .frame(width: size * 0.9, height: size * 0.56)
                .offset(y: size * 0.14)
            RoundedRectangle(cornerRadius: size * 0.22, style: .continuous)
                .fill(LinearGradient(colors: [body1.opacity(0.95), body1], startPoint: .top, endPoint: .bottom))
                .frame(width: size * 0.9, height: size * 0.34)
                .offset(y: -size * 0.14)
            Rectangle()
                .fill(Color(hex: 0xFFD36E))
                .frame(width: size * 0.9, height: size * 0.06)
                .offset(y: size * 0.04)
            RoundedRectangle(cornerRadius: size * 0.05, style: .continuous)
                .fill(Color(hex: 0xFFD36E))
                .frame(width: size * 0.2, height: size * 0.24)
                .offset(y: size * 0.06)
            Circle()
                .fill(body2)
                .frame(width: size * 0.07, height: size * 0.07)
                .offset(y: size * 0.07)
        }
        .frame(width: size, height: size)
    }
}

/// Moneda o gema con su cantidad: la pastilla de arriba en cada pantalla.
struct AmountPill: View {
    enum Kind { case coin, gem, shard, token }
    let kind: Kind
    let amount: Int
    var plus: Bool = false

    var body: some View {
        HStack(spacing: 5) {
            icon
            Text(formatNumber(amount))
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink)
            if plus {
                Image(systemName: "plus.circle.fill")
                    .font(.system(size: 14))
                    .foregroundColor(Theme.accent)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            Capsule()
                .fill(LinearGradient(colors: [Color.white, Color(hex: 0xFBF7EE)], startPoint: .top, endPoint: .bottom))
                .overlay(Capsule().strokeBorder(Color.white, lineWidth: 1))
        )
        .overlay(Capsule().stroke(Theme.ink.opacity(0.06), lineWidth: 1))
        .shadow(color: Theme.ink.opacity(0.10), radius: 0, x: 0, y: 2)
    }

    @ViewBuilder
    private var icon: some View {
        switch kind {
        case .coin: CoinIcon(size: 20)
        case .gem: GemIcon(size: 20)
        case .shard:
            Image(systemName: "sparkle").font(.system(size: 15, weight: .bold)).foregroundColor(Color(hex: 0xB57CF0))
        case .token:
            Image(systemName: "arrow.left.arrow.right.circle.fill").font(.system(size: 16)).foregroundColor(Color(hex: 0x2E9E8F))
        }
    }
}

/// Barra de monedas y gemas, con un toque para ir a la tienda.
struct CurrencyBar: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        HStack(spacing: 8) {
            Button(action: { model.tab = .shop }) {
                AmountPill(kind: .coin, amount: model.coins, plus: true)
            }
            Button(action: { model.tab = .shop }) {
                AmountPill(kind: .gem, amount: model.gems, plus: true)
            }
            Spacer()
        }
    }
}

extension Color {
    /// Aclara (amount > 0) u oscurece (amount < 0) un color mezclándolo con blanco o con negro.
    func shade(_ amount: Double) -> Color {
        let c = ArtColor.from(self).resolve([:], [:])
        let target: Double = amount >= 0 ? 1 : 0
        let k = abs(amount)
        return Color(.sRGB, red: c.r + (target - c.r) * k, green: c.g + (target - c.g) * k, blue: c.b + (target - c.b) * k, opacity: c.a)
    }
}

/// Tarjeta de la app: relieve de juguete (borde inferior sólido), brillo arriba, borde claro y una sombra suave que da profundidad.
struct CardBackground: ViewModifier {
    var radius: CGFloat = 22
    var fill: Color = Color.white

    func body(content: Content) -> some View {
        let soft = PowerMonitor.shared.heavyEffects
        return content
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: radius, style: .continuous).fill(fill)
                    RoundedRectangle(cornerRadius: radius, style: .continuous)
                        .fill(LinearGradient(colors: [Color.white.opacity(0.55), Color.white.opacity(0)], startPoint: .top, endPoint: UnitPoint(x: 0.5, y: 0.45)))
                    RoundedRectangle(cornerRadius: radius, style: .continuous)
                        .strokeBorder(Color.white.opacity(0.85), lineWidth: 1.2)
                }
            )
            .overlay(RoundedRectangle(cornerRadius: radius, style: .continuous).stroke(Theme.ink.opacity(0.06), lineWidth: 1))
            .shadow(color: Theme.ink.opacity(0.10), radius: 0, x: 0, y: 3)
            .shadow(color: Theme.ink.opacity(soft ? 0.07 : 0), radius: 10, x: 0, y: 7)
    }
}

extension View {
    func card(radius: CGFloat = 22, fill: Color = Color.white) -> some View {
        modifier(CardBackground(radius: radius, fill: fill))
    }
}

/// Se hunde un poco al tocarlo, como un botón de juguete.
struct ToyPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.965 : 1)
            .offset(y: configuration.isPressed ? 2 : 0)
            .animation(.spring(response: 0.22, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

/// Botón grande de acción: degradado, brillo y borde inferior que se hunde al tocar.
struct BigButton: View {
    let title: String
    var color: Color = Theme.accent
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        let base = enabled ? color : Color.gray.opacity(0.45)
        return Button(action: action) {
            Text(loc(title))
                .font(.system(size: 16, weight: .black, design: .rounded))
                .kerning(1.5)
                .foregroundColor(.white)
                .shadow(color: Color.black.opacity(0.18), radius: 0, x: 0, y: 1)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(
                    ZStack {
                        RoundedRectangle(cornerRadius: 18, style: .continuous).fill(base.shade(-0.28)).offset(y: 4)
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(LinearGradient(colors: [base.shade(0.16), base], startPoint: .top, endPoint: .bottom))
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(LinearGradient(colors: [Color.white.opacity(0.32), Color.white.opacity(0)], startPoint: .top, endPoint: .center))
                            .padding(2)
                        RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(Color.white.opacity(0.3), lineWidth: 1)
                    }
                )
                .padding(.bottom, 4)
        }
        .buttonStyle(ToyPressStyle())
        .disabled(!enabled)
    }
}

/// Pequeña etiqueta con un número encima de un icono (por ejemplo, "3 cofres").
struct CountBadge: View {
    let count: Int

    var body: some View {
        Text("\(count)")
            .font(.system(size: 11, weight: .black, design: .rounded))
            .foregroundColor(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Capsule().fill(Color(hex: 0xE0475B)))
    }
}

struct SectionTitle: View {
    let text: String
    var detail: String? = nil

    var body: some View {
        HStack(spacing: 8) {
            Capsule().fill(Theme.accent).frame(width: 4, height: 13)
            Text(loc(text).uppercased())
                .font(.system(size: 11, weight: .heavy))
                .kerning(2.5)
                .foregroundColor(Theme.ink.opacity(0.5))
            Spacer()
            if let detail = detail {
                Text(loc(detail))
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.4))
            }
        }
        .padding(.horizontal, 4)
    }
}
