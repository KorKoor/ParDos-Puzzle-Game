import SwiftUI
import UIKit
import Shared

enum AppScreen {
    case menu
    case map
    case game
}

/// Estado de la app. La partida la lleva `GameSession` (Kotlin, la misma lógica que Android); aquí solo se guarda el
/// progreso del jugador y se traduce lo que devuelve la sesión.
final class AppModel: ObservableObject {
    private let session = GameSession()
    private let defaults = UserDefaults.standard
    private let sounds = SoundManager.shared

    @Published var screen: AppScreen = .menu
    @Published var snap: BoardSnap?
    @Published var hint: GuideHint?
    @Published var preview: LevelCard?
    @Published var intro: LevelCard?
    @Published var toast: String?
    @Published var dailyCard: LevelCard?
    @Published var showSettings = false

    private var cards: [Int: LevelCard] = [:]
    private var idleSeconds: Double = 0
    private var lastPhase: Int = 1
    private var recordedKey: String = ""
    private var playing = false
    private var lastDailyDay: Int = 0

    init() {
        refreshDaily()
        sounds.updateMusic(shouldPlay: true)
    }

    // MARK: Ajustes

    var hapticsOn: Bool {
        (defaults.object(forKey: "haptics_on") as? Bool) ?? true
    }

    private func buzz(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        if hapticsOn {
            UIImpactFeedbackGenerator(style: style).impactOccurred()
        }
    }

    /// La música suena en el menú y el mapa, no durante la partida.
    func syncMusic() {
        sounds.updateMusic(shouldPlay: screen != .game)
    }

    func resetProgress() {
        let prefixes = ["stars_", "best_", "daily_", "seen_", "streak_"]
        let exact = ["unlocked", "tutorial_done"]
        for key in defaults.dictionaryRepresentation().keys {
            if exact.contains(key) || prefixes.contains(where: { key.hasPrefix($0) }) {
                defaults.removeObject(forKey: key)
            }
        }
        objectWillChange.send()
    }

    // MARK: Progreso guardado

    var levelCount: Int { Int(session.levelCount()) }

    /// Último nivel desbloqueado.
    var unlocked: Int {
        let saved = defaults.integer(forKey: "unlocked")
        return saved < 1 ? 1 : saved
    }

    func stars(_ level: Int) -> Int { defaults.integer(forKey: "stars_\(level)") }

    func best(_ level: Int) -> Int { defaults.integer(forKey: "best_\(level)") }

    var totalStars: Int {
        var total = 0
        var level = 1
        while level <= unlocked {
            total += stars(level)
            level += 1
        }
        return total
    }

    var levelsWon: Int {
        var won = 0
        var level = 1
        while level <= unlocked {
            if stars(level) > 0 { won += 1 }
            level += 1
        }
        return won
    }

    func chapterStars(_ chapter: Int) -> Int {
        var total = 0
        let first = chapter * Chapters.size + 1
        for level in first..<(first + Chapters.size) {
            total += stars(level)
        }
        return total
    }

    // MARK: Racha y reto diario

    /// Día local contado desde 1970 (el mismo que usa Android para el reto diario).
    func localDay() -> Int {
        let seconds = Date().timeIntervalSince1970 + Double(TimeZone.current.secondsFromGMT())
        return Int(floor(seconds / 86400.0))
    }

    /// Días seguidos jugando (0 si ya se rompió).
    var streak: Int {
        let last = defaults.integer(forKey: "streak_last")
        let today = localDay()
        if last == today || last == today - 1 {
            return defaults.integer(forKey: "streak_count")
        }
        return 0
    }

    private func bumpStreak() {
        let today = localDay()
        let last = defaults.integer(forKey: "streak_last")
        if last == today { return }
        let next = (last == today - 1) ? defaults.integer(forKey: "streak_count") + 1 : 1
        defaults.set(next, forKey: "streak_count")
        defaults.set(today, forKey: "streak_last")
    }

    var dailyDone: Bool { defaults.bool(forKey: "daily_done_\(localDay())") }

    var dailyStars: Int { defaults.integer(forKey: "daily_stars_\(localDay())") }

    func refreshDaily() {
        let day = localDay()
        lastDailyDay = day
        dailyCard = decodeJSON(LevelCard.self, session.dailyInfo(epochDay: Int32(day)))
    }

    // MARK: Niveles

    func card(_ level: Int) -> LevelCard? {
        if let cached = cards[level] { return cached }
        guard let fresh = decodeJSON(LevelCard.self, session.levelInfo(level: Int32(level))) else { return nil }
        cards[level] = fresh
        return fresh
    }

    func openPreview(_ level: Int) {
        preview = card(level)
    }

    func start(_ level: Int) {
        session.tutorialEnabled = level == 1 && !defaults.bool(forKey: "tutorial_done")
        session.start(level: Int32(level))
        begin(introFor: card(level), key: levelKey(level))
    }

