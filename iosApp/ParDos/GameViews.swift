import SwiftUI

// MARK: - Pantalla de partida

struct GameView: View {
    @EnvironmentObject var model: AppModel
    @State private var confirmExit = false
    private let clock = Timer.publish(every: 0.25, on: .main, in: .common).autoconnect()

    var body: some View {
        let style = model.boardStyle
        return ZStack {
            if style.changesTheme {
                style.background.ignoresSafeArea()
            }
            ParticlesView(kind: style.particles, tint: style.particleTint)
            if let snap = model.snap {
                VStack(spacing: 12) {
                    topBar(snap, style)
                    if model.mode == .tower { TowerHearts() }
                    GoalCard(snap: snap)
                    RuleChips(snap: snap)
                    BoardView(snap: snap, hint: currentGuide(snap))
                        .padding(.horizontal, 14)
                    StatsRow(snap: snap)
                    if model.selectMode != nil { SelectBanner() }
                    bottom(snap)
                    Spacer(minLength: 0)
                }
                .padding(.top, 6)
                if snap.status != "playing" {
                    if model.mode == .campaign || model.mode == .daily {
                        ResultOverlay(snap: snap)
                    } else {
                        ModeResultOverlay(snap: snap)
                    }
                }
                if !snap.callout.isEmpty {
                    Text(snap.callout)
                        .font(.system(size: 30, weight: .black, design: .rounded))
                        .foregroundColor(Theme.energy)
                        .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
                        .transition(.scale)
                        .allowsHitTesting(false)
                }
                if let flash = model.raceFlash {
                    Text("+\(flash) s")
                        .font(.system(size: 44, weight: .black, design: .rounded))
                        .foregroundColor(Theme.gold)
                        .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                        .transition(.scale)
                }
                if let card = model.intro {
                    RuleIntroView(card: card) { model.dismissIntro() }
                }
            }
        }
        .onReceive(clock) { _ in model.tick() }
        .alert(isPresented: $confirmExit) {
            Alert(
                title: Text("¿Salir del nivel?"),
                message: Text("Se pierde lo que llevas de esta partida."),
                primaryButton: .destructive(Text("Salir")) { model.backToMap() },
                secondaryButton: .cancel(Text("Seguir jugando"))
            )
        }
    }

    private func leave(_ snap: BoardSnap) {
        if snap.status == "playing" && snap.moves > 0 {
            confirmExit = true
        } else {
            model.backToMap()
        }
    }

    /// La guía del tutorial manda; si no hay tutorial, la pista suelta.
    private func currentGuide(_ snap: BoardSnap) -> GuideHint? {
        if let step = snap.coach, !step.isEmpty, let dir = snap.coachDir {
            return GuideHint(dir: dir, cells: snap.coachCells ?? [])
        }
        return model.hint
    }

