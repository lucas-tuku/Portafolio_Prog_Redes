# Portafolio_Prog_Redes[
# Krause DataPipe Engine 🚀
### Portafolio Cohesivo de Ingeniería de Software — EEST N°1 Ing. Otto Krause
**Curso**: 6to Computación  
**Asignatura**: Programación sobre Redes y Sistemas Concurrentes  
**Metodología**: Aprendizaje Basado en Proyectos (ABP)

---

## 📋 Descripción del Proyecto

El **Krause DataPipe Engine** es un motor de procesamiento de flujos de datos asíncronos de alta velocidad implementado en Java 23. Su propósito principal es procesar archivos de telemetría y logs en tiempo real, aplicando transformaciones criptográficas (XOR) directamente en memoria RAM antes de persistirlos en disco.

Este sistema demuestra de forma integrada tres grandes pilares de la programación avanzada:
1. **Manejo Polimórfico de Excepciones y Resiliencia Forense** (try-with-resources cruzados, excepciones suprimidas y redireccionamiento de datos huérfanos).
2. **Concurrencia Activa y Tuberías en RAM** (acoplamiento asíncrono con hilos independientes usando buffers circulares de 4KB para minimizar Context Switches).
3. **Persistencia NIO y Optimización de Hardware** (gestión de descriptores de archivos, danza de punteros `.flip()`, `.compact()`, `.clear()` en buffers directos de memoria, y bypass de Kernel *Zero-Copy* para archivos masivos).

---

## 🛠️ Organización Estratégica por Roles

La arquitectura del sistema divide las responsabilidades técnicas del equipo en tres roles clave:

| Rol | Especialidad | Responsabilidad Principal | Componentes del Código |
| :--- | :--- | :--- | :--- |
| **Rol A** | **NIO / NIO.2** | Infraestructura de archivos, validación de seguridad (POSIX/Windows), persistencia optimizada y ruta rápida de hardware (*Zero-Copy*). | `DirectoryManager`, `NioWriter`, `ZeroCopyTransfer` |
| **Rol B** | **Pipes & Threads** | Concurrencia activa, ciclo de vida de los hilos Ingestor y Analizador, contrato de datos y canalización neumática en RAM. | `LogFrame`, `IngestorThread`, `AnalyzerThread` |
| **Rol C** | **Excepciones** | Resiliencia catastrófica, jerarquía de excepciones personalizadas, captura forense de errores concurrentes suprimidos y plan de Stress Testing. | `FallaInfraestructuraException`, `AccesoCarpetaDenegadoException`, `CanalRotoException`, `FailureSimulator` |

---

## 🏗️ Arquitectura y Flujo de Datos

```mermaid
flowchart LR
    A[data/ingesta/*.log] -->|Lectura| B(Hilo Ingestor)
    B -->|LogFrame Binario| C[Tubería RAM 4KB]
    C -->|Deserialización| D(Hilo Analizador)
    D -->|Criptografía XOR| E[Persistidor NIO]
    E -->|ByteBuffer + FileChannel| F[data/procesados/*.log]
    
    subgraph Resiliencia (Rol C)
        C -.->|En caso de Pipe Broken| G[data/errores/datos_huerfanos.bin]
    end
```

---

## 📦 Estructura del Proyecto

