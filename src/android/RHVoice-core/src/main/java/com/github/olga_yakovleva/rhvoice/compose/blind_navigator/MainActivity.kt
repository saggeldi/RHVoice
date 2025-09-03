package com.github.olga_yakovleva.rhvoice.compose.blind_navigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.github.olga_yakovleva.rhvoice.compose.theme.JetpackComposeMLKitTutorialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackComposeMLKitTutorialTheme {
                Navigation(navController = rememberNavController())
                }
            }
        }
    }

