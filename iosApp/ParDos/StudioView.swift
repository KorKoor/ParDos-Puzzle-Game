import SwiftUI

struct StudioConfigInfo: Decodable {
    let finish: String
    let hue: Int
    let hue2: Int
    let tone: String
    let background: String
    let particles: String
}

struct StudioPresetInfo: Decodable, Identifiable {
    let name: String
    let finish: String
    let hue: Int
    let hue2: Int
    let tone: String
    let background: String
    let particles: String

    var id: String { name }
}

struct StudioStateData: Decodable {
    let owned: Bool
    let config: StudioConfigInfo
    let presets: [StudioPresetInfo]
    let finishes: [String]
    let particleKinds: [String]
}

private func finishName(_ id: String) -> String {
    switch id {
    case "JELLY": return "Gelatina"
    case "FLAT": return "Papel"
    case "GLASS": return "Cristal"
    case "WOOD": return "Madera"
    case "NEON": return "Neón"
    case "PORCELAIN": return "Porcelana"
    case "METAL": return "Metal"
    default: return id
    }
}

private func particleName(_ id: String) -> String {
    switch id {
    case "NONE": return "Ninguna"
    case "PETALS": return "Pétalos"
    case "SNOW": return "Nieve"
    case "LEAVES": return "Hojas"
    case "FIREFLIES": return "Luciérnagas"
    case "BUBBLES": return "Burbujas"
    case "STARS": return "Estrellas"
    case "EMBERS": return "Brasas"
    case "SPRINKLES": return "Chispitas"
    case "RAIN": return "Lluvia"
    case "SAND": return "Arena"
    case "HEARTS": return "Corazones"
    case "BATS": return "Murciélagos"
    case "MARIGOLD": return "Cempasúchil"
    case "FLOWERS": return "Flores"
    case "CONFETTI": return "Confeti"
    case "FLAG_CONFETTI": return "Banderines"
    case "FIREWORKS": return "Fuegos"
    case "SNOWFLAKE": return "Copos"
    case "SPARKLES": return "Destellos"
    case "METEORS": return "Meteoros"
    default: return id
    }
}

