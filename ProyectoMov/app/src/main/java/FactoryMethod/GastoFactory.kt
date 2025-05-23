package FactoryMethod

class GastoFactory : TransaccionFactory {
    override fun crearTransaccion(
        idUser: String,
        nombre: String,
        descripcion: String?,
        fecha: String,
        monto: Double,
        tipo: String?,
        archivo: String?,
        transactionId: String? // Parameter matches interface
    ): Transaccion {
        // Pass transactionId to the Gasto constructor.
        // If it's null (e.g., during new registration), Gasto's default null will be used.
        return Gasto(
            transactionId = transactionId,
            idUser = idUser,
            nombre = nombre,
            descripcion = descripcion,
            fecha = fecha,
            monto = monto,
            tipo = tipo,
            archivo = archivo
        )
    }
}