    private func topBar(_ snap: BoardSnap, _ style: BoardStyle) -> some View {
        HStack(spacing: 10) {
            Button(action: { leave(snap) }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(Theme.ink)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(Color.white))
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(headerKicker(snap))
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(2)
                    .foregroundColor(model.mode == .campaign ? kindColor(snap.kind) : Theme.accent)
                Text(headerTitle(snap))
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(style.ink ?? Theme.ink)
            }
            Spacer()
            Button(action: { model.restart() }) {
                Image(systemName: "arrow.counterclockwise")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(Theme.ink)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(Color.white))
            }
        }
        .padding(.horizontal, 16)
    }

    private func headerKicker(_ snap: BoardSnap) -> String {
        switch model.mode {
        case .daily: return "RETO DIARIO · " + snap.kindLabel.uppercased()
        case .tower: return model.tower?.label ?? "TORRE"
        case .race: return "CARRERA · \(model.raceCleared) SUPERADAS"
        case .duel: return "DUELO LOCAL"
        case .custom: return "PARTIDA LIBRE"
        case .campaign: return snap.kindLabel.uppercased()
        }
    }

    private func headerTitle(_ snap: BoardSnap) -> String {
        switch model.mode {
        case .daily: return "Reto de hoy"
        case .tower: return "Piso \(model.tower?.floor ?? 1)"
        case .race: return "Etapa \(model.raceStage?.n ?? 1)"
        case .duel: return "Jugador \(model.duelPlayer)"
        case .custom: return "\(snap.size)×\(snap.size) · \(snap.goal)"
        case .campaign: return "Nivel \(snap.level)"
        }
    }

    @ViewBuilder
    private func bottom(_ snap: BoardSnap) -> some View {
        if let text = snap.coach, !text.isEmpty {
            CoachCard(text: text, done: snap.coachDone ?? 0, needed: snap.coachNeeded ?? 3)
        } else {
            HStack(spacing: 10) {
                actionButton("arrow.uturn.backward", "Deshacer", count: model.state?.undos ?? 0, enabled: snap.canUndo) { model.undo() }
                actionButton("lightbulb.fill", "Pista", count: nil, enabled: snap.status == "playing") { model.showHint() }
                if snap.timeLeftMs != nil {
                    actionButton("timer", "+20 s", count: model.state?.extraTimes ?? 0, enabled: snap.status == "playing") { model.addExtraTime() }
                }
            }
            .padding(.horizontal, 16)
            if snap.powers && model.selectMode == nil && snap.status == "playing" {
                PowerBar()
            }
        }
    }

    private func actionButton(_ symbol: String, _ title: String, count: Int?, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: symbol)
                Text(title).font(.system(size: 14, weight: .heavy, design: .rounded))
                if let count = count {
                    Text("\(count)")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(count > 0 ? Theme.accent : Color.gray.opacity(0.6)))
                }
            }
            .foregroundColor(enabled ? Theme.ink : Theme.ink.opacity(0.3))
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Color.white))
        }
        .disabled(!enabled)
    }
}

// MARK: - Meta, reglas y marcadores

struct GoalCard: View {
    let snap: BoardSnap

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text(snap.goal)
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Text("\(snap.size)×\(snap.size)")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.1))
                    Capsule()
                        .fill(LinearGradient(colors: [Theme.accent, Theme.gold], startPoint: .leading, endPoint: .trailing))
                        .frame(width: max(8, geo.size.width * CGFloat(min(1, max(0, snap.progress)))))
                }
            }
            .frame(height: 8)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
        .padding(.horizontal, 16)
    }
}

struct RuleChips: View {
    let snap: BoardSnap

    var body: some View {
        HStack(spacing: 8) {
            if let left = snap.movesLeft {
                chip("\(left) mov.", color: left <= 5 ? Theme.energy : Theme.ink, filled: left <= 5)
            }
            if !snap.twist.isEmpty {
                chip(snap.twist, color: Color(hex: 0x5C6BC0), filled: false)
            }
            if snap.phase >= 2 {
                chip("Fase 2", color: Color(hex: 0xB4413C), filled: true)
            }
            if !snap.stones.isEmpty || !snap.storm.isEmpty {
                chip("\(snap.stones.count + snap.storm.count) piedras", color: Color(hex: 0x7C869B), filled: false)
            }
        }
        .frame(minHeight: 26)
    }

    private func chip(_ text: String, color: Color, filled: Bool) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .heavy, design: .rounded))
            .foregroundColor(filled ? .white : color)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Capsule().fill(filled ? color : Color.white))
    }
}

struct StatsRow: View {
    let snap: BoardSnap

    var body: some View {
        HStack(spacing: 10) {
            stat("MOVIMIENTOS", "\(snap.moves)")
            stat("PUNTOS", "\(snap.score)")
            if let ms = snap.timeLeftMs {
                stat("TIEMPO", clock(ms))
            }
        }
        .padding(.horizontal, 16)
    }

