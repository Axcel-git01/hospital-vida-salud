package pe.edu.hospital.citas;

/** Estados de la cita y transiciones permitidas. */
public enum EstadoCita {
    PENDIENTE, CONFIRMADA, ATENDIDA, CANCELADA, AUSENTE;

    public boolean puedeTransicionarA(EstadoCita nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == CONFIRMADA || nuevo == CANCELADA;
            case CONFIRMADA -> nuevo == ATENDIDA || nuevo == AUSENTE || nuevo == CANCELADA;
            case ATENDIDA, CANCELADA, AUSENTE -> false;
        };
    }

    /** Solo las citas aún vigentes (PENDIENTE o CONFIRMADA) pueden cambiar de fecha/hora. */
    public boolean admiteReprogramacion() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
