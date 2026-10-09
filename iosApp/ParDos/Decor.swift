import SwiftUI

/// Imagen 3D de la carpeta de recursos (los mismos iconos que en Android).
struct SpriteImage: View {
    let name: String
    let size: CGFloat

    var body: some View {
        Image(name)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
    }
}

/// Icono que flota y se balancea: para botones y tarjetas.
struct FloatingSprite: View {
    let name: String
    let size: CGFloat
    var tilt: Double = 0
    var phase: Double = 0
    @State private var up = false

    var body: some View {
        SpriteImage(name: name, size: size)
            .rotationEffect(.degrees(tilt + (up ? 6 : -6)))
            .offset(y: up ? -4 : 4)
            .onAppear {
                withAnimation(Animation.easeInOut(duration: 1.6 + phase).repeatForever(autoreverses: true)) {
                    up = true
                }
            }
    }
}

/// Fondo de temporada: en Noche de brujas suben despacio calabazas, fantasmas, murciélagos y dulces, con telarañas en las esquinas.
struct SeasonBackdrop: View {
    private let names = ["ico_pumpkin", "ico_ghost", "ico_bat", "ico_candy", "ico_skull", "ico_lollipop"]

    var body: some View {
        if Theme.halloween {
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    drawSprites(context, size, t)
                    drawWebs(context, size)
                }
            }
            .allowsHitTesting(false)
            .ignoresSafeArea()
        }
    }

    private func drawSprites(_ context: GraphicsContext, _ size: CGSize, _ t: Double) {
        for i in 0..<9 {
            let image = context.resolve(Image(names[i % names.count]))
            let speed = 0.012 + 0.006 * Double(i % 4)
            let phase = Double(i) * 0.37
            let progress = (t * speed + phase).truncatingRemainder(dividingBy: 1.0)
            let side = 38.0 + 12.0 * Double(i % 3)
            let baseX = size.width * (0.1 + 0.8 * Double((i * 37) % 100) / 100.0)
            let x = baseX + 18.0 * sin(t * 0.5 + phase * 6.0)
            let y = size.height * (1.1 - 1.2 * progress)
            let fade = sin(progress * Double.pi)
            var layer = context
            layer.opacity = 0.16 * fade
            layer.draw(image, in: CGRect(x: x - side / 2, y: y - side / 2, width: side, height: side))
        }
    }

    private func drawWebs(_ context: GraphicsContext, _ size: CGSize) {
        let web = context.resolve(Image("ico_web"))
        var layer = context
        layer.opacity = 0.35
        let big = size.width * 0.34
        layer.draw(web, in: CGRect(x: -big * 0.1, y: -big * 0.1, width: big, height: big))
        var mirrored = context
        mirrored.opacity = 0.25
        mirrored.translateBy(x: size.width, y: 0)
        mirrored.scaleBy(x: -1, y: 1)
        let small = size.width * 0.22
        mirrored.draw(web, in: CGRect(x: -small * 0.1, y: -small * 0.1, width: small, height: small))
    }
}

/// Cuerda de luces de colores que cuelga a lo ancho (solo en Noche de brujas).
struct HalloweenGarland: View {
    private let colors: [Color] = [
        Color(hex: 0xFFB347), Color(hex: 0xB27BFF), Color(hex: 0xFF7A59), Color(hex: 0xFFE08A), Color(hex: 0x7DE0A6)
    ]