    private func stat(_ title: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 20, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text(title)
                .font(.system(size: 9, weight: .heavy))
                .kerning(1.5)
                .foregroundColor(Theme.ink.opacity(0.4))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Color.white))
    }

    private func clock(_ ms: Int) -> String {
        let total = max(0, ms) / 1000
        return String(format: "%d:%02d", total / 60, total % 60)
    }
}

// MARK: - Tutorial

struct CoachCard: View {
    let text: String
    let done: Int
    let needed: Int

    var body: some View {
        HStack(spacing: 12) {
            SpriteImage(name: "ico_hand", size: 46)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text("TUTORIAL")
                        .font(.system(size: 10, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.accent)
                    ForEach(0..<needed, id: \.self) { i in
                        Circle()
                            .fill(i < done ? Theme.gold : Theme.ink.opacity(0.12))
                            .frame(width: 8, height: 8)
                    }
                }
                Text(text)
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.ink)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Color.white))
        .padding(.horizontal, 16)
    }
}

// MARK: - Avisos

struct ToastView: View {
    let text: String

    var body: some View {
        VStack {
            Text(text)
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(.white)
                .padding(.horizontal, 18)
                .padding(.vertical, 10)
                .background(Capsule().fill(Theme.ink.opacity(0.92)))
                .padding(.top, 90)
            Spacer()
        }
        .transition(.opacity)
        .allowsHitTesting(false)
    }
}

struct ResultOverlay: View {
    @EnvironmentObject var model: AppModel
    let snap: BoardSnap

    private var won: Bool { snap.status == "won" }

