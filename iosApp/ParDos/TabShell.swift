import SwiftUI

/// Pantalla principal: el contenido de la pestaña y la barra de abajo.
struct MainShell: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .top) {
                tabContent
                HalloweenGarland()
                if model.state == nil {
                    StateErrorCard()
                }
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
            ZStack(alignment: .top) {
                Rectangle()
                    .fill(Color.white)
                    .shadow(color: Theme.ink.opacity(0.12), radius: 6, x: 0, y: -2)
                Rectangle().fill(Theme.ink.opacity(0.06)).frame(height: 1)
            }
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
            if model.tab != tab {
                model.sounds.play(.tab)
                Haptics.select()
            }
            model.tab = tab
            if tab == .album || tab == .home { model.refreshState() }
        }) {
            VStack(spacing: 3) {
                ZStack(alignment: .topTrailing) {
                    Capsule()
                        .fill(selected ? Theme.accent.opacity(0.16) : Color.clear)
                        .frame(width: 52, height: 30)
                        .offset(x: -4, y: -1)
                    Image(systemName: symbol)
                        .font(.system(size: 21, weight: .bold))
                        .foregroundColor(selected ? Theme.accent : Theme.ink.opacity(0.35))
                        .frame(width: 44, height: 28)
                        .scaleEffect(selected ? 1.1 : 1)
                        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: selected)
                    if badge > 0 {
                        Circle()
                            .fill(Color(hex: 0xE0475B))
                            .frame(width: 10, height: 10)
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            .offset(x: 8, y: -2)
                    }
                }
                Text(loc(title))
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundColor(selected ? Theme.accent : Theme.ink.opacity(0.4))
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

/// Si por algún motivo no se puede leer el progreso, en vez de dejar la pantalla vacía se explica y se puede reintentar.
struct StateErrorCard: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 36))
                .foregroundColor(Theme.energy)
            Text("No pude leer tu progreso")
                .font(.system(size: 18, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            Text("Prueba otra vez. Si sigue igual, abre Ajustes → Diagnóstico y mándame lo que dice.")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Theme.ink.opacity(0.6))
                .multilineTextAlignment(.center)
            HStack(spacing: 10) {
                Button(action: { model.refreshState() }) {
                    Text("Reintentar")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16).padding(.vertical, 9)
                        .background(Capsule().fill(Theme.accent))
                }
                Button(action: { model.sheet = .settings }) {
                    Text("Ajustes")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.ink)
                        .padding(.horizontal, 16).padding(.vertical, 9)
                        .background(Capsule().fill(Color.white))
                }
            }
        }
        .padding(20)
        .frame(maxWidth: 320)
        .card(radius: 24)
        .padding(.top, 120)
    }
}
