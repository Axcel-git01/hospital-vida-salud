package pe.edu.hospital.atenciones;

public enum EstadoAtencion {
    REGISTRADA, FINALIZADA, ANULADA;

    public boolean puedeTransicionarA(EstadoAtencion nuevo) {
        return this == REGISTRADA && (nuevo == FINALIZADA || nuevo == ANULADA);
    }
}
