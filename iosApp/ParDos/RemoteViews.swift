import SwiftUI
import UIKit

struct RemoteCreateInfo: Decodable {
    let ok: Bool
    let coins: Int
    let code: String
    let text: String
}

struct RemoteChallengeInfo: Decodable {
    let ok: Bool
    let reason: String?
    let seed: Int64?
    let score: Int?
    let name: String?
    let display: String?
    let played: Bool?
}

struct RemoteResultInfo: Decodable {
    let ok: Bool
    let outcome: String
    let coins: Int
    let gems: Int
    let firstTime: Bool
    let mine: Int
    let theirs: Int
}

struct RemoteHistoryItem: Decodable, Identifiable {
    let opponent: String
    let mine: Int
    let theirs: Int
    let outcome: String
    let day: Int

    var id: String { "\(opponent)-\(day)-\(mine)-\(theirs)" }
}

struct RemoteHistoryData: Decodable {
    let history: [RemoteHistoryItem]
    let wins: Int
    let losses: Int
    let ties: Int
    let streak: Int
    let best: Int
    let createdToday: Int
    let createLimit: Int
}

// MARK: - Modelo

extension AppModel {
    func startRemoteCreate() {
        remoteSeed = meta.remoteNewSeed()
        remoteRole = "CREATOR"
        remoteChallenge = nil
        remoteCreate = nil
        remoteResult = nil
        startRemoteRound(label: "TU RETO")
    }

    func startRemoteAccept(_ challenge: RemoteChallengeInfo) {
        remoteSeed = challenge.seed ?? 1
        remoteRole = "CHALLENGED"
        remoteChallenge = challenge
        remoteCreate = nil
        remoteResult = nil
        startRemoteRound(label: "RETO DE " + (challenge.display ?? "AMIGO").uppercased())
    }

    func startRemoteRound(label: String) {
        guard let cfg = decodeJSON(DuelConfigInfo.self, meta.duelConfig()) else { return }
        mode = .remote
        session.tutorialEnabled = false
        session.startCustom(
            size: Int32(cfg.size), target: Int32(cfg.target), timeLimitMs: Int64(cfg.roundMs), seed: remoteSeed,
            levelNumber: 1, label: label, comboBonus: false, powers: false
        )
        assistMessage = nil
        sheet = nil
        begin(introFor: nil, key: "remote")
    }

    func restartRemote() {
        if remoteRole == "CHALLENGED", let c = remoteChallenge {
            startRemoteAccept(c)
        } else {
            startRemoteCreate()
        }
    }

    func finishRemote(_ s: BoardSnap) {
        let myName = state?.name ?? "Jugador"
        if remoteRole == "CREATOR" {
            let json = act { $0.remoteFinishCreator(seed: remoteSeed, score: Int32(s.score), name: myName) }
            remoteCreate = decodeJSON(RemoteCreateInfo.self, json)
        } else if let c = remoteChallenge {
            let json = act { $0.remoteFinishChallenged(seed: remoteSeed, theirScore: Int32(c.score ?? 0), name: c.name ?? "AMIGO", myScore: Int32(s.score)) }
            remoteResult = decodeJSON(RemoteResultInfo.self, json)
        }
    }

    func decodeRemote(_ text: String) -> RemoteChallengeInfo? {
        return decodeJSON(RemoteChallengeInfo.self, meta.remoteDecode(text: text))
    }

    func loadRemoteHistory() -> RemoteHistoryData? {
        tickClock()
        return decodeJSON(RemoteHistoryData.self, meta.remoteHistory())
    }
}

// MARK: - Pantalla del duelo a distancia

