import SwiftUI

// Hora feliz y calendario del mes: dos motivos para volver cada día.

private func clockText(_ minuteOfDay: Int) -> String {
    return String(format: "%02d:%02d", (minuteOfDay / 60) % 24, minuteOfDay % 60)
}

private let monthNames = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"]

extension AppModel {
    func claimCalendar() {
        let json = act { $0.calendarClaim() }
        guard let result = decodeJSON(CalendarClaimResult.self, json) else { return }
        if result.ok {
            let big = result.big ?? false
            sounds.play(big ? Sfx.claim_all : Sfx.claim)
            Haptics.success()
            if let chest = result.chest {
                push(.info("¡Cofre del calendario!", "Ganaste un \(chestName(chest).lowercased()). Lo tienes en tu inventario de cofres.", "gift.fill"))
            }
            if let bonus = result.bonusChest {
                push(.info("¡Premio del mes!", "Completaste casillas suficientes: te llevas un \(chestName(bonus).lowercased()) extra.", "star.fill"))
            }
            showToast("¡Casilla cobrada! +\(result.coins ?? 0) monedas" + ((result.gems ?? 0) > 0 ? " y \(result.gems ?? 0) gemas" : ""))
        } else if let reason = result.reason {
            showToast(reason)
            sounds.play(.error, volume: 0.7)
        }
    }

    func recoverCalendar(_ day: Int) {
        let json = act { $0.calendarRecover(day: Int32(day)) }
        guard let result = decodeJSON(CalendarClaimResult.self, json) else { return }
        if result.ok {
            sounds.play(.unlock)
            Haptics.success()
            showToast("Día \(day) recuperado")
        } else if let reason = result.reason {
            showToast(reason)
            sounds.play(.error, volume: 0.7)
        }
    }
}

// MARK: - Hora feliz

struct HappyHourCard: View {
    let info: HappyHourInfo

    var body: some View {
        if info.phase == "ACTIVE" {
            card(active: true)
        } else if info.phase == "UPCOMING" {
            card(active: false)
        }
    }

    private func card(active: Bool) -> some View {
        let hot = Color(hex: 0xE8772E)
        let calm = Color(hex: 0x6B9E86)
        let tint = active ? hot : calm
        return HStack(spacing: 12) {
            ArtView(id: active ? "cozy.flame" : "cozy.timer")
                .frame(width: 46, height: 46)
            VStack(alignment: .leading, spacing: 2) {
                Text(active ? "HORA FELIZ" : "HORA FELIZ HOY")
                    .font(.system(size: 11, weight: .heavy))
                    .kerning(2)
                    .foregroundColor(Color.white.opacity(0.8))
                Text(active ? "Monedas x2 en cada victoria" : "A las \(clockText(info.startMin)) tus victorias dan el doble")
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
                if active && info.extraToday > 0 {
                    Text("Llevas +\(info.extraToday) monedas extra hoy")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(Color.white.opacity(0.85))
                }
            }
            Spacer(minLength: 0)
            VStack(spacing: 0) {
                Text(active ? "\(info.minutes)" : clockText(info.startMin))
                    .font(.system(size: active ? 24 : 18, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text(active ? "MIN" : "HORA")
                    .font(.system(size: 9, weight: .heavy))
                    .kerning(1.5)
                    .foregroundColor(Color.white.opacity(0.8))
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(LinearGradient(colors: [tint.shade(0.12), tint.shade(-0.1)], startPoint: .topLeading, endPoint: .bottomTrailing))
        )
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(Color.white.opacity(0.35), lineWidth: 1))
        .shadow(color: tint.shade(-0.3).opacity(0.5), radius: 0, x: 0, y: 3)
    }
}

// MARK: - Calendario (tarjeta de Inicio)

struct CalendarCard: View {
    @EnvironmentObject var model: AppModel
    let info: CalendarInfo

    var body: some View {
        if info.frozen { EmptyView() } else { content }
    }

    private var content: some View {
        let total = info.daysInMonth ?? 30
        let claimed = info.claimed ?? 0
        let claimable = info.claimable ?? false
        return VStack(spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: "calendar")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Theme.accent)
                    .frame(width: 34, height: 34)
                VStack(alignment: .leading, spacing: 1) {
                    Text("CALENDARIO")
                        .font(.system(size: 11, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.ink.opacity(0.5))
                    Text("\(claimed) de \(total) casillas cobradas")
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                }
                Spacer()
                Button(action: { model.sheet = .calendar }) {
                    Text("Ver mes")
                        .font(.system(size: 12, weight: .heavy, design: .rounded))
                        .foregroundColor(Theme.accent)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(Capsule().fill(Theme.accent.opacity(0.14)))
                }
            }
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(info.cells) { cell in
                            CalendarChip(cell: cell, width: 46)
                                .id(cell.day)
                        }
                    }
                    .padding(.vertical, 2)
                }
                .onAppear {
                    proxy.scrollTo(max(1, (info.today ?? 1) - 2), anchor: .leading)
                }
            }
            if claimable {
                BigButton(title: "COBRAR LA CASILLA DE HOY") {
                    model.claimCalendar()
                }
            } else if let next = info.nextBonusAt {
                Text("Con \(next) casillas cobradas ganas un cofre extra")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
        }
        .padding(14)
        .card()
    }
}

/// Una casilla del calendario: número, premio y estado.
struct CalendarChip: View {
    let cell: CalendarCell
    var width: CGFloat = 46

