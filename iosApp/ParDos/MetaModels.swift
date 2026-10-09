import Foundation

// Lo que el "meta" compartido (Kotlin: MetaSession) entrega a la interfaz: monedas, tienda, cofres, álbum, misiones, pase, liga.

struct ChestCounts: Decodable {
    let COMMON: Int
    let RARE: Int
    let EPIC: Int

    func count(_ type: String) -> Int {
        if type == "RARE" { return RARE }
        if type == "EPIC" { return EPIC }
        return COMMON
    }
}

struct DailyRewardInfo: Decodable {
    let claimable: Bool
    let day: Int
    let coins: Int
    let gems: Int
    let isChest: Bool
}

struct CycleDay: Decodable {
    let coins: Int
    let gems: Int
    let isChest: Bool
}

struct FreeChestInfo: Decodable {
    let ready: Bool
    let remainingMs: Int
    let type: String
    let skipCost: Int
}

struct WheelInfo: Decodable {
    let freeLeft: Int
    let adLeft: Int
}

struct MissionInfo: Decodable, Identifiable {
    let id: Int
    let desc: String
    let target: Int
    let progress: Int
    let done: Bool
    let claimed: Bool
    let coins: Int
    let type: String
}

struct WeeklyInfo: Decodable, Identifiable {
    let id: String
    let title: String
    let target: Int
    let progress: Int
    let done: Bool
    let claimed: Bool
    let coins: Int
}

struct SeasonInfo: Decodable {
    let id: Int
    let name: String
    let daysLeft: Int
    let points: Int
    let tier: Int
    let pointsInTier: Int
    let premium: Bool
    let claimable: Int
    let skipCost: Int
    let claimed: [String]
    let skin: String
}

struct LeaguePending: Decodable {
    let outcome: String
    let from: String
    let to: String
    let stars: Int
    let coins: Int
    let gems: Int
    let chest: String?
}

struct LeagueInfo: Decodable {
    let id: String
    let name: String
    let next: String?
    let weekStars: Int
    let promote: Int
    let keep: Int
    let weekDaysLeft: Int
    let pending: LeaguePending?
}

struct OfferInfo: Decodable {
    let kind: String
    let id: String
    let discount: Int
    let coin: Int
    let gem: Int
    let owned: Bool
}

struct EventInfo: Decodable, Identifiable {
    let id: String
    let name: String
    let coinMult: Double
    let xpMult: Double
    let daysLeft: Int
}

struct EventSkinInfo: Decodable, Identifiable {
    let id: String
    let skin: String?
    let name: String
    let wins: Int
    let need: Int
    let daysLeft: Int
    let owned: Bool
}

struct NextGoalInfo: Decodable {
    let title: String
    let detail: String
    let progress: Double
    let kind: String
}

struct RepairInfo: Decodable {
    let lost: Int
    let cost: Int
}

struct MetaState: Decodable {
    let coins: Int
    let gems: Int
    let shards: Int
    let tokens: Int
    let undos: Int
    let freezes: Int
    let extraTimes: Int
    let vip: Bool
    let boostWins: Int
    let piggy: Int
    let canBreakPiggy: Bool
    let chests: ChestCounts
    let totalChests: Int
    let streak: Int
    let bestStreak: Int
    let winStreak: Int
    let playerLevel: Int
    let xp: Int
    let xpNext: Int
    let name: String
    let avatar: Int
    let banner: Int
    let unlocked: Int
    let totalStars: Int
    let levelsWon: Int
    let tutorialDone: Bool
    let ownedSkins: [String]
    let equippedSkin: String
    let ownedFx: [String]
    let equippedFx: String
    let ownedAvatars: [Int]
    let ownedBanners: [Int]
    let dailyReward: DailyRewardInfo
    let cycle: [CycleDay]
    let freeChest: FreeChestInfo
    let wheel: WheelInfo
    let missions: [MissionInfo]
    let perfectDays: Int
    let perfectToday: Bool
    let weekly: [WeeklyInfo]
    let weeklyBonusReady: Bool
    let weeklyBonusClaimed: Bool
    let season: SeasonInfo
    let league: LeagueInfo
    let offer: OfferInfo
    let events: [EventInfo]
    let eventSkins: [EventSkinInfo]
    let nextGoal: NextGoalInfo?
    let badges: Int
    let repair: RepairInfo?
    let coinPercent: Int
    let dayOfWeek: Int
}

