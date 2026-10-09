import SwiftUI
import UIKit

/// Pregunta (después de la primera victoria) si quiere avisos, explicando qué se le va a avisar.
struct NotifPrimerView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        PopupFrame(title: "¿Quieres avisos?") {
            Image(systemName: "bell.badge.fill")
                .font(.system(size: 46))
                .foregroundColor(Theme.gold)
            VStack(alignment: .leading, spacing: 6) {
                line("Tu cofre gratis está listo")
                line("Tu regalo y misiones del día")
                line("Tu racha en peligro")
                line("Fiestas con skins exclusivas")
            }
            Text("Pocos avisos, nunca de madrugada. Puedes apagarlos en Ajustes.")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.5))
                .multilineTextAlignment(.center)
            BigButton(title: "SÍ, AVÍSAME") {
                model.answerNotificationPrimer(accept: true)
            }
            Button(action: { model.answerNotificationPrimer(accept: false) }) {
                Text("Ahora no")
                    .font(.system(size: 14, weight: .heavy, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.55))
            }
        }
    }

    private func line(_ text: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "checkmark.circle.fill").foregroundColor(Theme.accent)
            Text(text).font(.system(size: 13, weight: .bold)).foregroundColor(Theme.ink)
        }
    }
}

extension AppModel {
    var notificationsEnabled: Bool {
        (defaults.object(forKey: "notif_on") as? Bool) ?? true
    }

    func setNotifications(_ on: Bool) {
        defaults.set(on, forKey: "notif_on")
        if on {
            Notifier.shared.requestPermission { [weak self] granted in
                if granted { self?.rescheduleReminders() }
            }
        } else {
            Notifier.shared.cancelAll()
        }
    }

    /// Vuelve a programar los avisos con lo último que pasó (se llama al salir de la app).
    func rescheduleReminders() {
        guard notificationsEnabled else { return }
        Notifier.shared.isAuthorized { [weak self] ok in
            guard let self = self, ok else { return }
            self.tickClock()
            if let list = decodeJSON([ReminderInfo].self, self.meta.reminders()) {
                Notifier.shared.schedule(list)
            }
            UIApplication.shared.applicationIconBadgeNumber = self.state?.badges ?? 0
        }
    }

    /// Tras la primera victoria (y pocas veces más) se pregunta si quiere avisos.
    func maybeAskForNotifications() {
        Notifier.shared.isAuthorized { [weak self] granted in
            guard let self = self else { return }
            self.tickClock()
            if self.meta.shouldAskNotifications(granted: granted, enabled: self.notificationsEnabled) {
                self.push(.notifPrimer)
            }
        }
    }

    func answerNotificationPrimer(accept: Bool) {
        meta.noteNotificationAsked()
        persist()
        dismissCelebration()
        if accept {
            Notifier.shared.requestPermission { [weak self] granted in
                if granted { self?.rescheduleReminders() }
            }
        }
    }
}
