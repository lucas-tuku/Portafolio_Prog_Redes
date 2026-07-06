package ar.edu.ottokrause.datapipe.infra;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Persistencia eficiente en disco usando FileChannel y ByteBuffer.
 * Diseñado por el Rol A (Especialista NIO/NIO.2).
 */
public class NioWriter {

    /**
     * Escribe datos en el canal a través de un ByteBuffer intermedio.
     * Realiza la coreografía estricta de punteros (flip/compact).
     * 
     * @param data bytes a escribir.
     * @param channel canal de archivo destino.
     * @param buffer buffer administrado.
     * @throws IOException si ocurre un error de escritura.
     */
    public static void writeData(byte[] data, FileChannel channel, ByteBuffer buffer) throws IOException {
        int offset = 0;
        while (offset < data.length) {
            int remainingSpace = buffer.remaining();
            int bytesToWrite = Math.min(remainingSpace, data.length - offset);

            // Cargar datos en el buffer (Modo Escritura)
            buffer.put(data, offset, bytesToWrite);
            offset += bytesToWrite;

            // Si el buffer se llenó, lo vaciamos al canal
            if (!buffer.hasRemaining()) {
                flush(channel, buffer);
            }
        }
    }

    /**
     * Vuelca los datos acumulados en el buffer al FileChannel físico.
     */
    public static void flush(FileChannel channel, ByteBuffer buffer) throws IOException {
        // --- Fase 1: Antes de flip() (Llenado completo) ---
        logBufferState("Fase 1 (Antes de .flip() - Lleno)", buffer);

        // --- Fase 2: .flip() (Pasar a modo Lectura/Canal) ---
        buffer.flip();
        logBufferState("Fase 2 (Después de .flip() - Listo para canal)", buffer);

        // Escribir todo el contenido disponible al canal físico
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        // --- Fase 3: .compact() (Retener bytes remanentes y volver a modo Escritura) ---
        buffer.compact();
        logBufferState("Fase 3 (Después de .compact() - Listo para escribir)", buffer);
    }

    /**
     * Realiza el vaciado final (EOF) y limpia el buffer.
     */
    public static void forceFinalFlush(FileChannel channel, ByteBuffer buffer) throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        buffer.clear();
        logBufferState("Cierre (Después de .clear() - Vacío)", buffer);
    }

    /**
     * Imprime el estado del buffer para la bitácora del entregable del portafolio.
     */
    private static void logBufferState(String fase, ByteBuffer buffer) {
        System.out.printf("[Rol A][NIO] Punteros de Buffer (%s) -> Position: %d, Limit: %d, Capacity: %d%n",
                fase, buffer.position(), buffer.limit(), buffer.capacity());
    }
}
