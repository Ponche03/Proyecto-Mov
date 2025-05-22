package FactoryMethod

class IngresoFactory : TransaccionFactory {
    override fun crearTransaccion(
        idUser: String,
        nombre: String,
        descripcion: String?,
        fecha: String,
        monto: Double,
        tipo: String?,
        archivo: String?
    ): Transaccion {
        return Ingreso(idUser, nombre, descripcion, fecha, monto, tipo, archivo)
    }
}