import SwiftUI
import UIKit

/// El selector de compartir del sistema (WhatsApp, Mensajes, Instagram...).
struct ShareSheet: UIViewControllerRepresentable {
    let text: String
    var image: UIImage? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        var items: [Any] = [text]
        if let image = image { items.insert(image, at: 0) }
        return UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

struct ShareItem: Identifiable {
    let text: String
    var image: UIImage? = nil
    var id: String { text }
}
