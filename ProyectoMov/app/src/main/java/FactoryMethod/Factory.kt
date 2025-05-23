package FactoryMethod

interface TransaccionFactory {
    fun crearTransaccion(
        idUser: String,
        nombre: String,
        descripcion: String?,
        fecha: String,
        monto: Double,
        tipo: String?,
        archivo: String?,
        transactionId: String? = null
    ): Transaccion
}