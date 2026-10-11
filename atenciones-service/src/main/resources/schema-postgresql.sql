-- Solo PostgreSQL. Una cita admite una sola atencion vigente (las ANULADAS no cuentan).
CREATE UNIQUE INDEX IF NOT EXISTS uk_atencion_cita_vigente
    ON atencion(cita_id) WHERE estado <> 'ANULADA';
