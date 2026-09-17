package com.gis.supermercados.data.local

import androidx.room.TypeConverter
import com.gis.supermercados.domain.model.DocumentType
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.domain.model.LogLevel
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.ProductUnit
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.domain.model.ThemeMode

/**
 * Conversores de tipos para Room.
 *
 * Los enums se persisten por NOMBRE y no por ordinal: si en el futuro se
 * anade o reordena un valor, los datos guardados siguen siendo validos.
 * Las conversiones usan [runCatching] para que un valor corrupto nunca
 * provoque un crash al leer la base de datos.
 */
class Converters {

    // ---- Enums de negocio ----

    @TypeConverter fun fromRole(value: Role): String = value.name
    @TypeConverter fun toRole(value: String): Role =
        runCatching { Role.valueOf(value) }.getOrDefault(Role.CAJERO)

    @TypeConverter fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun toPaymentMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.EFECTIVO)

    @TypeConverter fun fromSaleStatus(value: SaleStatus): String = value.name
    @TypeConverter fun toSaleStatus(value: String): SaleStatus =
        runCatching { SaleStatus.valueOf(value) }.getOrDefault(SaleStatus.COMPLETADA)

    @TypeConverter fun fromMovementType(value: MovementType): String = value.name
    @TypeConverter fun toMovementType(value: String): MovementType =
        runCatching { MovementType.valueOf(value) }.getOrDefault(MovementType.AJUSTE_POSITIVO)

    @TypeConverter fun fromExpenseCategory(value: ExpenseCategory): String = value.name
    @TypeConverter fun toExpenseCategory(value: String): ExpenseCategory =
        runCatching { ExpenseCategory.valueOf(value) }.getOrDefault(ExpenseCategory.OTROS)

    @TypeConverter fun fromProductUnit(value: ProductUnit): String = value.name
    @TypeConverter fun toProductUnit(value: String): ProductUnit =
        runCatching { ProductUnit.valueOf(value) }.getOrDefault(ProductUnit.UNIDAD)

    @TypeConverter fun fromDocumentType(value: DocumentType): String = value.name
    @TypeConverter fun toDocumentType(value: String): DocumentType =
        runCatching { DocumentType.valueOf(value) }.getOrDefault(DocumentType.CEDULA)

    @TypeConverter fun fromThemeMode(value: ThemeMode): String = value.name
    @TypeConverter fun toThemeMode(value: String): ThemeMode =
        runCatching { ThemeMode.valueOf(value) }.getOrDefault(ThemeMode.SISTEMA)

    @TypeConverter fun fromLogLevel(value: LogLevel): String = value.name
    @TypeConverter fun toLogLevel(value: String): LogLevel =
        runCatching { LogLevel.valueOf(value) }.getOrDefault(LogLevel.INFO)

    // ---- Colecciones simples ----

    /** Set de dias ISO (1 = lunes .. 7 = domingo) a texto "1,2,3,4,5". */
    @TypeConverter fun fromDaySet(value: Set<Int>): String = value.sorted().joinToString(",")

    @TypeConverter fun toDaySet(value: String): Set<Int> = value.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()

    @TypeConverter fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter fun toLongList(value: String): List<Long> =
        value.split(",").mapNotNull { it.trim().toLongOrNull() }
}
