package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.BackgroundMusicController
import com.example.ui.FollowLensApp
import com.example.ui.theme.FollowLensTheme
import com.example.ui.viewmodel.FollowLensViewModel

class MainActivity : ComponentActivity() {

    private lateinit var musicController: BackgroundMusicController

    private val followLensViewModel: FollowLensViewModel by viewModels {
        FollowLensViewModel.provideFactory(application)
    }

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> musicController.onForeground()
            Lifecycle.Event.ON_PAUSE -> musicController.onBackground()
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        musicController = BackgroundMusicController(this)
        lifecycle.addObserver(lifecycleObserver)
        musicController.startIfEnabled()

        handleIncomingImportIntent(intent)

        setContent {
            FollowLensTheme {
                val isMusicPlaying by musicController.isPlaying.collectAsStateWithLifecycle()
                FollowLensApp(
                    viewModel = followLensViewModel,
                    isMusicPlaying = isMusicPlaying,
                    onToggleMusic = { musicController.togglePlayPause() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingImportIntent(intent)
    }

    override fun onDestroy() {
        lifecycle.removeObserver(lifecycleObserver)
        musicController.release()
        super.onDestroy()
    }

    private fun handleIncomingImportIntent(intent: Intent?) {
        if (intent == null) return
        val uris = when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> listOfNotNull(extractParcelableUri(intent, Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> extractParcelableUriList(intent, Intent.EXTRA_STREAM)
            else -> emptyList()
        }
        if (uris.isNotEmpty()) {
            followLensViewModel.importFiles(uris)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractParcelableUri(intent: Intent, key: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, Uri::class.java)
        } else {
            intent.getParcelableExtra(key)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractParcelableUriList(intent: Intent, key: String): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(key, Uri::class.java).orEmpty()
        } else {
            intent.getParcelableArrayListExtra<Uri>(key).orEmpty()
        }
    }
}
