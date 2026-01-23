package com.example.unitrack20.pantallas.inicio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.unitrack20.firebase.FirebaseRepository
import com.example.unitrack20.firebase.Task as RepoTask
import com.example.unitrack20.firebase.Exam as RepoExam
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    navController: NavController,
    openMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    nombreUsuario: String,
    refreshKey: Int = 0
) {
    val scope = rememberCoroutineScope()
    var loading by rememberSaveable { mutableStateOf(true) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val tareas = remember { mutableStateListOf<RepoTask>() }
    val examenes = remember { mutableStateListOf<RepoExam>() }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    // Dialogo para crear tarea
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDue by remember { mutableStateOf("") }

    // Estados para el diálogo de confirmación de eliminación
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Buscador
    var query by rememberSaveable { mutableStateOf("") }

    // Cargar datos reales usando FirebaseRepository
    LaunchedEffect(refreshKey) {
        loading = true
        errorMessage = null
        val uid = FirebaseRepository.currentUserUid()
        if (uid == null) {
            errorMessage = "Usuario no autenticado"
            loading = false
            return@LaunchedEffect
        }

        // Cargar perfil
        val profileRes = FirebaseRepository.getUserProfile(uid)
        if (profileRes.isSuccess) {
            profileImageUrl = profileRes.getOrNull()?.get("profileImageUrl") as? String
        }

        val tasksRes = FirebaseRepository.getTasksForUser(uid)
        if (tasksRes.isSuccess) {
            tareas.clear()
            tareas.addAll(tasksRes.getOrNull() ?: emptyList())
        } else {
            errorMessage = tasksRes.exceptionOrNull()?.localizedMessage ?: "Error al cargar tareas"
        }

        val examsRes = FirebaseRepository.getExamsForUser(uid)
        if (examsRes.isSuccess) {
            examenes.clear()
            examenes.addAll(examsRes.getOrNull() ?: emptyList())
        } else {
            errorMessage = examsRes.exceptionOrNull()?.localizedMessage ?: "Error al cargar exámenes"
        }

        loading = false
    }

    val filteredTasks by remember(query, tareas) {
        derivedStateOf {
            if (query.isBlank()) tareas.toList()
            else tareas.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    // Animación del icono de usuario
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    val headerGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Bienvenido") },
                navigationIcon = {
                    IconButton(onClick = openMenu) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        // Mostrar imagen de perfil si está disponible, sino mostrar icono por defecto
                        if (profileImageUrl != null) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Perfil",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Crear")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {

            // --- HEADER ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(brush = headerGradient)
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) { append("Hola, ") }

                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) { append(nombreUsuario) }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text("Buscar tareas...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- ESTADÍSTICAS ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMiniCard(title = "Tareas", value = tareas.size.toString())
                    StatMiniCard(title = "Exámenes", value = examenes.size.toString())
                    StatMiniCard(title = "Activas", value = (tareas.size + examenes.size).toString())
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- LISTA DE TAREAS ---
            if (loading) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                        repeat(3) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(vertical = 6.dp)
                            ) {}
                        }
                    }
                }
            } else {
                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }

                // Encabezado Tareas
                item {
                    Text(
                        text = "Tareas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }

                items(filteredTasks) { tarea ->
                    ModernTaskRow(
                        tarea = tarea,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        onEdit = { taskId -> navController.navigate("agregar_tarea?taskId=$taskId&examId=") },
                        onDelete = { itemToDelete = "task" to it; showDeleteConfirmation = true }
                    )
                }

                // Encabezado Exámenes
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Exámenes próximos",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }

                items(examenes) { exam ->
                    ModernExamRow(
                        exam = exam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        onEdit = { examId -> navController.navigate("agregar_tarea?taskId=&examId=$examId") },
                        onDelete = { itemToDelete = "exam" to it; showDeleteConfirmation = true }
                    )
                }

                // Espacio final para FAB
                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }

    // --- DIALOGO CREAR TAREA ---
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val uid = FirebaseRepository.currentUserUid()
                        if (uid != null && newTaskTitle.isNotBlank()) {
                            val res = FirebaseRepository.addTaskForUser(uid, newTaskTitle, newTaskDue.ifBlank { null })
                            if (res.isSuccess) {
                                val tasksRes = FirebaseRepository.getTasksForUser(uid)
                                if (tasksRes.isSuccess) {
                                    tareas.clear()
                                    tareas.addAll(tasksRes.getOrNull() ?: emptyList())
                                }
                            }
                        }
                        showAddTaskDialog = false
                        newTaskTitle = ""
                        newTaskDue = ""
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancelar") }
            },
            title = { Text("Crear tarea") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Título") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskDue,
                        onValueChange = { newTaskDue = it },
                        placeholder = { Text("Fecha / Vencimiento") }
                    )
                }
            }
        )
    }

    // --- DIALOGO CONFIRMAR ELIMINACIÓN ---
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar este elemento?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val uid = FirebaseRepository.currentUserUid()
                            if (uid != null && itemToDelete != null) {
                                val (type, id) = itemToDelete!!
                                val result = if (type == "task") {
                                    FirebaseRepository.deleteTask(uid, id)
                                } else {
                                    FirebaseRepository.deleteExam(uid, id)
                                }

                                if (result.isSuccess) {
                                    if (type == "task") {
                                        tareas.removeIf { it.id == id }
                                    } else {
                                        val examenesActualizados = examenes.filter { it.id != id }
                                        examenes.clear()
                                        examenes.addAll(examenesActualizados)
                                    }
                                }
                            }
                            showDeleteConfirmation = false
                            itemToDelete = null
                        }
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") }
            }
        )
    }
}

/* -------------------------
   COMPONENTES MODERNOS
   ------------------------- */

@Composable
fun StatMiniCard(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.widthIn(min = 96.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
fun ModernTaskRow(
    tarea: RepoTask,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarea.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!tarea.due.isNullOrEmpty()) {
                    Text(
                        text = tarea.due,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            IconButton(onClick = { onEdit(tarea.id) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(tarea.id) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ModernExamRow(
    exam: RepoExam,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.subject,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = exam.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { onEdit(exam.id) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(exam.id) }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaInicioPreview() {
    val navController = rememberNavController()
    PantallaInicio(
        navController = navController,
        openMenu = {},
        onNavigateToProfile = {},
        nombreUsuario = "Karina"
    )
}
