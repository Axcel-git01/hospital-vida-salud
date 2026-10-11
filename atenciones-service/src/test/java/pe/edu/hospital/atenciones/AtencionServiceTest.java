package pe.edu.hospital.atenciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.hospital.common.ConflictoException;
import pe.edu.hospital.common.EventoNegocio;
import pe.edu.hospital.common.EventoPublisher;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ReglaNegocioException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AtencionServiceTest {

    @Mock
    AtencionRepository repo;
    @Mock
    CitasClient citas;
    @Mock
    EventoPublisher eventos;
    @InjectMocks
    AtencionService service;

    private final AtencionRequest solicitud = new AtencionRequest(3L, "Consulta externa");

    @Test
    void registraAtencionSobreCitaConfirmada() {
        when(citas.obtener(3L)).thenReturn(Mono.just(new CitaDto(3L, 1L, "CONFIRMADA")));
        when(repo.existsByCitaIdAndEstadoNot(3L, EstadoAtencion.ANULADA)).thenReturn(Mono.just(false));
        when(repo.save(any(Atencion.class))).thenAnswer(inv -> {
            Atencion a = inv.getArgument(0);
            return Mono.just(new Atencion(9L, a.codigo(), a.pacienteId(), a.citaId(), a.fechaAtencion(),
                    a.area(), a.estado()));
        });
        when(eventos.publicar(eq("atencion.registrada"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.crear(solicitud))
                .assertNext(a -> {
                    assertEquals(EstadoAtencion.REGISTRADA, a.estado());
                    assertEquals(1L, a.pacienteId());
                })
                .verifyComplete();
    }

    @Test
    void rechazaAtencionSiLaCitaNoEstaConfirmada() {
        when(citas.obtener(3L)).thenReturn(Mono.just(new CitaDto(3L, 1L, "PENDIENTE")));

        StepVerifier.create(service.crear(solicitud)).verifyError(ReglaNegocioException.class);
        verify(repo, never()).save(any(Atencion.class));
    }

    @Test
    void rechazaAtencionSiLaCitaNoExiste() {
        when(citas.obtener(3L)).thenReturn(Mono.error(new RecursoNoEncontradoException("Cita 3 no existe")));

        StepVerifier.create(service.crear(solicitud)).verifyError(RecursoNoEncontradoException.class);
    }

    @Test
    void rechazaSegundaAtencionParaLaMismaCita() {
        when(citas.obtener(3L)).thenReturn(Mono.just(new CitaDto(3L, 1L, "CONFIRMADA")));
        when(repo.existsByCitaIdAndEstadoNot(3L, EstadoAtencion.ANULADA)).thenReturn(Mono.just(true));

        StepVerifier.create(service.crear(solicitud)).verifyError(ConflictoException.class);
        verify(repo, never()).save(any(Atencion.class));
    }

    @Test
    void atencionFinalizadaNoPuedeAnularse() {
        when(repo.findById(9L)).thenReturn(Mono.just(
                new Atencion(9L, "ATN-1", 1L, 3L, LocalDateTime.now(), "Consulta externa", EstadoAtencion.FINALIZADA)));

        StepVerifier.create(service.cambiarEstado(9L, EstadoAtencion.ANULADA))
                .verifyError(ReglaNegocioException.class);
    }
}
