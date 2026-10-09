import SwiftUI
import UIKit

/// El selector de compartir del sistema (WhatsApp, Mensajes, Instagram...).
struct ShareSheet: UIViewControllerRepresentable {
    let text: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        return UIActivityViewController(activityItems: [text], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

struct ShareItem: Identifiable {
    let text: String
    var id: String { text }
}
