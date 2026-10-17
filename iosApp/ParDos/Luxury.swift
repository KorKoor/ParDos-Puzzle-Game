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

/// Altura del contenido de la partida: si cabe en pantalla no se desplaza (así deslizar el tablero nunca mueve la pantalla).
struct ContentHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}

/// Fondo de botón redondo: marfil con borde de luz y sombra suave.
struct LuxCircle: View {
    var body: some View {
        Circle()
            .fill(LinearGradient(colors: [Color.white, Theme.cream], startPoint: .top, endPoint: .bottom))
            .overlay(Circle().strokeBorder(LinearGradient(colors: [Color.white, Theme.ink.opacity(0.10)], startPoint: .top, endPoint: .bottom), lineWidth: 1))
            .shadow(color: Theme.ink.opacity(0.10), radius: 6, x: 0, y: 3)
    }
}

/// Fondo de píldora (etiquetas, chips) con el mismo acabado.
struct LuxCapsule: View {
    var body: some View {
        Capsule()
            .fill(LinearGradient(colors: [Color.white, Theme.cream], startPoint: .top, endPoint: .bottom))
            .overlay(Capsule().strokeBorder(LinearGradient(colors: [Color.white, Theme.ink.opacity(0.09)], startPoint: .top, endPoint: .bottom), lineWidth: 1))
            .shadow(color: Theme.ink.opacity(0.07), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Fondo del mapa

/// Mezcla de dos colores (t de 0 a 1).
func lerpColor(_ a: Color, _ b: Color, _ t: Double) -> Color {
    let x = ArtColor.from(a).resolve([:], [:])
    let y = ArtColor.from(b).resolve([:], [:])
    let k = max(0, min(1, t))
    return Color(.sRGB, red: x.r + (y.r - x.r) * k, green: x.g + (y.g - x.g) * k, blue: x.b + (y.b - x.b) * k, opacity: 1)
}

/// Número "al azar" repetible: el mismo nivel y el mismo índice dan siempre lo mismo (así el fondo no salta al recargar).
func mapNoise(_ i: Int, _ level: Int) -> Double {
    let v = sin(Double(i * 127 + level * 311) * 12.9898) * 43758.5453
    return v - floor(v)
}

/// Fondo de cada tramo del mapa: el cielo se va convirtiendo en suelo a lo largo del capítulo (sin cortes entre filas),
/// con bordes más oscuros y partículas propias de cada mundo (pétalos, luciérnagas, nieve, arena, estrellas, luces).
struct MapBackdropRow: View {
    let level: Int
    let theme: ChapterTheme

    var body: some View {
        let pos = Double((level - 1) % Chapters.size)
        let n = Double(Chapters.size)
        let sky: Color = theme.night ? theme.color.shade(-0.5) : theme.color.shade(0.34)
        let ground: Color = theme.night ? theme.color.shade(-0.2) : theme.color.shade(-0.12)
        let top: Color = lerpColor(sky, ground, pos / n)
        let bottom: Color = lerpColor(sky, ground, (pos + 1) / n)
        return ZStack {
            LinearGradient(colors: [top, bottom], startPoint: .top, endPoint: .bottom)
            LinearGradient(colors: [Color.black.opacity(0.14), Color.clear, Color.clear, Color.black.opacity(0.14)], startPoint: .leading, endPoint: .trailing)
            MapAmbience(level: level, scenery: theme.scenery, night: theme.night)
        }
    }
}

/// Partículas suaves de cada mundo. Quietas con "Reducir movimiento" o con el ahorro de energía.
struct MapAmbience: View {
    let level: Int
    let scenery: String
    let night: Bool

    var body: some View {
        if UIAccessibility.isReduceMotionEnabled || !PowerMonitor.shared.heavyEffects {
            Canvas { ctx, size in
                var c = ctx
                MapAmbience.draw(&c, size, 0, level, scenery, night)
            }
            .allowsHitTesting(false)
        } else {
            TimelineView(.animation(minimumInterval: 1.0 / 12.0)) { timeline in
                let t = timeline.date.timeIntervalSinceReferenceDate
                Canvas { ctx, size in
                    var c = ctx
                    MapAmbience.draw(&c, size, t, level, scenery, night)
                }
            }
            .allowsHitTesting(false)
        }
    }

    static func draw(_ ctx: inout GraphicsContext, _ size: CGSize, _ t: Double, _ level: Int, _ scenery: String, _ night: Bool) {
        let w = size.width
        let h = size.height
        // bruma que cruza despacio
        let mistX = CGFloat(((t * 6 + mapNoise(1, level) * Double(w)).truncatingRemainder(dividingBy: Double(w) * 1.6))) - w * 0.3
        let mist = Path(ellipseIn: CGRect(x: mistX - w * 0.35, y: h * 0.55, width: w * 0.7, height: h * 0.45))
        ctx.fill(mist, with: .color(Color.white.opacity(night ? 0.05 : 0.10)))

        switch scenery {
        case "cherry":
            for i in 0..<9 {
                let speed = 9.0 + mapNoise(i, level + 3) * 11.0
                let y = CGFloat((mapNoise(i, level + 5) * Double(h) + t * speed).truncatingRemainder(dividingBy: Double(h) + 24)) - 12
                let x = CGFloat(mapNoise(i, level + 7)) * w + CGFloat(sin(t * 0.8 + Double(i))) * 14
                var c = ctx
                c.translateBy(x: x, y: y)
                c.rotate(by: .radians(t * 0.9 + Double(i)))
                c.fill(Path(ellipseIn: CGRect(x: -4.5, y: -2.5, width: 9, height: 5)), with: .color(Color(hex: 0xFFC2D4).opacity(0.85)))
            }
        case "pines", "bamboo", "lavender", "hills":
            for i in 0..<9 {
                let bx = CGFloat(mapNoise(i, level + 11)) * w
                let by = CGFloat(mapNoise(i, level + 13)) * h
                let x = bx + CGFloat(sin(t * 0.4 + Double(i) * 2.0)) * 18
                let y = by + CGFloat(cos(t * 0.35 + Double(i) * 1.3)) * 12
                let glow = 0.25 + 0.75 * max(0, sin(t * 1.4 + Double(i) * 1.7))
                let r: CGFloat = 11
                ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)),
                         with: .radialGradient(Gradient(colors: [Color(hex: 0xFFF3A8).opacity(0.5 * glow), .clear]), center: CGPoint(x: x, y: y), startRadius: 0, endRadius: r))
                ctx.fill(Path(ellipseIn: CGRect(x: x - 1.8, y: y - 1.8, width: 3.6, height: 3.6)), with: .color(Color(hex: 0xFFFBD0).opacity(0.4 + 0.6 * glow)))
            }
        case "mountains", "clouds":
            for i in 0..<12 {
                let speed = 12.0 + mapNoise(i, level + 3) * 14.0
                let y = CGFloat((mapNoise(i, level + 5) * Double(h) + t * speed).truncatingRemainder(dividingBy: Double(h) + 10)) - 5
                let x = CGFloat(mapNoise(i, level + 7)) * w + CGFloat(sin(t * 0.6 + Double(i))) * 8
                let r = CGFloat(1.2 + mapNoise(i, level + 9) * 2.2)
                ctx.fill(Path(ellipseIn: CGRect(x: x, y: y, width: r * 2, height: r * 2)), with: .color(Color.white.opacity(0.8)))
            }
        case "dunes", "gold_dunes", "river":
            for i in 0..<10 {
                let speed = 10.0 + mapNoise(i, level + 3) * 16.0
                let x = CGFloat((mapNoise(i, level + 5) * Double(w) + t * speed).truncatingRemainder(dividingBy: Double(w) + 20)) - 10
                let y = CGFloat(mapNoise(i, level + 7)) * h + CGFloat(sin(t * 0.9 + Double(i))) * 5
                let a = 0.25 + 0.5 * max(0, sin(t * 1.6 + Double(i) * 2.1))
                ctx.fill(Path(ellipseIn: CGRect(x: x, y: y, width: 3, height: 3)), with: .color(Color(hex: 0xFFE9A8).opacity(a)))
            }
        case "moon", "lanterns":
            for i in 0..<10 {
                let x = CGFloat(mapNoise(i, level + 11)) * w
                let y = CGFloat(mapNoise(i, level + 13)) * h
                let a = 0.25 + 0.75 * max(0, sin(t * 1.1 + Double(i) * 1.9))
                let r = CGFloat(2.0 + mapNoise(i, level + 17) * 3.0)
                var p = Path()
                p.move(to: CGPoint(x: x - r, y: y)); p.addLine(to: CGPoint(x: x + r, y: y))
                p.move(to: CGPoint(x: x, y: y - r)); p.addLine(to: CGPoint(x: x, y: y + r))
                ctx.stroke(p, with: .color(Color.white.opacity(a)), style: StrokeStyle(lineWidth: 1.2, lineCap: .round))
            }
        default:
            for i in 0..<7 {
                let bx = CGFloat(mapNoise(i, level + 11)) * w
                let by = CGFloat(mapNoise(i, level + 13)) * h
                let x = bx + CGFloat(sin(t * 0.25 + Double(i))) * 16
                let y = by + CGFloat(cos(t * 0.2 + Double(i) * 1.4)) * 10
                let r = CGFloat(8 + mapNoise(i, level + 17) * 16)
                ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)),
                         with: .radialGradient(Gradient(colors: [Color.white.opacity(0.22), .clear]), center: CGPoint(x: x, y: y), startRadius: 0, endRadius: r))
            }
        }
    }
}