// MARK: - Catálogos

struct SkinItem: Decodable, Identifiable {
    let id: String
    let name: String
    let rarity: String
    let source: String
    let coin: Int
    let gem: Int
    let exclusive: Bool
    let free: Bool
    let finish: String
    let palette: [Int]?
    let darkText: Int
    let lightText: Int
    let lightFrom: Int
    let bgTop: Int?
    let bgBottom: Int?
    let ink: Int?
    let accent: Int?
    let surface: Int?
    let particles: String
    let particleTint: Int
    let hint: String?
    let event: String?
}

struct FxItem: Decodable, Identifiable {
    let id: String
    let name: String
    let blurb: String
    let rarity: String
    let coin: Int
    let gem: Int
    let source: String
    let buyable: Bool
}

struct AvatarItem: Decodable, Identifiable {
    let id: Int
    let name: String
    let source: String
    let coin: Int
    let animal: String?
    let accessory: String
    let variant: String
    let scene: String
    let frame: String
    let rarity: String
    let rank: String?
    let halloween: Bool
}

struct BannerItem: Decodable, Identifiable {
    let id: Int
    let name: String
    let source: String
    let pattern: String
    let top: Int
    let bottom: Int
    let accent: Int
    let ink: Int
    let coin: Int
    let gem: Int
    let rarity: String
    let rank: String?
    let dark: Bool
    let halloween: Bool
}

struct AlbumSeries: Decodable, Identifiable {
    let id: String
    let name: String
    let glyph: String
    let top: Int
    let bottom: Int
    let accent: Int
    let motif: String
    let perk: String
    let perkLabel: String
    let coins: Int
    let gems: Int
}

struct AlbumPiece: Decodable, Identifiable {
    let id: String
    let series: String
    let rarity: String
    let glyph: String
    let name: String
    let desc: String
    let n: Int
    let perk: String?
    let foilPerk: String?
    let craft: Int
    let sell: Int
    let foilCost: Int
}

struct AlbumCatalogData: Decodable {
    let series: [AlbumSeries]
    let pieces: [AlbumPiece]
}

struct SeasonRewardInfo: Decodable {
    let coins: Int
    let gems: Int
    let chest: String?
    let freezes: Int
    let undos: Int
    let skin: String?
    let avatar: Int
    let banner: Int
    let fx: String?
    let tokens: Int
}

struct SeasonTierInfo: Decodable, Identifiable {
    let tier: Int
    let free: SeasonRewardInfo
    let premium: SeasonRewardInfo

    var id: Int { tier }
}

struct WheelSliceInfo: Decodable {
    let kind: String
    let amount: Int
    let chest: String?
    let label: String
    let weight: Int
}

struct EconomyInfo: Decodable {
    let undoPrice: Int
    let extraTimePrice: Int
    let freezePrice: Int
    let maxFreezes: Int
    let commonChestCoins: Int
    let rareChestCoins: Int
    let rareChestGems: Int
    let epicChestGems: Int
    let boostPrice: Int
    let boostWins: Int
    let boostPercent: Int
    let coinsPerGem: Int
    let seasonTiers: Int
    let pointsPerTier: Int
    let eventSkinGems: Int
    let eventWins: Int
    let shardPackShards: Int
    let shardPackGems: Int
    let shardPacksPerDay: Int
    let tokenGems: Int
    let tokensPerDay: Int
    let seriesPackCoins: Int
    let seriesPackGems: Int
    let seriesPackCards: Int
    let albumGems: Int
}

