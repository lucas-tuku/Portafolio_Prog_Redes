package ar.edu.ottokrause.datapipe.concurrency;

import ar.edu.ottokrause.datapipe.infra.DirectoryManager;
import ar.edu.ottokrause.datapipe.infra.NioWriter;
import ar.edu.ottokrause.datapipe.model.LogFrame;

import java.io.IOException;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Hilo Analizador/Criptográfico que lee de la tubería RAM, procesa los bytes en memoria y
 * realiza el volcado masivo en disco usando FileChannel y ByteBuffer.
 * Diseñado por el Rol B (Concurrencia) y colabora con el Rol A (E/S).
 */
public class AnalyzerThread implements Runnable {
    private final PipedInputStream pipeInput;
    private final Path archivoDestino;
    private final byte xorKey = 0x5F; // Clave para transformación algorítmica/criptográfica en RAM

    // Para la danza de punteros de ByteBuffer (Rol A)
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(1024); 

    public AnalyzerThread(PipedInputStream pipeInput, Path archivoDestino) {
        this.pipeInput = pipeInput;
        this.archivoDestino = archivoDestino;
    }

    @Override
    public void run() {
        System.out.println("[Rol B][Threads] Hilo Analizador/Criptográfico INICIADO. Esperando datos...");
        
        // Bloque try-with-resources jerárquico cruzado para garantizar el cierre seguro de recursos
        // Diseñado junto al Rol C para capturar excepciones suprimidas al cerrar canales.
        try (PipedInputStream in = pipeInput;
             FileChannel outChannel = FileChannel.open(archivoDestino, 
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            
            LogFrame frame;
            long procesados = 0;
            
            // Leer tramas deserializadas desde la tubería RAM
            while ((frame = LogFrame.deserialize(in)) != null) {
                // Verificar que no nos hayan quitado permisos del directorio en plena ejecución (QA de Rol C)
                DirectoryManager.validateDirectoryPermissions(archivoDestino.getParent());

                procesados++;
                
                // --- Transformación Algorítmica en RAM sin tocar disco (Rol B) ---
                String originalMsg = frame.getMessage();
                byte[] msgBytes = originalMsg.getBytes(StandardCharsets.UTF_8);
                byte[] transformedBytes = new byte[msgBytes.length];
                
                // Criptografía XOR simple
                for (int i = 0; i < msgBytes.length; i++) {
                    transformedBytes[i] = (byte) (msgBytes[i] ^ xorKey);
                }
                
                // Formatear el registro final transformado
                String hexTransformed = bytesToHex(transformedBytes);
                String registroProcesado = String.format("[%s][%d] MSG: %s | XOR: %s%n", 
                        frame.getLevelString(), frame.getTimestamp(), originalMsg, hexTransformed);
                
                byte[] registroBytes = registroProcesado.getBytes(StandardCharsets.UTF_8);
                
                // --- Escritura Definitiva usando FileChannel y ByteBuffer (Danza del Rol A) ---
                NioWriter.writeData(registroBytes, outChannel, writeBuffer);
                
                // Simulación de carga de trabajo
                Thread.sleep(5);
            }
            
            // Forzar el vaciado de los bytes remanentes en el buffer antes de cerrar
            NioWriter.forceFinalFlush(outChannel, writeBuffer);
            System.out.printf("[Rol B][Threads] Hilo Analizador finalizó correctamente. Tramas procesadas: %d%n", procesados);
            
        } catch (InterruptedException e) {
            System.out.println("[Rol B][Threads] Hilo Analizador/Criptográfico INTERRUMPIDO por señal externa.");
            Thread.currentThread().interrupt(); // Restablecer bandera de interrupción
        } catch (IOException e) {
            // Manejo forense de excepciones
            System.err.println("[Rol C][Excepciones] ERROR DE E/S EN HILO ANALIZADOR: " + e.toString());
            
            // Reportar las excepciones suprimidas si existieran
            Throwable[] suprimidas = e.getSuppressed();
            if (suprimidas.length > 0) {
                System.err.println("[Rol C][Excepciones] !!! Detectadas excepciones concurrentes suprimidas al cerrar canales !!!");
                for (Throwable t : suprimidas) {
                    System.err.println("   -> Suprimida: " + t.toString());
                }
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
