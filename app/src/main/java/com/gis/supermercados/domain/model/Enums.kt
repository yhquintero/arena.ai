package com.gis.supermercados.domain.model

/**
 * Enums del dominio. Se persisten por nombre (Room TypeConverter) para que la
 * base de datos sea legible y las migraciones no dependan de ordinales.
 */

/** Rol del usuario: define los permisos funcionales dentro de la app. */
enum class Role {
    ADMIN,
    GERENTE,
    SUPERVISOR,
    CAJERO;

    val canManageStores: Boolean get() = this == ADMIN
    val canManageUsers: Boolean get() = this == ADMIN
    val canManageProducts: Boolean get() = this == ADMIN || this == GERENTE
    val canAdjustInventory: Boolean
        get() = this == ADMIN || this == GERENTE || this == SUPERVISOR
    val canTransferStock: Boolean
        get() = this == ADMIN || this == GERENTE || this == SUPERVISOR
    val canRegisterExpenses: Boolean get() = this == ADMIN || this == GERENTE
    val canViewReports: Boolean
        get() = this == ADMIN || this == GERENTE || this == SUPERVISOR
    val canViewGlobalReports: Boolean get() = this == ADMIN || this == GERENTE
    val canProcessSales: Boolean get() = true
    val canApproveReturns: Boolean
        get() = this == ADMIN || this == GERENTE || this == SUPERVISOR
    val canManageCustomers: Boolean get() = this != CAJERO
    val canManageSettings: Boolean get() = this == ADMIN
    val canManageBackups: Boolean get() = this == ADMIN || this == GERENTE
    val canViewLogs: Boolean get() = this == ADMIN
}

/** Formas de pago admitidas en el punto de venta y en gastos. */
enum class PaymentMethod {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA,
    VALE,
    CREDITO_CLIENTE,
    OTRO;

    val isCash: Boolean get() = this == EFECTIVO
}

/** Ciclo de vida de una venta. */
enum class SaleStatus {
    COMPLETADA,
    PARCIALMENTE_DEVUELTA,
    DEVUELTA,
    ANULADA;

    val countsForRevenue: Boolean get() = this != ANULADA
}

/** Tipos de movimiento de inventario. [increasesStock] indica el efecto en la tienda origen. */
enum class MovementType(val increasesStock: Boolean) {
    ENTRADA(true),
    SALIDA(false),
    TRANSFERENCIA(false),
    AJUSTE_POSITIVO(true),
    AJUSTE_NEGATIVO(false),
    VENTA(false),
    DEVOLUCION_CLIENTE(true),
    DEVOLUCION_PROVEEDOR(false);

    val isAdjustment: Boolean get() = this == AJUSTE_POSITIVO || this == AJUSTE_NEGATIVO
    val isTransfer: Boolean get() = this == TRANSFERENCIA
}

/** Categorias estandar de gastos operacionales. */
enum class ExpenseCategory {
    NOMINA,
    SERVICIOS_BASICOS,
    ALQUILER,
    MANTENIMIENTO,
    PUBLICIDAD,
    COMPRA_MERCANCIA,
    TRANSPORTE,
    IMPUESTOS,
    SEGUROS,
    TECNOLOGIA,
    OTROS;

    /** Los gastos de mercancia no son "operacionales": se reportan aparte. */
    val isOperational: Boolean get() = this != COMPRA_MERCANCIA && this != IMPUESTOS
}

/** Unidades de medida (supermercado mixto: productos a granel y servicios). */
enum class ProductUnit {
    UNIDAD,
    KILOGRAMO,
    GRAMO,
    LITRO,
    MILILITRO,
    METRO,
    CAJA,
    PAQUETE,
    DOCENA,
    SERVICIO;

    val allowsDecimals: Boolean
        get() = this == KILOGRAMO || this == GRAMO || this == LITRO || this == MILILITRO || this == METRO
}

/** Tipo de documento de identidad del cliente. */
enum class DocumentType {
    CEDULA,
    DNI,
    RUC,
    NIT,
    PASAPORTE,
    OTRO
}

/** Modo de tema de la interfaz. */
enum class ThemeMode { SISTEMA, CLARO, OSCURO }

/** Niveles del log interno de la aplicacion. */
enum class LogLevel { DEBUG, INFO, WARN, ERROR }
