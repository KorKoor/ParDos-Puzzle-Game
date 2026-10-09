import SwiftUI
import UIKit
import Shared

/// Torre infinita, Carrera, Duelo local, partidas libres y poderes. Las reglas viven en Kotlin (`MetaSession`/`GameSession`).
extension AppModel {

    // MARK: Torre infinita

    func startTower() {
        let json = act { $0.towerStart() }
        tower = decodeJSON(TowerInfo.self, json)
        startTowerFloor()
    }

    func startTowerFloor() {
        guard let info = tower else { return }
        mode = .tower
        session.tutorialEnabled = false
        session.start(level: Int32(info.levelId))
        assistMessage = nil
        begin(introFor: card(info.levelId), key: "tower_\(info.floor)")
    }

    func finishTower(_ s: BoardSnap) {
        if s.status == "won" {
            let json = act { $0.towerWin(maxTile: Int32(s.maxTile), merges: Int32(s.merges), kind: s.kind, boss: s.kind == "BOSS", flow: Int32(s.flow)) }
            towerWinInfo = decodeJSON(TowerWinInfo.self, json)
            if hapticsOn { UINotificationFeedbackGenerator().notificationOccurred(.success) }
        } else {
            let json = act { $0.towerLose() }
            towerLossInfo = decodeJSON(TowerLossInfo.self, json)
        }
        tower = decodeJSON(TowerInfo.self, meta.towerInfo())
    }

    // MARK: Carrera

    func startRace() {
        mode = .race
        raceCleared = 0
        raceMerges = 0
        raceMaxTile = 0
        raceEnd = nil
        raceStage = decodeJSON(RaceStageInfo.self, meta.raceStage(n: 1))
        guard let stage = raceStage else { return }
        session.tutorialEnabled = false
        session.startCustom(
            size: Int32(stage.size), target: Int32(stage.target), timeLimitMs: Int64(stage.startMs), seed: 0,
            levelNumber: Int32(stage.n), label: "ETAPA \(stage.n)", comboBonus: false, powers: true
        )
        assistMessage = nil
        begin(introFor: nil, key: "race")
    }

    func finishRace(_ s: BoardSnap) {
        raceMerges += s.merges
        raceMaxTile = max(raceMaxTile, s.maxTile)
        if s.status == "won", let stage = raceStage {
            raceCleared += 1
            act { m in
                m.raceStageCleared()
                return "{}"
            }
            let remaining = Int64(s.timeLeftMs ?? 0)
            let newTime = meta.raceNextTime(n: Int32(stage.n), remainingMs: remaining)
            raceFlash = Int((newTime - remaining) / 1000)
            let next = decodeJSON(RaceStageInfo.self, meta.raceStage(n: Int32(stage.n + 1)))
            raceStage = next
            if let n = next {
                session.startCustom(
                    size: Int32(n.size), target: Int32(n.target), timeLimitMs: newTime, seed: 0,
                    levelNumber: Int32(n.n), label: "ETAPA \(n.n)", comboBonus: false, powers: true
                )
                rewardedKey = ""
                hint = nil
                refresh(animated: false)
                sounds.play(.goal_reached)
                Haptics.success()
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { [weak self] in
                    self?.raceFlash = nil
                }
            }
        } else {
            let json = act { $0.raceFinish(stages: Int32(raceCleared), merges: Int32(raceMerges), maxTile: Int32(raceMaxTile)) }
            raceEnd = decodeJSON(RaceEndInfo.self, json)
        }
    }

    // MARK: Duelo local

    func startDuel() {
        mode = .duel
        duelSeed = Int64(Date().timeIntervalSince1970 * 1000.0) % 1_000_000_007
        duelPlayer = 1
        duelScores = [0, 0]
        duelPhase = .playing
        duelResult = nil
        startDuelRound()
    }

    func startDuelRound() {
        guard let cfg = decodeJSON(DuelConfigInfo.self, meta.duelConfig()) else { return }
        mode = .duel
        duelPhase = .playing
        session.tutorialEnabled = false
        session.startCustom(
            size: Int32(cfg.size), target: Int32(cfg.target), timeLimitMs: Int64(cfg.roundMs), seed: duelSeed,
            levelNumber: Int32(duelPlayer), label: "JUGADOR \(duelPlayer)", comboBonus: false, powers: false
        )
        assistMessage = nil
        begin(introFor: nil, key: "duel_\(duelPlayer)")
    }

    func duelStartSecondPlayer() {
        duelPlayer = 2
        startDuelRound()
    }

    func finishDuel(_ s: BoardSnap) {
        duelScores[duelPlayer - 1] = s.score
        if duelPlayer == 1 {
            duelPhase = .handover
        } else {
            let json = act { $0.duelResult(score1: Int32(duelScores[0]), score2: Int32(duelScores[1])) }
            duelResult = decodeJSON(DuelResultInfo.self, json)
            duelPhase = .result
        }
    }

