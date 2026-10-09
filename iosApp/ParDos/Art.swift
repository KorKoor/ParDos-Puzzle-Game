import SwiftUI
import UIKit

// Dibujos vectoriales de ParDos (avatares, piezas del álbum, mapa, adornos...). Se dibujan en Python con iosApp/tools/art,
// se guardan como JSON en la carpeta Art (archivos art_*.json) y aquí se leen y se pintan con Canvas. Así no hay emojis en ninguna pantalla.

// MARK: - Colores

/// Color de un dibujo: fijo, una ranura de la paleta (para cambiar colores sin redibujar), una mezcla o una opacidad.
indirect enum ArtColor {
    case rgba(Double, Double, Double, Double)
    case slot(String)
    case mix(ArtColor, ArtColor, Double)
    case alpha(ArtColor, Double)

    static func hex(_ value: UInt32, alpha: Double = 1) -> ArtColor {
        return .rgba(Double((value >> 16) & 0xFF) / 255.0, Double((value >> 8) & 0xFF) / 255.0, Double(value & 0xFF) / 255.0, alpha)
    }

    func resolve(_ palette: ArtPalette, _ fallback: ArtPalette, _ depth: Int = 0) -> ArtRGBA {
        if depth > 8 { return ArtRGBA(r: 1, g: 0, b: 1, a: 1) }
        switch self {
        case .rgba(let r, let g, let b, let a):
            return ArtRGBA(r: r, g: g, b: b, a: a)
        case .slot(let name):
            if let c = palette[name] { return c.resolve(palette, fallback, depth + 1) }
            if let c = fallback[name] { return c.resolve(palette, fallback, depth + 1) }
            return ArtRGBA(r: 1, g: 0, b: 1, a: 1)
        case .mix(let first, let second, let t):
            let x = first.resolve(palette, fallback, depth + 1)
            let y = second.resolve(palette, fallback, depth + 1)
            return ArtRGBA(r: x.r + (y.r - x.r) * t, g: x.g + (y.g - x.g) * t, b: x.b + (y.b - x.b) * t, a: x.a + (y.a - x.a) * t)
        case .alpha(let base, let factor):
            let x = base.resolve(palette, fallback, depth + 1)
            return ArtRGBA(r: x.r, g: x.g, b: x.b, a: x.a * factor)
        }
    }
}

extension ArtColor {
    /// Convierte un Color de SwiftUI en color de dibujo (para teñir partículas con el color de una skin).
    static func from(_ color: Color) -> ArtColor {
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        UIColor(color).getRed(&r, green: &g, blue: &b, alpha: &a)
        return ArtColor.rgba(Double(r), Double(g), Double(b), Double(a))
    }
}

struct ArtRGBA {
    var r: Double
    var g: Double
    var b: Double
    var a: Double

