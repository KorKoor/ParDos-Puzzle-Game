import SwiftUI

/// Después de superar el nivel 2, una sola vez: ponerle nombre al jugador y elegir un avatar.
struct ProfileSetupView: View {
    @EnvironmentObject var model: AppModel
    @State private var name: String = ""
    @State private var avatar: Int = 1

    var body: some View {
        PopupFrame(title: "¿Cómo te llamas?") {
            AvatarView(id: avatar, size: 84, mine: true)
                .overlay(Circle().stroke(Theme.gold, lineWidth: 3))
            TextField("Tu nombre", text: $name)
                .font(.system(size: 18, weight: .black, design: .rounded))
                .multilineTextAlignment(.center)
                .padding(10)
                .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Theme.ink.opacity(0.07)))
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(1..<11, id: \.self) { id in
                    Button(action: { avatar = id }) {
                        AvatarView(id: id, size: 44)
                            .overlay(Circle().stroke(avatar == id ? Theme.accent : Color.clear, lineWidth: 3).padding(-2))
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            BigButton(title: "LISTO") {
                model.finishProfileSetup(name: name, avatar: avatar)
            }
            Text("Podrás cambiarlos cuando quieras en tu perfil.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.5))
        }
        .onAppear {
            name = model.state?.name == "Jugador Zen" ? "" : (model.state?.name ?? "")
            avatar = model.state?.avatar ?? 1
        }
    }
}

extension AppModel {
    /// Tras ganar el nivel 2 (o más), si todavía no eligió nombre ni avatar, se le pregunta una vez.
    func maybeAskForProfile(_ s: BoardSnap) {
        if defaults.bool(forKey: "profile_setup_done") { return }
        if s.daily || s.level < 2 { return }
        defaults.set(true, forKey: "profile_setup_done")
        push(.profileSetup)
    }

    func finishProfileSetup(name: String, avatar: Int) {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clean.isEmpty { setName(clean) }
        setAvatar(avatar)
        dismissCelebration()
    }
}