    // MARK: Partida libre

    func startTables() {
        guard let info = decodeJSON(TablesInfo.self, meta.tablesInfo()) else { return }
        startCustom(size: 4, target: info.target, timed: false, fast: false, title: info.label)
        customTables = true
    }

    func startCustom(size: Int, target: Int, timed: Bool, fast: Bool = false, title: String? = nil) {
        mode = .custom
        customTables = false
        customSize = size
        customTarget = target
        customTimed = timed || fast
        customFast = fast
        var seconds = 0
        if fast {
            seconds = 60
        } else if timed {
            if target <= 64 { seconds = 180 } else if target <= 128 { seconds = 360 } else if target <= 512 { seconds = 600 } else if target <= 2048 { seconds = 900 } else { seconds = 1200 }
        }
        session.tutorialEnabled = false
        session.startCustom(
            size: Int32(size), target: Int32(target), timeLimitMs: Int64(seconds * 1000), seed: 0,
            levelNumber: 1, label: title ?? (fast ? "Partida rápida" : "Partida libre"), comboBonus: timed || fast, powers: true
        )
        assistMessage = nil
        begin(introFor: nil, key: "custom")
    }

    func finishCustom(_ s: BoardSnap) {
        if customTables && s.status == "won" {
            act { m in
                m.tablesWon()
                return "{}"
            }
        }
        act { m in
            m.customFinished(score: Int32(s.score), won: s.status == "won", merges: Int32(s.merges), maxTile: Int32(s.maxTile))
            return "{}"
        }
    }

    func loadRecords() -> RecordsInfo? {
        return decodeJSON(RecordsInfo.self, meta.records())
    }

    func loadRuns() -> [RunInfo] {
        return decodeJSON([RunInfo].self, meta.recentRuns()) ?? []
    }

    // MARK: Poderes

    var powerCooldownMs: Int { eco?.powerCooldownMs ?? 900000 }

    /// Milisegundos que faltan para volver a usar un poder con espera (0 = listo).
    func powerRemaining(_ last: Date?, now: Date) -> Int {
        guard let last = last else { return 0 }
        let passed = Int(now.timeIntervalSince(last) * 1000.0)
        return max(0, powerCooldownMs - passed)
    }

    func useClean() {
        guard let current = snap, current.status == "playing", current.powers else { return }
        if powerRemaining(lastClean, now: Date()) > 0 {
            showToast("Limpiar se está recargando")
            return
        }
        if session.powerClean() {
            lastClean = Date()
            sounds.play(.power_clean)
            afterPower(silent: true)
        } else {
            showToast("No hay nada que limpiar todavía")
        }
    }

    func useMerge() {
        guard let current = snap, current.status == "playing", current.powers else { return }
        if powerRemaining(lastMerge, now: Date()) > 0 {
            showToast("Fusión se está recargando")
            return
        }
        if session.powerMerge() {
            lastMerge = Date()
            sounds.play(.power_merge)
            afterPower(silent: true)
        } else {
            showToast("No hay dos fichas iguales")
        }
    }

    /// Escoba (quitar una ficha) y Unir (fusionar dos iguales): se eligen con un toque y se pagan con monedas.
    func beginSelect(_ kind: String) {
        guard let current = snap, current.status == "playing", current.powers else { return }
        let price = eco?.powerPrice ?? 80
        if coins < price && !(state?.vip ?? false) {
            adPowerKind = kind
            return
        }
        selectMode = kind
        firstPick = nil
    }

    func cancelSelect() {
        selectMode = nil
        firstPick = nil
    }

    func tapTile(_ id: String) {
        guard let kind = selectMode else { return }
        if kind == "BROOM" {
            if session.powerBroom(tileId: id) {
                sounds.play(.power_broom)
                chargeManualPower()
            } else {
                cancelSelect()
            }
            return
        }
        guard let first = firstPick else {
            firstPick = id
            buzz(.light)
            return
        }
        if first == id {
            firstPick = nil
            return
        }
        if session.powerLink(firstId: first, secondId: id) {
            sounds.play(.power_link)
            chargeManualPower()
        } else {
            showToast("Tienen que valer lo mismo")
            cancelSelect()
        }
    }

    func chargeManualPower() {
        if freePowerPending {
            freePowerPending = false
        } else {
            run { $0.buyManualPower() }
        }
        cancelSelect()
        afterPower(silent: true)
    }

    func afterPower(silent: Bool = false) {
        usedHelp = true
        hint = nil
        if !silent { sounds.play(.power_merge) }
        Haptics.medium()
        refresh(animated: true)
    }
}

// MARK: - Logros y prestigio

