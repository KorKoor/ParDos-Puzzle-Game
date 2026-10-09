import SwiftUI

/// El tablero: casillas, piedras, fichas animadas por su identificador y la guía (halos + mano) del tutorial o de las pistas.
struct BoardView: View {
    @EnvironmentObject var model: AppModel
    let snap: BoardSnap
    let hint: GuideHint?

    var body: some View {
        GeometryReader { geo in
            let side = min(geo.size.width, geo.size.height)
            BoardCanvas(snap: snap, hint: hint, side: side, style: model.boardStyle)
                .frame(width: side, height: side)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 18)
                        .onEnded { value in handleSwipe(value.translation) }
                )
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private func handleSwipe(_ t: CGSize) {
        if abs(t.width) > abs(t.height) {
            model.swipe(t.width > 0 ? 3 : 2)
        } else {
            model.swipe(t.height > 0 ? 1 : 0)
        }
    }
}

struct BoardCanvas: View {
    let snap: BoardSnap
    let hint: GuideHint?
    let side: CGFloat
    let style: BoardStyle

    private var gap: CGFloat { snap.size >= 5 ? 6 : 8 }
    private var cell: CGFloat { (side - gap * CGFloat(snap.size + 1)) / CGFloat(snap.size) }

    private func x(_ col: Int) -> CGFloat { gap + CGFloat(col) * (cell + gap) + cell / 2 }
    private func y(_ row: Int) -> CGFloat { gap + CGFloat(row) * (cell + gap) + cell / 2 }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill((style.ink ?? Theme.ink).opacity(0.12))
            emptyCells
            stoneCells
            tileViews
            guide
        }
        .frame(width: side, height: side)
    }

    private var emptyCells: some View {
        ForEach(0..<(snap.size * snap.size), id: \.self) { index in
            RoundedRectangle(cornerRadius: cell * 0.2, style: .continuous)
                .fill((style.surface ?? Color.white).opacity(0.55))
                .frame(width: cell, height: cell)
                .position(x: x(index % snap.size), y: y(index / snap.size))
        }
    }

    private var stoneCells: some View {
        ZStack {
            ForEach(0..<snap.stones.count, id: \.self) { i in
                StoneView(size: cell, life: nil)
                    .position(x: x(snap.stones[i][1]), y: y(snap.stones[i][0]))
            }
            ForEach(0..<snap.storm.count, id: \.self) { i in
                StoneView(size: cell, life: snap.storm[i][2])
                    .position(x: x(snap.storm[i][1]), y: y(snap.storm[i][0]))
            }
        }
    }

    private var tileViews: some View {
        ZStack {
            ForEach(snap.tiles) { tile in
                TileView(tile: tile, size: cell, style: style)
                    .position(x: x(tile.c), y: y(tile.r))
                    .transition(.scale)
            }
        }
    }

    @ViewBuilder
    private var guide: some View {
        if let hint = hint {
            ZStack {
                ForEach(0..<hint.cells.count, id: \.self) { i in
                    RoundedRectangle(cornerRadius: cell * 0.2, style: .continuous)
                        .stroke(Theme.gold, lineWidth: 4)
                        .frame(width: cell, height: cell)
                        .position(x: x(hint.cells[i][1]), y: y(hint.cells[i][0]))
                }
                GuideHandView(direction: hint.dir, start: guideStart(hint), travel: cell * 1.5)
            }
            .allowsHitTesting(false)
        }
    }

    /// Desde dónde sale la mano: el centro de las fichas resaltadas, o el centro del tablero.
    private func guideStart(_ hint: GuideHint) -> CGPoint {
        if hint.cells.isEmpty { return CGPoint(x: side / 2, y: side / 2) }
        var sx: CGFloat = 0
        var sy: CGFloat = 0
        for c in hint.cells {
            sx += x(c[1])
            sy += y(c[0])
        }
        return CGPoint(x: sx / CGFloat(hint.cells.count), y: sy / CGFloat(hint.cells.count))
    }
}

struct TileView: View {
    let tile: TileSnap
    let size: CGFloat
    let style: BoardStyle
    @State private var pop: CGFloat = 1

    private var corner: CGFloat { size * 0.2 }
    private var fillColor: Color { style.fill(tile.v) }
    private var isNeon: Bool { style.finish == "NEON" }

