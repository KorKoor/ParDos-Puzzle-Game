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