struct GemPackInfo: Decodable, Identifiable {
    let id: String
    let gems: Int
    let usdCents: Int
    let bonus: Int
    let first: Bool
}

struct PerkInfo: Decodable, Identifiable {
    let kind: String
    let label: String
    let tenths: Int
    let cap: Int

    var id: String { kind }
}

struct AlbumStateData: Decodable {
    let copies: [String: Int]
    let foil: [String]
    let owned: Int
    let total: Int
    let shards: Int
    let tokens: Int
    let claimedSeries: [String]
    let albumClaimed: Bool
    let albumClaimable: Bool
    let claimableSeries: [String]
    let showcase: [String]
    let slots: Int
    let maxSlots: Int
    let slotCost: Int
    let shardPacksToday: Int
    let tokensToday: Int
    let coinPercent: Int
    let perks: [PerkInfo]
    let chests: ChestCounts
}

// MARK: - Resultados de acciones

struct ActionResult: Decodable {
    let ok: Bool
    let reason: String?
}

struct DropInfo: Decodable, Identifiable {
    let id: String
    let new: Bool
    let shards: Int
    let bonus: Bool
    let rarity: String
}

struct ChestOpenResult: Decodable {
    let ok: Bool
    let reason: String?
    let drops: [DropInfo]?
    let reveals: [String]?
}

struct LevelRewardInfo: Decodable {
    let level: Int
    let coins: Int
    let gems: Int
    let chest: String?
}

struct ChapterChestInfo: Decodable {
    let chapter: Int
    let coins: Int
    let gems: Int
    let chest: String
}

struct WinMilestone: Decodable {
    let gems: Int
    let undos: Int
}

struct TeaserInfo: Decodable {
    let level: Int
    let title: String
    let boss: Bool
    let toChest: Int
}

struct WinReward: Decodable {
    let ok: Bool
    let coins: Int
    let rawCoins: Int
    let streakPct: Int
    let albumPct: Int
    let eventMult: Double
    let vipBonus: Bool
    let boostActive: Bool
    let firstClear: Bool
    let firstWinCoins: Int
    let piggyGems: Int
    let seasonPoints: Int
    let newBest: Bool
    let stars: Int
    let winStreak: Int
    let winMilestone: WinMilestone?
    let levelRewards: [LevelRewardInfo]
    let chapterChest: ChapterChestInfo?
    let newSkins: [String]
    let teaser: TeaserInfo?
    let nextGoal: NextGoalInfo?
}

struct LossInfo: Decodable {
    let ok: Bool
    let winStreak: Int
    let nextGoal: NextGoalInfo?
}

struct ComebackInfo: Decodable {
    let coins: Int
    let chest: String
    let freezes: Int
}

struct StreakMilestoneInfo: Decodable {
    let days: Int
    let coins: Int
    let gems: Int
    let chest: String?
}

struct OpenAppResult: Decodable {
    let ok: Bool
    let change: String
    let streak: Int
    let daysAway: Int
    let tokens: Int
    let comeback: ComebackInfo?
    let milestone: StreakMilestoneInfo?
    let repairLost: Int
    let repairCost: Int
    let vipGems: Int
    let reveals: [String]
}

struct DailyGiftResult: Decodable {
    let ok: Bool
    let coins: Int?
    let gems: Int?
    let isChest: Bool?
}

struct WheelResult: Decodable {
    let ok: Bool
    let index: Int?
    let reason: String?
}

struct CoinsResult: Decodable {
    let ok: Bool
    let coins: Int?
    let reason: String?
}

struct TierClaimResult: Decodable {
    let ok: Bool
    let coins: Int?
    let gems: Int?
    let chest: String?
    let skin: String?
    let avatar: Int?
    let banner: Int?
    let fx: String?
}

struct MissionClaimResult: Decodable {
    let ok: Bool
    let coins: Int?
    let allDone: Bool?
    let perfectGems: Int?
}
