package com.example.proyectomov  // Cambia esto si tu paquete es otro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
data class Ingreso(
    val nombre: String,
    val fecha: String,
    val tipo: String,
    val monto: Double,
    val archivo: String
)

class IngresoAdapter(private val ingresos: List<Ingreso>) :
    RecyclerView.Adapter<IngresoAdapter.IngresoViewHolder>() {

    class IngresoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fecha: TextView = itemView.findViewById(R.id.fecha)
        val titulo: TextView = itemView.findViewById(R.id.titulo)
        val monto: TextView = itemView.findViewById(R.id.monto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngresoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingreso, parent, false)
        return IngresoViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngresoViewHolder, position: Int) {
        val ingreso = ingresos[position]
        holder.fecha.text = ingreso.fecha
        holder.titulo.text = ingreso.nombre
        holder.monto.text = "$%.2f".format(ingreso.monto)
    }

    override fun getItemCount(): Int = ingresos.size
}
