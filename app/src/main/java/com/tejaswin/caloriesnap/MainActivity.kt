package com.tejaswin.caloriesnap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tejaswin.caloriesnap.ui.CameraScreen
import com.tejaswin.caloriesnap.ui.HistoryScreen
import com.tejaswin.caloriesnap.ui.ResultCard
import com.tejaswin.caloriesnap.ui.SetupScreen
import com.tejaswin.caloriesnap.ui.theme.CalorieSnapTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* UI recomposes based on permission state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CalorieSnapTheme {
                CalorieSnapApp(viewModel)
            }
        }
    }
}

@Composable
private fun CalorieSnapApp(viewModel: MainViewModel) {
    val modelState by viewModel.modelState.collectAsState()
    val modelError by viewModel.modelError.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    var setupDismissed by remember { mutableStateOf(false) }

    if (!setupDismissed) {
        SetupScreen(
            modelState = modelState,
            errorMessage = modelError,
            downloadProgress = downloadProgress,
            onDownload = viewModel::downloadModel,
            onContinue = { setupDismissed = true },
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val estimate by viewModel.estimate.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val selectedExtras by viewModel.selectedExtras.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") },
                    label = { Text("Camera") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                0 -> {
                    if (capturedBitmap != null) {
                        // Show result card over a dimmed background
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            ResultCard(
                                estimate = estimate,
                                isLoading = isAnalyzing,
                                selectedExtras = selectedExtras,
                                onToggleExtra = viewModel::toggleExtra,
                                onSave = {
                                    viewModel.save()
                                    selectedTab = 1
                                },
                                onRetake = viewModel::clearCapture,
                            )
                        }
                    } else {
                        CameraScreen(
                            onPhotoCaptured = viewModel::onPhotoCaptured,
                        )
                    }
                }
                1 -> HistoryScreen(entries = allEntries)
            }
        }
    }
}
