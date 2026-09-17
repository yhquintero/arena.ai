package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.gis.supermercados.data.local.entity.CategoryProfitRow
import com.gis.supermercados.data.local.entity.CustomerSpendRow
import com.gis.supermercados.data.local.entity.DayExpenseRow
import com.gis.supermercados.data.local.entity.DayTotalRow
import com.gis.supermercados.data.local.entity.ExpenseCategoryRow
import com.gis.supermercados.data.local.entity.ExpenseStoreRow
import com.gis.supermercados.data.local.entity.MoneyCountRow
import com.gis.supermercados.data.local.entity.PaymentMethodRow
import com.gis.supermercados.data.local.entity.ReturnsRow
import com.gis.supermercados.data.local.entity.StoreSalesRow
import com.gis.supermercados.data.local.entity.TopProductRow

/**
 * Consultas agregadas que alimentan el motor de reportes.
 *
 * Convenciones:
 * - Todos los importes estan en CENTAVOS.
 * - Las ventas ANULADAS nunca computan como ingreso.
 * - [tzOffset] permite agrupar por DIA LOCAL usando epoch-millis UTC:
 *   ((created_at + tzOffset) / 86400000) es el numero de dia local.
 * - Un parametro nulo significa "sin filtro" (todas las tiendas / todo el periodo).
 */
@Dao
interface ReportQueriesDao {

    // ------------------------------ VENTAS ------------------------------

    /** Totales globales del periodo (una unica fila). */
    @Query(
        """
        SELECT 0 AS store_id, '' AS store_name, COUNT(s.id) AS tickets,
               IFNULL(SUM(s.subtotal_cents), 0) AS subtotal_cents,
               IFNULL(SUM(s.discount_cents), 0) AS discount_cents,
               IFNULL(SUM(s.tax_cents), 0) AS tax_cents,
               IFNULL(SUM(s.total_cents), 0) AS total_cents,
               IFNULL(SUM(s.cost_cents), 0) AS cost_cents
        FROM sales s
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND (:storeId IS NULL OR s.store_id = :storeId)
        """
    )
    suspend fun salesTotals(start: Long, end: Long, storeId: Long?): StoreSalesRow

    @Query(
        """
        SELECT s.store_id AS store_id, st.name AS store_name, COUNT(s.id) AS tickets,
               IFNULL(SUM(s.subtotal_cents), 0) AS subtotal_cents,
               IFNULL(SUM(s.discount_cents), 0) AS discount_cents,
               IFNULL(SUM(s.tax_cents), 0) AS tax_cents,
               IFNULL(SUM(s.total_cents), 0) AS total_cents,
               IFNULL(SUM(s.cost_cents), 0) AS cost_cents
        FROM sales s
        INNER JOIN stores st ON st.id = s.store_id
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND (:storeId IS NULL OR s.store_id = :storeId)
        GROUP BY s.store_id, st.name
        ORDER BY total_cents DESC
        """
    )
    suspend fun salesByStore(start: Long, end: Long, storeId: Long?): List<StoreSalesRow>

    /** Serie diaria de ventas (base de graficos y proyecciones). */
    @Query(
        """
        SELECT ((s.created_at + :tzOffset) / 86400000) * 86400000 - :tzOffset AS day_bucket,
               IFNULL(SUM(s.total_cents), 0) AS total_cents,
               IFNULL(SUM(s.cost_cents), 0) AS cost_cents,
               COUNT(s.id) AS tickets
        FROM sales s
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND (:storeId IS NULL OR s.store_id = :storeId)
        GROUP BY (s.created_at + :tzOffset) / 86400000
        ORDER BY day_bucket ASC
        """
    )
    suspend fun dailySales(
        start: Long,
        end: Long,
        storeId: Long?,
        tzOffset: Long,
    ): List<DayTotalRow>

    @Query(
        """
        SELECT si.product_id AS product_id, si.product_name AS product_name, si.sku AS sku,
               IFNULL(c.name, '') AS category_name,
               IFNULL(SUM(CASE WHEN si.quantity > 0 THEN si.quantity - si.returned_quantity ELSE 0 END), 0) AS units,
               IFNULL(SUM(CASE WHEN si.quantity > 0
                        THEN (si.line_total_cents * (si.quantity - si.returned_quantity)) / si.quantity
                        ELSE 0 END), 0) AS total_cents,
               IFNULL(SUM(CASE WHEN si.quantity > 0
                        THEN si.unit_cost_cents * (si.quantity - si.returned_quantity)
                        ELSE 0 END), 0) AS cost_cents
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.sale_id
        LEFT JOIN products p ON p.id = si.product_id
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND (:storeId IS NULL OR s.store_id = :storeId)
        GROUP BY si.product_id, si.product_name, si.sku, c.name
        ORDER BY units DESC, total_cents DESC
        LIMIT :limit
        """
    )
    suspend fun topProducts(start: Long, end: Long, storeId: Long?, limit: Int): List<TopProductRow>

    @Query(
        """
        SELECT c.id AS category_id, c.name AS category_name, c.color AS color,
               IFNULL(SUM(CASE WHEN si.quantity > 0 THEN si.quantity - si.returned_quantity ELSE 0 END), 0) AS units,
               IFNULL(SUM(CASE WHEN si.quantity > 0
                        THEN (si.line_total_cents * (si.quantity - si.returned_quantity)) / si.quantity
                        ELSE 0 END), 0) AS sales_cents,
               IFNULL(SUM(CASE WHEN si.quantity > 0
                        THEN si.unit_cost_cents * (si.quantity - si.returned_quantity)
                        ELSE 0 END), 0) AS cost_cents
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.sale_id
        INNER JOIN products p ON p.id = si.product_id
        INNER JOIN categories c ON c.id = p.category_id
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND (:storeId IS NULL OR s.store_id = :storeId)
        GROUP BY c.id, c.name, c.color
        ORDER BY sales_cents DESC
        """
    )
    suspend fun categoryProfitability(
        start: Long,
        end: Long,
        storeId: Long?,
    ): List<CategoryProfitRow>

