import SwiftUI
import UIKit
import Shared

enum AppScreen {
    case main
    case game
}

enum PlayMode {
    case campaign
    case daily
    case tower
    case race
    case duel
    case custom
    case remote
}

enum DuelPhase {
    case playing
    case handover
    case result
}

enum MainTab: Int {
    case home
    case play
    case shop
    case album
    case profile
}

/// Hojas que se abren desde cualquier pantalla.
enum Sheet: Identifiable {
    case level(LevelCard)
    case season
    case missions
    case wheel
    case league
    case settings
    case lowFunds
    case custom
    case records
    case achievements
    case prestige
    case studio
    case remote
    case backup
    case friends
    case calendar

    var id: String {
        switch self {
        case .level(let card): return "level\(card.id)"
        case .season: return "season"
        case .missions: return "missions"
        case .wheel: return "wheel"
        case .league: return "league"
        case .settings: return "settings"
        case .lowFunds: return "lowFunds"
        case .custom: return "custom"
        case .records: return "records"
        case .achievements: return "achievements"
        case .prestige: return "prestige"
        case .studio: return "studio"
        case .remote: return "remote"
        case .backup: return "backup"
        case .friends: return "friends"
        case .calendar: return "calendar"
        }
    }
}

/// Avisos a pantalla completa que se van mostrando de uno en uno (regalos, cofres, skins nuevas...).
enum Celebration: Identifiable {
    case dailyGift
    case comeback(ComebackInfo)
    case milestone(StreakMilestoneInfo)
    case repair(RepairInfo)
    case chest(String, [DropInfo])
    case reveal(String)
    case leagueResult(LeaguePending)
    case info(String, String, String)
    case notifPrimer
    case whatsNew
    case profileSetup

    var id: String {
        switch self {
        case .dailyGift: return "gift"
        case .comeback: return "comeback"
        case .milestone(let m): return "milestone\(m.days)"
        case .repair: return "repair"
        case .chest(let type, let drops): return "chest\(type)\(drops.count)\(drops.first?.id ?? "")"
        case .reveal(let skin): return "reveal\(skin)"
        case .leagueResult(let p): return "league\(p.from)\(p.to)"
        case .info(let title, _, _): return "info\(title)"
        case .notifPrimer: return "notifPrimer"
        case .whatsNew: return "whatsNew"
        case .profileSetup: return "profileSetup"
        }
    }
}

/// Estado de la app. La partida la lleva `GameSession` y todo lo demás (monedas, tienda, cofres, álbum, misiones, pase, liga)
/// lo lleva `MetaSession`: las dos son Kotlin compartido con Android. Aquí solo se guarda el texto de estado y se traduce a vistas.
final class AppModel: ObservableObject {
    let session = GameSession()
    let meta = MetaSession()
    let defaults = UserDefaults.standard
    let sounds = SoundManager.shared

    @Published var screen: AppScreen = .main
    @Published var tab: MainTab = .home {
        didSet {
            if oldValue != tab { syncMusic() }
        }
    }
    @Published var snap: BoardSnap?
    @Published var hint: GuideHint?
    @Published var sheet: Sheet?
    @Published var intro: LevelCard?
    @Published var toast: String?
    @Published var dailyCard: LevelCard?
    @Published var reward: WinReward?
    @Published var loss: LossInfo?
    @Published var queue: [Celebration] = []

    // Modos de juego y poderes
    @Published var mode: PlayMode = .campaign
    @Published var achBanner: AchUnlock?
    var achQueue: [AchUnlock] = []
    @Published var tower: TowerInfo?
    @Published var towerWinInfo: TowerWinInfo?
    @Published var towerLossInfo: TowerLossInfo?
    @Published var raceStage: RaceStageInfo?
    @Published var raceCleared = 0
    @Published var raceFlash: Int?
    @Published var raceEnd: RaceEndInfo?
    @Published var duelPlayer = 1
    @Published var duelScores: [Int] = [0, 0]
    @Published var duelPhase: DuelPhase = .playing
    @Published var duelResult: DuelResultInfo?
    @Published var assistMessage: String?
    @Published var selectMode: String?
    /// Escoba o Unir que se quiere usar sin monedas suficientes: se ofrece verlo con un anuncio.
    @Published var adPowerKind: String?
    /// El próximo poder manual ya está pagado con un anuncio.
    var freePowerPending = false
    @Published var firstPick: String?
    @Published var lastClean: Date?
    @Published var lastMerge: Date?
    @Published var remoteChallenge: RemoteChallengeInfo?
    @Published var remoteCreate: RemoteCreateInfo?
    @Published var remoteResult: RemoteResultInfo?
    var remoteRole = "CREATOR"
    var remoteSeed: Int64 = 1
    var raceMerges = 0
    var raceMaxTile = 0
    var duelSeed: Int64 = 0
    var customSize = 4
    var customTarget = 512
    var customTimed = false
    var customFast = false
    var customTables = false