    var color: Color {
        return Color(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

/// Colores que sustituyen a las ranuras de un dibujo (por ejemplo "head", "dark" en los avatares).
typealias ArtPalette = [String: ArtColor]

// MARK: - Pinturas y formas

struct ArtStop {
    let offset: Double
    let color: ArtColor
}

enum ArtPaint {
    case solid(ArtColor)
    case linear(CGPoint, CGPoint, [ArtStop])
    case radial(CGPoint, Double, [ArtStop])
}

struct ArtShape {
    let path: Path
    let fill: ArtPaint?
    let stroke: ArtPaint?
    let lineWidth: CGFloat
    let cap: CGLineCap
    let join: CGLineJoin
    let opacity: Double
    let evenOdd: Bool
    /// 0 siempre, 1 solo con los ojos abiertos, 2 solo al parpadear.
    let tag: Int
}

// MARK: - Lectura del JSON

private func artNumbers(_ any: Any?) -> [Double] {
    guard let list = any as? [Any] else { return [] }
    var out: [Double] = []
    out.reserveCapacity(list.count)
    for item in list {
        if let n = item as? NSNumber { out.append(n.doubleValue) }
    }
    return out
}

func artParseColor(_ any: Any?) -> ArtColor? {
    if let text = any as? String {
        if text.hasPrefix("$") {
            return ArtColor.slot(String(text.dropFirst()))
        }
        var digits = text
        if digits.hasPrefix("#") { digits.removeFirst() }
        guard digits.count == 6 || digits.count == 8, let value = UInt64(digits, radix: 16) else { return nil }
        if digits.count == 6 {
            return ArtColor.hex(UInt32(value), alpha: 1)
        }
        let rgb = UInt32((value >> 8) & 0xFFFFFF)
        let a = Double(value & 0xFF) / 255.0
        return ArtColor.hex(rgb, alpha: a)
    }
    if let list = any as? [Any], list.count >= 3, let kind = list[0] as? String {
        if kind == "m", list.count >= 4,
           let first = artParseColor(list[1]), let second = artParseColor(list[2]),
           let t = (list[3] as? NSNumber)?.doubleValue {
            return ArtColor.mix(first, second, t)
        }
        if kind == "a", let base = artParseColor(list[1]), let factor = (list[2] as? NSNumber)?.doubleValue {
            return ArtColor.alpha(base, factor)
        }
    }
    return nil
}

private func artParsePaint(_ any: Any?) -> ArtPaint? {
    if let dict = any as? [String: Any] {
        guard let kind = dict["t"] as? String, let rawStops = dict["st"] as? [Any] else { return nil }
        let p = artNumbers(dict["p"])
        var stops: [ArtStop] = []
        for raw in rawStops {
            if let pair = raw as? [Any], pair.count == 2,
               let offset = (pair[0] as? NSNumber)?.doubleValue, let color = artParseColor(pair[1]) {
                stops.append(ArtStop(offset: offset, color: color))
            }
        }
        if stops.isEmpty { return nil }
        if kind == "l" && p.count >= 4 {
            return ArtPaint.linear(CGPoint(x: p[0], y: p[1]), CGPoint(x: p[2], y: p[3]), stops)
        }
        if kind == "r" && p.count >= 3 {
            return ArtPaint.radial(CGPoint(x: p[0], y: p[1]), p[2], stops)
        }
        return nil
    }
    if let color = artParseColor(any) { return ArtPaint.solid(color) }
    return nil
}

private func artBuildPath(_ n: [Double]) -> Path {
    var path = Path()
    var i = 0
    while i < n.count {
        let code = Int(n[i])
        if code == 0 && i + 2 < n.count {
            path.move(to: CGPoint(x: n[i + 1], y: n[i + 2]))
            i += 3
        } else if code == 1 && i + 2 < n.count {
            path.addLine(to: CGPoint(x: n[i + 1], y: n[i + 2]))
            i += 3
        } else if code == 2 && i + 6 < n.count {
            path.addCurve(to: CGPoint(x: n[i + 5], y: n[i + 6]),
                          control1: CGPoint(x: n[i + 1], y: n[i + 2]),
                          control2: CGPoint(x: n[i + 3], y: n[i + 4]))
            i += 7
        } else if code == 3 {
            path.closeSubpath()
            i += 1
        } else {
            break
        }
    }
    return path
}

private func artParseShape(_ json: [String: Any]) -> ArtShape? {
    let nums = artNumbers(json["p"])
    if nums.isEmpty { return nil }
    let fill = artParsePaint(json["f"])
    let stroke = artParsePaint(json["k"])
    if fill == nil && stroke == nil { return nil }
    let capCode = (json["c"] as? NSNumber)?.intValue ?? 0
    let joinCode = (json["j"] as? NSNumber)?.intValue ?? 0
    let cap: CGLineCap = capCode == 1 ? .round : (capCode == 2 ? .square : .butt)
    let join: CGLineJoin = joinCode == 1 ? .round : (joinCode == 2 ? .bevel : .miter)
    return ArtShape(
        path: artBuildPath(nums),
        fill: fill,
        stroke: stroke,
        lineWidth: CGFloat((json["w"] as? NSNumber)?.doubleValue ?? 0),
        cap: cap,
        join: join,
        opacity: (json["o"] as? NSNumber)?.doubleValue ?? 1,
        evenOdd: ((json["e"] as? NSNumber)?.intValue ?? 0) == 1,
        tag: (json["g"] as? NSNumber)?.intValue ?? 0
    )
}

// MARK: - Dibujo

final class ArtIcon {
    let width: CGFloat
    let height: CGFloat
    let palette: ArtPalette
    let shapes: [ArtShape]

    init?(json: [String: Any]) {
        guard let w = (json["w"] as? NSNumber)?.doubleValue, let h = (json["h"] as? NSNumber)?.doubleValue, w > 0, h > 0 else { return nil }
        var pal: ArtPalette = [:]
        if let raw = json["pal"] as? [String: Any] {
            for (name, value) in raw {
                if let color = artParseColor(value) { pal[name] = color }
            }
        }
        var list: [ArtShape] = []
        if let raw = json["s"] as? [[String: Any]] {
            for item in raw {
                if let shape = artParseShape(item) { list.append(shape) }
            }
        }
        self.width = CGFloat(w)
        self.height = CGFloat(h)
        self.palette = pal
        self.shapes = list
    }

    private func shading(_ paint: ArtPaint, _ pal: ArtPalette, _ transform: CGAffineTransform, _ scale: CGFloat) -> GraphicsContext.Shading {
        switch paint {
        case .solid(let color):
            return GraphicsContext.Shading.color(color.resolve(pal, palette).color)
        case .linear(let from, let to, let stops):
            return GraphicsContext.Shading.linearGradient(
                gradient(stops, pal),
                startPoint: from.applying(transform),
                endPoint: to.applying(transform)
            )
        case .radial(let center, let radius, let stops):
            return GraphicsContext.Shading.radialGradient(
                gradient(stops, pal),
                center: center.applying(transform),
                startRadius: 0,
                endRadius: CGFloat(radius) * scale
            )
        }
    }

    private func gradient(_ stops: [ArtStop], _ pal: ArtPalette) -> Gradient {
        var list: [Gradient.Stop] = []
        for stop in stops {
            list.append(Gradient.Stop(color: stop.color.resolve(pal, palette).color, location: CGFloat(stop.offset)))
        }
        return Gradient(stops: list)
    }

    /// Dibuja el dibujo dentro de `rect`. `palette` cambia colores de las ranuras; `blink` cierra los ojos.
    /// `fit`: .fit lo ajusta sin deformar y centrado; .fillBottom llena el ancho y lo apoya abajo (banners, el sobrante se recorta fuera).
    func draw(_ context: GraphicsContext, in rect: CGRect, palette pal: ArtPalette = [:], blink: Bool = false, fit: ArtFit = .fit, opacity: Double = 1, rotation: Double = 0) {
        if rect.width <= 0 || rect.height <= 0 { return }
        var scale = min(rect.width / width, rect.height / height)
        var originX = rect.minX + (rect.width - width * scale) / 2
        var originY = rect.minY + (rect.height - height * scale) / 2
        if fit == .fillBottom {
            scale = max(rect.width / width, rect.height / height)
            originX = rect.minX + (rect.width - width * scale) / 2
            originY = rect.maxY - height * scale
        }
        var transform = CGAffineTransform(a: scale, b: 0, c: 0, d: scale, tx: originX, ty: originY)
        if rotation != 0 {
            let pivot = CGPoint(x: rect.midX, y: rect.midY)
            transform = transform
                .concatenating(CGAffineTransform(translationX: -pivot.x, y: -pivot.y))
                .concatenating(CGAffineTransform(rotationAngle: CGFloat(rotation)))
                .concatenating(CGAffineTransform(translationX: pivot.x, y: pivot.y))
        }
        let base = context.opacity * opacity
        for shape in shapes {
            if shape.tag == 1 && blink { continue }
            if shape.tag == 2 && !blink { continue }
            let placed = shape.path.applying(transform)
            var layer = context
            layer.opacity = base * shape.opacity
            if let fill = shape.fill {
                layer.fill(placed, with: shading(fill, pal, transform, scale), style: FillStyle(eoFill: shape.evenOdd))
            }
            if let stroke = shape.stroke, shape.lineWidth > 0 {
                let style = StrokeStyle(lineWidth: shape.lineWidth * scale, lineCap: shape.cap, lineJoin: shape.join)
                layer.stroke(placed, with: shading(stroke, pal, transform, scale), style: style)
            }
        }
    }
}

enum ArtFit {
    case fit
    case fillBottom
}

// MARK: - Biblioteca

/// Todos los dibujos de los archivos art_*.json. Se leen una sola vez (mejor en segundo plano con `preload()`).
final class ArtLibrary {
    static let shared = ArtLibrary()

    private let lock = NSLock()
    private var loaded = false
    private var raw: [String: [String: Any]] = [:]
    private var palettes: [String: ArtPalette] = [:]
    private var cache: [String: ArtIcon] = [:]
    private var missingIds: Set<String> = []
    private var files: [String] = []
    private var brokenFiles: [String] = []

    func preload() {
        DispatchQueue.global(qos: .userInitiated).async {
            _ = self.icon("__warm__")
        }
    }

    func icon(_ id: String) -> ArtIcon? {
        lock.lock()
        defer { lock.unlock() }
        loadFilesLocked()
        if let hit = cache[id] { return hit }
        guard let json = raw[id] else {
            missingIds.insert(id)
            return nil
        }
        guard let built = ArtIcon(json: json) else {
            missingIds.insert(id)
            return nil
        }
        cache[id] = built
        return built
    }

    func has(_ id: String) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        loadFilesLocked()
        return raw[id] != nil
    }

    /// Atajo para dibujar un dibujo por nombre dentro de un Canvas (no hace nada si no existe).
    func draw(_ id: String, _ context: GraphicsContext, in rect: CGRect, palette: ArtPalette = [:], blink: Bool = false, fit: ArtFit = .fit, opacity: Double = 1, rotation: Double = 0) {
        if let icon = icon(id) {
            icon.draw(context, in: rect, palette: palette, blink: blink, fit: fit, opacity: opacity, rotation: rotation)
        }
    }

    /// Paleta guardada en el JSON con ese nombre (por ejemplo "avatar.FOX.GOLD").
    func palette(_ name: String) -> ArtPalette? {
        lock.lock()
        defer { lock.unlock() }
        loadFilesLocked()
        return palettes[name]
    }

    /// Resumen para Ajustes → Diagnóstico.
    func report() -> String {
        lock.lock()
        defer { lock.unlock() }
        loadFilesLocked()
        var text = "Dibujos: \(raw.count) en \(files.count) archivos"
        if !brokenFiles.isEmpty { text += " · ilegibles: " + brokenFiles.joined(separator: ", ") }
        if !missingIds.isEmpty { text += " · faltan: " + missingIds.sorted().prefix(8).joined(separator: ", ") }
        return text
    }

    private func loadFilesLocked() {
        if loaded { return }
        loaded = true
        let urls = Bundle.main.urls(forResourcesWithExtension: "json", subdirectory: nil) ?? []
        for url in urls {
            let name = url.lastPathComponent
            if !name.hasPrefix("art_") { continue }
            guard let data = try? Data(contentsOf: url),
                  let object = try? JSONSerialization.jsonObject(with: data, options: []),
                  let dict = object as? [String: Any] else {
                brokenFiles.append(name)
                continue
            }
            files.append(name)
            for (key, value) in dict {
                guard let entry = value as? [String: Any] else { continue }
                if key.hasPrefix("@pal:") {
                    var pal: ArtPalette = [:]
                    for (slot, color) in entry {
                        if let parsed = artParseColor(color) { pal[slot] = parsed }
                    }
                    palettes[String(key.dropFirst(5))] = pal
                } else {
                    raw[key] = entry
                }
            }
        }
    }
}

// MARK: - Vista

/// Un dibujo por su nombre, ajustado al espacio disponible.
struct ArtView: View {
    let id: String
    var palette: ArtPalette = [:]
    var blink: Bool = false

    var body: some View {
        let icon = ArtLibrary.shared.icon(id)
        return Canvas { context, size in
            guard let icon = icon else { return }
            icon.draw(context, in: CGRect(origin: CGPoint.zero, size: size), palette: palette, blink: blink)
        }
        .accessibilityHidden(true)
    }
}
