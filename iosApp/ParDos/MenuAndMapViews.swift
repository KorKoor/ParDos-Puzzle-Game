import SwiftUI

// MARK: - Menú

struct MenuView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 16) {
                topBar
                Spacer(minLength: 0)
                header
                Spacer(minLength: 0)
                playButton
                dailyCard
                mapButton
                Spacer(minLength: 0)
                Text("Versión de prueba para iPhone · la lógica del juego es la misma que en Android")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.4))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                    .padding(.bottom, 10)
            }
            .padding(.horizontal, 22)
            HalloweenGarland()
        }
    }

    private var topBar: some View {
        HStack(spacing: 10) {
            statChip(symbol: "star.fill", color: Theme.gold, text: "\(model.totalStars)")
            statChip(symbol: "flame.fill", color: Theme.energy, text: "\(model.streak) \(model.streak == 1 ? "día" : "días")")
            Spacer()
            Button(action: { model.showSettings = true }) {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(Theme.ink)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(Color.white))
            }
        }
        .padding(.top, 6)
    }

    private func statChip(symbol: String, color: Color, text: String) -> some View {
        HStack(spacing: 5) {
            Image(systemName: symbol).foregroundColor(color)
            Text(text)
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Capsule().fill(Color.white))
    }

    private var header: some View {
        VStack(spacing: 10) {
            Text("PARDOS")
                .font(.system(size: 54, weight: .black, design: .rounded))
                .kerning(8)
                .foregroundColor(Theme.ink)
            Text(Theme.halloween ? "SUMA… SI TE ATREVES" : "SUMA Y RELÁJATE")
                .font(.system(size: 12, weight: .heavy))
                .kerning(5)
                .foregroundColor(Theme.ink.opacity(0.45))
        }
    }

    private var playButton: some View {
        Button(action: { model.start(model.unlocked) }) {
            HStack(spacing: 14) {
                Image(systemName: "play.fill")
                    .font(.system(size: 26, weight: .bold))
                VStack(alignment: .leading, spacing: 2) {
                    Text("JUGAR")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .kerning(3)
                    Text(Theme.halloween ? "Nivel \(model.unlocked) · ¡BUU!" : "Nivel \(model.unlocked)")
                        .font(.system(size: 13, weight: .bold))
                        .opacity(0.85)
                }
                Spacer()
                menuSprites
            }
            .foregroundColor(.white)
            .padding(.horizontal, 24)
            .frame(height: 100)
            .background(
                RoundedRectangle(cornerRadius: 30, style: .continuous)
                    .fill(LinearGradient(colors: [Theme.accent, Theme.accentDark], startPoint: .top, endPoint: .bottom))
            )
            .shadow(color: Theme.accentDark.opacity(0.5), radius: 0, x: 0, y: 6)
        }
    }

    @ViewBuilder
    private var menuSprites: some View {
        if Theme.halloween {
            HStack(spacing: -6) {
                FloatingSprite(name: "ico_ghost", size: 40, tilt: -8, phase: 0.2)
                FloatingSprite(name: "ico_pumpkin", size: 50, tilt: 6, phase: 0.5)
            }
        } else {
            FloatingSprite(name: "ico_star", size: 46, tilt: 8, phase: 0.3)
        }
    }

    private var dailyCard: some View {
        Button(action: { model.startDaily() }) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(kindColor(model.dailyCard?.kind ?? "ZEN"))
                        .frame(width: 46, height: 46)
                    Image(systemName: kindSymbol(model.dailyCard?.kind ?? "ZEN"))
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("RETO DIARIO")
                        .font(.system(size: 10, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.gold)
                    Text(model.dailyCard?.kindLabel ?? "Reto")
                        .font(.system(size: 17, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                    Text(model.dailyCard?.goal ?? "")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Color.white.opacity(0.7))
                        .lineLimit(1)
                }
                Spacer()
                if model.dailyDone {
                    HStack(spacing: 2) {
                        ForEach(0..<3, id: \.self) { i in
                            Image(systemName: "star.fill")
                                .font(.system(size: 13))
                                .foregroundColor(i < model.dailyStars ? Theme.gold : Color.white.opacity(0.2))
                        }
                    }
                } else {
                    Image(systemName: "chevron.right")
                        .foregroundColor(Color.white.opacity(0.6))
                }
            }
            .padding(.horizontal, 16)
            .frame(height: 74)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Theme.ink))
            .shadow(color: Theme.ink.opacity(0.25), radius: 0, x: 0, y: 4)
        }
    }

    private var mapButton: some View {
        Button(action: { model.screen = .map }) {
            HStack {
                Image(systemName: "map.fill")
                Text("Mapa de niveles")
                    .font(.system(size: 16, weight: .heavy, design: .rounded))
            }
            .foregroundColor(Theme.ink)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Color.white))
            .shadow(color: Theme.ink.opacity(0.12), radius: 0, x: 0, y: 4)
        }
    }
}

