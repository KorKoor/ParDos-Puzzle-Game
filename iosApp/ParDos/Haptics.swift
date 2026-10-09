import UIKit
import CoreHaptics

/// Vibraciones suaves del teléfono (el Taptic Engine) que acompañan a los sonidos. Se apagan en Ajustes.
enum Haptics {
    private static var enabled: Bool {
        return (UserDefaults.standard.object(forKey: "haptics_on") as? Bool) ?? true
    }

    private static let lightGen = UIImpactFeedbackGenerator(style: .light)
    private static let mediumGen = UIImpactFeedbackGenerator(style: .medium)
    private static let heavyGen = UIImpactFeedbackGenerator(style: .heavy)
    private static let softGen = UIImpactFeedbackGenerator(style: .soft)
    private static let rigidGen = UIImpactFeedbackGenerator(style: .rigid)
    private static let selectGen = UISelectionFeedbackGenerator()
    private static let noticeGen = UINotificationFeedbackGenerator()

    /// Prepara el motor justo antes de jugar para que la primera vibración no llegue tarde.
    static func warmUp() {
        if !enabled { return }
        lightGen.prepare()
        mediumGen.prepare()
    }

    static func tap() {
        if !enabled { return }
        lightGen.impactOccurred(intensity: 0.7)
    }

    static func soft() {
        if !enabled { return }
        softGen.impactOccurred(intensity: 0.6)
    }

    static func select() {
        if !enabled { return }
        selectGen.selectionChanged()
    }

    static func medium() {
        if !enabled { return }
        mediumGen.impactOccurred()
    }

    static func heavy() {
        if !enabled { return }
        heavyGen.impactOccurred()
    }

    static func rigid() {
        if !enabled { return }
        rigidGen.impactOccurred(intensity: 0.8)
    }

    static func success() {
        if !enabled { return }
        noticeGen.notificationOccurred(.success)
    }

    static func warning() {
        if !enabled { return }
        noticeGen.notificationOccurred(.warning)
    }

    static func error() {
        if !enabled { return }
        noticeGen.notificationOccurred(.error)
    }

    /// Fusión de fichas: más fuerte cuanto más grande es la ficha (nivel 1 a 4).
    static func merge(level: Int) {
        if !enabled { return }
        switch level {
        case ...1: softGen.impactOccurred(intensity: 0.55)
        case 2: lightGen.impactOccurred(intensity: 0.8)
        case 3: mediumGen.impactOccurred(intensity: 0.9)
        default: heavyGen.impactOccurred()
        }
    }

    private static var engine: CHHapticEngine? = nil

    private static func ensureEngine() -> CHHapticEngine? {
        if !CHHapticEngine.capabilitiesForHardware().supportsHaptics { return nil }
        if let existing = engine { return existing }
        do {
            let created = try CHHapticEngine()
            created.isAutoShutdownEnabled = true
            created.resetHandler = {
                try? engine?.start()
            }
            try created.start()
            engine = created
            return created
        } catch {
            return nil
        }
    }

    /// Una secuencia de toques (tiempo en segundos, fuerza 0 a 1, nitidez 0 a 1) con el motor de vibración fino del iPhone.
    @discardableResult
    static func pattern(_ taps: [(t: Double, i: Float, s: Float)]) -> Bool {
        guard enabled, let engine = ensureEngine() else { return false }
        var events: [CHHapticEvent] = []
        for tap in taps {
            let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: tap.i)
            let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: tap.s)
            events.append(CHHapticEvent(eventType: .hapticTransient, parameters: [intensity, sharpness], relativeTime: tap.t))
        }
        do {
            let built = try CHHapticPattern(events: events, parameters: [])
            let player = try engine.makePlayer(with: built)
            try player.start(atTime: CHHapticTimeImmediate)
            return true
        } catch {
            return false
        }
    }

    /// Cofre que se abre: toques que se aceleran, un golpe grande y destellos.
    static func chestOpen() {
        if !enabled { return }
        var taps: [(t: Double, i: Float, s: Float)] = []
        var time = 0.0
        var gap = 0.16
        var level: Float = 0.25
        while time < 0.9 {
            taps.append((t: time, i: level, s: 0.4))
            time += gap
            gap = max(0.04, gap * 0.82)
            level = min(0.8, level + 0.07)
        }
        taps.append((t: time + 0.05, i: 1.0, s: 0.2))
        for k in 1...4 {
            taps.append((t: time + 0.2 + Double(k) * 0.07, i: 0.5, s: 0.9))
        }
        if pattern(taps) { return }
        heavyGen.impactOccurred()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) {
            noticeGen.notificationOccurred(.success)
        }
    }

    /// Celebración corta (subir de nivel, logro): una escalera de toques que sube.
    static func celebrate() {
        if !enabled { return }
        var taps: [(t: Double, i: Float, s: Float)] = []
        for k in 0..<6 {
            taps.append((t: Double(k) * 0.07, i: 0.35 + Float(k) * 0.1, s: 0.3 + Float(k) * 0.1))
        }
        if pattern(taps) { return }
        noticeGen.notificationOccurred(.success)
    }

    /// Tictac de ruleta o contador: muy ligero.
    static func tick() {
        if !enabled { return }
        selectGen.selectionChanged()
    }
}