/// Resplandor que respira alrededor de un elemento importante (botón de jugar, cosas listas para cobrar).
struct PulsingGlow: ViewModifier {
    let color: Color
    let radius: CGFloat
    let strength: Double

    func body(content: Content) -> some View {
        if UIAccessibility.isReduceMotionEnabled || ProcessInfo.processInfo.isLowPowerModeEnabled {
            content.shadow(color: color.opacity(0.35 * strength), radius: radius, x: 0, y: 5)
        } else {
            TimelineView(.animation(minimumInterval: 1.0 / 20.0)) { timeline in
                let p = 0.5 + 0.5 * sin(timeline.date.timeIntervalSinceReferenceDate * 2.2)
                content.shadow(color: color.opacity((0.26 + 0.32 * p) * strength), radius: radius * CGFloat(0.85 + 0.3 * p), x: 0, y: 5)
            }
        }
    }
}

extension View {
    func pulsingGlow(_ color: Color, radius: CGFloat = 16, strength: Double = 1) -> some View {
        modifier(PulsingGlow(color: color, radius: radius, strength: strength))
    }
}

/// Icono de lujo: cuadrado redondeado con degradado, brillo de cristal arriba y el símbolo en blanco (como los de Ajustes de iOS, pero con más luz).
struct LuxIcon: View {
    let symbol: String
    let tint: Color
    let size: CGFloat

