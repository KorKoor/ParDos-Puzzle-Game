import Foundation
import UserNotifications

struct ReminderInfo: Decodable {
    let key: String
    let id: Int
    let title: String
    let body: String
    let delayMs: Int
}

/// Avisos del teléfono (notificaciones locales). El plan lo decide Kotlin (`ReminderPlanner`, el mismo que en Android).
final class Notifier {
    static let shared = Notifier()
    private let center = UNUserNotificationCenter.current()

    func isAuthorized(_ done: @escaping (Bool) -> Void) {
        center.getNotificationSettings { settings in
            let ok = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
            DispatchQueue.main.async { done(ok) }
        }
    }

    func requestPermission(_ done: @escaping (Bool) -> Void) {
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            DispatchQueue.main.async { done(granted) }
        }
    }

    func cancelAll() {
        center.removeAllPendingNotificationRequests()
    }

    /// Borra los avisos anteriores y programa los nuevos.
    func schedule(_ reminders: [ReminderInfo]) {
        center.removeAllPendingNotificationRequests()
        for reminder in reminders {
            let content = UNMutableNotificationContent()
            content.title = reminder.title
            content.body = reminder.body
            content.sound = UNNotificationSound.default
            let seconds = max(5.0, Double(reminder.delayMs) / 1000.0)
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: seconds, repeats: false)
            let request = UNNotificationRequest(identifier: reminder.key, content: content, trigger: trigger)
            center.add(request, withCompletionHandler: nil)
        }
    }
}
