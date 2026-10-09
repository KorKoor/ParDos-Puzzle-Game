import Foundation

// Lo que la lógica compartida (Kotlin) entrega a la interfaz: texto JSON que aquí se convierte en estas estructuras.

struct TileSnap: Decodable, Identifiable {
    let id: String
    let v: Int
    let r: Int
    let c: Int
    let new: Bool
    let merged: Bool
}

struct BoardSnap: Decodable {
    let level: Int
    let daily: Bool
    let title: String
    let kind: String
    let kindLabel: String
    let rule: String
    let tip: String?
    let goal: String
    let size: Int
    let progress: Double
    let score: Int
    let moves: Int
    let movesLeft: Int?
    let timeLeftMs: Int?
    let status: String
    let lostReason: String
    let stars: Int
    let tiles: [TileSnap]
    let stones: [[Int]]
    let storm: [[Int]]
    let twist: String
    let twistHint: String
    let blocked: String
    let phase: Int
    let phaseTitle: String
    let chips: [String]
    let coach: String?
    let coachKind: String?
    let coachDir: Int?
    let coachCells: [[Int]]?
    let coachDone: Int?
    let coachNeeded: Int?
    let tutorialDone: Bool
    let canUndo: Bool
}

struct GuideHint: Decodable {
    let dir: Int
    let cells: [[Int]]
}

struct LevelCard: Decodable, Identifiable {
    let id: Int
    let title: String
    let kind: String
    let kindLabel: String
    let rule: String
    let goal: String
    let size: Int
    let boss: Bool
    let moveLimit: Int?
    let timeMs: Int?
    let chips: [String]
    let tip: String?
    let threeStars: String
}

func decodeJSON<T: Decodable>(_ type: T.Type, _ json: String) -> T? {
    guard let data = json.data(using: .utf8) else { return nil }
    return try? JSONDecoder().decode(T.self, from: data)
}