    private var headline: String {
        if won { return Theme.halloween ? "¡MONSTRUOSO!" : "¡NIVEL SUPERADO!" }
        switch snap.lostReason {
        case "out_of_moves": return "¡Sin movimientos!"
        case "time_up": return "¡Se acabó el tiempo!"
        default: return "¡Tablero lleno!"
        }
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.45).ignoresSafeArea()
            if won { ConfettiView() }
            ScrollView(showsIndicators: false) {
                card
                    .padding(.vertical, 40)
            }
        }
    }

    private var card: some View {
        VStack(spacing: 14) {
            Text(headline)
                .font(.system(size: 26, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
                .multilineTextAlignment(.center)
            if won {
                HStack(spacing: 8) {
                    ForEach(0..<3, id: \.self) { i in
                        Image(systemName: "star.fill")
                            .font(.system(size: 38))
                            .foregroundColor(i < snap.stars ? Theme.gold : Theme.ink.opacity(0.12))
                    }
                }
                Text("\(snap.moves) movimientos · \(snap.score) puntos")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.6))
                if let reward = model.reward {
                    rewardBox(reward)
                }
            } else {
                Text("Prueba otro orden: cada jugada cuenta.")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.6))
                if let goal = model.loss?.nextGoal {
                    goalLine(goal)
                }
                if snap.canRevive {
                    bigButton("SEGUIR JUGANDO · " + String(model.eco?.revivePrice ?? 12) + " GEMAS", Theme.energy) { model.revive() }
                }
            }
            buttons
        }
        .padding(24)
        .frame(maxWidth: 340)
        .background(RoundedRectangle(cornerRadius: 30, style: .continuous).fill(Theme.cream))
        .padding(.horizontal, 24)
    }

    // MARK: Premios

    private func eventText(_ value: Double) -> String {
        if value == value.rounded() { return "×\(Int(value))" }
        return "×" + String(format: "%.1f", value)
    }

    private func rewardBox(_ reward: WinReward) -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                CoinIcon(size: 30)
                Text("+\(reward.coins)")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
            VStack(spacing: 4) {
                if reward.firstClear { line("Primera vez en este nivel", "+20") }
                if reward.streakPct > 0 { line("Racha de \(reward.winStreak) victorias", "+\(reward.streakPct)%") }
                if reward.albumPct > 0 { line("Bono del álbum", "+\(reward.albumPct)%") }
                if reward.eventMult > 1.0 { line("Evento activo", eventText(reward.eventMult)) }
                if reward.vipBonus { line("VIP", "+20%") }
                if reward.boostActive { line("Impulso de monedas", "+50%") }
                if reward.firstWinCoins > 0 { line("Primera victoria del día", "+\(reward.firstWinCoins)") }
            }
            HStack(spacing: 14) {
                if reward.piggyGems > 0 { pill(AnyView(GemIcon(size: 16)), "+\(reward.piggyGems) hucha") }
                if reward.seasonPoints > 0 { pill(AnyView(SpriteImage(name: "ico_crown", size: 18)), "+\(reward.seasonPoints) pase") }
            }
            if let milestone = reward.winMilestone {
                Text("¡Racha de victorias! +\(milestone.gems) gemas" + (milestone.undos > 0 ? " y \(milestone.undos) deshacer" : ""))
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundColor(Theme.energy)
                    .multilineTextAlignment(.center)
            }
            ForEach(0..<reward.levelRewards.count, id: \.self) { i in
                Text(levelUpText(reward.levelRewards[i]))
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundColor(Theme.accent)
                    .multilineTextAlignment(.center)
            }
            if let chest = reward.chapterChest {
                HStack(spacing: 8) {
                    ChestIcon(type: chest.chest, size: 34)
                    Text("¡Cofre del capítulo \(chest.chapter)! +\(chest.coins) monedas, +\(chest.gems) gemas y un \(chestName(chest.chest).lowercased())")
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundColor(Theme.ink)
                }
            }
            if let goal = reward.nextGoal {
                goalLine(goal)
            }
            if let teaser = reward.teaser {
                Text(teaser.toChest > 0 ? "Siguiente: \(teaser.title) · \(teaser.toChest) niveles para el cofre" : "Siguiente: \(teaser.title)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .multilineTextAlignment(.center)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Theme.gold.opacity(0.13)))
    }

    private func levelUpText(_ lr: LevelRewardInfo) -> String {
        var text = "¡Nivel de jugador \(lr.level)! +\(lr.coins) monedas"
        if lr.gems > 0 { text += " +\(lr.gems) gemas" }
        if lr.chest != nil { text += " + cofre" }
        return text
    }

    private func line(_ text: String, _ value: String) -> some View {
        HStack {
            Text(text)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.6))
            Spacer()
            Text(value)
                .font(.system(size: 12, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
        }
    }

    private func pill(_ icon: AnyView, _ text: String) -> some View {
        HStack(spacing: 4) {
            icon
            Text(text)
                .font(.system(size: 12, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(Capsule().fill(Color.white))
    }

    private func goalLine(_ goal: NextGoalInfo) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "flag.checkered").foregroundColor(Theme.accent)
            VStack(alignment: .leading, spacing: 1) {
                Text(goal.title).font(.system(size: 12, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                Text(goal.detail).font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.55))
            }
            Spacer()
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Color.white))
    }

    // MARK: Botones

    private var buttons: some View {
        VStack(spacing: 12) {
            if won {
                bigButton(snap.daily ? "VOLVER AL MENÚ" : "SIGUIENTE", Theme.accent) { model.nextLevel() }
                smallButton(snap.daily ? "Jugar otra vez" : "Repetir nivel") { model.restart() }
            } else {
                bigButton("REINTENTAR", Theme.accent) { model.restart() }
            }
            if !snap.daily {
                smallButton("Volver al mapa") { model.backToMap() }
            } else if !won {
                smallButton("Volver al menú") { model.backToMenu() }
            }
        }
    }

    private func bigButton(_ title: String, _ color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 17, weight: .black, design: .rounded))
                .kerning(2)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(color))
        }
    }

    private func smallButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink.opacity(0.6))
        }
    }
}
