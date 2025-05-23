package com.example.proyectomov

import FactoryMethod.Gasto
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


class GastoAdapter(
    var gastos: MutableList<Gasto>,
    private val onItemClicked: (Gasto) -> Unit
) : RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {

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


    class GastoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fecha: TextView = itemView.findViewById(R.id.fecha)
        val titulo: TextView = itemView.findViewById(R.id.titulo)
        val monto: TextView = itemView.findViewById(R.id.monto)
        val iconoDocumento: ImageView = itemView.findViewById(R.id.icono_documento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val gasto = gastos[position]
        holder.fecha.text = formatarFechaBonita(gasto.fecha) // Format date for display
        holder.titulo.text = gasto.nombre
        holder.monto.text = String.format(Locale.getDefault(), "$%.2f", gasto.monto)

        if (!gasto.archivo.isNullOrEmpty()) {
            holder.iconoDocumento.visibility = View.VISIBLE
        } else {
            holder.iconoDocumento.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClicked(gasto)
        }
    }

    override fun getItemCount(): Int = gastos.size
}