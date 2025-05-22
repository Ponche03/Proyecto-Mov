package FactoryMethod

class GastoFactory : TransaccionFactory {
    override fun crearTransaccion(
        idUser: String,
        nombre: String,
        descripcion: String?,
        fecha: String,
        monto: Double,
        tipo: String?,
        archivo: String?
    ): Transaccion {
        return Gasto(idUser, nombre, descripcion, fecha, monto, tipo, archivo)
    }
}