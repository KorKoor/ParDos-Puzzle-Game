import SwiftUI
import UIKit

/// Tarjeta bonita con el resultado de una partida, lista para compartir como imagen (iOS 16 o más; en iOS 15 se comparte solo el texto).
struct ShareCardView: View {
    @EnvironmentObject var model: AppModel
    let snap: BoardSnap

    private var title: String {
        return snap.daily ? "Reto diario" : "Nivel \(snap.level)"
    }

    var body: some View {
        let tint = Theme.accent
        return VStack(spacing: 16) {
            HStack(spacing: 12) {
                AvatarView(id: model.state?.avatar ?? 1, size: 56, mine: true, animate: false)
                VStack(alignment: .leading, spacing: 2) {
                    Text(model.state?.name ?? "Jugador Zen")
                        .font(.system(size: 20, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text(loc("NIVEL") + " \(model.state?.playerLevel ?? 1)")
                        .font(.system(size: 11, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
            }
            Spacer(minLength: 0)
            Text(title)
                .font(.system(size: 34, weight: .black, design: .rounded))
                .foregroundColor(Theme.ink)
            HStack(spacing: 10) {
                ForEach(0..<3, id: \.self) { i in
                    Image(systemName: "star.fill")
                        .font(.system(size: 40))
                        .foregroundColor(i < snap.stars ? Theme.gold : Theme.ink.opacity(0.14))
                }
            }
            Text("\(snap.maxTile)")
                .font(.system(size: 54, weight: .black, design: .rounded))
                .foregroundColor(tileTextColor(snap.maxTile))
                .minimumScaleFactor(0.5)
                .frame(width: 130, height: 130)
                .background(RoundedRectangle(cornerRadius: 28, style: .continuous).fill(tileColor(snap.maxTile)))
                .shadow(color: Theme.ink.opacity(0.2), radius: 0, x: 0, y: 6)
            Text("\(snap.moves) movimientos · \(snap.score) puntos")
                .font(.system(size: 15, weight: .bold, design: .rounded))
                .foregroundColor(Theme.ink.opacity(0.7))
            Spacer(minLength: 0)
            Text("ParDos · Math Zen Puzzle")
                .font(.system(size: 12, weight: .heavy))
                .kerning(2)
                .foregroundColor(tint)
        }
        .padding(24)
        .frame(width: 360, height: 500)
        .background(LinearGradient(colors: [Color(hex: 0xFFFBF5), Color(hex: 0xF3EFE6), tint.opacity(0.35)], startPoint: .top, endPoint: .bottom))
    }
}

extension AppModel {
    /// La imagen de la tarjeta (nil en iOS 15, donde solo se comparte el texto).
    @MainActor func shareImage(_ snap: BoardSnap) -> UIImage? {
        if #available(iOS 16.0, *) {
            let renderer = ImageRenderer(content: ShareCardView(snap: snap).environmentObject(self))
            renderer.scale = 3
            return renderer.uiImage
        }
        return nil
    }
}

/// Tarjeta de jugador para compartir como imagen: banner, avatar, nombre, rango y marcas.
struct PlayerCardView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let state = model.state
        return ZStack(alignment: .bottomLeading) {
            BannerView(id: state?.banner ?? 1, height: 210, animate: false)
            LinearGradient(colors: [Color.black.opacity(0), Color.black.opacity(0.35)], startPoint: .center, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 14) {
                    AvatarView(id: state?.avatar ?? 1, size: 78, mine: true, animate: false)
                        .shadow(color: Color.black.opacity(0.3), radius: 6, x: 0, y: 3)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(state?.name ?? "Jugador Zen")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .shadow(color: Color.black.opacity(0.4), radius: 2, x: 0, y: 1)
                        Text("\(state?.rank ?? "Novato") · \(state?.prestige ?? 0) pts")
                            .font(.system(size: 13, weight: .heavy, design: .rounded))
                            .foregroundColor(Color.white.opacity(0.92))
                    }
                    Spacer()
                }
                HStack(spacing: 8) {
                    stat("NIVEL", "\(state?.playerLevel ?? 1)")
                    stat("ESTRELLAS", "\(model.totalStars)")
                    stat("PIEZAS", "\(model.album?.owned ?? 0)")
                    stat("RACHA", "\(model.streak)")
                }
                Text("ParDos · Math Zen Puzzle")
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(2)
                    .foregroundColor(Color.white.opacity(0.85))
            }
            .padding(18)
        }
        .frame(width: 360, height: 210)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
    }

    private func stat(_ label: String, _ value: String) -> some View {
        VStack(spacing: 1) {
            Text(value)
                .font(.system(size: 17, weight: .black, design: .rounded))
                .foregroundColor(.white)
            Text(loc(label))
                .font(.system(size: 8, weight: .heavy))
                .kerning(1)
                .foregroundColor(Color.white.opacity(0.85))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 6)
        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color.black.opacity(0.28)))
    }
}

extension AppModel {
    /// La tarjeta de jugador como imagen (nil en iOS 15).
    @MainActor func playerCardImage() -> UIImage? {
        if #available(iOS 16.0, *) {
            let renderer = ImageRenderer(content: PlayerCardView().environmentObject(self))
            renderer.scale = 3
            return renderer.uiImage
        }
        return nil
    }
}
