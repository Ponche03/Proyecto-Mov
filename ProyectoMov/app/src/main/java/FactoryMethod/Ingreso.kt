package FactoryMethod

data class Ingreso(
    override val idUser: String,
    override val nombre: String,
    override val descripcion: String?,
    override val fecha: String,
    override val monto: Double,
    override val tipo: String?,
    override val archivo: String?
) : Transaccion