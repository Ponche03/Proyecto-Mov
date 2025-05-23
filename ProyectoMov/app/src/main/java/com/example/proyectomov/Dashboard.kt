package com.example.proyectomov

import FactoryMethod.Gasto
import UsuarioGlobal
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import Services.TransactionService
import com.google.android.material.imageview.ShapeableImageView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import FactoryMethod.GastoFactory
import FactoryMethod.Ingreso
import FactoryMethod.IngresoFactory


class Dashboard : AppCompatActivity() {

    private lateinit var fabMain: ImageButton
    private lateinit var fabPerfil: ShapeableImageView
    private lateinit var fabOpciones: ImageView

    private lateinit var menuOptions: LinearLayout
    private lateinit var menuOptionsUser: LinearLayout
    private lateinit var menuOptionsNav: LinearLayout

    private lateinit var radioGroupTipoTransaccion: RadioGroup
    private lateinit var recyclerViewTransacciones: RecyclerView
    private lateinit var textViewMontoTotalMes: TextView
    private lateinit var textViewLeyendaMes: TextView


    private val transactionService by lazy { TransactionService(this) }
    private val gastoFactory = GastoFactory()
    private val ingresoFactory = IngresoFactory()

    private var listaGastos: MutableList<Gasto> = mutableListOf()
    private var listaIngresos: MutableList<Ingreso> = mutableListOf()

    private lateinit var gastoAdapter: GastoAdapter
    private lateinit var ingresoAdapter: IngresoAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fabMain = findViewById(R.id.fab_main)
        fabPerfil = findViewById(R.id.profile_picture)
        fabOpciones = findViewById(R.id.app_icon)
        menuOptions = findViewById(R.id.menu_options)
        menuOptionsUser = findViewById(R.id.menu_options_pfp)
        menuOptionsNav = findViewById(R.id.menu_options_main)
        textViewMontoTotalMes = findViewById(R.id.amount)
        textViewLeyendaMes = findViewById(R.id.title2)


        radioGroupTipoTransaccion = findViewById(R.id.radioGroupTipoTransaccion)
        recyclerViewTransacciones = findViewById(R.id.recyclerViewTransacciones)
        recyclerViewTransacciones.layoutManager = LinearLayoutManager(this)


        gastoAdapter = GastoAdapter(listaGastos, this::onGastoClicked)
        ingresoAdapter = IngresoAdapter(listaIngresos, this::onIngresoClicked)


        cargarImagenPerfil()
        configurarListenersMenus()
        configurarRadioGroupListener()