    private var tint: Color {
        switch cell.status {
        case "claimed", "todayDone": return Color(hex: 0x6B9E86)
        case "today": return Color(hex: 0xE8772E)
        case "recoverable": return Color(hex: 0x8E6BD6)
        case "missed": return Color(hex: 0xB9B2A0)
        default: return Color(hex: 0xD8D3E0)
        }
    }

    private var icon: String {
        if cell.chest != nil { return "chest.common.closed" }
        if cell.gems > 0 { return "cozy.gem" }
        return "cozy.coin"
    }

    var body: some View {
        let done = cell.status == "claimed" || cell.status == "todayDone"
        return VStack(spacing: 2) {
            Text("\(cell.day)")
                .font(.system(size: 10, weight: .heavy, design: .rounded))
                .foregroundColor(Color.white.opacity(0.9))
            ZStack {
                ArtView(id: icon)
                    .frame(width: width * 0.56, height: width * 0.56)
                    .opacity(done || cell.status == "missed" ? 0.45 : 1)
                if done {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: width * 0.34))
                        .foregroundColor(.white)
                        .shadow(color: Color.black.opacity(0.25), radius: 1, x: 0, y: 1)
                }
                if cell.status == "recoverable" {
                    Image(systemName: "arrow.uturn.backward.circle.fill")
                        .font(.system(size: width * 0.3))
                        .foregroundColor(.white)
                        .offset(x: width * 0.2, y: width * 0.2)
                }
            }
        }
        .frame(width: width, height: width * 1.15)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(LinearGradient(colors: [tint.shade(0.12), tint], startPoint: .top, endPoint: .bottom))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(cell.status == "today" ? Color.white : Color.white.opacity(0.25), lineWidth: cell.status == "today" ? 2.5 : 1)
        )
        .overlay(
            Group {
                if cell.big {
                    Image(systemName: "star.fill")
                        .font(.system(size: 9))
                        .foregroundColor(Theme.gold)
                        .offset(x: width * 0.32, y: -width * 0.46)
                }
            }
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Día \(cell.day), \(cell.status == "claimed" || cell.status == "todayDone" ? "cobrado" : (cell.status == "recoverable" ? "se puede recuperar" : (cell.status == "today" ? "hoy" : "")))")
    }
}

// MARK: - Calendario (hoja del mes)

struct CalendarSheet: View {
    @EnvironmentObject var model: AppModel
    @State private var askDay: CalendarCell? = nil

    var body: some View {
        let info = model.state?.calendar
        let rawCost = info?.recoverCost ?? 3
        let cost = rawCost == 0 ? 3 : rawCost
        return VStack(spacing: 14) {
            Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
            if let info = info, !info.frozen {
                let month = (info.month ?? 1) - 1
                Text("Calendario de \(monthNames[max(0, min(11, month))])")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text("Cobra tu casilla cada día. Si faltas, puedes recuperar un día perdido: el primero del mes es gratis y después cuesta \(cost) gemas.")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 8)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 5), spacing: 6) {
                    ForEach(info.cells) { cell in
                        Button(action: { tapped(cell, info) }) {
                            CalendarChip(cell: cell, width: 58)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                if info.claimable ?? false {
                    BigButton(title: "COBRAR HOY") {
                        model.claimCalendar()
                    }
                }
                if let next = info.nextBonusAt, let chest = info.nextBonusChest {
                    Text("A las \(next) casillas cobradas: cofre \(chestName(chest).lowercased())")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(Theme.ink.opacity(0.55))
                }
            } else {
                Text("El calendario se detuvo porque el reloj del teléfono está en un mes anterior.")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .padding(24)
            }
            Spacer(minLength: 8)
        }
        .padding(.horizontal, 18)
        .background(Theme.cream.ignoresSafeArea())
        .alert(item: $askDay) { cell in
            let cost = model.state?.calendar?.recoverCost ?? 3
            return Alert(
                title: Text("Recuperar el día \(cell.day)"),
                message: Text(cost == 0 ? "Es tu recuperación gratis de este mes." : "Cuesta \(cost) gemas."),
                primaryButton: .default(Text(cost == 0 ? "Recuperar gratis" : "Recuperar por \(cost) gemas")) {
                    model.recoverCalendar(cell.day)
                },
                secondaryButton: .cancel(Text("Ahora no"))
            )
        }
    }

    private func tapped(_ cell: CalendarCell, _ info: CalendarInfo) {
        if cell.status == "today" {
            model.claimCalendar()
        } else if cell.status == "recoverable" {
            askDay = cell
        }
    }
}

// MARK: - Serie destacada

struct FeaturedSeriesCard: View {
    @EnvironmentObject var model: AppModel
    let info: FeaturedInfo
    let jump: (String) -> Void

    var body: some View {
        let series = model.series(info.series)
        return Button(action: { jump(info.series) }) {
            HStack(spacing: 12) {
                if let series = series {
                    SeriesCrest(series: series, size: 46)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("SERIE DESTACADA DE HOY")
                        .font(.system(size: 10, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(Theme.ink.opacity(0.5))
                    Text(info.name)
                        .font(.system(size: 16, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text(loc(info.bonusReady ? "Tu primer cofre de hoy trae una carta extra de esta serie" : "Ya recibiste la carta extra de hoy"))
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(info.bonusReady ? Theme.accent : Theme.ink.opacity(0.5))
                }
                Spacer(minLength: 4)
                Text("\(info.owned)/\(info.total)")
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Capsule().fill(Theme.ink.opacity(0.07)))
            }
            .padding(14)
            .card(radius: 20)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
