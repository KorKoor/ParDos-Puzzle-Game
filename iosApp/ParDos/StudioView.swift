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
                Text("Studio")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                if let data = data {
                    if data.owned {
                        editor(data)
                    } else {
                        locked
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 30)
        }
        .background(Theme.cream.ignoresSafeArea())
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
                BigButton(title: "CONSEGUIR STUDIO · " + item.price) {
                    model.testBuyProduct("skin_studio")
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
            HStack(spacing: 6) {
                ForEach([2, 4, 8, 16, 32, 64], id: \.self) { v in
                    ZStack {
                        RoundedRectangle(cornerRadius: 8, style: .continuous).fill(style.fill(v))
                        Text("\(v)")
                            .font(.system(size: 14, weight: .black, design: .rounded))
                            .foregroundColor(style.text(v))
                    }
                    .frame(width: 44, height: 44)
                }
            }
        }
        .frame(height: 120)
        .clipped()
    }

    private func section<Content: View>(_ title: String, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(.system(size: 10, weight: .heavy))
                .kerning(2)
                .foregroundColor(Theme.ink.opacity(0.4))
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func chip(_ title: String, _ on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .heavy, design: .rounded))
                .foregroundColor(on ? .white : Theme.ink.opacity(0.6))
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(Capsule().fill(on ? Theme.accent : Color.white))
        }
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
        VStack(spacing: 4) {
            LinearGradient(
                colors: stride(from: 0, through: 360, by: 40).map { Color(hue: Double($0) / 360.0, saturation: 0.75, brightness: 0.95) },
                startPoint: .leading, endPoint: .trailing
            )
            .frame(height: 10)
            .clipShape(Capsule())
            Slider(value: value, in: 0...359, step: 1) { editing in
                if !editing { refreshPreview() }
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
