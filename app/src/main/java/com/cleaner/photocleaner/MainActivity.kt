package com.cleaner.photocleaner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.cleaner.photocleaner.ui.CleanerViewModel
import com.cleaner.photocleaner.ui.screens.BlurryScreen
import com.cleaner.photocleaner.ui.screens.DuplicatesScreen
import com.cleaner.photocleaner.ui.screens.MainScreen
import com.cleaner.photocleaner.ui.screens.ScreenshotsScreen
import com.cleaner.photocleaner.ui.theme.PhotoCleanerTheme

enum class CurrentScreen {
    MAIN,
    DUPLICATES,
    BLURRY,
    SCREENSHOTS
}

class MainActivity : ComponentActivity() {

    private val viewModel: CleanerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhotoCleanerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(CurrentScreen.MAIN) }

                    val scanStatus by viewModel.scanStatus.collectAsState()
                    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
                    val blurryPhotos by viewModel.blurryPhotos.collectAsState()
                    val screenshots by viewModel.screenshots.collectAsState()
                    val selectedUris by viewModel.selectedUris.collectAsState()

                    // Trash/Delete launcher with system confirmation dialog
                    val trashLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            viewModel.onTrashCompleted()
                        }
                    }

                    // Permission launcher for Android 13/14/15
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val isGranted = permissions.values.any { it }
                        if (isGranted) {
                            viewModel.startScan()
                        }
                    }

                    fun checkAndStartScan() {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            // Android 14 and 15
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Android 13
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            // Android 12 and below
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }

                        val allGranted = permissions.any {
                            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (allGranted) {
                            viewModel.startScan()
                        } else {
                            permissionLauncher.launch(permissions)
                        }
                    }

                    fun requestClean() {
                        viewModel.prepareTrashRequest { intentSender ->
                            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                            trashLauncher.launch(intentSenderRequest)
                        }
                    }

                    when (currentScreen) {
                        CurrentScreen.MAIN -> {
                            MainScreen(
                                scanStatus = scanStatus,
                                duplicateGroups = duplicateGroups,
                                blurryPhotos = blurryPhotos,
                                screenshots = screenshots,
                                selectedUris = selectedUris,
                                onStartScan = { checkAndStartScan() },
                                onOpenDuplicates = { currentScreen = CurrentScreen.DUPLICATES },
                                onOpenBlurry = { currentScreen = CurrentScreen.BLURRY },
                                onOpenScreenshots = { currentScreen = CurrentScreen.SCREENSHOTS },
                                onRequestClean = { requestClean() }
                            )
                        }

                        CurrentScreen.DUPLICATES -> {
                            DuplicatesScreen(
                                groups = duplicateGroups,
                                selectedUris = selectedUris,
                                onToggleSelect = { viewModel.toggleSelection(it) },
                                onBack = { currentScreen = CurrentScreen.MAIN }
                            )
                        }

                        CurrentScreen.BLURRY -> {
                            BlurryScreen(
                                photos = blurryPhotos,
                                selectedUris = selectedUris,
                                onToggleSelect = { viewModel.toggleSelection(it) },
                                onSelectAll = { viewModel.selectAllBlurry() },
                                onBack = { currentScreen = CurrentScreen.MAIN }
                            )
                        }

                        CurrentScreen.SCREENSHOTS -> {
                            ScreenshotsScreen(
                                photos = screenshots,
                                selectedUris = selectedUris,
                                onToggleSelect = { viewModel.toggleSelection(it) },
                                onSelectAll = { viewModel.selectAllScreenshots() },
                                onBack = { currentScreen = CurrentScreen.MAIN }
                            )
                        }
                    }
                }
            }
        }
    }
}