    var body: some View {
        if Theme.halloween {
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    draw(context, size, t)
                }
            }
            .frame(height: 40)
            .allowsHitTesting(false)
        }
    }

    private func draw(_ context: GraphicsContext, _ size: CGSize, _ t: Double) {
        let sag = Double(size.height) * 0.5
        let p0 = CGPoint(x: -8, y: 2)
        let p1 = CGPoint(x: size.width + 8, y: 2)
        let ctrl = CGPoint(x: size.width / 2, y: 2 + sag * 2)
        var rope = Path()
        rope.move(to: p0)
        rope.addQuadCurve(to: p1, control: ctrl)
        context.stroke(rope, with: .color(Color(hex: 0x1A0E2E).opacity(0.8)), lineWidth: 3)
        let count = 12
        for i in 0..<count {
            let u = (Double(i) + 0.5) / Double(count)
            let a = (1 - u) * (1 - u)
            let b = 2 * (1 - u) * u
            let c = u * u
            let x = a * Double(p0.x) + b * Double(ctrl.x) + c * Double(p1.x)
            let y = a * Double(p0.y) + b * Double(ctrl.y) + c * Double(p1.y)
            let twinkle = 0.55 + 0.45 * sin(t * 6.2832 / 5.0 * 6.0 + Double(i) * 1.7)
            let color = colors[i % colors.count]
            let glow = Path(ellipseIn: CGRect(x: x - 13, y: y - 4, width: 26, height: 26))
            context.fill(glow, with: .color(color.opacity(0.35 * twinkle)))
            let bulb = Path(ellipseIn: CGRect(x: x - 5.5, y: y + 4, width: 11, height: 11))
            context.fill(bulb, with: .color(color.opacity(0.55 + 0.45 * twinkle)))
        }
    }
}

/// Confeti que cae al ganar un nivel.
struct ConfettiView: View {
    private let colors: [Color] = [
        Color(hex: 0xE8772E), Color(hex: 0x9B4FC9), Color(hex: 0xE0A93B), Color(hex: 0x6B9E86), Color(hex: 0xE07A5F), Color(hex: 0x4E8FA6)
    ]

    var body: some View {
        TimelineView(.animation) { timeline in
            Canvas { context, size in
                let t = timeline.date.timeIntervalSinceReferenceDate
                draw(context, size, t)
            }
        }
        .allowsHitTesting(false)
        .ignoresSafeArea()
    }

    private func draw(_ context: GraphicsContext, _ size: CGSize, _ t: Double) {
        for i in 0..<70 {
            let seed = Double(i)
            let speed = 0.16 + 0.12 * Double((i * 7) % 5) / 4.0
            let progress = (t * speed + seed * 0.137).truncatingRemainder(dividingBy: 1.0)
            let x = size.width * Double((i * 53) % 100) / 100.0 + 22.0 * sin(t * 2.0 + seed)
            let y = -20.0 + (size.height + 40.0) * progress
            var layer = context
            layer.translateBy(x: x, y: y)
            layer.rotate(by: .radians(t * 3.0 + seed))
            let rect = CGRect(x: -4, y: -7, width: 8, height: 14)
            layer.fill(Path(roundedRect: rect, cornerRadius: 2), with: .color(colors[i % colors.count]))
        }
    }
}

/// Portada breve al abrir la app: una escena con luna, estrellas y bichitos que flotan, y el título de siempre.
struct SplashView: View {
    @State private var shown = false
    @State private var glow = false

