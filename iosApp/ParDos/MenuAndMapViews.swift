import SwiftUI
import UIKit

// MARK: - Mapa de niveles

struct MapView: View {
    @EnvironmentObject var model: AppModel

    private var visibleCount: Int {
        min(model.levelCount, model.unlocked + 30)
    }

    private var chapterCount: Int {
        (model.unlocked - 1) / Chapters.size + 1
    }

    var body: some View {
        ScrollViewReader { proxy in
            VStack(spacing: 0) {
                topBar(proxy)
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ModesSection()
                            .padding(.bottom, 6)
                        ForEach(1...visibleCount, id: \.self) { level in
                            VStack(spacing: 0) {
                                if (level - 1) % Chapters.size == 0 {
                                    ChapterBanner(chapter: Chapters.index(of: level))
                                }
                                LevelRow(level: level)
                            }
                            .id(level)
                        }
                    }
                    .padding(.vertical, 12)
                }
                .onAppear {
                    proxy.scrollTo(model.unlocked, anchor: .center)
                }
            }
        }
    }

    private func topBar(_ proxy: ScrollViewProxy) -> some View {
        VStack(spacing: 10) {
            HStack {
                Text("Campaña")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                HStack(spacing: 4) {
                    Image(systemName: "star.fill").foregroundColor(Theme.gold)
                    Text("\(model.totalStars)")
                        .font(.system(size: 15, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.ink)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Capsule().fill(Color.white))
            }
            HStack(spacing: 8) {
                CurrencyBar()
                Spacer()
                Button(action: { withAnimation { proxy.scrollTo(model.unlocked, anchor: .center) } }) {
                    HStack(spacing: 4) {
                        Image(systemName: "location.fill")
                        Text("Mi nivel")
                    }
                    .font(.system(size: 12, weight: .heavy, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(Theme.accent))
                }
                Menu {
                    ForEach(0..<chapterCount, id: \.self) { chapter in
                        Button("Capítulo \(chapter + 1) · \(Chapters.theme(chapter).name)") {
                            withAnimation { proxy.scrollTo(chapter * Chapters.size + 1, anchor: .top) }
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "list.bullet")
                        Text("Capítulos")
                    }
                    .font(.system(size: 12, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(Color.white))
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 26)
        .padding(.bottom, 6)
    }
}

struct ChapterBanner: View {
    @EnvironmentObject var model: AppModel
    let chapter: Int

    /// El cofre que se abre al superar el último nivel del capítulo.
    private var chestBadge: some View {
        let claimed = model.chapterChestClaimed(chapter)
        return VStack(spacing: 2) {
            ZStack(alignment: .bottomTrailing) {
                ChestIcon(type: chapter >= 3 ? "RARE" : "COMMON", size: 40).opacity(claimed ? 0.6 : 1)
                if claimed {
                    Image(systemName: "checkmark.circle.fill").font(.system(size: 16)).foregroundColor(.white)
                }
            }
            Text(claimed ? "COBRADO" : "AL FINAL")
                .font(.system(size: 8, weight: .heavy))
                .kerning(1)
                .foregroundColor(Color.white.opacity(0.85))
        }
    }

    var body: some View {
        let theme = Chapters.theme(chapter)
        let stars = model.chapterStars(chapter)
        return HStack(spacing: 12) {
            ArtView(id: theme.emblem)
                .frame(width: 60, height: 60)
                .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 3)
            VStack(alignment: .leading, spacing: 4) {
                Text("CAPÍTULO \(chapter + 1)")
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(3)
                    .foregroundColor(Color.white.opacity(0.75))
                Text(loc(theme.name))
                    .font(.system(size: 20, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
                HStack(spacing: 4) {
                    Image(systemName: "star.fill").font(.system(size: 11))
                    Text("\(stars)/\(Chapters.size * 3)")
                        .font(.system(size: 12, weight: .heavy))
                }
                .foregroundColor(Color.white.opacity(0.85))
            }
            Spacer(minLength: 0)
            chestBadge
        }
        .padding(16)
        .background(
            ZStack(alignment: .bottomTrailing) {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(LinearGradient(colors: [theme.color, theme.color.opacity(0.75)], startPoint: .topLeading, endPoint: .bottomTrailing))
                ArtView(id: "prop.\(theme.scenery)_b")
                    .frame(width: 104, height: 104)
                    .opacity(0.92)
                    .offset(x: -56, y: 8)
                    .allowsHitTesting(false)
            }
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}

/// Camino que une cada nivel con el siguiente: una cinta suave con una línea punteada encima.
struct MapRoad: View {
    let level: Int
    let tint: Color
    let locked: Bool

    private func sway(_ n: Int) -> CGFloat {
        return n <= 0 ? 0 : CGFloat(sin(Double(n) * 0.85)) * 86
    }

    var body: some View {
        let rowHeight: CGFloat = 92
        let nodeY: CGFloat = 40
        let prev = sway(level - 1)
        let here = sway(level)
        let next = sway(level + 1)
        return Canvas { context, size in
            let cx = size.width / 2
            let p0 = CGPoint(x: cx + prev, y: nodeY - rowHeight)
            let p1 = CGPoint(x: cx + here, y: nodeY)
            let p2 = CGPoint(x: cx + next, y: nodeY + rowHeight)
            var path = Path()
            let start = CGPoint(x: (p0.x + p1.x) / 2, y: (p0.y + p1.y) / 2)
            let end = CGPoint(x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2)
            path.move(to: start)
            path.addQuadCurve(to: end, control: p1)
            let ribbon = tint.opacity(locked ? 0.10 : 0.22)
            context.stroke(path, with: .color(ribbon), style: StrokeStyle(lineWidth: 16, lineCap: .butt, lineJoin: .round))
            context.stroke(path, with: .color(Color.white.opacity(locked ? 0.35 : 0.75)), style: StrokeStyle(lineWidth: 2.6, lineCap: .butt, lineJoin: .round, dash: [2, 7]))
        }
        .allowsHitTesting(false)
    }
}

struct LevelRow: View {
    @EnvironmentObject var model: AppModel
    let level: Int
    @State private var pulse = false

    var body: some View {
        let locked = level > model.unlocked
        let current = level == model.unlocked
        let card = model.card(level)
        let kind = card?.kind ?? "ZEN"
        let boss = card?.boss ?? false
        let stars = model.stars(level)
        let sway = CGFloat(sin(Double(level) * 0.85)) * 86
        let theme = Chapters.theme(Chapters.index(of: level))
        let tint = kind == "ZEN" ? theme.color : kindColor(kind)
        let propId = Chapters.prop(level: level, theme: theme)
        let propSide: CGFloat = sway >= 0 ? -1 : 1
        let propX = propSide * (126 + CGFloat(level % 3) * 14)
        return Button(action: { if !locked { model.openPreview(level) } }) {
            VStack(spacing: 4) {
                ZStack {
                    if current {
                        Circle()
                            .stroke(tint.opacity(0.5), lineWidth: 3)
                            .frame(width: 76, height: 76)
                            .scaleEffect(pulse ? 1.18 : 0.92)
                            .opacity(pulse ? 0.1 : 0.9)
                    }
                    node(locked: locked, tint: tint, boss: boss)
                    if locked {
                        Image(systemName: "lock.fill")
                            .font(.system(size: 20))
                            .foregroundColor(Theme.ink.opacity(0.3))
                    } else {
                        Text("\(level)")
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }
                    if !locked && kind != "ZEN" {
                        Image(systemName: kindSymbol(kind))
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                            .padding(5)
                            .background(Circle().fill(kindColor(kind)).overlay(Circle().stroke(Color.white, lineWidth: 2)))
                            .offset(x: 24, y: -22)
                    }
                }
                starRow(stars: stars, show: !locked && stars > 0)
            }
            .offset(x: sway)
            .frame(height: 92)
            .frame(maxWidth: .infinity)
            .background(
                ZStack {
                    theme.color.opacity(0.07)
                    MapRoad(level: level, tint: tint, locked: locked)
                }
            )
            .overlay(
                Group {
                    if let propId = propId {
                        ArtView(id: propId)
                            .frame(width: 58, height: 58)
                            .offset(x: propX, y: 12)
                            .opacity(locked ? 0.55 : 1)
                    }
                }
                .allowsHitTesting(false)
            )
        }
        .buttonStyle(PlainButtonStyle())
        .onAppear {
            if current {
                withAnimation(Animation.easeInOut(duration: 1.3).repeatForever(autoreverses: false)) {
                    pulse = true
                }
            }
        }
    }

    @ViewBuilder
    private func node(locked: Bool, tint: Color, boss: Bool) -> some View {
        let size: CGFloat = boss ? 64 : 56
        let shape = RoundedRectangle(cornerRadius: boss ? 20 : size / 2, style: .continuous)
        if locked {
            shape.fill(Color(hex: 0xD8D3E0)).frame(width: size, height: size)
        } else {
            shape
                .fill(LinearGradient(colors: [tint.opacity(0.85), tint], startPoint: .top, endPoint: .bottom))
                .frame(width: size, height: size)
                .shadow(color: tint.opacity(0.5), radius: 0, x: 0, y: 4)
        }
    }

    @ViewBuilder
    private func starRow(stars: Int, show: Bool) -> some View {
        HStack(spacing: 2) {
            ForEach(0..<3, id: \.self) { i in
                Image(systemName: "star.fill")
                    .font(.system(size: 11))
                    .foregroundColor(i < stars ? Theme.gold : Theme.ink.opacity(0.12))
            }
        }
        .opacity(show ? 1 : 0)
    }
}

// MARK: - Vista previa del nivel

struct LevelPreviewSheet: View {
    @EnvironmentObject var model: AppModel
    let card: LevelCard

    var body: some View {
        VStack(spacing: 14) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            ZStack {
                Circle().fill(kindColor(card.kind)).frame(width: 64, height: 64)
                Image(systemName: kindSymbol(card.kind))
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)
            }
            Text("Nivel \(card.id)")
                .font(.system(size: 26, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text(card.boss ? card.title.uppercased() : card.kindLabel.uppercased())
                .font(.system(size: 12, weight: .heavy))
                .kerning(3)
                .foregroundColor(kindColor(card.kind))
            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { i in
                    Image(systemName: "star.fill")
                        .font(.system(size: 22))
                        .foregroundColor(i < model.stars(card.id) ? Theme.gold : Theme.ink.opacity(0.12))
                }
            }
            Text(loc(card.rule))
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.65))
                .multilineTextAlignment(.center)
            goalBox
            if model.best(card.id) > 0 {
                Text("Tu mejor: \(model.best(card.id)) movimientos")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
            Button(action: { model.start(card.id) }) {
                Text(model.stars(card.id) > 0 ? "JUGAR DE NUEVO" : "JUGAR")
                    .font(.system(size: 18, weight: .black, design: .rounded))
                    .kerning(2)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(kindColor(card.kind)))
            }
            Spacer(minLength: 4)
        }
        .padding(.horizontal, 24)
        .background(Theme.cream.ignoresSafeArea())
    }

    private var goalBox: some View {
        VStack(spacing: 6) {
            Text("META")
                .font(.system(size: 10, weight: .heavy))
                .kerning(2)
                .foregroundColor(Theme.ink.opacity(0.4))
            Text(card.goal)
                .font(.system(size: 18, weight: .black, design: .rounded))
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
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(kindColor(card.kind).opacity(0.12)))
    }
}

// MARK: - Ajustes

struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @AppStorage("sound_on") private var soundOn = true
    @AppStorage("music_on") private var musicOn = true
    @AppStorage("haptics_on") private var hapticsOn = true
    @AppStorage("notif_on") private var notifOn = true
    @AppStorage("auto_next") private var autoNext = true
    @AppStorage("dpad_on") private var dpadOn = false
    @State private var iconPref = AppIconManager.preference
    @State private var confirmReset = false
    @State private var sfxLevel = Double(SoundManager.shared.sfxVolume)
    @State private var musicLevel = Double(SoundManager.shared.musicVolume)
    @State private var powerMode = PowerMonitor.shared.mode
    @ObservedObject private var power = PowerMonitor.shared

    var body: some View {
        VStack(spacing: 14) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            Text("Ajustes")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
            VStack(spacing: 0) {
                toggleRow("speaker.wave.2.fill", "Sonidos", $soundOn)
                Divider()
                toggleRow("music.note", "Música", $musicOn)
                Divider()
                toggleRow("hand.tap.fill", "Vibración", $hapticsOn)
                Divider()
                toggleRow("bell.fill", "Avisos", $notifOn)
                Divider()
                toggleRow("forward.fill", "Pasar solo al siguiente nivel", $autoNext)
                Divider()
                toggleRow("gamecontroller.fill", "Botones de dirección", $dpadOn)
            }
            .padding(.horizontal, 16)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
            volumeCard
            powerCard
            iconCard
            VStack(spacing: 6) {
                Text("TU PROGRESO")
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(2)
                    .foregroundColor(Theme.ink.opacity(0.4))
                Text("\(model.levelsWon) niveles superados · \(model.totalStars) estrellas")
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text("Racha: \(model.streak) \(model.streak == 1 ? "día" : "días")")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.55))
            }
            .padding(14)
            .frame(maxWidth: .infinity)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
            VStack(spacing: 0) {
                Button(action: { model.switchSheet(.backup) }) {
                    HStack(spacing: 12) {
                        Image(systemName: "externaldrive.fill").frame(width: 24).foregroundColor(Theme.accent)
                        Text("Copia de seguridad").font(.system(size: 16, weight: .bold, design: .rounded)).foregroundColor(Theme.ink)
                        Spacer()
                        Image(systemName: "chevron.right").font(.system(size: 12, weight: .bold)).foregroundColor(Theme.ink.opacity(0.3))
                    }
                    .padding(.vertical, 12)
                }
                Divider()
                linkRow("camera.fill", "Síguenos en Instagram", "https://www.instagram.com/kourkoour/")
                Divider()
                linkRow("cup.and.saucer.fill", "Invítame un café (Ko-fi)", "https://ko-fi.com/korkor0209")
            }
            .padding(.horizontal, 16)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
            Text(ArtLibrary.shared.report())
                .font(.system(size: 9, weight: .semibold, design: .monospaced))
                .foregroundColor(Theme.ink.opacity(0.35))
            if !decodeDiagnostics.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("DIAGNÓSTICO")
                        .font(.system(size: 10, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Color(hex: 0xB4413C))
                    Text(decodeDiagnostics.joined(separator: "\n"))
                        .font(.system(size: 9, design: .monospaced))
                        .foregroundColor(Theme.ink.opacity(0.7))
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Color.white))
            }
            Button(action: { confirmReset = true }) {
                Text("Borrar mi progreso")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Color(hex: 0xB4413C))
            }
            }
            }
        }
        .padding(.horizontal, 22)
        .background(Theme.cream.ignoresSafeArea())
        .onChange(of: musicOn) { _ in
            SoundManager.shared.settingsChanged()
            model.syncMusic()
        }
        .onChange(of: soundOn) { value in
            if value { SoundManager.shared.play(.toggle_on) }
        }
        .onChange(of: notifOn) { value in model.setNotifications(value) }
        .alert(isPresented: $confirmReset) {
            Alert(
                title: Text("¿Borrar el progreso?"),
                message: Text("Se pierden las estrellas, los niveles desbloqueados y la racha de este iPhone."),
                primaryButton: .destructive(Text("Borrar")) { model.resetProgress() },
                secondaryButton: .cancel(Text("Cancelar"))
            )
        }
    }

    private var volumeCard: some View {
        VStack(spacing: 4) {
            sliderRow("speaker.wave.2.fill", "Volumen de sonidos", $sfxLevel) { value in
                SoundManager.shared.sfxVolume = Float(value)
            } onEnd: {
                SoundManager.shared.play(.coin)
            }
            Divider()
            sliderRow("music.note", "Volumen de música", $musicLevel) { value in
                SoundManager.shared.musicVolume = Float(value)
            } onEnd: {
                SoundManager.shared.settingsChanged()
            }
        }
        .padding(.horizontal, 16)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
    }

    private var iconCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Image(systemName: "app.badge.fill").frame(width: 24).foregroundColor(Theme.accent)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Icono de la app")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text("Automático: el de Noche de brujas en octubre y el clásico el resto del año.")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.55))
                }
            }
            Picker("", selection: $iconPref) {
                Text("Automático").tag("auto")
                Text("Clásico").tag("classic")
                Text("Halloween").tag("halloween")
            }
            .pickerStyle(SegmentedPickerStyle())
            .onChange(of: iconPref) { value in
                AppIconManager.setPreference(value)
            }
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
    }

    private var powerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Image(systemName: "battery.100").frame(width: 24).foregroundColor(Theme.accent)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Ahorro de energía")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text("Ahora: \(power.label). Con ahorro hay menos animaciones y el teléfono se calienta menos.")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.55))
                }
            }
            Picker("", selection: $powerMode) {
                Text("Automático").tag(0)
                Text("Siempre").tag(1)
                Text("Nunca").tag(2)
            }
            .pickerStyle(SegmentedPickerStyle())
            .onChange(of: powerMode) { value in
                PowerMonitor.shared.setMode(value)
            }
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
    }

    private func sliderRow(_ symbol: String, _ title: String, _ value: Binding<Double>, onChange: @escaping (Double) -> Void, onEnd: @escaping () -> Void) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol).frame(width: 24).foregroundColor(Theme.accent)
            VStack(alignment: .leading, spacing: 2) {
                Text(loc(title))
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink)
                Slider(value: value, in: 0...1, onEditingChanged: { editing in
                    onChange(value.wrappedValue)
                    if !editing { onEnd() }
                })
                .accentColor(Theme.accent)
            }
        }
        .padding(.vertical, 10)
        .onChange(of: value.wrappedValue) { newValue in
            onChange(newValue)
        }
    }

    private func linkRow(_ symbol: String, _ title: String, _ url: String) -> some View {
        Button(action: {
            if let target = URL(string: url) { UIApplication.shared.open(target) }
        }) {
            HStack(spacing: 12) {
                Image(systemName: symbol).frame(width: 24).foregroundColor(Theme.accent)
                Text(loc(title)).font(.system(size: 16, weight: .bold, design: .rounded)).foregroundColor(Theme.ink)
                Spacer()
                Image(systemName: "arrow.up.right").font(.system(size: 12, weight: .bold)).foregroundColor(Theme.ink.opacity(0.3))
            }
            .padding(.vertical, 12)
        }
    }

    private func toggleRow(_ symbol: String, _ title: String, _ value: Binding<Bool>) -> some View {
        Toggle(isOn: value) {
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .frame(width: 24)
                    .foregroundColor(Theme.accent)
                Text(loc(title))
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
        }
        .padding(.vertical, 12)
    }
}