    @Published var state: MetaState?
    @Published var album: AlbumStateData?
    private(set) var stateAt = Date()

    // Catálogos (no cambian mientras la app está abierta)
    var skins: [SkinItem] = []
    private(set) var fxs: [FxItem] = []
    private(set) var avatars: [AvatarItem] = []
    private(set) var banners: [BannerItem] = []
    private(set) var albumCatalog = AlbumCatalogData(series: [], pieces: [])
    private(set) var tiers: [SeasonTierInfo] = []
    private(set) var wheelSlices: [WheelSliceInfo] = []
    @Published var store: StoreCatalogData?
    @Published var livePrices: [String: String] = [:]
    private(set) var eco: EconomyInfo?

    var cards: [Int: LevelCard] = [:]
    var idleSeconds: Double = 0
    var lastPhase: Int = 1
    var rewardedKey: String = ""
    var usedHelp = false
    var lastDailyDay: Int = 0
    var skinByID: [String: SkinItem] = [:]
    var pieceByID: [String: AlbumPiece] = [:]
    var avatarByID: [Int: AvatarItem] = [:]
    var bannerByID: [Int: BannerItem] = [:]
    var seriesByID: [String: AlbumSeries] = [:]

    init() {
        if let saved = defaults.string(forKey: "meta_state_v1") {
            meta.load(state: saved)
        }
        loadCatalogs()
        tickClock()
        migrateLegacyProgress()
        refreshDaily()
        openToday()
        refreshState()
        syncMusic()
        startStore()
    }

    // MARK: Carga

    func loadCatalogs() {
        meta.setLanguage(code: AppLanguage.code)
        skins = decodeJSON([SkinItem].self, meta.skinCatalog()) ?? []
        fxs = decodeJSON([FxItem].self, meta.fxCatalog()) ?? []
        avatars = decodeJSON([AvatarItem].self, meta.avatarCatalog()) ?? []
        banners = decodeJSON([BannerItem].self, meta.bannerCatalog()) ?? []
        albumCatalog = decodeJSON(AlbumCatalogData.self, meta.albumCatalog()) ?? AlbumCatalogData(series: [], pieces: [])
        wheelSlices = decodeJSON([WheelSliceInfo].self, meta.wheelSlices()) ?? []
        eco = decodeJSON(EconomyInfo.self, meta.economyInfo())
        for skin in skins { skinByID[skin.id] = skin }
        for piece in albumCatalog.pieces { pieceByID[piece.id] = piece }
        for series in albumCatalog.series { seriesByID[series.id] = series }
        for item in avatars { avatarByID[item.id] = item }
        for item in banners { bannerByID[item.id] = item }
    }

    /// El progreso de la primera versión (UserDefaults) pasa a la memoria compartida, una sola vez.
    func migrateLegacyProgress() {
        if defaults.bool(forKey: "meta_migrated") { return }
        let unlockedOld = defaults.integer(forKey: "unlocked")
        if unlockedOld > 1 {
            var level = 1
            while level <= unlockedOld {
                let stars = defaults.integer(forKey: "stars_\(level)")
                let best = defaults.integer(forKey: "best_\(level)")
                if stars > 0 {
                    meta.importLevel(level: Int32(level), stars: Int32(stars), bestMoves: Int32(best))
                }
                level += 1
            }
            meta.importUnlocked(level: Int32(unlockedOld))
        }
        if defaults.bool(forKey: "tutorial_done") { meta.markTutorialDone() }
        defaults.set(true, forKey: "meta_migrated")
        persist()
    }

