# Arquitectura técnica

**Aplicación:** Gestión Integral Supermercados · **paquete:** `com.gis.supermercados`
**Módulo:** único (`:app`) · **minSdk 26 · target/compile 35 · JVM 17**
**Stack:** Kotlin 2.0 · Jetpack Compose (BOM 2024.10.01) · Material 3 · Room 2.6 · Hilt 2.52 ·
Coroutines/Flow · Navigation Compose · WorkManager · Biometric · SplashScreen.

---

## 1. Principios de diseño

1. **100 % sin conexión.** No se declara `android.permission.INTERNET` en el manifiesto: es una
   garantía estructural de que ningún dato sale del dispositivo.
2. **Dinero exacto.** Todos los importes son **centavos en `Long`**. Nada de `Double` para dinero:
   solo para tasas/porcentajes. Reglas: precios **sin impuesto**, el impuesto se **suma** al cobrar,
   descuentos primero por línea y luego globales.
3. **Una sola fuente de verdad.** Las pantallas observan `Flow` de Room; nunca guardan copias locales
   de los datos. Escribir → la base emite → la UI se refresca.
4. **Sin dependencias de terceros para informes.** PDF, XLSX y CSV se generan con código propio y las
   gráficas se dibujan con `Canvas`. Menos superficie de fallo y control total del diseño impreso.
5. **Todo texto externalizado.** Ninguna cadena visible en el código: `res/values/strings.xml`
   (español, por defecto) y `res/values-en/strings.xml` (inglés).
6. **Errores tipados y localizables.** La capa de datos devuelve `AppResult<T>` con `UiText`
   (recurso o texto dinámico), nunca excepciones crudas hacia la UI.

## 2. Capas

```
┌──────────────────────────── UI (Jetpack Compose) ────────────────────────────┐
│ ui/<feature>/  Screen (estado + eventos)   ViewModel (StateFlow, validación)  │
│ core/designsystem/  Theme, Components, Charts   ui/common/  UiState, labels   │
├──────────────────────────── Dominio (puro Kotlin) ───────────────────────────┤
│ domain/model/       entidades de negocio inmutables                          │
│ domain/repository/  11 interfaces (contratos)                                │
│ domain/calculator/  SaleCalculator (cálculo exacto de la venta)              │
├──────────────────────────── Datos (Room / archivo) ──────────────────────────┤
│ data/local/  GisDatabase (15 tablas), 14 DAO, Converters, migrations, seed   │
│ data/mapper/ EntityMappers (entity ⇄ domain)                                 │
│ data/repository/ 11 implementaciones (@Singleton, transacciones, AppResult)  │
├──────────────────────────── Núcleo transversal ──────────────────────────────┤
│ core/common/    Money, AppDateTime, AppResult, UiText, Validators, Formats   │
│ core/security/  PasswordHasher, CryptoManager, SessionStore, BiometricHelper │
│ core/logging/   AppLogger + PersistentLogSink (auditoría interna)            │
│ core/backup/    BackupManager, BackupScheduler, AutoBackupWorker             │
│ core/reporting/ ReportPeriods, ReportBuilder(+Extra), Exporter, pdf/xlsx/csv │
│ core/navigation/ Routes, GisNavHost (guardas por rol)                        │
└──────────────────────────────────────────────────────────────────────────────┘
              di/ AppModule · RepositoryModule · Qualifiers (Hilt)
```

Regla de dependencia: **UI → dominio ← datos**. El dominio no conoce Android ni Room; la UI no
conoce DAO ni SQL.

## 3. Modelo de datos (Room, versión 1)

15 tablas, claves foráneas con `onDelete` explícito e índices en las columnas de filtrado
(`created_at`, `store_id`, `product_id`, `status`, `category`).

