import SwiftUI
import UIKit
import PhotosUI

// Avatar propio: un emoji de Apple, un sticker (Memoji, Genmoji o de Mensajes) o una foto, con movimiento y fondo a elegir.
// Se guarda solo en este iPhone (los amigos ven un avatar normal de la lista).

struct CustomAvatarData: Codable, Equatable {
    var enabled: Bool = false
    /// "emoji" o "image"
    var mode: String = "emoji"
    var emoji: String = ""
    /// none, float, breathe, wiggle, bounce, heartbeat, swing, shine
    var animation: String = "float"
    var background: Int = 0
    var imageVersion: Int = 0
}

let customAvatarAnimations: [(id: String, name: String, symbol: String)] = [
    ("none", "Quieto", "pause.circle"),
    ("float", "Flotar", "cloud"),
    ("breathe", "Respirar", "wind"),
    ("wiggle", "Bailar", "music.note"),
    ("bounce", "Rebotar", "arrow.up.circle"),
    ("heartbeat", "Latido", "heart"),
    ("swing", "Girar", "arrow.triangle.2.circlepath"),
    ("shine", "Brillar", "sparkles")
]

let customAvatarBackgrounds: [[UInt32]] = [
    [0xFFE6C9, 0xFFCB9A],
    [0xE3EEFF, 0xC9DCFF],
    [0xD9F2E3, 0xB5E3C8],
    [0xF0E0FF, 0xD9C2FF],
    [0xFFE0EE, 0xFFC4DD],
    [0xFFF3C4, 0xFFE28A],
    [0xD6ECFA, 0xB0DAF2],
    [0x3B2563, 0x241447],
    [0x2A2E63, 0x454B94],
    [0xFFD1C2, 0xE57A9A],
    [0xE9ECF3, 0xCDD3EB],
    [0x1E1136, 0x6B2D7A]
]

final class CustomAvatarStore: ObservableObject {
    static let shared = CustomAvatarStore()

    @Published private(set) var data: CustomAvatarData = CustomAvatarData()
    @Published private(set) var image: UIImage? = nil

    private let key = "custom_avatar_v1"

    private init() {
        if let raw = UserDefaults.standard.data(forKey: key),
           let decoded = try? JSONDecoder().decode(CustomAvatarData.self, from: raw) {
            data = decoded
        }
        if data.mode == "image" {
            image = CustomAvatarStore.loadImage(version: data.imageVersion)
        }
    }

    var hasContent: Bool {
        if data.mode == "image" { return image != nil }
        return !data.emoji.isEmpty
    }

    /// ¿Se usa el avatar propio en lugar del de la lista?
    var active: Bool {
        return data.enabled && hasContent
    }

    private func save() {
        if let raw = try? JSONEncoder().encode(data) {
            UserDefaults.standard.set(raw, forKey: key)
        }
    }

    func setEnabled(_ on: Bool) {
        data.enabled = on
        save()
    }

    func setAnimation(_ id: String) {
        data.animation = id
        save()
    }

    func setBackground(_ index: Int) {
        data.background = max(0, min(index, customAvatarBackgrounds.count - 1))
        save()
    }

    func setEmoji(_ text: String) {
        data.mode = "emoji"
        data.emoji = text
        data.enabled = true
        save()
    }

    func setImage(_ source: UIImage) {
        let prepared = CustomAvatarStore.prepare(source)
        data.imageVersion += 1
        if let png = prepared.pngData(), CustomAvatarStore.write(png, version: data.imageVersion) {
            image = prepared
            data.mode = "image"
            data.enabled = true
            save()
        }
    }

    func clear() {
        data = CustomAvatarData()
        image = nil
        save()
    }

    // MARK: archivos

