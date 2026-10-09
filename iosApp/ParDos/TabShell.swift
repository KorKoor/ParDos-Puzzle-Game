import SwiftUI

/// Pantalla principal: el contenido de la pestaña y la barra de abajo.
struct MainShell: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .top) {
                tabContent
                HalloweenGarland()
            }
            TabBarView()
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch model.tab {
        case .home:
            HomeView()
        case .play:
            MapView()
        case .shop:
            ShopView()
        case .album:
            AlbumView()
        case .profile:
            ProfileView()
        }
    }
}

struct TabBarView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        HStack(spacing: 0) {
            item(.home, "house.fill", "Inicio", badge: model.state?.badges ?? 0)
            item(.play, "map.fill", "Jugar", badge: 0)
            item(.shop, "bag.fill", "Tienda", badge: 0)
            item(.album, "rectangle.stack.fill", "Álbum", badge: albumBadge)
            item(.profile, "person.crop.circle.fill", "Perfil", badge: 0)
        }
        .padding(.horizontal, 8)
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(
            Rectangle()
                .fill(Color.white)
                .shadow(color: Theme.ink.opacity(0.12), radius: 6, x: 0, y: -2)
                .ignoresSafeArea(edges: .bottom)
        )
    }

    private var albumBadge: Int {
        guard let album = model.album else { return 0 }
        return album.claimableSeries.count + (album.albumClaimable ? 1 : 0)
    }

    private func item(_ tab: MainTab, _ symbol: String, _ title: String, badge: Int) -> some View {
        let selected = model.tab == tab
        return Button(action: {
            model.tab = tab
            if tab == .album || tab == .home { model.refreshState() }
        }) {
            VStack(spacing: 3) {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: symbol)
                        .font(.system(size: 21, weight: .bold))
                        .foregroundColor(selected ? Theme.accent : Theme.ink.opacity(0.35))
                        .frame(height: 26)
                        .scaleEffect(selected ? 1.12 : 1)
                    if badge > 0 {
                        Circle()
                            .fill(Color(hex: 0xE0475B))
                            .frame(width: 10, height: 10)
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            .offset(x: 8, y: -2)
                    }
                }
                Text(title)
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundColor(selected ? Theme.accent : Theme.ink.opacity(0.4))
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
