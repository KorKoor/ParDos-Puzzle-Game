import SwiftUI

// MARK: - Poderes

/// Los cuatro poderes de la partida: Limpiar y Fusión (con espera) y Escoba y Unir (se eligen con un toque y cuestan monedas).
struct PowerBar: View {
    @EnvironmentObject var model: AppModel
    @State private var now = Date()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        let cleanLeft = model.powerRemaining(model.lastClean, now: now)
        let mergeLeft = model.powerRemaining(model.lastMerge, now: now)
        let price = model.eco?.powerPrice ?? 80
        return HStack(spacing: 8) {
            power("sparkles", "Limpiar", Theme.accent, cleanLeft > 0 ? formatClock(ms: cleanLeft) : nil) { model.useClean() }
            power("arrow.triangle.merge", "Fusión", Theme.gold, mergeLeft > 0 ? formatClock(ms: mergeLeft) : nil) { model.useMerge() }
            power("trash.fill", "Escoba", Theme.energy, "\(price)") { model.beginSelect("BROOM") }
            power("link", "Unir", Color(hex: 0x4E8FA6), "\(price)") { model.beginSelect("LINK") }
        }
        .padding(.horizontal, 16)
        .onReceive(timer) { value in now = value }
    }

    private func power(_ symbol: String, _ title: String, _ color: Color, _ detail: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: symbol)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(color))
                Text(title)
                    .font(.system(size: 10, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text(detail ?? "listo")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.45))
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct SelectBanner: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let text: String
        if model.selectMode == "BROOM" {
            text = "Toca la ficha que quieres quitar"
        } else if model.firstPick == nil {
            text = "Toca la primera ficha"
        } else {
            text = "Ahora toca otra igual"
        }
        return HStack {
            Text(text)
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(.white)
            Spacer()
            Button(action: { model.cancelSelect() }) {
                Text("Cancelar")
                    .font(.system(size: 13, weight: .heavy, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(Color.white.opacity(0.25)))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Theme.energy))
        .padding(.horizontal, 16)
    }
}

/// Corazones de la Torre infinita.
struct TowerHearts: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let info = model.tower
        let hearts = info?.hearts ?? 0
        let maxHearts = max(info?.maxHearts ?? 4, hearts)
        return HStack(spacing: 6) {
            ForEach(0..<maxHearts, id: \.self) { i in
                Image(systemName: i < hearts ? "heart.fill" : "heart")
                    .foregroundColor(i < hearts ? Color(hex: 0xE0475B) : Theme.ink.opacity(0.25))
            }
            Spacer()
            if let label = info?.label {
                Text(label)
                    .font(.system(size: 12, weight: .heavy))
                    .kerning(1.5)
                    .foregroundColor((info?.boss ?? false) ? Color(hex: 0xB4413C) : Theme.ink.opacity(0.6))
            }
        }
        .padding(.horizontal, 20)
    }
}

// MARK: - Resultados de los modos

struct ModeResultOverlay: View {
    @EnvironmentObject var model: AppModel
    let snap: BoardSnap

    var body: some View {
        ZStack {
            Color.black.opacity(0.5).ignoresSafeArea()
            if snap.status == "won" && model.mode == .tower { ConfettiView() }
            ScrollView(showsIndicators: false) {
                VStack(spacing: 14) {
                    content
                }
                .padding(24)
                .frame(maxWidth: 340)
                .background(RoundedRectangle(cornerRadius: 30, style: .continuous).fill(Theme.cream))
                .padding(.horizontal, 24)
                .padding(.vertical, 40)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch model.mode {
        case .tower: towerContent
        case .race: raceContent
        case .duel: duelContent
        case .remote: RemoteResultContent()
        default: customContent
        }
    }

    private func title(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 26, weight: .black, design: .rounded))
            .foregroundColor(Theme.ink)
            .multilineTextAlignment(.center)
    }

    private func sub(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(Theme.ink.opacity(0.6))
            .multilineTextAlignment(.center)
    }

