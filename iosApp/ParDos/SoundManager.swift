import AVFoundation
import Foundation

/// Sonidos y música (los mismos archivos que en Android). Respeta el interruptor de silencio del iPhone y los ajustes de la app.
final class SoundManager {
    static let shared = SoundManager()

    private var players: [String: AVAudioPlayer] = [:]
    private var music: AVAudioPlayer?

    private init() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.ambient, mode: .default, options: [])
        try? session.setActive(true)
    }

    private var soundEnabled: Bool {
        (UserDefaults.standard.object(forKey: "sound_on") as? Bool) ?? true
    }

    private var musicEnabled: Bool {
        (UserDefaults.standard.object(forKey: "music_on") as? Bool) ?? true
    }

    func play(_ name: String, volume: Float = 1.0) {
        guard soundEnabled, let player = player(for: name) else { return }
        player.currentTime = 0
        player.volume = volume
        player.play()
    }

    private func player(for name: String) -> AVAudioPlayer? {
        if let cached = players[name] { return cached }
        guard let url = Bundle.main.url(forResource: name, withExtension: "mp3"),
              let fresh = try? AVAudioPlayer(contentsOf: url) else { return nil }
        fresh.prepareToPlay()
        players[name] = fresh
        return fresh
    }

    /// Música del menú y del mapa: suena si está activada y se calla durante la partida.
    func updateMusic(shouldPlay: Bool) {
        if shouldPlay && musicEnabled {
            if music == nil {
                if let url = Bundle.main.url(forResource: "theme_song", withExtension: "mp3") {
                    music = try? AVAudioPlayer(contentsOf: url)
                    music?.numberOfLoops = -1
                    music?.volume = 0.3
                    music?.prepareToPlay()
                }
            }
            if music?.isPlaying == false { music?.play() }
        } else {
            music?.pause()
        }
    }
}
