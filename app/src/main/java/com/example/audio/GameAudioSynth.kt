package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GameAudioSynth {
    private var toneGenerator: ToneGenerator? = null
    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.e("GameAudioSynth", "Failed to initialize ToneGenerator", e)
        }
    }

    fun playCoin() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Short double chime for retro coin collection
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 60)
                delay(70)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 80)
            } catch (e: Exception) {
                // Safely catch interruptions
            }
        }
    }

    fun playNitro() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Surging rising sweep tones for rocket boost!
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 100)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_4, 110)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_8, 140)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }

    fun playCrash() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Low pitch discordant tone representing crash
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }

    fun playCountdownBeep() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }

    fun playCountdownGo() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Higher pitched long tone for start line!
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }

    fun playLevelUp() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // High arpeggio fan-fare for completing missions or leveling up
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 80)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 80)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 100)
                delay(110)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 200)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }

    fun playSirenSwell() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Alternating siren tones
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_DIAL, 120)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }

    fun playEngineRev(speed: Int) {
        if (!isSoundEnabled) return
        // Engine hum pitch based on driving speed!
        // To avoid flooding the ToneGenerator, we trigger a subtle click only at specific speed transitions
        if (speed % 35 == 0 && speed > 20) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 25)
            } catch (e: Exception) {
                // Safe fallthrough
            }
        }
    }
}