    private func big(_ text: String, color: Color = Theme.accent, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 17, weight: .black, design: .rounded))
                .kerning(1.5)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(color))
        }
    }

    private func small(_ text: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink.opacity(0.6))
        }
    }

    // Torre

    @ViewBuilder
    private var towerContent: some View {
        if snap.status == "won", let win = model.towerWinInfo {
            title("¡Piso \(win.floor) superado!")
            HStack(spacing: 16) {
                HStack(spacing: 5) { CoinIcon(size: 26); Text("+\(win.coins)") }
                if win.gems > 0 { HStack(spacing: 5) { GemIcon(size: 26); Text("+\(win.gems)") } }
                if win.heart { HStack(spacing: 5) { Image(systemName: "heart.fill").foregroundColor(Color(hex: 0xE0475B)); Text("+1") } }
            }
            .font(.system(size: 22, weight: .black, design: .rounded))
            .foregroundColor(Theme.ink)
            sub("Tienes \(win.hearts) \(win.hearts == 1 ? "corazón" : "corazones"). Cada 5 pisos hay un jefe que devuelve uno.")
            big("SUBIR AL PISO \(win.floor + 1)") { model.nextLevel() }
            small("Salir de la torre") { model.backToMap() }
        } else if let loss = model.towerLossInfo {
            if loss.over {
                title(loss.newRecord ? "¡Nuevo récord!" : "Fin de la subida")
                sub("Llegaste al piso \(loss.floor). Tu mejor piso: \(loss.best).")
                big("EMPEZAR OTRA SUBIDA") { model.startTower() }
            } else {
                title("Perdiste un corazón")
                HStack(spacing: 6) {
                    ForEach(0..<max(loss.hearts, 1), id: \.self) { _ in
                        Image(systemName: "heart.fill").foregroundColor(Color(hex: 0xE0475B)).font(.system(size: 26))
                    }
                }
                sub("Te quedan \(loss.hearts). El piso se repite.")
                big("REINTENTAR EL PISO") { model.restart() }
            }
            small("Salir de la torre") { model.backToMap() }
        }
    }

    // Carrera

    @ViewBuilder
    private var raceContent: some View {
        if let end = model.raceEnd {
            title(end.newRecord ? "¡Nuevo récord!" : "Carrera terminada")
            Text("\(end.stages) \(end.stages == 1 ? "etapa" : "etapas")")
                .font(.system(size: 34, weight: .black, design: .rounded))
                .foregroundColor(Theme.accent)
            HStack(spacing: 6) {
                CoinIcon(size: 24)
                Text("+\(end.coins)")
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
            sub("Tu récord: \(end.best) etapas")
            big("OTRA CARRERA") { model.startRace() }
            small("Volver al mapa") { model.backToMap() }
        }
    }

    // Duelo

    @ViewBuilder
    private var duelContent: some View {
        if model.duelPhase == .handover {
            title("¡Turno del jugador 2!")
            Text("Jugador 1: \(model.duelScores[0]) puntos")
                .font(.system(size: 20, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            sub("Pásale el teléfono: tendrá el mismo tablero y los mismos 60 segundos.")
            big("EMPIEZA EL JUGADOR 2", color: Theme.energy) { model.duelStartSecondPlayer() }
        } else if let result = model.duelResult {
            title(result.winner == "TIE" ? "¡Empate!" : (result.winner == "PLAYER_1" ? "¡Gana el jugador 1!" : "¡Gana el jugador 2!"))
            HStack(spacing: 24) {
                scoreColumn("Jugador 1", model.duelScores[0], result.winner == "PLAYER_1")
                scoreColumn("Jugador 2", model.duelScores[1], result.winner == "PLAYER_2")
            }
            if result.winner != "TIE" { sub("Ganó por \(result.margin) puntos") }
            big("REVANCHA", color: Theme.energy) { model.startDuel() }
            small("Salir") { model.backToMap() }
        }
    }

    private func scoreColumn(_ name: String, _ score: Int, _ winner: Bool) -> some View {
        VStack(spacing: 4) {
            Image(systemName: winner ? "crown.fill" : "person.fill")
                .foregroundColor(winner ? Theme.gold : Theme.ink.opacity(0.3))
            Text("\(score)")
                .font(.system(size: 28, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text(name)
                .font(.system(size: 11, weight: .heavy))
                .foregroundColor(Theme.ink.opacity(0.5))
        }
    }

    // Partida libre

    @ViewBuilder
    private var customContent: some View {
        title(snap.status == "won" ? "¡Meta lograda!" : "Partida terminada")
        Text("\(snap.score) puntos")
            .font(.system(size: 26, weight: .black, design: .rounded))
            .foregroundColor(Theme.accent)
        sub("Las partidas libres no dan monedas, pero cuentan para misiones y el pase.")
        if snap.canRevive {
            big("SEGUIR JUGANDO · " + String(model.eco?.revivePrice ?? 12) + " GEMAS", color: Theme.energy) { model.revive() }
        }
        big("OTRA VEZ") { model.restart() }
        small("Volver al mapa") { model.backToMap() }
    }
}

// MARK: - Modos en la pestaña Jugar

struct ModesSection: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack(spacing: 8) {
            SectionTitle(text: "Otros modos")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    modeCard("Torre infinita", "Piso a piso con 3 corazones", "building.2.fill", Color(hex: 0x8E6BD6)) { model.startTower() }
                    modeCard("Carrera", "Etapas contra el reloj", "flag.checkered", Theme.gold) { model.startRace() }
                    modeCard("Duelo", "Dos jugadores, un teléfono", "person.2.fill", Theme.energy) { model.startDuel() }
                    modeCard("A distancia", "Reta a un amigo con un código", "paperplane.fill", Color(hex: 0xE0568B)) { model.sheet = .remote }
                    modeCard("Libre", "Tu tablero, tu meta", "slider.horizontal.3", Color(hex: 0x2A9D8F)) { model.sheet = .custom }
                    modeCard("Récords", "Tus mejores marcas", "trophy.fill", Color(hex: 0x4E8FA6)) { model.sheet = .records }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 2)
            }
        }
        .padding(.horizontal, 16)
    }

    private func modeCard(_ title: String, _ detail: String, _ symbol: String, _ color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                Image(systemName: symbol)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(color))
                Text(title)
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text(detail)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
            .frame(width: 130, alignment: .leading)
            .padding(12)
            .card(radius: 20)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Partida libre y récords

struct CustomGameSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var size = 4
    @State private var targetIndex = 4
    @State private var timed = false
    private let targets = [16, 32, 64, 128, 256, 512, 1024, 2048, 4096]

    var body: some View {
        VStack(spacing: 14) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            Text("Partida libre")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            VStack(spacing: 14) {
                row("Tablero") {
                    HStack(spacing: 8) {
                        ForEach(3..<7, id: \.self) { n in
                            Button(action: { size = n }) {
                                Text("\(n)×\(n)")
                                    .font(.system(size: 14, weight: .black, design: .rounded))
                                    .foregroundColor(size == n ? .white : Theme.ink.opacity(0.6))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(Capsule().fill(size == n ? Theme.accent : Theme.ink.opacity(0.07)))
                            }
                        }
                    }
                }
                row("Meta") {
                    HStack(spacing: 10) {
                        Button(action: { targetIndex = max(0, targetIndex - 1) }) {
                            Image(systemName: "minus.circle.fill").font(.system(size: 26)).foregroundColor(Theme.accent)
                        }
                        Text("\(targets[targetIndex])")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundColor(Theme.ink)
                            .frame(minWidth: 80)
                        Button(action: { targetIndex = min(targets.count - 1, targetIndex + 1) }) {
                            Image(systemName: "plus.circle.fill").font(.system(size: 26)).foregroundColor(Theme.accent)
                        }
                    }
                }
                Toggle(isOn: $timed) {
                    Text("Con reloj (las combinaciones dan tiempo)")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(Theme.ink)
                }
            }
            .padding(16)
            .card()
            if size == 3 && targets[targetIndex] > 128 {
                Text("En un tablero de 3×3 las metas muy altas son casi imposibles.")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.energy)
            }
            BigButton(title: "JUGAR") {
                model.startCustom(size: size, target: targets[targetIndex], timed: timed)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 22)
        .background(Theme.cream.ignoresSafeArea())
    }

    private func row<Content: View>(_ title: String, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(spacing: 8) {
            Text(title.uppercased())
                .font(.system(size: 10, weight: .heavy))
                .kerning(2)
                .foregroundColor(Theme.ink.opacity(0.4))
            content()
        }
    }
}

