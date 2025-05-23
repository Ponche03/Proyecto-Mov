package com.example.proyectomov

import FactoryMethod.Ingreso
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class IngresoAdapter(
    val ingresos: List<Ingreso>,
    private val onItemClicked: (Ingreso) -> Unit
) : RecyclerView.Adapter<IngresoAdapter.IngresoViewHolder>() {

    private fun formatarFechaBonita(fechaISO: String): String {
        if (fechaISO.isNullOrEmpty()) {
            return "Fecha no disponible"
        }

        val patrones = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        var fechaParseada: java.util.Date? = null

        for (patron in patrones) {
            try {
                fechaParseada = patron.parse(fechaISO)
                if (fechaParseada != null) break
            } catch (e: Exception) {

            }
        }

        return if (fechaParseada != null) {

            val outputFormat = SimpleDateFormat("dd MMM yy", Locale("es", "ES"))

            outputFormat.format(fechaParseada)
        } else {
            Log.e("DateParseErrorAdapter", "Error formateando fecha con todos los patrones: $fechaISO")
            fechaISO.substringBefore("T")
        }
    }


    class IngresoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fecha: TextView = itemView.findViewById(R.id.fecha)
        val titulo: TextView = itemView.findViewById(R.id.titulo)
        val monto: TextView = itemView.findViewById(R.id.monto)
        val iconoDocumento: ImageView = itemView.findViewById(R.id.icono_documento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngresoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingreso, parent, false)
        return IngresoViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngresoViewHolder, position: Int) {
        val ingreso = ingresos[position]
        holder.fecha.text = formatarFechaBonita(ingreso.fecha)
        holder.titulo.text = ingreso.nombre
        holder.monto.text = String.format(Locale.getDefault(), "$%.2f", ingreso.monto)

        if (!ingreso.archivo.isNullOrEmpty()) {
            holder.iconoDocumento.visibility = View.VISIBLE
        } else {
            holder.iconoDocumento.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClicked(ingreso)
        }
    }

    override fun getItemCount(): Int = ingresos.size
}