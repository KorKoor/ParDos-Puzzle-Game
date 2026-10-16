import SwiftUI

// Pulido de lujo: destello dorado que cruza las tarjetas importantes (se apaga con "Reducir movimiento" y con el ahorro de energía).

struct ShimmerModifier: ViewModifier {
    let radius: CGFloat
    let period: Double

    func body(content: Content) -> some View {
        if UIAccessibility.isReduceMotionEnabled || ProcessInfo.processInfo.isLowPowerModeEnabled {
            content
        } else {
            content.overlay(
                TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
                    GeometryReader { geo in
                        let t = timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: period) / period
                        // el destello cruza en el primer 40 % del ciclo y descansa el resto
                        let p = min(1.0, t / 0.4)
                        LinearGradient(
                            colors: [Color.white.opacity(0), Color.white.opacity(0.26), Color.white.opacity(0)],
                            startPoint: .leading, endPoint: .trailing
                        )
                        .frame(width: geo.size.width * 0.4)
                        .rotationEffect(.degrees(14))
                        .offset(x: CGFloat(p) * geo.size.width * 1.7 - geo.size.width * 0.5)
                        .opacity(t < 0.4 ? 1 : 0)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
                    .allowsHitTesting(false)
                }
            )
        }
    }
}

extension View {
    func shimmer(radius: CGFloat = 20, period: Double = 3.6) -> some View {
        modifier(ShimmerModifier(radius: radius, period: period))
    }
}

/// Fondo vivo del menú: manchas de color muy suaves que se desplazan despacio (se queda quieto con "Reducir movimiento" o ahorro de energía).
struct LivingBackdrop: View {
    var body: some View {
        if UIAccessibility.isReduceMotionEnabled || ProcessInfo.processInfo.isLowPowerModeEnabled {
            Color.clear.allowsHitTesting(false)
        } else {
            TimelineView(.animation(minimumInterval: 1.0 / 15.0)) { timeline in
                let t = timeline.date.timeIntervalSinceReferenceDate
                Canvas { ctx, size in
                    let blobs: [(Color, Double, Double, Double)] = [
                        (Theme.accent, 0.0, 0.18, 0.55),
                        (Theme.gold, 2.1, 0.13, 0.45),
                        (Theme.energy, 4.2, 0.11, 0.5)
                    ]
                    for (color, phase, alpha, scale) in blobs {
                        let x = size.width * (0.5 + 0.38 * sin(t * 0.07 + phase))
                        let y = size.height * (0.4 + 0.30 * cos(t * 0.05 + phase * 1.3))
                        let r = size.width * scale
                        ctx.fill(
                            Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)),
                            with: .radialGradient(Gradient(colors: [color.opacity(alpha), .clear]), center: CGPoint(x: x, y: y), startRadius: 0, endRadius: r)
                        )
                    }
                }
            }
            .allowsHitTesting(false)
            .ignoresSafeArea()
        }
    }
}

/// Todos los botones "planos" de la app se hunden un poco al tocarlos (esta definición tapa a la de SwiftUI en todo el proyecto).
struct PlainButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .opacity(configuration.isPressed ? 0.93 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

/// Onda de luz al formar una ficha grande (256 o más): un anillo que se abre y un destello breve.
struct BigMergeFlash: View {
    let value: Int
    let size: CGFloat
    @State private var t: CGFloat = 0

    private var tint: Color { value >= 1024 ? Color(hex: 0xFFE27A) : (value >= 512 ? Theme.gold : Theme.accent) }

    var body: some View {
        ZStack {
            Circle()
                .stroke(tint.opacity(Double(1 - t) * 0.9), lineWidth: 5 * (1 - t) + 1)
                .frame(width: size * (0.35 + 0.9 * t), height: size * (0.35 + 0.9 * t))
            Circle()
                .fill(RadialGradient(colors: [tint.opacity(Double(1 - t) * 0.45), .clear], center: .center, startRadius: 0, endRadius: size * 0.5))
                .frame(width: size, height: size)
        }
        .onAppear {
            if UIAccessibility.isReduceMotionEnabled { t = 1; return }
            withAnimation(.easeOut(duration: 0.7)) { t = 1 }
        }
    }
}

/// Al vencer a un jefe: su emblema entra con rebote y aparece "JEFE VENCIDO" en letras doradas.
struct BossDefeatedBanner: View {
    let motif: String
    @State private var shown = false

    var body: some View {
        VStack(spacing: 8) {
            BossEmblemView(motif: motif, size: 88)
                .scaleEffect(shown ? 1 : 0.3)
                .rotationEffect(.degrees(shown ? 0 : -25))
            Text(loc("JEFE VENCIDO"))
                .font(.system(size: 13, weight: .black, design: .serif))
                .kerning(5)
                .foregroundColor(Color(hex: 0xB07A1E))
                .opacity(shown ? 1 : 0)
                .offset(y: shown ? 0 : 10)
        }
        .onAppear {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.55).delay(0.15)) { shown = true }
            Haptics.success()
        }
    }
}

