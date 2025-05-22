package FactoryMethod

interface Transaccion {
    val idUser: String
    val nombre: String
    val descripcion: String?
    val fecha: String
    val monto: Double
    val tipo: String?
    val archivo: String?
}
