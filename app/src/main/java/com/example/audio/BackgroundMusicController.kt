package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackgroundMusicController(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared: Boolean = false
    private var isPreparing: Boolean = false
    private var shouldPlayWhenPrepared: Boolean = false
    private var userPaused: Boolean = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun startIfEnabled() {
        if (userPaused) return
        shouldPlayWhenPrepared = true
        ensurePlayerPrepared()
        val player = mediaPlayer ?: return
        if (isPrepared && !player.isPlaying) {
            runCatching {
                player.start()
                _isPlaying.value = true
            }.onFailure { e ->
                Log.e(TAG, "Failed to start MediaPlayer", e)
                _isPlaying.value = false
            }
        }
    }

    fun onForeground() {
        startIfEnabled()
    }

    fun onBackground() {
        shouldPlayWhenPrepared = false
        val player = mediaPlayer ?: return
        if (isPrepared && player.isPlaying) {
            runCatching { player.pause() }
        }
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null && isPrepared) {
            if (player.isPlaying) {
                userPaused = true
                shouldPlayWhenPrepared = false
                runCatching { player.pause() }
                _isPlaying.value = false
            } else {
                userPaused = false
                shouldPlayWhenPrepared = true
                runCatching {
                    player.start()
                    _isPlaying.value = true
                }.onFailure { e ->
                    Log.e(TAG, "Failed to resume MediaPlayer", e)
                }
            }
        } else {
            userPaused = !userPaused
            shouldPlayWhenPrepared = !userPaused
            if (!userPaused) {
                ensurePlayerPrepared()
            } else {
                _isPlaying.value = false
            }
        }
    }

    fun release() {
        shouldPlayWhenPrepared = false
        isPrepared = false
        isPreparing = false
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun ensurePlayerPrepared() {
        if (mediaPlayer != null || isPreparing) return
        isPreparing = true
        runCatching {
            val afd = appContext.resources.openRawResourceFd(R.raw.fallin_bg_music)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(0.85f, 0.85f)
                setOnPreparedListener { mp ->
                    isPreparing = false
                    isPrepared = true
                    if (shouldPlayWhenPrepared && !userPaused) {
                        runCatching {
                            mp.start()
                            _isPlaying.value = true
                        }.onFailure { e ->
                            Log.e(TAG, "Failed to start after prepareAsync", e)
                        }
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    isPreparing = false
                    isPrepared = false
                    _isPlaying.value = false
                    true
                }
            }
            try {
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } finally {
                afd.close()
            }
            mediaPlayer = player
            player.prepareAsync()
        }.onFailure { e ->
            Log.e(TAG, "Error initializing MediaPlayer", e)
            isPreparing = false
            isPrepared = false
            _isPlaying.value = false
        }
    }

    companion object {
        private const val TAG = "BackgroundMusicCtrl"
    }
}
