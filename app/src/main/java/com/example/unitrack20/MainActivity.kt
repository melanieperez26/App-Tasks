package com.example.unitrack20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.unitrack20.firebase.FirebaseInit
import com.example.unitrack20.navegacion.AppNavegacion
import com.example.unitrack20.ui.theme.ThemeState
import com.example.unitrack20.ui.theme.UniTrackTheme
import com.example.unitrack20.data.ThemePreferences

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 Firebase seguro
        try {
            FirebaseInit.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()

        setContent {

            // 🌙 Estado del tema (SIN dynamic color)
            var themeState by remember {
                mutableStateOf(
                    ThemeState(
                        darkTheme = null,      // sigue el sistema
                        dynamicColor = false   // 🔥 DESACTIVADO
                    )
                )
            }

            // 📦 Escuchar preferencia de modo oscuro
            LaunchedEffect(Unit) {
                ThemePreferences.isDarkFlow(applicationContext).collect { isDark ->
                    themeState = ThemeState(
                        darkTheme = isDark,
                        dynamicColor = false // 🔥 siempre false
                    )
                }
            }

            UniTrackTheme(themeState = themeState) {
                Surface(modifier = Modifier) {
                    val navController = rememberNavController()
                    AppNavegacion(navController)
                }
            }
        }
    }
}
