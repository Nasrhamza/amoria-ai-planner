package com.example.amoriaiaplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.amoriaiaplanner.core.navigation.AppNav
import com.example.amoriaiaplanner.ui.theme.AmoriaIAPlannerTheme
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AmoriaIAPlannerTheme {
                AppNav()
            }
        }
    }
}
