package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.Category
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.ProductStockSummary
import kotlinx.coroutines.flow.Flow

/** CRUD de productos, servicios y categorias. */
interface CatalogRepository {

    fun observeCategories(): Flow<List<Category>>

    suspend fun getCategories(): List<Category>

    suspend fun getCategory(categoryId: Long): Category?

    suspend fun saveCategory(category: Category): AppResult<Long>

    suspend fun deleteCategory(categoryId: Long): AppResult<Unit>

    /** Busqueda reactiva por nombre, SKU o codigo de barras. */
    fun observeProducts(query: String?, categoryId: Long?): Flow<List<Product>>

    suspend fun getProduct(productId: Long): Product?

    /** Busca por SKU o codigo de barras (lector del punto de venta). */
    suspend fun findBySkuOrBarcode(term: String): Product?

    suspend fun saveProduct(product: Product): AppResult<Long>

    suspend fun setActive(productId: Long, isActive: Boolean): AppResult<Unit>

    suspend fun deleteProduct(productId: Long): AppResult<Unit>

    suspend fun isSkuTaken(sku: String, excludeId: Long = 0L): Boolean

    suspend fun stockSummary(productId: Long): ProductStockSummary?

    fun observeActiveProductCount(): Flow<Int>
}
