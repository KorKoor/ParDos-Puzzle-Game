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
    @State private var splashDone = false

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            SeasonBackdrop()
            content
            if !splashDone {
                SplashView()
                    .transition(.opacity)
                    .zIndex(10)
            }
        }
        .sheet(item: $model.preview) { card in
            LevelPreviewSheet(card: card)
                .environmentObject(model)
        }
        .background(
            Color.clear.sheet(isPresented: $model.showSettings) {
                SettingsView()
                    .environmentObject(model)
            }
        )
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
        case .menu:
            MenuView()
        case .map:
            MapView()
        case .game:
            GameView()
        }
    }
}