struct RemoteSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var pasted: String = ""
    @State private var found: RemoteChallengeInfo?
    @State private var problem: String?
    @State private var history: RemoteHistoryData?

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                Text("Duelo a distancia")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text("Juega 60 segundos, comparte tu código por WhatsApp y tu amigo (en iPhone o Android) juega el mismo tablero.")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.55))
                    .multilineTextAlignment(.center)
                createCard
                acceptCard
                if let h = history { historyCard(h) }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 30)
        }
        .background(Theme.cream.ignoresSafeArea())
        .onAppear { history = model.loadRemoteHistory() }
    }

    private var createCard: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: "paperplane.fill").font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                    .frame(width: 42, height: 42).background(Circle().fill(Theme.energy))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Lanzar un reto").font(.system(size: 16, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text("Juegas tú primero y compartes el código").font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
            }
            BigButton(title: "JUGAR MI RETO", color: Theme.energy) { model.startRemoteCreate() }
        }
        .padding(14)
        .card()
    }

    private var acceptCard: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: "doc.on.clipboard.fill").font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                    .frame(width: 42, height: 42).background(Circle().fill(Theme.accent))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Tengo un código").font(.system(size: 16, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text("Pega el mensaje que te mandaron").font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
            }
            TextField("PD1-...", text: $pasted)
                .font(.system(size: 14, design: .monospaced))
                .autocapitalization(.allCharacters)
                .disableAutocorrection(true)
                .padding(10)
                .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Theme.ink.opacity(0.06)))
            HStack(spacing: 10) {
                Button(action: {
                    pasted = UIPasteboard.general.string ?? ""
                    check()
                }) {
                    Text("Pegar")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.accent)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .background(Capsule().fill(Theme.accent.opacity(0.14)))
                }
                Button(action: { check() }) {
                    Text("Leer código")
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .background(Capsule().fill(Theme.accent))
                }
                Spacer()
            }
            if let problem = problem {
                Text(problem).font(.system(size: 12, weight: .bold)).foregroundColor(Color(hex: 0xB4413C))
            }
            if let c = found, c.ok {
                VStack(spacing: 8) {
                    Text("\(c.display ?? "Un amigo") hizo \(c.score ?? 0) puntos")
                        .font(.system(size: 15, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                    if c.played ?? false {
                        Text("Ya jugaste este reto: puedes repetirlo, pero no paga ni cuenta.")
                            .font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                            .multilineTextAlignment(.center)
                    }
                    BigButton(title: "ACEPTAR EL RETO") { model.startRemoteAccept(c) }
                }
            }
        }
        .padding(14)
        .card()
    }

    private func check() {
        let result = model.decodeRemote(pasted)
        if let r = result, r.ok {
            found = r
            problem = nil
        } else {
            found = nil
            problem = result?.reason ?? "No encontré un código válido"
        }
    }

    private func historyCard(_ h: RemoteHistoryData) -> some View {
        VStack(spacing: 8) {
            HStack {
                Text("TUS DUELOS")
                    .font(.system(size: 10, weight: .heavy)).kerning(2).foregroundColor(Theme.ink.opacity(0.4))
                Spacer()
                Text("\(h.wins) G · \(h.losses) P · \(h.ties) E")
                    .font(.system(size: 12, weight: .heavy)).foregroundColor(Theme.ink.opacity(0.6))
            }
            if h.history.isEmpty {
                Text("Aún no has jugado ninguno.")
                    .font(.system(size: 12, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.45))
            }
            ForEach(h.history) { item in
                HStack {
                    Image(systemName: item.outcome == "WIN" ? "checkmark.circle.fill" : (item.outcome == "LOSE" ? "xmark.circle.fill" : "equal.circle.fill"))
                        .foregroundColor(item.outcome == "WIN" ? Theme.accent : (item.outcome == "LOSE" ? Color(hex: 0xB4413C) : Theme.gold))
                    Text(item.opponent)
                        .font(.system(size: 13, weight: .bold, design: .rounded)).foregroundColor(Theme.ink)
                    Spacer()
                    Text("\(item.mine) - \(item.theirs)")
                        .font(.system(size: 13, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                }
            }
        }
        .padding(14)
        .card()
    }
}

// MARK: - Resultado del duelo a distancia

struct RemoteResultContent: View {
    @EnvironmentObject var model: AppModel
    @State private var sharing: ShareItem?

    var body: some View {
        VStack(spacing: 14) {
            if model.remoteRole == "CREATOR" {
                if let created = model.remoteCreate {
                    Text("¡Reto listo!")
                        .font(.system(size: 26, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    Text("Hiciste \(model.snap?.score ?? 0) puntos")
                        .font(.system(size: 20, weight: .black, design: .rounded)).foregroundColor(Theme.accent)
                    if created.coins > 0 {
                        HStack(spacing: 6) { CoinIcon(size: 22); Text("+\(created.coins)") }
                            .font(.system(size: 18, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                    }
                    Text(created.code)
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(Theme.ink.opacity(0.7))
                        .padding(10)
                        .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Theme.ink.opacity(0.06)))
                    BigButton(title: "COMPARTIR EL RETO", color: Theme.energy) { sharing = ShareItem(text: created.text) }
                }
            } else if let result = model.remoteResult {
                Text(result.outcome == "WIN" ? "¡Le ganaste!" : (result.outcome == "LOSE" ? "Esta vez perdiste" : "¡Empate!"))
                    .font(.system(size: 26, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                Text("\(result.mine) contra \(result.theirs)")
                    .font(.system(size: 20, weight: .black, design: .rounded)).foregroundColor(Theme.accent)
                if result.firstTime {
                    HStack(spacing: 14) {
                        HStack(spacing: 6) { CoinIcon(size: 22); Text("+\(result.coins)") }
                        if result.gems > 0 { HStack(spacing: 6) { GemIcon(size: 22); Text("+\(result.gems)") } }
                    }
                    .font(.system(size: 18, weight: .black, design: .rounded)).foregroundColor(Theme.ink)
                } else {
                    Text("Ya habías jugado este reto: no paga de nuevo.")
                        .font(.system(size: 11, weight: .semibold)).foregroundColor(Theme.ink.opacity(0.5))
                }
            }
            Button(action: { model.restartRemote() }) {
                Text("Jugar otra vez")
                    .font(.system(size: 14, weight: .heavy, design: .rounded)).foregroundColor(Theme.ink.opacity(0.6))
            }
            Button(action: { model.backToMap() }) {
                Text("Volver al mapa")
                    .font(.system(size: 14, weight: .heavy, design: .rounded)).foregroundColor(Theme.ink.opacity(0.6))
            }
        }
        .sheet(item: $sharing) { item in
            ShareSheet(text: item.text)
        }
    }
}