    private static func fileURL(version: Int) -> URL? {
        guard let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return nil }
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        return base.appendingPathComponent("custom_avatar_\(version).png")
    }

    private static func write(_ png: Data, version: Int) -> Bool {
        guard let url = fileURL(version: version) else { return false }
        do {
            try png.write(to: url, options: .atomic)
            if version > 1, let old = fileURL(version: version - 1) {
                try? FileManager.default.removeItem(at: old)
            }
            return true
        } catch {
            return false
        }
    }

    private static func loadImage(version: Int) -> UIImage? {
        guard let url = fileURL(version: version), let raw = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: raw)
    }

    /// Recorta al cuadrado central y reduce a 384 px (conserva la transparencia de los stickers).
    private static func prepare(_ source: UIImage) -> UIImage {
        let w = source.size.width
        let h = source.size.height
        if w <= 0 || h <= 0 { return source }
        let side = min(w, h)
        let target: CGFloat = 384
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = false
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: target, height: target), format: format)
        return renderer.image { _ in
            let scale = target / side
            let drawW = w * scale
            let drawH = h * scale
            source.draw(in: CGRect(x: (target - drawW) / 2, y: (target - drawH) / 2, width: drawW, height: drawH))
        }
    }
}

// MARK: - Cara animada

struct CustomAvatarFace: View {
    @ObservedObject private var store = CustomAvatarStore.shared
    @ObservedObject private var power = PowerMonitor.shared
    var size: CGFloat = 48
    var animated: Bool = true
    /// Para previsualizar sin guardar.
    var overrideEmoji: String? = nil

    private var colors: [Color] {
        let index = max(0, min(store.data.background, customAvatarBackgrounds.count - 1))
        return customAvatarBackgrounds[index].map { Color(hex: $0) }
    }

