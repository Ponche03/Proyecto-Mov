package FactoryMethod

class IngresoFactory : TransaccionFactory {
    override fun crearTransaccion(
        idUser: String,
        nombre: String,
        descripcion: String?,
        fecha: String,
        monto: Double,
        tipo: String?,
        archivo: String?,
        transactionId: String?
    ): Transaccion {
        return Ingreso(
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