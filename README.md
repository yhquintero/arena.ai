# Gestión Integral Supermercados

Aplicación **Android 100 % sin conexión** para administrar una cadena mixta de supermercados con
varias sucursales: punto de venta, inventario por tienda, gastos operacionales, clientes, usuarios
con roles e **informes económicos exportables a PDF, Excel y CSV**.

> Todos los datos se guardan cifrados en el dispositivo. La aplicación **no pide permiso de
> internet**: es imposible que la información del negocio salga del teléfono o la tableta.

---

## ¿Qué incluye?

| Módulo | Qué permite hacer |
| --- | --- |
| **Autenticación** | Usuario y contraseña, bloqueo por intentos fallidos, desbloqueo biométrico opcional y cierre de sesión por inactividad. |
| **Panel de control** | Ventas de hoy, utilidad del mes, ticket promedio, tendencia de 14 días, desempeño por sucursal, productos destacados y alertas de stock. |
| **Punto de venta (POS)** | Búsqueda por nombre/SKU/código de barras, descuentos por línea y globales, impuestos, formas de pago, efectivo recibido y vuelto, clientes y notas. |
| **Ventas** | Historial con filtros por sucursal, estado, periodo y texto; detalle de la venta; devoluciones con nota de crédito y reingreso a inventario; anulación. |
| **Inventario** | Existencias por sucursal, valor del inventario al costo y a precio de venta, alertas de stock bajo/agotado, ajustes con mínimos y máximos. |
| **Movimientos** | Entradas, salidas, ajustes, devoluciones y **traslados entre sucursales** con costo y motivo. |
| **Catálogo** | Productos con SKU, código de barras, categoría, costo, precio, impuesto, unidad, proveedor y margen calculado; alta de categorías en línea; servicios sin stock. |
| **Sucursales** | Datos de contacto, horario por días, horario extendido y **permisos operativos** (vender, devolver, ajustar, trasladar, gastos, informes, descuento máximo). |
| **Gastos** | Gastos por categoría, de sucursal o corporativos, con impuesto, forma de pago, referencia y **justificante adjunto**. |
| **Clientes** | Ficha, documento, límite de crédito, saldo, puntos de fidelidad e historial de compras. |
| **Informes** | 10 tipos de informe × 7 periodicidades (diario, semanal, mensual, trimestral, semestral, anual y rango personalizado), por sucursal o toda la empresa, con vista previa en pantalla y exportación a **PDF profesional, Excel multi-hoja y CSV**. Historial con responsable y fecha. |
| **Copias de seguridad** | Copia local cifrada `.gisbak` (automática programable), restauración con reinicio, exportación/importación a cualquier carpeta. |
| **Configuración** | Datos del negocio, moneda, impuesto y umbral de stock por defecto, prefijos de ticket/nota de crédito, tema claro/oscuro/sistema, colores dinámicos, cambio de contraseña y registro interno de diagnóstico. |

Informes disponibles: resumen ejecutivo, balance general, flujo de caja, ventas por sucursal,
productos más vendidos, rentabilidad por categoría, análisis de gastos, comparativa de periodos,
proyecciones simples e inventario valorado.

## Características técnicas

- **Kotlin** + **Jetpack Compose** con **Material Design 3** (interfaz adaptable: barra inferior en
  teléfono, riel lateral en tableta).
- **MVVM** con `StateFlow` + `collectAsStateWithLifecycle`, **Hilt** para inyección de dependencias.
- **Room** con convertidores, DAO reactivos y **migraciones versionadas**.
- Dinero siempre en **centavos (Long)**: sin errores de punto flotante; precios sin impuesto y el
  impuesto se suma al cobrar; descuentos globales repartidos por resto mayor (no se pierde ni un centavo).
- **Seguridad**: contraseñas con PBKDF2-HMAC-SHA256 + sal, sesión y datos sensibles cifrados con
  AES/GCM y claves del Android Keystore, bloqueo tras intentos fallidos, caducidad de sesión.
- **Informes sin dependencias externas**: generadores propios de PDF, XLSX y CSV, y gráficas dibujadas
  con Canvas (la interfaz y el PDF comparten la misma identidad visual).
- **Cadenas externalizadas** en español (por defecto) e inglés.
- **Registro interno** de eventos y errores, exportable para diagnóstico.
- **Datos de ejemplo** sembrados en el primer arranque para probar todo sin cargar información real.
- **Pruebas unitarias** de la lógica crítica (dinero, cálculo de ventas, periodos, validaciones).

## Credenciales de demostración

En el primer arranque puede crear su propio administrador **o** cargar los datos de ejemplo, que
crean estos usuarios (contraseña `Gis#2026` para todos):

| Usuario | Rol |
| --- | --- |
| `admin` | Administrador (acceso total) |
| `gerente` | Gerente |
| `supervisor` | Supervisor |
| `cajero` | Cajero |

> Cambie esas contraseñas antes de usar la aplicación con datos reales
> (Configuración → Cambiar mi contraseña).

## Cómo obtener la aplicación (APK)

La forma más sencilla, **sin instalar nada**:

1. Suba este proyecto a un repositorio de GitHub.
2. Abra la pestaña **Actions** → flujo **Android CI** → **Run workflow**.
3. Cuando termine (unos 10-15 minutos), entre en la ejecución y descargue los artefactos:
   - `gis-supermercados-release-apk` → APK para instalar en cualquier teléfono (Android 8.0 o superior).
   - `gis-supermercados-release-aab` → archivo para publicar en Google Play.
   - `gis-supermercados-debug-apk` → APK de pruebas.

Guía completa (incluida la firma con su propia clave y la publicación en Play Store):
**[docs/COMPILACION.md](docs/COMPILACION.md)**.

Si prefiere compilar en su computadora con Android Studio: abra la carpeta del proyecto, espere a que
Gradle termine y pulse **Build → Build Bundle(s)/APK(s) → Build APK(s)**.

## Documentación

- [docs/COMPILACION.md](docs/COMPILACION.md) — compilar, firmar, publicar y resolver problemas (paso a paso).
- [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) — arquitectura, capas, base de datos, seguridad e informes.
- [docs/MANUAL_USUARIO.md](docs/MANUAL_USUARIO.md) — manual de uso para el personal (sin tecnicismos).

## Requisitos

- Android **8.0 (API 26)** o superior; optimizada para teléfonos y tabletas.
- Para compilar: JDK 17 y Android SDK 35 (o simplemente GitHub Actions).

## Licencia

Uso interno de la organización. El código se entrega tal cual para su administración y evolución.
