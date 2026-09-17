# Manual de uso

Guía para el personal, escrita sin tecnicismos. La aplicación funciona **sin internet**: todo se
guarda en el teléfono o la tableta.

---

## 1. Primer arranque

1. Abra **Gestión Integral Supermercados** (icono verde con una cesta de compras).
2. La primera vez verá la pantalla de bienvenida con dos opciones:
   - **Cargar datos de ejemplo**: crea 3 sucursales, productos, ventas de los últimos 90 días,
     gastos y 4 usuarios de prueba. Es lo más cómodo para conocer la aplicación.
   - **Crear usuario administrador**: empieza desde cero, con la base vacía.
3. Si cargó los datos de ejemplo podrá entrar con cualquiera de estos usuarios
   (contraseña `Gis#2026`):

   | Usuario | Rol | Qué puede hacer |
   | --- | --- | --- |
   | `admin` | Administrador | Todo: sucursales, usuarios, catálogos, informes, respaldos. |
   | `gerente` | Gerente | Todo lo operativo e informes; no administra usuarios ni sucursales. |
   | `supervisor` | Supervisor | Vender, devoluciones, ajustes de inventario e informes de su alcance. |
   | `cajero` | Cajero | Vender y consultar su caja. |

4. **Cambie las contraseñas** antes de usar datos reales: menú **Más → Cambiar mi contraseña**, y
   para los demás usuarios **Más → Usuarios y roles → icono de llave**.

## 2. Pantalla de inicio (Panel de control)

Muestra de un vistazo: ventas de hoy, utilidad de hoy, ventas y utilidad del mes, gastos del mes,
ticket promedio, la tendencia de los últimos 14 días, el desempeño de cada sucursal, los productos
más vendidos y las **alertas** (productos por debajo del mínimo o agotados).

Cada tarjeta tiene un acceso directo: **Cobrar**, **Ventas**, **Inventario**, **Gastos**, **Informes**.

## 3. Cobrar una venta (Punto de venta)

1. Elija la **sucursal** arriba (solo aparecen las que tienen permiso para vender).
2. Busque el producto:
   - escriba parte del nombre, el SKU o el código de barras, o
   - escanee/teclee el código de barras en el campo **Código de barras** y pulse **Agregar**.
3. Pulse el producto para añadirlo al carrito. Con **+** y **−** cambia la cantidad.
4. Descuentos (si la sucursal lo permite):
   - **por línea**: en el artículo del carrito;
   - **global**: al final del carrito, en importe o porcentaje.
5. Opcional: seleccione **cliente** (necesario para vender a crédito), **forma de pago**,
   **referencia** del pago y **notas**.
6. Pulse **Cobrar**. Si el pago es en efectivo, escriba el **efectivo recibido**: la aplicación
   calcula el **vuelto**.
7. Al terminar aparece el resumen con el **número de ticket**, artículos, total y cambio. Desde ahí
   puede ir al historial o empezar una **nueva venta**.

> La aplicación impide vender más unidades de las que hay en existencia y avisa si el descuento
> supera el máximo permitido para la sucursal.

## 4. Historial de ventas, devoluciones y anulaciones

**Más → Ventas** (o el acceso directo del panel):

1. Filtre por **sucursal**, **estado** (todas, completadas, devueltas, parcialmente devueltas,
   anuladas), **periodo** o busque por ticket, cliente o cajero.
2. Pulse una venta para ver su **detalle**: artículos con precio, descuento, impuesto y total;
   cliente, cajero, forma de pago, utilidad y notas.
3. **Registrar devolución**:
   - pulse el botón de devolución, indique la **cantidad** (no puede superar lo vendido),
     el **motivo** y si la mercancía **vuelve al inventario**.
   - Se genera una **nota de crédito** con su número; la venta pasa a *parcialmente devuelta* o
     *devuelta*, y el stock se reintegra si lo indicó.
4. **Anular venta**: la venta deja de contar como ingreso (no se borra, queda marcada como anulada
   para mantener la trazabilidad).

## 5. Inventario

**Inventario** (barra inferior):

- Elija la **sucursal** en la parte superior; verá el **valor del inventario** al costo y a precio
  de venta, y las **alertas** (cuántos productos están bajo mínimo o agotados).
- Busque por nombre, SKU o código; active **Solo stock bajo** para repasar lo que hay que reponer.
- Cada fila muestra existencias disponibles, el valor y el estado (*Sin existencias*, *Stock bajo*,
  *Óptimo*), además del mínimo y máximo configurados.
- Pulse el icono de ajuste de una fila para **ajustar existencias**: escriba la diferencia
  (use números negativos para descontar) y, si lo desea, corrija el **mínimo** y el **máximo**.
  Todo ajuste queda registrado como movimiento con su motivo.
