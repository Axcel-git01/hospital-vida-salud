package pe.edu.hospital.laboratorio;

public enum EstadoSolicitud {
    SOLICITADO, EN_PROCESO, COMPLETADO, CANCELADO;

    public boolean puedeTransicionarA(EstadoSolicitud nuevo) {
        return switch (this) {
            case SOLICITADO -> nuevo == EN_PROCESO || nuevo == CANCELADO;
            case EN_PROCESO -> nuevo == COMPLETADO || nuevo == CANCELADO;
            case COMPLETADO, CANCELADO -> false;
        };
    }
}