    var body: some View {
        let style = store.data.animation
        let moving = animated && style != "none" && power.decorativeMotion
        return ZStack {
            Circle().fill(LinearGradient(colors: colors, startPoint: .top, endPoint: .bottom))
            if moving {
                TimelineView(.animation(minimumInterval: power.frameInterval, paused: false)) { timeline in
                    content(time: timeline.date.timeIntervalSinceReferenceDate, style: style)
                }
            } else {
                content(time: 0.7, style: "none")
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }

    @ViewBuilder
    private func face() -> some View {
        if store.data.mode == "image", let picture = store.image {
            Image(uiImage: picture)
                .resizable()
                .scaledToFit()
                .frame(width: size * 0.9, height: size * 0.9)
        } else {
            Text(overrideEmoji ?? store.data.emoji)
                .font(.system(size: size * 0.58))
                .minimumScaleFactor(0.5)
        }
    }

    private func content(time t: Double, style: String) -> some View {
        var dy: CGFloat = 0
        var scale: CGFloat = 1
        var scaleY: CGFloat = 1
        var angle: Double = 0
        var swing: Double = 0
        switch style {
        case "float":
            dy = CGFloat(sin(t * 1.6)) * size * 0.04
        case "breathe":
            scale = 1 + 0.045 * CGFloat(sin(t * 2.0))
        case "wiggle":
            angle = 7 * sin(t * 3.2)
        case "bounce":
            let hop = abs(sin(t * 3.0))
            dy = -CGFloat(hop) * size * 0.1
            scaleY = 1 - 0.06 * CGFloat(1 - hop)
        case "heartbeat":
            let beat = max(0, sin(t * 4.5))
            scale = 1 + 0.11 * CGFloat(pow(beat, 6))
        case "swing":
            swing = 20 * sin(t * 1.8)
        default:
            break
        }
        return face()
            .scaleEffect(x: scale, y: scale * scaleY)
            .rotationEffect(.degrees(angle))
            .rotation3DEffect(.degrees(swing), axis: (x: 0, y: 1, z: 0), perspective: 0.6)
            .offset(y: dy + size * 0.02)
            .overlay(shine(time: t, style: style))
    }

    @ViewBuilder
    private func shine(time t: Double, style: String) -> some View {
        if style == "shine" {
            let phase = CGFloat((t * 0.35).truncatingRemainder(dividingBy: 1.0))
            LinearGradient(
                colors: [Color.white.opacity(0), Color.white.opacity(0.55), Color.white.opacity(0)],
                startPoint: .leading,
                endPoint: .trailing
            )
            .frame(width: size * 0.5, height: size * 1.4)
            .rotationEffect(.degrees(20))
            .offset(x: (phase * 2.2 - 1.1) * size)
            .blendMode(.plusLighter)
            .allowsHitTesting(false)
        }
    }
}

// MARK: - Entrada de emojis y stickers

/// Campo invisible que abre el teclado de emojis y recibe lo que se toque: un emoji (texto) o un sticker/Memoji (imagen).
final class EmojiTextView: UITextView {
    var onEmoji: ((String) -> Void)?
    var onImage: ((UIImage) -> Void)?

    override var textInputContextIdentifier: String? { return "" }

    override var textInputMode: UITextInputMode? {
        for mode in UITextInputMode.activeInputModes where mode.primaryLanguage == "emoji" {
            return mode
        }
        return super.textInputMode
    }

    override func paste(_ sender: Any?) {
        if let picture = UIPasteboard.general.image {
            onImage?(picture)
            return
        }
        super.paste(sender)
    }

    func harvest() {
        var picture: UIImage? = nil
        attributedText.enumerateAttribute(.attachment, in: NSRange(location: 0, length: attributedText.length), options: []) { value, _, stop in
            guard let attachment = value as? NSTextAttachment else { return }
            if let direct = attachment.image {
                picture = direct
            } else if let raw = attachment.fileWrapper?.regularFileContents, let decoded = UIImage(data: raw) {
                picture = decoded
            } else if let raw = attachment.contents, let decoded = UIImage(data: raw) {
                picture = decoded
            }
            if picture != nil { stop.pointee = true }
        }
        if let found = picture {
            text = ""
            onImage?(found)
            return
        }
        let plain = text ?? ""
        if let first = plain.first(where: { $0.isEmoji }) {
            text = ""
            onEmoji?(String(first))
        }
    }
}

private extension Character {
    var isEmoji: Bool {
        guard let scalar = unicodeScalars.first else { return false }
        if unicodeScalars.count > 1 { return scalar.properties.isEmoji || unicodeScalars.contains(where: { $0.properties.isEmojiPresentation }) }
        return scalar.properties.isEmojiPresentation
    }
}

struct EmojiKeyboardField: UIViewRepresentable {
    @Binding var focus: Bool
    var onEmoji: (String) -> Void
    var onImage: (UIImage) -> Void

    func makeUIView(context: Context) -> EmojiTextView {
        let view = EmojiTextView()
        view.backgroundColor = UIColor.clear
        view.textColor = UIColor.clear
        view.tintColor = UIColor.clear
        view.allowsEditingTextAttributes = true
        view.autocorrectionType = .no
        view.delegate = context.coordinator
        view.onEmoji = onEmoji
        view.onImage = onImage
        return view
    }

    func updateUIView(_ view: EmojiTextView, context: Context) {
        view.onEmoji = onEmoji
        view.onImage = onImage
        if focus && !view.isFirstResponder {
            DispatchQueue.main.async { view.becomeFirstResponder() }
        } else if !focus && view.isFirstResponder {
            DispatchQueue.main.async { view.resignFirstResponder() }
        }
    }

    func makeCoordinator() -> Coordinator {
        return Coordinator()
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        func textViewDidChange(_ textView: UITextView) {
            (textView as? EmojiTextView)?.harvest()
        }
    }
}

// MARK: - Fotos

struct PhotoPicker: UIViewControllerRepresentable {
    var onPick: (UIImage) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ controller: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        return Coordinator(onPick: onPick)
    }

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let onPick: (UIImage) -> Void

        init(onPick: @escaping (UIImage) -> Void) {
            self.onPick = onPick
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picker.dismiss(animated: true)
            guard let provider = results.first?.itemProvider, provider.canLoadObject(ofClass: UIImage.self) else { return }
            provider.loadObject(ofClass: UIImage.self) { object, _ in
                if let picture = object as? UIImage {
                    DispatchQueue.main.async { self.onPick(picture) }
                }
            }
        }
    }
}

// MARK: - Hoja para crear el avatar propio