- El botón flotante abre **Movimientos**.

### Movimientos y traslados entre sucursales

- **Registrar movimiento**: elija producto, sucursal, tipo (entrada, salida, ajuste positivo,
  ajuste negativo, devolución de cliente, devolución a proveedor), cantidad, costo unitario, motivo
  y referencia.
- **Trasladar entre sucursales**: elija producto, **sucursal de origen**, **sucursal de destino** y
  cantidad. La mercancía sale de una y entra en la otra con el mismo costo; si el origen no tiene
  existencias suficientes, la aplicación lo impide.

## 6. Catálogo de productos

**Más → Catálogo** (o desde Inventario):

- Busque y filtre por categoría. Cada tarjeta muestra precio, costo, **margen** (verde si es
  saludable, ámbar si es bajo), si es un **servicio** y un interruptor para **activar/desactivar**.
- **Nuevo producto** (botón flotante): nombre, SKU, código de barras (opcional), categoría,
  descripción, proveedor y su teléfono, unidad (unidad, kilogramo, litro, caja, docena, paquete,
  servicio…), **costo**, **precio de venta**, **impuesto %** y el margen calculado en vivo.
  Al crearlo puede indicar las **existencias iniciales por sucursal**.
- **Nueva categoría**: dentro del mismo formulario, con nombre y margen objetivo.
- **Servicios** (por ejemplo "Domicilio" o "Corte de carne"): no descuentan existencias ni piden
  stock inicial.
- **Eliminar**: desactiva el producto (no borra el historial de ventas).

## 7. Sucursales (solo administrador)

**Más → Configuración → Sucursales** o desde el panel:

- **Datos**: nombre, código, identificación fiscal, dirección, ciudad, teléfono, correo y responsable.
- **Horario**: pulse los días abiertos (Lun…Dom) y escriba la hora de apertura y cierre
  (formato `08:00`, `21:00`). Puede marcar **horario extendido**.
- **Permisos operativos**: vender, procesar devoluciones, ajustar inventario, trasladar mercancía,
  registrar gastos, ver informes, y el **descuento máximo (%)** que puede aplicar el personal.
- **Estado**: el interruptor activa o desactiva la sucursal. Una sucursal desactivada no puede operar,
  pero conserva su historial. Solo se puede **eliminar** si no tiene registros asociados.

## 8. Gastos operacionales

**Más → Gastos**:

1. Filtre por **periodo** (hoy, semana, mes, trimestre, todo), **sucursal** o **categoría**,
   o busque por concepto/proveedor.
2. Arriba verá el **total del periodo**, cuánto es **corporativo** y cuánto de **sucursales**.
3. **Nuevo gasto**: categoría (nómina, alquiler, servicios básicos, mercancía, transporte,
   mantenimiento, publicidad, tecnología, seguros, impuestos, otros), **sucursal** o *Corporativo*,
   concepto, proveedor, importe, impuesto %, forma de pago y referencia. El total se calcula solo.
4. **Justificante**: después de guardar el gasto, pulse **Adjuntar** para elegir la foto o el archivo
   del comprobante desde el teléfono. Los gastos con justificante muestran una etiqueta verde.
5. Pulse un gasto para editarlo o el icono de papelera para eliminarlo.

## 9. Clientes

**Más → Clientes**:

- **Nuevo cliente**: nombre, tipo y número de documento, teléfono, correo, dirección, ciudad,
  **límite de crédito** y notas.
- En la lista verá los **puntos de fidelidad**, el **saldo** y el **crédito disponible**.
- Pulse un cliente para abrir su **ficha**: compras realizadas, última compra, puntos, saldo y
  acciones rápidas (abonar o cargar saldo, sumar puntos).
- Para vender a crédito, seleccione el cliente en el punto de venta y elija la forma de pago
  **Crédito de cliente**. La aplicación impide pasarse del límite definido.
- El interruptor de la tarjeta activa o desactiva el cliente (los inactivos no aparecen al cobrar).

## 10. Informes económicos

**Informes** (barra inferior) → pestaña **Nuevo informe**:

1. **Tipo de informe**:
   - *Resumen ejecutivo*: los indicadores clave del negocio.
   - *Balance general*: situación financiera al cierre.
   - *Flujo de caja*: entradas, salidas y saldo acumulado.
   - *Ventas por sucursal*: desempeño comparado.
   - *Productos más vendidos*: los que más ingresan y su aporte.
   - *Rentabilidad por categoría*: ventas, costo y margen por categoría.
   - *Análisis de gastos*: en qué se gasta y dónde.
   - *Comparativa de periodos*: este periodo contra el anterior equivalente.
   - *Proyecciones*: estimación de ventas y utilidad de los próximos días.
   - *Inventario valorado*: valor del stock al costo y a precio de venta, con alertas.
