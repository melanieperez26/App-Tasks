package com.example.unitrack20.pantallas.calendario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unitrack20.components.TopBar
import com.example.unitrack20.firebase.FirebaseRepository
import com.example.unitrack20.firebase.Task as RepoTask
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCalendario(openMenu: () -> Unit) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var fechaSeleccionada by remember { mutableStateOf(Calendar.getInstance()) }

    // Gradiente sutil de fondo consistente con el estilo "cool & elegant"
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.04f)
    )

    // tareas y estado
    val tareas = remember { mutableStateListOf<RepoTask>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // cargar tareas del usuario
    LaunchedEffect(Unit) {
        loading = true
        error = null
        val uid = FirebaseRepository.currentUserUid()
        if (uid == null) {
            error = "Usuario no autenticado"
            loading = false
        } else {
            val res = FirebaseRepository.getTasksForUser(uid)
            if (res.isSuccess) {
                tareas.clear()
                tareas.addAll(res.getOrNull() ?: emptyList())
            } else {
                error = res.exceptionOrNull()?.localizedMessage
            }
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors))
    ) {
        // decorativas
        DecorativeBubble(
            size = 180.dp,
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
            ),
            offsetX = -12f,
            offsetY = -6f,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = 48.dp)
        )

        DecorativeBubble(
            size = 120.dp,
            colors = listOf(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)
            ),
            offsetX = 8f,
            offsetY = 6f,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-40).dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = "Calendario", onMenuClick = openMenu)

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                // header control
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        }) {
                            Icon(imageVector = Icons.Filled.ArrowBackIos, contentDescription = "Mes anterior", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val monthName = currentMonth.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                            val year = currentMonth.get(Calendar.YEAR)
                            Text(text = "$monthName ${year}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface))
                            Text(text = "Selecciona una fecha para ver tareas", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)))
                        }

                        IconButton(onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        }) {
                            Icon(imageVector = Icons.Filled.ArrowForwardIos, contentDescription = "Mes siguiente", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        WeekDaysRow()
                        Spacer(modifier = Modifier.height(8.dp))
                        CalendarioMes(monthCalendar = currentMonth, fechaSeleccionada = fechaSeleccionada, onFechaSeleccionada = { nueva ->
                            fechaSeleccionada = nueva
                        })
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(text = "Tareas del ${fechaSeleccionada.get(Calendar.DAY_OF_MONTH)} / ${fechaSeleccionada.get(Calendar.MONTH) + 1}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface))
                Spacer(modifier = Modifier.height(10.dp))

                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (loading) {
                            Text(text = "Cargando tareas...", style = MaterialTheme.typography.bodyLarge)
                        } else if (error != null) {
                            Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                        } else {
                            val tasksForDay = tareas.filter { task ->
                                // intentamos comparar por campo due (dd/MM/yyyy)
                                val due = task.due
                                if (due.isNullOrBlank()) return@filter false
                                try {
                                    val parts = due.split("/")
                                    val d = parts[0].toInt()
                                    val m = parts[1].toInt() - 1
                                    val y = parts[2].toInt()
                                    val cal = Calendar.getInstance().apply { set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, d) }
                                    cal.get(Calendar.YEAR) == fechaSeleccionada.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == fechaSeleccionada.get(Calendar.MONTH) && cal.get(Calendar.DAY_OF_MONTH) == fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
                                } catch (_: Exception) {
                                    false
                                }
                            }

                            if (tasksForDay.isEmpty()) {
                                Text(text = "No hay tareas programadas", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface))
                            } else {
                                tasksForDay.forEach { t ->
                                    ModernTaskRow(t)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekDaysRow() {
    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { d ->
            Text(text = d, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)), modifier = Modifier.weight(1f), maxLines = 1)
        }
    }
}

@Composable
fun CalendarioMes(
    monthCalendar: Calendar,
    fechaSeleccionada: Calendar,
    onFechaSeleccionada: (Calendar) -> Unit
) {
    val clone = monthCalendar.clone() as Calendar
    clone.set(Calendar.DAY_OF_MONTH, 1)
    val firstWeekDay = clone.get(Calendar.DAY_OF_WEEK) // 1 = Dom .. 7 = Sáb
    val leadEmpty = ((firstWeekDay + 5) % 7) // lunes inicio
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val daysList = mutableListOf<Int?>()
    repeat(leadEmpty) { daysList.add(null) }
    for (d in 1..daysInMonth) daysList.add(d)
    while (daysList.size % 7 != 0) daysList.add(null)

    Column {
        daysList.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).padding(6.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val seleccionado = (fechaSeleccionada.get(Calendar.YEAR) == monthCalendar.get(Calendar.YEAR)
                                    && fechaSeleccionada.get(Calendar.MONTH) == monthCalendar.get(Calendar.MONTH)
                                    && fechaSeleccionada.get(Calendar.DAY_OF_MONTH) == day)

                            val bgColor = if (seleccionado) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)

                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(bgColor).clickable {
                                val nueva = (monthCalendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                                onFechaSeleccionada(nueva)
                            }, contentAlignment = Alignment.Center) {
                                Text(text = day.toString(), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
                            }
                        } else {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecorativeBubble(modifier: Modifier = Modifier, size: Dp, colors: List<Color>, offsetX: Float = 0f, offsetY: Float = 0f) {
    Box(modifier = modifier
        .size(size)
        .graphicsLayer { translationX = offsetX; translationY = offsetY }
        .background(brush = Brush.radialGradient(colors = colors), shape = RoundedCornerShape(999.dp))
        .alpha(0.95f))
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(14.dp), content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)), border = BorderStroke(width = 1.dp, brush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.surface.copy(alpha = 0.06f)))), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(content = content)
    }
}

@Composable
fun ModernTaskRow(tarea: RepoTask, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Text(text = tarea.title.take(1).uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = tarea.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), maxLines = 1)
                if (!tarea.due.isNullOrEmpty()) {
                    Text(text = tarea.due.orEmpty(), style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }

            Text(text = "Abrir", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { /* abrir tarea */ })
        }
    }
}
