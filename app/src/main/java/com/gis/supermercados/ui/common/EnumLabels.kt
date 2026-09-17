package com.gis.supermercados.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gis.supermercados.R
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.PeriodType
import com.gis.supermercados.core.reporting.model.ReportType
import com.gis.supermercados.domain.model.DocumentType
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.domain.model.LogLevel
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.PeriodType
import com.gis.supermercados.core.reporting.model.ReportType
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.ProductUnit
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.domain.model.ThemeMode
import com.gis.supermercados.domain.model.ThemeMode

/**
 * Etiquetas localizadas de los enums dentro de la interfaz.
 *
 * Usan LAS MISMAS claves de recurso que `core.common.Labels` (la version para
 * contextos sin composicion, usada en informes y exportaciones): una unica
 * traduccion por concepto, sin duplicidades ni textos divergentes.
 */

@Composable
fun roleLabel(role: Role): String = stringResource(
    when (role) {
        Role.ADMIN -> R.string.role_admin
        Role.GERENTE -> R.string.role_manager
        Role.SUPERVISOR -> R.string.role_supervisor
        Role.CAJERO -> R.string.role_cashier
    }
)

@Composable
fun paymentLabel(method: PaymentMethod): String = stringResource(paymentLabelRes(method))

@StringRes
fun paymentLabelRes(method: PaymentMethod): Int = when (method) {
    PaymentMethod.EFECTIVO -> R.string.payment_cash
    PaymentMethod.TARJETA_DEBITO -> R.string.payment_debit
    PaymentMethod.TARJETA_CREDITO -> R.string.payment_credit
    PaymentMethod.TRANSFERENCIA -> R.string.payment_transfer
    PaymentMethod.VALE -> R.string.payment_voucher
    PaymentMethod.CREDITO_CLIENTE -> R.string.payment_customer_credit
    PaymentMethod.OTRO -> R.string.payment_other
}

@Composable
fun saleStatusLabel(status: SaleStatus): String = stringResource(
    when (status) {
        SaleStatus.COMPLETADA -> R.string.sale_status_completed
        SaleStatus.PARCIALMENTE_DEVUELTA -> R.string.sale_status_partially_returned
        SaleStatus.DEVUELTA -> R.string.sale_status_returned
        SaleStatus.ANULADA -> R.string.sale_status_void
    }
)

@Composable
fun movementLabel(type: MovementType): String = stringResource(
    when (type) {
        MovementType.ENTRADA -> R.string.movement_entry
        MovementType.SALIDA -> R.string.movement_exit
        MovementType.TRANSFERENCIA -> R.string.movement_transfer
        MovementType.AJUSTE_POSITIVO -> R.string.movement_adjustment_up
        MovementType.AJUSTE_NEGATIVO -> R.string.movement_adjustment_down
        MovementType.VENTA -> R.string.movement_sale
        MovementType.DEVOLUCION_CLIENTE -> R.string.movement_customer_return
        MovementType.DEVOLUCION_PROVEEDOR -> R.string.movement_supplier_return
    }
)

@Composable
fun expenseCategoryLabel(category: ExpenseCategory): String = stringResource(
    when (category) {
        ExpenseCategory.NOMINA -> R.string.expense_payroll
        ExpenseCategory.SERVICIOS_BASICOS -> R.string.expense_utilities
        ExpenseCategory.ALQUILER -> R.string.expense_rent
        ExpenseCategory.MANTENIMIENTO -> R.string.expense_maintenance
        ExpenseCategory.PUBLICIDAD -> R.string.expense_advertising
        ExpenseCategory.COMPRA_MERCANCIA -> R.string.expense_merchandise
        ExpenseCategory.TRANSPORTE -> R.string.expense_transport
        ExpenseCategory.IMPUESTOS -> R.string.expense_taxes
        ExpenseCategory.SEGUROS -> R.string.expense_insurance
        ExpenseCategory.TECNOLOGIA -> R.string.expense_technology
        ExpenseCategory.OTROS -> R.string.expense_other
    }
)

@Composable
fun unitLabel(unit: ProductUnit): String = stringResource(
    when (unit) {
        ProductUnit.UNIDAD -> R.string.unit_unit
        ProductUnit.KILOGRAMO -> R.string.unit_kilogram
        ProductUnit.GRAMO -> R.string.unit_gram
        ProductUnit.LITRO -> R.string.unit_liter
        ProductUnit.MILILITRO -> R.string.unit_milliliter
        ProductUnit.METRO -> R.string.unit_meter
        ProductUnit.CAJA -> R.string.unit_box
        ProductUnit.PAQUETE -> R.string.unit_pack
        ProductUnit.DOCENA -> R.string.unit_dozen
        ProductUnit.SERVICIO -> R.string.unit_service
    }
)

@Composable
fun documentTypeLabel(type: DocumentType): String = stringResource(
    when (type) {
        DocumentType.CEDULA -> R.string.doc_id_card
        DocumentType.DNI -> R.string.doc_dni
        DocumentType.RUC -> R.string.doc_ruc
        DocumentType.NIT -> R.string.doc_nit
        DocumentType.PASAPORTE -> R.string.doc_passport
        DocumentType.OTRO -> R.string.doc_other
    }
)

@Composable
fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SISTEMA -> R.string.theme_system
        ThemeMode.CLARO -> R.string.theme_light
        ThemeMode.OSCURO -> R.string.theme_dark
    }
)

@Composable
fun logLevelLabel(level: LogLevel): String = stringResource(
    when (level) {
        LogLevel.DEBUG -> R.string.log_debug
        LogLevel.INFO -> R.string.log_info
        LogLevel.WARN -> R.string.log_warn
        LogLevel.ERROR -> R.string.log_error
    }
)

@Composable
fun reportTypeLabel(type: ReportType): String = stringResource(reportTypeLabelRes(type))

@StringRes
fun reportTypeLabelRes(type: ReportType): Int = when (type) {
    ReportType.RESUMEN_EJECUTIVO -> R.string.report_type_executive
    ReportType.BALANCE_GENERAL -> R.string.report_type_balance
    ReportType.FLUJO_CAJA -> R.string.report_type_cash_flow
    ReportType.VENTAS_POR_TIENDA -> R.string.report_type_sales_by_store
    ReportType.PRODUCTOS_MAS_VENDIDOS -> R.string.report_type_top_products
    ReportType.RENTABILIDAD_CATEGORIA -> R.string.report_type_category_profit
    ReportType.ANALISIS_GASTOS -> R.string.report_type_expense_analysis
    ReportType.COMPARATIVA_PERIODOS -> R.string.report_type_comparison
    ReportType.PROYECCIONES -> R.string.report_type_projection
    ReportType.INVENTARIO_VALORADO -> R.string.report_type_inventory
}

@Composable
fun periodTypeLabel(type: PeriodType): String = stringResource(
    when (type) {
        PeriodType.DIARIO -> R.string.period_daily
        PeriodType.SEMANAL -> R.string.period_weekly
        PeriodType.MENSUAL -> R.string.period_monthly
        PeriodType.TRIMESTRAL -> R.string.period_quarterly
        PeriodType.SEMESTRAL -> R.string.period_semester
        PeriodType.ANUAL -> R.string.period_annual
        PeriodType.PERSONALIZADO -> R.string.period_custom
    }
)

@Composable
fun exportFormatLabel(format: ExportFormat): String = stringResource(
    when (format) {
        ExportFormat.PDF -> R.string.format_pdf
        ExportFormat.EXCEL -> R.string.format_excel
        ExportFormat.CSV -> R.string.format_csv
    }
)
