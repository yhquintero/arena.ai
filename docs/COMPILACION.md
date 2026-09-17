# Guía de compilación, firma y publicación

Esta guía está escrita paso a paso, sin dar por sentado ningún conocimiento técnico.
Elija **una** de las dos rutas:

- **Ruta A (recomendada):** compilar en la nube con GitHub Actions. No instala nada.
- **Ruta B:** compilar en su computadora con Android Studio.

---

## Ruta A — Compilar con GitHub Actions (sin instalar nada)

### A.1. Subir el proyecto a GitHub

1. Cree una cuenta en <https://github.com> (si aún no tiene).
2. Pulse el botón **+** (arriba a la derecha) → **New repository**.
3. Nombre: `gestion-integral-supermercados`. Déjelo en **Private**. Pulse **Create repository**.
4. En la página siguiente pulse **uploading an existing file**.
5. Arrastre **todo el contenido** de la carpeta del proyecto (incluidas las carpetas `app`,
   `gradle`, `.github`, y los archivos `build.gradle.kts`, `settings.gradle.kts`,
   `gradle.properties`, `gradlew`, `gradlew.bat`, `README.md`).
6. Abajo pulse **Commit changes**.

> Verifique que la carpeta `.github/workflows/android-build.yml` se haya subido: es el "robot" que
> compila la aplicación. Si no aparece, active *View → Show hidden files* en su explorador de
> archivos y vuelva a subirla.

### A.2. Ejecutar la compilación

1. En su repositorio pulse la pestaña **Actions**.
2. Si GitHub pregunta si desea habilitar los flujos de trabajo, pulse **I understand my workflows, go ahead and enable them**.
3. En la lista de la izquierda elija **Android CI**.
4. Pulse **Run workflow** → **Run workflow** (botón verde).
5. Espere entre 10 y 15 minutos. El círculo amarillo se convertirá en un ✔ verde.

### A.3. Descargar la aplicación compilada

1. Pulse sobre la ejecución finalizada (la que tiene ✔ verde).
2. Baje hasta **Artifacts** y descargue:
   - **`gis-supermercados-release-apk`** → contiene el archivo `.apk`: la aplicación lista para instalar.
   - **`gis-supermercados-release-aab`** → contiene el archivo `.aab`: el que se sube a Google Play.
   - **`gis-supermercados-debug-apk`** → versión de pruebas.
   - **`informes-pruebas-unitarias`** → reporte de las pruebas automáticas (ábralo en el navegador).
3. Descomprima el ZIP descargado para obtener el `.apk` o `.aab`.

### A.4. Instalar el APK en el teléfono

1. Copie el archivo `.apk` al teléfono (cable, correo o Drive).
2. Ábralo con el explorador de archivos del teléfono.
3. Android mostrará el aviso *"Por tu seguridad, el teléfono no puede instalar aplicaciones
   desconocidas"* → pulse **Configuración** → active **Permitir desde esta fuente** → vuelva atrás y
   pulse **Instalar**.
4. Al terminar, busque el icono verde con una cesta de compras: **Gestión Integral Supermercados**.

---

## Firmar la aplicación con su propia clave (opcional pero recomendado)

Sin este paso, GitHub Actions firma la versión release con una **clave de depuración**: sirve para
probar, pero **no** para publicar en Google Play y no permite actualizar la aplicación más adelante
de forma consistente.

### 1. Crear el keystore (una sola vez, guárdelo para siempre)

