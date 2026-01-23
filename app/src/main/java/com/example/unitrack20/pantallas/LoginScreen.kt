package com.example.unitrack20.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unitrack20.R
import com.example.unitrack20.firebase.FirebaseRepository
import com.example.unitrack20.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxSize()) {
        // Fondo navy
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF061E34), Color(0xFF072442))
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Card grande que contiene el logo y el título
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .height(260.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2740)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_unitrack_logo),
                            contentDescription = "UniTrack logo",
                            modifier = Modifier.size(140.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "UniTrack",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Estados y validación
                var usuario by remember { mutableStateOf("") }
                var correo by remember { mutableStateOf("") }
                var clave by remember { mutableStateOf("") }
                var intentoLogin by remember { mutableStateOf(false) }
                var loading by remember { mutableStateOf(false) }
                var isRegisterMode by remember { mutableStateOf(false) }

                val usuarioError = usuario.isBlank()
                val correoError = correo.isBlank() || !correo.contains("@")
                val claveError = clave.length < if (isRegisterMode) 6 else 4 // Firebase necesita >=6 para registro
                val formularioValido = !usuarioError && !correoError && !claveError

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Mostrar estado de inicialización de Firebase y deshabilitar acciones si no está listo
                val firebaseReady = remember { FirebaseRepository.isInitialized() }
                if (!firebaseReady) {
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar("Firebase no inicializado. Añade app/google-services.json en el módulo app/ y sincroniza Gradle.")
                    }
                }

                // Usuario
                OutlinedTextField(
                    value = usuario,
                    onValueChange = { usuario = it },
                    placeholder = { Text("Usuario", color = AccentWhite.copy(alpha = 0.8f)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    isError = intentoLogin && usuarioError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentWhite,
                        unfocusedTextColor = AccentWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = AccentWhite
                    )
                )

                if (intentoLogin && usuarioError) {
                    Text(
                        text = "El nombre de usuario no puede estar vacío",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 20.dp, top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Correo
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = { Text("Correo electrónico", color = AccentWhite.copy(alpha = 0.8f)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    isError = intentoLogin && correoError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentWhite,
                        unfocusedTextColor = AccentWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = AccentWhite
                    )
                )

                if (intentoLogin && correoError) {
                    Text(
                        text = "Ingresa un correo válido (debe contener @)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 20.dp, top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contraseña
                OutlinedTextField(
                    value = clave,
                    onValueChange = { clave = it },
                    placeholder = { Text("Contraseña", color = AccentWhite.copy(alpha = 0.8f)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    isError = intentoLogin && claveError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentWhite,
                        unfocusedTextColor = AccentWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = AccentWhite
                    )
                )

                if (intentoLogin && claveError) {
                    Text(
                        text = if (isRegisterMode) "La contraseña debe tener al menos 6 caracteres" else "La contraseña debe tener al menos 4 caracteres",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 20.dp, top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón principal (LOGIN / REGISTER)
                Button(
                    onClick = {
                        intentoLogin = true
                        if (formularioValido) {
                            if (!firebaseReady) {
                                scope.launch { snackbarHostState.showSnackbar("Firebase no inicializado. Añade google-services.json en app/ y sincroniza Gradle.") }
                                return@Button
                            }
                            scope.launch {
                                loading = true
                                if (isRegisterMode) {
                                    // Registro
                                    val regResult = FirebaseRepository.registerUser(correo, clave, usuario)
                                    loading = false
                                    if (regResult.isSuccess) {
                                        val uid = regResult.getOrNull() ?: ""
                                        // recuperar perfil guardado en Firestore para obtener displayName
                                        val profile = FirebaseRepository.getUserProfile(uid)
                                        val displayName = profile.getOrNull()?.get("displayName") as? String ?: usuario
                                        onLoginSuccess(displayName)
                                    } else {
                                        snackbarHostState.showSnackbar(regResult.exceptionOrNull()?.localizedMessage ?: "Error al registrar")
                                    }
                                } else {
                                    // Login
                                    val loginResult = FirebaseRepository.loginUser(correo, clave)
                                    loading = false
                                    if (loginResult.isSuccess) {
                                        val uid = loginResult.getOrNull() ?: ""
                                        val profile = FirebaseRepository.getUserProfile(uid)
                                        val displayName = profile.getOrNull()?.get("displayName") as? String ?: usuario
                                        onLoginSuccess(displayName)
                                    } else {
                                        snackbarHostState.showSnackbar(loginResult.exceptionOrNull()?.localizedMessage ?: "Error de login")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !loading && firebaseReady // deshabilitar si Firebase no está listo
                ) {
                    if (loading) CircularProgressIndicator(color = AccentWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text(if (isRegisterMode) "REGISTRAR" else "LOGIN", color = AccentWhite)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle entre login y registro
                TextButton(onClick = {
                    isRegisterMode = !isRegisterMode
                    // resetear estado de intento para limpiar mensajes
                    intentoLogin = false
                }) {
                    Text(if (isRegisterMode) "¿Ya tienes cuenta? Iniciar sesión" else "¿No tienes cuenta? Regístrate", color = AccentWhite)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Snackbar host para mostrar errores
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }
}