    // MARK: Reloj y guardado

    func localDay() -> Int {
        let seconds = Date().timeIntervalSince1970 + Double(TimeZone.current.secondsFromGMT())
        return Int(floor(seconds / 86400.0))
    }

    func minuteOfDay() -> Int {
        let c = Calendar.current.dateComponents([.hour, .minute], from: Date())
        return (c.hour ?? 0) * 60 + (c.minute ?? 0)
    }

    func tickClock() {
        let ms = Int64(Date().timeIntervalSince1970 * 1000.0)
        meta.tick(epochDay: Int32(localDay()), nowMs: ms, minuteOfDay: Int32(minuteOfDay()))
    }

    func persist() {
        defaults.set(meta.save(), forKey: "meta_state_v1")
    }

    func refreshState() {
        tickClock()
        if let fresh = decodeJSON(MetaState.self, meta.state()) {
            state = fresh
            stateAt = Date()
        }
        album = decodeJSON(AlbumStateData.self, meta.albumState())
        store = decodeJSON(StoreCatalogData.self, meta.storeProducts())
        deliverPrestigeEvents()
        persist()
    }

    /// Ejecuta una acción de la memoria compartida, guarda y vuelve a leer el estado.
    @discardableResult
    func act(_ block: (MetaSession) -> String) -> String {
        tickClock()
        let json = block(meta)
        persist()
        refreshState()
        return json
    }

    /// Como `act`, pero devuelve si salió bien y avisa del motivo si no.
    @discardableResult
    func run(sound: Sfx? = .success_small, _ block: (MetaSession) -> String) -> Bool {
        let json = act(block)
        guard let result = decodeJSON(ActionResult.self, json) else { return false }
        if !result.ok, let reason = result.reason {
            showToast(reason)
            sounds.play(.error, volume: 0.8)
            Haptics.warning()
        } else if result.ok {
            Haptics.tap()
            if let sound = sound { sounds.play(sound) }
        }
        return result.ok
    }

    // MARK: Atajos de lectura

    var coins: Int { state?.coins ?? 0 }
    var gems: Int { state?.gems ?? 0 }
    var unlocked: Int { state?.unlocked ?? 1 }
    var streak: Int { state?.streak ?? 0 }
    var totalStars: Int { state?.totalStars ?? 0 }
    var levelsWon: Int { state?.levelsWon ?? 0 }
    var levelCount: Int { Int(session.levelCount()) }
    var hapticsOn: Bool { (defaults.object(forKey: "haptics_on") as? Bool) ?? true }

    func skin(_ id: String) -> SkinItem? { skinByID[id] }
    func piece(_ id: String) -> AlbumPiece? { pieceByID[id] }
    func series(_ id: String) -> AlbumSeries? { seriesByID[id] }
    func avatar(_ id: Int) -> AvatarItem? { avatarByID[id] }
    func banner(_ id: Int) -> BannerItem? { bannerByID[id] }

    func owns(skin id: String) -> Bool { state?.ownedSkins.contains(id) ?? false }

    var boardStyle: BoardStyle {
        BoardStyle(skin: skinByID[state?.equippedSkin ?? "jelly"])
    }

    func stars(_ level: Int) -> Int { Int(meta.starsOf(level: Int32(level))) }
    func best(_ level: Int) -> Int { Int(meta.bestMovesOf(level: Int32(level))) }

    func chapterStars(_ chapter: Int) -> Int { Int(meta.chapterStars(chapter: Int32(chapter))) }
    func chapterChestClaimed(_ chapter: Int) -> Bool { meta.chapterChestClaimed(chapter: Int32(chapter)) }

    /// Milisegundos que faltan para el cofre gratis, contando el tiempo que pasó desde la última lectura.
    func freeChestRemaining(now: Date) -> Int {
        guard let info = state?.freeChest else { return 0 }
        let passed = Int(now.timeIntervalSince(stateAt) * 1000.0)
        return max(0, info.remainingMs - passed)
    }

    // MARK: Ajustes

    func buzz(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        switch style {
        case .light: Haptics.tap()
        case .medium: Haptics.medium()
        case .heavy: Haptics.heavy()
        default: Haptics.soft()
        }
    }

