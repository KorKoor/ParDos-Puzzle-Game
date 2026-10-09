package com.korkoor.pardos.ui.game.logic

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.korkoor.pardos.BuildConfig
import com.korkoor.pardos.audio.GameAudio
import com.korkoor.pardos.audio.Sfx
import com.korkoor.pardos.data.local.AdFrequency
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.shop.AdPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Anuncios de AdMob.
 *
 * - Con premio (los pide el jugador): siempre disponibles, con reintentos solos si no hay anuncio.
 * - De pantalla completa: solo tras ganar un nivel de campaña, pocos y espaciados (reglas en [AdPolicy]); nunca con VIP.
 * - Consentimiento (UMP): se pide donde la ley lo exige (EEE/Reino Unido y algunos estados de EE. UU.) antes de cargar nada.
 *
 * En depuración se usan SIEMPRE los anuncios de prueba de Google (ver app/build.gradle.kts).
 */
object AdManager {
    private const val TAG = "AdManager"
    private val REWARDED_ID: String = BuildConfig.REWARDED_AD_UNIT_ID
    private val INTERSTITIAL_ID: String = BuildConfig.INTERSTITIAL_AD_UNIT_ID

    private val main = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var consent: ConsentInformation? = null
    private val started = AtomicBoolean(false)

    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedFails = 0
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialFails = 0
    @Volatile private var interstitialInFlight = false
    @Volatile private var interstitialStartedAt = 0L

    private val _rewardedReady = MutableStateFlow(false)
    /** Hay un anuncio con premio listo: los botones "ver anuncio" lo usan para mostrarse activos. */
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /** Hay un anuncio de pantalla completa en pantalla ahora mismo (la música se calla sola al pasar la app a segundo plano). */
    @Volatile var adOnScreen: Boolean = false
        private set

    /** Los intersticiales se activan al poner su ID en gradle.properties (AdMob → Bloques de anuncios → Intersticial). */
    val interstitialsEnabled: Boolean get() = INTERSTITIAL_ID.isNotBlank()

    // ---------------------------------------------------------------- Arranque y consentimiento

