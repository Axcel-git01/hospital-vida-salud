-- Solo PostgreSQL (los tests usan H2, que no soporta indices parciales).
-- Garantiza a nivel de BD lo que CitaService valida por consulta: aunque dos solicitudes
-- lleguen a la vez, solo una puede ocupar el mismo horario. Las citas CANCELADAS liberan el cupo.
CREATE UNIQUE INDEX IF NOT EXISTS uk_cita_profesional_horario
    ON cita(profesional_codigo, fecha_hora) WHERE estado <> 'CANCELADA';
CREATE UNIQUE INDEX IF NOT EXISTS uk_cita_paciente_horario
    ON cita(paciente_id, fecha_hora) WHERE estado <> 'CANCELADA';