| Tabla | Contenido relevante |
| --- | --- |
| `stores` | Datos de contacto, horario (`open_days` como CSV de ISO 1-7, apertura/cierre, horario extendido) y **permisos operativos** + `max_discount_percent`. |
| `categories` | Nombre único, color, margen objetivo, orden, `is_active`. |
| `products` | SKU único, código de barras opcional, costo/precio en centavos, `tax_rate` (fracción), unidad, proveedor, `is_service`, **borrado lógico** (`is_active`). |
| `stocks` | Existencias **por producto y sucursal** (`UNIQUE(product_id, store_id)`), cantidad, reservado, `min_stock`, `max_stock`. |
| `inventory_movements` | Tipo (venta, devolución, entrada, salida, ajuste ±, traslado), cantidad, costo unitario, sucursal origen/destino, motivo, referencia, usuario. |
| `customers` | Documento único opcional, puntos de fidelidad, límite de crédito, saldo. |
| `sales` | Ticket único, sucursal, cajero, cliente, forma de pago, subtotal/descuento/impuesto/total/costo, efectivo recibido/vuelto, estado (`COMPLETADA`, `DEVUELTA`, `PARCIALMENTE_DEVUELTA`, `ANULADA`), `created_at`. |
| `sale_items` | **Foto** del producto en el momento de la venta (nombre, SKU, precio, costo, tasa), cantidad, descuento, impuesto, devuelto. |
| `credit_notes` / `credit_note_items` | Notas de crédito (devoluciones) con su propia foto de importes y `restock`. |
| `expenses` | Categoría, sucursal (`NULL` = corporativo), concepto, proveedor, importe/impuesto/total, forma de pago, referencia, ruta del justificante, usuario, fecha. |
| `users` | Usuario único, hash + sal de la contraseña, rol, sucursal asignada, intentos fallidos, bloqueo hasta, biometría habilitada, `is_active`. |
| `app_settings` | Clave/valor cifrado para configuración (negocio, moneda, impuesto, tema, respaldos). |
| `audit_logs` | Registro interno (nivel, etiqueta, mensaje, pantalla, instante). |
| `report_logs` | Historial de informes exportados (tipo, periodo, formato, archivo, tamaño, usuario). |

**Migraciones:** `data/local/migrations/Migrations.kt` contiene el procedimiento documentado y una
plantilla `TEMPLATE_MIGRATION_1_2`. `exportSchema = true` vuelca `app/schemas/<versión>.json`.
Nunca se usa `fallbackToDestructiveMigration()` en producción.

**Semilla:** `data/local/seed/DatabaseSeeder.kt` crea, en el primer arranque y si el usuario lo
acepta, 3 sucursales, categorías, ~40 productos con stock, 90 días de ventas coherentes
(con devoluciones y anulaciones), gastos por categoría y los 4 usuarios de demostración.

## 4. Cálculo de una venta (`SaleCalculator`)

```
gross      = Σ (precioUnitario × cantidad)
lineDisc   = Σ percentOf(grossLinea, descuentoLinea)
base       = gross − lineDisc
globalDisc = globalCents si > 0, si no percentOf(base, globalPercent)   // acotado a base
reparto    = distribute(globalDisc, pesos = neto de cada línea)          // resto mayor, exacto
netoLinea  = netoLinea − reparto
impuesto_i = Money.tax(netoLinea_i, tasa_i)      // redondeo HALF_UP por línea
total      = Σ (netoLinea_i + impuesto_i)
costo      = Σ (costoUnitario × cantidad)
utilidad   = Σ netoLinea_i − costo
puntos     = floor(total / 100)                  // 1 punto por unidad monetaria
```

`distribute` garantiza que la suma de los repartos sea **exactamente** el descuento global
(ajusta el resto en las líneas de mayor peso). `CartTotals.from` replica la misma matemática para la
vista previa del carrito, de modo que lo mostrado y lo guardado cuadran al centavo.

Devoluciones: crean `CreditNote` + `CreditNoteItem` con foto de importes, actualizan
`sale_items.returned_quantity`, recalculan el estado de la venta y —si `restock`— registran un
movimiento `DEVOLUCION_CLIENTE` que reintegra el stock.

## 5. Informes económicos

Pipeline en cuatro pasos, todo en `core/reporting/`:

1. **Periodo** — `ReportPeriods.rangeFor(type, ref, customStart, customEnd)` devuelve
   `DateRangeModel(start, end, label)`; `previous()` calcula el periodo anterior por longitud.
   `daysInRange()` genera la serie natural de días (rellena huecos con ceros para las gráficas).
2. **Datos** — `ReportQueriesDao` ejecuta consultas agregadas (agrupación por día con
   `((created_at + tzOffset)/86400000)*86400000 − tzOffset`, por sucursal, por categoría, por forma
   de pago, top productos, gastos, stock valorado, clientes). `ReportRepositoryImpl` los combina en
   `ReportData` (totales, series, comparativas, valor de inventario, alertas).
   Regla contable: las ventas **anuladas nunca** son ingreso; las devoluciones se restan
   (`neto = bruto − devoluciones`); los gastos se suman por fecha de gasto.
