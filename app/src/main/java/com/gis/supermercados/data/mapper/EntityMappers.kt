package com.gis.supermercados.data.mapper

import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.data.local.dao.UserWithStoreRow
import com.gis.supermercados.data.local.entity.AppSettingEntity
import com.gis.supermercados.data.local.entity.AuditLogEntity
import com.gis.supermercados.data.local.entity.CategoryEntity
import com.gis.supermercados.data.local.entity.CreditNoteEntity
import com.gis.supermercados.data.local.entity.CreditNoteItemEntity
import com.gis.supermercados.data.local.entity.CustomerEntity
import com.gis.supermercados.data.local.entity.CustomerWithStatsRow
import com.gis.supermercados.data.local.entity.ExpenseDetailRow
import com.gis.supermercados.data.local.entity.ExpenseEntity
import com.gis.supermercados.data.local.entity.InventoryMovementEntity
import com.gis.supermercados.data.local.entity.MovementDetailRow
import com.gis.supermercados.data.local.entity.ProductEntity
import com.gis.supermercados.data.local.entity.ProductWithCategoryRow
import com.gis.supermercados.data.local.entity.ReportLogEntity
import com.gis.supermercados.data.local.entity.SaleEntity
import com.gis.supermercados.data.local.entity.SaleItemEntity
import com.gis.supermercados.data.local.entity.SaleListRow
import com.gis.supermercados.data.local.entity.StockDetailRow
import com.gis.supermercados.data.local.entity.StockEntity
import com.gis.supermercados.data.local.entity.StoreEntity
import com.gis.supermercados.data.local.entity.UserEntity
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.model.AuditEntry
import com.gis.supermercados.domain.model.Category
import com.gis.supermercados.domain.model.CreditNote
import com.gis.supermercados.domain.model.CreditNoteItem
import com.gis.supermercados.domain.model.Customer
import com.gis.supermercados.domain.model.Expense
import com.gis.supermercados.domain.model.InventoryMovement
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.ReportRecord
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.model.SaleItem
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.model.StorePermissions
import com.gis.supermercados.domain.model.StoreSchedule
import com.gis.supermercados.domain.model.ThemeMode
import com.gis.supermercados.domain.model.User

/**
 * Mapeo Entidad (Room) <-> Modelo (dominio).
 *
 * La capa de datos nunca "escapa" hacia la UI: esto permite cambiar el esquema
 * de la base de datos sin tocar pantallas ni ViewModels.
 */

// ------------------------------- TIENDAS -------------------------------

