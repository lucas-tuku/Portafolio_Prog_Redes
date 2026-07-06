package ar.edu.ottokrause.datapipe.exceptions;

import java.io.IOException;

/**
 * Excepción base para fallas de infraestructura (directorios, accesos, etc.).
 * Diseñada por el Rol C (Líder de Resiliencia).
 */
public class FallaInfraestructuraException extends IOException {
    private final String rutaAfectada;

    public FallaInfraestructuraException(String mensaje, String rutaAfectada) {
        super(mensaje);
        this.rutaAfectada = rutaAfectada;
    }

    public FallaInfraestructuraException(String mensaje, String rutaAfectada, Throwable causa) {
        super(mensaje, causa);
        this.rutaAfectada = rutaAfectada;
    }

    public String getRutaAfectada() {
        return rutaAfectada;
    }

    @Override
    public String toString() {
        return super.toString() + " | Ruta afectada: " + rutaAfectada;
    }
}