    init(_ symbol: String, tint: Color = Theme.accent, size: CGFloat = 32) {
        self.symbol = symbol
        self.tint = tint
        self.size = size
    }

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
        return ZStack {
            shape.fill(LinearGradient(colors: [tint.shade(0.22), tint, tint.shade(-0.14)], startPoint: .top, endPoint: .bottom))
            shape.fill(LinearGradient(colors: [Color.white.opacity(0.42), Color.white.opacity(0)], startPoint: .top, endPoint: .center))
            shape.strokeBorder(Color.white.opacity(0.4), lineWidth: 0.8)
            Image(systemName: symbol)
                .font(.system(size: size * 0.5, weight: .bold))
                .foregroundColor(.white)
                .shadow(color: Color.black.opacity(0.22), radius: 1, y: 1)
        }
        .frame(width: size, height: size)
        .shadow(color: tint.opacity(0.4), radius: 5, x: 0, y: 3)
    }
}

/// Número que "rueda" hasta su nuevo valor en un instante (puntos y movimientos de la partida).
struct RollingNumber: View {
    let value: Int
    @State private var shown: Int = 0
    @State private var started = false

    var body: some View {
        Text("\(shown)")
            .monospacedDigit()
            .onAppear {
                shown = value
                started = true
            }
            .onChange(of: value) { newValue in
                if UIAccessibility.isReduceMotionEnabled || abs(newValue - shown) > 4000 { shown = newValue; return }
                let from = shown
                let steps = 8
                for i in 1...steps {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.05 * Double(i)) {
                        let p = Double(i) / Double(steps)
                        shown = from + Int(Double(newValue - from) * (1 - pow(1 - p, 2)))
                        if i == steps { shown = newValue }
                    }
                }
            }
    }
}

/// Barra de ímpetu: se llena con las jugadas seguidas que fusionan y cambia de color; siempre ocupa su sitio para que nada se mueva.
struct FlowBar: View {
    let streak: Int

    var body: some View {
        let fraction = min(1.0, Double(streak) / 20.0)
        let on = streak >= 2
        return HStack(spacing: 8) {
            Image(systemName: "flame.fill")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(LinearGradient(colors: [Color(hex: 0xFFE27A), Color(hex: 0xFF7A1F)], startPoint: .top, endPoint: .bottom))
                .shadow(color: Color(hex: 0xFF7A1F).opacity(0.3 + 0.6 * fraction), radius: 4 + 6 * CGFloat(fraction))
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.10))
                    LuxFill(LinearGradient(colors: [Theme.accent, Theme.gold, Color(hex: 0xFF7A1F)], startPoint: .leading, endPoint: .trailing), width: max(8, geo.size.width * CGFloat(fraction)))
                }
            }
            .frame(height: 8)
            Text("\(streak)")
                .font(.system(size: 13, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink.opacity(0.7))
                .monospacedDigit()
                .frame(minWidth: 22, alignment: .trailing)
        }
        .padding(.horizontal, 20)
        .frame(height: 20)
        .opacity(on ? 1 : 0)
        .animation(.easeOut(duration: 0.25), value: on)
    }
}
