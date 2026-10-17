import SwiftUI
import UIKit

struct FriendRow: Decodable, Identifiable {
    let id: String
    let name: String
    let avatar: Int
    let banner: Int
    let level: Int
    let prestige: Int
    let stars: Int
    let pieces: Int
    let tower: Int
    let league: String
    let daysAgo: Int
    let me: Bool
    let position: Int
}

struct RivalInfo: Decodable {
    let name: String
    let gap: Int
}

struct FriendsData: Decodable {
    let rows: [FriendRow]
    let count: Int
    let max: Int
    let position: Int
    let above: RivalInfo?
    let below: RivalInfo?
    let code: String
}

struct FriendAddResult: Decodable {
    let ok: Bool
    let reason: String?
    let name: String?
    let updated: Bool?
}

extension AppModel {
    func loadFriends() -> FriendsData? {
        tickClock()
        persist()
        return decodeJSON(FriendsData.self, meta.friendsJson())
    }

    func friendInvite() -> String {
        tickClock()
        return meta.friendInviteText()
    }

    func addFriend(_ text: String) -> FriendAddResult? {
        let json = act { $0.addFriend(text: text) }
        return decodeJSON(FriendAddResult.self, json)
    }

    func removeFriend(_ id: String) { run { $0.removeFriend(id: id) } }
}

/// Amigos sin cuenta: se comparte la tarjeta (un código) por WhatsApp y quien la pega aparece en la lista, ordenada por prestigio.
struct FriendsSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var data: FriendsData?
    @State private var pasted: String = ""
    @State private var message: String?
    @State private var sharing: ShareItem?

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                SheetTitle("Amigos", symbol: "person.2.fill")
                Text("Sin cuentas ni servidores: compartes tu tarjeta por WhatsApp y tus amigos pegan la suya. Cada vez que quieras actualizar tus marcas, vuelve a compartirla.")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .multilineTextAlignment(.center)
                shareCard.rise(0.05)
                addCard.rise(0.12)
                if let data = data { listCard(data).rise(0.19) }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 30)
        }
        .background(LuxSheetBackground())
        .sheet(item: $sharing) { item in
            ShareSheet(text: item.text, image: item.image)
        }
        .onAppear { data = model.loadFriends() }
    }

    private var shareCard: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                LuxIcon("paperplane.fill", tint: Theme.energy, size: 42)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Tu tarjeta").font(.system(size: 16, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text("Tu nombre, avatar, nivel, prestigio y estrellas de hoy").font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
            }
            BigButton(title: "COMPARTIR MI TARJETA", color: Theme.energy) {
                sharing = ShareItem(text: model.friendInvite(), image: model.playerCardImage())
            }
        }
        .padding(14)
        .card()
    }

    private var addCard: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                LuxIcon("person.badge.plus", tint: Theme.accent, size: 42)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Agregar un amigo").font(.system(size: 16, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text("Pega el mensaje con su tarjeta (PF1-...)").font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
            }
            TextField("PF1-...", text: $pasted)
                .font(.system(size: 13, design: .monospaced))
                .autocapitalization(.allCharacters)
                .disableAutocorrection(true)
                .padding(10)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Theme.ink.opacity(0.05))
                        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).strokeBorder(Theme.ink.opacity(0.12), lineWidth: 1))
                )
            HStack(spacing: 10) {
                Button(action: { pasted = UIPasteboard.general.string ?? "" }) {
                    Text("Pegar")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.accent)
                        .padding(.horizontal, 16).padding(.vertical, 9)
                        .background(LuxCapsule())
                }
                .buttonStyle(ToyPressStyle())
                Button(action: add) {
                    Text("Agregar")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16).padding(.vertical, 9)
                        .background(Capsule().fill(LinearGradient(colors: [Theme.accent.shade(0.16), Theme.accent], startPoint: .top, endPoint: .bottom)))
                        .shadow(color: Theme.accent.opacity(0.4), radius: 6, y: 3)
                }
                .buttonStyle(ToyPressStyle())
                Spacer()
            }
            if let message = message {
                Text(loc(message)).font(.system(size: 12, weight: .bold)).foregroundColor(Theme.accent)
            }
        }
        .padding(14)
        .card()
    }

    private func add() {
        let result = model.addFriend(pasted)
        if let r = result, r.ok {
            message = (r.updated ?? false) ? "Tarjeta de \(r.name ?? "tu amigo") actualizada" : "¡\(r.name ?? "Tu amigo") ya es tu amigo!"
            pasted = ""
        } else {
            message = result?.reason ?? "No encontré una tarjeta válida"
        }
        data = model.loadFriends()
    }

    private func listCard(_ d: FriendsData) -> some View {
        VStack(spacing: 10) {
            HStack {
                Text("RANKING POR PRESTIGIO")
                    .font(.system(size: 10, weight: .heavy)).kerning(2).foregroundColor(Theme.ink.opacity(0.4))
                Spacer()
                Text("\(d.count)/\(d.max)").font(.system(size: 11, weight: .bold)).foregroundColor(Theme.ink.opacity(0.4))
            }
            if let above = d.above {
                Text("Te faltan \(above.gap) puntos para pasar a \(above.name)")
                    .font(.system(size: 12, weight: .bold)).foregroundColor(Theme.energy)
            }
            if d.count == 0 {
                Text("Todavía no tienes amigos. ¡Comparte tu tarjeta!")
                    .font(.system(size: 12, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.45))
            }
            ForEach(d.rows) { row in
                friendRow(row)
            }
            if let below = d.below, d.count > 0 {
                Text("\(below.name) va \(below.gap) puntos por detrás de ti")
                    .font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.45))
            }
        }
        .padding(14)
        .card()
    }

    private func friendRow(_ row: FriendRow) -> some View {
        HStack(spacing: 10) {
            if row.position <= 3 {
                LuxIcon("medal.fill", tint: row.position == 1 ? Theme.gold : (row.position == 2 ? Color(hex: 0xA9B0BE) : Color(hex: 0xB5733A)), size: 26)
            } else {
                Text("\(row.position)")
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink.opacity(0.5))
                    .frame(width: 26)
            }
            AvatarView(id: row.avatar, size: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(row.me ? row.name + " (tú)" : row.name)
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text("Nivel \(row.level) · \(row.league) · \(row.stars) estrellas · \(row.pieces) piezas")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
                if !row.me && row.daysAgo > 0 {
                    Text("tarjeta de hace \(row.daysAgo) \(row.daysAgo == 1 ? "día" : "días")")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.35))
                }
            }
            Spacer(minLength: 4)
            VStack(alignment: .trailing, spacing: 2) {
                Text("\(row.prestige)")
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text("prestigio").font(.system(size: 8, weight: .heavy)).foregroundColor(Theme.ink.opacity(0.4))
            }
            if !row.me {
                Button(action: {
                    model.removeFriend(row.id)
                    data = model.loadFriends()
                }) {
                    Image(systemName: "xmark.circle.fill").foregroundColor(Theme.ink.opacity(0.25))
                }
            }
        }
        .padding(8)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(row.me ? Theme.gold.opacity(0.14) : Color.clear)
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(row.me ? Theme.gold.opacity(0.6) : Color.clear, lineWidth: 1.3))
        )
    }
}
