package internalStorage

import FactoryMethod.Gasto
import FactoryMethod.Ingreso
import internalStorage.GastoEntity
import internalStorage.IngresoEntity

fun GastoEntity.toDomainModel(): Gasto {
    return Gasto(
        transactionId = this.transactionId,
        idUser = this.idUser,
        nombre = this.nombre,
        descripcion = this.descripcion,
        fecha = this.fecha,
        monto = this.monto,
        tipo = this.tipo,
        archivo = this.archivo
    )
}

fun IngresoEntity.toDomainModel(): Ingreso {
    return Ingreso(
        transactionId = this.transactionId,
        idUser = this.idUser,
        nombre = this.nombre,
        descripcion = this.descripcion,
        fecha = this.fecha,
        monto = this.monto,
        tipo = this.tipo,
        archivo = this.archivo
    )
}