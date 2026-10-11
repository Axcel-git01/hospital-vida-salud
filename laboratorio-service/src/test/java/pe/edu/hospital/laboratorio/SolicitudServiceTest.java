package pe.edu.hospital.laboratorio;

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
import pe.edu.hospital.common.EventoNegocio;
import pe.edu.hospital.common.EventoPublisher;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ReglaNegocioException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    SolicitudRepository repo;
    @Mock
    AtencionesClient atenciones;
    @Mock
    EventoPublisher eventos;
    @InjectMocks
    SolicitudService service;

    private SolicitudLaboratorio solicitud(EstadoSolicitud estado) {
        LocalDateTime ahora = LocalDateTime.now();
        return new SolicitudLaboratorio(8L, "LAB-1", 1L, 9L, "Examen de demostración", estado, null, ahora, ahora);
    }

    @Test
    void creaSolicitudSobreAtencionExistente() {
        when(atenciones.obtener(9L)).thenReturn(Mono.just(new AtencionDto(9L, 1L, "REGISTRADA")));
        when(repo.save(any(SolicitudLaboratorio.class))).thenAnswer(inv -> {
            SolicitudLaboratorio s = inv.getArgument(0);
            return Mono.just(new SolicitudLaboratorio(8L, s.codigo(), s.pacienteId(), s.atencionId(),
                    s.tipoExamen(), s.estado(), null, s.fechaSolicitud(), s.fechaActualizacion()));
        });
        when(eventos.publicar(eq("laboratorio.solicitado"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.crear(new SolicitudRequest(9L, "Examen de demostración")))
                .assertNext(s -> assertEquals(EstadoSolicitud.SOLICITADO, s.estado()))
                .verifyComplete();
    }

    @Test
    void rechazaSolicitudSiLaAtencionNoExiste() {
        when(atenciones.obtener(9L)).thenReturn(Mono.error(new RecursoNoEncontradoException("Atención 9 no existe")));

        StepVerifier.create(service.crear(new SolicitudRequest(9L, "X"))).verifyError(RecursoNoEncontradoException.class);
        verify(repo, never()).save(any(SolicitudLaboratorio.class));
    }

    @Test
    void rechazaSolicitudSiLaAtencionEstaAnulada() {
        when(atenciones.obtener(9L)).thenReturn(Mono.just(new AtencionDto(9L, 1L, "ANULADA")));

        StepVerifier.create(service.crear(new SolicitudRequest(9L, "X"))).verifyError(ReglaNegocioException.class);
    }

    @Test
    void completarExigeResultadoFicticio() {
        when(repo.findById(8L)).thenReturn(Mono.just(solicitud(EstadoSolicitud.EN_PROCESO)));

        StepVerifier.create(service.cambiarEstado(8L, EstadoSolicitud.COMPLETADO, " "))
                .verifyError(ReglaNegocioException.class);
    }

    @Test
    void solicitudCompletadaNoPuedeCancelarse() {
        when(repo.findById(8L)).thenReturn(Mono.just(solicitud(EstadoSolicitud.COMPLETADO)));

        StepVerifier.create(service.cambiarEstado(8L, EstadoSolicitud.CANCELADO, null))
                .verifyError(ReglaNegocioException.class);
    }

    @Test
    void flujoValidoEnProcesoACompletado() {
        when(repo.findById(8L)).thenReturn(Mono.just(solicitud(EstadoSolicitud.EN_PROCESO)));
        when(repo.save(any(SolicitudLaboratorio.class))).thenAnswer(inv -> Mono.just((SolicitudLaboratorio) inv.getArgument(0)));
        when(eventos.publicar(eq("laboratorio.completado"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(8L, EstadoSolicitud.COMPLETADO, "Valor de prueba"))
                .assertNext(s -> assertEquals("Valor de prueba", s.resultadoFicticio()))
                .verifyComplete();
    }
}
