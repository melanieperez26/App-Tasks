package com.example.unitrack20.pantallas.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.unitrack20.components.TopBar
import com.example.unitrack20.firebase.FirebaseRepository
import kotlinx.coroutines.launch

@Composable
fun PantallaPerfil(
    openMenu: () -> Unit,
    navegarConfiguracion: () -> Unit = {},
    navegarEditarPerfil: () -> Unit = {},
    cerrarSesion: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados del perfil
    var displayName by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var loadingProfile by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf<String?>(null) }

    // Estadísticas
    var totalTasks by remember { mutableStateOf(0) }
    var totalExams by remember { mutableStateOf(0) }
    var loadingStats by remember { mutableStateOf(true) }

    // Dialogo de confirmación cierre sesión
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Fondo elegante con gradiente sutil (alto contraste en texto)
    val fondo = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
        )
    )
    // foto de perfil
    var profileImageUri by remember { mutableStateOf<Uri?>(null) } // URI local seleccionada
    var profileImageUrl by remember { mutableStateOf<String?>(null) } // URL en Firebase Storage


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            // Subir la imagen a Firebase Storage y obtener la URL
            scope.launch {
                val uid = FirebaseRepository.currentUserUid()
                if (uid != null) {
                    val result = FirebaseRepository.uploadProfileImage(uid, it)
                    if (result.isSuccess) {
                        profileImageUrl = result.getOrNull()
                        // Actualizar la URL de la imagen en el perfil del usuario
                        FirebaseRepository.updateUserProfile(uid, mapOf("profileImageUrl" to profileImageUrl))
                    } else {
                        snackbarHostState.showSnackbar("Error al subir la imagen: ${result.exceptionOrNull()?.localizedMessage}")
                    }
                }
            }
        }
    }

    // Carga perfil y stats al iniciar
    LaunchedEffect(Unit) {
        loadingProfile = true
        profileError = null
        val uid = FirebaseRepository.currentUserUid()
        if (uid == null) {
            profileError = "Usuario no autenticado"
            loadingProfile = false
        } else {
            val prof = FirebaseRepository.getUserProfile(uid)
            if (prof.isSuccess) {
                val map = prof.getOrNull()
                displayName = (map?.get("displayName") as? String) ?: (map?.get("email") as? String)
                email = (map?.get("email") as? String)
                profileImageUrl = map?.get("profileImageUrl") as? String
            } else {
                profileError = prof.exceptionOrNull()?.localizedMessage
            }
            loadingProfile = false

            // cargar stats
            loadingStats = true
            val tasksR = FirebaseRepository.getTasksForUser(uid)
            if (tasksR.isSuccess) {
                totalTasks = tasksR.getOrNull()?.size ?: 0
            }
            val examsR = FirebaseRepository.getExamsForUser(uid)
            if (examsR.isSuccess) {
                totalExams = examsR.getOrNull()?.size ?: 0
            }
            loadingStats = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        // Burbujas decorativas (sutiles)
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer { rotationZ = -12f }
                .offset(x = (-40).dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Box(
            modifier = Modifier
                .size(110.dp)
                .graphicsLayer { rotationZ = 6f }
                .offset(x = 110.dp, y = 260.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // TOPBAR
            TopBar(title = "Mi Perfil", onMenuClick = openMenu)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Avatar circular con degradado
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Imagen de perfil seleccionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Imagen de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar del usuario",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nombre y email (bind dinámico)
                if (loadingProfile) {
                    Text(text = "Cargando perfil...", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp), color = MaterialTheme.colorScheme.onBackground)
                } else if (profileError != null) {
                    Text(text = "Error: $profileError", color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        text = displayName.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Resumen - glass style card (dinámico)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Resumen académico",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (loadingStats) {
                            Text("Cargando estadísticas...", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text("Tareas totales: $totalTasks", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
                            Text("Exámenes totales: $totalExams", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estadísticas - otro card (más visual)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Estadísticas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Simple indicators accesibles
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Tareas", style = MaterialTheme.typography.bodyMedium)
                                Text("$totalTasks", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Exámenes", style = MaterialTheme.typography.bodyMedium)
                                Text("$totalExams", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Completadas", style = MaterialTheme.typography.bodyMedium)
                                // No tenemos completadas en el modelo simple; mostrar totalTasks como placeholder
                                Text("${totalTasks}", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Opciones estilo filas limpias
                OpcionPerfil(
                    texto = "Configuración",
                    icono = Icons.Default.Settings,
                    onClick = { navegarConfiguracion() }
                )

                HorizontalDivider()

                OpcionPerfil(
                    texto = "Editar perfil",
                    icono = Icons.Default.Person,
                    onClick = { navegarEditarPerfil() }
                )

                HorizontalDivider()

                // Cerrar sesión con diálogo de confirmación
                OpcionPerfil(
                    texto = "Cerrar sesión",
                    icono = Icons.AutoMirrored.Filled.ExitToApp,
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        showLogoutDialog = true
                    }
                )
            }
        }

        // Diálogo de confirmación fuera de la columna (renderiza sobre la UI)
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        scope.launch { cerrarSesion() }
                    }) {
                        Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
                },
                title = { Text("¿Deseas cerrar sesión?") },
                text = { Text("Se cerrará tu sesión y volverás a la pantalla de login.") }
            )
        }
    }

    // Snackbar host global para mensajes cortos
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun OpcionPerfil(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
            .semantics { contentDescription = "Opción: $texto" },
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Icono en círculo (estético)
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icono, contentDescription = texto, tint = color, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = color,
            textAlign = TextAlign.Start
        )
    }
}