    var body: some View {
        ZStack {
            base
            finishOverlay
            Text("\(tile.v)")
                .font(.system(size: size * fontFactor, weight: .black, design: .rounded))
                .foregroundColor(isNeon ? fillColor : style.text(tile.v))
                .minimumScaleFactor(0.5)
                .lineLimit(1)
        }
        .frame(width: size, height: size)
        .shadow(color: Color.black.opacity(style.finish == "FLAT" ? 0 : 0.12), radius: 0, x: 0, y: 3)
        .scaleEffect(pop)
        .onAppear {
            if tile.new {
                pop = 0.3
                withAnimation(.spring(response: 0.3, dampingFraction: 0.55)) { pop = 1 }
            }
        }
        .onChange(of: tile.v) { _ in
            pop = 1.18
            withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) { pop = 1 }
        }
    }

    @ViewBuilder
    private var base: some View {
        if isNeon {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .fill(Color.black.opacity(0.6))
                .overlay(RoundedRectangle(cornerRadius: corner, style: .continuous).stroke(fillColor, lineWidth: 3))
                .shadow(color: fillColor.opacity(0.8), radius: 6, x: 0, y: 0)
        } else if style.finish == "GLASS" {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .fill(fillColor.opacity(0.62))
                .overlay(RoundedRectangle(cornerRadius: corner, style: .continuous).stroke(Color.white.opacity(0.7), lineWidth: 1.5))
        } else if style.finish == "METAL" {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .fill(LinearGradient(colors: [fillColor.opacity(0.75), fillColor, fillColor.opacity(0.8)], startPoint: .topLeading, endPoint: .bottomTrailing))
        } else {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .fill(fillColor)
        }
    }

    @ViewBuilder
    private var finishOverlay: some View {
        if style.finish == "JELLY" || style.finish == "PORCELAIN" {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .fill(LinearGradient(colors: [Color.white.opacity(0.30), Color.clear], startPoint: .top, endPoint: .center))
        } else if style.finish == "WOOD" {
            VStack(spacing: size * 0.12) {
                ForEach(0..<4, id: \.self) { _ in
                    Capsule().fill(Color.black.opacity(0.07)).frame(height: 1.5)
                }
            }
            .padding(.horizontal, size * 0.1)
        } else if style.finish == "METAL" {
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .stroke(Color.white.opacity(0.35), lineWidth: 1.5)
        }
    }

    private var fontFactor: CGFloat {
        let digits = String(tile.v).count
        if digits <= 2 { return 0.5 }
        if digits == 3 { return 0.4 }
        if digits == 4 { return 0.34 }
        return 0.28
    }
}

struct StoneView: View {
    let size: CGFloat
    let life: Int?

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
                .fill(LinearGradient(colors: [Color(hex: 0xC9CFDB), Color(hex: 0x8C95A8)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .padding(size * 0.06)
            if let life = life {
                Text("\(max(life, 0))")
                    .font(.system(size: size * 0.34, weight: .black, design: .rounded))
                    .foregroundColor(Color(hex: 0x4A5164))
            }
        }
        .frame(width: size, height: size)
        .shadow(color: Color.black.opacity(0.18), radius: 0, x: 0, y: 3)
    }
}

/// La mano que enseña el deslizamiento: se hunde, desliza hacia donde toca y vuelve a empezar.
/// La punta del dedo (en la imagen, a un 39 % del ancho y un 7 % del alto) es el punto que recorre el camino.
struct GuideHandView: View {
    let direction: Int
    let start: CGPoint
    let travel: CGFloat
    @State private var go = false

    private let handSize: CGFloat = 78
    private let tipX: CGFloat = 0.39
    private let tipY: CGFloat = 0.07

    private var vector: CGSize {
        switch direction {
        case 0: return CGSize(width: 0, height: -1)
        case 1: return CGSize(width: 0, height: 1)
        case 2: return CGSize(width: -1, height: 0)
        default: return CGSize(width: 1, height: 0)
        }
    }

    private var rotation: Double {
        switch direction {
        case 0: return 0
        case 1: return 180
        case 2: return -90
        default: return 90
        }
    }

    /// Dónde está la punta del dedo en este momento del gesto.
    private var tipPoint: CGPoint {
        let factor: CGFloat = go ? 1 : 0
        let dx: CGFloat = vector.width * travel * factor
        let dy: CGFloat = vector.height * travel * factor
        return CGPoint(x: start.x + dx, y: start.y + dy)
    }

    var body: some View {
        let tip = tipPoint
        let centerX = tip.x + (0.5 - tipX) * handSize
        let centerY = tip.y + (0.5 - tipY) * handSize
        return Image("ico_hand")
            .resizable()
            .scaledToFit()
            .frame(width: handSize, height: handSize)
            .shadow(color: Color.black.opacity(0.35), radius: 4, x: 2, y: 3)
            .rotationEffect(.degrees(rotation), anchor: UnitPoint(x: tipX, y: tipY))
            .scaleEffect(go ? 0.92 : 1.12, anchor: UnitPoint(x: tipX, y: tipY))
            .opacity(go ? 0.25 : 1)
            .position(x: centerX, y: centerY)
            .onAppear {
                withAnimation(Animation.easeInOut(duration: 1.2).repeatForever(autoreverses: false)) {
                    go = true
                }
            }
    }
}
