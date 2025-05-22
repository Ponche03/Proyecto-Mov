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
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import services.TransactionService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FiltrarTransacciones : AppCompatActivity() {

    private lateinit var textViewTituloFiltro: TextView
    private lateinit var textViewFecha: TextView
    private lateinit var recyclerViewTransacciones: RecyclerView
    private lateinit var rightArrow: ImageView
    private lateinit var leftArrow: ImageView
    private lateinit var backArrow: ImageView
    private lateinit var checkboxTodoElMes: CheckBox
    private lateinit var iconCalendar: ImageView

    private var usuarioID: String = ""
    private var paginaActual: Int = 1
    private var diaSeleccionado: Int = 0
    private var mesSeleccionado: Int = 0
    private var anioSeleccionado: Int = 0

    private lateinit var tipoFiltroActual: String
    private lateinit var endpointAPI: String
    private lateinit var jsonArrayKey: String


    private val transactionService by lazy { TransactionService(this) }
    private val gastoFactory = GastoFactory()
    private val ingresoFactory = IngresoFactory()

    private var listaTransacciones: MutableList<Transaccion> = mutableListOf()
    private lateinit var gastoAdapter: GastoAdapter
    private lateinit var ingresoAdapter: IngresoAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_filtrar_transacciones)

        tipoFiltroActual = intent.getStringExtra("TIPO_FILTRO") ?: "gastos"

        if (tipoFiltroActual == "gastos") {
            endpointAPI = "gastos"
            jsonArrayKey = "gastos"
        } else {
            endpointAPI = "ingresos"
            jsonArrayKey = "ingresos"
        }

        usuarioID = UsuarioGlobal.id ?: ""
        if (usuarioID.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textViewFecha = findViewById(R.id.title2)
        recyclerViewTransacciones = findViewById(R.id.recyclerViewTransacciones)
        rightArrow = findViewById(R.id.right_arrow)
        leftArrow = findViewById(R.id.left_arrow)
        backArrow = findViewById(R.id.imageView4)
        checkboxTodoElMes = findViewById(R.id.checkboxTodoElMes)
        iconCalendar = findViewById(R.id.imageView5)

        // Configurar título de la pantalla (opcional, si tienes un TextView para ello)
        // supportActionBar?.title = if (tipoFiltroActual == "gastos") "Filtrar Gastos" else "Filtrar Ingresos"

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
        diaSeleccionado = 0

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
            paginaActual++
            cargarTransaccionesFiltradas()
        }

        leftArrow.setOnClickListener {
            if (paginaActual > 1) {
                paginaActual--
                cargarTransaccionesFiltradas()
            } else {
                Toast.makeText(this, "Ya estás en la primera página.", Toast.LENGTH_SHORT).show()
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
            mesSeleccionado = month + 1
            diaSeleccionado = dayOfMonth

            if (checkboxTodoElMes.isChecked) {
                checkboxTodoElMes.isChecked = false
                paginaActual = 1
                actualizarTextViewFecha()
                cargarTransaccionesFiltradas()
            } else {
                paginaActual = 1
                actualizarTextViewFecha()
                cargarTransaccionesFiltradas()
            }
        }, anioInicial, mesInicial, diaInicial)

        datePickerDialog.show()
    }

    private fun actualizarTextViewFecha() {
        val nombreMes = obtenerNombreMes(mesSeleccionado - 1)
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
            "limite" to "10",
            "pagina" to paginaActual.toString(),
            "ordenFecha" to "desc"
        )

        if (!checkboxTodoElMes.isChecked && diaSeleccionado > 0) {
            queryParams["dia"] = diaSeleccionado.toString()
        }

        transactionService.obtenerTransacciones(endpointAPI, queryParams,
            onSuccess = { response ->
                try {
                    val transaccionesJsonArray = response.getJSONArray(jsonArrayKey)
                    val totalPaginas = response.optInt("totalPaginas", 1)

                    listaTransacciones.clear()

                    for (i in 0 until transaccionesJsonArray.length()) {
                        val item = transaccionesJsonArray.getJSONObject(i)
                        val transaccion: Transaccion = if (tipoFiltroActual == "gastos") {
                            gastoFactory.crearTransaccion(
                                idUser = item.optString("Id_user", usuarioID),
                                nombre = item.getString("Nombre"),
                                descripcion = item.optString("Descripcion"),
                                fecha = formatarFechaBonita(item.getString("FechaLocal")),
                                monto = item.getDouble("Monto"),
                                tipo = item.getString("Tipo"),
                                archivo = item.optString("Archivo")
                            )
                        } else {
                            ingresoFactory.crearTransaccion(
                                idUser = item.optString("Id_user", usuarioID),
                                nombre = item.getString("Nombre"),
                                descripcion = item.optString("Descripcion"),
                                fecha = formatarFechaBonita(item.getString("FechaLocal")),
                                monto = item.getDouble("Monto"),
                                tipo = item.getString("Tipo"),
                                archivo = item.optString("Archivo")
                            )
                        }
                        listaTransacciones.add(transaccion)
                    }

                    if (tipoFiltroActual == "gastos") {
                        (gastoAdapter.gastos as MutableList<Gasto>).clear()
                        (gastoAdapter.gastos as MutableList<Gasto>).addAll(listaTransacciones.filterIsInstance<Gasto>())
                        gastoAdapter.notifyDataSetChanged()
                    } else {
                        (ingresoAdapter.ingresos as MutableList<Ingreso>).clear()
                        (ingresoAdapter.ingresos as MutableList<Ingreso>).addAll(listaTransacciones.filterIsInstance<Ingreso>())
                        ingresoAdapter.notifyDataSetChanged()
                    }

                    leftArrow.isEnabled = paginaActual > 1
                    rightArrow.isEnabled = paginaActual < totalPaginas

                } catch (e: Exception) {
                    Log.e("FiltrarTransParse", "Error al parsear $tipoFiltroActual: ${e.message}", e)
                    Toast.makeText(this, "Error al procesar datos de $tipoFiltroActual.", Toast.LENGTH_LONG).show()
                    limpiarYNotificarAdaptador()
                }
            },
            onError = { errorMessage ->
                Log.e("FiltrarTransFetch", "Error obteniendo $tipoFiltroActual: $errorMessage")
                Toast.makeText(this, "Error al cargar $tipoFiltroActual: $errorMessage", Toast.LENGTH_LONG).show()
                limpiarYNotificarAdaptador()
            }
        )
    }

    private fun limpiarYNotificarAdaptador() {
        if (tipoFiltroActual == "gastos") {
            (gastoAdapter.gastos as MutableList<Gasto>).clear()
            gastoAdapter.notifyDataSetChanged()
        } else {
            (ingresoAdapter.ingresos as MutableList<Ingreso>).clear()
            ingresoAdapter.notifyDataSetChanged()
        }
    }

    private fun onTransaccionItemClicked(transaccion: Transaccion) {
        val intent: Intent = if (transaccion is Gasto) {
            Intent(this, DetalleGasto::class.java)
        } else { // Es Ingreso
            Intent(this, DetalleIngreso::class.java)
        }

        intent.apply {
            putExtra("nombre", transaccion.nombre)
            putExtra("monto", transaccion.monto.toString())
            putExtra("fecha", transaccion.fecha)
            putExtra("tipo", transaccion.tipo)
            putExtra("descripcion", transaccion.descripcion)
            putExtra("archivo", transaccion.archivo)
        }
        startActivity(intent)
    }

    private fun formatarFechaBonita(fechaISO: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = inputFormat.parse(fechaISO)
            val outputFormat = SimpleDateFormat("dd 'de' MMMM, yy", Locale("es", "ES")) // yy para año corto
            date?.let { outputFormat.format(it) } ?: fechaISO
        } catch (e: Exception) {
            Log.e("DateParseError", "Error al formatear fecha: $fechaISO", e)
            fechaISO.substringBefore("T")
        }
    }

    private fun obtenerNombreMes(monthIndex: Int): String {
        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return if (monthIndex in meses.indices) meses[monthIndex] else ""
    }
}