extension AppModel {
    var achievementMode: String {
        switch mode {
        case .campaign, .tower: return "CLASICO"
        case .daily, .race: return "DESAFIO"
        case .duel, .remote: return "DUELO"
        case .custom: return customFast ? "RAPIDO" : (customTimed ? "DESAFIO" : "ZEN")
        }
    }

    /// Después de cada jugada se miran los logros con el estado del tablero.
    func checkAchievements(_ now: BoardSnap) {
        var parts: [String] = []
        for tile in now.tiles { parts.append("\(tile.v):\(tile.r):\(tile.c)") }
        tickClock()
        let json = meta.checkAchievements(
            completed: now.status == "won", level: Int32(now.level), moves: Int32(now.moves), elapsedMs: Int64(now.elapsedMs),
            score: Int32(now.score), combo: Int32(now.combo), empty: Int32(now.empty), hasMoves: !now.stuck,
            size: Int32(now.size), mode: achievementMode, tiles: parts.joined(separator: ";")
        )
        guard let result = decodeJSON(AchCheckResult.self, json), !result.unlocked.isEmpty else { return }
        persist()
        refreshState()
        achQueue.append(contentsOf: result.unlocked)
        showNextAchievement()
    }

    func showNextAchievement() {
        if achBanner != nil || achQueue.isEmpty { return }
        achBanner = achQueue.removeFirst()
        sounds.play(.achievement)
        Haptics.celebrate()
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.6) { [weak self] in
            self?.achBanner = nil
            self?.showNextAchievement()
        }
    }

    func loadAchievements() -> AchListData? {
        return decodeJSON(AchListData.self, meta.achievementsList())
    }

    func loadPrestige() -> PrestigeData? {
        tickClock()
        let data = decodeJSON(PrestigeData.self, meta.prestigeState())
        persist()
        return data
    }

    func equipTitle(_ id: String) { run { $0.equipTitle(id: id) } }
    func buyTitle(_ id: String) { run { $0.buyTitle(id: id) } }

    /// Hitos, rangos, títulos y Platino nuevos: se avisan una sola vez.
    func deliverPrestigeEvents() {
        guard let events = decodeJSON([PrestigeEventInfo].self, meta.takePrestigeEvents()) else { return }
        for event in events {
            switch event.type {
            case "rank":
                push(.info("¡Rango \(event.title ?? "")!", "Premio: +\(event.coins ?? 0) monedas y +\(event.gems ?? 0) gemas" + (event.chest != nil ? " y un cofre" : ""), "crown.fill"))
            case "platinum":
                push(.info("¡PLATINO!", "Tienes todos los logros y todos los hitos. +\(event.coins ?? 0) monedas, +\(event.gems ?? 0) gemas.", "trophy.fill"))
            case "backfill":
                push(.info("Hitos cobrados", "Ya habías logrado \(event.count ?? 0) hitos: +\(event.coins ?? 0) monedas y +\(event.gems ?? 0) gemas.", "checkmark.seal.fill"))
            case "title":
                showToast("Título nuevo: \(event.title ?? "")")
            default:
                showToast("Hito: \(event.title ?? "") · +\(event.coins ?? 0) monedas")
            }
        }
    }
}

// MARK: - Segunda oportunidad

extension AppModel {
    /// Seguir jugando tras perder (una vez por partida): cuesta gemas, porque en iPhone no hay anuncios con premio.
    func revive() {
        guard let current = snap, current.canRevive else { return }
        let price = eco?.revivePrice ?? 12
        if gems < price {
            showToast("Necesitas \(price) gemas")
            return
        }
        if run({ $0.buyRevive() }) && session.revive() {
            finishRevive()
        }
    }

    /// Seguir jugando viendo un anuncio (gratis, una vez por partida).
    func reviveWithAd() {
        guard let current = snap, current.canRevive else { return }
        withAd {
            if self.session.revive() {
                self.finishRevive()
            }
        }
    }

    /// Escoba o Unir gratis viendo un anuncio.
    func useAdPower() {
        guard let kind = adPowerKind else { return }
        adPowerKind = nil
        withAd {
            self.freePowerPending = true
            self.selectMode = kind
            self.firstPick = nil
        }
    }

    private func finishRevive() {
        do {
            rewardedKey = ""
            loss = nil
            hint = nil
            refresh(animated: true)
        }
    }
}

// MARK: - Compartir

extension AppModel {
    func shareText(_ snap: BoardSnap) -> String {
        tickClock()
        let name = snap.daily ? "Reto diario" : "Campaña nivel " + String(snap.level)
        let day: Int32 = snap.daily ? Int32(localDay()) : -1
        return meta.shareVictory(
            modeName: name, stars: Int32(snap.stars), targetTile: Int32(snap.maxTile), moves: Int32(snap.moves),
            timeMs: Int64(snap.elapsedMs), dailyDay: day
        )
    }
}
