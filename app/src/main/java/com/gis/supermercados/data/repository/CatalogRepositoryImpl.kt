package com.gis.supermercados.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.data.mapper.toEntity
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.Category
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.ProductStockSummary
import com.gis.supermercados.domain.repository.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** CRUD de categorias y productos (incluye servicios del supermercado mixto). */
@Singleton
class CatalogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CatalogRepository {

    private val productDao get() = database.productDao()
    private val categoryDao get() = database.categoryDao()

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getCategories(): List<Category> =
        withContext(ioDispatcher) { categoryDao.getAll().map { it.toDomain() } }

    override suspend fun getCategory(categoryId: Long): Category? =
        withContext(ioDispatcher) { categoryDao.getById(categoryId)?.toDomain() }

    override suspend fun saveCategory(category: Category): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.category_error_save)) {
            withContext(ioDispatcher) {
                val name = category.name.trim()
                if (name.isBlank()) {
                    throw IllegalArgumentException(context.getString(R.string.category_error_name_required))
                }
                val existingId = categoryDao.getIdByName(name)
                if (existingId != null && existingId != category.id) {
                    throw IllegalArgumentException(context.getString(R.string.category_error_name_taken, name))
                }
                val entity = category.toEntity().copy(name = name)
                if (category.id == 0L) categoryDao.insert(entity) else {
                    categoryDao.update(entity)
                    entity.id
                }
            }
        }

    override suspend fun deleteCategory(categoryId: Long): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.category_error_delete)) {
            withContext(ioDispatcher) {
                try {
                    categoryDao.deleteById(categoryId)
                } catch (constraint: SQLiteConstraintException) {
                    // La clave foranea es RESTRICT: hay productos usando la categoria.
                    throw IllegalArgumentException(context.getString(R.string.category_error_in_use))
                }
                Unit
            }
        }

    override fun observeProducts(query: String?, categoryId: Long?): Flow<List<Product>> =
        productDao.observeFiltered(
            query = query?.trim()?.takeIf { it.isNotEmpty() },
            categoryId = categoryId?.takeIf { it > 0L }
        ).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getProduct(productId: Long): Product? = withContext(ioDispatcher) {
        val entity = productDao.getById(productId) ?: return@withContext null
        val categoryName = categoryDao.getById(entity.categoryId)?.name.orEmpty()
        entity.toDomain(categoryName)
    }

    override suspend fun findBySkuOrBarcode(term: String): Product? = withContext(ioDispatcher) {
        val clean = term.trim()
        if (clean.isEmpty()) return@withContext null
        val entity = productDao.getByBarcode(clean) ?: productDao.getBySku(clean.uppercase())
        entity?.let { product ->
            product.toDomain(categoryDao.getById(product.categoryId)?.name.orEmpty())
        }
    }

    override suspend fun saveProduct(product: Product): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.product_error_save)) {
            withContext(ioDispatcher) {
                val name = product.name.trim()
                val sku = product.sku.trim().uppercase()
                if (name.isBlank()) throw IllegalArgumentException(context.getString(R.string.product_error_name_required))
                if (sku.isBlank()) throw IllegalArgumentException(context.getString(R.string.product_error_sku_required))
                if (productDao.getIdBySkuExcluding(sku, product.id) != null) {
                    throw IllegalArgumentException(context.getString(R.string.product_error_sku_taken, sku))
                }
                if (product.priceCents < 0 || product.costCents < 0) {
                    throw IllegalArgumentException(context.getString(R.string.product_error_negative_price))
                }
                if (categoryDao.getById(product.categoryId) == null) {
                    throw IllegalArgumentException(context.getString(R.string.product_error_invalid_category))
                }

                val now = System.currentTimeMillis()
                val entity = product.toEntity().copy(
                    name = name,
                    sku = sku,
                    createdAt = if (product.id == 0L) now else product.createdAt,
                    updatedAt = now
                )
                val id = if (product.id == 0L) productDao.insert(entity) else {
                    productDao.update(entity)
                    entity.id
                }
                AppLogger.i(TAG, "Producto guardado: $name ($sku)")
                id
            }
        }

    override suspend fun setActive(productId: Long, isActive: Boolean): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.product_error_update)) {
            withContext(ioDispatcher) {
                productDao.setActive(productId, isActive, System.currentTimeMillis())
                Unit
            }
        }

    override suspend fun deleteProduct(productId: Long): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.product_error_delete)) {
            withContext(ioDispatcher) {
                // El historial de ventas conserva nombre/SKU (foto del momento),
                // por lo que borrar el producto no rompe los informes antiguos.
                val entity = productDao.getById(productId) ?: throw IllegalArgumentException(
                    context.getString(R.string.product_error_not_found)
                )
                // Se limpian existencias para no dejar huérfanos; el historial de
                // ventas permanece intacto porque guarda foto del producto.
                database.stockDao().getByProduct(productId).forEach {
                    database.stockDao().delete(it.productId, it.storeId)
                }
                productDao.update(entity.copy(isActive = false))
                AppLogger.w(TAG, "Producto $productId dado de baja")
                Unit
            }
        }

    override suspend fun isSkuTaken(sku: String, excludeId: Long): Boolean =
        withContext(ioDispatcher) {
            productDao.getIdBySkuExcluding(sku.trim().uppercase(), excludeId) != null
        }

    override suspend fun stockSummary(productId: Long): ProductStockSummary? =
        withContext(ioDispatcher) {
            val product = getProduct(productId) ?: return@withContext null
            val row = productDao.stockSummary(productId) ?: return@withContext null
            ProductStockSummary(
                product = product,
                totalQuantity = row.totalQuantity,
                lowStockStores = row.lowStockStores,
                outOfStockStores = row.outOfStockStores,
                stockValueCents = row.stockValueCents
            )
        }

    override fun observeActiveProductCount(): Flow<Int> =
        productDao.observeActiveCount().flowOn(ioDispatcher)

    private companion object {
        const val TAG = "CatalogRepository"
    }
}