3. **Documento** — `ReportBuilder` + `ReportBuilderExtra` convierten `ReportData` en un
   `ReportDocument` con bloques tipados: `SectionTitle`, `Paragraph`, `KpiGrid`, `Table`
   (columnas con peso y alineación, filas de `Cell` tipadas —texto, dinero, conteo, decimal, fecha—,
   fila de totales y nota) y `Chart` (`BARS`, `HORIZONTAL_BARS`, `LINE`, `DONUT`). Las celdas
   tipadas permiten que Excel reciba **números reales** y no texto.
4. **Render y exportación** — `ReportExporter` elige el renderizador y escribe en
   `getExternalFilesDir(null)/Reportes` (privado de la app, declarado en `res/xml/file_paths.xml`):
   - `pdf/PdfWriter` + `PdfReportRenderer` + `PdfReportLayout`: PDF propio (objetos, fuentes,
     tablas con salto de página, cabecera/pie, gráficas dibujadas con el mismo `Brand` que la UI).
   - `xlsx/XlsxWriter` + `XlsxReportRenderer` + `XlsStyle`: **libro multi-hoja** (una hoja por
     tabla/KPI, con estilos, anchos y totales).
   - `csv/CsvReportRenderer`: CSV por tabla (compatible con Excel/Sheets).

Nombre de archivo: `GIS_<titulo>_<sucursal>_<yyyymmdd>-<yyyymmdd>.<ext>`.
Compartir: `FileSharing` genera el `Uri` con `FileProvider` (`${packageName}.fileprovider`) y permiso
de lectura temporal. Cada exportación registra un `ReportRecord` (auditoría: quién, qué, cuándo).

La previsualización usa `ReportPreviewStore` (singleton en memoria) para pasar el documento entre el
centro de informes y la pantalla de vista previa sin serializarlo por la navegación.

## 6. Seguridad

| Pieza | Implementación |
| --- | --- |
| Contraseñas | `PasswordHasher`: PBKDF2WithHmacSHA256, sal aleatoria por usuario, iteraciones altas, comparación en tiempo constante. Nunca se guarda la contraseña. |
| Datos sensibles | `CryptoManager`: AES/GCM/NoPadding con clave residente en **Android Keystore**; se cifran la sesión y los valores de configuración marcados como sensibles. |
| Sesión | `SessionStore`: sesión activa cifrada, `rememberUser`/`lastUsername`, caducidad por inactividad (`SESSION_IDLE_TIMEOUT_MINUTES`) y `logout()` completo. |
| Bloqueo | Tras `MAX_FAILED_LOGINS` intentos, la cuenta se bloquea `LOCK_MINUTES` (persistido en la fila del usuario, sobrevive reinicios). |
| Biometría | `BiometricHelper` envuelve `androidx.biometric` (huella/rostro) y solo se ofrece si el usuario la activó y el dispositivo la soporta. |
| Copias | `.gisbak` = ZIP con `manifest.json` + `database.db`, cifrado con AES/GCM; contraseña derivada opcional; la restauración exige reiniciar la app. |
| Permisos por rol | `Role` expone capacidades (`canManageStores`, `canViewReports`, `canRegisterExpenses`…) que se comprueban **en el dominio, en la UI y en `GisNavHost`** (defensa en profundidad). Los permisos por sucursal (`allowSales`, `allowTransfers`, `maxDiscountPercent`…) se validan al operar sobre esa tienda. |

## 7. Inyección de dependencias (Hilt)

- `di/AppModule`: `GisDatabase` (Room + `Migrations.ALL`), DAO, `@IoDispatcher`, `@DefaultDispatcher`,
  `@ApplicationScope` (scope sin calificar para el `PersistentLogSink`), `CryptoManager`,
  `PasswordHasher`, `SessionStore`, `BackupManager`, `BackupScheduler`, `ReportExporter`,
  `ReportPreviewStore`, `DatabaseSeeder`.
- `di/RepositoryModule`: `@Binds` de las 11 interfaces de repositorio con sus implementaciones.
- `GisApplication`: `@HiltAndroidApp`, implementa `Configuration.Provider` con `HiltWorkerFactory`
  para `AutoBackupWorker` (`@HiltWorker`) y programa la copia automática según la configuración.

## 8. Navegación y UI adaptable

- `Routes` centraliza todas las rutas (constantes + constructores con parámetros).
- `GisNavHost` arma el grafo, pasa los argumentos (`navArgument` con `defaultValue` para los
  opcionales) y aplica **guardas por rol**: si el rol no tiene permiso se muestra `NoPermission`
  en lugar de la pantalla.
