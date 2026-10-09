package com.korkoor.pardos.audio

/**
 * Todos los efectos de sonido del juego. [file] es el nombre del .ogg en res/raw (los genera tools/audio/sfx.py).
 *
 * @param gain volumen relativo (se multiplica por el volumen de efectos de Ajustes)
 * @param minGapMs separación mínima entre dos reproducciones (para que un sonido no se amontone)
 * @param priority prioridad en SoundPool si se acaban los canales (más alto = se conserva)
 */
enum class Sfx(val file: String, val gain: Float = 1f, val minGapMs: Long = 0L, val priority: Int = 1) {
    // --- Interfaz ---
    UI_TAP("ui_tap", 0.9f, 30),
    UI_TAP2("ui_tap2", 0.9f, 30),
    UI_BACK("ui_back", 0.85f, 60),
    UI_OPEN("ui_open", 0.8f, 80),
    UI_CLOSE("ui_close", 0.75f, 80),
    UI_ON("ui_on", 0.85f, 60),
    UI_OFF("ui_off", 0.85f, 60),
    UI_TAB("ui_tab", 0.8f, 40),
    UI_ERROR("ui_error", 0.85f, 120),
    UI_LOCKED("ui_locked", 0.8f, 120),
    UI_WHOOSH("ui_whoosh", 0.7f, 120),
    UI_NOTIFY("ui_notify", 0.85f, 200, 2),
    UI_CONFIRM("ui_confirm", 0.9f, 100, 2),
    UI_TICK("ui_tick", 0.6f, 25),
    UI_POPUP("ui_popup", 0.8f, 100),

    // --- Tablero ---
    SWIPE("swipe", 0.7f, 45, 0),
    SPAWN("spawn", 0.45f, 70, 0),
    MERGE_SUB("merge_sub", 0.9f, 40),
    MERGE_SPARKLE("merge_sparkle", 0.8f, 60),
    COMBO_SPARK("combo_spark", 0.75f, 90),
    COMBO_1("combo_1", 0.9f, 150, 2),
    COMBO_2("combo_2", 0.95f, 150, 2),
    COMBO_3("combo_3", 1f, 150, 2),
    COMBO_4("combo_4", 1f, 150, 3),
    FLOW_IN("flow_in", 1f, 300, 3),
    FLOW_TIER("flow_tier", 0.95f, 200, 2),
    FLOW_OUT("flow_out", 0.6f, 300),
    MILESTONE_SMALL("ms_small", 0.95f, 200, 2),
    MILESTONE_MID("ms_mid", 1f, 200, 2),
    MILESTONE_BIG("ms_big", 1f, 200, 3),
    GOAL_PING("goal_ping", 0.8f, 150),
    GOAL_DONE("goal_done", 0.9f, 200, 2),
    STAR_1("star_1", 0.95f, 0, 2),
    STAR_2("star_2", 0.95f, 0, 2),
    STAR_3("star_3", 1f, 0, 3),
    WIN("win", 1f, 0, 3),
    WIN_BIG("win_big", 1f, 0, 3),
    FAIL("fail", 0.8f, 0, 3),
    NEAR_MISS("near_miss", 0.7f, 300),
    TICK_WARN("tick_warn", 0.7f, 200),
    HEARTBEAT("heartbeat", 0.85f, 400),
    UNDO("undo", 0.8f, 150),
    HAMMER("hammer", 1f, 150, 2),
    SHUFFLE("shuffle", 0.8f, 150),
    MAGIC("magic", 0.95f, 150, 2),
    FREEZE("freeze", 0.9f, 200),
    REVIVE("revive", 1f, 300, 3),
    BOARD_CLEAR("board_clear", 1f, 300, 2),
    STONE_BUMP("stone_bump", 0.85f, 150),
    BLOCKED("blocked", 0.7f, 140),
    STORM_WARN("storm_warn", 0.85f, 500),
    STORM_HIT("storm_hit", 0.9f, 200),
    BOSS_INTRO("boss_intro", 1f, 500, 3),
    BOSS_PHASE("boss_phase", 1f, 500, 3),
    BOSS_WIN("boss_win", 1f, 0, 3),
    TOWER_FLOOR("tower_floor", 0.95f, 200, 2),
    HEART_LOST("heart_lost", 0.95f, 300, 2),
    NEW_BEST("new_best", 1f, 300, 3),
    COUNTDOWN("countdown", 0.8f, 200),
    GO("go", 0.95f, 200, 2),

