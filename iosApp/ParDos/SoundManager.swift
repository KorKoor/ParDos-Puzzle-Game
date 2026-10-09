import AVFoundation
import UIKit

/// Música de fondo: cada pantalla tiene la suya. En Noche de brujas el menú y el mapa suenan embrujados.
enum MusicTheme: String {
    case menu
    case map
    case game
    case shop
    case halloween
}

/// Sonidos y música. Los efectos son archivos sfx_<nombre>.mp3 (ver Sfx.swift, generado por iosApp/tools/audio) y las músicas mus_<nombre>.wav.
/// Respeta el interruptor de silencio del iPhone, los ajustes de la app y se calla si otra app tiene el audio en una llamada.
final class SoundManager {
    static let shared = SoundManager()

    private var datas: [String: Data] = [:]
    private var players: [String: [AVAudioPlayer]] = [:]
    private var lastPlayed: [String: CFTimeInterval] = [:]
    private var music: AVAudioPlayer?
    private var musicTheme: MusicTheme?
    private var wantedTheme: MusicTheme?
    private var duckTimer: Timer?
    private let queue = DispatchQueue(label: "pardos.audio.preload", qos: .utility)
    private let dataLock = NSLock()

    private init() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.ambient, mode: .default, options: [])
        try? session.setActive(true)
        let center = NotificationCenter.default
        center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: OperationQueue.main) { [weak self] note in
            guard let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                  let kind = AVAudioSession.InterruptionType(rawValue: raw) else { return }
            if kind == .ended {
                try? AVAudioSession.sharedInstance().setActive(true)
                self?.resumeMusic()
            } else {
                self?.pauseMusic()
            }
        }
        center.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: OperationQueue.main) { [weak self] _ in
            self?.pauseMusic()
        }
        center.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: OperationQueue.main) { [weak self] _ in
            self?.resumeMusic()
        }
        preload()
    }

    // MARK: ajustes

    private var soundEnabled: Bool {
        return (UserDefaults.standard.object(forKey: "sound_on") as? Bool) ?? true
    }

    private var musicEnabled: Bool {
        return (UserDefaults.standard.object(forKey: "music_on") as? Bool) ?? true
    }

    /// Volumen de los efectos (0 a 1).
    var sfxVolume: Float {
        get { return Float((UserDefaults.standard.object(forKey: "sfx_volume") as? Double) ?? 0.85) }
        set { UserDefaults.standard.set(Double(newValue), forKey: "sfx_volume") }
    }

    /// Volumen de la música (0 a 1).
    var musicVolume: Float {
        get { return Float((UserDefaults.standard.object(forKey: "music_volume") as? Double) ?? 0.6) }
        set {
            UserDefaults.standard.set(Double(newValue), forKey: "music_volume")
            music?.volume = newValue * 0.5
            applyIntensity(fade: 0.2)
        }
    }

    // MARK: efectos

    /// Carga en segundo plano los sonidos que suenan a cada rato para que no haya retraso al primer toque.
    private func preload() {
        let names = ["tap", "tap_soft", "merge_1", "merge_2", "merge_3", "slide_soft", "tile_spawn", "wheel_tick", "coin", "tab", "back", "select", "popup", "blocked", "countdown_tick", "xp_tick"]
        queue.async { [weak self] in
            for name in names { _ = self?.loadData(name) }
        }
    }

    private func loadData(_ name: String) -> Data? {
        dataLock.lock()
        defer { dataLock.unlock() }
        if let cached = datas[name] { return cached }
        let file = name.hasPrefix("sfx_") ? name : "sfx_" + name
        var url = Bundle.main.url(forResource: file, withExtension: "mp3")
        if url == nil { url = Bundle.main.url(forResource: name, withExtension: "mp3") }
        guard let found = url, let raw = try? Data(contentsOf: found) else { return nil }
        datas[name] = raw
        return raw
    }

    /// Toca un efecto nuevo (ver Sfx.swift).
    func play(_ sfx: Sfx, volume: Float = 1.0) {
        playNamed(sfx.rawValue, volume: volume)
    }

    /// Nombres antiguos (los archivos de Android) y nuevos como texto.
    func play(_ name: String, volume: Float = 1.0) {
        playNamed(name, volume: volume)
    }

    private func playNamed(_ name: String, volume: Float) {
        if !soundEnabled { return }
        let now = CACurrentMediaTime()
        if let last = lastPlayed[name], now - last < 0.04 { return }
        lastPlayed[name] = now
        var pool = players[name] ?? []
        var player = pool.first(where: { !$0.isPlaying })
        if player == nil {
            if pool.count >= 4 { pool.removeFirst().stop() }
            guard let raw = loadData(name), let fresh = try? AVAudioPlayer(data: raw) else { return }
            fresh.prepareToPlay()
            pool.append(fresh)
            player = fresh
        }
        players[name] = pool
        guard let ready = player else { return }
        ready.currentTime = 0
        ready.volume = max(0, min(1, volume * sfxVolume))
        ready.play()
    }

    /// Fusión de fichas: suena más rico cuanto más grande es la ficha.
    func playMerge(value: Int, combo: Int = 0) {
        let name: Sfx
        if value >= 2048 {
            name = .merge_max
        } else if value >= 1024 {
            name = .merge_4
        } else if value >= 128 {
            name = .merge_3
        } else if value >= 16 {
            name = .merge_2
        } else {
            name = .merge_1
        }
        play(name, volume: 0.9)
        if combo >= 2 {
            let steps: [Sfx] = [.combo_1, .combo_2, .combo_3, .combo_4, .combo_5, .combo_6]
            play(steps[min(combo, steps.count) - 1], volume: 0.7)
        }
    }

    /// Nivel del golpe de vibración que acompaña a una fusión (1 a 4).
    static func hapticLevel(value: Int) -> Int {
        if value >= 1024 { return 4 }
        if value >= 128 { return 3 }
        if value >= 16 { return 2 }
        return 1
    }

    // MARK: música

    /// La música de partida son tres capas que suenan a la vez (base, ritmo y melodía) y entran según lo emocionante que vaya la partida.
    private var stems: [AVAudioPlayer] = []
    private var intensity: Double = 0

    private var masterMusic: Float {
        return musicVolume * 0.5
    }

    func setMusic(_ theme: MusicTheme?) {
        wantedTheme = theme
        guard let theme = theme, musicEnabled else {
            fadeOutMusic()
            return
        }
        var actual = theme
        if Theme.halloween && (theme == .menu || theme == .map) { actual = .halloween }
        if actual == .game {
            if musicTheme == .game && !stems.isEmpty {
                resumeMusic()
                return
            }
            let old = music
            old?.setVolume(0, fadeDuration: 1.0)
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) { old?.stop() }
            music = nil
            if startStems() { musicTheme = .game }
            return
        }
        stopStems()
        if musicTheme == actual, music != nil {
            resumeMusic()
            return
        }
        guard let url = Bundle.main.url(forResource: "mus_" + actual.rawValue, withExtension: "wav"),
              let next = try? AVAudioPlayer(contentsOf: url) else { return }
        next.numberOfLoops = -1
        next.volume = 0
        next.prepareToPlay()
        next.play()
        next.setVolume(masterMusic, fadeDuration: 1.4)
        let old = music
        old?.setVolume(0, fadeDuration: 1.0)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) { old?.stop() }
        music = next
        musicTheme = actual
    }

    private func startStems() -> Bool {
        var list: [AVAudioPlayer] = []
        for layer in ["base", "groove", "lead"] {
            guard let url = Bundle.main.url(forResource: "mus_game_" + layer, withExtension: "wav"),
                  let player = try? AVAudioPlayer(contentsOf: url) else { return false }
            player.numberOfLoops = -1
            player.volume = 0
            player.prepareToPlay()
            list.append(player)
        }
        let start = list[0].deviceCurrentTime + 0.15
        for player in list { player.play(atTime: start) }
        stems = list
        applyIntensity(fade: 1.4)
        return true
    }

    private func stopStems() {
        if stems.isEmpty { return }
        let old = stems
        stems = []
        for player in old { player.setVolume(0, fadeDuration: 0.9) }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            for player in old { player.stop() }
        }
    }

    /// 0 = partida tranquila (solo la base), 0.5 = entra el ritmo, 1 = entra también la melodía.
    func setIntensity(_ value: Double) {
        let clamped = max(0, min(1, value))
        if abs(clamped - intensity) < 0.01 { return }
        intensity = clamped
        applyIntensity(fade: 1.2)
    }

    private func applyIntensity(fade: TimeInterval) {
        if stems.count != 3 { return }
        let master = masterMusic
        stems[0].setVolume(master, fadeDuration: fade)
        stems[1].setVolume(master * Float(min(1, intensity * 2)), fadeDuration: fade)
        stems[2].setVolume(master * Float(max(0, intensity * 2 - 1)), fadeDuration: fade)
    }

    private func pauseMusic() {
        music?.pause()
        for player in stems { player.pause() }
    }

    private func fadeOutMusic() {
        stopStems()
        guard let old = music else { return }
        old.setVolume(0, fadeDuration: 0.8)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) { old.stop() }
        music = nil
        musicTheme = nil
    }

    private func resumeMusic() {
        if !musicEnabled {
            fadeOutMusic()
            return
        }
        if let current = music {
            if !current.isPlaying { current.play() }
        } else if !stems.isEmpty {
            let start = stems[0].deviceCurrentTime + 0.1
            for player in stems where !player.isPlaying { player.play(atTime: start) }
        } else if let wanted = wantedTheme {
            setMusic(wanted)
        }
    }

    /// Pausa o reanuda la música (por ejemplo mientras se ve un anuncio).
    func setMusicPaused(_ paused: Bool) {
        if paused {
            pauseMusic()
        } else {
            resumeMusic()
        }
    }

    /// Baja la música un momento (por ejemplo mientras suena la fanfarria de un cofre legendario).
    func duckMusic(for seconds: Double) {
        music?.setVolume(musicVolume * 0.1, fadeDuration: 0.2)
        for player in stems { player.setVolume(musicVolume * 0.08, fadeDuration: 0.2) }
        duckTimer?.invalidate()
        duckTimer = Timer.scheduledTimer(withTimeInterval: seconds, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            self.music?.setVolume(self.masterMusic, fadeDuration: 0.6)
            self.applyIntensity(fade: 0.6)
        }
    }

    /// Compatibilidad con el código anterior: música del menú si "shouldPlay", si no silencio.
    func updateMusic(shouldPlay: Bool) {
        setMusic(shouldPlay ? (wantedTheme ?? .menu) : nil)
    }

    /// Se llama al cambiar el ajuste de música o de sonido en Ajustes.
    func settingsChanged() {
        if musicEnabled {
            if let wanted = wantedTheme { setMusic(wanted) }
            music?.volume = masterMusic
            applyIntensity(fade: 0.2)
        } else {
            fadeOutMusic()
        }
    }
}

/// Un tic suave en cada toque de la pantalla (los botones, las pestañas, las tarjetas...), sin tocar cada botón uno a uno.
final class GlobalTapSound: NSObject, UIGestureRecognizerDelegate {
    static let shared = GlobalTapSound()
    private var installed = false

    func install() {
        if installed { return }
        var found: UIWindow? = nil
        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            for window in windowScene.windows where window.isKeyWindow {
                found = window
            }
        }
        guard let window = found else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
                self?.install()
            }
            return
        }
        let tap = UITapGestureRecognizer(target: self, action: #selector(tapped))
        tap.cancelsTouchesInView = false
        tap.delaysTouchesBegan = false
        tap.delaysTouchesEnded = false
        tap.delegate = self
        window.addGestureRecognizer(tap)
        installed = true
    }

    @objc private func tapped() {
        SoundManager.shared.play(.tap, volume: 0.6)
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
}