- `GisApp` decide: cargando → `LoadingState`; sin sesión → `LoginScreen`; con sesión →
  `MainScaffold` con **`NavigationBar`** (ancho compacto = teléfono) o **`NavigationRail`**
  (medio/expandido = tableta), usando `WindowSizeClass`. El conjunto de destinos depende del rol.
- `core/designsystem`: `GisTheme` (claro/oscuro/sistema + colores dinámicos en Android 12+),
  componentes reutilizables (`GisTopBar`, `SectionCard`, `StatCard`, `AmountText`, `AmountField`,
  `StatusChip`, `SearchField`, `EmptyState`, `LoadingState`, `ErrorState`) y gráficas
  (`LineChart`, `DonutChart`, `ShareBar`) dibujadas con `Canvas`.
- Convención de pantallas: `MutableStateFlow<XUiState>` en el ViewModel + `collectAsStateWithLifecycle()`
  en la Composable; errores y confirmaciones mediante `AlertDialog`; edición de entidades en
  `ModalBottomSheet` u hojas de diálogo; selección de archivos con SAF
  (`ActivityResultContracts.OpenDocument` / `CreateDocument`).

## 9. Registro interno y diagnóstico

`AppLogger` (niveles DEBUG/INFO/WARN/ERROR) escribe en `audit_logs` a través de `PersistentLogSink`
en el scope de aplicación (no se pierden mensajes al destruir una pantalla) y espeja en `Logcat`.
La pantalla **Registro interno** permite filtrar por nivel, buscar, exportar a texto y compartir.
Todas las operaciones de repositorio registran su resultado (`runCatchingApp`) con etiqueta propia.

## 10. Pruebas

`app/src/test/java/com/gis/supermercados/`:

- `core/common/MoneyTest` — parseo de formatos, formateo `$1.234,56`, impuestos, porcentajes, compacto.
- `core/common/AppDateTimeTest` — límites de día/semana/mes/año, `daysBetween` inclusivo, formatos.
- `core/common/ValidatorsTest` — usuario, contraseña, SKU, código de barras, importes, porcentajes,
  horas, rangos de fecha y `firstError`.
- `domain/calculator/SaleCalculatorTest` — impuestos por línea, descuentos globales repartidos sin
  perder centavos, priorización de descuento, tope al neto, vuelto, puntos y servicios.
- `core/reporting/ReportPeriodsTest` — los 7 periodos, periodo personalizado, periodo anterior y
  serie de días.

Ejecutar: `./gradlew testDebugUnitTest` (o el flujo **Android CI**, que además las publica como
artefacto HTML).

## 11. Convenciones de código

- Nombres en español para el dominio (el negocio es hispanohablante) y en inglés para la
  infraestructura técnica (`Repository`, `Dao`, `ViewModel`).
- Un archivo por clase pública; KDoc en cada clase/función no trivial explicando **por qué**, no qué.
- Inmutabilidad: `data class` + `copy`; colecciones de solo lectura en las firmas públicas.
- `withContext(io)` en toda operación de base/archivos; `Flow` para todo lo observable;
  `database.withTransaction { }` cuando una operación toca varias tablas (venta, devolución, traslado).
- Errores de negocio: `IllegalArgumentException` con mensaje localizado dentro de
  `runCatchingApp(TAG) { … }`, que lo convierte en `AppResult.Failure(UiText)`.

## 12. Cómo extender la aplicación

**Añadir un tipo de informe**
1. Agregue el valor en `core/reporting/model/ReportModel.kt → ReportType`.
2. Construya los bloques en `ReportBuilderExtra` (caso del `when (request.type)`).
3. Añada la etiqueta en `ui/common/EnumLabels.kt` y las cadenas `report_type_*` / `report_desc_*`
   en `res/values/strings.xml` y `res/values-en/strings.xml`.

**Añadir una tabla o columna**
1. Modifique la entidad y suba `GisDatabase.VERSION`.
2. Cree la `Migration` correspondiente en `Migrations.kt` y regístrela en `ALL`.
3. Añada el mapper en `data/mapper/EntityMappers.kt` y exponga la operación en el DAO, el
   repositorio (interfaz + implementación) y, si hace falta, el ViewModel.
4. Haga commit del nuevo `app/schemas/<versión>.json`.

**Añadir un idioma**
Copie `res/values-en/strings.xml` a `res/values-<código>/strings.xml`, traduzca y añada el código en
`res/xml/locales_config.xml`. No se requiere tocar el código.
