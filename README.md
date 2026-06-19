# NEXUS DAE — Simulador de Despacho Fiscal

<p align="center">
  <strong>Actividad de Simulación Interactiva de Despacho Fiscal</strong><br/>
  <em>Fiscalía General de la Nación · Colombia</em>
</p>

---

## 📋 Descripción

**NEXUS DAE** es una aplicación de escritorio construida con **JavaFX 17** que simula un entorno de despacho fiscal interactivo. Permite a fiscales en formación practicar la gestión de casos judiciales, análisis de evidencia, agrupación de casos y toma de decisiones bajo presión temporal, todo dentro de un tablero analítico visual.

La aplicación está diseñada como una herramienta pedagógica para la **Fiscalía General de la Nación de Colombia**, orientada a fortalecer las competencias del personal en el manejo de despachos fiscales.

---

## ✨ Características Principales

| Funcionalidad | Descripción |
|---|---|
| **Tablero Analítico** | Canvas interactivo con nodos arrastrables, conexiones entre casos y zoom/pan |
| **Gestión de Casos** | Vista de cuadrícula con tarjetas, búsqueda en tiempo real y detalles con visor de imagen |
| **Agrupación de Casos** | Crear nuevos grupos o agregar casos a grupos existentes con justificación obligatoria |
| **Reloj de Investigación** | Temporizador regresivo de 3 horas que simula la presión temporal real |
| **Alertas de Distracción** | Sistema de alertas contextuales que penalizan el tiempo restante |
| **Exportación PDF** | Generación de reportes de investigación en formato PDF |
| **Persistencia de Sesión** | Guardado automático del estado (nodos, conexiones, grupos) en JSON |
| **Registro de Equipo** | Captura de integrantes del equipo investigador antes de iniciar |
| **Empaquetado Nativo** | Distribución auto-contenida para Windows (.zip) y macOS (.dmg) |

---

## 🏗️ Arquitectura del Proyecto

```
PRISMA-DAE-APP/
├── src/main/java/com/prisma/
│   ├── App.java                  # Punto de entrada JavaFX (Application)
│   ├── Launcher.java             # Launcher para empaquetado con jpackage
│   ├── data/
│   │   └── CasoRepository.java   # Repositorio de casos (carga desde carpeta o seed)
│   ├── models/
│   │   └── Caso.java             # Modelo de datos de un caso judicial
│   ├── ui/
│   │   └── Theme.java            # Aplicación de tema CSS global
│   ├── views/
│   │   ├── LoginView.java         # Pantalla de inicio con acceso al simulador
│   │   ├── AdminViewNew.java      # Panel administrativo principal
│   │   ├── InstructionsView.java  # Vista de instrucciones del simulador
│   │   ├── CasesManagementBrownView.java  # Gestión de casos (cuadrícula)
│   │   ├── PlayerView.java        # Tablero analítico (canvas con nodos)
│   │   ├── PlayerViewBrown.java   # Wrapper de PlayerView con tema brown
│   │   ├── DistractionAlertManager.java  # Gestor de alertas de distracción
│   │   ├── InvestigationClock.java       # Temporizador de investigación
│   │   ├── InvestigationTeamContext.java # Contexto del equipo investigador
│   │   ├── GroupBounds.java       # Utilidad de cálculo de límites de grupo
│   │   └── AdminAlertView.java    # Vista de configuración de alertas
│   └── reports/
│       ├── InvestigationReportPdfExporter.java  # Exportador de PDF
│       └── InvestigationTestPdfGenerator.java   # Generador de pruebas PDF
├── src/main/resources/styles/
│   ├── nexus.css                  # Hoja de estilos principal
│   ├── prisma.css                 # Hoja de estilos alternativa
│   ├── board-brown.css            # Estilos del tablero analítico
│   └── assets/                    # Imágenes y recursos gráficos
├── casos/                         # Carpeta de imágenes de casos (auto-detectada)
├── alertas/                       # Carpeta de configuración de alertas
├── scripts/                       # Scripts de empaquetado multiplataforma
├── pom.xml                        # Configuración de Maven
└── PACKAGING.md                   # Instrucciones de empaquetado
```

---

## 🔧 Requisitos Previos

| Requisito | Versión |
|---|---|
| **JDK** | 17 o superior |
| **Maven** | 3.8+ |
| **JavaFX** | 17.0.6 (se resuelve vía Maven) |

> **Nota:** No se requiere instalación separada de JavaFX. Las dependencias se descargan automáticamente a través de Maven.

