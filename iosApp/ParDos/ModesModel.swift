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
            let json = act { $0.towerWin(maxTile: Int32(s.maxTile), merges: Int32(s.merges)) }
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
                sounds.play("win", volume: 0.6)
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

    func startCustom(size: Int, target: Int, timed: Bool) {
        mode = .custom
        customSize = size
        customTarget = target
        customTimed = timed
        var seconds = 0
        if timed {
            if target <= 64 { seconds = 180 } else if target <= 128 { seconds = 360 } else if target <= 512 { seconds = 600 } else if target <= 2048 { seconds = 900 } else { seconds = 1200 }
        }
        session.tutorialEnabled = false
        session.startCustom(
            size: Int32(size), target: Int32(target), timeLimitMs: Int64(seconds * 1000), seed: 0,
            levelNumber: 1, label: "Partida libre", comboBonus: timed, powers: true
        )
        assistMessage = nil
        begin(introFor: nil, key: "custom")
    }

    func finishCustom(_ s: BoardSnap) {
        act { m in
            m.customFinished(score: Int32(s.score), won: s.status == "won", merges: Int32(s.merges), maxTile: Int32(s.maxTile))
            return "{}"
        }
    }

    func loadRecords() -> RecordsInfo? {
        return decodeJSON(RecordsInfo.self, meta.records())
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
            afterPower()
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
            afterPower()
        } else {
            showToast("No hay dos fichas iguales")
        }
    }

    /// Escoba (quitar una ficha) y Unir (fusionar dos iguales): se eligen con un toque y se pagan con monedas.
    func beginSelect(_ kind: String) {
        guard let current = snap, current.status == "playing", current.powers else { return }
        let price = eco?.powerPrice ?? 80
        if coins < price {
            showToast("Necesitas \(price) monedas")
            sheet = .lowFunds
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
            if session.powerBroom(tileId: id) { chargeManualPower() } else { cancelSelect() }
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
            chargeManualPower()
        } else {
            showToast("Tienen que valer lo mismo")
            cancelSelect()
        }
    }

    func chargeManualPower() {
        run { $0.buyManualPower() }
        cancelSelect()
        afterPower()
    }

    func afterPower() {
        usedHelp = true
        hint = nil
        sounds.play("better_pop")
        buzz(.medium)
        refresh(animated: true)
    }
}