fun StoreEntity.toDomain(): Store = Store(
    id = id,
    name = name,
    code = code,
    address = address,
    city = city,
    phone = phone,
    email = email,
    managerName = managerName,
    taxId = taxId,
    isActive = isActive,
    schedule = StoreSchedule(
        openDays = openDays.ifEmpty { StoreSchedule.DEFAULT_OPEN_DAYS },
        openingTime = openingTime,
        closingTime = closingTime,
        hasExtendedHours = hasExtendedHours
    ),
    permissions = StorePermissions(
        allowSales = allowSales,
        allowReturns = allowReturns,
        allowInventoryAdjust = allowInventoryAdjust,
        allowTransfers = allowTransfers,
        allowExpenses = allowExpenses,
        allowReports = allowReports,
        maxDiscountPercent = maxDiscountPercent
    ),
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Store.toEntity(): StoreEntity = StoreEntity(
    id = id,
    name = name.trim(),
    code = code.trim().uppercase(),
    address = address.trim(),
    city = city.trim(),
    phone = phone.trim(),
    email = email.trim(),
    managerName = managerName.trim(),
    taxId = taxId.trim(),
    isActive = isActive,
    openDays = schedule.openDays,
    openingTime = schedule.openingTime,
    closingTime = schedule.closingTime,
    hasExtendedHours = schedule.hasExtendedHours,
    allowSales = permissions.allowSales,
    allowReturns = permissions.allowReturns,
    allowInventoryAdjust = permissions.allowInventoryAdjust,
    allowTransfers = permissions.allowTransfers,
    allowExpenses = permissions.allowExpenses,
    allowReports = permissions.allowReports,
    maxDiscountPercent = permissions.maxDiscountPercent,
    notes = notes.trim(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --------------------------- CATEGORIAS / PRODUCTOS ---------------------------

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    description = description,
    color = color,
    targetMarginPercent = targetMarginPercent,
    isActive = isActive,
    sortOrder = sortOrder
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name.trim(),
    description = description.trim(),
    color = color,
    targetMarginPercent = targetMarginPercent,
    isActive = isActive,
    sortOrder = sortOrder
)

fun ProductEntity.toDomain(categoryName: String = ""): Product = Product(
    id = id,
    name = name,
    sku = sku,
    barcode = barcode,
    categoryId = categoryId,
    categoryName = categoryName,
    costCents = costCents,
    priceCents = priceCents,
    taxRate = taxRate,
    unit = unit,
    isService = isService,
    supplierName = supplierName,
    supplierPhone = supplierPhone,
    minStockGlobal = minStockGlobal,
    isActive = isActive,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProductWithCategoryRow.toDomain(): Product = product.toDomain(categoryName)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name.trim(),
    sku = sku.trim().uppercase(),
    barcode = barcode.trim(),
    categoryId = categoryId,
    costCents = costCents,
    priceCents = priceCents,
    taxRate = taxRate,
    unit = unit,
    isService = isService,
    supplierName = supplierName.trim(),
    supplierPhone = supplierPhone.trim(),
    minStockGlobal = minStockGlobal,
    isActive = isActive,
    description = description.trim(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ------------------------------- INVENTARIO -------------------------------

fun StockEntity.toDomain(): StockLevel = StockLevel(
    id = id,
    productId = productId,
    storeId = storeId,
    quantity = quantity,
    reserved = reserved,
    minStock = minStock,
    maxStock = maxStock,
    updatedAt = updatedAt
)

fun StockDetailRow.toDomain(): StockLevel = StockLevel(
    id = stockId,
    productId = productId,
    storeId = storeId,
    productName = productName,
    sku = sku,
    storeName = storeName,
    categoryName = categoryName,
    quantity = quantity,
    reserved = reserved,
    minStock = minStock,
    maxStock = maxStock,
    costCents = costCents,
    priceCents = priceCents,
    unit = unit,
    updatedAt = updatedAt
)

fun InventoryMovementEntity.toDomain(): InventoryMovement = InventoryMovement(
    id = id,
    productId = productId,
    storeId = storeId,
    destinationStoreId = destinationStoreId,
    type = type,
    quantity = quantity,
    unitCostCents = unitCostCents,
    reason = reason,
    reference = reference,
    userId = userId,
    createdAt = createdAt
)

fun MovementDetailRow.toDomain(): InventoryMovement = InventoryMovement(
    id = movementId,
    productId = productId,
    productName = productName,
    sku = sku,
    storeId = storeId,
    storeName = storeName,
    destinationStoreName = destinationStoreName.orEmpty(),
    type = type,
    quantity = quantity,
    unitCostCents = unitCostCents,
    reason = reason,
    reference = reference,
    userName = userName,
    createdAt = createdAt
)

// -------------------------------- CLIENTES --------------------------------

fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    fullName = fullName,
    documentType = documentType,
    documentId = documentId,
    phone = phone,
    email = email,
    address = address,
    city = city,
    loyaltyPoints = loyaltyPoints,
    creditLimitCents = creditLimitCents,
    balanceCents = balanceCents,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt
)

fun CustomerWithStatsRow.toDomain(): Customer = customer.toDomain().copy(
    purchaseCount = purchaseCount,
    totalPurchasesCents = totalPurchasesCents,
    lastPurchaseAt = lastPurchaseAt
)

fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    fullName = fullName.trim(),
    documentType = documentType,
    documentId = documentId.trim(),
    phone = phone.trim(),
    email = email.trim(),
    address = address.trim(),
    city = city.trim(),
    loyaltyPoints = loyaltyPoints,
    creditLimitCents = creditLimitCents,
    balanceCents = balanceCents,
    notes = notes.trim(),
    isActive = isActive,
    createdAt = createdAt
)

// --------------------------------- VENTAS ---------------------------------

fun SaleEntity.toDomain(
    storeName: String = "",
    customerName: String = "",
    userName: String = "",
): Sale = Sale(
    id = id,
    ticketNumber = ticketNumber,
    storeId = storeId,
    storeName = storeName,
    customerId = customerId,
    customerName = customerName,
    userId = userId,
    userName = userName,
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    taxCents = taxCents,
    totalCents = totalCents,
    costCents = costCents,
    paymentMethod = paymentMethod,
    paymentReference = paymentReference,
    cashReceivedCents = cashReceivedCents,
    changeCents = changeCents,
    status = status,
    itemCount = itemCount,
    notes = notes,
    createdAt = createdAt
)

fun SaleListRow.toDomain(): Sale = Sale(
    id = saleId,
    ticketNumber = ticketNumber,
    storeId = storeId,
    storeName = storeName,
    customerId = customerId,
    customerName = customerName.orEmpty(),
    userId = userId,
    userName = userName,
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    taxCents = taxCents,
    totalCents = totalCents,
    costCents = costCents,
    paymentMethod = paymentMethod,
    status = status,
    itemCount = itemCount,
    createdAt = createdAt
)

fun SaleItemEntity.toDomain(): SaleItem = SaleItem(
    id = id,
    saleId = saleId,
    productId = productId,
    productName = productName,
    sku = sku,
    quantity = quantity,
    returnedQuantity = returnedQuantity,
    unitPriceCents = unitPriceCents,
    unitCostCents = unitCostCents,
    discountPercent = discountPercent,
    taxRate = taxRate,
    lineTotalCents = lineTotalCents
)

fun CreditNoteEntity.toDomain(
    ticketNumber: String = "",
    storeName: String = "",
    customerName: String = "",
    userName: String = "",
    items: List<CreditNoteItem> = emptyList(),
): CreditNote = CreditNote(
    id = id,
    creditNoteNumber = creditNoteNumber,
    saleId = saleId,
    ticketNumber = ticketNumber,
    storeId = storeId,
    storeName = storeName,
    customerId = customerId,
    customerName = customerName,
    userId = userId,
    userName = userName,
    reason = reason,
    subtotalCents = subtotalCents,
    taxCents = taxCents,
    totalCents = totalCents,
    restock = restock,
    itemCount = itemCount,
    createdAt = createdAt,
    items = items
)

fun CreditNoteItemEntity.toDomain(): CreditNoteItem = CreditNoteItem(
    id = id,
    creditNoteId = creditNoteId,
    saleItemId = saleItemId,
    productId = productId,
    productName = productName,
    sku = sku,
    quantity = quantity,
    unitPriceCents = unitPriceCents,
    taxRate = taxRate,
    lineTotalCents = lineTotalCents
)

// --------------------------------- GASTOS ---------------------------------

fun ExpenseEntity.toDomain(storeName: String = "", userName: String = ""): Expense = Expense(
    id = id,
    category = category,
    storeId = storeId,
    storeName = storeName,
    concept = concept,
    provider = provider,
    amountCents = amountCents,
    taxCents = taxCents,
    totalCents = totalCents,
    paymentMethod = paymentMethod,
    reference = reference,
    receiptPath = receiptPath,
    hasReceipt = receiptPath.isNotBlank(),
    userId = userId,
    userName = userName,
    expenseDate = expenseDate,
    createdAt = createdAt
)

fun ExpenseDetailRow.toDomain(): Expense = expense.toDomain(
    storeName = storeName.orEmpty(),
    userName = userName
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    category = category,
    storeId = storeId,
    concept = concept.trim(),
    provider = provider.trim(),
    amountCents = amountCents,
    taxCents = taxCents,
    totalCents = totalCents,
    paymentMethod = paymentMethod,
    reference = reference.trim(),
    receiptPath = receiptPath,
    userId = userId,
    expenseDate = expenseDate,
    createdAt = createdAt
)

// -------------------------------- USUARIOS --------------------------------

fun UserEntity.toDomain(storeName: String = ""): User = User(
    id = id,
    username = username,
    fullName = fullName,
    role = role,
    storeId = storeId,
    storeName = storeName,
    biometricEnabled = biometricEnabled,
    isActive = isActive,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt
)

fun UserWithStoreRow.toDomain(): User = user.toDomain(storeName)

// -------------------------------- AJUSTES ---------------------------------

/** Convierte la tabla clave/valor en el objeto de configuracion tipado. */
fun List<AppSettingEntity>.toSettings(): AppSettings {
    val map = associate { it.key to it.value }
    val defaults = AppSettings.Default
    val keys = AppConstants.Keys
    return AppSettings(
        businessName = map[keys.BUSINESS_NAME] ?: defaults.businessName,
        businessLegalName = map[keys.BUSINESS_LEGAL_NAME] ?: defaults.businessLegalName,
        businessTaxId = map[keys.BUSINESS_TAX_ID] ?: defaults.businessTaxId,
        businessAddress = map[keys.BUSINESS_ADDRESS] ?: defaults.businessAddress,
        businessPhone = map[keys.BUSINESS_PHONE] ?: defaults.businessPhone,
        currencySymbol = map[keys.CURRENCY_SYMBOL] ?: defaults.currencySymbol,
        defaultTaxRate = map[keys.DEFAULT_TAX_RATE]?.toDoubleOrNull() ?: defaults.defaultTaxRate,
        lowStockThreshold = map[keys.LOW_STOCK_THRESHOLD]?.toIntOrNull() ?: defaults.lowStockThreshold,
        autoBackupEnabled = map[keys.AUTO_BACKUP_ENABLED]?.toBooleanStrictOrNull() ?: defaults.autoBackupEnabled,
        autoBackupHours = map[keys.AUTO_BACKUP_HOURS]?.toIntOrNull() ?: defaults.autoBackupHours,
        backupEncrypted = map[keys.BACKUP_ENCRYPTED]?.toBooleanStrictOrNull() ?: defaults.backupEncrypted,
        themeMode = map[keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: defaults.themeMode,
        dynamicColors = map[keys.DYNAMIC_COLORS]?.toBooleanStrictOrNull() ?: defaults.dynamicColors,
        requireAuthOnStart = map[keys.REQUIRE_AUTH_ON_START]?.toBooleanStrictOrNull()
            ?: defaults.requireAuthOnStart,
        biometricEnabled = map[keys.BIOMETRIC_ENABLED]?.toBooleanStrictOrNull() ?: defaults.biometricEnabled,
        ticketPrefix = map[keys.TICKET_PREFIX] ?: defaults.ticketPrefix,
        creditNotePrefix = map[keys.CREDIT_NOTE_PREFIX] ?: defaults.creditNotePrefix,
    )
}

/** Descompone la configuracion en filas clave/valor listas para persistir. */
fun AppSettings.toEntities(now: Long = System.currentTimeMillis()): List<AppSettingEntity> {
    val keys = AppConstants.Keys
    fun setting(key: String, value: String) = AppSettingEntity(key, value, now)
    return listOf(
        setting(keys.BUSINESS_NAME, businessName),
        setting(keys.BUSINESS_LEGAL_NAME, businessLegalName),
        setting(keys.BUSINESS_TAX_ID, businessTaxId),
        setting(keys.BUSINESS_ADDRESS, businessAddress),
        setting(keys.BUSINESS_PHONE, businessPhone),
        setting(keys.CURRENCY_SYMBOL, currencySymbol),
        setting(keys.DEFAULT_TAX_RATE, defaultTaxRate.toString()),
        setting(keys.LOW_STOCK_THRESHOLD, lowStockThreshold.toString()),
        setting(keys.AUTO_BACKUP_ENABLED, autoBackupEnabled.toString()),
        setting(keys.AUTO_BACKUP_HOURS, autoBackupHours.toString()),
        setting(keys.BACKUP_ENCRYPTED, backupEncrypted.toString()),
        setting(keys.THEME_MODE, themeMode.name),
        setting(keys.DYNAMIC_COLORS, dynamicColors.toString()),
        setting(keys.REQUIRE_AUTH_ON_START, requireAuthOnStart.toString()),
        setting(keys.BIOMETRIC_ENABLED, biometricEnabled.toString()),
        setting(keys.TICKET_PREFIX, ticketPrefix),
        setting(keys.CREDIT_NOTE_PREFIX, creditNotePrefix),
    )
}

// ---------------------------------- LOGS ----------------------------------

fun AuditLogEntity.toDomain(): AuditEntry = AuditEntry(
    id = id,
    level = level,
    tag = tag,
    message = message,
    screen = screen,
    createdAt = createdAt
)

fun AuditEntry.toEntity(): AuditLogEntity = AuditLogEntity(
    id = id,
    level = level,
    tag = tag,
    message = message,
    screen = screen,
    createdAt = createdAt
)

fun ReportLogEntity.toDomain(): ReportRecord = ReportRecord(
    id = id,
    reportType = reportType,
    periodType = periodType,
    startMillis = startMillis,
    endMillis = endMillis,
    format = format,
    fileName = fileName,
    filePath = filePath,
    sizeBytes = sizeBytes,
    storeId = storeId,
    createdAt = createdAt
)
