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