---

## 🚀 Inicio Rápido

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/PRISMA-DAE-APP.git
cd PRISMA-DAE-APP
```

### 2. Agregar imágenes de casos (opcional)

Coloca las imágenes de los casos judiciales (`.png`, `.jpg`, `.jpeg`, `.gif`, `.bmp`, `.webp`) en la carpeta `casos/` en la raíz del proyecto. Cada archivo se convertirá automáticamente en un caso con el nombre del archivo (sin extensión).

```
casos/
├── Caso Aurora.jpg
├── Operación NEXUS.png
└── Noche Cero.jpeg
```

> Si la carpeta `casos/` está vacía o no existe, se cargan **5 casos de prueba** predefinidos.

### 3. Compilar y ejecutar

```bash
mvn clean javafx:run
```

La aplicación se abrirá en pantalla completa mostrando la pantalla de login.

---

## 📱 Flujo Sugerido de la Simulación

Para completar con éxito la actividad del simulador, el flujo de trabajo correcto e institucional recomendado es el siguiente:

1. **Leer las Instrucciones (`InstructionsView`)**: Antes de iniciar, ingresa a la sección de instrucciones desde el menú de Login para comprender las mecánicas del juego, el uso del tablero y las reglas del reloj de investigación.
2. **Analizar y Leer los Casos (`CasesManagementBrownView`)**: Navega a la gestión de casos, revisa cada una de las tarjetas disponibles y abre los detalles/imágenes de los casos para familiarizarte con las pruebas y los hechos.
3. **Asociar Casos con su Justificación**: Selecciona los casos relacionados que consideres que pertenecen a una misma línea investigativa y agrégalos a un nuevo grupo (o agrégalos a uno existente). Es obligatorio escribir una justificación penal e investigativa sólida para esta asociación.
4. **Justificar el Grupo (Tablero Analítico)**: Dirígete al tablero analítico (`PlayerView`), visualiza el cluster/grupo formado en el canvas y define/justifica la metadata global del grupo para consolidar la línea investigativa.
5. **Generar el Reporte PDF**: Una vez organizada la información y estructurado el tablero analítico de la investigación, haz clic en el botón de exportación para generar el reporte de investigación formal en formato PDF.

---

## 📦 Empaquetado y Distribución

La aplicación se puede distribuir como un paquete **auto-contenido** que incluye su propio runtime de Java personalizado mediante `jlink` y `jpackage`. Esto significa que el usuario final no necesita tener Java instalado en su sistema.

> ⚠️ **Importante:** La compilación y empaquetado del instalador nativo debe realizarse directamente en el sistema operativo de destino (Windows para generar el `.zip`/`.exe`, y macOS para generar el `.dmg`).

---

### 🪟 Especificaciones para Windows

#### Requisitos de Construcción (Desarrollador)
- **JDK 17 o superior** con la herramienta `jpackage` disponible en el `PATH` o definida bajo la variable de entorno `JAVA_HOME`.
- **Maven** instalado y configurado en las variables de entorno.
- *(Opcional)* **WiX Toolset (v3.x)** si se desea empaquetar como instalador `.msi` en lugar de un archivo comprimido `.zip`.

#### Requisitos para Ejecutar (Usuario Final)
- **Sistema Operativo:** Windows 10 o superior (64-bit).
- **Dependencias:** Ninguna (el paquete es auto-contenido y no requiere tener Java JDK/JRE instalado).
- **Instalación:** Descomprimir el archivo `.zip` generado y ejecutar `NEXUS-DAE.exe` haciendo doble clic.

#### Instrucciones de Empaquetado
Ejecuta el script de PowerShell en una terminal con permisos apropiados:
```powershell
.\scripts\package-windows.ps1
```

#### Resultado y Distribución
- El script generará el archivo auto-contenido en: `target\dist\NEXUS-DAE-1.0.0-windows.zip`.
- Al descomprimir este archivo en cualquier máquina Windows, se puede ejecutar la aplicación directamente haciendo doble clic sobre el ejecutable `NEXUS-DAE.exe`.
- Las carpetas locales de recursos (`casos/` y `alertas/`) se copiarán de manera automática junto al ejecutable para asegurar su correcto funcionamiento sin configuraciones manuales adicionales.

---

### 🍎 Especificaciones para macOS

#### Requisitos de Construcción (Desarrollador)
- **JDK 17 o superior** para macOS (compatible con la arquitectura del equipo: Intel o Apple Silicon M1/M2/M3).
- **Maven** instalado (se puede instalar fácilmente vía [Homebrew](https://brew.sh) con `brew install maven`).
- Permisos de ejecución habilitados para los scripts `.sh` de la carpeta `scripts/`.

#### Requisitos para Ejecutar (Usuario Final)
- **Sistema Operativo:** macOS 11.0 (Big Sur) o superior.
- **Dependencias:** Ninguna (el ejecutable `.dmg` incluye su propio entorno Java auto-contenido).
- **Instalación:** Abrir el `.dmg` y arrastrar `NEXUS-DAE.app` a la carpeta de **Aplicaciones**.

#### Instrucciones de Empaquetado
Asigna permisos de ejecución al script y lánzalo desde la terminal de macOS:
```bash
chmod +x scripts/package-macos.sh
bash scripts/package-macos.sh
```

#### Resultado y Distribución
- El script generará los siguientes archivos bajo la ruta: `target/dist/`
  - `NEXUS-DAE.app` (El bundle de aplicación nativa para macOS).
  - `NEXUS-DAE-1.0.0.dmg` (La imagen de disco de instalación).
- Para distribuir en macOS, se comparte el archivo `.dmg`. Al abrirlo, el usuario simplemente debe arrastrar la aplicación a su carpeta de **Aplicaciones**.
- **Nota sobre Seguridad en macOS (Gatekeeper):** Dado que el paquete no está firmado con un certificado de desarrollador de Apple, la primera vez que se ejecute la aplicación en una máquina destino podría mostrar un bloqueo de seguridad ("Desarrollador no identificado"). Para abrirla:
  1. Haz clic derecho (o `Ctrl + clic`) sobre la aplicación en la carpeta de Aplicaciones y selecciona **Abrir**.
  2. O bien, ve a **Ajustes del Sistema > Privacidad y Seguridad** y haz clic en **Abrir de todos modos**.

---

## 🗄️ Persistencia de Datos

La aplicación guarda el estado de la investigación automáticamente en:

```
~/Documents/NEXUS/active-session-snapshot.json
```

Este archivo contiene:
- Posiciones de nodos en el canvas
- Conexiones entre casos
- Grupos creados (con justificación y metadatos)
- Estado general de la investigación

### Eliminar datos guardados

Para reiniciar completamente la sesión, elimina el archivo de snapshot:

**Windows (PowerShell):**
```powershell
Remove-Item "$env:USERPROFILE\Documents\NEXUS\active-session-snapshot.json" -Force
```

**macOS / Linux:**
```bash
rm ~/Documents/NEXUS/active-session-snapshot.json
```

---

## ⚡ Optimizaciones de Rendimiento

La vista de **Gestión de Casos** incluye las siguientes optimizaciones para una carga fluida:

| Optimización | Detalle |
|---|---|
| **Carga asíncrona de imágenes** | Las imágenes se cargan en un hilo de fondo (`backgroundLoading=true`) con resolución reducida para thumbnails (580×280) |
| **Cache de imágenes** | Las imágenes cargadas se almacenan en memoria para evitar recargas en cada renderizado |
| **Cache de existencia de archivos** | Los resultados de `Files.exists()` se cachean para eliminar accesos repetidos al disco |
| **Renderizado diferido** | Las tarjetas se construyen en el siguiente frame (`Platform.runLater`) para que la UI aparezca inmediatamente |

---

## 🛠️ Dependencias

| Librería | Versión | Propósito |
|---|---|---|
| [JavaFX Controls](https://openjfx.io/) | 17.0.6 | Framework de interfaz gráfica |
| [JavaFX FXML](https://openjfx.io/) | 17.0.6 | Soporte para FXML (markup declarativo) |
| [Apache PDFBox](https://pdfbox.apache.org/) | 2.0.30 | Generación de reportes en PDF |
| [Ikonli JavaFX](https://kordamp.org/ikonli/) | 12.3.1 | Iconos vectoriales en la interfaz |
| [Ikonli FontAwesome5](https://kordamp.org/ikonli/) | 12.3.1 | Pack de iconos FontAwesome 5 |

---

## 📄 Licencia

Proyecto de uso institucional para la **Fiscalía General de la Nación de Colombia**.

---

<p align="center">
  <strong>NEXUS DAE</strong> · Simulador de Despacho Fiscal<br/>
  <em>Construido con JavaFX 17 · Maven · Apache PDFBox</em>
</p>
