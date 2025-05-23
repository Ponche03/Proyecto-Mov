package FactoryMethod

interface TransaccionFactory {
    fun crearTransaccion(
        idUser: String,
        nombre: String,
        descripcion: String?,
        fecha: String, // Should be ISO date string
        monto: Double,
        tipo: String?,
        archivo: String?,
        transactionId: String? = null // Make nullable and last parameter with default
    ): Transaccion
}