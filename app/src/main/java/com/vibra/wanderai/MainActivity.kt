
package com.vibra.wanderai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vibra.wanderai.presentation.screens.welcome.WelcomeScreen
import com.vibra.wanderai.ui.theme.WanderAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WanderAITheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    WelcomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        onGetStarted = {
                            // Navigation will be implemented next.
                        }
                    )
                }
            }
        }
    }
}