```text
Portafolio_prog_redes/
├── .idea/                           # Configuraciones de IntelliJ IDEA
│   ├── misc.xml                     # Asignación de SDK JDK 23
│   └── modules.xml                  # Registro de módulos
├── src/
│   └── ar/edu/ottokrause/datapipe/
│       ├── concurrency/             # Hilos ingestor, analizador (Rol B)
│       │   ├── IngestorThread.java
│       │   └── AnalyzerThread.java
│       ├── exceptions/              # Jerarquía de excepciones checked (Rol C)
│       │   ├── FallaInfraestructuraException.java
│       │   ├── AccesoCarpetaDenegadoException.java
│       │   └── CanalRotoException.java
│       ├── infra/                   # Motores de persistencia y archivos (Rol A)
│       │   ├── DirectoryManager.java
│       │   ├── NioWriter.java
│       │   └── ZeroCopyTransfer.java
│       ├── model/                   # Estructura de la trama binaria (Rol B)
│       │   └── LogFrame.java
│       ├── resilience/              # Inyectores de fallas en caliente (Rol C)
│       │   └── FailureSimulator.java
│       └── Main.java                # Orquestador del menú interactivo de pruebas
├── Portafolio_prog_redes.iml        # Mapeo de IntelliJ IDEA
├── compile_and_run.ps1              # Script PowerShell de compilación y ejecución rápida
└── README.md                        # Documentación principal del sistema
```

---

## 🚀 Guía de Uso y Ejecución

### Requisitos Previos
* **Java Development Kit (JDK)** versión **23** o superior.
* Asegúrate de tener configurada la variable de entorno `JAVA_HOME` y `javac` en tu variable de entorno `PATH`.

---

### Compilación y Ejecución por Consola (PowerShell)

Para compilar las clases del proyecto y lanzar la interfaz interactiva de escenarios de pruebas, ejecuta el script proporcionado en la consola PowerShell (omitiendo restricciones de ejecución locales):

```powershell
powershell -ExecutionPolicy Bypass -File .\compile_and_run.ps1
```

---

### Menú de Escenarios de Prueba (Stress Testing Integrado)

Al iniciar la aplicación, se desplegará el siguiente menú interactivo en tu terminal:

1. **`[HAPPY PATH] Procesamiento asíncrono RAM`**: Lee datos desde `data/ingesta/telemetria_1.log`, los canaliza por tuberías asíncronas en RAM y genera los logs cifrados correspondientes en `data/procesados/output_analizado.log`.
2. **`[FAST-PATH] Optimización de hardware (Zero-Copy)`**: Genera automáticamente un archivo temporal de 51 MB en disco y ejecuta una transferencia directa de hardware a través del Kernel, logrando velocidades superiores a **300 MB/s** sin sobrecargar la CPU del proceso.
3. **`[ESTRÉS ROL C] Falla de Pipe Roto (Divergir huérfanos)`**: Inicia la transferencia concurrente en memoria y colapsa el lector a mitad de la ejecución. El hilo ingestor captura la excepción `CanalRotoException` y desvía de forma segura los bytes huérfanos a `data/errores/`.
4. **`[ESTRÉS ROL C] Acceso Carpeta Denegado`**: Simula una revocación física de los permisos de escritura en la carpeta de salida a mitad de la ejecución, arrojando la excepción `AccesoCarpetaDenegadoException` y verificando que el servidor capture el fallo sin congelar su ejecución.
5. **`[ESTRÉS ROL C] Interrupción física de Hilos`**: Simula una señal de interrupción física de hardware (`.interrupt()`) en ambos hilos, comprobando que el sistema libere todos los descriptores de canales NIO abiertos de manera armoniosa y controlada.


## 📊 Bitácora de Punteros de Buffer (Clase 3)

| Fase Operativa | Operación Realizada | Position (Posición) | Limit (Límite) | Capacity (Capacidad) | Explicación |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Fase 1: Llenado** | Antes de llamar a `.flip()` | `1024` | `1024` | `1024` | El buffer ha terminado de cargarse con datos en memoria RAM. El puntero está al final de la carga. |
| **Fase 2: Lectura** | Después de llamar a `.flip()` | `0` | `1024` | `1024` | El puntero vuelve a la posición inicial `0` y el límite se fija al tamaño total escrito. Listo para pasar al canal de archivo. |
| **Fase 3: Compactación** | Después de llamar a `.compact()` | `0` | `1024` | `1024` | El canal escribió todos los bytes. El buffer se vació por completo y los punteros se preparan para recibir una nueva tanda de bytes. |