    func startDaily() {
        if localDay() != lastDailyDay { refreshDaily() }
        session.tutorialEnabled = false
        session.startDaily(epochDay: Int32(localDay()))
        begin(introFor: dailyCard, key: "daily_\(localDay())")
    }

    private func levelKey(_ level: Int) -> String { "level_\(level)" }

    private func begin(introFor card: LevelCard?, key: String) {
        preview = nil
        hint = nil
        toast = nil
        idleSeconds = 0
        lastPhase = 1
        recordedKey = ""
        playing = true
        refresh(animated: false)
        intro = needsIntro(card) ? card : nil
        screen = .game
        syncMusic()
    }

    /// La primera vez que sale cada tipo de regla se explica; los jefes se explican una vez cada uno.
    private func needsIntro(_ card: LevelCard?) -> Bool {
        guard let card = card, card.kind != "ZEN" else { return false }
        let key = card.boss ? "seen_boss_\(card.id)_\(card.title)" : "seen_kind_\(card.kind)"
        return !defaults.bool(forKey: key)
    }

    func dismissIntro() {
        if let card = intro {
            let key = card.boss ? "seen_boss_\(card.id)_\(card.title)" : "seen_kind_\(card.kind)"
            defaults.set(true, forKey: key)
        }
        intro = nil
        idleSeconds = 0
    }

    func restart() {
        guard let current = snap else { return }
        if current.daily { startDaily() } else { start(current.level) }
    }

    func nextLevel() {
        guard let current = snap else { return }
        if current.daily {
            backToMenu()
        } else {
            start(min(current.level + 1, levelCount))
        }
    }

    func backToMap() {
        screen = .map
        syncMusic()
    }

    func backToMenu() {
        screen = .menu
        refreshDaily()
        syncMusic()
    }

    // MARK: Jugar

    func swipe(_ direction: Int) {
        guard intro == nil, let current = snap, current.status == "playing" else { return }
        idleSeconds = 0
        hint = nil
        let moved = session.move(direction: Int32(direction))
        refresh(animated: true)
        guard let now = snap else { return }
        if moved {
            let merged = now.tiles.contains(where: { $0.merged })
            sounds.play(merged ? "better_pop" : "move_pop")
            buzz(merged ? .medium : .light)
        }
        if !now.blocked.isEmpty {
            showToast(now.blocked)
        } else if moved && now.phase == 2 && lastPhase == 1 {
            showToast(now.phaseTitle)
        }
        lastPhase = now.phase
    }

    func undo() {
        if session.undoMove() {
            hint = nil
            refresh(animated: true)
        }
    }

    func showHint() {
        guard let current = snap, current.status == "playing" else { return }
        hint = decodeJSON(GuideHint.self, session.hint())
    }

    /// Se llama cuatro veces por segundo mientras se juega: mueve el reloj y enseña una pista si te quedas parado.
    func tick() {
        guard screen == .game, intro == nil, let current = snap, current.status == "playing" else { return }
        session.tick(deltaMs: 250)
        idleSeconds += 0.25
        if current.timeLeftMs != nil {
            refresh(animated: false)
        }
        if hint == nil && current.level <= 15 && !current.daily && current.coach == nil && idleSeconds >= 7 {
            hint = decodeJSON(GuideHint.self, session.hint())
        }
    }

    private func refresh(animated: Bool) {
        guard let fresh = decodeJSON(BoardSnap.self, session.snapshot()) else { return }
        let before = snap?.status
        if animated {
            withAnimation(.spring(response: 0.26, dampingFraction: 0.78)) {
                snap = fresh
            }
        } else {
            snap = fresh
        }
        if before == "playing" && fresh.status != "playing" {
            sounds.play(fresh.status == "won" ? "win" : "game_over")
        }
        record(fresh)
    }

    /// Guarda estrellas y desbloquea el siguiente nivel la primera vez que se gana; marca el tutorial como hecho.
    private func record(_ s: BoardSnap) {
        if s.tutorialDone && !defaults.bool(forKey: "tutorial_done") {
            defaults.set(true, forKey: "tutorial_done")
        }
        guard s.status == "won" else { return }
        let key = s.daily ? "daily_\(localDay())" : levelKey(s.level)
        guard recordedKey != key else { return }
        recordedKey = key
        bumpStreak()
        if s.daily {
            defaults.set(true, forKey: "daily_done_\(localDay())")
            if s.stars > dailyStars { defaults.set(s.stars, forKey: "daily_stars_\(localDay())") }
        } else {
            if s.stars > stars(s.level) { defaults.set(s.stars, forKey: "stars_\(s.level)") }
            if best(s.level) == 0 || s.moves < best(s.level) { defaults.set(s.moves, forKey: "best_\(s.level)") }
            if s.level + 1 > unlocked { defaults.set(s.level + 1, forKey: "unlocked") }
            if s.level == 1 { defaults.set(true, forKey: "tutorial_done") }
        }
        if hapticsOn {
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        }
    }

    private func showToast(_ text: String) {
        toast = text
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { [weak self] in
            if self?.toast == text { self?.toast = nil }
        }
    }
}