Necesita el programa `keytool`, que viene con el JDK. Si no lo tiene instalado, instale el
[Temurin JDK 17](https://adoptium.net/) y abra una terminal (Símbolo del sistema en Windows).

```bash
keytool -genkeypair -v -keystore gis-release.jks -keyalg RSA -keysize 2048 \
        -validity 10950 -alias gis_supermercados
```

Le pedirá una contraseña y algunos datos (nombre, organización, ciudad, país). Guarde el archivo
`gis-release.jks` **en dos lugares seguros** (por ejemplo un pendrive y una copia en la nube privada):
si lo pierde, **no podrá publicar actualizaciones** de la aplicación en Google Play.

### 2. Convertirlo a texto (Base64) para pegarlo en GitHub

**Windows (PowerShell):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("gis-release.jks")) | Set-Clipboard
```

**macOS / Linux:**

```bash
base64 -i gis-release.jks | pbcopy     # macOS
base64 gis-release.jks                 # Linux: copie el texto que aparece en pantalla
```

### 3. Guardar los secretos en GitHub

1. En su repositorio: **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.
2. Cree estos cuatro secretos con exactamente estos nombres:

| Nombre del secreto | Valor |
| --- | --- |
| `KEYSTORE_BASE64` | El texto Base64 del paso anterior (todo, sin espacios ni saltos). |
| `KEYSTORE_PASSWORD` | La contraseña del keystore que definió con `keytool`. |
| `KEY_ALIAS` | `gis_supermercados` (el alias que usó en el comando). |
| `KEY_PASSWORD` | La contraseña de la clave (normalmente la misma del keystore). |

3. Vuelva a **Actions** → **Android CI** → **Run workflow**. Ahora el release quedará firmado con su
   clave y el registro mostrará `Tipo de firma usada: release`.

---

## Publicar en Google Play

1. Cree una cuenta de desarrollador en <https://play.google.com/console> (pago único de 25 USD).
2. **Create app** → nombre: *Gestión Integral Supermercados*; idioma: Español (Estados Unidos);
   aplicación; gratuita.
3. Complete el cuestionario de contenido, la política de privacidad y la ficha de la tienda
   (icono 512×512, captura de pantalla, descripción).
4. En **Producción → Create new release** suba el archivo **`.aab`** descargado de GitHub Actions.
5. Firme con **Play App Signing** (Google conserva una clave de firma propia; usted sube con la
   clave creada arriba, llamada *upload key*).
6. Revise y pulse **Review release** → **Start rollout**.

> La aplicación **no solicita permiso de internet**, así que la ficha de "seguridad de los datos"
> se responde fácilmente: no se recogen ni se comparten datos con servidores externos.

---

## Ruta B — Compilar con Android Studio

### B.1. Instalar Android Studio

1. Descargue Android Studio desde <https://developer.android.com/studio>.
2. Instálelo con las opciones por defecto (incluye el SDK de Android y el JDK).

### B.2. Abrir el proyecto

1. Abra Android Studio → **Open** → seleccione la **carpeta raíz** del proyecto
   (la que contiene `settings.gradle.kts`).
2. Espere a que termine la sincronización de Gradle (barra inferior). La primera vez descarga las
   librerías y puede tardar 10-20 minutos; necesita internet **solo en este paso**.

### B.3. Probar en un emulador

1. **Device Manager** (icono de teléfono en la barra lateral) → **Create Virtual Device**.
2. Elija *Pixel 7* → imagen **API 35** → **Finish**.
3. Pulse el botón ▶ **Run 'app'**. La aplicación se instalará y abrirá en el emulador.

### B.4. Generar el APK

- **APK de pruebas:** menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
  Aparecerá un aviso con el enlace *locate*: la carpeta es
  `app/build/outputs/apk/debug/app-debug.apk`.
- **APK/AAB release (firmado):**
  1. Copie el archivo `keystore.properties.example` a `keystore.properties` en la raíz del proyecto.
  2. Edítelo con los datos reales de su keystore (ver tabla de la sección *Firmar*):

     ```properties
     storeFile=gis-release.jks
     storePassword=SU_PASSWORD
     keyAlias=gis_supermercados
     keyPassword=SU_PASSWORD
     ```

  3. Coloque `gis-release.jks` en la raíz del proyecto (junto a `settings.gradle.kts`).
  4. Menú **Build → Generate Signed Bundle / APK** → **Android App Bundle** (para Play) o
     **APK** (para instalar directamente) → seleccione su keystore → **release** → **Create**.
     Los archivos quedan en `app/build/outputs/`.

> Si no existe `keystore.properties`, el proyecto compila igualmente y firma el release con la clave
> de depuración: nunca se queda trabado por falta de credenciales.

### B.5. Ejecutar las pruebas unitarias

Menú **Run → Edit Configurations → + → Android Unit Test**, o en la terminal del proyecto:

```bash
./gradlew testDebugUnitTest
```

El reporte HTML queda en `app/build/reports/tests/testDebugUnitTest/index.html`.

---

## Solución de problemas frecuentes

| Problema | Qué hacer |
| --- | --- |
| El flujo de Actions falla en la primera compilación | Ábralo, pulse el paso fallido y copie el mensaje. La causa más común es no haber subido la carpeta `.github` completa o la carpeta `gradle/wrapper`. |
| `gradlew: Permission denied` | El flujo ya ejecuta `chmod +x ./gradlew`. Si compila en Linux/macOS a mano, ejecute `chmod +x gradlew` una vez. |
| Android dice "aplicación no instalada" | Ya existía una versión firmada con otra clave. Desinstale la aplicación anterior y vuelva a instalar. |
| Android Studio no termina de sincronizar | Revise su conexión (la primera sincronización necesita internet) y pulse **File → Sync Project with Gradle Files**. |
| La aplicación abre pero no tiene datos | En la pantalla de inicio active **Cargar datos de ejemplo** antes de crear el administrador, o cree sucursales y productos desde el menú **Más**. |
| Quiero cambiar el nombre de la aplicación | Edite `app_name` en `app/src/main/res/values/strings.xml` (español) y en `values-en/strings.xml` (inglés). |
| Quiero cambiar el icono | Reemplace `app/src/main/res/drawable/ic_launcher_foreground.xml` y el color `ic_launcher_background` en `res/values/colors.xml`. |

## Estructura del repositorio

```
.
├── .github/workflows/android-build.yml   # Compilación, pruebas y firma en la nube
├── app/
│   ├── build.gradle.kts                  # Configuración del módulo (SDK 35, minSdk 26, R8)
│   ├── proguard-rules.pro                # Reglas de ofuscación para el release
│   └── src/
│       ├── main/java/com/gis/supermercados/   # Código fuente (ver docs/ARQUITECTURA.md)
│       ├── main/res/                          # Iconos, temas y cadenas de texto (es / en)
│       └── test/                              # Pruebas unitarias de la lógica crítica
├── docs/                                 # Esta documentación
├── gradle/libs.versions.toml             # Versiones centralizadas de todas las librerías
├── keystore.properties.example           # Plantilla de credenciales de firma
├── build.gradle.kts / settings.gradle.kts
└── gradlew / gradlew.bat / gradle/wrapper/   # Gradle incluido (no hace falta instalarlo)
```