struct CustomAvatarSheet: View {
    @ObservedObject private var store = CustomAvatarStore.shared
    @State private var keyboard = false
    @State private var showPhotos = false
    @State private var pasteMessage: String? = nil
    var onClose: () -> Void = {}

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                Text("Mi avatar")
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                preview
                sources
                animationPicker
                backgroundPicker
                toggleRow
                Button(action: onClose) {
                    Text("LISTO")
                        .font(.system(size: 16, weight: .black, design: .rounded))
                        .kerning(2)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Theme.accent))
                }
                Spacer(minLength: 20)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.cream.ignoresSafeArea())
        .overlay(
            EmojiKeyboardField(
                focus: $keyboard,
                onEmoji: { text in
                    store.setEmoji(text)
                    Haptics.select()
                },
                onImage: { picture in
                    store.setImage(picture)
                    keyboard = false
                    Haptics.success()
                }
            )
            .frame(width: 1, height: 1)
            .opacity(0.01),
            alignment: .topLeading
        )
        .sheet(isPresented: $showPhotos) {
            PhotoPicker { picture in
                store.setImage(picture)
                Haptics.success()
            }
        }
    }

    private var preview: some View {
        ZStack {
            if store.hasContent {
                CustomAvatarFace(size: 132)
            } else {
                Circle()
                    .fill(Theme.ink.opacity(0.08))
                    .frame(width: 132, height: 132)
                    .overlay(
                        Image(systemName: "face.smiling")
                            .font(.system(size: 52))
                            .foregroundColor(Theme.ink.opacity(0.3))
                    )
            }
        }
        .shadow(color: Theme.ink.opacity(0.15), radius: 10, x: 0, y: 6)
    }

    private var sources: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: "Elige tu sticker")
            Text("Toca el botón y, en el teclado de emojis, escoge un emoji o uno de tus stickers de Memoji. También puedes usar una foto o pegar una imagen copiada.")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.55))
            HStack(spacing: 10) {
                sourceButton(symbol: "face.smiling", title: "Emoji o Memoji") {
                    keyboard = true
                }
                sourceButton(symbol: "photo", title: "Foto") {
                    showPhotos = true
                }
                sourceButton(symbol: "doc.on.clipboard", title: "Pegar") {
                    pasteImage()
                }
            }
            if let message = pasteMessage {
                Text(loc(message))
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.energy)
            }
        }
    }

    private func sourceButton(symbol: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: symbol)
                    .font(.system(size: 22, weight: .bold))
                Text(loc(title))
                    .font(.system(size: 11, weight: .heavy, design: .rounded))
            }
            .foregroundColor(Theme.ink)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .card(radius: 16)
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func pasteImage() {
        if let picture = UIPasteboard.general.image {
            store.setImage(picture)
            pasteMessage = nil
            Haptics.success()
        } else {
            pasteMessage = "No hay ninguna imagen copiada. Mantén pulsado un sticker y elige Copiar."
        }
    }

    private var animationPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: "Movimiento")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(customAvatarAnimations, id: \.id) { item in
                        let selected = store.data.animation == item.id
                        Button(action: { store.setAnimation(item.id) }) {
                            HStack(spacing: 5) {
                                Image(systemName: item.symbol)
                                Text(item.name)
                            }
                            .font(.system(size: 12, weight: .heavy, design: .rounded))
                            .foregroundColor(selected ? .white : Theme.ink)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 9)
                            .background(Capsule().fill(selected ? Theme.accent : Color.white))
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }

    private var backgroundPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: "Fondo")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 6), spacing: 10) {
                ForEach(0..<customAvatarBackgrounds.count, id: \.self) { index in
                    let pair = customAvatarBackgrounds[index].map { Color(hex: $0) }
                    Button(action: { store.setBackground(index) }) {
                        Circle()
                            .fill(LinearGradient(colors: pair, startPoint: .top, endPoint: .bottom))
                            .frame(height: 40)
                            .overlay(Circle().stroke(store.data.background == index ? Theme.ink : Color.white, lineWidth: 3))
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
        }
    }

    private var toggleRow: some View {
        VStack(spacing: 10) {
            Toggle(isOn: Binding(get: { store.data.enabled }, set: { store.setEnabled($0) })) {
                Text("Usar mi sticker como avatar")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink)
            }
            .padding(14)
            .card(radius: 16)
            if store.hasContent {
                Button(action: { store.clear() }) {
                    Text("Quitar mi sticker")
                        .font(.system(size: 12, weight: .heavy, design: .rounded))
                        .foregroundColor(Color(hex: 0xE0475B))
                }
            }
        }
    }
}
