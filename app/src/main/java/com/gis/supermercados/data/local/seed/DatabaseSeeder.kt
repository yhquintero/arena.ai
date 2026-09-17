package com.gis.supermercados.data.local.seed

import androidx.room.withTransaction
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.security.PasswordHasher
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.CategoryEntity
import com.gis.supermercados.data.local.entity.CreditNoteEntity
import com.gis.supermercados.data.local.entity.CreditNoteItemEntity
import com.gis.supermercados.data.local.entity.CustomerEntity
import com.gis.supermercados.data.local.entity.ExpenseEntity
import com.gis.supermercados.data.local.entity.InventoryMovementEntity
import com.gis.supermercados.data.local.entity.ProductEntity
import com.gis.supermercados.data.local.entity.SaleEntity
import com.gis.supermercados.data.local.entity.SaleItemEntity
import com.gis.supermercados.data.local.entity.StockEntity
import com.gis.supermercados.data.local.entity.StoreEntity
import com.gis.supermercados.data.local.entity.UserEntity
import com.gis.supermercados.di.ApplicationScope
import com.gis.supermercados.domain.model.DocumentType
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.ProductUnit
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Datos de ejemplo realistas para que la aplicacion se pueda probar (y demostrar)
 * desde el primer arranque: 3 sucursales, catalogo mixto, 90 dias de ventas,
 * gastos, devoluciones, transferencias y usuarios con distintos roles.
 *
 * TODO es DETERMINISTA: se usa `Random(SEED)`, de modo que dos instalaciones
 * distintas generan exactamente los mismos datos (imprescindible para los tests
 * de informes y para poder reproducir cualquier incidencia).
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: GisDatabase,
    private val passwordHasher: PasswordHasher,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    /** Lanza la siembra en segundo plano (no bloquea el arranque de la app). */
    fun seedInBackground() {
        applicationScope.launch {
            runCatching { seedIfEmpty() }
                .onSuccess { seeded -> if (seeded) AppLogger.i(TAG, "Datos de ejemplo cargados") }
                .onFailure { AppLogger.e(TAG, "No se pudieron cargar los datos de ejemplo", it) }
        }
    }

    /** Siembra solo si la base de datos esta vacia y aun no se ha hecho antes. */
    suspend fun seedIfEmpty(): Boolean = withContext(Dispatchers.IO) {
        if (settingsRepository.isSampleDataLoaded()) return@withContext false
        val hasData = database.userDao().count() > 0 || database.storeDao().countActive() > 0
        if (hasData) {
            settingsRepository.markSampleDataLoaded()
            return@withContext false
        }
        seed()
        settingsRepository.markSampleDataLoaded()
        true
    }

    /** Crea todo el conjunto de datos de ejemplo en UNA transaccion. */
    suspend fun seed() = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val random = Random(SEED)
        val now = started

        database.withTransaction {
            val storeIds = seedStores(now)
            val categoryIds = seedCategories()
            val products = seedProducts(categoryIds, now)
            val userIds = seedUsers(storeIds, now)
            val customerIds = seedCustomers(now, random)
            val soldByStore = seedSales(products, storeIds, customerIds, userIds, now, random)
            seedStock(products, storeIds, soldByStore, now, random)
            seedTransfers(products, storeIds, userIds, now, random)
            seedExpenses(storeIds, userIds, now, random)
            seedReturns(storeIds, userIds, now, random)
        }
        AppLogger.i(TAG, "Siembra completada en ${System.currentTimeMillis() - started} ms")
    }

    // --------------------------------- tiendas ---------------------------------

    private suspend fun seedStores(now: Long): List<Long> {
        val stores = listOf(
            StoreEntity(
                name = "Supermercado Central",
                code = "CEN",
                address = "Calle 23 #1234 e/ 12 y 14",
                city = "La Habana",
                phone = "+53 7 833 1234",
                email = "central@gis.cu",
                managerName = "María Fernández",
                taxId = "9876543210",
                openingTime = "08:00",
                closingTime = "21:00",
                maxDiscountPercent = 25,
                notes = "Tienda insignia con carnicería y panadería propias.",
                createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY,
                updatedAt = now
            ),
            StoreEntity(
                name = "Mercado Playa",
                code = "PLY",
                address = "Avenida 41 #5602",
                city = "La Habana",
                phone = "+53 7 204 5678",
                email = "playa@gis.cu",
                managerName = "Jorge Díaz",
                taxId = "9876543211",
                openingTime = "09:00",
                closingTime = "20:00",
                maxDiscountPercent = 15,
                notes = "Formato mediano, fuerte en frescos.",
                createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY,
                updatedAt = now
            ),
            StoreEntity(
                name = "Mini Market Vedado",
                code = "VED",
                address = "Calle L #455",
                city = "La Habana",
                phone = "+53 7 877 9090",
                email = "vedado@gis.cu",
                managerName = "Ana Rodríguez",
                taxId = "9876543212",
                openingTime = "07:30",
                closingTime = "23:00",
                hasExtendedHours = true,
                maxDiscountPercent = 10,
                notes = "Conveniencia 24/5 con servicios rápidos.",
                createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY,
                updatedAt = now
            )
        )
        return stores.map { database.storeDao().insert(it) }
    }

    // -------------------------------- categorias -------------------------------

    private suspend fun seedCategories(): Map<String, Long> {
        val categories = listOf(
            CategoryEntity(name = "Abarrotes", description = "Granos, conservas y básicos", color = 0xFF1B6EF3L, targetMarginPercent = 22, sortOrder = 1),
            CategoryEntity(name = "Bebidas", description = "Aguas, refrescos y jugos", color = 0xFF00A3A3L, targetMarginPercent = 28, sortOrder = 2),
            CategoryEntity(name = "Carnicería", description = "Carnes frescas y embutidos", color = 0xFFD64545L, targetMarginPercent = 20, sortOrder = 3),
            CategoryEntity(name = "Lácteos", description = "Leche, quesos y yogures", color = 0xFFF2B705L, targetMarginPercent = 18, sortOrder = 4),
            CategoryEntity(name = "Frutas y Verduras", description = "Frescos del día", color = 0xFF3FA34DL, targetMarginPercent = 30, sortOrder = 5),
            CategoryEntity(name = "Panadería", description = "Pan y repostería propia", color = 0xFFB5651D, targetMarginPercent = 35, sortOrder = 6),
            CategoryEntity(name = "Limpieza", description = "Hogar y lavandería", color = 0xFF6C5CE7L, targetMarginPercent = 25, sortOrder = 7),
            CategoryEntity(name = "Cuidado Personal", description = "Higiene y cosmética", color = 0xFFE84393L, targetMarginPercent = 32, sortOrder = 8),
            CategoryEntity(name = "Congelados", description = "Helados y precocinados", color = 0xFF3498DBL, targetMarginPercent = 24, sortOrder = 9),
            CategoryEntity(name = "Servicios", description = "Servicios del supermercado mixto", color = 0xFF2D3436L, targetMarginPercent = 60, sortOrder = 10)
        )
        val ids = database.categoryDao().insertAll(categories)
        return categories.mapIndexed { index, category -> category.name to ids[index] }.toMap()
    }

    // -------------------------------- productos --------------------------------

    private suspend fun seedProducts(categoryIds: Map<String, Long>, now: Long): List<ProductEntity> {
        fun id(category: String): Long = categoryIds.getValue(category)
        val createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY

        val catalog = listOf(
            // nombre, sku, codigo de barras, categoria, costo, precio, tasa, unidad, servicio, proveedor
            SeedProduct("Arroz blanco 1 kg", "ABR-001", "8501234567890", "Abarrotes", 95_00, 145_00, 0.0, ProductUnit.PAQUETE, false, "Distribuidora Nacional"),
            SeedProduct("Frijoles negros 1 kg", "ABR-002", "8501234567891", "Abarrotes", 110_00, 168_00, 0.0, ProductUnit.PAQUETE, false, "Distribuidora Nacional"),
            SeedProduct("Azúcar refinada 1 kg", "ABR-003", "8501234567892", "Abarrotes", 60_00, 95_00, 0.0, ProductUnit.PAQUETE, false, "Central Azucarera"),
            SeedProduct("Aceite vegetal 1 L", "ABR-004", "8501234567893", "Abarrotes", 180_00, 265_00, 0.0, ProductUnit.LITRO, false, "Importadora Caribe"),
            SeedProduct("Pasta espagueti 500 g", "ABR-005", "8501234567894", "Abarrotes", 70_00, 110_00, 0.0, ProductUnit.PAQUETE, false, "Importadora Caribe"),
            SeedProduct("Café molido 250 g", "ABR-006", "8501234567895", "Abarrotes", 210_00, 340_00, 0.0, ProductUnit.PAQUETE, false, "Café Sierra Maestra"),
            SeedProduct("Agua natural 1.5 L", "BEB-001", "8502234567890", "Bebidas", 40_00, 70_00, 0.16, ProductUnit.UNIDAD, false, "Embotelladora Habana"),
            SeedProduct("Refresco de cola 2 L", "BEB-002", "8502234567891", "Bebidas", 95_00, 155_00, 0.16, ProductUnit.UNIDAD, false, "Embotelladora Habana"),
            SeedProduct("Jugo de naranja 1 L", "BEB-003", "8502234567892", "Bebidas", 85_00, 140_00, 0.16, ProductUnit.UNIDAD, false, "Frutas del Valle"),
            SeedProduct("Cerveza lata 355 ml", "BEB-004", "8502234567893", "Bebidas", 60_00, 110_00, 0.16, ProductUnit.UNIDAD, false, "Cervecería Bucanero"),
            SeedProduct("Pollo entero fresco (kg)", "CAR-001", "8503234567890", "Carnicería", 210_00, 320_00, 0.0, ProductUnit.KILOGRAMO, false, "Granja Avícola Sur"),
            SeedProduct("Carne de cerdo (kg)", "CAR-002", "8503234567891", "Carnicería", 320_00, 470_00, 0.0, ProductUnit.KILOGRAMO, false, "Frigorífico Central"),
            SeedProduct("Jamón cocido (kg)", "CAR-003", "8503234567892", "Carnicería", 280_00, 420_00, 0.0, ProductUnit.KILOGRAMO, false, "Embutidos Habana"),
            SeedProduct("Leche entera 1 L", "LAC-001", "8504234567890", "Lácteos", 75_00, 115_00, 0.0, ProductUnit.LITRO, false, "Lácteos Escambray"),
            SeedProduct("Queso gouda (kg)", "LAC-002", "8504234567891", "Lácteos", 480_00, 720_00, 0.0, ProductUnit.KILOGRAMO, false, "Lácteos Escambray"),
            SeedProduct("Yogur natural 1 kg", "LAC-003", "8504234567892", "Lácteos", 130_00, 195_00, 0.0, ProductUnit.KILOGRAMO, false, "Lácteos Escambray"),
            SeedProduct("Mantequilla 200 g", "LAC-004", "8504234567893", "Lácteos", 150_00, 230_00, 0.0, ProductUnit.UNIDAD, false, "Importadora Caribe"),
            SeedProduct("Plátano (kg)", "FRU-001", "8505234567890", "Frutas y Verduras", 25_00, 45_00, 0.0, ProductUnit.KILOGRAMO, false, "Cooperativa Valle"),
            SeedProduct("Tomate (kg)", "FRU-002", "8505234567891", "Frutas y Verduras", 55_00, 95_00, 0.0, ProductUnit.KILOGRAMO, false, "Cooperativa Valle"),
            SeedProduct("Mango (kg)", "FRU-003", "8505234567892", "Frutas y Verduras", 45_00, 80_00, 0.0, ProductUnit.KILOGRAMO, false, "Cooperativa Valle"),
            SeedProduct("Cebolla (kg)", "FRU-004", "8505234567893", "Frutas y Verduras", 60_00, 100_00, 0.0, ProductUnit.KILOGRAMO, false, "Cooperativa Valle"),
            SeedProduct("Pan blanco 500 g", "PAN-001", "8506234567890", "Panadería", 40_00, 75_00, 0.0, ProductUnit.UNIDAD, false, "Producción propia"),
            SeedProduct("Pan integral 500 g", "PAN-002", "8506234567891", "Panadería", 55_00, 95_00, 0.0, ProductUnit.UNIDAD, false, "Producción propia"),
            SeedProduct("Croquetas (docena)", "PAN-003", "8506234567892", "Panadería", 120_00, 210_00, 0.0, ProductUnit.DOCENA, false, "Producción propia"),
            SeedProduct("Detergente en polvo 1 kg", "LIM-001", "8507234567890", "Limpieza", 140_00, 225_00, 0.16, ProductUnit.PAQUETE, false, "Química Cubana"),
            SeedProduct("Jabón de baño", "LIM-002", "8507234567891", "Limpieza", 45_00, 80_00, 0.16, ProductUnit.UNIDAD, false, "Química Cubana"),
            SeedProduct("Cloro 1 L", "LIM-003", "8507234567892", "Limpieza", 35_00, 65_00, 0.16, ProductUnit.LITRO, false, "Química Cubana"),
            SeedProduct("Papel higiénico x4", "CUI-001", "8508234567890", "Cuidado Personal", 90_00, 150_00, 0.16, ProductUnit.PAQUETE, false, "Química Cubana"),
            SeedProduct("Pasta dental 100 ml", "CUI-002", "8508234567891", "Cuidado Personal", 65_00, 110_00, 0.16, ProductUnit.UNIDAD, false, "Importadora Caribe"),
            SeedProduct("Helado de vainilla 1 L", "CON-001", "8509234567890", "Congelados", 130_00, 210_00, 0.16, ProductUnit.UNIDAD, false, "Helados Coppelia"),
            SeedProduct("Nuggets congelados 500 g", "CON-002", "8509234567891", "Congelados", 160_00, 260_00, 0.16, ProductUnit.PAQUETE, false, "Frigorífico Central"),
            SeedProduct("Servicio de entrega a domicilio", "SER-001", "", "Servicios", 0, 250_00, 0.16, ProductUnit.SERVICIO, true, ""),
            SeedProduct("Recarga de teléfono móvil", "SER-002", "", "Servicios", 0, 100_00, 0.16, ProductUnit.SERVICIO, true, ""),
            SeedProduct("Corte de carne al gusto", "SER-003", "", "Servicios", 0, 80_00, 0.16, ProductUnit.SERVICIO, true, "")
        )

        val entities = catalog.map { seed ->
            ProductEntity(
                name = seed.name,
                sku = seed.sku,
                barcode = seed.barcode,
                categoryId = id(seed.category),
                costCents = seed.costCents,
                priceCents = seed.priceCents,
                taxRate = seed.taxRate,
                unit = seed.unit,
                isService = seed.isService,
                supplierName = seed.supplier,
                supplierPhone = if (seed.supplier.isEmpty()) "" else "+53 7 555 01${abs(seed.sku.hashCode()) % 90 + 10}",
                minStockGlobal = if (seed.isService) 0 else 12,
                description = "${seed.category} - ${seed.name}",
                createdAt = createdAt,
                updatedAt = now
            )
        }
        val ids = database.productDao().insertAll(entities)
        return entities.mapIndexed { index, entity -> entity.copy(id = ids[index]) }
    }

    // --------------------------------- usuarios --------------------------------

    private suspend fun seedUsers(storeIds: List<Long>, now: Long): List<Long> {
        val salt = passwordHasher.generateSalt()
        val users = listOf(
            SeedUser(DEMO_ADMIN, DEMO_PASSWORD, "Administrador General", Role.ADMIN, null),
            SeedUser(DEMO_MANAGER, DEMO_PASSWORD, "María Fernández", Role.GERENTE, storeIds[0]),
            SeedUser(DEMO_SUPERVISOR, DEMO_PASSWORD, "Jorge Díaz", Role.SUPERVISOR, storeIds[1]),
            SeedUser(DEMO_CASHIER, DEMO_PASSWORD, "Laura Pérez", Role.CAJERO, storeIds[0])
        )
        val entities = users.map { seed ->
            UserEntity(
                username = seed.username,
                fullName = seed.fullName,
                passwordHash = passwordHasher.hash(seed.password, salt),
                passwordSalt = salt,
                role = seed.role,
                storeId = seed.storeId,
                biometricEnabled = false,
                isActive = true,
                createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY,
                lastLoginAt = 0L
            )
        }
        return database.userDao().insertAll(entities)
    }

    // --------------------------------- clientes --------------------------------

    private suspend fun seedCustomers(now: Long, random: Random): List<Long> {
        val customers = listOf(
            CustomerEntity(
                fullName = "Restaurante El Marinero S.A.",
                documentType = DocumentType.NIT,
                documentId = "NIT-100234",
                phone = "+53 7 832 1111",
                email = "compras@elmarinero.cu",
                address = "Malecón #450",
                city = "La Habana",
                creditLimitCents = 500_000_00,
                balanceCents = 78_450_00,
                notes = "Cliente mayorista, entrega los martes.",
                createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY
            ),
            CustomerEntity(
                fullName = "Cafetería Doña Rosa",
                documentType = DocumentType.NIT,
                documentId = "NIT-200876",
                phone = "+53 7 209 4455",
                email = "donarosa@correo.cu",
                address = "Calle 42 #1301",
                city = "La Habana",
                creditLimitCents = 120_000_00,
                balanceCents = 0L,
                notes = "Compra panadería a diario.",
                createdAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY
            ),
            CustomerEntity(
                fullName = "Carlos Manuel Arias",
                documentType = DocumentType.CEDULA,
                documentId = "85042312345",
                phone = "+53 5 234 5678",
                email = "carlos.arias@correo.cu",
                city = "La Habana",
                loyaltyPoints = random.nextInt(400) + 120,
                createdAt = now - (DAYS_OF_HISTORY - 20) * MILLIS_PER_DAY
            )
        )
        return database.customerDao().insertAll(customers)
    }

    // ---------------------------------- ventas ---------------------------------

    /**
     * Genera [DAYS_OF_HISTORY] dias de ventas. Devuelve las unidades vendidas por
     * (producto, tienda) para ajustar las existencias iniciales de forma coherente.
     */
    private suspend fun seedSales(
        products: List<ProductEntity>,
        storeIds: List<Long>,
        customerIds: List<Long>,
        userIds: List<Long>,
        now: Long,
        random: Random,
    ): Map<Pair<Long, Long>, Int> {
        val sellable = products.filter { it.isActive }
        val sold = HashMap<Pair<Long, Long>, Int>()
        val sales = mutableListOf<SaleEntity>()
        val items = mutableListOf<SaleItemEntity>()
        var ticketSequence = 0L

        for (dayOffset in (DAYS_OF_HISTORY - 1) downTo 0) {
            val dayStart = AppDateTime.startOfDay(now - dayOffset * MILLIS_PER_DAY)
            // Fin de semana: mas tickets (patron realista de supermercado).
            val isWeekend = AppDateTime.toLocalDate(dayStart).dayOfWeek.value >= SATURDAY
            val ticketsToday = (if (isWeekend) 9 else 5) + random.nextInt(7)

            repeat(ticketsToday) {
                val storeId = storeIds[random.nextInt(storeIds.size)]
                val cashier = if (random.nextInt(10) < 7) userIds.last() else userIds[random.nextInt(userIds.size)]
                val hour = (if (isWeekend) 9 else 8) + random.nextInt(11)
                val createdAt = minOf(
                    dayStart + hour * MILLIS_PER_HOUR + random.nextInt(MILLIS_PER_HOUR.toInt()).toLong(),
                    now
                )
                val lineCount = 1 + random.nextInt(6)
                val chosen = mutableMapOf<Long, ProductEntity>()
                repeat(lineCount) {
                    val product = sellable[random.nextInt(sellable.size)]
                    chosen[product.id] = product
                }

                ticketSequence += 1
                val salePosition = sales.size.toLong()
                var subtotal = 0L
                var discount = 0L
                var tax = 0L
                var cost = 0L
                var itemCount = 0

                chosen.values.forEach { product ->
                    val maxQty = if (product.unit.allowsDecimals || product.isService) 3 else 5
                    val quantity = (1 + random.nextInt(maxQty)).coerceAtLeast(1)
                    val gross = product.priceCents * quantity
                    val lineDiscountPercent = when {
                        random.nextInt(100) < 8 -> 5 + random.nextInt(11)
                        else -> 0
                    }
                    val net = Money.afterDiscount(gross, lineDiscountPercent)
                    val lineTax = Money.tax(net, product.taxRate)
                    subtotal += gross
                    discount += gross - net
                    tax += lineTax
                    cost += product.costCents * quantity
                    itemCount += quantity
                    sold.merge(product.id to storeId, quantity, Int::plus)

                    items += SaleItemEntity(
                        saleId = salePosition,
                        productId = product.id,
                        productName = product.name,
                        sku = product.sku,
                        quantity = quantity,
                        unitPriceCents = product.priceCents,
                        unitCostCents = product.costCents,
                        discountPercent = lineDiscountPercent,
                        taxRate = product.taxRate,
                        lineTotalCents = net + lineTax
                    )
                }

                // Descuento global ocasional (promocion de cierre de dia).
                val globalPercent = if (random.nextInt(100) < 6) 5 else 0
                val globalDiscount = Money.percentOf(subtotal - discount, globalPercent.toDouble())
                discount += globalDiscount
                val netBase = subtotal - discount
                tax = if (netBase <= 0L) 0L else Money.tax(netBase, weightedTaxRate(chosen.values))

                val method = when (random.nextInt(100)) {
                    in 0..54 -> PaymentMethod.EFECTIVO
                    in 55..79 -> PaymentMethod.TARJETA_DEBITO
                    in 80..89 -> PaymentMethod.TARJETA_CREDITO
                    in 90..94 -> PaymentMethod.TRANSFERENCIA
                    else -> PaymentMethod.CREDITO_CLIENTE
                }
                val customer = if (method == PaymentMethod.CREDITO_CLIENTE || random.nextInt(100) < 25) {
                    customerIds[random.nextInt(customerIds.size)]
                } else {
                    null
                }
                val total = netBase + tax

                sales += SaleEntity(
                    ticketNumber = "TMP-$ticketSequence",
                    storeId = storeId,
                    customerId = customer,
                    userId = cashier,
                    subtotalCents = subtotal,
                    discountCents = discount,
                    taxCents = tax,
                    totalCents = total,
                    costCents = cost,
                    paymentMethod = method,
                    paymentReference = if (method == PaymentMethod.TRANSFERENCIA) "TRF-${1000 + random.nextInt(8999)}" else "",
                    cashReceivedCents = if (method == PaymentMethod.EFECTIVO) total + random.nextInt(200) else 0L,
                    changeCents = if (method == PaymentMethod.EFECTIVO) random.nextInt(200).toLong() else 0L,
                    status = SaleStatus.COMPLETADA,
                    itemCount = itemCount,
                    notes = if (random.nextInt(100) < 5) "Cliente solicita factura." else "",
                    createdAt = createdAt
                )
            }
        }

        // Se insertan las cabeceras y luego los detalles con el id real.
        val realIds = database.saleDao().insertAll(sales)
        database.saleDao().insertItems(items.map { it.copy(saleId = realIds[it.saleId.toInt()]) })

        // Numeros de ticket definitivos (por tienda y orden cronologico).
        val counters = HashMap<Long, Int>()
        sales.forEachIndexed { index, sale ->
            val counter = (counters[sale.storeId] ?: 0) + 1
            counters[sale.storeId] = counter
            database.saleDao().update(
                sale.copy(
                    id = realIds[index],
                    ticketNumber = "V${sale.storeId}-${counter.toString().padStart(5, '0')}"
                )
            )
        }
        AppLogger.i(TAG, "Ventas sembradas: ${sales.size} tickets / ${items.size} lineas")
        return sold
    }

    /** Tasa media ponderada del ticket (por peso de cada producto). */
    private fun weightedTaxRate(products: Collection<ProductEntity>): Double {
        if (products.isEmpty()) return 0.0
        val total = products.sumOf { it.priceCents }
        if (total == 0L) return 0.0
        return products.sumOf { it.taxRate * it.priceCents } / total
    }

    // ---------------------------------- stock ----------------------------------

    private suspend fun seedStock(
        products: List<ProductEntity>,
        storeIds: List<Long>,
        sold: Map<Pair<Long, Long>, Int>,
        now: Long,
        random: Random,
    ) {
        val initialCreatedAt = now - DAYS_OF_HISTORY * MILLIS_PER_DAY
        val stocks = mutableListOf<StockEntity>()
        val movements = mutableListOf<InventoryMovementEntity>()

        products.forEach { product ->
            if (product.isService) return@forEach
            storeIds.forEachIndexed { storeIndex, storeId ->
                val soldUnits = sold[product.id to storeId] ?: 0
                // Stock inicial = lo vendido + colchon variable; se fuerzan algunos
                // productos por debajo del minimo para demostrar las alertas.
                val cushion = 5 + random.nextInt(45)
                val forcedLow = (storeIndex == 2 && product.sku.endsWith("3")) ||
                    (storeIndex == 1 && product.sku.endsWith("7"))
                val quantity = if (forcedLow) {
                    random.nextInt(6)
                } else {
                    (soldUnits + cushion).coerceAtLeast(0)
                }
                val minStock = product.minStockGlobal
                stocks += StockEntity(
                    productId = product.id,
                    storeId = storeId,
                    quantity = quantity,
                    reserved = 0,
                    minStock = minStock,
                    maxStock = minStock * MAX_STOCK_MULTIPLIER,
                    lastCountAt = initialCreatedAt,
                    updatedAt = now - random.nextInt(5) * MILLIS_PER_DAY
                )
                movements += InventoryMovementEntity(
                    productId = product.id,
                    storeId = storeId,
                    type = MovementType.ENTRADA,
                    quantity = soldUnits + quantity,
                    unitCostCents = product.costCents,
                    reason = "Inventario inicial de la siembra",
                    reference = "INV-${product.sku}",
                    userId = 0L,
                    createdAt = initialCreatedAt
                )
            }
        }
        database.stockDao().insertAll(stocks)
        database.inventoryMovementDao().insertAll(movements)
        AppLogger.i(TAG, "Existencias sembradas: ${stocks.size} registros")
    }

    // ------------------------------ transferencias -----------------------------

    private suspend fun seedTransfers(
        products: List<ProductEntity>,
        storeIds: List<Long>,
        userIds: List<Long>,
        now: Long,
        random: Random,
    ) {
        val physical = products.filter { !it.isService }
        val movements = mutableListOf<InventoryMovementEntity>()
        repeat(TRANSFER_COUNT) { index ->
            val product = physical[random.nextInt(physical.size)]
            val from = storeIds[random.nextInt(storeIds.size)]
            val to = storeIds.firstOrNull { it != from } ?: return@repeat
            val quantity = 5 + random.nextInt(20)
            movements += InventoryMovementEntity(
                productId = product.id,
                storeId = from,
                destinationStoreId = to,
                type = MovementType.TRANSFERENCIA,
                quantity = quantity,
                unitCostCents = product.costCents,
                reason = "Reabastecimiento entre sucursales",
                reference = "TR-${(index + 1).toString().padStart(3, '0')}",
                userId = userIds[random.nextInt(userIds.size)],
                createdAt = now - random.nextInt(DAYS_OF_HISTORY) * MILLIS_PER_DAY
            )
        }
        database.inventoryMovementDao().insertAll(movements)
    }

    // ---------------------------------- gastos ---------------------------------

    private suspend fun seedExpenses(storeIds: List<Long>, userIds: List<Long>, now: Long, random: Random) {
        val expenses = mutableListOf<ExpenseEntity>()
        val categories = listOf(
            ExpenseCategory.NOMINA to 180_000_00,
            ExpenseCategory.SERVICIOS_BASICOS to 22_000_00,
            ExpenseCategory.ALQUILER to 45_000_00,
            ExpenseCategory.MANTENIMIENTO to 12_500_00,
            ExpenseCategory.TRANSPORTE to 8_000_00,
            ExpenseCategory.PUBLICIDAD to 6_000_00
        )

        for (month in 0 until MONTHS_OF_EXPENSES) {
            val monthStart = AppDateTime.startOfMonth(now - month * 30L * MILLIS_PER_DAY)
            categories.forEach { (category, base) ->
                storeIds.forEach { storeId ->
                    val amount = base + random.nextInt(base.toInt() / 4 + 1).toLong()
                    val taxRate = if (category == ExpenseCategory.SERVICIOS_BASICOS) 0.16 else 0.0
                    val tax = Money.tax(amount, taxRate)
                    expenses += ExpenseEntity(
                        category = category,
                        storeId = storeId,
                        concept = "${category.name.lowercase().replace('_', ' ')} - sucursal",
                        provider = providerFor(category),
                        amountCents = amount,
                        taxCents = tax,
                        totalCents = amount + tax,
                        paymentMethod = if (random.nextBoolean()) PaymentMethod.TRANSFERENCIA else PaymentMethod.EFECTIVO,
                        reference = "FAC-${month + 1}${storeId}",
                        userId = userIds.first(),
                        expenseDate = monthStart + (1 + random.nextInt(24)) * MILLIS_PER_DAY,
                        createdAt = monthStart + (1 + random.nextInt(24)) * MILLIS_PER_DAY
                    )
                }
            }
            // Gastos corporativos (store_id = null): oficina central y seguros.
            expenses += ExpenseEntity(
                category = ExpenseCategory.SEGUROS,
                storeId = null,
                concept = "Poliza multirriesgo corporativa",
                provider = "Aseguradora Nacional",
                amountCents = 34_000_00,
                taxCents = 0L,
                totalCents = 34_000_00,
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                reference = "POL-$month",
                userId = userIds.first(),
                expenseDate = monthStart + 5 * MILLIS_PER_DAY,
                createdAt = monthStart + 5 * MILLIS_PER_DAY
            )
            expenses += ExpenseEntity(
                category = ExpenseCategory.TECNOLOGIA,
                storeId = null,
                concept = "Licencias y soporte de sistemas",
                provider = "Xetid",
                amountCents = 15_500_00,
                taxCents = Money.tax(15_500_00, 0.16),
                totalCents = 15_500_00 + Money.tax(15_500_00, 0.16),
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                reference = "TEC-$month",
                userId = userIds.first(),
                expenseDate = monthStart + 10 * MILLIS_PER_DAY,
                createdAt = monthStart + 10 * MILLIS_PER_DAY
            )
        }
        database.expenseDao().insertAll(expenses.filter { it.expenseDate <= now })
        AppLogger.i(TAG, "Gastos sembrados: ${expenses.size}")
    }

    private fun providerFor(category: ExpenseCategory): String = when (category) {
        ExpenseCategory.NOMINA -> "Nómina interna"
        ExpenseCategory.SERVICIOS_BASICOS -> "Empresa Eléctrica / Aguas"
        ExpenseCategory.ALQUILER -> "Inmobiliaria Habana"
        ExpenseCategory.MANTENIMIENTO -> "Servicios Técnicos SRL"
        ExpenseCategory.TRANSPORTE -> "Transporte Carga Express"
        ExpenseCategory.PUBLICIDAD -> "Agencia Creativa"
        else -> "Proveedor varios"
    }

    // -------------------------------- devoluciones ------------------------------

    private suspend fun seedReturns(storeIds: List<Long>, userIds: List<Long>, now: Long, random: Random) {
        val candidates = database.saleDao().getRecentForSeed(RETURN_CANDIDATES)
        if (candidates.isEmpty()) return
        var sequence = 0L
        candidates.take(RETURN_COUNT).forEachIndexed { index, sale ->
            if (index % 2 == 1) return@forEachIndexed
            val items = database.saleDao().getItems(sale.id)
            val item = items.firstOrNull { it.quantity > 1 } ?: items.firstOrNull() ?: return@forEachIndexed
            val quantity = 1 + random.nextInt((item.quantity - 1).coerceAtLeast(1))
            val net = Money.afterDiscount(item.unitPriceCents * quantity, item.discountPercent)
            val tax = Money.tax(net, item.taxRate)
            sequence += 1
            val noteId = database.creditNoteDao().insert(
                CreditNoteEntity(
                    creditNoteNumber = "NC-${sequence.toString().padStart(5, '0')}",
                    saleId = sale.id,
                    storeId = sale.storeId,
                    customerId = sale.customerId,
                    userId = userIds.first(),
                    reason = if (random.nextBoolean()) "Producto dañado en transporte" else "Devolucion del cliente",
                    subtotalCents = net,
                    taxCents = tax,
                    totalCents = net + tax,
                    restock = true,
                    itemCount = quantity,
                    createdAt = sale.createdAt + MILLIS_PER_DAY
                )
            )
            database.creditNoteDao().insertItems(
                listOf(
                    CreditNoteItemEntity(
                        creditNoteId = noteId,
                        saleItemId = item.id,
                        productId = item.productId,
                        productName = item.productName,
                        sku = item.sku,
                        quantity = quantity,
                        unitPriceCents = item.unitPriceCents,
                        taxRate = item.taxRate,
                        lineTotalCents = net + tax
                    )
                )
            )
            database.saleDao().addReturnedQuantity(item.id, quantity)
            val updated = database.saleDao().getItems(sale.id)
            database.saleDao().updateStatus(
                sale.id,
                if (updated.all { it.returnedQuantity >= it.quantity }) SaleStatus.DEVUELTA
                else SaleStatus.PARCIALMENTE_DEVUELTA
            )
        }
        // Se anulan dos ventas para demostrar el tratamiento de ANULADA en informes.
        candidates.takeLast(2).forEach { sale ->
            database.saleDao().updateStatus(sale.id, SaleStatus.ANULADA)
        }
        AppLogger.i(TAG, "Devoluciones sembradas en ${storeIds.size} sucursales")
    }

    private data class SeedProduct(
        val name: String,
        val sku: String,
        val barcode: String,
        val category: String,
        val costCents: Long,
        val priceCents: Long,
        val taxRate: Double,
        val unit: ProductUnit,
        val isService: Boolean,
        val supplier: String,
    )

    private data class SeedUser(
        val username: String,
        val password: String,
        val fullName: String,
        val role: Role,
        val storeId: Long?,
    )

    companion object {
        const val TAG = "DatabaseSeeder"
        const val SEED = 42L
        const val DAYS_OF_HISTORY = 90
        const val MONTHS_OF_EXPENSES = 3
        const val TRANSFER_COUNT = 12
        const val RETURN_COUNT = 8
        const val RETURN_CANDIDATES = 40
        const val MAX_STOCK_MULTIPLIER = 6
        const val SATURDAY = 6

        // Credenciales de demostracion (documentadas en README y docs/).
        const val DEMO_PASSWORD = "Gis#2026"
        const val DEMO_ADMIN = "admin"
        const val DEMO_MANAGER = "gerente"
        const val DEMO_SUPERVISOR = "supervisor"
        const val DEMO_CASHIER = "cajero"

        private const val MILLIS_PER_DAY = 86_400_000L
        private const val MILLIS_PER_HOUR = 3_600_000L
    }
}
