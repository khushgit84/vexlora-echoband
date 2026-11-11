package com.vexlora.echoband

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vexlora.echoband.ui.screens.HomeScreen
import com.vexlora.echoband.ui.theme.VexloraEchoBandTheme
import com.vexlora.echoband.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VexloraEchoBandTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
