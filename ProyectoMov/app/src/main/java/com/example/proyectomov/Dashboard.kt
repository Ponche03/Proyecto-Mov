package com.example.proyectomov
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        lateinit var fabMain: ImageButton
        lateinit var fabPerfil: ShapeableImageView
        lateinit var fabOpciones: ImageView

        lateinit var menuOptions: LinearLayout
        lateinit var menuOptionsUser: LinearLayout
        lateinit var menuOptionsNav: LinearLayout

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Cargar imagen de perfil con el link.
        val profilePicture = findViewById<ImageView>(R.id.profile_picture)

        val nombre = UsuarioGlobal.nombreUsuario
        val fotoPerfilUrl = UsuarioGlobal.fotoPerfil

        Glide.with(this)
            .load(fotoPerfilUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(profilePicture)



        // Menú de opciones: Crear ingreso / gasto
        fabMain = findViewById(R.id.fab_main)
        menuOptions = findViewById(R.id.menu_options)

        fabMain.setOnClickListener {
            if (menuOptions.visibility == View.GONE) {

                // Animación de entrada
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_fade)
                menuOptions.visibility = View.VISIBLE
                menuOptions.startAnimation(animation)
            } else {

                // Animación de salida
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_fade)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        menuOptions.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                menuOptions.startAnimation(animation)
            }
        }

        findViewById<Button>(R.id.btn_register_expense).setOnClickListener {
            val intent = Intent(this, RegisterGasto::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_register_income).setOnClickListener {
            val intent = Intent(this, RegisterIngreso::class.java)
            startActivity(intent)
        }




        // Menu de opciones: Foto de Perfil
        fabPerfil = findViewById(R.id.profile_picture)
        menuOptionsUser = findViewById(R.id.menu_options_pfp)

        fabPerfil.setOnClickListener {
            if (menuOptionsUser.visibility == View.GONE) {
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
                menuOptionsUser.visibility = View.VISIBLE
                menuOptionsUser.startAnimation(animation)
            } else {
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_out)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        menuOptionsUser.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                menuOptionsUser.startAnimation(animation)
            }
        }

        findViewById<Button>(R.id.btn_editar_usuario).setOnClickListener {
            val intent = Intent(this, UpdateUser::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_logOut).setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }


        // Menú de opciones: Navegación Principal
        fabOpciones = findViewById(R.id.app_icon)
        menuOptionsNav = findViewById(R.id.menu_options_main)

        fabOpciones.setOnClickListener {
            if (menuOptionsNav.visibility == View.GONE) {
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
                menuOptionsNav.visibility = View.VISIBLE
                menuOptionsNav.startAnimation(animation)
            } else {
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_out)
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        menuOptionsNav.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                menuOptionsNav.startAnimation(animation)
            }
        }


        findViewById<Button>(R.id.btn_ver_ingresos).setOnClickListener {
            val intent = Intent(this, FiltrarRegistros::class.java)
            intent.putExtra("tipo_filtro", "ingreso")  // <-- Envía "ingreso"
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_ver_gastos).setOnClickListener {
            val intent = Intent(this, FiltrarRegistros::class.java)
            intent.putExtra("tipo_filtro", "gasto")  // <-- Envía "gasto"
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_reporte).setOnClickListener {
            val intent = Intent(this, Report::class.java)
            startActivity(intent)
        }


    }
}