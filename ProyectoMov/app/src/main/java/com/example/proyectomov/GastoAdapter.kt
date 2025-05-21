package com.example.proyectomov // Cambia esto si tu paquete es otro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Gasto(
    val nombre: String,
    val fecha: String,
    val tipo: String,
    val monto: Double,
    val archivo: String
)

class GastoAdapter(private val gastos: List<Gasto>) :
    RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {

    class GastoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fecha: TextView = itemView.findViewById(R.id.fecha)
        val titulo: TextView = itemView.findViewById(R.id.titulo)
        val monto: TextView = itemView.findViewById(R.id.monto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false) // Asegúrate de que el layout es el correcto
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val gasto = gastos[position]
        holder.fecha.text = gasto.fecha
        holder.titulo.text = gasto.nombre
        holder.monto.text = "$%.2f".format(gasto.monto)
    }

    override fun getItemCount(): Int = gastos.size
}