package ar.edu.ottokrause.datapipe.infra;

import ar.edu.ottokrause.datapipe.exceptions.FallaInfraestructuraException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Optimización de hardware por canal directo (Zero-Copy).
 * Diseñado por el Rol A (Especialista NIO/NIO.2).
 */
public class ZeroCopyTransfer {
    private static final long FAST_PATH_THRESHOLD = 50 * 1024 * 1024; // 50 Megabytes

    /**
     * Evalúa si corresponde aplicar Fast-Path (Zero-Copy) y transfiere.
     * 
     * @param source Archivo de origen.
     * @param destination Archivo de destino.
     * @return true si se aplicó Zero-Copy, false si debe procesarse por tuberías de RAM convencionales.
     * @throws FallaInfraestructuraException si hay problemas de acceso físico.
     */
    public static boolean checkAndTransfer(Path source, Path destination) throws FallaInfraestructuraException {
        try {
            long size = source.toFile().length();
            if (size >= FAST_PATH_THRESHOLD) {
                System.out.printf("[Rol A][NIO] ARCHIVO PESADO DETECTADO (%d bytes). Iniciando Fast-Path (Zero-Copy)...%n", size);
                
                // Usamos Try-With-Resources para asegurar el cierre de canales de forma segura
                try (FileChannel srcChannel = FileChannel.open(source, StandardOpenOption.READ);
                     FileChannel destChannel = FileChannel.open(destination, 
                             StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    
                    long bytesTransferred = 0;
                    long totalSize = srcChannel.size();
                    
                    // TransferTo puede transferir menos de lo requerido si se interrumpe, iterar en bucle
                    while (bytesTransferred < totalSize) {
                        long transferred = srcChannel.transferTo(
                                bytesTransferred, 
                                totalSize - bytesTransferred, 
                                destChannel
                        );
                        if (transferred <= 0) {
                            break;
                        }
                        bytesTransferred += transferred;
                    }
                    
                    System.out.printf("[Rol A][NIO] Transferencia Zero-Copy completada. Se transfirieron %d bytes directamente a nivel de Kernel.%n", bytesTransferred);
                    return true;
                }
            }
        } catch (IOException e) {
            throw new FallaInfraestructuraException(
                    "Error crítico durante la transferencia Zero-Copy del archivo", 
                    source.toString() + " -> " + destination.toString(), 
                    e
            );
        }
        return false; // El archivo es pequeño, pasa al pipeline asíncrono
    }

    /**
     * Sobrecarga para forzar la transferencia Zero-Copy con fines de pruebas rápidas (sin importar el peso).
     */
    public static void forceTransfer(Path source, Path destination) throws FallaInfraestructuraException {
        try (FileChannel srcChannel = FileChannel.open(source, StandardOpenOption.READ);
             FileChannel destChannel = FileChannel.open(destination, 
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            
            srcChannel.transferTo(0, srcChannel.size(), destChannel);
            System.out.println("[Rol A][NIO] Transferencia Zero-Copy FORZADA exitosa.");
        } catch (IOException e) {
            throw new FallaInfraestructuraException(
                    "Error crítico durante la transferencia Zero-Copy forzada", 
                    source.toString(), 
                    e
            );
        }
    }
}
