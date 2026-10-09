import SwiftUI

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
            CurrencyBar()
        }
        .padding(.horizontal, 16)
        .padding(.top, 26)
        .padding(.bottom, 6)
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
            ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
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
