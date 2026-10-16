import SwiftUI

// El Panteón de jefes: 12 salas, 60 jefes. Se abre con el nivel 30 de la campaña y se avanza jefe a jefe.

struct PantheonBoss: Decodable, Identifiable {
    let id: Int
    let name: String
    let motif: String
    let taunt: String
    let stars: Int
    let unlocked: Bool
    let roomBoss: Bool
    let coins: Int
    let gems: Int
}

struct PantheonRoom: Decodable, Identifiable {
    let index: Int
    let name: String
    let bosses: [PantheonBoss]
    var id: Int { index }
}

struct PantheonData: Decodable {
    let open: Bool
    let minLevel: Int
    let rooms: [PantheonRoom]
    let beaten: Int
    let total: Int
}

struct PantheonWin: Decodable {
    let ok: Bool
    let first: Bool
    let coins: Int
    let gems: Int
    let chest: String
    let room: String?
    let roomDone: Bool?
}

extension AppModel {
    func loadPantheon() -> PantheonData? {
        return decodeJSON(PantheonData.self, meta.pantheon())
    }

    func finishPantheon(_ s: BoardSnap) {
        guard s.status == "won" else { return }
        let json = act { $0.pantheonWin(level: Int32(s.level), stars: Int32(max(1, s.stars))) }
        if let r = decodeJSON(PantheonWin.self, json), r.ok, r.first {
            var text = "Jefe vencido: +\(r.coins) monedas, +\(r.gems) gemas"
            if !r.chest.isEmpty { text += " y un cofre" }
            if r.roomDone == true, let room = r.room { text = "¡Sala completada: \(room)! " + text; Haptics.success(); sounds.play(.stars_3) }
            showToast(text)
        }
        maybeAskForReview()
    }
}

/// Fondo vivo del Panteón: cielo profundo, estrellas que titilan y un resplandor dorado que respira.
struct PantheonBackdrop: View {
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 24.0)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            Canvas { ctx, size in
                let bg = Gradient(colors: [Color(hex: 0x07040F), Color(hex: 0x1A0F33), Color(hex: 0x2A1650)])
                ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .linearGradient(bg, startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
                let pulse = 0.5 + 0.5 * sin(t * 0.8)
                let c = CGPoint(x: size.width * 0.5, y: size.height * 0.12)
                ctx.fill(
                    Path(ellipseIn: CGRect(x: c.x - size.width * 0.8, y: c.y - size.width * 0.8, width: size.width * 1.6, height: size.width * 1.6)),
                    with: .radialGradient(Gradient(colors: [Color(hex: 0xE9B949).opacity(0.16 + 0.07 * pulse), .clear]), center: c, startRadius: 0, endRadius: size.width * 0.8)
                )
                for i in 0..<46 {
                    let fx = Double((i * 7919) % 1000) / 1000.0
                    let fy = Double((i * 104729) % 1000) / 1000.0
                    let tw = 0.35 + 0.65 * max(0, sin(t * (0.6 + Double(i % 5) * 0.25) + Double(i)))
                    let r = 0.8 + Double(i % 3) * 0.7
                    ctx.fill(Path(ellipseIn: CGRect(x: fx * size.width, y: fy * size.height, width: r * 2, height: r * 2)), with: .color(Color.white.opacity(0.7 * tw)))
                }
            }
        }
        .ignoresSafeArea()
    }
}