2. **Periodo**: diario, semanal, mensual, trimestral, semestral, anual o **personalizado**
   (elija *Desde* y *Hasta* en el calendario).
3. **Alcance**: una sucursal o **toda la empresa**.
4. **Formato**: **PDF** (documento profesional con cabecera, tablas y gráficas), **Excel**
   (libro con varias hojas, cifras reales para seguir calculando) o **CSV** (datos planos).
5. Pulse **Vista previa** para revisarlo en pantalla antes de exportar, o **Exportar** directamente.
6. Al exportar puede **Compartir** el archivo (WhatsApp, correo, Drive) o **Guardar como…** en la
   carpeta que elija.

En la pestaña **Historial** quedan registrados todos los informes exportados con fecha, tamaño y
quién los generó; desde ahí puede volver a compartirlos o guardarlos en otra carpeta.

> Las ventas anuladas nunca cuentan como ingreso y las devoluciones se descuentan, así que las cifras
> del informe coinciden con la contabilidad real del periodo.

## 11. Copias de seguridad (muy recomendable)

**Más → Copias de seguridad**:

- **Copia automática**: actívela y defina cada cuántas horas se hace (por defecto, 24). Se guardan
  las últimas copias y las más antiguas se eliminan solas.
- **Copia manual**: pulse **Crear copia ahora**. Opcionalmente defina una **frase de contraseña**:
  si la usa, será indispensable para restaurar.
- **Restaurar**: elija la copia y pulse **Restaurar**. Se reemplazan **todos** los datos actuales y
  la aplicación se reinicia sola.
- **Exportar / Importar**: saque una copia a cualquier carpeta o pendrive, o importe una copia desde
  otro dispositivo. Es la forma de pasar los datos a un teléfono nuevo.

**Recomendación:** exporte una copia cada semana a una carpeta externa o a su almacenamiento en la
nube. Así, si el teléfono se pierde o se daña, no pierde el negocio.

## 12. Usuarios y roles (solo administrador)

**Más → Usuarios y roles**:

- **Nuevo usuario**: nombre de usuario (sin espacios), nombre completo, contraseña
  (mínimo 8 caracteres, con letras y números), **rol** y **sucursal** asignada (o *Todas*).
- En cada tarjeta puede **editar** el rol o la sucursal, **activar/desactivar** al usuario y
  **restablecer la contraseña** (icono de llave).
- Tras 5 intentos fallidos la cuenta se bloquea temporalmente.

## 13. Configuración

**Más → Configuración**:

- **Datos del negocio**: nombre comercial, razón social, identificación fiscal, dirección y teléfono.
  Aparecen en la cabecera de los informes y en los documentos exportados.
- **Valores por defecto**: impuesto por defecto, umbral global de stock bajo, prefijos de ticket y
  de nota de crédito, y símbolo de moneda.
- **Apariencia**: tema claro, oscuro o del sistema, y colores dinámicos (Android 12+).
- **Seguridad**: pedir contraseña al abrir, desbloqueo con **huella o rostro**, copias automáticas,
  y **Cambiar mi contraseña**.
- **Herramientas**: copias de seguridad, usuarios y roles, y **registro interno** (para diagnóstico
  técnico; se puede exportar y compartir).
- **Cerrar sesión** al final de la pantalla.

## 14. Preguntas frecuentes

**¿Necesito internet?** No. La aplicación no tiene permiso de internet: funciona siempre. Solo
necesita conexión la primera vez que Android Studio o GitHub compilan la aplicación, no el teléfono.

**¿Puedo usarla en varios dispositivos?** Cada dispositivo guarda sus propios datos. Puede mover la
información con **Exportar** copia en un teléfono e **Importar** en el otro.

**¿Qué pasa si cambio de teléfono?** Exporte la copia de seguridad, instale la aplicación en el
teléfono nuevo e importe el archivo.

**Olvidé mi contraseña.** Entre como administrador y use **Usuarios y roles → icono de llave** para
restablecerla. Si perdió también el administrador, restaure una copia de seguridad.

**¿Se pueden borrar ventas?** No. Por control interno las ventas se **anulan** o se **devuelven**;
el registro siempre permanece.

**Los informes salen en español aunque el teléfono esté en inglés.** La aplicación incluye ambos
idiomas y sigue el idioma del sistema; puede cambiarlo en la configuración de idiomas de Android
(Ajustes → Sistema → Idiomas → Aplicaciones).

**¿Dónde se guardan los archivos exportados?** Dentro del espacio privado de la aplicación
(carpeta `Reportes`). Al exportar use **Guardar como…** o **Compartir** para llevarlos a la carpeta
que prefiera.
