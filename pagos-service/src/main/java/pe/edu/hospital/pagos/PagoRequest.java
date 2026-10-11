package pe.edu.hospital.pagos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PagoRequest(
        @NotNull Long pacienteId,
        @NotBlank @Size(max = 120) String concepto,
        @NotNull @Positive BigDecimal monto,
        @NotBlank @Size(max = 30) String medioPago,
        @NotBlank @Size(max = 30) String numeroOperacion) {
}
