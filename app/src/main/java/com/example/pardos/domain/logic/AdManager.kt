package com.example.pardos.ui.game.logic

import android.app.Activity
import android.content.Context
import android.util.Log
// ✅ IMPORTA ESTO EXPLÍCITAMENTE
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    // ID DE PRUEBA (Cámbialo por el tuyo real antes de publicar)
    // El tuyo era: "ca-app-pub-3851960142449906/7125882091"
    private const val AD_UNIT_ID = "ca-app-pub-3851960142449906/7125882091"

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    fun initialize(context: Context) {
        MobileAds.initialize(context)
        loadRewardedAd(context)
    }

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isAdLoading) return

        isAdLoading = true

        // ✅ CORRECCIÓN: Asegúrate de que AdRequest.Builder() esté disponible
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e("AdManager", "Error al cargar: ${adError.message}")
                rewardedAd = null
                isAdLoading = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("AdManager", "Anuncio cargado lista para mostrar")
                rewardedAd = ad
                isAdLoading = false
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity) // Precargar el siguiente
                }

                // Si falla al mostrarse, limpiamos para intentar cargar otro
                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    rewardedAd = null
                }
            }

            rewardedAd?.show(activity) { _ ->
                // El usuario obtuvo la recompensa
                onRewardEarned()
            }
        } else {
            Log.d("AdManager", "El anuncio no estaba listo.")
            loadRewardedAd(activity)
        }
    }
}