    /// Cada pantalla tiene su música: partida, tienda y álbum, mapa y menú.
    func syncMusic() {
        if screen == .game {
            sounds.setMusic(.game)
            return
        }
        switch tab {
        case .shop, .album: sounds.setMusic(.shop)
        case .play: sounds.setMusic(.map)
        default: sounds.setMusic(.menu)
        }
    }

    func resetProgress() {
        let prefixes = ["stars_", "best_", "daily_", "seen_", "streak_", "meta_"]
        let exact = ["unlocked", "tutorial_done", "profile_setup_done"]
        for key in defaults.dictionaryRepresentation().keys {
            if exact.contains(key) || prefixes.contains(where: { key.hasPrefix($0) }) {
                defaults.removeObject(forKey: key)
            }
        }
        meta.resetAll()
        defaults.set(true, forKey: "meta_migrated")
        persist()
        refreshState()
        refreshDaily()
    }

    // MARK: Al abrir la app

    func openToday() {
        let json = act { $0.openApp() }
        guard let opened = decodeJSON(OpenAppResult.self, json) else { return }
        var list: [Celebration] = []
        if let gift = opened.comeback { list.append(.comeback(gift)) }
        if let milestone = opened.milestone { list.append(.milestone(milestone)) }
        if opened.repairLost > 0 {
            list.append(.repair(RepairInfo(lost: opened.repairLost, cost: opened.repairCost)))
        }
        for skinID in opened.reveals { list.append(.reveal(skinID)) }
        if opened.vipGems > 0 {
            list.append(.info("¡VIP!", "Tus gemas de hoy: +\(opened.vipGems)", "diamond.fill"))
        }
        if state?.dailyReward.claimable ?? false { list.append(.dailyGift) }
        queue.append(contentsOf: list)
        showWelcomeMessages()
    }

    /// Al volver a la app (otro día) se vuelve a mirar la racha y el regalo.
    func appBecameActive() {
        UIApplication.shared.applicationIconBadgeNumber = 0
        if localDay() != lastDailyDay {
            refreshDaily()
            openToday()
        } else {
            refreshState()
        }
    }

    // MARK: Avisos

    /// Pasa de una hoja a otra (primero se cierra la actual para que iOS no se confunda).
    func switchSheet(_ next: Sheet) {
        sheet = nil
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { [weak self] in
            self?.sheet = next
        }
    }

    func dismissCelebration() {
        if !queue.isEmpty { queue.removeFirst() }
    }

    func push(_ celebration: Celebration) {
        queue.append(celebration)
        switch celebration {
        case .dailyGift:
            sounds.play(.daily_gift)
        case .comeback:
            sounds.play(.welcome_back)
        case .milestone:
            sounds.play(.streak_up)
        case .repair:
            sounds.play(.popup)
        case .chest(let type, _):
            switch type {
            case "RARE": sounds.play(.chest_open_rare)
            case "EPIC", "SERIES": sounds.play(.chest_open_epic)
            case "LEGENDARY":
                sounds.play(.chest_open_legendary)
                sounds.duckMusic(for: 5.0)
            default: sounds.play(.chest_open_common)
            }
            Haptics.chestOpen()
        case .reveal:
            sounds.play(.unlock)
        case .leagueResult:
            sounds.play(.tier_up)
        case .info:
            sounds.play(.badge)
        default:
            sounds.play(.popup)
        }
    }

    func showToast(_ text: String) {
        let shown = loc(text)
        toast = shown
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { [weak self] in
            if self?.toast == shown { self?.toast = nil }
        }
    }

    // MARK: Racha, regalo y cofres

    func claimDailyGift() {
        let json = act { $0.claimDailyReward() }
        if let result = decodeJSON(DailyGiftResult.self, json), result.ok {
            sounds.play(.claim)
            Haptics.success()
        }
    }

    func repairStreak() { run(sound: .streak_saved) { $0.repairStreak() } }
    func declineRepair() { act { $0.declineRepair() } }

    func claimFreeChest() {
        let json = act { $0.claimFreeChest() }
        if let result = decodeJSON(ActionResult.self, json), result.ok {
            sounds.play(.claim)
            Haptics.success()
            showToast("¡Cofre gratis en tu inventario!")
        }
    }

