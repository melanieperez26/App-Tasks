package com.example.unitrack20.pantallas.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.unitrack20.components.TopBar
import com.example.unitrack20.data.ThemePreferences
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

// Modelo para cada opción de color
data class ColorOption(
    val nombre: String,
    val color: Color
)

@Composable
fun PantallaConfiguracion(
    openMenu: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados iniciales: leer preferencias
    var colorSeleccionado by remember { mutableStateOf<Color?>(null) }
    var darkPreference by remember { mutableStateOf<Boolean?>(null) }
    var dynamicColor by remember { mutableStateOf(true) }

    // Cargar prefs
    LaunchedEffect(Unit) {
        ThemePreferences.primaryColorFlow(context).collect { c -> if (c != null) colorSeleccionado = Color(c) }
        ThemePreferences.isDarkFlow(context).collect { d -> darkPreference = d }
        ThemePreferences.dynamicColorFlow(context).collect { dynamicColor = it }
    }

    // Lista de colores disponibles
    val listaColores = listOf(
        ColorOption("Verde", Color(0xFF4CAF50)),
        ColorOption("Azul", Color(0xFF2962FF)),
        ColorOption("Morado", Color(0xFF7E57C2)),
        ColorOption("Rojo", Color(0xFFE53935)),
        ColorOption("Naranja", Color(0xFFFF9800))
    )

    Scaffold(
        topBar = { TopBar(title = "Configuración", onMenuClick = openMenu) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Personalización de color",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Toggle dark mode
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Modo oscuro", modifier = Modifier.weight(1f))
                Switch(checked = darkPreference ?: false, onCheckedChange = { new ->
                    darkPreference = new
                    scope.launch { ThemePreferences.saveDark(context, new) }
                })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Color dinámico", modifier = Modifier.weight(1f))
                Switch(checked = dynamicColor, onCheckedChange = { new ->
                    dynamicColor = new
                    scope.launch { ThemePreferences.saveDynamic(context, new) }
                })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Panel de colores
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listaColores.forEach { opcion ->
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        colorSeleccionado = opcion.color
                        scope.launch { ThemePreferences.savePrimaryColor(context, opcion.color.value.toInt()) }
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

                        Box(modifier = Modifier.size(28.dp).background(opcion.color, shape = MaterialTheme.shapes.small))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = opcion.nombre, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        if (colorSeleccionado == opcion.color) {
                            Text("✔", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(text = "Color seleccionado:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.size(80.dp).background(colorSeleccionado ?: Color.Gray, shape = MaterialTheme.shapes.medium))
        }
    }
}

