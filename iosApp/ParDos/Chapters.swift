import SwiftUI

/// Mundos del mapa: cada 20 niveles cambia el capítulo (y el cofre en Android). En Noche de brujas son los mundos embrujados.
struct ChapterTheme {
    let name: String
    let color: Color
    /// Nombre del dibujo del emblema ("chapter.d0"...).
    let emblem: String
    /// Tipo de paisaje: da nombre a los adornos del borde del camino ("prop.<paisaje>_a/b/c").
    let scenery: String
    /// Capítulo de noche (cielo oscuro, texto claro).
    let night: Bool
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
        ChapterTheme(name: loc("Jardín de Arena"), color: Color(hex: 0x6B9E86), emblem: "chapter.d0", scenery: "dunes", night: false),
        ChapterTheme(name: loc("Bosque Sereno"), color: Color(hex: 0x3F9468), emblem: "chapter.d1", scenery: "pines", night: false),
        ChapterTheme(name: loc("Orilla del Río"), color: Color(hex: 0x4E8FA6), emblem: "chapter.d2", scenery: "river", night: false),
        ChapterTheme(name: loc("Colinas de Té"), color: Color(hex: 0x8DB04A), emblem: "chapter.d3", scenery: "hills", night: false),
        ChapterTheme(name: loc("Faro en la Bruma"), color: Color(hex: 0x7A8FB5), emblem: "chapter.d4", scenery: "clouds", night: false),
        ChapterTheme(name: loc("Valle de Lavanda"), color: Color(hex: 0x8E6BD6), emblem: "chapter.d5", scenery: "lavender", night: false),
        ChapterTheme(name: loc("Mercado Nocturno"), color: Color(hex: 0xE0782F), emblem: "chapter.d6", scenery: "lanterns", night: false),
        ChapterTheme(name: loc("Pico Nevado"), color: Color(hex: 0x5B8FC4), emblem: "chapter.d7", scenery: "mountains", night: false),
        ChapterTheme(name: loc("Isla de Cerezos"), color: Color(hex: 0xE57A9A), emblem: "chapter.d8", scenery: "cherry", night: false),
        ChapterTheme(name: loc("Desierto Dorado"), color: Color(hex: 0xE0A93B), emblem: "chapter.d9", scenery: "gold_dunes", night: false),
        ChapterTheme(name: loc("Bahía Lunar"), color: Color(hex: 0x4B5BA8), emblem: "chapter.d10", scenery: "moon", night: false),
        ChapterTheme(name: loc("Templo de Bambú"), color: Color(hex: 0x3E9E7A), emblem: "chapter.d11", scenery: "bamboo", night: false)
    ]

    private static let night: [ChapterTheme] = [
        ChapterTheme(name: loc("Cementerio Sereno"), color: Color(hex: 0x7A56C0), emblem: "chapter.n0", scenery: "graveyard", night: true),
        ChapterTheme(name: loc("Bosque Encantado"), color: Color(hex: 0x2F8A7A), emblem: "chapter.n1", scenery: "dead_forest", night: true),
        ChapterTheme(name: loc("Pantano Brumoso"), color: Color(hex: 0x5E9C45), emblem: "chapter.n2", scenery: "river", night: true),
        ChapterTheme(name: loc("Calabazar"), color: Color(hex: 0xE8772E), emblem: "chapter.n3", scenery: "pumpkins", night: true),
        ChapterTheme(name: loc("Faro Fantasma"), color: Color(hex: 0x8E86C0), emblem: "chapter.n4", scenery: "clouds", night: true),
        ChapterTheme(name: loc("Mansión Embrujada"), color: Color(hex: 0x6A3FA0), emblem: "chapter.n5", scenery: "haunted", night: true),
        ChapterTheme(name: loc("Mercado de Brujas"), color: Color(hex: 0xE0782F), emblem: "chapter.n6", scenery: "lanterns", night: true),
        ChapterTheme(name: loc("Pico Aullante"), color: Color(hex: 0x5668A8), emblem: "chapter.n7", scenery: "mountains", night: true),
        ChapterTheme(name: loc("Árbol de las Almas"), color: Color(hex: 0x9B5FD0), emblem: "chapter.n8", scenery: "cherry", night: true),
        ChapterTheme(name: loc("Tumbas de Momias"), color: Color(hex: 0xD59A2B), emblem: "chapter.n9", scenery: "gold_dunes", night: true),
        ChapterTheme(name: loc("Bahía del Barco Fantasma"), color: Color(hex: 0x3F4FA0), emblem: "chapter.n10", scenery: "moon", night: true),
        ChapterTheme(name: loc("Templo Maldito"), color: Color(hex: 0xB33C5E), emblem: "chapter.n11", scenery: "bamboo", night: true)
    ]

    /// Adorno que acompaña a un nivel en el borde del camino (siempre el mismo para el mismo nivel). Nil si ese nivel va sin adorno.
    static func prop(level: Int, theme: ChapterTheme) -> String? {
        let pick = (level * 7 + level / 3) % 11
        switch pick {
        case 0, 1, 2:
            return "prop.\(theme.scenery)_a"
        case 3, 4:
            return "prop.\(theme.scenery)_b"
        case 5:
            return "prop.\(theme.scenery)_c"
        case 6:
            return theme.night ? "prop.mushroom" : "prop.flower_a"
        case 7:
            return theme.night ? "prop.stump" : "prop.bush_a"
        case 8:
            return level % 2 == 0 ? "prop.rock_a" : (theme.night ? "prop.lamp_post" : "prop.flower_b")
        default:
            return nil
        }
    }
}