    fun initialize(activity: Activity) {
        val ctx = activity.applicationContext
        appContext = ctx
        val info = UserMessagingPlatform.getConsentInformation(ctx)
        consent = info
        // Si ya hay consentimiento de otra sesión no se espera a la red
        if (info.canRequestAds()) startSdk()
        info.requestConsentInfoUpdate(
            activity, ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consentimiento: ${error.errorCode} ${error.message}")
                    startSdk()
                }
            },
            { error ->
                Log.w(TAG, "Consentimiento sin actualizar: ${error.errorCode} ${error.message}")
                startSdk()
            }
        )
    }

    /** Ajustes: la ley obliga a poder revisar la decisión de privacidad. Solo se enseña la opción cuando corresponde. */
    val privacyOptionsRequired: Boolean
        get() = consent?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Opciones de privacidad: ${error.errorCode} ${error.message}")
            startSdk()
        }
    }

    private fun startSdk() {
        val ctx = appContext ?: return
        if (consent?.canRequestAds() != true) return
        if (!started.compareAndSet(false, true)) { loadRewardedAd(ctx); loadInterstitialAd(ctx); return }
        // La documentación de AdMob pide inicializar fuera del hilo principal
        Thread {
            MobileAds.initialize(ctx) {
                main.post { loadRewardedAd(ctx); loadInterstitialAd(ctx) }
            }
        }.start()
    }

    // ---------------------------------------------------------------- Anuncios con premio

    fun loadRewardedAd(anyContext: Context) {
        if (rewardedAd != null || rewardedLoading || !started.get()) return
        val context = anyContext.applicationContext   // los reintentos programados no retienen la Activity
        rewardedLoading = true
        RewardedAd.load(context, REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w(TAG, "Premio sin cargar: ${adError.code} ${adError.message}")
                rewardedAd = null
                rewardedLoading = false
                _rewardedReady.value = false
                rewardedFails++
                // Sin conexión o sin anuncios: se vuelve a intentar solo, cada vez más tarde
                main.postDelayed({ loadRewardedAd(context) }, backoff(rewardedFails))
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Premio listo")
                rewardedAd = ad
                rewardedLoading = false
                rewardedFails = 0
                _rewardedReady.value = true
            }
        })
    }

    /**
     * Enseña el anuncio y entrega el premio solo si el jugador lo ve completo. Si no hay anuncio listo avisa con un mensaje claro
     * (antes no pasaba nada y parecía que el botón no funcionaba) y sigue intentando cargar.
     */
    fun showRewardedAd(activity: Activity, onUnavailable: (() -> Unit)? = null, onRewardEarned: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            GameAudio.play(Sfx.UI_ERROR)
            Toast.makeText(activity, com.korkoor.pardos.R.string.ad_not_ready, Toast.LENGTH_SHORT).show()
            loadRewardedAd(activity)
            onUnavailable?.invoke()
            return
        }
        rewardedAd = null
        _rewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { adOnScreen = true }
            override fun onAdDismissedFullScreenContent() { adOnScreen = false; loadRewardedAd(activity) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                adOnScreen = false
                Log.w(TAG, "Premio sin mostrar: ${error.code} ${error.message}")
                Toast.makeText(activity, com.korkoor.pardos.R.string.ad_not_ready, Toast.LENGTH_SHORT).show()
                loadRewardedAd(activity)
            }
        }
        ad.show(activity) {
            AdFrequency(activity).recordRewarded()
            GameAudio.play(Sfx.AD_REWARD)
            onRewardEarned()
        }
    }

    // ---------------------------------------------------------------- Anuncios de pantalla completa

    private fun loadInterstitialAd(anyContext: Context) {
        if (!interstitialsEnabled || interstitialAd != null || interstitialLoading || !started.get()) return
        val context = anyContext.applicationContext
        if (EconomyManager(context).isVip.value) return
        interstitialLoading = true
        InterstitialAd.load(context, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w(TAG, "Intersticial sin cargar: ${adError.code} ${adError.message}")
                interstitialAd = null
                interstitialLoading = false
                interstitialFails++
                main.postDelayed({ loadInterstitialAd(context) }, backoff(interstitialFails))
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Intersticial listo")
                interstitialAd = ad
                interstitialLoading = false
                interstitialFails = 0
            }
        })
    }

    /** ¿Toca un anuncio de pantalla completa al pasar al siguiente nivel? (para no avanzar solo sin que el jugador lo sepa) */
    fun isInterstitialDue(context: Context, campaignLevel: Int, vip: Boolean, bigMoment: Boolean = false): Boolean =
        interstitialsEnabled && interstitialAd != null &&
            AdPolicy.shouldShowInterstitial(AdFrequency(context).state(vip, campaignLevel, afterWin = true, bigMoment = bigMoment))

    /** Cuenta una victoria de campaña para decidir cuándo toca el siguiente anuncio. */
    fun onCampaignWin(context: Context) = AdFrequency(context).onCampaignWin()

    /**
     * Pausa natural tras ganar: si las reglas lo permiten se enseña un anuncio y luego se sigue; si no, se sigue de inmediato.
     * Nunca bloquea al jugador: sin anuncio cargado, [onDone] se llama al momento.
     */
    fun showInterstitialIfDue(activity: Activity, campaignLevel: Int, vip: Boolean, bigMoment: Boolean = false, onDone: () -> Unit) {
        // Un segundo toque en SIGUIENTE mientras el anuncio se abre no debe saltarse un nivel
        // (si algo raro dejó la bandera puesta, a los 90 s se ignora: SIGUIENTE nunca debe quedarse sin responder)
        if (interstitialInFlight && System.currentTimeMillis() - interstitialStartedAt < 90_000L) return
        interstitialInFlight = false
        val ad = interstitialAd
        if (ad == null || !isInterstitialDue(activity, campaignLevel, vip, bigMoment)) {
            loadInterstitialAd(activity)
            onDone()
            return
        }
        interstitialAd = null
        interstitialInFlight = true
        interstitialStartedAt = System.currentTimeMillis()
        var finished = false
        val finish = { if (!finished) { finished = true; adOnScreen = false; interstitialInFlight = false; loadInterstitialAd(activity); onDone() } }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { adOnScreen = true; AdFrequency(activity).recordInterstitial() }
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Intersticial sin mostrar: ${error.code} ${error.message}")
                finish()
            }
        }
        ad.show(activity)
    }

    private fun backoff(fails: Int): Long = (4_000L shl (fails - 1).coerceIn(0, 4)).coerceAtMost(60_000L)
}
