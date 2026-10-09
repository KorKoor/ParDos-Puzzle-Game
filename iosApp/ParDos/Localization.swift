import Foundation

/// Idioma de la app: el español es el idioma base (los textos del código están en español) y hay traducción al inglés
/// en en.lproj/Localizable.strings (se genera con iosApp/tools/make_localizable.py).
enum AppLanguage {
    /// "en" si el teléfono está en inglés, "es" en cualquier otro caso.
    static var code: String {
        let first = Locale.preferredLanguages.first ?? "es"
        return first.lowercased().hasPrefix("en") ? "en" : "es"
    }

    static var isEnglish: Bool {
        return code == "en"
    }
}

/// Traduce un texto en español al idioma del teléfono si hay traducción; si no, lo deja igual.
/// Sirve para textos que llegan como String (avisos, títulos, nombres) y no como literal de SwiftUI.
func loc(_ text: String) -> String {
    return NSLocalizedString(text, comment: "")
}