    func skipChestWithGems() { run(sound: .unlock) { $0.skipFreeChestWithGems() } }

    func openChest(_ type: String) {
        let json = act { $0.openChest(type: type, seed: Int64(Date().timeIntervalSince1970 * 1000.0)) }
        guard let result = decodeJSON(ChestOpenResult.self, json) else { return }
        if result.ok, let drops = result.drops {
            push(.chest(type, drops))
            for skinID in result.reveals ?? [] { push(.reveal(skinID)) }
        } else if let reason = result.reason {
            showToast(reason)
        }
    }

    func buyChest(_ type: String, gems withGems: Bool) { run(sound: .purchase) { $0.buyChest(type: type, withGems: withGems) } }

    /// Gira la ruleta. Devuelve la casilla ganadora (el premio ya está entregado).
    func spinWheel(ad: Bool = false) -> Int? {
        let seed = Int64(Date().timeIntervalSince1970 * 1000.0)
        let json = act { ad ? $0.adWheelSpin(seed: seed) : $0.spinWheel(seed: seed) }
        guard let result = decodeJSON(WheelResult.self, json), result.ok else { return nil }
        sounds.play(.wheel_spin)
        Haptics.medium()
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) { [weak self] in
            self?.sounds.play(.wheel_stop)
            Haptics.heavy()
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.35) { [weak self] in
            self?.sounds.play(.wheel_win)
            Haptics.success()
        }
        return result.index
    }

    // MARK: Misiones, pase y liga

    func claimMission(_ id: Int) {
        let json = act { $0.claimMission(id: Int32(id)) }
        guard let result = decodeJSON(MissionClaimResult.self, json), result.ok else { return }
        sounds.play(.mission_done)
        Haptics.success()
        if result.allDone ?? false {
            push(.info("¡Día perfecto!", "Cobraste las tres misiones: cofre, gemas y ficha de intercambio.", "checkmark.seal.fill"))
        }
    }

    func claimWeekly(_ id: String) { run(sound: .claim) { $0.claimWeekly(id: id) } }
    func claimWeeklyBonus() { run(sound: .claim_all) { $0.claimWeeklyBonus() } }

    func claimTier(_ tier: Int, premium: Bool) {
        let json = act { $0.claimTier(tier: Int32(tier), premium: premium) }
        if let result = decodeJSON(TierClaimResult.self, json), result.ok {
            sounds.play(.tier_up)
            Haptics.success()
        }
    }

    func claimAllTiers() { run(sound: .claim_all) { $0.claimAllTiers() } }
    func buySeasonTier() { run(sound: .tier_up) { $0.buySeasonTier() } }

    /// Pase premium de la temporada: en la versión de prueba la "compra" no cobra.
    func unlockPremium() {
        if run({ $0.testBuyProduct(id: "season_pass") }) {
            showToast("Pase premium activado (versión de prueba)")
        }
    }

    func loadTiers() {
        tiers = decodeJSON([SeasonTierInfo].self, meta.seasonTiers()) ?? []
    }

    func claimLeague() {
        let json = act { $0.claimLeague() }
        if let result = decodeJSON(ActionResult.self, json), result.ok {
            sounds.play(.league_promote)
            Haptics.success()
        }
    }

    // MARK: Tienda

    func buySkin(_ id: String, discount: Int = 0) { run(sound: .purchase) { $0.buySkin(id: id, discount: Int32(discount)) } }
    func equipSkin(_ id: String) { run(sound: .select) { $0.equipSkin(id: id) } }
    func buyFx(_ id: String) { run(sound: .purchase) { $0.buyFx(id: id, discount: 0) } }
    func equipFx(_ id: String) { run(sound: .select) { $0.equipFx(id: id) } }
    func buyAvatar(_ id: Int) { run(sound: .purchase) { $0.buyAvatar(id: Int32(id)) } }
    func buyBanner(_ id: Int) { run(sound: .purchase) { $0.buyBanner(id: Int32(id)) } }
    func buyUndos(_ pack: Int) { run(sound: .purchase) { $0.buyUndos(pack: Int32(pack)) } }
    func buyExtraTimes(_ pack: Int) { run(sound: .purchase) { $0.buyExtraTimes(pack: Int32(pack)) } }
    func buyFreeze() { run(sound: .purchase) { $0.buyFreeze() } }
    func buyBoost() { run(sound: .purchase) { $0.buyCoinBoost() } }
    func exchangeGems(_ gems: Int) { run(sound: .purchase) { $0.exchangeGems(gems: Int32(gems)) } }
    func buyDailyOffer() { run(sound: .purchase) { $0.buyDailyOffer() } }
    func buyEventSkin(_ eventID: String) { run(sound: .purchase) { $0.buyEventSkin(eventId: eventID) } }
    func testBuyProduct(_ id: String) { run(sound: .purchase) { $0.testBuyProduct(id: id) } }

    func setAvatar(_ id: Int) {
        tickClock()
        if meta.setAvatar(id: Int32(id)) {
            persist()
            refreshState()
        } else {
            showToast("Aún no tienes ese avatar")
        }
    }

    func setBanner(_ id: Int) {
        tickClock()
        if meta.setBanner(id: Int32(id)) {
            persist()
            refreshState()
        } else {
            showToast("Aún no tienes ese banner")
        }
    }

    func setName(_ name: String) {
        meta.setProfileName(name: name)
        persist()
        refreshState()
    }

    // MARK: Álbum

    func sellPiece(_ id: String, qty: Int = 1) { run(sound: .coin) { $0.sellPiece(id: id, qty: Int32(qty)) } }
    func sellAll(_ rarity: String) { run(sound: .coins_small) { $0.sellAll(maxRarity: rarity) } }
    func recyclePiece(_ id: String, qty: Int = 1) { run(sound: .shard) { $0.recyclePiece(id: id, qty: Int32(qty)) } }
    func craftPiece(_ id: String) { run(sound: .unlock) { $0.craftPiece(id: id) } }
    func foilPiece(_ id: String) { run(sound: .card_foil) { $0.foilPiece(id: id) } }
    func buyShardPack() { run(sound: .purchase) { $0.buyShardPack() } }
    func buyTradeToken() { run(sound: .purchase) { $0.buyTradeToken() } }
    func claimSeriesReward(_ id: String) { run(sound: .series_complete) { $0.claimSeriesReward(seriesId: id) } }
    func claimAlbumReward() { run(sound: .album_complete) { $0.claimAlbumReward() } }
    func unlockShowcaseSlot() { run(sound: .unlock) { $0.unlockShowcaseSlot() } }

    func setShowcase(_ ids: [String]) {
        meta.setShowcase(ids: ids.joined(separator: ","))
        persist()
        refreshState()
    }

    func openSeriesPack(_ seriesID: String, gems withGems: Bool) {
        let json = act { $0.openSeriesPack(seriesId: seriesID, withGems: withGems, seed: Int64(Date().timeIntervalSince1970 * 1000.0)) }
        guard let result = decodeJSON(ChestOpenResult.self, json) else { return }
        if result.ok, let drops = result.drops {
            push(.chest("SERIES", drops))
        } else if let reason = result.reason {
            showToast(reason)
        }
    }

    // MARK: Niveles

    func card(_ level: Int) -> LevelCard? {
        if let cached = cards[level] { return cached }
        guard let fresh = decodeJSON(LevelCard.self, session.levelInfo(level: Int32(level))) else { return nil }
        cards[level] = fresh
        return fresh
    }

    func openPreview(_ level: Int) {
        if let card = card(level) { sheet = .level(card) }
    }

    func refreshDaily() {
        let day = localDay()
        lastDailyDay = day
        dailyCard = decodeJSON(LevelCard.self, session.dailyInfo(epochDay: Int32(day)))
    }

    var dailyDone: Bool { defaults.bool(forKey: "daily_done_\(localDay())") }
    var dailyStars: Int { defaults.integer(forKey: "daily_stars_\(localDay())") }

    func start(_ level: Int) {
        mode = .campaign
        session.tutorialEnabled = level == 1 && !(state?.tutorialDone ?? false)
        let json = act { $0.prepareLevel(level: Int32(level)) }
        let assist = decodeJSON(AssistInfo.self, json)
        session.startAssisted(level: Int32(level), percent: Int32(assist?.percent ?? 100))
        assistMessage = assist?.message
        begin(introFor: card(level), key: "level_\(level)")
    }

    func startDaily() {
        mode = .daily
        if localDay() != lastDailyDay { refreshDaily() }
        session.tutorialEnabled = false
        session.startDaily(epochDay: Int32(localDay()))
        assistMessage = nil
        begin(introFor: dailyCard, key: "daily_\(localDay())")
    }

    func begin(introFor card: LevelCard?, key: String) {
        sheet = nil
        hint = nil
        toast = nil
        reward = nil
        loss = nil
        towerWinInfo = nil
        towerLossInfo = nil
        raceFlash = nil
        selectMode = nil
        firstPick = nil
        idleSeconds = 0
        lastPhase = 1
        rewardedKey = ""
        usedHelp = false
        refresh(animated: false)
        intro = needsIntro(card) ? card : nil
        screen = .game
        syncMusic()
        if let message = assistMessage { showToast(message) }
    }

    /// La primera vez que sale cada tipo de regla se explica; los jefes se explican una vez cada uno.
    func needsIntro(_ card: LevelCard?) -> Bool {
        guard let card = card, card.kind != "ZEN" else { return false }
        let key = card.boss ? "seen_boss_\(card.id)_\(card.title)" : "seen_kind_\(card.kind)"
        return !defaults.bool(forKey: key)
    }

    func dismissIntro() {
        if let card = intro {
            let key = card.boss ? "seen_boss_\(card.id)_\(card.title)" : "seen_kind_\(card.kind)"
            defaults.set(true, forKey: key)
        }
        intro = nil
        idleSeconds = 0
    }

    func restart() {
        guard let current = snap else { return }
        switch mode {
        case .tower: startTowerFloor()
        case .race: startRace()
        case .duel: startDuelRound()
        case .custom:
            if customTables { startTables() } else { startCustom(size: customSize, target: customTarget, timed: customTimed, fast: customFast) }
        case .remote: restartRemote()
        case .daily: startDaily()
        case .campaign: start(current.level)
        }
    }

    func nextLevel() {
        guard let current = snap else { return }
        switch mode {
        case .tower:
            act { m in
                _ = m.towerNext()
                return "{}"
            }
            tower = decodeJSON(TowerInfo.self, meta.towerInfo())
            startTowerFloor()
        case .campaign:
            start(min(current.level + 1, levelCount))
        default:
            backToMenu()
        }
    }

    func backToMap() {
        tab = .play
        screen = .main
        syncMusic()
    }

    func backToMenu() {
        tab = .home
        screen = .main
        refreshDaily()
        refreshState()
        syncMusic()
    }

    // MARK: Jugar

    func swipe(_ direction: Int) {
        guard intro == nil, let current = snap, current.status == "playing" else { return }
        idleSeconds = 0
        hint = nil
        let moved = session.move(direction: Int32(direction))
        refresh(animated: true)
        guard let now = snap else { return }
        if moved {
            checkAchievements(now)
            announce(moveAnnouncement(now))
        }
        if moved {
            let mergedTiles = now.tiles.filter { $0.merged }
            if let biggest = mergedTiles.map({ $0.v }).max() {
                sounds.playMerge(value: biggest, combo: mergedTiles.count - 1)
                Haptics.merge(level: SoundManager.hapticLevel(value: biggest))
            } else {
                sounds.play("move_pop", volume: 0.5)
                Haptics.soft()
            }
        }
        if !now.blocked.isEmpty {
            showToast(now.blocked)
        } else if moved && now.phase == 2 && lastPhase == 1 {
            showToast(now.phaseTitle)
        }
        lastPhase = now.phase
    }

    /// Deshacer gasta uno de los "Deshacer" del inventario (se compran con monedas, como en Android).
    func undo() {
        guard let current = snap, current.canUndo else { return }
        if (state?.undos ?? 0) <= 0 {
            sheet = .lowFunds
            return
        }
        tickClock()
        if meta.useUndo() && session.undoMove() {
            usedHelp = true
            hint = nil
            persist()
            refreshState()
            refresh(animated: true)
        }
    }

    func addExtraTime() {
        guard let current = snap, current.status == "playing", current.timeLeftMs != nil else { return }
        if (state?.extraTimes ?? 0) <= 0 {
            sheet = .lowFunds
            return
        }
        tickClock()
        if meta.useExtraTime() {
            _ = session.addExtraTime(ms: 20000)
            usedHelp = true
            persist()
            refreshState()
            refresh(animated: false)
        }
    }

    func showHint() {
        guard let current = snap, current.status == "playing" else { return }
        usedHelp = true
        hint = decodeJSON(GuideHint.self, session.hint())
    }

    /// Se llama cuatro veces por segundo mientras se juega: mueve el reloj y enseña una pista si te quedas parado.
    func tick() {
        guard screen == .game, intro == nil, let current = snap, current.status == "playing" else { return }
        session.tick(deltaMs: 250)
        idleSeconds += 0.25
        if current.timeLeftMs != nil {
            refresh(animated: false)
        }
        if hint == nil && current.level <= 15 && !current.daily && current.coach == nil && idleSeconds >= 7 {
            hint = decodeJSON(GuideHint.self, session.hint())
        }
    }

    func refresh(animated: Bool) {
        guard let fresh = decodeJSON(BoardSnap.self, session.snapshot()) else { return }
        let before = snap?.status
        if animated {
            withAnimation(.spring(response: 0.26, dampingFraction: 0.78)) {
                snap = fresh
            }
        } else {
            snap = fresh
        }
        if fresh.status == "playing" {
            let timeRunningOut = (fresh.timeLeftMs ?? Int.max) < 20_000
            var level = 0.0
            if fresh.flow >= 1 || fresh.combo >= 2 { level = 0.5 }
            if fresh.flow >= 2 || timeRunningOut || fresh.empty <= 2 { level = 1.0 }
            sounds.setIntensity(level)
        } else {
            sounds.setIntensity(0)
        }
        if before == "playing" && fresh.status != "playing" {
            if fresh.status == "won" {
                sounds.play(.result_win)
                sounds.duckMusic(for: 3.2)
                Haptics.success()
                let stars = max(1, min(3, fresh.stars))
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { [weak self] in
                    self?.sounds.play(stars >= 3 ? .stars_3 : (stars == 2 ? .stars_2 : .stars_1))
                }
            } else {
                sounds.play(.result_lose)
                Haptics.error()
            }
            finish(fresh)
        }
    }

    /// Al terminar la partida se pagan los premios (una sola vez por partida).
    func finish(_ s: BoardSnap) {
        let key = "\(s.daily)_\(s.level)_\(s.moves)_\(s.score)_\(s.status)"
        guard rewardedKey != key else { return }
        rewardedKey = key
        switch mode {
        case .tower: finishTower(s)
        case .race: finishRace(s)
        case .duel: finishDuel(s)
        case .custom: finishCustom(s)
        case .remote: finishRemote(s)
        default: finishCampaign(s)
        }
    }

    func finishCampaign(_ s: BoardSnap) {
        if s.status == "won" {
            let json = act { m in
                m.onWin(
                    level: Int32(s.level), daily: s.daily, stars: Int32(s.stars), moves: Int32(s.moves),
                    timeMs: Int64(s.elapsedMs), maxTile: Int32(s.maxTile), merges: Int32(s.merges), usedHelp: usedHelp,
                    kind: s.kind, boss: s.kind == "BOSS", flow: Int32(s.flow)
                )
            }
            reward = decodeJSON(WinReward.self, json)
            if s.tutorialDone { defaults.set(true, forKey: "tutorial_done") }
            if s.daily {
                defaults.set(true, forKey: "daily_done_\(localDay())")
                if s.stars > dailyStars { defaults.set(s.stars, forKey: "daily_stars_\(localDay())") }
            }
            if hapticsOn {
                UINotificationFeedbackGenerator().notificationOccurred(.success)
            }
            if let won = reward {
                for skinID in won.newSkins { push(.reveal(skinID)) }
            }
            maybeAskForNotifications()
            maybeAskForProfile(s)
            maybeAskForReview()
        } else {
            let json = act { m in
                m.onLoss(level: Int32(s.level), daily: s.daily, maxTile: Int32(s.maxTile), merges: Int32(s.merges))
            }
            loss = decodeJSON(LossInfo.self, json)
        }
    }
}
