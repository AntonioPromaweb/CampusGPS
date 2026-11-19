package com.vasquez.campusgps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vasquez.campusgps.ui.CampusApp
import com.vasquez.campusgps.ui.theme.CampusGPSTheme
import com.vasquez.campusgps.viewmodel.CampusViewModelFactory
import com.vasquez.campusgps.viewmodel.MapaViewModel

class MainActivity : ComponentActivity() {

    private val vm: MapaViewModel by viewModels { CampusViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusGPSTheme {
                CampusApp(vm)
            }
        }
    }
}
