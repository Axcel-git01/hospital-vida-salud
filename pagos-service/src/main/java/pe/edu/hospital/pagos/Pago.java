package pe.edu.hospital.pagos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("pago")
public record Pago(
        @Id Long id,
        Long pacienteId,
        String concepto,
        BigDecimal monto,
        LocalDateTime fechaPago,
        String medioPago,
        String numeroOperacion,
        String estado) {
}
