import SwiftUI

/// Álbum de coleccionables: 32 series de 10 piezas. Cofres, repetidas, esencia, brillantes y vitrina.
struct AlbumView: View {
    @EnvironmentObject var model: AppModel
    @State private var selected: String?

    @State private var onlyMissing = false

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                LazyVStack(spacing: 14) {
                    header
                    if let album = model.album {
                        summary(album)
                        if !album.perks.isEmpty { perks(album) }
                        sellRow(album)
                        controls(proxy)
                        ForEach(model.albumCatalog.series) { series in
                            if !onlyMissing || !isComplete(series, album) {
                                SeriesSection(series: series, album: album) { id in selected = id }
                                    .id(series.id)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 26)
                .padding(.bottom, 24)
            }
        }
        .sheet(item: Binding(get: { selected.map { PieceKey(id: $0) } }, set: { selected = $0?.id })) { key in
            PieceDetail(pieceID: key.id)
                .environmentObject(model)
        }
        .onAppear { model.refreshState() }
    }

    private func isComplete(_ series: AlbumSeries, _ album: AlbumStateData) -> Bool {
        let list = model.albumCatalog.pieces.filter { $0.series == series.id }
        return list.allSatisfy { (album.copies[$0.id] ?? 0) > 0 }
    }

    private func controls(_ proxy: ScrollViewProxy) -> some View {
        HStack(spacing: 10) {
            Menu {
                ForEach(model.albumCatalog.series) { series in
                    Button(series.glyph + " " + series.name) {
                        withAnimation { proxy.scrollTo(series.id, anchor: .top) }
                    }
                }
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "list.bullet")
                    Text("Ir a una serie")
                }
                .font(.system(size: 12, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Capsule().fill(Color.white))
            }
            Button(action: { onlyMissing.toggle() }) {
                HStack(spacing: 4) {
                    Image(systemName: onlyMissing ? "checkmark.circle.fill" : "circle")
                    Text("Solo las que me faltan")
                }
                .font(.system(size: 12, weight: .heavy, design: .rounded))
                .foregroundColor(onlyMissing ? .white : Theme.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Capsule().fill(onlyMissing ? Theme.accent : Color.white))
            }
            Spacer()
        }
    }

    private var header: some View {
        VStack(spacing: 10) {
            HStack {
                Text("Álbum")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
            }
            HStack(spacing: 8) {
                AmountPill(kind: .coin, amount: model.coins)
                AmountPill(kind: .gem, amount: model.gems)
                AmountPill(kind: .shard, amount: model.album?.shards ?? 0)
                AmountPill(kind: .token, amount: model.album?.tokens ?? 0)
                Spacer()
            }
        }
    }

    private func summary(_ album: AlbumStateData) -> some View {
        let fraction = Double(album.owned) / Double(max(1, album.total))
        return VStack(spacing: 10) {
            HStack {
                Text("\(album.owned) / \(album.total) piezas")
                    .font(.system(size: 20, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Spacer()
                Text("+\(album.coinPercent)% monedas")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundColor(Theme.accent)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.ink.opacity(0.08))
                    Capsule().fill(LinearGradient(colors: [Theme.accent, Theme.gold], startPoint: .leading, endPoint: .trailing))
                        .frame(width: max(8, geo.size.width * CGFloat(fraction)))
                }
            }
            .frame(height: 10)
            HStack(spacing: 8) {
                chestPill("COMMON", album.chests.COMMON)
                chestPill("RARE", album.chests.RARE)
                chestPill("EPIC", album.chests.EPIC)
            }
            if album.albumClaimable {
                BigButton(title: "¡ÁLBUM COMPLETO! COBRAR PREMIO", color: Theme.gold) { model.claimAlbumReward() }
            } else if album.albumClaimed {
                Text("Álbum completo cobrado ✓ · skin Oro Real")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Theme.accent)
            } else {
                Text("Completa todo el álbum: skin Oro Real + \(model.eco?.albumGems ?? 50) gemas")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
        }
        .padding(14)
        .card()
    }

    private func chestPill(_ type: String, _ count: Int) -> some View {
        Button(action: { if count > 0 { model.openChest(type) } else { model.showToast("No tienes cofres de este tipo") } }) {
            HStack(spacing: 6) {
                ChestIcon(type: type, size: 26).opacity(count > 0 ? 1 : 0.4)
                Text(count > 0 ? "Abrir ×\(count)" : "0")
                    .font(.system(size: 12, weight: .black, design: .rounded))
                    .foregroundColor(count > 0 ? Theme.accent : Theme.ink.opacity(0.35))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Theme.ink.opacity(0.05)))
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func perks(_ album: AlbumStateData) -> some View {
        VStack(spacing: 8) {
            SectionTitle(text: "Mejoras del álbum", detail: "piezas épicas y legendarias")
            ForEach(album.perks) { perk in
                HStack {
                    Text(String(perk.label.prefix(1)).uppercased() + String(perk.label.dropFirst()))
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Spacer()
                    Text("+\(String(format: "%.1f", Double(perk.tenths) / 10.0))%")
                        .font(.system(size: 13, weight: .black, design: .rounded))
                        .foregroundColor(perk.tenths > 0 ? Theme.accent : Theme.ink.opacity(0.3))
                }
            }
        }
        .padding(14)
        .card()
    }

    private func sellRow(_ album: AlbumStateData) -> some View {
        let spares = album.copies.values.filter { $0 > 1 }.count
        return HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Repetidas")
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(Theme.ink)
                Text(spares > 0 ? "\(spares) piezas con copias de sobra" : "Aún no tienes repetidas")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.5))
            }
            Spacer()
            Button(action: { model.sellAll("COMMON") }) {
                Text("Vender comunes")
                    .font(.system(size: 11, weight: .heavy, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(spares > 0 ? Theme.accent : Color.gray.opacity(0.5)))
            }
            .disabled(spares == 0)
            Button(action: { model.sellAll("RARE") }) {
                Text("Hasta raras")
                    .font(.system(size: 11, weight: .heavy, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(Capsule().fill(spares > 0 ? Color(hex: 0x3F8FE0) : Color.gray.opacity(0.5)))
            }
            .disabled(spares == 0)
        }
        .padding(14)
        .card()
    }
}

