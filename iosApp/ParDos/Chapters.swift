import SwiftUI

/// Mundos del mapa: cada 20 niveles cambia el capítulo (y el cofre en Android). En Noche de brujas son los mundos embrujados.
struct ChapterTheme {
    let name: String
    let color: Color
}

enum Chapters {
    static let size = 20

    static func index(of level: Int) -> Int {
        return (level - 1) / size
    }

    static func theme(_ chapter: Int) -> ChapterTheme {
        let list = Theme.halloween ? night : day
        return list[chapter % list.count]
    }

    private static let day: [ChapterTheme] = [
        ChapterTheme(name: "Jardín de Arena", color: Color(hex: 0x6B9E86)),
        ChapterTheme(name: "Bosque Sereno", color: Color(hex: 0x3F9468)),
        ChapterTheme(name: "Orilla del Río", color: Color(hex: 0x4E8FA6)),
        ChapterTheme(name: "Colinas de Té", color: Color(hex: 0x8DB04A)),
        ChapterTheme(name: "Faro en la Bruma", color: Color(hex: 0x7A8FB5)),
        ChapterTheme(name: "Valle de Lavanda", color: Color(hex: 0x8E6BD6)),
        ChapterTheme(name: "Mercado Nocturno", color: Color(hex: 0xE0782F)),
        ChapterTheme(name: "Pico Nevado", color: Color(hex: 0x5B8FC4)),
        ChapterTheme(name: "Isla de Cerezos", color: Color(hex: 0xE57A9A)),
        ChapterTheme(name: "Desierto Dorado", color: Color(hex: 0xE0A93B)),
        ChapterTheme(name: "Bahía Lunar", color: Color(hex: 0x4B5BA8)),
        ChapterTheme(name: "Templo de Bambú", color: Color(hex: 0x3E9E7A))
    ]

    private static let night: [ChapterTheme] = [
        ChapterTheme(name: "Cementerio Sereno", color: Color(hex: 0x7A56C0)),
        ChapterTheme(name: "Bosque Encantado", color: Color(hex: 0x2F8A7A)),
        ChapterTheme(name: "Pantano Brumoso", color: Color(hex: 0x5E9C45)),
        ChapterTheme(name: "Calabazar", color: Color(hex: 0xE8772E)),
        ChapterTheme(name: "Faro Fantasma", color: Color(hex: 0x8E86C0)),
        ChapterTheme(name: "Mansión Embrujada", color: Color(hex: 0x6A3FA0)),
        ChapterTheme(name: "Mercado de Brujas", color: Color(hex: 0xE0782F)),
        ChapterTheme(name: "Pico Aullante", color: Color(hex: 0x5668A8)),
        ChapterTheme(name: "Árbol de las Almas", color: Color(hex: 0x9B5FD0)),
        ChapterTheme(name: "Tumbas de Momias", color: Color(hex: 0xD59A2B)),
        ChapterTheme(name: "Bahía del Barco Fantasma", color: Color(hex: 0x3F4FA0)),
        ChapterTheme(name: "Templo Maldito", color: Color(hex: 0xB33C5E))
    ]
}