struct PantheonSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var selected: PantheonBoss?
    private let gold = Color(hex: 0xE9B949)

    var body: some View {
        let data = model.loadPantheon()
        return ZStack {
            PantheonBackdrop()
            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    Capsule().fill(Color.white.opacity(0.25)).frame(width: 40, height: 4).padding(.top, 10)
                    title
                    if let d = data {
                        if d.open {
                            Text("\(d.beaten) / \(d.total) \(loc("jefes vencidos"))")
                                .font(.system(size: 12, weight: .heavy))
                                .foregroundColor(.white.opacity(0.65))
                            ProgressView(value: Double(d.beaten), total: Double(max(1, d.total)))
                                .tint(gold)
                                .padding(.horizontal, 36)
                            ForEach(d.rooms) { room in roomCard(room) }
                        } else {
                            locked(d.minLevel)
                        }
                    }
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 16)
            }
            if let boss = selected {
                detail(boss)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    private var title: some View {
        VStack(spacing: 4) {
            Text("PANTEÓN")
                .font(.system(size: 34, weight: .black, design: .serif))
                .kerning(8)
                .foregroundColor(.clear)
                .overlay(
                    LinearGradient(colors: [Color(hex: 0xFFF1B5), gold, Color(hex: 0xB07A1E)], startPoint: .top, endPoint: .bottom)
                        .mask(Text("PANTEÓN").font(.system(size: 34, weight: .black, design: .serif)).kerning(8))
                )
                .shadow(color: gold.opacity(0.5), radius: 12)
            Text(loc("DE JEFES"))
                .font(.system(size: 11, weight: .heavy))
                .kerning(6)
                .foregroundColor(.white.opacity(0.55))
        }
        .padding(.top, 8)
    }

    private func locked(_ minLevel: Int) -> some View {
        VStack(spacing: 10) {
            Image(systemName: "lock.fill").font(.system(size: 30, weight: .bold)).foregroundColor(gold)
            Text("\(loc("Se abre al llegar al nivel")) \(minLevel) \(loc("de la campaña"))")
                .font(.system(size: 15, weight: .bold, design: .rounded))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
            Text(loc("Aquí esperan 60 jefes en 12 salas, con premios para cada uno."))
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(.white.opacity(0.6))
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Color.white.opacity(0.07)))
    }

    private func roomCard(_ room: PantheonRoom) -> some View {
        let done = room.bosses.filter { $0.stars > 0 }.count
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(loc(room.name).uppercased())
                    .font(.system(size: 12, weight: .heavy))
                    .kerning(2.5)
                    .foregroundColor(gold)
                Spacer()
                Text("\(done)/5")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundColor(.white.opacity(0.55))
            }
            HStack(spacing: 8) {
                ForEach(room.bosses) { boss in medallion(boss) }
            }
        }
        .padding(14)
        .background(
            ZStack {
                RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Color.white.opacity(0.06))
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(LinearGradient(colors: [gold.opacity(done == 5 ? 0.9 : 0.35), Color.white.opacity(0.05)], startPoint: .topLeading, endPoint: .bottomTrailing), lineWidth: 1.2)
            }
        )
        .shimmer(radius: 24, period: done == 5 ? 3.2 : 9)
    }

    private func medallion(_ boss: PantheonBoss) -> some View {
        Button(action: {
            if boss.unlocked {
                Haptics.tap()
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) { selected = boss }
            } else {
                model.showToast("Vence al jefe anterior para abrir este")
            }
        }) {
            VStack(spacing: 5) {
                ZStack {
                    BossEmblemView(motif: boss.motif, size: 54, locked: boss.stars == 0)
                        .opacity(boss.unlocked ? 1 : 0.35)
                    if !boss.unlocked {
                        Image(systemName: "lock.fill").font(.system(size: 14, weight: .bold)).foregroundColor(.white.opacity(0.8))
                    }
                    if boss.roomBoss {
                        Circle().stroke(gold, lineWidth: 2).frame(width: 60, height: 60)
                    }
                }
                HStack(spacing: 1) {
                    ForEach(0..<3, id: \.self) { i in
                        Image(systemName: "star.fill")
                            .font(.system(size: 7))
                            .foregroundColor(i < boss.stars ? gold : Color.white.opacity(0.18))
                    }
                }
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func detail(_ boss: PantheonBoss) -> some View {
        VStack {
            Spacer()
            VStack(spacing: 10) {
                BossEmblemView(motif: boss.motif, size: 96, locked: boss.stars == 0)
                Text(boss.name)
                    .font(.system(size: 24, weight: .black, design: .serif))
                    .foregroundColor(.white)
                Text("\u{201C}\(boss.taunt)\u{201D}")
                    .font(.system(size: 13, weight: .medium, design: .serif))
                    .foregroundColor(Color(hex: 0xFFE9B0))
                    .multilineTextAlignment(.center)
                HStack(spacing: 12) {
                    HStack(spacing: 3) { Text("+\(boss.coins)"); CoinIcon(size: 14) }
                    HStack(spacing: 3) { Text("+\(boss.gems)"); GemIcon(size: 14) }
                }
                .font(.system(size: 13, weight: .black, design: .rounded))
                .foregroundColor(.white.opacity(boss.stars > 0 ? 0.4 : 0.9))
                Button(action: {
                    selected = nil
                    model.start(boss.id)
                }) {
                    Text(boss.stars > 0 ? loc("VOLVER A ENFRENTAR") : loc("ENFRENTAR"))
                        .font(.system(size: 16, weight: .black, design: .rounded))
                        .kerning(2)
                        .foregroundColor(Color(hex: 0x2A1650))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Capsule().fill(LinearGradient(colors: [Color(hex: 0xFFF1B5), gold], startPoint: .top, endPoint: .bottom)))
                }
                .buttonStyle(PlainButtonStyle())
                Button(action: { withAnimation { selected = nil } }) {
                    Text(loc("Cerrar")).font(.system(size: 12, weight: .bold)).foregroundColor(.white.opacity(0.55))
                }
            }
            .padding(20)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(Color(hex: 0x1A0F33))
                    .overlay(RoundedRectangle(cornerRadius: 28, style: .continuous).stroke(gold.opacity(0.6), lineWidth: 1.4))
            )
            .shimmer(radius: 28, period: 4)
            .padding(16)
        }
        .background(Color.black.opacity(0.45).ignoresSafeArea().onTapGesture { withAnimation { selected = nil } })
    }
}

/// Tarjeta de Inicio que lleva al Panteón.
struct PantheonCard: View {
    @EnvironmentObject var model: AppModel
    private let gold = Color(hex: 0xE9B949)

    var body: some View {
        let data = model.loadPantheon()
        Button(action: { model.sheet = .pantheon }) {
            HStack(spacing: 14) {
                BossEmblemView(motif: "EYE", size: 62, locked: !(data?.open ?? false))
                VStack(alignment: .leading, spacing: 3) {
                    Text(loc("PANTEÓN DE JEFES"))
                        .font(.system(size: 11, weight: .heavy))
                        .kerning(2.5)
                        .foregroundColor(gold)
                    Text(loc("60 jefes · 12 salas"))
                        .font(.system(size: 18, weight: .black, design: .serif))
                        .foregroundColor(.white)
                    Text((data?.open ?? false) ? "\(data?.beaten ?? 0) / \(data?.total ?? 60) \(loc("vencidos"))" : "\(loc("Se abre en el nivel")) \(data?.minLevel ?? 30)")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.white.opacity(0.6))
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").font(.system(size: 14, weight: .black)).foregroundColor(.white.opacity(0.6))
            }
            .padding(16)
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(LinearGradient(colors: [Color(hex: 0x120A26), Color(hex: 0x2A1650)], startPoint: .topLeading, endPoint: .bottomTrailing))
                    RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(gold.opacity(0.55), lineWidth: 1.3)
                }
            )
            .shimmer(radius: 24, period: 4.2)
            .shadow(color: Color(hex: 0x2A1650).opacity(0.45), radius: 14, y: 8)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
