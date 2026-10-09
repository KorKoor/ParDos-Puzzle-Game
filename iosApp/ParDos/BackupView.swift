import SwiftUI
import UIKit

/// Copia de seguridad del progreso como texto: sirve para no perder nada al reinstalar la app (con Sideloadly, cada 7 días).
struct BackupSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var sharing: ShareItem?
    @State private var pasted: String = ""
    @State private var message: String?
    @State private var confirmRestore = false

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                Text("Copia de seguridad")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text("Tu progreso vive solo en este iPhone. Guarda una copia (en Notas, WhatsApp, correo…) y pégala si reinstalas la app.")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.55))
                    .multilineTextAlignment(.center)
                VStack(spacing: 10) {
                    Text("GUARDAR")
                        .font(.system(size: 10, weight: .heavy)).kerning(2).foregroundColor(Theme.ink.opacity(0.4))
                    BigButton(title: "COMPARTIR MI COPIA") {
                        sharing = ShareItem(text: "Copia de ParDos (no la borres):\n" + model.exportBackup())
                    }
                    Button(action: {
                        UIPasteboard.general.string = model.exportBackup()
                        message = "Copia guardada en el portapapeles"
                    }) {
                        Text("Copiar al portapapeles")
                            .font(.system(size: 13, weight: .heavy, design: .rounded))
                            .foregroundColor(Theme.accent)
                    }
                }
                .padding(14)
                .card()
                VStack(spacing: 10) {
                    Text("RESTAURAR")
                        .font(.system(size: 10, weight: .heavy)).kerning(2).foregroundColor(Theme.ink.opacity(0.4))
                    TextEditor(text: $pasted)
                        .font(.system(size: 11, design: .monospaced))
                        .frame(height: 110)
                        .padding(6)
                        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Theme.ink.opacity(0.06)))
                    HStack(spacing: 10) {
                        Button(action: { pasted = UIPasteboard.general.string ?? "" }) {
                            Text("Pegar")
                                .font(.system(size: 13, weight: .heavy, design: .rounded))
                                .foregroundColor(Theme.accent)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 9)
                                .background(Capsule().fill(Theme.accent.opacity(0.14)))
                        }
                        Button(action: { confirmRestore = true }) {
                            Text("Restaurar")
                                .font(.system(size: 13, weight: .heavy, design: .rounded))
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 9)
                                .background(Capsule().fill(Theme.energy))
                        }
                        Spacer()
                    }
                }
                .padding(14)
                .card()
                if let message = message {
                    Text(message)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Theme.accent)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 30)
        }
        .background(Theme.cream.ignoresSafeArea())
        .sheet(item: $sharing) { item in
            ShareSheet(text: item.text)
        }
        .alert(isPresented: $confirmRestore) {
            Alert(
                title: Text("¿Reemplazar tu progreso?"),
                message: Text("Se cambia lo que tienes ahora por la copia que pegaste."),
                primaryButton: .destructive(Text("Restaurar")) {
                    let ok = model.restoreBackup(pasted)
                    message = ok ? "¡Listo! Progreso restaurado." : "No encontré una copia válida en ese texto."
                },
                secondaryButton: .cancel(Text("Cancelar"))
            )
        }
    }
}

extension AppModel {
    func exportBackup() -> String {
        tickClock()
        return meta.exportBackup()
    }

    func restoreBackup(_ text: String) -> Bool {
        tickClock()
        let json = meta.importBackup(text: text)
        guard let result = decodeJSON(ActionResult.self, json), result.ok else { return false }
        persist()
        defaults.set(true, forKey: "meta_migrated")
        reloadSkinCatalog()
        refreshState()
        return true
    }
}
