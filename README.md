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

## 📱 Flujo de la Aplicación

```
Login → AdminViewNew → CasesManagement (Gestión de Casos)
  │                        ├── Ver detalle de caso (modal)
  │                        ├── Agrupar casos (nuevo grupo / grupo existente)
  │                        └── Tablero Analítico (PlayerView)
  │                              ├── Nodos arrastrables
  │                              ├── Conexiones entre casos
  │                              ├── Zoom / Pan
  │                              └── Exportar PDF
  └→ Instrucciones (InstructionsView)
```

### Pantallas principales

1. **LoginView** — Pantalla de bienvenida con animaciones. Acceso al simulador o a las instrucciones.
2. **AdminViewNew** — Panel central del despacho fiscal.
3. **CasesManagementBrownView** — Grid de tarjetas con:
   - Búsqueda en tiempo real
   - Visor de imagen con zoom y arrastre
   - Selección múltiple para agrupación
4. **PlayerView / PlayerViewBrown** — Tablero analítico tipo canvas:
   - Nodos de casos arrastrables
   - Conexiones con líneas
   - Agrupaciones visuales con justificación
   - Persistencia automática en JSON

---

## 📦 Empaquetado y Distribución

La aplicación se puede distribuir como un paquete **auto-contenido** (no requiere Java instalado en la máquina destino).

### Windows

```powershell
.\scripts\package-windows.ps1
```

**Salida:** `target\dist\NEXUS-DAE-1.0.0-windows.zip`

### macOS

```bash
bash scripts/package-macos.sh
```

**Salida:** `target/dist/NEXUS-DAE-1.0.0.dmg`

> ⚠️ Cada paquete debe construirse en su sistema operativo correspondiente.

Para más detalles, consulta [PACKAGING.md](PACKAGING.md).

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
