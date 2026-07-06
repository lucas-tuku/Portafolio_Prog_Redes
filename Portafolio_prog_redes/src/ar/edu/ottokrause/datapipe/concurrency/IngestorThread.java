package ar.edu.ottokrause.datapipe.concurrency;

import ar.edu.ottokrause.datapipe.exceptions.CanalRotoException;
import ar.edu.ottokrause.datapipe.infra.DirectoryManager;
import ar.edu.ottokrause.datapipe.model.LogFrame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Hilo Ingestor que procesa los archivos de logs de entrada y los vuelca en la tubería RAM.
 * Diseñado por el Rol B (Especialista en Concurrencia y Pipes).
 */
public class IngestorThread implements Runnable {
    private final Path archivoIngesta;
    private final PipedOutputStream pipeOutput;
    private final Path carpetaErrores;

    public IngestorThread(Path archivoIngesta, PipedOutputStream pipeOutput, Path carpetaErrores) {
        this.archivoIngesta = archivoIngesta;
        this.pipeOutput = pipeOutput;
        this.carpetaErrores = carpetaErrores;
    }

    @Override
    public void run() {
        System.out.println("[Rol B][Threads] Hilo Ingestor INICIADO. Leyendo desde: " + archivoIngesta.getFileName());
        
        // Usamos try-with-resources para leer el archivo de ingesta de forma limpia
        try (BufferedReader reader = Files.newBufferedReader(archivoIngesta)) {
            String linea;
            long lineaNum = 0;
            
            while ((linea = reader.readLine()) != null) {
                lineaNum++;
                
                // Formatear mensaje como trama. Formato del archivo origen: NIVEL|MENSAJE (ej: 1|Inicio de sistema)
                byte level = 1; // INFO por defecto
                String mensaje = linea;
                
                if (linea.contains("|")) {
                    try {
                        int pos = linea.indexOf("|");
                        level = Byte.parseByte(linea.substring(0, pos).trim());
                        mensaje = linea.substring(pos + 1);
                    } catch (NumberFormatException ignored) {}
                }
                
                // Modela el contrato de datos (LogFrame)
                LogFrame frame = new LogFrame(System.currentTimeMillis(), level, mensaje);
                byte[] bytesTrama = frame.serialize();
                
                try {
                    // Escribir en la tubería en RAM
                    pipeOutput.write(bytesTrama);
                    pipeOutput.flush();
                    
                    // Simular pequeño retraso para observar la concurrencia
                    Thread.sleep(10); 
                    
                } catch (IOException e) {
                    // Captura forense de error de pipe roto (ej: IOException: Pipe broken)
                    if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("broken") || e.getMessage().toLowerCase().contains("pipe closed"))) {
                        desviarDatosHuerfanos(bytesTrama, e);
                        // Lanzamos una excepción personalizada de negocio para reportar al controlador
                        throw new CanalRotoException("Tubería RAM colapsada inesperadamente durante escritura de trama", bytesTrama.length, e);
                    } else {
                        throw e; // Otros errores de E/S
                    }
                }
            }
            
            System.out.println("[Rol B][Threads] Hilo Ingestor finalizó correctamente. Total líneas: " + lineaNum);
            
        } catch (InterruptedException e) {
            System.out.println("[Rol B][Threads] Hilo Ingestor INTERRUMPIDO por señal externa.");
            Thread.currentThread().interrupt(); // Restablecer estado de interrupción
        } catch (CanalRotoException e) {
            System.err.println("[Rol C][Resiliencia] ERROR FORENSE CAPTURADO: " + e.getMessage() + ". Detalle: " + e.toString());
        } catch (IOException e) {
            System.err.println("[Rol C][Resiliencia] Error de lectura de ingesta: " + e.getMessage());
        } finally {
            // Cerramos de forma segura la salida de la tubería
            try {
                pipeOutput.close();
            } catch (IOException e) {
                System.err.println("[Rol C][QA] Error cerrando el PipedOutputStream: " + e.getMessage());
            }
        }
    }

    /**
     * Captura y desvía la trama huérfana al disco (data/errores) para no perder información.
     */
    private void desviarDatosHuerfanos(byte[] datos, Exception causa) {
        Path recoverFile = carpetaErrores.resolve("datos_huerfanos_" + System.currentTimeMillis() + ".bin");
        System.err.printf("[Rol C][Resiliencia] !!! DETECTADA CAÍDA DE RECEPTOR !!! Desviando %d bytes huérfanos a: %s%n", 
                datos.length, recoverFile.getFileName());
        
        try {
            // Validar que la carpeta de errores esté disponible
            DirectoryManager.validateDirectoryPermissions(carpetaErrores);
            
            // Escritura directa de rescate usando FileChannel
            try (FileChannel channel = FileChannel.open(recoverFile, 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(datos);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
            System.out.println("[Rol C][Resiliencia] Datos huérfanos respaldados con éxito en disco.");
        } catch (IOException ex) {
            System.err.println("[Rol C][Resiliencia] ERROR CRÍTICO: No se pudieron respaldar los datos huérfanos. " 
                    + ex.getMessage());
            // Agregar al error original como excepción suprimida
            causa.addSuppressed(ex);
        }
    }
}