/// Número que sube hasta su valor (monedas del premio): cuenta en menos de un segundo y vibra al llegar.
struct CountUpText: View {
    let value: Int
    var prefix: String = ""
    @State private var shown = 0

    var body: some View {
        Text("\(prefix)\(shown)")
            .monospacedDigit()
            .onAppear {
                shown = 0
                if UIAccessibility.isReduceMotionEnabled || value <= 0 { shown = value; return }
                let steps = 24
                for i in 1...steps {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5 + 0.9 * Double(i) / Double(steps)) {
                        let p = Double(i) / Double(steps)
                        shown = Int(Double(value) * (1 - pow(1 - p, 3)))
                        if i == steps { shown = value; Haptics.tap() }
                    }
                }
            }
    }
}

/// Rayos de luz que giran despacio detrás de un objeto importante (cofres, jefes).
struct LightRays: View {
    let color: Color

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 24.0)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            Canvas { ctx, size in
                let c = CGPoint(x: size.width / 2, y: size.height / 2)
                let r = size.width / 2
                for i in 0..<12 {
                    let a = t * 0.25 + Double(i) / 12.0 * .pi * 2
                    var p = Path()
                    p.move(to: c)
                    p.addArc(center: c, radius: r, startAngle: .radians(a - 0.09), endAngle: .radians(a + 0.09), clockwise: false)
                    p.closeSubpath()
                    ctx.fill(p, with: .radialGradient(Gradient(colors: [color.opacity(0.45), .clear]), center: c, startRadius: 0, endRadius: r))
                }
            }
        }
        .allowsHitTesting(false)
    }
}

/// Relleno de barra de progreso: crece con muelle cuando cambia el valor y lleva un brillo de cristal arriba.
struct LuxFill<S: ShapeStyle>: View {
    let style: S
    let width: CGFloat

    init(_ style: S, width: CGFloat) {
        self.style = style
        self.width = width
    }

    var body: some View {
        Capsule().fill(style)
            .overlay(
                Capsule().fill(LinearGradient(colors: [Color.white.opacity(0.38), Color.white.opacity(0)], startPoint: .top, endPoint: .center))
            )
            .frame(width: width)
            .animation(.spring(response: 0.6, dampingFraction: 0.8), value: width)
    }
}

/// Fondo de las hojas: crema con un degradado suave y dos manchas de luz muy lentas (quieto con "Reducir movimiento").
struct LuxSheetBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Theme.cream, Theme.paper], startPoint: .top, endPoint: .bottom)
            LivingBackdrop()
        }
        .ignoresSafeArea()
    }
}

/// Título de hoja: icono con degradado, nombre grande y un filete dorado que se desvanece.
struct SheetTitle: View {
    let text: String
    let symbol: String

    init(_ text: String, symbol: String) {
        self.text = text
        self.symbol = symbol
    }

    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .font(.system(size: 15, weight: .black))
                    .foregroundColor(.white)
                    .frame(width: 34, height: 34)
                    .background(Circle().fill(LinearGradient(colors: [Theme.accent, Theme.accentDark], startPoint: .top, endPoint: .bottom)))
                    .shadow(color: Theme.accent.opacity(0.4), radius: 6, y: 3)
                Text(loc(text))
                    .font(.system(size: 26, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
            Rectangle()
                .fill(LinearGradient(colors: [.clear, Theme.gold.opacity(0.7), .clear], startPoint: .leading, endPoint: .trailing))
                .frame(height: 1)
                .padding(.horizontal, 40)
        }
        .padding(.bottom, 2)
    }
}

/// Aparece con un empujón hacia arriba y un fundido, con retraso para que las tarjetas de una hoja entren una tras otra.
struct RiseModifier: ViewModifier {
    let delay: Double
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : 18)
            .onAppear {
                if UIAccessibility.isReduceMotionEnabled { shown = true; return }
                withAnimation(.spring(response: 0.55, dampingFraction: 0.82).delay(delay)) { shown = true }
            }
    }
}

extension View {
    func rise(_ delay: Double = 0) -> some View { modifier(RiseModifier(delay: delay)) }
}

/// Entrada del contenido de cada hoja (la hoja misma entra deslizándose, como siempre en iOS).
struct SheetEntrance: ViewModifier {
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .scaleEffect(shown ? 1 : 0.97, anchor: .top)
            .onAppear {
                if UIAccessibility.isReduceMotionEnabled { shown = true; return }
                withAnimation(.easeOut(duration: 0.32)) { shown = true }
            }
    }
}
