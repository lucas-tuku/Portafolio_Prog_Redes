package ar.edu.ottokrause.datapipe.infra;

import ar.edu.ottokrause.datapipe.exceptions.AccesoCarpetaDenegadoException;
import ar.edu.ottokrause.datapipe.exceptions.FallaInfraestructuraException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de directorios del sistema usando NIO.2.
 * Diseñado por el Rol A (Especialista NIO.2).
 */
public class DirectoryManager {
    private static final ConcurrentHashMap<Path, Boolean> blockedDirectories = new ConcurrentHashMap<>();

    /**
     * Inicializa y valida las carpetas operativas del sistema.
     */
    public static void initDirectories(Path basePath) throws FallaInfraestructuraException {
        Path ingesta = basePath.resolve("data/ingesta");
        Path procesados = basePath.resolve("data/procesados");
        Path errores = basePath.resolve("data/errores");

        createAndSecureDirectory(ingesta);
        createAndSecureDirectory(procesados);
        createAndSecureDirectory(errores);
    }

    /**
     * Crea un directorio y valida o establece permisos POSIX (con fallback en Windows).
     */
    private static void createAndSecureDirectory(Path dir) throws FallaInfraestructuraException {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // Verificar si el sistema soporta atributos POSIX
            boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
            if (isPosix) {
                // Permisos POSIX: Propietario (Lectura/Escritura/Ejecución), Grupo/Otros (Lectura/Ejecución)
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                Files.setPosixFilePermissions(dir, perms);
                System.out.println("[Rol A][NIO.2] Permisos POSIX 'rwxr-xr-x' aplicados a: " + dir.getFileName());
            } else {
                // Fallback para Windows u otros sistemas no-POSIX
                dir.toFile().setReadable(true, false);
                dir.toFile().setWritable(true, false);
                dir.toFile().setExecutable(true, false);
                System.out.println("[Rol A][NIO.2] Fallback de permisos Windows aplicado a: " + dir.getFileName());
            }
            
            // Validar acceso básico de escritura y lectura
            validateDirectoryPermissions(dir);

        } catch (IOException e) {
            throw new FallaInfraestructuraException("Error catastrófico inicializando carpeta operativa", dir.toString(), e);
        }
    }

    /**
     * Valida si un directorio tiene los permisos necesarios y no está bloqueado.
     */
    public static void validateDirectoryPermissions(Path dir) throws AccesoCarpetaDenegadoException {
        if (blockedDirectories.getOrDefault(dir, Boolean.FALSE)) {
            throw new AccesoCarpetaDenegadoException(
                "Acceso denegado: El directorio está bloqueado de forma simulada por el simulador de fallas.", 
                dir.toString(), 
                "WRITE"
            );
        }

        if (!Files.exists(dir)) {
            throw new AccesoCarpetaDenegadoException(
                "Acceso denegado: El directorio no existe.", 
                dir.toString(), 
                "EXISTS"
            );
        }

        if (!Files.isReadable(dir)) {
            throw new AccesoCarpetaDenegadoException(
                "Acceso denegado: No hay permisos de lectura.", 
                dir.toString(), 
                "READ"
            );
        }

        if (!Files.isWritable(dir)) {
            throw new AccesoCarpetaDenegadoException(
                "Acceso denegado: No hay permisos de escritura.", 
                dir.toString(), 
                "WRITE"
            );
        }
    }

    /**
     * Simula la revocación de permisos bloqueando un directorio en memoria.
     * Útil para prubas con el Rol C.
     */
    public static void setDirectoryBlocked(Path dir, boolean block) {
        if (block) {
            blockedDirectories.put(dir, Boolean.TRUE);
            // Intentar también deshabilitar permisos a nivel de File de Java
            dir.toFile().setWritable(false, false);
            System.out.println("[INFRA] Directorio bloqueado para simulación de estrés: " + dir);
        } else {
            blockedDirectories.remove(dir);
            dir.toFile().setWritable(true, false);
            System.out.println("[INFRA] Directorio desbloqueado: " + dir);
        }
    }
}
