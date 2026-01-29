package com.example.unitrack20.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.unitrack20.pantallas.LoginScreen
import com.example.unitrack20.pantallas.inicio.PantallaInicio
import com.example.unitrack20.pantallas.calendario.PantallaCalendario
import com.example.unitrack20.pantallas.agregar_tarea.PantallaAgregarTarea
import com.example.unitrack20.pantallas.perfil.PantallaPerfil
import com.example.unitrack20.pantallas.configuracion.PantallaConfiguracion
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import androidx.navigation.navArgument
import com.example.unitrack20.firebase.FirebaseRepository

// Simple representation of bottom navigation items
private sealed class BottomNavItem(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Inicio : BottomNavItem("inicio", "Inicio", { Icon(Icons.Default.Home, contentDescription = "Inicio") })
    object Calendario : BottomNavItem("calendario", "Calendario", { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario") })
    object AgregarTarea : BottomNavItem("agregar_tarea", "Agregar", { Icon(Icons.Default.List, contentDescription = "Agregar") })
    object Perfil : BottomNavItem("perfil", "Perfil", { Icon(Icons.Default.Person, contentDescription = "Perfil") })
}

// A very small DataStore-based session helper (username only)
private val Context.dataStore by preferencesDataStore("session_prefs")
class SessionDataStore(private val context: Context) {
    private val USER_KEY = stringPreferencesKey("session_username")
    val getUsername: Flow<String> = context.dataStore.data.map { it[USER_KEY] ?: "" }
    val isLogged: Flow<Boolean> = getUsername.map { it.isNotEmpty() }

    suspend fun saveSession(username: String) {
        context.dataStore.edit { prefs -> prefs[USER_KEY] = username }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs -> prefs.remove(USER_KEY) }
    }
}

@Composable
fun AppNavegacion(navController: NavHostController) {
    val context = LocalContext.current
    val session = remember { SessionDataStore(context) }

    val isLogged by session.isLogged.collectAsState(initial = false)
    val savedUser by session.getUsername.collectAsState(initial = "")

    // decide start destination: require both local session and firebase auth
    val firebaseUid = FirebaseRepository.currentUserUid()
    val startDestination = if (isLogged && firebaseUid != null) "menu" else "login"

    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
   ) {
        composable("login") {
            LoginScreen(onLoginSuccess = { username ->
                scope.launch { session.saveSession(username) }
                navController.navigate("menu") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("menu") {
            PantallaConMenu(nombreUsuario = savedUser, onLogout = {
                scope.launch { session.clearSession() }
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConMenu(nombreUsuario: String, onLogout: () -> Unit) {
    val internalNavController = rememberNavController()
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetContentVisible by remember { mutableStateOf(false) }
    val openMenu: () -> Unit = {
        sheetContentVisible = true
        scope.launch { sheetState.show() }
    }

    // observe internal nav destination to decide whether to show the global bottom bar
    val navBackStackEntry by internalNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showGlobalBottomBar = currentRoute != "agregar_tarea"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (showGlobalBottomBar) {
                AppBottomBar(navController = internalNavController)
            }
        }
    ) { innerPadding ->
        if (sheetContentVisible) {
            ModalBottomSheet(onDismissRequest = { scope.launch { sheetState.hide() }; sheetContentVisible = false }, sheetState = sheetState) {
                Column(Modifier.padding(12.dp)) {
                    Text(text = "UniTrack", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(8.dp))
                    HorizontalDivider()

                    NavigationDrawerItem(label = { Text("Inicio") }, selected = false, onClick = {
                        internalNavController.navigate("inicio") { popUpTo(internalNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true }
                        scope.launch { sheetState.hide() }
                        sheetContentVisible = false
                    }, modifier = Modifier.fillMaxWidth())

                    NavigationDrawerItem(label = { Text("Calendario") }, selected = false, onClick = {
                        internalNavController.navigate("calendario") { popUpTo(internalNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true }
                        scope.launch { sheetState.hide() }
                        sheetContentVisible = false
                    }, modifier = Modifier.fillMaxWidth())

                    NavigationDrawerItem(label = { Text("Agregar tarea") }, selected = false, onClick = {
                        internalNavController.navigate("agregar_tarea") { popUpTo(internalNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true }
                        scope.launch { sheetState.hide() }
                        sheetContentVisible = false
                    }, modifier = Modifier.fillMaxWidth())

                    NavigationDrawerItem(label = { Text("Perfil") }, selected = false, onClick = {
                        internalNavController.navigate("perfil") { popUpTo(internalNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true }
                        scope.launch { sheetState.hide() }
                        sheetContentVisible = false
                    }, modifier = Modifier.fillMaxWidth())

                    NavigationDrawerItem(label = { Text("Cerrar sesión") }, selected = false, onClick = {
                        scope.launch { sheetState.hide() }
                        sheetContentVisible = false
                        onLogout()
                    }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = internalNavController, startDestination = "inicio", modifier = Modifier.fillMaxSize()) {
                composable("inicio") { PantallaInicio( navController = internalNavController, openMenu = openMenu, onNavigateToProfile = { internalNavController.navigate("perfil") }, nombreUsuario = nombreUsuario, refreshKey = refreshKey) }
                composable("calendario") { PantallaCalendario(openMenu = openMenu) }
                composable(
                    route = "agregar_tarea?taskId={taskId}&examId={examId}",
                    arguments = listOf(
                        navArgument("taskId") { defaultValue = "" },
                        navArgument("examId") { defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId")
                    val examId = backStackEntry.arguments?.getString("examId")

                    PantallaAgregarTarea(
                        openMenu = openMenu,
                        taskId = taskId,
                        examId = examId,
                        onGuardarOk = { id ->
                            refreshKey++
                            internalNavController.navigate("inicio") {
                                popUpTo(internalNavController.graph.findStartDestination().id) { inclusive = false }
                            }
                        }
                    )
                }
                composable("perfil") { PantallaPerfil(openMenu = openMenu, navegarConfiguracion = { internalNavController.navigate("configuracion") }, navegarEditarPerfil = {}, cerrarSesion = { onLogout() }) }
                composable("configuracion") { PantallaConfiguracion(openMenu = openMenu) }
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val bottomBarBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
        )
    )

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .background(bottomBarBrush)
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                BottomNavItem.Inicio,
                BottomNavItem.Calendario,
                BottomNavItem.AgregarTarea,
                BottomNavItem.Perfil
            )

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(
                containerColor = Color.Transparent
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.routeMatches(item.route) ?: false
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}


private fun NavDestination?.routeMatches(route: String): Boolean {
    return this?.route == route || this?.route?.startsWith(route) == true
}
