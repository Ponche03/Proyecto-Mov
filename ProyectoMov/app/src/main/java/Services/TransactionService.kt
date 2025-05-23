package Services

import UsuarioGlobal
import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.Request // Import Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.proyectomov.R
import FactoryMethod.Transaccion
import org.json.JSONObject

class TransactionService(private val context: Context) {

    private val requestQueue by lazy { Volley.newRequestQueue(context.applicationContext) }
    private val baseUrl by lazy { context.getString(R.string.base_url) }

    fun registrarTransaccion(
        transaccion: Transaccion,
        endpoint: String,
        onSuccess: (response: JSONObject) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val jsonBody = JSONObject().apply {
            put("Id_user", transaccion.idUser) // Ensure this is the correct user ID
            put("Fecha", transaccion.fecha) // Assumes transaccion.fecha is ISO format
            put("Monto", transaccion.monto)
            put("Nombre", transaccion.nombre)
            put("Tipo", transaccion.tipo)
            put("Descripcion", transaccion.descripcion ?: "")
            put("Archivo", transaccion.archivo ?: "")
        }

        val apiUrl = "$baseUrl/$endpoint/"

        val jsonObjectRequestWithAuth = object : JsonObjectRequest(
            Method.POST,
            apiUrl,
            jsonBody,
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                val errorMessage = try {
                    val responseBody = String(error.networkResponse.data, Charsets.UTF_8)
                    val jsonError = JSONObject(responseBody)
                    jsonError.optString("message", error.message ?: "Error desconocido")
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor"
                }
                onError(errorMessage)
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = UsuarioGlobal.token
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }
        requestQueue.add(jsonObjectRequestWithAuth)
    }

    fun actualizarTransaccion(
        transactionId: String,
        transaccion: Transaccion,
        endpoint: String, // "gastos" or "ingresos"
        onSuccess: (response: JSONObject) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val jsonBody = JSONObject().apply {

            put("Id_user", transaccion.idUser)
            put("Nombre", transaccion.nombre)
            put("Monto", transaccion.monto)
            put("Descripcion", transaccion.descripcion ?: "")
            put("Tipo", transaccion.tipo)
            if (transaccion.archivo != null && transaccion.archivo!!.isNotEmpty()) {
                put("Archivo", transaccion.archivo)
            }

        }

        val apiUrl = "$baseUrl/$endpoint/$transactionId"

        val jsonObjectRequestWithAuth = object : JsonObjectRequest(
            Request.Method.PUT, // Use PUT for updates
            apiUrl,
            jsonBody,
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                val errorMessage = try {
                    val networkResponse = error.networkResponse
                    if (networkResponse?.data != null) {
                        val responseBody = String(networkResponse.data, Charsets.UTF_8)
                        val jsonError = JSONObject(responseBody)
                        jsonError.optString("message", error.message ?: "Error desconocido.")
                    } else {
                        error.message ?: "Error de red o servidor."
                    }
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor."
                }
                onError(errorMessage)
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = UsuarioGlobal.token
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }
        requestQueue.add(jsonObjectRequestWithAuth)
    }


    fun obtenerTransacciones(
        endpoint: String,
        queryParams: Map<String, String>?,
        onSuccess: (response: JSONObject) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        var urlConParams = "$baseUrl/$endpoint/"

        queryParams?.let { params ->
            if (params.isNotEmpty()) {
                val queryString = params.map { (key, value) -> "$key=$value" }.joinToString("&")
                urlConParams += "?$queryString"
            }
        }

        val jsonObjectRequestWithAuth = object : JsonObjectRequest(
            Method.GET,
            urlConParams,
            null,
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                val errorMessage = try {
                    val networkResponse = error.networkResponse
                    if (networkResponse?.data != null) {
                        val responseBody = String(networkResponse.data, Charsets.UTF_8)
                        val jsonError = JSONObject(responseBody)
                        jsonError.optString("message", error.message ?: "Error desconocido.")
                    } else {
                        error.message ?: "Error de red o servidor."
                    }
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor."
                }
                onError(errorMessage)
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = UsuarioGlobal.token
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }
        requestQueue.add(jsonObjectRequestWithAuth)
    }


    fun eliminarTransaccion(
        transactionId: String,
        endpoint: String, // "gastos" or "ingresos"
        onSuccess: (response: JSONObject) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val apiUrl = "$baseUrl/$endpoint/$transactionId"

        val jsonObjectRequestWithAuth = object : JsonObjectRequest(
            Request.Method.DELETE,
            apiUrl,
            null, // No body for DELETE request
            Response.Listener { response ->
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                val errorMessage = try {
                    val networkResponse = error.networkResponse
                    if (networkResponse?.data != null) {
                        val responseBody = String(networkResponse.data, Charsets.UTF_8)
                        val jsonError = JSONObject(responseBody)
                        jsonError.optString("mensaje", error.message ?: "Error desconocido.") // "mensaje" from your API
                    } else {
                        error.message ?: "Error de red o servidor."
                    }
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor."
                }
                onError(errorMessage)
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = UsuarioGlobal.token
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }
        requestQueue.add(jsonObjectRequestWithAuth)
    }
}