/// Studio: el editor de tu propia skin (acabado, dos colores, fondo, partículas) con vista previa al instante.
struct StudioSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var data: StudioStateData?
    @State private var finish = "JELLY"
    @State private var hue: Double = 200
    @State private var hue2: Double = 285
    @State private var tone = "VIVID"
    @State private var background = "TINTED"
    @State private var particles = "STARS"
    @State private var preview: SkinItem?

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                SheetTitle("Studio", symbol: "paintpalette.fill")
                if let data = data {
                    if data.owned {
                        editor(data).rise(0.05)
                    } else {
                        locked.rise(0.05)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 30)
        }
        .background(LuxSheetBackground())
        .onAppear { load() }
    }

    private var locked: some View {
        VStack(spacing: 12) {
            Image(systemName: "paintpalette.fill").font(.system(size: 44)).foregroundColor(Theme.energy)
            Text("Diseña tus propias fichas: acabado, colores, fondo y partículas.")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.6))
                .multilineTextAlignment(.center)
            if let item = model.store?.specials.first(where: { $0.id == "skin_studio" }) {
                BigButton(title: "CONSEGUIR STUDIO · " + model.priceLabel(item.id, item.price)) {
                    model.buyProduct("skin_studio")
                    load()
                }
            }
        }
        .padding(18)
        .card()
    }

    private func editor(_ data: StudioStateData) -> some View {
        VStack(spacing: 14) {
            previewCard
            HStack(spacing: 10) {
                actionPill("dice.fill", "Sorpréndeme") { randomize(data) }
                actionPill("arrow.counterclockwise", "Volver a lo guardado") { load() }
            }
            section("Puntos de partida") {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(data.presets) { p in
                            chip(p.name, false) { apply(p) }
                        }
                    }
                }
            }
            section("Acabado") {
                chips(data.finishes.map { ($0, finishName($0)) }, selected: finish) { finish = $0; refreshPreview() }
            }
            section("Color de las fichas pequeñas") {
                hueSlider($hue)
            }
            section("Color de las fichas grandes") {
                hueSlider($hue2)
            }
            section("Carácter del color") {
                chips([("PASTEL", "Pastel"), ("VIVID", "Vivo"), ("DEEP", "Profundo")], selected: tone) { tone = $0; refreshPreview() }
            }
            section("Fondo") {
                chips([("LIGHT", "Claro"), ("TINTED", "Teñido"), ("DARK", "Oscuro")], selected: background) { background = $0; refreshPreview() }
            }
            section("Partículas") {
                chips(data.particleKinds.map { ($0, particleName($0)) }, selected: particles) { particles = $0; refreshPreview() }
            }
            BigButton(title: "GUARDAR Y USAR") { save() }
        }
    }

    private var previewCard: some View {
        let style = BoardStyle(skin: preview)
        return ZStack {
            if style.changesTheme {
                RoundedRectangle(cornerRadius: 24, style: .continuous).fill(style.background)
            } else {
                RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Theme.background)
            }
            ParticlesView(kind: style.particles, tint: style.particleTint)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            boardPreview
        }
        .frame(height: 262)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 28, style: .continuous).strokeBorder(LinearGradient(colors: [Color.white.opacity(0.9), Color(hex: 0xE9B949).opacity(0.5)], startPoint: .top, endPoint: .bottom), lineWidth: 1.5))
        .shadow(color: Theme.ink.opacity(0.18), radius: 16, x: 0, y: 10)
    }

    /// Un tablero de verdad con todas las fichas, hasta la 4096, para ver cómo queda el degradado completo.
    private var boardPreview: some View {
        let style = BoardStyle(skin: preview)
        let values = [2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 2, 4, 8, 16]
        return VStack(spacing: 6) {
            ForEach(0..<4, id: \.self) { r in
                HStack(spacing: 6) {
                    ForEach(0..<4, id: \.self) { c in
                        TileView(tile: TileSnap(id: "p\(r)\(c)", v: values[r * 4 + c], r: r, c: c, new: false, merged: false), size: 54, style: style)
                    }
                }
            }
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill((style.ink ?? Theme.ink).opacity(0.12)))
    }

    private func actionPill(_ symbol: String, _ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: symbol)
                Text(loc(title))
            }
            .font(.system(size: 13, weight: .heavy, design: .rounded))
            .foregroundColor(Theme.ink)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .card(radius: 16)
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func randomize(_ d: StudioStateData) {
        finish = d.finishes.randomElement() ?? finish
        let first = Double(Int.random(in: 0...359))
        hue = first
        hue2 = (first + Double(Int.random(in: 70...200))).truncatingRemainder(dividingBy: 360)
        tone = ["PASTEL", "VIVID", "DEEP"].randomElement() ?? tone
        background = ["LIGHT", "TINTED", "DARK"].randomElement() ?? background
        particles = d.particleKinds.randomElement() ?? particles
        Haptics.tap()
        refreshPreview()
    }

    private func section<Content: View>(_ title: String, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(loc(title).uppercased())
                .font(.system(size: 10, weight: .heavy))
                .kerning(2)
                .foregroundColor(Theme.ink.opacity(0.45))
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .card(radius: 20)
    }

    private func chip(_ title: String, _ on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(loc(title))
                .font(.system(size: 12, weight: .heavy, design: .rounded))
                .foregroundColor(on ? .white : Theme.ink.opacity(0.6))
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(
                    Group {
                        if on {
                            Capsule().fill(LinearGradient(colors: [Theme.accent.shade(0.16), Theme.accent], startPoint: .top, endPoint: .bottom))
                                .shadow(color: Theme.accent.opacity(0.45), radius: 7, y: 3)
                        } else {
                            LuxCapsule()
                        }
                    }
                )
        }
        .buttonStyle(ToyPressStyle())
    }

    private func chips(_ items: [(String, String)], selected: String, pick: @escaping (String) -> Void) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(0..<items.count, id: \.self) { i in
                    chip(items[i].1, items[i].0 == selected) { pick(items[i].0) }
                }
            }
        }
    }

    private func hueSlider(_ value: Binding<Double>) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color(hue: value.wrappedValue / 360.0, saturation: 0.75, brightness: 0.95))
                .frame(width: 34, height: 34)
                .overlay(Circle().strokeBorder(Color.white, lineWidth: 2))
                .shadow(color: Color(hue: value.wrappedValue / 360.0, saturation: 0.75, brightness: 0.95).opacity(0.6), radius: 8, y: 3)
            VStack(spacing: 4) {
                LinearGradient(
                    colors: stride(from: 0, through: 360, by: 30).map { Color(hue: Double($0) / 360.0, saturation: 0.75, brightness: 0.95) },
                    startPoint: .leading, endPoint: .trailing
                )
                .frame(height: 10)
                .clipShape(Capsule())
                Slider(value: value, in: 0...359, step: 1) { editing in
                    if !editing { refreshPreview() }
                }
            }
        }
    }

    // MARK: Acciones

    private func load() {
        data = model.loadStudio()
        if let d = data {
            finish = d.config.finish
            hue = Double(d.config.hue)
            hue2 = Double(d.config.hue2)
            tone = d.config.tone
            background = d.config.background
            particles = d.config.particles
            refreshPreview()
        }
    }

    private func apply(_ p: StudioPresetInfo) {
        finish = p.finish
        hue = Double(p.hue)
        hue2 = Double(p.hue2)
        tone = p.tone
        background = p.background
        particles = p.particles
        refreshPreview()
    }

    private func refreshPreview() {
        preview = model.studioPreview(finish: finish, hue: Int(hue), hue2: Int(hue2), tone: tone, background: background, particles: particles)
    }

    private func save() {
        model.saveStudio(finish: finish, hue: Int(hue), hue2: Int(hue2), tone: tone, background: background, particles: particles)
    }
}

extension AppModel {
    func loadStudio() -> StudioStateData? {
        return decodeJSON(StudioStateData.self, meta.studioState())
    }

    func studioPreview(finish: String, hue: Int, hue2: Int, tone: String, background: String, particles: String) -> SkinItem? {
        let json = meta.studioPreview(finish: finish, hue: Int32(hue), hue2: Int32(hue2), tone: tone, background: background, particles: particles)
        return decodeJSON(SkinItem.self, json)
    }

    func saveStudio(finish: String, hue: Int, hue2: Int, tone: String, background: String, particles: String) {
        let ok = run { $0.saveStudio(finish: finish, hue: Int32(hue), hue2: Int32(hue2), tone: tone, background: background, particles: particles) }
        if ok {
            reloadSkinCatalog()
            equipSkin("studio")
            showToast("¡Tu skin Studio está lista!")
        }
    }

    func reloadSkinCatalog() {
        skins = decodeJSON([SkinItem].self, meta.skinCatalog()) ?? skins
        for skin in skins { skinByID[skin.id] = skin }
        objectWillChange.send()
    }
}