    // --- Recompensas y meta ---
    COIN("coin", 0.9f, 35),
    COINS("coins", 0.95f, 120),
    GEM("gem", 0.95f, 80),
    XP_TICK("xp_tick", 0.55f, 25),
    CLAIM("claim", 1f, 120, 2),
    LEVEL_UP("level_up", 1f, 200, 3),
    RANK_UP("rank_up", 1f, 200, 3),
    PLATINUM("platinum", 1f, 200, 3),
    ACHIEVEMENT("achievement", 1f, 200, 3),
    MISSION_DONE("mission_done", 0.95f, 150, 2),
    DAILY("daily", 1f, 200, 2),
    STREAK("streak", 0.95f, 200, 2),
    CHEST_SHAKE("chest_shake", 0.9f, 150),
    CHEST_OPEN("chest_open", 1f, 150, 3),
    CARD_FLIP("card_flip", 0.7f, 60),
    CARD_COMMON("card_common", 0.9f, 60),
    CARD_RARE("card_rare", 0.95f, 60, 2),
    CARD_EPIC("card_epic", 1f, 60, 2),
    CARD_LEGEND("card_legend", 1f, 60, 3),
    CARD_NEW("card_new", 0.9f, 60, 2),
    FOIL("foil", 1f, 200, 2),
    SELL("sell", 0.9f, 150),
    TRADE_SEND("trade_send", 0.85f, 200),
    TRADE_DONE("trade_done", 0.95f, 200, 2),
    WHEEL_TICK("wheel_tick", 0.6f, 20, 0),
    WHEEL_STOP("wheel_stop", 0.9f, 200),
    WHEEL_WIN("wheel_win", 1f, 200, 3),
    PIGGY("piggy", 1f, 300, 3),
    PURCHASE("purchase", 1f, 300, 3),
    AD_REWARD("ad_reward", 0.95f, 300, 2),
    GIFT("gift", 0.9f, 200),
    SEASON_TIER("season_tier", 0.95f, 150, 2),
    REWARD_BIG("reward_big", 1f, 300, 3);
}

/** Voces con las que suenan las fusiones: cada efecto cosmético de fusión tiene la suya (ver tools/audio/synth.py). */
object MergeVoices {
    const val LEVELS = 12

    fun voiceFor(fx: com.korkoor.pardos.domain.shop.MergeFx): String = when (fx) {
        com.korkoor.pardos.domain.shop.MergeFx.CLASSIC -> "marimba"
        com.korkoor.pardos.domain.shop.MergeFx.SPARKS -> "kalimba"
        com.korkoor.pardos.domain.shop.MergeFx.BUBBLES -> "bloop"
        com.korkoor.pardos.domain.shop.MergeFx.PETALS -> "koto"
        com.korkoor.pardos.domain.shop.MergeFx.HEARTS -> "musicbox"
        com.korkoor.pardos.domain.shop.MergeFx.CONFETTI -> "xylophone"
        com.korkoor.pardos.domain.shop.MergeFx.STARS -> "glass"
        com.korkoor.pardos.domain.shop.MergeFx.RIPPLE -> "ripple"
        com.korkoor.pardos.domain.shop.MergeFx.LIGHTNING -> "zap"
        com.korkoor.pardos.domain.shop.MergeFx.FIREWORKS -> "firebell"
    }

    /** Nivel de una ficha: 2 -> 1, 4 -> 2, 8 -> 3… (una nota distinta por nivel, de la escala pentatónica). */
    fun levelOf(value: Int): Int {
        if (value < 2) return 1
        var l = 0
        var v = value
        while (v > 1) { v = v shr 1; l++ }
        return l.coerceIn(1, LEVELS)
    }

    fun file(voice: String, level: Int): String = "mg_${voice}_${level.coerceIn(1, LEVELS).toString().padStart(2, '0')}"
}
