package com.example.proyectomov

import FactoryMethod.Gasto
import FactoryMethod.Ingreso
import FactoryMethod.GastoFactory
import FactoryMethod.IngresoFactory
import FactoryMethod.Transaccion
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import Services.TransactionService
import internalStorage.NetworkUtils
import internalStorage.TransactionRepository
import internalStorage.toDomainModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class FiltrarTransacciones : AppCompatActivity() {

    private lateinit var textViewFecha: TextView
    private lateinit var recyclerViewTransacciones: RecyclerView
    private lateinit var rightArrow: ImageView
    private lateinit var leftArrow: ImageView
    private lateinit var backArrow: ImageView
    private lateinit var checkboxTodoElMes: CheckBox
    private lateinit var iconCalendar: ImageView
    private lateinit var offlineIndicator: TextView


    private var usuarioID: String = ""
    private var paginaActual: Int = 1
    private var diaSeleccionado: Int = 0
    private var mesSeleccionado: Int = 0
    private var anioSeleccionado: Int = 0

    private lateinit var tipoFiltroActual: String
    private lateinit var endpointAPI: String
    private lateinit var jsonArrayKey: String


    private val transactionService by lazy { TransactionService(this) } //
    private val gastoFactory = GastoFactory() //
    private val ingresoFactory = IngresoFactory() //
    private val transactionRepository: TransactionRepository by lazy { //
        TransactionRepository(applicationContext)
    }


    private var listaTransaccionesAPI: MutableList<Transaccion> = mutableListOf()
    private lateinit var gastoAdapter: GastoAdapter
    private lateinit var ingresoAdapter: IngresoAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_filtrar_transacciones) //

        tipoFiltroActual = intent.getStringExtra("TIPO_FILTRO") ?: "gastos"

        if (tipoFiltroActual == "gastos") {
            endpointAPI = "gastos"
            jsonArrayKey = "gastos"
        } else {
            endpointAPI = "ingresos"
            jsonArrayKey = "ingresos"
        }

        usuarioID = UsuarioGlobal.id ?: "" //
        if (usuarioID.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textViewFecha = findViewById(R.id.title2) //
        recyclerViewTransacciones = findViewById(R.id.recyclerViewTransacciones) //
        rightArrow = findViewById(R.id.right_arrow) //
        leftArrow = findViewById(R.id.left_arrow) //
        backArrow = findViewById(R.id.imageView4) //
        checkboxTodoElMes = findViewById(R.id.checkboxTodoElMes) //
        iconCalendar = findViewById(R.id.imageView5) //
        offlineIndicator = findViewById(R.id.offline_indicator_filter) //

        recyclerViewTransacciones.layoutManager = LinearLayoutManager(this)

        gastoAdapter = GastoAdapter(mutableListOf(), this::onTransaccionItemClicked)
        ingresoAdapter = IngresoAdapter(mutableListOf(), this::onTransaccionItemClicked)

        if (tipoFiltroActual == "gastos") {
            recyclerViewTransacciones.adapter = gastoAdapter
        } else {
            recyclerViewTransacciones.adapter = ingresoAdapter
        }


        val calendario = Calendar.getInstance()
        anioSeleccionado = calendario.get(Calendar.YEAR)
        mesSeleccionado = calendario.get(Calendar.MONTH) + 1
        diaSeleccionado = 0 // Default to whole month

        checkboxTodoElMes.isChecked = true
        actualizarTextViewFecha()
        cargarTransaccionesFiltradas()

        textViewFecha.setOnClickListener {
            mostrarDatePicker()
        }
        iconCalendar.setOnClickListener {
            mostrarDatePicker()
        }

        checkboxTodoElMes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                diaSeleccionado = 0
            }
            paginaActual = 1
            actualizarTextViewFecha()
            cargarTransaccionesFiltradas()
        }

        rightArrow.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                paginaActual++
                cargarTransaccionesFiltradas()
            } else {
                Toast.makeText(this, "Paginación no disponible en modo offline.", Toast.LENGTH_SHORT).show()
            }
        }

        leftArrow.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                if (paginaActual > 1) {
                    paginaActual--
                    cargarTransaccionesFiltradas()
                } else {
                    Toast.makeText(this, "Ya estás en la primera página.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Paginación no disponible en modo offline.", Toast.LENGTH_SHORT).show()
            }
        }

        backArrow.setOnClickListener {
            finish()
        }
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        val anioInicial = if (anioSeleccionado != 0) anioSeleccionado else calendario.get(Calendar.YEAR)
        val mesInicial = if (mesSeleccionado != 0) mesSeleccionado - 1 else calendario.get(Calendar.MONTH)
        val diaInicial = if (diaSeleccionado != 0 && !checkboxTodoElMes.isChecked) diaSeleccionado else calendario.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            anioSeleccionado = year
            mesSeleccionado = month + 1 // Month is 0-indexed
            diaSeleccionado = dayOfMonth

            if (checkboxTodoElMes.isChecked) {
                checkboxTodoElMes.isChecked = false
            }
            paginaActual = 1 // Reset page on new date selection
            actualizarTextViewFecha()
            cargarTransaccionesFiltradas()
        }, anioInicial, mesInicial, diaInicial)

        datePickerDialog.show()
    }

    private fun actualizarTextViewFecha() {
        val nombreMes = obtenerNombreMes(mesSeleccionado - 1) // Month is 0-indexed for array
        if (checkboxTodoElMes.isChecked || diaSeleccionado == 0) {
            textViewFecha.text = "$nombreMes $anioSeleccionado"
        } else {
            textViewFecha.text = "$diaSeleccionado de $nombreMes, $anioSeleccionado"
        }
    }

    private fun cargarTransaccionesFiltradas() {
        if (usuarioID.isEmpty()) return

        val queryParams = mutableMapOf(
            "usuarioID" to usuarioID,
            "mes" to mesSeleccionado.toString(),
            "anio" to anioSeleccionado.toString(),
            "limite" to "5",
            "pagina" to paginaActual.toString(),
            "ordenFecha" to "desc"
        )

        if (!checkboxTodoElMes.isChecked && diaSeleccionado > 0) {
            queryParams["dia"] = diaSeleccionado.toString()
        }

        if (NetworkUtils.isNetworkAvailable(this)) { //
            offlineIndicator.visibility = View.GONE //
            transactionService.obtenerTransacciones(endpointAPI, queryParams, //
                onSuccess = { response ->
                    try {
                        val transaccionesJsonArray = response.getJSONArray(jsonArrayKey)
                        val totalPaginas = response.optInt("totalPaginas", 1)
                        listaTransaccionesAPI.clear()

                        for (i in 0 until transaccionesJsonArray.length()) {
                            val item = transaccionesJsonArray.getJSONObject(i)
                            var actualIdUser = usuarioID
                            val idUserField = item.opt("Id_user")
                            if (idUserField is JSONObject) {
                                actualIdUser = idUserField.optString("_id", usuarioID)
                            } else if (idUserField is String && idUserField.isNotBlank()) {
                                if (idUserField.startsWith("{")) {
                                    try {
                                        val jsonFromString = JSONObject(idUserField)
                                        actualIdUser = jsonFromString.optString("_id", usuarioID)
                                    } catch (e: org.json.JSONException) {
                                        Log.w("FiltrarTrans", "$endpointAPI - Id_user is a string but not valid JSON: $idUserField. Using general userId.")
                                    }
                                } else {
                                    actualIdUser = idUserField
                                }
                            }

                            val transaccion: Transaccion = if (tipoFiltroActual == "gastos") { //
                                gastoFactory.crearTransaccion( //
                                    transactionId = item.getString("_id"),
                                    idUser = actualIdUser,
                                    nombre = item.getString("Nombre"),
                                    descripcion = item.optString("Descripcion"),
                                    fecha = item.getString("Fecha"),
                                    monto = item.getDouble("Monto"),
                                    tipo = item.getString("Tipo"),
                                    archivo = item.optString("Archivo")
                                )
                            } else {
                                ingresoFactory.crearTransaccion( //
                                    transactionId = item.getString("_id"),
                                    idUser = actualIdUser,
                                    nombre = item.getString("Nombre"),
                                    descripcion = item.optString("Descripcion"),
                                    fecha = item.getString("Fecha"),
                                    monto = item.getDouble("Monto"),
                                    tipo = item.getString("Tipo"),
                                    archivo = item.optString("Archivo")
                                )
                            }
                            listaTransaccionesAPI.add(transaccion)
                        }

                        actualizarAdaptadorConDatos(listaTransaccionesAPI)
                        leftArrow.isEnabled = paginaActual > 1
                        rightArrow.isEnabled = paginaActual < totalPaginas

                    } catch (e: Exception) {
                        Log.e("FiltrarTransParse", "Error al parsear $tipoFiltroActual: ${e.message}", e)
                        Toast.makeText(this, "Error al procesar datos de $tipoFiltroActual. Mostrando datos locales.", Toast.LENGTH_LONG).show()
                        cargarTransaccionesLocales() // Fallback
                    }
                },
                onError = { errorMessage ->
                    Log.e("FiltrarTransFetch", "Error obteniendo $tipoFiltroActual de API: $errorMessage")
                    Toast.makeText(this, "Error al cargar $tipoFiltroActual desde API: $errorMessage. Mostrando datos locales.", Toast.LENGTH_LONG).show()
                    cargarTransaccionesLocales() // Fallback
                }
            )
        } else {
            offlineIndicator.visibility = View.VISIBLE //
            Toast.makeText(this, "Modo offline. Mostrando datos locales.", Toast.LENGTH_SHORT).show()
            cargarTransaccionesLocales()
        }
    }

    private fun cargarTransaccionesLocales() {
        lifecycleScope.launch {
            if (tipoFiltroActual == "gastos") {
                transactionRepository.getGastos(usuarioID).collectLatest { gastoEntities -> //
                    val filteredLocalGastos = gastoEntities.filter { entity ->
                        isTransactionInSelectedFilter(entity.fecha) //
                    }.map { it.toDomainModel() } //
                    actualizarAdaptadorConDatos(filteredLocalGastos)
                    if (filteredLocalGastos.isEmpty()) {
                        Toast.makeText(this@FiltrarTransacciones, "No hay gastos locales para este filtro.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else { // ingresos
                transactionRepository.getIngresos(usuarioID).collectLatest { ingresoEntities -> //
                    val filteredLocalIngresos = ingresoEntities.filter { entity ->
                        isTransactionInSelectedFilter(entity.fecha) //
                    }.map { it.toDomainModel() } //
                    actualizarAdaptadorConDatos(filteredLocalIngresos)
                    if (filteredLocalIngresos.isEmpty()) {
                        Toast.makeText(this@FiltrarTransacciones, "No hay ingresos locales para este filtro.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            leftArrow.isEnabled = false
            rightArrow.isEnabled = false
        }
    }

    private fun actualizarAdaptadorConDatos(transacciones: List<Transaccion>) {
        if (tipoFiltroActual == "gastos") {
            gastoAdapter.gastos.clear()
            gastoAdapter.gastos.addAll(transacciones.filterIsInstance<Gasto>()) //
            gastoAdapter.notifyDataSetChanged()
        } else {
            (ingresoAdapter.ingresos as MutableList<Ingreso>).clear()
            (ingresoAdapter.ingresos as MutableList<Ingreso>).addAll(transacciones.filterIsInstance<Ingreso>()) //
            ingresoAdapter.notifyDataSetChanged()
        }
    }


    private fun isTransactionInSelectedFilter(transactionDateStr: String): Boolean {
        try {
            val odt = OffsetDateTime.parse(transactionDateStr)
            if (odt.year != anioSeleccionado) return false
            if (odt.monthValue != mesSeleccionado) return false
            if (!checkboxTodoElMes.isChecked && diaSeleccionado != 0) {
                if (odt.dayOfMonth != diaSeleccionado) return false
            }
            return true
        } catch (e: DateTimeParseException) {
            Log.w("FilterDateParse", "DateTimeParseException for date: \"$transactionDateStr\". Attempting fallback.", e)

            val formatsToTry = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()),// ISO with millis and offset
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),   // ISO without millis, with offset
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply{ timeZone = TimeZone.getTimeZone("UTC") },
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply{ timeZone = TimeZone.getTimeZone("UTC") },
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Date only
            )
            var parsedDate: Date? = null
            for (format in formatsToTry) {
                try {
                    parsedDate = if (transactionDateStr.contains("T")) format.parse(transactionDateStr) else format.parse(transactionDateStr.substringBefore("T") + "T00:00:00.000Z")
                    if (parsedDate != null) break
                } catch (pe: java.text.ParseException) { /* Try next format */ }
            }

            if (parsedDate != null) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.time = parsedDate
                if (cal.get(Calendar.YEAR) != anioSeleccionado) return false
                if ((cal.get(Calendar.MONTH) + 1) != mesSeleccionado) return false
                if (!checkboxTodoElMes.isChecked && diaSeleccionado != 0) {
                    if (cal.get(Calendar.DAY_OF_MONTH) != diaSeleccionado) return false
                }
                return true
            }
            Log.e("FilterDateParseFallback", "Could not parse date with any fallback: \"$transactionDateStr\"")
            return false
        } catch (e: Exception) {
            Log.e("FilterDateGeneric", "Generic error parsing date: \"$transactionDateStr\"", e)
            return false
        }
    }


    private fun onTransaccionItemClicked(transaccion: Transaccion) { //
        val intent: Intent = if (transaccion is Gasto) { //
            Intent(this, DetalleGasto::class.java) //
        } else { // Es Ingreso
            Intent(this, DetalleIngreso::class.java) //
        }

        intent.apply {
            putExtra("EXTRA_TRANSACTION_ID", transaccion.transactionId)
            putExtra("nombre", transaccion.nombre)
            putExtra("monto", transaccion.monto.toString())
            putExtra("fecha", transaccion.fecha)
            putExtra("tipo", transaccion.tipo)
            putExtra("descripcion", transaccion.descripcion)
            putExtra("archivo", transaccion.archivo)
        }
        startActivity(intent)
    }

    private fun obtenerNombreMes(monthIndex: Int): String {
        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return if (monthIndex in meses.indices) meses[monthIndex] else ""
    }
}