struct RecordsSheet: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let r = model.loadRecords()
        return VStack(spacing: 14) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            Text("Récords")
                .font(.system(size: 24, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            if let r = r {
                VStack(spacing: 0) {
                    line("building.2.fill", "Torre infinita", "piso \(r.tower)")
                    Divider()
                    line("flag.checkered", "Carrera", "\(r.race) etapas")
                    Divider()
                    line("person.2.fill", "Duelo (mejor puntaje)", "\(r.duel)")
                    Divider()
                    line("slider.horizontal.3", "Partida libre", "\(r.custom)")
                    Divider()
                    line("flame.fill", "Mejor racha de días", "\(r.bestStreak)")
                    Divider()
                    line("square.stack.3d.up.fill", "Ficha más alta", "\(r.bestTile)")
                    Divider()
                    line("flag.fill", "Victorias", "\(r.totalWins)")
                    Divider()
                    line("star.fill", "Estrellas", "\(r.totalStars)")
                    Divider()
                    line("calendar", "Días jugados", "\(r.daysPlayed)")
                }
                .padding(.horizontal, 16)
                .card()
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 22)
        .background(Theme.cream.ignoresSafeArea())
    }

    private func line(_ symbol: String, _ title: String, _ value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol).foregroundColor(Theme.accent).frame(width: 26)
            Text(title)
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundColor(Theme.ink)
            Spacer()
            Text(value)
                .font(.system(size: 15, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
        }
        .padding(.vertical, 12)
    }
}
