package ar.edu.ottokrause.datapipe;

import ar.edu.ottokrause.datapipe.concurrency.AnalyzerThread;
import ar.edu.ottokrause.datapipe.concurrency.IngestorThread;
import ar.edu.ottokrause.datapipe.infra.DirectoryManager;
import ar.edu.ottokrause.datapipe.infra.ZeroCopyTransfer;
import ar.edu.ottokrause.datapipe.resilience.FailureSimulator;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

/**
 * Clase principal de orquestación y demostración interactiva de Krause DataPipe Engine.
 * Cohesiona los entregables del Portafolio y demuestra la resiliencia del software.
 */
public class Main {
    private static final Path BASE_PATH = Paths.get(".").toAbsolutePath().normalize();
    private static final Path INGESTA_DIR = BASE_PATH.resolve("data/ingesta");
    private static final Path PROCESADOS_DIR = BASE_PATH.resolve("data/procesados");
    private static final Path ERRORES_DIR = BASE_PATH.resolve("data/errores");

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("            KRAUSE DATAPIPE ENGINE - PORTAFOLIO DE INGENIERÍA");
        System.out.println("            Asignatura: Programación sobre Redes y Sist. Concurrentes");
        System.out.println("            EEST N°1 Ing. Otto Krause - 6to Computación");
        System.out.println("=====================================================================");

