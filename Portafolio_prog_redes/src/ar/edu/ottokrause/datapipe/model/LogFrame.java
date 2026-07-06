package ar.edu.ottokrause.datapipe.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Contrato de datos y estructura de trama binaria.
 * Diseñado por el Rol B (Especialista en Concurrencia).
 */
public class LogFrame {
    public static final byte MAGIC = 0x5A; // Byte de sincronismo

    private final long timestamp;
    private final byte level;
    private final String message;

    public LogFrame(long timestamp, byte level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getLevelString() {
        switch (level) {
            case 1: return "INFO";
            case 2: return "WARN";
            case 3: return "ERROR";
            default: return "UNKNOWN";
        }
    }

    /**
     * Serializa la trama en un array de bytes.
     */
    public byte[] serialize() {
        byte[] payloadBytes = message.getBytes(StandardCharsets.UTF_8);
        int frameSize = 1 + 8 + 1 + 4 + payloadBytes.length + 8; // magic + ts + lvl + len + payload + checksum
        ByteBuffer buffer = ByteBuffer.allocate(frameSize);
        
        buffer.put(MAGIC);
        buffer.putLong(timestamp);
        buffer.put(level);
        buffer.putInt(payloadBytes.length);
        buffer.put(payloadBytes);
        
        // Calcular checksum sobre los bytes de datos
        long checksum = calculateChecksum(buffer.array(), 0, 1 + 8 + 1 + 4 + payloadBytes.length);
        buffer.putLong(checksum);
        
        return buffer.array();
    }

    /**
     * Deserializa una trama desde un InputStream.
     */
    public static LogFrame deserialize(InputStream in) throws IOException {
        int magicVal = in.read();
        if (magicVal == -1) {
            return null; // Fin de stream limpio
        }
        byte magic = (byte) magicVal;
        if (magic != MAGIC) {
            throw new IOException("Byte de control mágico inválido: " + String.format("0x%02X", magic));
        }

        // Leer cabecera fija: timestamp (8 bytes) + level (1 byte) + payloadLength (4 bytes)
        byte[] header = new byte[8 + 1 + 4];
        readFully(in, header);

        ByteBuffer headerBuf = ByteBuffer.wrap(header);
        long timestamp = headerBuf.getLong();
        byte level = headerBuf.get();
        int payloadLength = headerBuf.getInt();

        if (payloadLength < 0 || payloadLength > 1024 * 1024) { // Límite de seguridad 1MB
            throw new IOException("Longitud de payload inválida o corrupta: " + payloadLength);
        }

        // Leer payload
        byte[] payloadBytes = new byte[payloadLength];
        readFully(in, payloadBytes);

        // Reconstruir bytes para validar checksum
        byte[] rawDataForChecksum = new byte[1 + header.length + payloadBytes.length];
        rawDataForChecksum[0] = MAGIC;
        System.arraycopy(header, 0, rawDataForChecksum, 1, header.length);
        System.arraycopy(payloadBytes, 0, rawDataForChecksum, 1 + header.length, payloadBytes.length);

        // Leer checksum recibido
        byte[] checksumBytes = new byte[8];
        readFully(in, checksumBytes);
        long receivedChecksum = ByteBuffer.wrap(checksumBytes).getLong();

        // Validar checksum
        long calculatedChecksum = calculateChecksum(rawDataForChecksum, 0, rawDataForChecksum.length);
        if (receivedChecksum != calculatedChecksum) {
            throw new IOException("Fallo de validación de checksum. Calculado: " 
                + calculatedChecksum + ", Recibido: " + receivedChecksum + " (Trama corrupta)");
        }

        String msg = new String(payloadBytes, StandardCharsets.UTF_8);
        return new LogFrame(timestamp, level, msg);
    }

    private static void readFully(InputStream in, byte[] b) throws IOException {
        int n = 0;
        while (n < b.length) {
            int count = in.read(b, n, b.length - n);
            if (count < 0) {
                throw new java.io.EOFException("Fin de flujo inesperado al leer estructura de LogFrame");
            }
            n += count;
        }
    }

    private static long calculateChecksum(byte[] data, int offset, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (data[offset + i] & 0xFF);
        }
        return sum;
    }

    @Override
    public String toString() {
        return "[" + getLevelString() + "][" + timestamp + "] " + message;
    }
}