    var body: some View {
        ZStack {
            background
            SeasonBackdrop()
            if Theme.halloween {
                moon
                HStack(spacing: 40) {
                    FloatingSprite(name: "ico_bat", size: 46, tilt: -10, phase: 0.2)
                    FloatingSprite(name: "ico_bat", size: 34, tilt: 8, phase: 0.6)
                }
                .offset(x: 30, y: -250)
                HStack {
                    FloatingSprite(name: "ico_ghost", size: 54, tilt: -6, phase: 0.4)
                    Spacer()
                    FloatingSprite(name: "ico_spider", size: 40, tilt: 5, phase: 0.8)
                }
                .padding(.horizontal, 36)
                .offset(y: 190)
            } else {
                HStack {
                    FloatingSprite(name: "ico_sparkles", size: 44, tilt: -6, phase: 0.3)
                    Spacer()
                    FloatingSprite(name: "ico_star", size: 36, tilt: 8, phase: 0.7)
                }
                .padding(.horizontal, 40)
                .offset(y: -210)
            }
            VStack(spacing: 14) {
                SpriteImage(name: Theme.halloween ? "ico_pumpkin" : "ico_star", size: 120)
                    .scaleEffect(shown ? 1 : 0.4)
                    .shadow(color: Theme.halloween ? Color(hex: 0xFFA23A).opacity(glow ? 0.8 : 0.2) : Color.clear, radius: 24, x: 0, y: 0)
                Text("PARDOS")
                    .font(.system(size: 50, weight: .black, design: .rounded))
                    .kerning(8)
                    .foregroundColor(Theme.halloween ? Color(hex: 0xFFA23A) : Theme.ink)
                    .opacity(shown ? 1 : 0)
            }
        }
        .onAppear {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.5)) {
                shown = true
            }
            withAnimation(Animation.easeInOut(duration: 1.2).repeatForever(autoreverses: true)) {
                glow = true
            }
        }
    }

    @ViewBuilder
    private var background: some View {
        if Theme.halloween {
            LinearGradient(colors: [Color(hex: 0x120A26), Color(hex: 0x2A1650), Color(hex: 0x4A2468)], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        } else {
            LinearGradient(colors: [Theme.cream, Theme.paper], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        }
    }

    private var moon: some View {
        ZStack {
            Circle().fill(Color(hex: 0xFFF1B5).opacity(0.18)).frame(width: 150, height: 150)
            Circle().fill(Color(hex: 0xFFF1B5).opacity(0.35)).frame(width: 112, height: 112)
            Circle().fill(Color(hex: 0xFFF6D6)).frame(width: 84, height: 84)
        }
        .offset(x: 110, y: -240)
        .opacity(shown ? 1 : 0)
    }
}

/// Tarjeta que explica el nivel antes de empezar (la primera vez que sale cada tipo de regla, y en cada jefe).
struct RuleIntroView: View {
    let card: LevelCard
    let onStart: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.5).ignoresSafeArea()
            VStack(spacing: 12) {
                ZStack {
                    Circle().fill(kindColor(card.kind)).frame(width: 78, height: 78)
                    Image(systemName: kindSymbol(card.kind))
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.white)
                }
                Text(card.boss ? "JEFE" : "NUEVA REGLA")
                    .font(.system(size: 11, weight: .heavy))
                    .kerning(3)
                    .foregroundColor(kindColor(card.kind))
                Text(card.boss ? card.title : card.kindLabel)
                    .font(.system(size: 26, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                    .multilineTextAlignment(.center)
                Text(card.rule)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.65))
                    .multilineTextAlignment(.center)
                if let tip = card.tip, !tip.isEmpty {
                    Text(tip)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(Theme.ink.opacity(0.45))
                        .multilineTextAlignment(.center)
                }
                VStack(spacing: 4) {
                    Text("META")
                        .font(.system(size: 9, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.ink.opacity(0.4))
                    Text(card.goal)
                        .font(.system(size: 17, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                        .multilineTextAlignment(.center)
                    if !card.chips.isEmpty {
                        Text(card.chips.joined(separator: "  ·  "))
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(kindColor(card.kind))
                            .multilineTextAlignment(.center)
                    }
                    Text(card.threeStars)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.5))
                        .multilineTextAlignment(.center)
                }
                .padding(12)
                .frame(maxWidth: .infinity)
                .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(kindColor(card.kind).opacity(0.12)))
                Button(action: onStart) {
                    Text("¡A JUGAR!")
                        .font(.system(size: 17, weight: .black, design: .rounded))
                        .kerning(2)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(kindColor(card.kind)))
                }
            }
            .padding(22)
            .frame(maxWidth: 340)
            .background(RoundedRectangle(cornerRadius: 30, style: .continuous).fill(Theme.cream))
            .padding(24)
        }
    }
}
