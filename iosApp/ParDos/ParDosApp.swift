import SwiftUI

@main
struct ParDosApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
        }
    }
}

struct RootView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var splashDone = false

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            SeasonBackdrop()
            content
                .frame(maxWidth: 640)
            if let message = model.toast {
                ToastView(text: message)
                    .zIndex(30)
            }
            CelebrationOverlay()
            AchievementBanner()
            if !splashDone {
                SplashView()
                    .transition(.opacity)
                    .zIndex(40)
            }
        }
        .sheet(item: $model.sheet) { sheet in
            sheetContent(sheet)
                .environmentObject(model)
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active { model.appBecameActive() }
            if phase == .background { model.rescheduleReminders() }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                withAnimation(.easeOut(duration: 0.4)) {
                    splashDone = true
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch model.screen {
        case .main:
            MainShell()
        case .game:
            GameView()
        }
    }

    @ViewBuilder
    private func sheetContent(_ sheet: Sheet) -> some View {
        switch sheet {
        case .level(let card):
            LevelPreviewSheet(card: card)
        case .season:
            SeasonSheet()
        case .missions:
            MissionsSheet()
        case .wheel:
            WheelSheet()
        case .league:
            LeagueSheet()
        case .settings:
            SettingsView()
        case .lowFunds:
            LowFundsSheet()
        case .custom:
            CustomGameSheet()
        case .records:
            RecordsSheet()
        case .achievements:
            AchievementsSheet()
        case .prestige:
            PrestigeSheet()
        case .studio:
            StudioSheet()
        case .remote:
            RemoteSheet()
        case .backup:
            BackupSheet()
        case .friends:
            FriendsSheet()
        }
    }
}
