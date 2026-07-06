package ar.edu.ottokrause.datapipe.exceptions;

import java.io.IOException;

/**
 * Excepción que captura fallas del Pipe de comunicación en RAM.
 * Diseñada por el Rol C (Líder de Resiliencia).
 */
public class CanalRotoException extends IOException {
    private final long bytesPerdidos;

    public CanalRotoException(String mensaje, long bytesPerdidos) {
        super(mensaje);
        this.bytesPerdidos = bytesPerdidos;
    }

    public CanalRotoException(String mensaje, long bytesPerdidos, Throwable causa) {
        super(mensaje, causa);
        this.bytesPerdidos = bytesPerdidos;
    }

    public long getBytesPerdidos() {
        return bytesPerdidos;
    }

    @Override
    public String toString() {
        return super.toString() + " | Bytes huérfanos/perdidos: " + bytesPerdidos;
    }
}