    @Query(
        """
        SELECT s.payment_method AS payment_method,
               IFNULL(SUM(s.total_cents), 0) AS total_cents,
               COUNT(s.id) AS tickets
        FROM sales s
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND (:storeId IS NULL OR s.store_id = :storeId)
        GROUP BY s.payment_method
        ORDER BY total_cents DESC
        """
    )
    suspend fun salesByPaymentMethod(
        start: Long,
        end: Long,
        storeId: Long?,
    ): List<PaymentMethodRow>

    @Query(
        """
        SELECT s.customer_id AS customer_id, IFNULL(c.full_name, '') AS customer_name,
               COUNT(s.id) AS tickets, IFNULL(SUM(s.total_cents), 0) AS total_cents
        FROM sales s
        LEFT JOIN customers c ON c.id = s.customer_id
        WHERE s.created_at BETWEEN :start AND :end
          AND s.status != 'ANULADA'
          AND s.customer_id IS NOT NULL
          AND (:storeId IS NULL OR s.store_id = :storeId)
        GROUP BY s.customer_id, c.full_name
        ORDER BY total_cents DESC
        LIMIT :limit
        """
    )
    suspend fun topCustomers(start: Long, end: Long, storeId: Long?, limit: Int): List<CustomerSpendRow>

    // ------------------------------ GASTOS ------------------------------

    @Query(
        """
        SELECT IFNULL(SUM(e.total_cents), 0) AS total_cents, 0 AS cost_cents, COUNT(e.id) AS cnt
        FROM expenses e
        WHERE e.expense_date BETWEEN :start AND :end
          AND (:storeId IS NULL OR e.store_id = :storeId)
        """
    )
    suspend fun expenseTotals(start: Long, end: Long, storeId: Long?): MoneyCountRow

    @Query(
        """
        SELECT e.category AS category,
               IFNULL(SUM(e.total_cents), 0) AS total_cents,
               COUNT(e.id) AS cnt
        FROM expenses e
        WHERE e.expense_date BETWEEN :start AND :end
          AND (:storeId IS NULL OR e.store_id = :storeId)
        GROUP BY e.category
        ORDER BY total_cents DESC
        """
    )
    suspend fun expensesByCategory(
        start: Long,
        end: Long,
        storeId: Long?,
    ): List<ExpenseCategoryRow>

    @Query(
        """
        SELECT e.store_id AS store_id, IFNULL(st.name, 'Corporativo') AS store_name,
               IFNULL(SUM(e.total_cents), 0) AS total_cents, COUNT(e.id) AS cnt
        FROM expenses e
        LEFT JOIN stores st ON st.id = e.store_id
        WHERE e.expense_date BETWEEN :start AND :end
        GROUP BY e.store_id, st.name
        ORDER BY total_cents DESC
        """
    )
    suspend fun expensesByStore(start: Long, end: Long): List<ExpenseStoreRow>

    @Query(
        """
        SELECT ((e.expense_date + :tzOffset) / 86400000) * 86400000 - :tzOffset AS day_bucket,
               IFNULL(SUM(e.total_cents), 0) AS total_cents
        FROM expenses e
        WHERE e.expense_date BETWEEN :start AND :end
          AND (:storeId IS NULL OR e.store_id = :storeId)
        GROUP BY (e.expense_date + :tzOffset) / 86400000
        ORDER BY day_bucket ASC
        """
    )
    suspend fun dailyExpenses(
        start: Long,
        end: Long,
        storeId: Long?,
        tzOffset: Long,
    ): List<DayExpenseRow>

    // --------------------------- DEVOLUCIONES ---------------------------

    @Query(
        """
        SELECT IFNULL(SUM(cn.total_cents), 0) AS total_cents, 0 AS cost_cents, COUNT(cn.id) AS cnt
        FROM credit_notes cn
        WHERE cn.created_at BETWEEN :start AND :end
          AND (:storeId IS NULL OR cn.store_id = :storeId)
        """
    )
    suspend fun returnsTotals(start: Long, end: Long, storeId: Long?): MoneyCountRow

    @Query(
        """
        SELECT cn.store_id AS store_id, IFNULL(st.name, '') AS store_name,
               IFNULL(SUM(cn.total_cents), 0) AS total_cents, COUNT(cn.id) AS cnt
        FROM credit_notes cn
        LEFT JOIN stores st ON st.id = cn.store_id
        WHERE cn.created_at BETWEEN :start AND :end
        GROUP BY cn.store_id, st.name
        ORDER BY total_cents DESC
        """
    )
    suspend fun returnsByStore(start: Long, end: Long): List<ReturnsRow>

    @Query(
        """
        SELECT ((cn.created_at + :tzOffset) / 86400000) * 86400000 - :tzOffset AS day_bucket,
               IFNULL(SUM(cn.total_cents), 0) AS total_cents
        FROM credit_notes cn
        WHERE cn.created_at BETWEEN :start AND :end
          AND (:storeId IS NULL OR cn.store_id = :storeId)
        GROUP BY (cn.created_at + :tzOffset) / 86400000
        ORDER BY day_bucket ASC
        """
    )
    suspend fun dailyReturns(
        start: Long,
        end: Long,
        storeId: Long?,
        tzOffset: Long,
    ): List<DayExpenseRow>
}
