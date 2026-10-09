import SwiftUI
import UIKit

/// Para VoiceOver: el tablero se describe con palabras, se puede mover con acciones del rotor y se anuncia cada jugada.
extension AppModel {
    func boardDescription(_ snap: BoardSnap) -> String {
        var text = "Tablero de \(snap.size) por \(snap.size). Meta: \(snap.goal). "
        text += "Movimientos: \(snap.moves). Puntos: \(snap.score). "
        if let left = snap.movesLeft { text += "Te quedan \(left) movimientos. " }
        if let ms = snap.timeLeftMs { text += "Tiempo restante: \(max(0, ms / 1000)) segundos. " }
        if snap.tiles.isEmpty {
            text += "No hay fichas."
        } else {
            let ordered = snap.tiles.sorted { ($0.r, $0.c) < ($1.r, $1.c) }
            var parts: [String] = []
            for tile in ordered {
                parts.append("\(tile.v) en fila \(tile.r + 1), columna \(tile.c + 1)")
            }
            text += "Fichas: " + parts.joined(separator: "; ") + "."
        }
        if !snap.stones.isEmpty { text += " Hay \(snap.stones.count + snap.storm.count) piedras que no se mueven." }
        return text
    }

    /// Resumen corto que se lee después de cada deslizamiento.
    func moveAnnouncement(_ snap: BoardSnap) -> String {
        if snap.status == "won" { return "¡Nivel superado! \(snap.stars) estrellas." }
        if snap.status == "lost" { return "Partida terminada. \(snap.score) puntos." }
        var biggest = 0
        for tile in snap.tiles { biggest = max(biggest, tile.v) }
        return "Ficha más alta \(biggest). \(snap.tiles.count) fichas. \(snap.score) puntos."
    }

    func announce(_ text: String) {
        if UIAccessibility.isVoiceOverRunning {
            UIAccessibility.post(notification: .announcement, argument: text)
        }
    }
}