        cargarTransaccionesDelMesActual()
    }

    override fun onResume() {
        super.onResume()
        // Asegurarse de que los menús estén ocultos cuando la actividad se reanude
        if (::menuOptions.isInitialized) { // Verificar si la vista ha sido inicializada
            menuOptions.visibility = View.GONE
        }
        if (::menuOptionsUser.isInitialized) {
            menuOptionsUser.visibility = View.GONE
        }
        if (::menuOptionsNav.isInitialized) {
            menuOptionsNav.visibility = View.GONE
        }
    }

    private fun cargarImagenPerfil() {
        val profilePictureImageView = findViewById<ImageView>(R.id.profile_picture)
        val fotoPerfilUrl = UsuarioGlobal.fotoPerfil
        Glide.with(this)
            .load(fotoPerfilUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image) // Considera agregar un placeholder
            .error(R.drawable.placeholder_image) // Considera agregar una imagen de error
            .into(profilePictureImageView)
    }

    private fun configurarListenersMenus() { //
        fabMain.setOnClickListener { toggleMenu(menuOptions, R.anim.slide_in_fade, R.anim.slide_out_fade) }
        findViewById<Button>(R.id.btn_register_expense).setOnClickListener {
            startActivity(Intent(this, RegisterGasto::class.java))
        }
        findViewById<Button>(R.id.btn_register_income).setOnClickListener {
            startActivity(Intent(this, RegisterIngreso::class.java))
        }

        fabPerfil.setOnClickListener { toggleMenu(menuOptionsUser, R.anim.slide_down_fade_in, R.anim.slide_up_fade_out) }
        findViewById<Button>(R.id.btn_editar_usuario).setOnClickListener {
            startActivity(Intent(this, UpdateUser::class.java))
        }
        findViewById<Button>(R.id.btn_logOut).setOnClickListener {

            UsuarioGlobal.id = null
            UsuarioGlobal.token = null
            val intent = Intent(this, LogIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        }

        fabOpciones.setOnClickListener { toggleMenu(menuOptionsNav, R.anim.slide_down_fade_in, R.anim.slide_up_fade_out) }

        findViewById<Button>(R.id.btn_ver_gastos).setOnClickListener {
            val intent = Intent(this, FiltrarTransacciones::class.java)
            intent.putExtra("TIPO_FILTRO", "gastos")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_ver_ingresos).setOnClickListener {
            val intent = Intent(this, FiltrarTransacciones::class.java)
            intent.putExtra("TIPO_FILTRO", "ingresos")
            startActivity(intent)
        }
        findViewById<Button>(R.id.btn_reporte).setOnClickListener {
            startActivity(Intent(this, Report::class.java))
        }
    }

    private fun toggleMenu(menu: LinearLayout, animIn: Int, animOut: Int) { //
        if (menu.visibility == View.GONE) {
            val animation = AnimationUtils.loadAnimation(this, animIn)
            menu.visibility = View.VISIBLE
            menu.startAnimation(animation)
        } else {
            val animation = AnimationUtils.loadAnimation(this, animOut)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) { menu.visibility = View.GONE }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            menu.startAnimation(animation)
        }
    }

    private fun configurarRadioGroupListener() { //
        radioGroupTipoTransaccion.setOnCheckedChangeListener { group, checkedId ->
            cargarTransaccionesDelMesActual()
        }
    }

    private fun cargarTransaccionesDelMesActual() { //
        val calendar = Calendar.getInstance()
        val mesActual = calendar.get(Calendar.MONTH) + 1
        val anioActual = calendar.get(Calendar.YEAR)

        val tipoSeleccionado = if (radioGroupTipoTransaccion.checkedRadioButtonId == R.id.radioButtonGasto) {
            "gastos"
        } else {
            "ingresos"
        }

        textViewLeyendaMes.text = "${obtenerNombreMes(mesActual-1)}, $anioActual:" // Actualiza leyenda

        fetchTransacciones(tipoSeleccionado, mesActual, anioActual)
    }

    private fun fetchTransacciones(endpoint: String, mes: Int, anio: Int) { //
        val usuarioID = UsuarioGlobal.id
        if (usuarioID.isNullOrEmpty()) {
            Toast.makeText(this, "ID de usuario no disponible.", Toast.LENGTH_SHORT).show()
            return
        }

        val queryParams = mapOf(
            "usuarioID" to usuarioID,
            "mes" to mes.toString(),
            "anio" to anio.toString(),
            "limite" to "6",
            "pagina" to "1"
        )

        transactionService.obtenerTransacciones(endpoint, queryParams,
            onSuccess = { response ->
                try {
                    val transaccionesJsonArray = response.getJSONArray(endpoint)
                    val totalMontoMes = response.optDouble("totalMonto", 0.0)

                    if (endpoint == "gastos") {
                        textViewMontoTotalMes.text = String.format(Locale.getDefault(), "Gastos recientes: $%.2f", totalMontoMes)
                        parseAndDisplayGastos(transaccionesJsonArray)
                    } else {
                        textViewMontoTotalMes.text = String.format(Locale.getDefault(), "Ingresos recientes: $%.2f", totalMontoMes)
                        parseAndDisplayIngresos(transaccionesJsonArray)
                    }

                } catch (e: Exception) {
                    Log.e("DashboardParse", "Error parseando $endpoint: ${e.message}")
                    Toast.makeText(this, "Error al procesar $endpoint.", Toast.LENGTH_SHORT).show()
                    // Limpiar lista en caso de error de parseo
                    if (endpoint == "gastos") {
                        listaGastos.clear()
                        gastoAdapter.notifyDataSetChanged()
                    } else {
                        listaIngresos.clear()
                        ingresoAdapter.notifyDataSetChanged()
                    }
                    textViewMontoTotalMes.text = if (endpoint == "gastos") "Gastos recientes: $0.00" else "Ingresos recientes: $0.00"
                }
            },
            onError = { errorMessage ->
                Log.e("DashboardFetch", "Error obteniendo $endpoint: $errorMessage")
                Toast.makeText(this, "Error al cargar $endpoint.", Toast.LENGTH_SHORT).show()
                if (endpoint == "gastos") {
                    listaGastos.clear()
                    gastoAdapter.notifyDataSetChanged()
                } else {
                    listaIngresos.clear()
                    ingresoAdapter.notifyDataSetChanged()
                }
                textViewMontoTotalMes.text = if (endpoint == "gastos") "Gastos del mes: $0.00" else "Ingresos del mes: $0.00"
            }
        )
    }

    private fun parseAndDisplayGastos(jsonArray: JSONArray) { //
        listaGastos.clear()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            try {
                val gasto = gastoFactory.crearTransaccion(
                    idUser = item.optString("Id_user", UsuarioGlobal.id ?: ""),
                    nombre = item.getString("Nombre"),
                    descripcion = item.optString("Descripcion"),
                    fecha = formatarFechaBonita(item.getString("FechaLocal")), //
                    monto = item.getDouble("Monto"),
                    tipo = item.getString("Tipo"),
                    archivo = item.optString("Archivo")
                )
                listaGastos.add(gasto as Gasto)
            } catch (e: Exception) {
                Log.e("DashboardFactory", "Error creando Gasto desde JSON: ${e.message} - JSON: $item")
            }
        }
        recyclerViewTransacciones.adapter = gastoAdapter
        gastoAdapter.notifyDataSetChanged()
    }

    private fun parseAndDisplayIngresos(jsonArray: JSONArray) { //
        listaIngresos.clear()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            try {
                val ingreso = ingresoFactory.crearTransaccion(
                    idUser = item.optString("Id_user", UsuarioGlobal.id ?: ""),
                    nombre = item.getString("Nombre"),
                    descripcion = item.optString("Descripcion"),
                    fecha = formatarFechaBonita(item.getString("FechaLocal")), //
                    monto = item.getDouble("Monto"),
                    tipo = item.getString("Tipo"),
                    archivo = item.optString("Archivo")
                )
                listaIngresos.add(ingreso as Ingreso)
            } catch (e: Exception) {
                Log.e("DashboardFactory", "Error creando Ingreso desde JSON: ${e.message} - JSON: $item")
            }
        }
        recyclerViewTransacciones.adapter = ingresoAdapter
        ingresoAdapter.notifyDataSetChanged()
    }

    private fun formatarFechaBonita(fechaISO: String): String { //
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = inputFormat.parse(fechaISO)
            val outputFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) //
            date?.let { outputFormat.format(it) } ?: fechaISO
        } catch (e: Exception) {
            Log.e("DateParseError", "Error formateando fecha: $fechaISO", e)
            fechaISO.substringBefore("T") //
        }
    }

    private fun obtenerNombreMes(monthIndex: Int): String { //
        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return if (monthIndex in meses.indices) meses[monthIndex] else ""
    }

    private fun onGastoClicked(gasto: Gasto) { //
        val intent = Intent(this, DetalleGasto::class.java).apply {
            putExtra("gastoId", gasto.idUser)
            putExtra("nombre", gasto.nombre)
            putExtra("monto", gasto.monto.toString())
            putExtra("fecha", gasto.fecha)
            putExtra("tipo", gasto.tipo)
            putExtra("descripcion", gasto.descripcion)
            putExtra("archivo", gasto.archivo)
        }
        startActivity(intent)
    }

    private fun onIngresoClicked(ingreso: Ingreso) { //
        val intent = Intent(this, DetalleIngreso::class.java).apply {
            putExtra("ingresoId", ingreso.idUser)
            putExtra("nombre", ingreso.nombre)
            putExtra("monto", ingreso.monto.toString())
            putExtra("fecha", ingreso.fecha)
            putExtra("tipo", ingreso.tipo)
            putExtra("descripcion", ingreso.descripcion)
            putExtra("archivo", ingreso.archivo)
        }
        startActivity(intent)
    }
}