package com.example.mediavault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge


import androidx.compose.ui.tooling.preview.Preview
import com.example.mediavault.ui.theme.MediaVaultTheme

class MainActivity : ComponentActivity() {
    private var fileManager = lazy { FileManager(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            MediaVaultTheme {
                MainScreen(
                    fileManager = fileManager.value
                )
            }
        }
    }
}

