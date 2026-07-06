package ar.edu.ottokrause.datapipe.exceptions;

/**
 * Excepción específica cuando se deniegan permisos a una carpeta operativa.
 * Diseñada por el Rol C (Líder de Resiliencia).
 */
public class AccesoCarpetaDenegadoException extends FallaInfraestructuraException {
    private final String permisosRequeridos;

    public AccesoCarpetaDenegadoException(String mensaje, String rutaAfectada, String permisosRequeridos) {
        super(mensaje, rutaAfectada);
        this.permisosRequeridos = permisosRequeridos;
    }

    public AccesoCarpetaDenegadoException(String mensaje, String rutaAfectada, String permisosRequeridos, Throwable causa) {
        super(mensaje, rutaAfectada, causa);
        this.permisosRequeridos = permisosRequeridos;
    }

    public String getPermisosRequeridos() {
        return permisosRequeridos;
    }

    @Override
    public String toString() {
        return super.toString() + " | Permisos requeridos: " + permisosRequeridos;
    }
}
