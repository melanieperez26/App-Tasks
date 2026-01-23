package com.example.unitrack20.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Inicialización simple de Firebase. Llama a FirebaseInit.initialize(context) desde la Activity.
 */
object FirebaseInit {
    private const val TAG = "FirebaseInit"

    fun initialize(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                Log.i(TAG, "Firebase ya inicializado (apps=${apps.size})")
                return
            }

            val defaultApp = FirebaseApp.initializeApp(context)
            if (defaultApp != null) {
                Log.i(TAG, "Firebase inicializado usando FirebaseApp.initializeApp(context)")
                return
            }

            Log.i(TAG, "Inicialización estándar falló — intentando inicializar desde JSON embebido")

            val optionsFromRaw = tryBuildOptionsFromRaw(context)
            if (optionsFromRaw != null) {
                FirebaseApp.initializeApp(context, optionsFromRaw)
                Log.i(TAG, "Firebase inicializado con opciones desde res/raw/google_services.json")
                return
            }

            val optionsFromAssets = tryBuildOptionsFromAssets(context)
            if (optionsFromAssets != null) {
                FirebaseApp.initializeApp(context, optionsFromAssets)
                Log.i(TAG, "Firebase inicializado con opciones desde assets/google-services.json")
                return
            }

            Log.w(TAG, "No se encontró google-services.json en res/raw ni en assets y la inicialización automática falló. Añade google-services.json en app/ o assets/ y aplica plugin com.google.gms.google-services en Gradle.")
        } catch (ex: Exception) {
            Log.e(TAG, "Error inicializando Firebase", ex)
            throw ex
        }
    }

    private fun tryBuildOptionsFromRaw(context: Context): FirebaseOptions? {
        return try {
            val resId = context.resources.getIdentifier("google_services", "raw", context.packageName)
            if (resId == 0) return null
            val input = context.resources.openRawResource(resId)
            buildOptionsFromStream(input.bufferedReader())
        } catch (ex: Exception) {
            Log.i(TAG, "No se pudo leer res/raw/google_services.json: ${ex.message}")
            null
        }
    }

    private fun tryBuildOptionsFromAssets(context: Context): FirebaseOptions? {
        return try {
            val assetName = "google-services.json"
            val inputStream = context.assets.open(assetName)
            buildOptionsFromStream(BufferedReader(InputStreamReader(inputStream)))
        } catch (ex: Exception) {
            Log.i(TAG, "No se pudo leer assets/google-services.json: ${ex.message}")
            null
        }
    }

    private fun buildOptionsFromStream(reader: BufferedReader): FirebaseOptions? {
        val text = reader.use { it.readText() }
        val root = JSONObject(text)

        return try {
            val clientObj = root.getJSONArray("client").getJSONObject(0)
            val clientInfo = clientObj.getJSONObject("client_info")

            // applicationId en google-services.json está en mobilesdk_app_id dentro de client_info
            val applicationId = clientInfo.optString("mobilesdk_app_id", null)

            val apiKeyArray = clientObj.optJSONArray("api_key")
            val apiKey = apiKeyArray?.optJSONObject(0)?.optString("current_key")

            val projectInfo = root.optJSONObject("project_info")
            val projectId = projectInfo?.optString("project_id")
            val storageBucket = projectInfo?.optString("storage_bucket")
            val databaseUrl = projectInfo?.optString("firebase_url")

            if (apiKey.isNullOrBlank() || applicationId.isNullOrBlank()) {
                Log.i(TAG, "Campos necesarios no encontrados en google-services.json")
                return null
            }

            val builder = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(applicationId)

            if (!projectId.isNullOrBlank()) builder.setProjectId(projectId)
            if (!storageBucket.isNullOrBlank()) builder.setStorageBucket(storageBucket)
            if (!databaseUrl.isNullOrBlank()) builder.setDatabaseUrl(databaseUrl)

            builder.build()
        } catch (ex: Exception) {
            Log.i(TAG, "Error parseando google-services.json: ${ex.message}")
            null
        }
    }
}