struct PieceKey: Identifiable {
    let id: String
}

struct SeriesSection: View {
    @EnvironmentObject var model: AppModel
    let series: AlbumSeries
    let album: AlbumStateData
    let onSelect: (String) -> Void

    private var pieces: [AlbumPiece] {
        model.albumCatalog.pieces.filter { $0.series == series.id }
    }

    var body: some View {
        let list = pieces
        let ownedCount = list.filter { (album.copies[$0.id] ?? 0) > 0 }.count
        let complete = ownedCount == list.count
        let claimable = album.claimableSeries.contains(series.id)
        let claimed = album.claimedSeries.contains(series.id)
        return VStack(spacing: 10) {
            HStack(spacing: 10) {
                ZStack {
                    Circle().fill(Color(hex: UInt32(series.top))).frame(width: 40, height: 40)
                    Text(series.glyph).font(.system(size: 22))
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(series.name)
                        .font(.system(size: 16, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text("\(ownedCount)/\(list.count) · mejora: \(series.perkLabel)")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(Theme.ink.opacity(0.5))
                }
                Spacer()
                if claimable {
                    Button(action: { model.claimSeriesReward(series.id) }) {
                        HStack(spacing: 4) { CoinIcon(size: 14); Text("\(series.coins)"); GemIcon(size: 14); Text("\(series.gems)") }
                            .font(.system(size: 11, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(Capsule().fill(Theme.accent))
                    }
                } else if claimed {
                    Image(systemName: "checkmark.seal.fill").foregroundColor(Theme.accent)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(list) { piece in
                        Button(action: { onSelect(piece.id) }) {
                            PieceCard(
                                piece: piece, series: series, owned: (album.copies[piece.id] ?? 0) > 0,
                                foil: album.foil.contains(piece.id), copies: album.copies[piece.id] ?? 0, width: 82
                            )
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(.vertical, 3)
                .padding(.horizontal, 2)
            }
            if !complete {
                HStack {
                    Spacer()
                    Menu {
                        Button("Sobre de \(model.eco?.seriesPackCards ?? 3) cartas · \(model.eco?.seriesPackCoins ?? 900) monedas") {
                            model.openSeriesPack(series.id, gems: false)
                        }
                        Button("Sobre de \(model.eco?.seriesPackCards ?? 3) cartas · \(model.eco?.seriesPackGems ?? 45) gemas") {
                            model.openSeriesPack(series.id, gems: true)
                        }
                    } label: {
                        Text("Comprar sobre de esta serie")
                            .font(.system(size: 11, weight: .heavy, design: .rounded))
                            .foregroundColor(Theme.accent)
                    }
                }
            }
        }
        .padding(14)
        .card()
    }
}

/// Detalle de una pieza: qué hace, copias y qué puedes hacer con ella.
struct PieceDetail: View {
    @EnvironmentObject var model: AppModel
    let pieceID: String

    var body: some View {
        let piece = model.piece(pieceID)
        let album = model.album
        let copies = album?.copies[pieceID] ?? 0
        let foil = album?.foil.contains(pieceID) ?? false
        return ScrollView(showsIndicators: false) {
            VStack(spacing: 14) {
                Capsule().fill(Theme.ink.opacity(0.15)).frame(width: 40, height: 4).padding(.top, 10)
                if let piece = piece {
                    PieceCard(piece: piece, series: model.series(piece.series), owned: copies > 0, foil: foil, copies: copies, width: 180)
                    Text(copies > 0 ? piece.name : "Pieza misteriosa")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundColor(Theme.ink)
                    Text(rarityName(piece.rarity).uppercased())
                        .font(.system(size: 11, weight: .heavy))
                        .kerning(2)
                        .foregroundColor(rarityColor(piece.rarity))
                    if copies > 0 {
                        Text(piece.desc)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Theme.ink.opacity(0.65))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }
                    if let perk = piece.perk {
                        Text("Mejora: \(foil ? (piece.foilPerk ?? perk) : perk)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(Theme.accent)
                    }
                    actions(piece, copies: copies, foil: foil)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 30)
        }
        .background(Theme.cream.ignoresSafeArea())
    }

    @ViewBuilder
    private func actions(_ piece: AlbumPiece, copies: Int, foil: Bool) -> some View {
        let shards = model.album?.shards ?? 0
        VStack(spacing: 10) {
            if copies == 0 {
                BigButton(title: "CREAR · \(piece.craft) ESENCIA", color: Color(hex: 0xB57CF0), enabled: shards >= piece.craft) {
                    model.craftPiece(piece.id)
                }
                Text("Tienes \(shards) de esencia. Las repetidas se reciclan en esencia.")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Theme.ink.opacity(0.45))
            } else {
                if copies > 1 {
                    BigButton(title: "VENDER 1 REPETIDA · \(piece.sell) MONEDAS", color: Theme.accent) {
                        model.sellPiece(piece.id)
                    }
                    BigButton(title: "RECICLAR 1 · ESENCIA", color: Color(hex: 0xB57CF0)) {
                        model.recyclePiece(piece.id)
                    }
                }
                if !foil {
                    BigButton(title: "HACER BRILLANTE · \(piece.foilCost) ESENCIA", color: Theme.gold, enabled: shards >= piece.foilCost) {
                        model.foilPiece(piece.id)
                    }
                } else {
                    Text("✨ Brillante")
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundColor(Theme.gold)
                }
                showcaseButton(piece)
            }
        }
    }

    private func showcaseButton(_ piece: AlbumPiece) -> some View {
        let inShowcase = model.album?.showcase.contains(piece.id) ?? false
        let current = model.album?.showcase ?? []
        let slots = model.album?.slots ?? 3
        return Button(action: {
            if inShowcase {
                model.setShowcase(current.filter { $0 != piece.id })
            } else if current.count < slots {
                model.setShowcase(current + [piece.id])
            } else {
                model.showToast("Tu vitrina está llena: abre más huecos en el perfil")
            }
        }) {
            Text(inShowcase ? "Quitar de mi vitrina" : "Poner en mi vitrina")
                .font(.system(size: 14, weight: .heavy, design: .rounded))
                .foregroundColor(Theme.ink.opacity(0.7))
        }
    }
}
