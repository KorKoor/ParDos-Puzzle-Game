import SwiftUI

// Ofertas de tiempo limitado honestas: días fijos del calendario, reloj real, se pagan con gemas y se compran una sola vez.

struct TimedOfferInfo: Decodable, Identifiable {
    let id: String
    let title: String
    let blurb: String
    let lines: [String]
    let priceGems: Int
    let gemValue: Int
    let saving: Int
    let bought: Bool
    let endDay: Int
}

struct TimedOfferList: Decodable {
    let offers: [TimedOfferInfo]
}

/// Tarjetas de oferta en la tienda; si hoy no hay ninguna no se dibuja nada.
struct TimedOffersSection: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let offers = model.loadTimedOffers()
        if offers.isEmpty {
            EmptyView()
        } else {
            VStack(spacing: 10) {
                ForEach(offers) { offer in TimedOfferCard(offer: offer) }
            }
        }
    }
}

struct TimedOfferCard: View {
    @EnvironmentObject var model: AppModel
    let offer: TimedOfferInfo
    private let gilt = Color(hex: 0xE9B949)
    private let night = Color(hex: 0x2E1F55)

    /// Fin real de la ventana: medianoche local del día siguiente al último día.
    private var endDate: Date {
        let utc = Date(timeIntervalSince1970: Double(offer.endDay) * 86_400)
        let offset = TimeZone.current.secondsFromGMT(for: utc)
        return Date(timeIntervalSince1970: Double(offer.endDay) * 86_400 - Double(offset))
    }

    private func left(_ now: Date) -> String {
        let secs = max(0, Int(endDate.timeIntervalSince(now)))
        let h = secs / 3600, m = (secs % 3600) / 60
        if secs <= 0 { return loc("Terminó") }
        return h >= 1 ? "\(h) h \(m) min" : "\(m) min"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(loc("OFERTA · TIEMPO LIMITADO"))
                    .font(.system(size: 10, weight: .heavy))
                    .kerning(1.5)
                    .foregroundColor(gilt)
                    .lineLimit(1)
                Spacer(minLength: 6)
                TimelineView(.periodic(from: Date(), by: 30)) { ctx in
                    Text("\(loc("Termina en")) \(left(ctx.date))")
                        .font(.system(size: 11, weight: .heavy))
                        .foregroundColor(night)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(gilt))
                        .fixedSize()
                }
            }
            Text(loc(offer.title))
                .font(.system(size: 20, weight: .black, design: .rounded))
                .foregroundColor(.white)
            Text(loc(offer.blurb))
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(.white.opacity(0.75))
            Text(offer.lines.map { loc($0) }.joined(separator: "  \u{00B7}  "))
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Color(hex: 0xFFE9B0))
            Text("\(loc("Por separado")): \(offer.gemValue) \u{25C6} \u{00B7} \(loc("Aquí")): \(offer.priceGems) \u{25C6} (\(loc("ahorras")) \(offer.saving) %). \(loc("Solo se compra una vez."))")
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(.white.opacity(0.6))
            if offer.bought {
                Text(loc("Ya la compraste. ¡Que la disfrutes!"))
                    .font(.system(size: 13, weight: .heavy))
                    .foregroundColor(gilt)
                    .padding(.top, 4)
            } else {
                let enough = model.gems >= offer.priceGems
                Button(action: { model.buyTimedOffer(offer.id) }) {
                    HStack(spacing: 6) {
                        if enough {
                            Text("\(loc("COMPRAR")) \u{00B7} \(offer.priceGems)")
                            GemIcon(size: 16)
                        } else {
                            Text("\(loc("Te faltan")) \(offer.priceGems - model.gems)")
                            GemIcon(size: 16)
                        }
                    }
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(enough ? Theme.accent : Color.gray.opacity(0.5)))
                }
                .buttonStyle(PlainButtonStyle())
                .disabled(!enough)
                .padding(.top, 4)
            }
        }
        .padding(18)
        .background(
            ZStack {
                RoundedRectangle(cornerRadius: 24, style: .continuous).fill(night)
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(LinearGradient(colors: [gilt.opacity(0.9), gilt.opacity(0.15)], startPoint: .topLeading, endPoint: .bottomTrailing), lineWidth: 1.5)
            }
        )
        .shimmer(radius: 24)
        .shadow(color: night.opacity(0.35), radius: 14, y: 8)
    }
}
