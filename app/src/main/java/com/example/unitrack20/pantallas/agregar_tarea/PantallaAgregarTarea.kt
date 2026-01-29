package com.example.unitrack20.pantallas.agregar_tarea

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.unitrack20.components.TopBar
import com.example.unitrack20.firebase.FirebaseRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.example.unitrack20.util.ReminderReceiver
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AddType { TASK, EXAM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarTarea(
    openMenu: () -> Unit,
    onGuardarOk: (String) -> Unit = {},
    taskId: String? = null,
    examId: String? = null
) {
    val context = LocalContext.current

    // --- Detectar si estamos editando una tarea o examen ---
    val isTaskEditMode = !taskId.isNullOrBlank()
    val isExamEditMode = !examId.isNullOrBlank()
    var addType by remember { mutableStateOf(if (isExamEditMode) AddType.EXAM else AddType.TASK) }

    // --- Campos ---
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fechaLimite by remember { mutableStateOf("") }
    var horaRecordatorio by remember { mutableStateOf<String?>(null) }
    var prioridad by remember { mutableStateOf("Normal") }
    var repetir by remember { mutableStateOf("Ninguno") }
    var recordatorioActivado by remember { mutableStateOf(false) }
    var fechaValida by remember { mutableStateOf(true) }
    var mensajeErrorFecha by remember { mutableStateOf<String?>(null) }

    val prioridades = listOf("Baja", "Normal", "Alta")
    val repetirOpciones = listOf("Ninguno", "Diario", "Semanal", "Mensual")
    var showMiniCalendar by remember { mutableStateOf(false) }
    var miniCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var calendarioSeleccionado by remember { mutableStateOf<Calendar?>(null) }

    val formatterDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isSaving by remember { mutableStateOf(false) }

    // --- Lógica de permisos de notificación ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                recordatorioActivado = true
                if (horaRecordatorio == null) {
                    openTimePicker(context) { h, m -> horaRecordatorio = "%02d:%02d".format(h, m) }
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("El permiso de notificación es necesario para los recordatorios.")
                }
            }
        }
    )

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    recordatorioActivado = true
                    if (horaRecordatorio == null) {
                        openTimePicker(context) { h, m -> horaRecordatorio = "%02d:%02d".format(h, m) }
                    }
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            recordatorioActivado = true
            if (horaRecordatorio == null) {
                openTimePicker(context) { h, m -> horaRecordatorio = "%02d:%02d".format(h, m) }
            }
        }
    }

    // --- Cargar datos en modo edición ---
    LaunchedEffect(taskId, examId) {
        val uid = FirebaseRepository.currentUserUid() ?: return@LaunchedEffect
        var doc: DocumentSnapshot? = null
        if (isTaskEditMode) doc = FirebaseRepository.getTask(uid, taskId)
        else if (isExamEditMode) doc = FirebaseRepository.getExam(uid, examId)

        doc?.let {
            if (isTaskEditMode) {
                titulo = it.getString("title") ?: ""
                descripcion = it.getString("description") ?: ""
                fechaLimite = it.getString("due") ?: ""
                prioridad = it.getString("priority") ?: "Normal"
                repetir = it.getString("repeat") ?: "Ninguno"
                recordatorioActivado = it.getBoolean("reminderEnabled") ?: false
                horaRecordatorio = it.getString("reminderTime")
            } else if (isExamEditMode) {
                titulo = it.getString("subject") ?: ""
                fechaLimite = it.getString("date") ?: ""
            }
        }
    }

    // --- Validación de fecha en tiempo real ---
    LaunchedEffect(fechaLimite) {
        if (fechaLimite.isNotBlank()) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val selectedDate = LocalDate.parse(fechaLimite, formatter)
                if (selectedDate.isBefore(LocalDate.now())) {
                    fechaValida = false
                    mensajeErrorFecha = "No se permiten fechas pasadas"
                } else {
                    fechaValida = true
                    mensajeErrorFecha = null
                }
            } catch (e: Exception) {
                fechaValida = false
                mensajeErrorFecha = "Formato de fecha inválido"
            }
        } else {
            fechaValida = true // O false si la fecha es obligatoria
            mensajeErrorFecha = null
        }
    }

    // --- Función guardar/editar ---
    fun guardar() {
        scope.launch {
            if (titulo.isBlank()) {
                snackbarHostState.showSnackbar("El título es obligatorio")
                return@launch
            }

            val uid = FirebaseRepository.currentUserUid()
            if (uid == null) {
                snackbarHostState.showSnackbar("Usuario no autenticado")
                return@launch
            }

            isSaving = true
            try {
                val success = if (addType == AddType.TASK) {
                    val data = mapOf(
                        "title" to titulo,
                        "description" to descripcion,
                        "priority" to prioridad,
                        "repeat" to repetir,
                        "reminderEnabled" to recordatorioActivado,
                        "reminderTime" to horaRecordatorio,
                        "due" to fechaLimite,
                        "updatedAt" to Timestamp.now()
                    )
                    if (isTaskEditMode)
                        FirebaseRepository.updateTask(uid, taskId, data).isSuccess
                    else
                        FirebaseRepository.addTaskDocument(uid, data).isSuccess
                } else {
                    val data = mapOf(
                        "subject" to titulo,
                        "date" to fechaLimite,
                        "updatedAt" to Timestamp.now()
                    )
                    if (isExamEditMode)
                        FirebaseRepository.updateExam(uid, examId, data).isSuccess
                    else
                        FirebaseRepository.addExamDocument(uid, data).isSuccess
                }

                if (success) {
                    if (recordatorioActivado && horaRecordatorio != null && fechaLimite.isNotBlank()) {
                        scheduleReminder(context, titulo, descripcion, fechaLimite, horaRecordatorio!!)
                    }

                    snackbarHostState.showSnackbar(
                        if (addType == AddType.TASK) "Tarea guardada" else "Examen guardado"
                    )
                    onGuardarOk(taskId ?: examId ?: "")

                    if (!isTaskEditMode && !isExamEditMode) {
                        // Limpiar campos solo si estamos creando
                        titulo = ""
                        descripcion = ""
                        fechaLimite = ""
                        horaRecordatorio = null
                        prioridad = "Normal"
                        repetir = "Ninguno"
                        recordatorioActivado = false
                        calendarioSeleccionado = null
                    }
                } else {
                    snackbarHostState.showSnackbar("Error al guardar")
                }
            } finally { isSaving = false }
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopBar(
                title = when {
                    isTaskEditMode -> "Editar Tarea"
                    isExamEditMode -> "Editar Examen"
                    addType == AddType.TASK -> "Agregar Tarea"
                    else -> "Agregar Examen"
                },
                onMenuClick = openMenu
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { if (!isSaving) guardar() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isSaving && fechaValida
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // --- Título ---
                    Text(
                        text = when {
                            isTaskEditMode -> "Editando tarea"
                            isExamEditMode -> "Editando examen"
                            addType == AddType.TASK -> "Nueva tarea"
                            else -> "Nuevo examen"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(8.dp))

                    if (!isTaskEditMode && !isExamEditMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = addType == AddType.TASK, onClick = { addType = AddType.TASK })
                            Text("Tarea")
                            Spacer(Modifier.width(16.dp))
                            RadioButton(selected = addType == AddType.EXAM, onClick = { addType = AddType.EXAM })
                            Text("Examen")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(value = titulo, onValueChange = { titulo = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true)

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(value = descripcion, onValueChange = { descripcion = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 6)

                    Spacer(Modifier.height(10.dp))

                    // --- Fecha ---
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (fechaLimite.isBlank()) "Sin fecha" else fechaLimite,
                            onValueChange = {}, readOnly = true,
                            label = { Text("Fecha límite") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showMiniCalendar = !showMiniCalendar },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            openDatePicker(context) { cal ->
                                calendarioSeleccionado = cal
                                fechaLimite = formatterDate.format(cal.time)
                            }
                        }) { Text("Seleccionar") }
                    }

                    if (mensajeErrorFecha != null) {
                        Text(
                            text = mensajeErrorFecha!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    if (showMiniCalendar) {
                        Spacer(Modifier.height(8.dp))
                        MiniCalendarSimple(
                            monthCalendar = miniCalendarMonth,
                            selectedCalendar = calendarioSeleccionado,
                            onMonthChange = { miniCalendarMonth = it },
                            onDaySelected = {
                                calendarioSeleccionado = it
                                fechaLimite = formatterDate.format(it.time)
                                showMiniCalendar = false
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- Prioridad ---
                    Text("Prioridad", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        prioridades.forEach { p ->
                            val selected = prioridad == p
                            ElevatedButton(
                                onClick = { prioridad = p },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(p)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Repetir ---
                    var repetirMenuOpen by remember { mutableStateOf(false) }
                    Text("Repetir", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(
                            onClick = { repetirMenuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(repetir, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        DropdownMenu(
                            expanded = repetirMenuOpen,
                            onDismissRequest = { repetirMenuOpen = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repetirOpciones.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        repetir = r
                                        repetirMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Recordatorio ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recordatorio", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = recordatorioActivado,
                            onCheckedChange = {
                                if (it) {
                                    checkAndRequestNotificationPermission()
                                } else {
                                    recordatorioActivado = false
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            openTimePicker(context) { h, m ->
                                horaRecordatorio = "%02d:%02d".format(h, m)
                                if (!recordatorioActivado) {
                                    checkAndRequestNotificationPermission()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = recordatorioActivado
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = "Hora del recordatorio")
                            Spacer(Modifier.width(8.dp))
                            Text(horaRecordatorio ?: "Hora no configurada")
                        }
                    }
                }
            }
        }
    }
}

private fun scheduleReminder(
    context: Context,
    title: String,
    message: String,
    date: String,
    time: String
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val notificationId = System.currentTimeMillis().toInt()
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("message", message)
        putExtra("notificationId", notificationId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    try {
        val parsedDate = dateFormat.parse(date)
        val parsedTime = timeFormat.parse(time)

        if (parsedDate != null && parsedTime != null) {
            val dateCalendar = Calendar.getInstance().apply { this.time = parsedDate }
            val timeCalendar = Calendar.getInstance().apply { this.time = parsedTime }

            calendar.set(
                dateCalendar.get(Calendar.YEAR),
                dateCalendar.get(Calendar.MONTH),
                dateCalendar.get(Calendar.DAY_OF_MONTH),
                timeCalendar.get(Calendar.HOUR_OF_DAY),
                timeCalendar.get(Calendar.MINUTE),
                0
            )

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    } else {
                        // Fallback for when exact alarms are not permitted
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("scheduleReminder", "Error parsing date or time", e)
    }
}

// --- Mini calendario ---
@Composable
fun MiniCalendarSimple(
    monthCalendar: Calendar,
    selectedCalendar: Calendar?,
    onMonthChange: (Calendar) -> Unit,
    onDaySelected: (Calendar) -> Unit
) {
    var currentMonth by remember { mutableStateOf((monthCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }) }

    LaunchedEffect(monthCalendar.timeInMillis) {
        currentMonth = (monthCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    }

    val monthName = currentMonth.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
    val year = currentMonth.get(Calendar.YEAR)

    Column(modifier = Modifier.fillMaxWidth().padding(6.dp).clip(RoundedCornerShape(8.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$monthName $year")
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }; onMonthChange(currentMonth) }) { Text("<") }
            TextButton(onClick = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }; onMonthChange(currentMonth) }) { Text(">") }
        }

        Spacer(Modifier.height(6.dp))
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        Row {
            for (day in 1..daysInMonth) {
                Box(modifier = Modifier.size(36.dp).padding(2.dp).clickable {
                    val selected = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                    onDaySelected(selected)
                }, contentAlignment = Alignment.Center) {
                    Text(day.toString(), fontSize = 12.sp)
                }
            }
        }
    }
}

// --- Pickers ---
private fun openDatePicker(context: Context, onDateSelected: (Calendar) -> Unit) {
    val today = Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, day ->
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        onDateSelected(cal)
    }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
}

private fun openTimePicker(context: Context, onTimeSelected: (Int, Int) -> Unit) {
    val now = Calendar.getInstance()
    TimePickerDialog(context, { _, hour, minute -> onTimeSelected(hour, minute) },
        now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
}