// MARK: - Mapa de niveles

struct MapView: View {
    @EnvironmentObject var model: AppModel

    private var visibleCount: Int {
        min(model.levelCount, model.unlocked + 30)
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
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

    private var topBar: some View {
        HStack {
            Button(action: { model.backToMenu() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Theme.ink)
                    .frame(width: 44, height: 44)
                    .background(Circle().fill(Color.white))
            }
            Text("Campaña")
                .font(.system(size: 22, weight: .black, design: .rounded))
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
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}

struct ChapterBanner: View {
    @EnvironmentObject var model: AppModel
    let chapter: Int

    var body: some View {
        let theme = Chapters.theme(chapter)
        let stars = model.chapterStars(chapter)
        return VStack(alignment: .leading, spacing: 4) {
            Text("CAPÍTULO \(chapter + 1)")
                .font(.system(size: 10, weight: .heavy))
                .kerning(3)
                .foregroundColor(Color.white.opacity(0.75))
            Text(theme.name)
                .font(.system(size: 22, weight: .black, design: .rounded))
                .foregroundColor(.white)
            HStack(spacing: 4) {
                Image(systemName: "star.fill").font(.system(size: 11))
                Text("\(stars)/\(Chapters.size * 3)")
                    .font(.system(size: 12, weight: .heavy))
            }
            .foregroundColor(Color.white.opacity(0.85))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(LinearGradient(colors: [theme.color, theme.color.opacity(0.75)], startPoint: .topLeading, endPoint: .bottomTrailing))
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
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
            Text(card.rule)
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
    @State private var confirmReset = false

    var body: some View {
        VStack(spacing: 14) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            Text("Ajustes")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            VStack(spacing: 0) {
                toggleRow("speaker.wave.2.fill", "Sonidos", $soundOn)
                Divider()
                toggleRow("music.note", "Música", $musicOn)
                Divider()
                toggleRow("hand.tap.fill", "Vibración", $hapticsOn)
            }
            .padding(.horizontal, 16)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
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
            Button(action: { confirmReset = true }) {
                Text("Borrar mi progreso")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Color(hex: 0xB4413C))
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 22)
        .background(Theme.cream.ignoresSafeArea())
        .onChange(of: musicOn) { _ in model.syncMusic() }
        .alert(isPresented: $confirmReset) {
            Alert(
                title: Text("¿Borrar el progreso?"),
                message: Text("Se pierden las estrellas, los niveles desbloqueados y la racha de este iPhone."),
                primaryButton: .destructive(Text("Borrar")) { model.resetProgress() },
                secondaryButton: .cancel(Text("Cancelar"))
            )
        }
    }

    private func toggleRow(_ symbol: String, _ title: String, _ value: Binding<Bool>) -> some View {
        Toggle(isOn: value) {
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .frame(width: 24)
                    .foregroundColor(Theme.accent)
                Text(title)
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
        }
        .padding(.vertical, 12)
    }
}
