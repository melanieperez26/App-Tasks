package com.example.unitrack20.pantallas.inicio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unitrack20.firebase.FirebaseRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PantallaInicioViewModel : ViewModel() {
    var taskName by mutableStateOf("")
    var taskDate by mutableStateOf("")
    var isDateValid by mutableStateOf(true)
    var dateErrorMessage by mutableStateOf<String?>(null)

    fun validateAndSetDate(date: LocalDate) {
        if (date.isBefore(LocalDate.now())) {
            isDateValid = false
            dateErrorMessage = "No se permiten fechas pasadas"
        } else {
            isDateValid = true
            dateErrorMessage = null
            taskDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }

    fun onTaskNameChange(newName: String) {
        taskName = newName
    }

    fun resetQuickAddTask() {
        taskName = ""
        taskDate = ""
        isDateValid = true
        dateErrorMessage = null
    }

    fun addQuickTask(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (taskName.isBlank() || taskDate.isBlank() || !isDateValid) {
            onError("El nombre y una fecha válida son obligatorios.")
            return
        }

        viewModelScope.launch {
            val uid = FirebaseRepository.currentUserUid()
            if (uid == null) {
                onError("Usuario no autenticado.")
                return@launch
            }

            val data = mapOf(
                "title" to taskName,
                "due" to taskDate,
                "description" to "",
                "priority" to "Normal",
                "repeat" to "Ninguno",
                "reminderEnabled" to false,
                "reminderTime" to null,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
                "completed" to false
            )

            val result = FirebaseRepository.addTaskDocument(uid, data)
            if (result.isSuccess) {
                resetQuickAddTask()
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error al guardar la tarea.")
            }
        }
    }
}

