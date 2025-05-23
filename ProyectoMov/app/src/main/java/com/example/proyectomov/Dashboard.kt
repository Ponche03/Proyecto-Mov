package com.example.proyectomov

import FactoryMethod.Gasto
import FactoryMethod.Ingreso
import FactoryMethod.GastoFactory
import FactoryMethod.IngresoFactory
import Services.TransactionService
import UsuarioGlobal
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import internalStorage.NetworkUtils
import internalStorage.TransactionRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

import internalStorage.toDomainModel
import internalStorage.toDomainModel

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException


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
    private lateinit var offlineIndicator: TextView

    private val transactionService by lazy { TransactionService(this) }
    private val gastoFactory = GastoFactory()
    private val ingresoFactory = IngresoFactory()

    private val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(applicationContext)
    }

    private var listaGastosAdapter: MutableList<Gasto> = mutableListOf()
    private var listaIngresosAdapter: MutableList<Ingreso> = mutableListOf()

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
        offlineIndicator = findViewById(R.id.offline_indicator)


        radioGroupTipoTransaccion = findViewById(R.id.radioGroupTipoTransaccion)
        recyclerViewTransacciones = findViewById(R.id.recyclerViewTransacciones)
        recyclerViewTransacciones.layoutManager = LinearLayoutManager(this)


        gastoAdapter = GastoAdapter(listaGastosAdapter, this::onGastoClicked)
        ingresoAdapter = IngresoAdapter(listaIngresosAdapter, this::onIngresoClicked)


        cargarImagenPerfil()
        configurarListenersMenus()
        configurarRadioGroupListener()

        loadAndObserveData()
    }

    override fun onResume() {
        super.onResume()
        if (::menuOptions.isInitialized) menuOptions.visibility = View.GONE
        if (::menuOptionsUser.isInitialized) menuOptionsUser.visibility = View.GONE
        if (::menuOptionsNav.isInitialized) menuOptionsNav.visibility = View.GONE

        loadAndObserveData()
    }

    private fun cargarImagenPerfil() {
        val profilePictureImageView = findViewById<ImageView>(R.id.profile_picture)
        val fotoPerfilUrl = UsuarioGlobal.fotoPerfil
        Glide.with(this)
            .load(fotoPerfilUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(profilePictureImageView)
    }

    private fun configurarListenersMenus() {
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

    private fun toggleMenu(menu: LinearLayout, animIn: Int, animOut: Int) {
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

    private fun configurarRadioGroupListener() {
        radioGroupTipoTransaccion.setOnCheckedChangeListener { _, _ ->
            loadAndObserveData()
        }
    }

    private fun loadAndObserveData() {
        val userId = UsuarioGlobal.id
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            return
        }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        textViewLeyendaMes.text = "${obtenerNombreMes(currentMonth - 1)}, $currentYear:"

        if (!NetworkUtils.isNetworkAvailable(this)) {
            offlineIndicator.visibility = View.VISIBLE
             Toast.makeText(this, "Modo offline: mostrando datos locales.", Toast.LENGTH_SHORT).show();
        } else {
            offlineIndicator.visibility = View.GONE

            fetchTotalAmountFromAPI(currentMonth, currentYear)
        }


        // Observe local data regardless of network state
        lifecycleScope.launch {
            val tipoSeleccionado = if (radioGroupTipoTransaccion.checkedRadioButtonId == R.id.radioButtonGasto) {
                "gastos"
            } else {
                "ingresos"
            }

            if (tipoSeleccionado == "gastos") {
                transactionRepository.getGastos(userId).collectLatest { gastoEntities ->

                    val gastosDelMesDomain = gastoEntities.filter { entity ->
                        isTransactionInMonthYear(entity.fecha, currentMonth, currentYear)
                    }.map { it.toDomainModel() }

                    listaGastosAdapter.clear()
                    listaGastosAdapter.addAll(gastosDelMesDomain.take(6))
                    gastoAdapter.notifyDataSetChanged()
                    recyclerViewTransacciones.adapter = gastoAdapter
                    if(!NetworkUtils.isNetworkAvailable(this@Dashboard)){
                        val totalLocal = gastosDelMesDomain.sumOf { it.monto }
                        textViewMontoTotalMes.text = String.format(Locale.getDefault(), "Gastos del mes: $%.2f", totalLocal)
                    }
                }
            } else {
                transactionRepository.getIngresos(userId).collectLatest { ingresoEntities ->
                    val ingresosDelMesDomain = ingresoEntities.filter { entity ->
                        isTransactionInMonthYear(entity.fecha, currentMonth, currentYear)
                    }.map { it.toDomainModel() }

                    listaIngresosAdapter.clear()
                    listaIngresosAdapter.addAll(ingresosDelMesDomain.take(6))
                    ingresoAdapter.notifyDataSetChanged()
                    recyclerViewTransacciones.adapter = ingresoAdapter
                    if(!NetworkUtils.isNetworkAvailable(this@Dashboard)){
                        val totalLocal = ingresosDelMesDomain.sumOf { it.monto }
                        textViewMontoTotalMes.text = String.format(Locale.getDefault(), "Ingresos del mes: $%.2f", totalLocal)
                    }
                }
            }
        }
    }


    private fun isTransactionInMonthYear(transactionDateStr: String, month: Int, year: Int): Boolean {
        try {

            val odt = OffsetDateTime.parse(transactionDateStr)
            val zonedDateTimeUTC = odt.atZoneSameInstant(ZoneId.of("UTC"))
            Log.d("DateDebug", "Original: \"$transactionDateStr\", ParsedMonth UTC: ${zonedDateTimeUTC.monthValue}, ParsedYear UTC: ${zonedDateTimeUTC.year}")

            return zonedDateTimeUTC.monthValue == month && zonedDateTimeUTC.year == year

        } catch (e: DateTimeParseException) {
            Log.e("DashboardDateParse", "Error parseando fecha con OffsetDateTime: \"$transactionDateStr\"", e)
            return false
        } catch (e: Exception) {
            Log.e("DashboardDateParse", "Error genérico procesando fecha: \"$transactionDateStr\"", e)
            return false
        }
    }

    private fun fetchTotalAmountFromAPI(mes: Int, anio: Int) {
        val userId = UsuarioGlobal.id ?: return
        val endpoint = if (radioGroupTipoTransaccion.checkedRadioButtonId == R.id.radioButtonGasto) "gastos" else "ingresos"

        val queryParams = mapOf(
            "usuarioID" to userId,
            "mes" to mes.toString(),
            "anio" to anio.toString()
        )

        transactionService.obtenerTransacciones(endpoint, queryParams,
            onSuccess = { response ->
                try {
                    val totalMontoMes = response.optDouble("totalMonto", 0.0)
                    if (endpoint == "gastos") {
                        textViewMontoTotalMes.text = String.format(Locale.getDefault(), "Gastos del mes: $%.2f", totalMontoMes)
                    } else {
                        textViewMontoTotalMes.text = String.format(Locale.getDefault(), "Ingresos del mes: $%.2f", totalMontoMes)
                    }
                } catch (e: Exception) {
                    Log.e("DashboardFetchTotal", "Error parsing total from API for $endpoint: ${e.message}")

                }
            },
            onError = { errorMessage ->
                Log.e("DashboardFetchTotal", "Error fetching total from API for $endpoint: $errorMessage")
            }
        )
    }


    private fun obtenerNombreMes(monthIndex: Int): String {
        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return if (monthIndex in meses.indices) meses[monthIndex] else ""
    }

    private fun onGastoClicked(gasto: Gasto) {
        val intent = Intent(this, DetalleGasto::class.java).apply {
            putExtra("EXTRA_TRANSACTION_ID", gasto.transactionId)
            putExtra("nombre", gasto.nombre)
            putExtra("monto", gasto.monto.toString())
            putExtra("fecha", gasto.fecha)
            putExtra("tipo", gasto.tipo)
            putExtra("descripcion", gasto.descripcion)
            putExtra("archivo", gasto.archivo)
        }
        startActivity(intent)
    }

    private fun onIngresoClicked(ingreso: Ingreso) {
        val intent = Intent(this, DetalleIngreso::class.java).apply {
            putExtra("EXTRA_TRANSACTION_ID", ingreso.transactionId)
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