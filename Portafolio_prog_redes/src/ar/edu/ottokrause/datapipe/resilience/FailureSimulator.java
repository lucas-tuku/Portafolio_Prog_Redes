package ar.edu.ottokrause.datapipe.resilience;

import ar.edu.ottokrause.datapipe.infra.DirectoryManager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.nio.file.Path;

/**
 * Simulador de fallas y pruebas de estrés forense.
 * Diseñado por el Rol C (Líder de Resiliencia y QA).
 */
public class FailureSimulator {

    /**
     * Simula la revocación de permisos de escritura de una carpeta en caliente.
     */
    public static void simulateFolderLockout(Path folderPath) {
        System.out.println("[Rol C][Resiliencia] Simulación de falla en caliente: Bloqueando escritura en " + folderPath.getFileName());
        DirectoryManager.setDirectoryBlocked(folderPath, true);
    }

    /**
     * Restaura el acceso a la carpeta.
     */
    public static void restoreFolderAccess(Path folderPath) {
        System.out.println("[Rol C][Resiliencia] Restaurando permisos en " + folderPath.getFileName());
        DirectoryManager.setDirectoryBlocked(folderPath, false);
    }

    /**
     * Interrumpe un hilo de ejecución en caliente para verificar el apagado armonioso.
     */
    public static void simulateThreadInterrupt(Thread targetThread) {
        System.out.println("[Rol C][Resiliencia] Simulación de interrupción física (señal de hardware) en el hilo: " + targetThread.getName());
        targetThread.interrupt();
    }

    /**
     * Colapsa el Pipe cerrando abruptamente el flujo del lector en caliente.
     * Esto provocará que el emisor reciba un 'IOException: Pipe broken'.
     */
    public static void simulatePipeCrash(PipedInputStream readerPipe) {
        System.out.println("[Rol C][Resiliencia] Simulación de falla catastrófica de canal: Cerrando PipedInputStream prematuramente...");
        try {
            readerPipe.close();
            System.out.println("[Rol C][Resiliencia] Lector del Pipe cerrado. El emisor debería colapsar y desviar datos.");
        } catch (IOException e) {
            System.err.println("[Rol C][Resiliencia] Error al cerrar el pipe para simulación: " + e.getMessage());
        }
    }
}