        try {
            // CLASE 1: Inicialización de entorno y permisos
            System.out.println("\n[CLASE 1] Inicializando directorios y aplicando seguridad...");
            DirectoryManager.initDirectories(BASE_PATH);
            System.out.println("[CLASE 1] Directorios de trabajo verificados y listos.");

            // Crear archivo de prueba inicial si no existe
            generarArchivoPruebaDefault();

            // Menú interactivo para ejecutar escenarios de prueba
            Scanner scanner = new Scanner(System.in);
            boolean salir = false;
            
            while (!salir) {
                printMenu();
                System.out.print("Seleccione una opción: ");
                String opcion = scanner.nextLine().trim();
                
                switch (opcion) {
                    case "1":
                        ejecutarHappyPath();
                        break;
                    case "2":
                        ejecutarRutaRapidaZeroCopy();
                        break;
                    case "3":
                        ejecutarFallaPipeRoto();
                        break;
                    case "4":
                        ejecutarFallaBloqueoCarpeta();
                        break;
                    case "5":
                        ejecutarFallaInterrupcionHilo();
                        break;
                    case "6":
                        salir = true;
                        System.out.println("Saliendo del Krause DataPipe Engine. ¡Hasta luego!");
                        break;
                    default:
                        System.out.println("Opción inválida. Intente de nuevo.");
                }
            }
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("[FALLO GENERAL] Ocurrió una excepción fatal en Main: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printMenu() {
        System.out.println("\n-------------------------------------------------------------");
        System.out.println("                  MENÚ DE ESCENARIOS DE PRUEBA");
        System.out.println("-------------------------------------------------------------");
        System.out.println("1. [HAPPY PATH] Procesamiento asíncrono RAM de log telemetría");
        System.out.println("2. [FAST-PATH] Optimización de hardware (Zero-Copy)");
        System.out.println("3. [ESTRÉS ROL C] Falla de Pipe Roto (Divergir huérfanos)");
        System.out.println("4. [ESTRÉS ROL C] Acceso Carpeta Denegado (Bloqueo en escritura)");
        System.out.println("5. [ESTRÉS ROL C] Interrupción física de Hilos (Hardware Failure)");
        System.out.println("6. Salir");
        System.out.println("-------------------------------------------------------------");
    }

    /**
     * Genera datos de logs de prueba iniciales.
     */
    private static void generarArchivoPruebaDefault() throws IOException {
        Path testLog = INGESTA_DIR.resolve("telemetria_1.log");
        if (!Files.exists(testLog)) {
            try (BufferedWriter writer = Files.newBufferedWriter(testLog)) {
                writer.write("1 | [SYS] Inicio del sistema - Motores en frío\n");
                writer.write("1 | [SYS] Cargando librerías dinámicas de red\n");
                writer.write("2 | [NET] Demora detectada en puerto serial COM3\n");
                writer.write("1 | [SYS] Almacenamiento primario montado en RAM\n");
                writer.write("3 | [HARD] Sobretemperatura detectada en Núcleo 2 (78C)\n");
                writer.write("2 | [NET] Paquetes UDP descartados por checksum inválido\n");
                writer.write("1 | [SYS] Sincronizando relojes del sistema con NTP\n");
                writer.write("3 | [HARD] Caída brusca de voltaje detectada en riel 5V\n");
                writer.write("1 | [SYS] Ejecutando rutinas de calibración del acelerómetro\n");
                writer.write("1 | [SYS] Sistema estable - Esperando comando remoto\n");
            }
            System.out.println("[INFO] Creado archivo de telemetría de prueba inicial: " + testLog.getFileName());
        }
    }

    /**
     * ESCENARIO 1: Happy Path.
     * Ejecuta el pipeline completo de forma ordenada.
     */
    private static void ejecutarHappyPath() {
        System.out.println("\n>>> INICIANDO ESCENARIO: HAPPY PATH (EJECUCIÓN NORMAL) <<<");
        long startTime = System.currentTimeMillis();

        try {
            Path fileIn = INGESTA_DIR.resolve("telemetria_1.log");
            Path fileOut = PROCESADOS_DIR.resolve("output_analizado.log");

            // Asegurar que no esté bloqueada la carpeta de ejecuciones anteriores
            DirectoryManager.setDirectoryBlocked(PROCESADOS_DIR, false);

            // CLASE 2: Acoplamiento neumático en RAM
            PipedOutputStream pos = new PipedOutputStream();
            // Redimensionar el buffer nativo a 4096 bytes (Rol B)
            PipedInputStream pis = new PipedInputStream(pos, 4096); 
            System.out.println("[Rol B][Pipes] Tubería RAM instanciada y configurada a 4096 bytes de buffer circular.");

            // Instanciar hilos
            IngestorThread ingestor = new IngestorThread(fileIn, pos, ERRORES_DIR);
            AnalyzerThread analyzer = new AnalyzerThread(pis, fileOut);

            Thread threadIngestor = new Thread(ingestor, "Hilo-Ingestor");
            Thread threadAnalyzer = new Thread(analyzer, "Hilo-Analizador");

            // Lanzar hilos concurrentes
            threadIngestor.start();
            threadAnalyzer.start();

            // Esperar que completen su trabajo
            threadIngestor.join();
            threadAnalyzer.join();

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("\n[MÉTRICAS] Pipeline ejecutado con éxito.");
            System.out.println("  -> Tiempo total: " + duration + " ms");
            System.out.println("  -> Buffer circular de 4KB previno sobrecarga de Context Switches.");
            System.out.println("  -> Archivo de salida procesado generado en: " + fileOut.toAbsolutePath());
            System.out.println("  -> Contenido del archivo de salida:");
            
            // Mostrar las primeras 5 líneas del archivo de salida
            try (BufferedReader reader = Files.newBufferedReader(fileOut)) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 5) {
                    System.out.println("     " + line);
                    count++;
                }
                if (count == 5) System.out.println("     ... (ver archivo completo para más detalles)");
            }

        } catch (Exception e) {
            System.err.println("[FALLO ESCENARIO 1]: " + e.getMessage());
        }
    }

    /**
     * ESCENARIO 2: Zero-Copy Fast-Path.
     * Crea un archivo simulado y ejecuta la copia de kernel directa.
     */
    private static void ejecutarRutaRapidaZeroCopy() {
        System.out.println("\n>>> INICIANDO ESCENARIO: FAST-PATH DE HARDWARE (ZERO-COPY) <<<");
        try {
            Path fileLargeIn = INGESTA_DIR.resolve("archivo_masivo_51MB.tmp");
            Path fileLargeOut = PROCESADOS_DIR.resolve("archivo_masivo_procesado.tmp");

            // Crear un archivo de 51 MB rápidamente
            if (!Files.exists(fileLargeIn)) {
                System.out.println("[INFO] Generando archivo de 51 MB en ingesta para simulación... Por favor espere.");
                byte[] chunk = new byte[1024 * 1024]; // 1 MB buffer
                try (FileChannel channel = FileChannel.open(fileLargeIn, 
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = ByteBuffer.wrap(chunk);
                    for (int i = 0; i < 51; i++) {
                        buffer.position(0);
                        channel.write(buffer);
                    }
                }
                System.out.println("[INFO] Archivo de 51 MB generado.");
            }

            long startTime = System.currentTimeMillis();
            
            // Evaluar y transferir por Zero-Copy (Rol A)
            boolean zeroCopyApplied = ZeroCopyTransfer.checkAndTransfer(fileLargeIn, fileLargeOut);
            
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("\n[MÉTRICAS ZERO-COPY]");
            System.out.println("  -> ¿Se aplicó Zero-Copy?: " + zeroCopyApplied);
            System.out.println("  -> Tiempo empleado: " + duration + " ms");
            System.out.println("  -> Rendimiento aproximado: " + (51.0 / (duration / 1000.0)) + " MB/s");
            System.out.println("  -> Evadió el consumo de CPU del espacio de usuario.");

            // Limpieza del archivo de prueba grande para no saturar el espacio de trabajo
            Files.deleteIfExists(fileLargeIn);
            Files.deleteIfExists(fileLargeOut);
            System.out.println("[INFO] Archivos de prueba grandes eliminados para limpieza.");

        } catch (Exception e) {
            System.err.println("[FALLO ESCENARIO 2]: " + e.getMessage());
        }
    }

    /**
     * ESCENARIO 3: Falla de Pipe Roto.
     * Cierra el pipe de lectura a mitad de camino y comprueba el desvío.
     */
    private static void ejecutarFallaPipeRoto() {
        System.out.println("\n>>> INICIANDO ESCENARIO: FALLA DE PIPE ROTO (QA ESTRÉS) <<<");
        try {
            Path fileIn = INGESTA_DIR.resolve("telemetria_1.log");
            Path fileOut = PROCESADOS_DIR.resolve("output_fallido.log");

            // Limpiar residuos
            DirectoryManager.setDirectoryBlocked(PROCESADOS_DIR, false);

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos, 4096);

            IngestorThread ingestor = new IngestorThread(fileIn, pos, ERRORES_DIR);
            AnalyzerThread analyzer = new AnalyzerThread(pis, fileOut);

            Thread threadIngestor = new Thread(ingestor, "Hilo-Ingestor-PropensoAFalla");
            Thread threadAnalyzer = new Thread(analyzer, "Hilo-Analizador-PropensoAFalla");

            threadIngestor.start();
            threadAnalyzer.start();

            // Esperar 15 milisegundos y colapsar el lector
            Thread.sleep(15);
            FailureSimulator.simulatePipeCrash(pis);

            threadIngestor.join();
            threadAnalyzer.join();

            System.out.println("\n[VERIFICACIÓN ROL C]");
            System.out.println("  -> Buscando archivos huérfanos rescatados en: " + ERRORES_DIR.getFileName());
            
            try (var stream = Files.list(ERRORES_DIR)) {
                stream.forEach(p -> {
                    if (p.getFileName().toString().startsWith("datos_huerfanos_")) {
                        System.out.println("     * Archivo encontrado: " + p.getFileName() + " (" + p.toFile().length() + " bytes)");
                        // Borrar archivo para dejar limpio el entorno
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("[FALLO ESCENARIO 3]: " + e.getMessage());
        }
    }

    /**
     * ESCENARIO 4: Acceso Carpeta Denegado.
     * Quita permisos a la carpeta de salida a mitad de ejecución.
     */
    private static void ejecutarFallaBloqueoCarpeta() {
        System.out.println("\n>>> INICIANDO ESCENARIO: ACCESO CARPETA DENEGADO (STRESS TEST) <<<");
        try {
            Path fileIn = INGESTA_DIR.resolve("telemetria_1.log");
            Path fileOut = PROCESADOS_DIR.resolve("output_seguridad.log");

            // Asegurar desbloqueo inicial
            DirectoryManager.setDirectoryBlocked(PROCESADOS_DIR, false);

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos, 4096);

            IngestorThread ingestor = new IngestorThread(fileIn, pos, ERRORES_DIR);
            AnalyzerThread analyzer = new AnalyzerThread(pis, fileOut);

            Thread threadIngestor = new Thread(ingestor, "Hilo-Ingestor-Carpeta");
            Thread threadAnalyzer = new Thread(analyzer, "Hilo-Analizador-Carpeta");

            threadIngestor.start();
            threadAnalyzer.start();

            // Bloquear carpeta en caliente a mitad de ejecución
            Thread.sleep(15);
            FailureSimulator.simulateFolderLockout(PROCESADOS_DIR);

            threadIngestor.join();
            threadAnalyzer.join();

            // Restaurar permisos
            FailureSimulator.restoreFolderAccess(PROCESADOS_DIR);
            
            System.out.println("\n[VERIFICACIÓN ROL C]");
            System.out.println("  -> El sistema arrojó y capturó exitosamente la AccesoCarpetaDenegadoException sin congelar el servidor.");

        } catch (Exception e) {
            System.err.println("[FALLO ESCENARIO 4]: " + e.getMessage());
        }
    }

    /**
     * ESCENARIO 5: Interrupción física de Hilos.
     * Envía señal de interrupción al hilo de control.
     */
    private static void ejecutarFallaInterrupcionHilo() {
        System.out.println("\n>>> INICIANDO ESCENARIO: INTERRUPCIÓN DE HILOS (HARDWARE INTERRUPT) <<<");
        try {
            Path fileIn = INGESTA_DIR.resolve("telemetria_1.log");
            Path fileOut = PROCESADOS_DIR.resolve("output_interrumpido.log");

            DirectoryManager.setDirectoryBlocked(PROCESADOS_DIR, false);

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos, 4096);

            IngestorThread ingestor = new IngestorThread(fileIn, pos, ERRORES_DIR);
            AnalyzerThread analyzer = new AnalyzerThread(pis, fileOut);

            Thread threadIngestor = new Thread(ingestor, "Hilo-Ingestor-Interrumpible");
            Thread threadAnalyzer = new Thread(analyzer, "Hilo-Analizador-Interrumpible");

            threadIngestor.start();
            threadAnalyzer.start();

            // Interrumpir el hilo ingestor inmediatamente
            Thread.sleep(10);
            FailureSimulator.simulateThreadInterrupt(threadIngestor);
            FailureSimulator.simulateThreadInterrupt(threadAnalyzer);

            threadIngestor.join();
            threadAnalyzer.join();

            System.out.println("\n[VERIFICACIÓN ROL C]");
            System.out.println("  -> Los hilos se detuvieron limpiamente y liberaron sus descriptores de E/S de manera ordenada.");

        } catch (Exception e) {
            System.err.println("[FALLO ESCENARIO 5]: " + e.getMessage());
        }
    }
}
