package com.example.unitrack20.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Modelos simples usados por la app y Firestore
 */


data class Task(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val due: String? = null,
    val priority: String? = "Normal",
    val repeat: String? = "Ninguno",
    val reminderEnabled: Boolean? = false,
    val reminderTime: String? = null,
    val userId: String? = null
)
data class Exam(
    val id: String = "",
    val subject: String = "",
    val date: String = ""
)

/**
 * Repositorio mínimo para autenticación y lectura/escritura en Firestore.
 * Métodos suspend para usarlos desde coroutines (LaunchedEffect o viewModelScope).
 */
object FirebaseRepository {
    private fun isFirebaseInitialized(): Boolean {
        return try {
            // Intentar obtener la instancia por defecto; si no existe lanzará IllegalStateException
            FirebaseApp.getInstance()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Expose public check for UI
    fun isInitialized(): Boolean = isFirebaseInitialized()

    private val auth get() = Firebase.auth
    private val firestore get() = Firebase.firestore

    fun currentUserUid(): String? = try { auth.currentUser?.uid } catch (_: Exception) { null }

    suspend fun registerUser(email: String, password: String, displayName: String): Result<String> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }

        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID no disponible")
            // Guardar info adicional en Firestore
            val userData = mapOf("uid" to uid, "email" to email, "displayName" to displayName)
            firestore.collection("users").document(uid).set(userData).await()
            Result.success(uid)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "registerUser", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }

        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID no disponible")
            Result.success(uid)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "loginUser", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<Map<String, Any>?> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }

        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            Result.success(doc.data)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "getUserProfile", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------
    // Firestore helpers para tasks / exams
    // -------------------------------------------------
    suspend fun getTask(uid: String, taskId: String): DocumentSnapshot? {
        return try {
            firestore.collection("users").document(uid).collection("tasks").document(taskId).get().await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "getTask", e)
            null
        }
    }

    suspend fun getExam(uid: String, examId: String): DocumentSnapshot? {
        return try {
            firestore.collection("users").document(uid).collection("exams").document(examId).get().await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "getExam", e)
            null
        }
    }

    suspend fun getTasksForUser(uid: String): Result<List<Task>> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }

        return try {
            val snap = firestore.collection("users").document(uid).collection("tasks").get().await()
            val list = snap.documents.map { doc ->
                // obtener due como String o como Timestamp
                val dueAny = doc.get("due")
                val dueStr = when (dueAny) {
                    is String -> dueAny
                    is com.google.firebase.Timestamp -> {
                        // formatear a dd/MM/yyyy
                        val date = Date(dueAny.toDate().time)
                        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        fmt.format(date)
                    }
                    else -> null
                }
                Task(id = doc.id, title = doc.getString("title") ?: "", due = dueStr)
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "getTasksForUser", e)
            Result.failure(e)
        }
    }

    suspend fun getExamsForUser(uid: String): Result<List<Exam>> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }

        return try {
            val snap = firestore.collection("users").document(uid).collection("exams").get().await()
            val list = snap.documents.map { doc ->
                Exam(id = doc.id, subject = doc.getString("subject") ?: "", date = doc.getString("date") ?: "")
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "getExamsForUser", e)
            Result.failure(e)
        }
    }

    suspend fun addTaskForUser(uid: String, title: String, due: String?): Result<String> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }

        return try {
            val data = mapOf("title" to title, "due" to (due ?: ""))
            val ref = firestore.collection("users").document(uid).collection("tasks").add(data).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "addTaskForUser", e)
            Result.failure(e)
        }
    }

    // Nueva función para guardar tarea completa como mapa
    suspend fun addTaskDocument(uid: String, data: Map<String, Any?>): Result<String> {
        if (!isFirebaseInitialized()) {
            return Result.failure(Exception("Firebase no inicializado. Añade app/google-services.json y reinicia la app, o llama a FirebaseInit.initialize(context)."))
        }
        return try {
            // Transformar el mapa para evitar tipos no compatibles: asegurar createdAt como serverTimestamp si no viene
            val transformed = data.toMutableMap()
            if (!transformed.containsKey("createdAt") || transformed["createdAt"] == null) {
                transformed["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            } else {
                // si es Long o String, convertir a serverTimestamp para evitar problemas
                val v = transformed["createdAt"]
                if (v is Long || v is String) transformed["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                // si ya es Timestamp lo dejamos
            }

            val ref = firestore.collection("users").document(uid).collection("tasks").add(transformed).await()
            Result.success(ref.id)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Log.e("FirebaseRepository", "addTaskDocument - FirestoreException", e)
            val code = e.code
            val friendly = when (code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permiso denegado: revisa las reglas de seguridad de Firestore y que el usuario esté autenticado."
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Usuario no autenticado: inicia sesión antes de guardar."
                else -> e.message ?: "Error desconocido de Firestore"
            }
            return Result.failure(Exception("Firestore error: ${code.name} - $friendly"))
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "addTaskDocument", e)
            Result.failure(e)
        }
    }

    suspend fun addExamDocument(
        uid: String,
        data: Map<String, Any?>
    ): Result<String> {
        return try {
            val doc = Firebase.firestore
                .collection("users")
                .document(uid)
                .collection("exams")
                .add(data)
                .await()

            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(uid: String, taskId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "deleteTask", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExam(uid: String, examId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).collection("exams").document(examId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "deleteExam", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(uid: String, taskId: String, data: Map<String, Any?>): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).collection("tasks").document(taskId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "updateTask", e)
            Result.failure(e)
        }
    }

    suspend fun updateExam(uid: String, examId: String, data: Map<String, Any?>): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).collection("exams").document(examId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "updateExam", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(uid: String, uri: Uri): Result<String> {
        return try {
            val storageRef = Firebase.storage.reference.child("profile_images/$uid.jpg")
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, data: Map<String, Any?>): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------
    // Helpers para creación de datos (útiles en consola o pruebas)
    // ------------------------
    /**
     * Asegura que exista el documento users/{uid} con los campos básicos.
     * Si ya existe no sobreescribe.
     */
    suspend fun ensureUserDocument(uid: String, displayName: String?, email: String?): Result<Unit> {
        if (!isFirebaseInitialized()) return Result.failure(Exception("Firebase no inicializado."))
        return try {
            val docRef = firestore.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val data = mutableMapOf<String, Any?>()
                displayName?.let { data["displayName"] = it }
                email?.let { data["email"] = it }
                data["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                docRef.set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "ensureUserDocument", e)
            Result.failure(e)
        }
    }

    /**
     * Agrega datos de ejemplo en las subcolecciones tasks y exams del usuario.
     */
    suspend fun seedSampleData(uid: String): Result<Unit> {
        if (!isFirebaseInitialized()) return Result.failure(Exception("Firebase no inicializado."))
        return try {
            val tasksRef = firestore.collection("users").document(uid).collection("tasks")
            val examsRef = firestore.collection("users").document(uid).collection("exams")

            val sampleTask = mapOf(
                "title" to "Tarea ejemplo",
                "description" to "Esta es una tarea de prueba",
                "due" to "01/01/2026",
                "priority" to "Normal",
                "reminderEnabled" to false,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val sampleExam = mapOf(
                "subject" to "Matemáticas",
                "date" to "05/02/2026",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            tasksRef.add(sampleTask).await()
            examsRef.add(sampleExam).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "seedSampleData", e)
            Result.failure(e)
        }
    }

    val storage = Firebase.storage                  // Conectar con Storage
    val storageRef: StorageReference = storage.reference  // Referencia raíz del bucket

    /**
     * Subir un archivo a Firebase Storage
     * @param path: ruta dentro del bucket, ej: "imagenes/tarea1.jpg"
     * @param uri: URI del archivo seleccionado en el dispositivo
     * @return URL de descarga pública del archivo o Exception en caso de error
     */
    suspend fun uploadFile(path: String, uri: Uri): Result<String> {
        return try {
            val fileRef = storageRef.child(path)   // crear referencia dentro del bucket
            fileRef.putFile(uri).await()           // subir archivo
            val downloadUrl = fileRef.downloadUrl.await() // obtener URL pública
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "uploadFile", e)
            Result.failure(e)
        }
    }

    @Suppress("unused")
    fun signOut() {
        try {
            if (isFirebaseInitialized()) auth.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "signOut", e)
        }
    }
}
