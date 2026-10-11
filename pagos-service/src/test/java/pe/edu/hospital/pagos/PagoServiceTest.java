package pe.edu.hospital.pagos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
class PagoServiceTest {

    @Mock
    PagoRepository repo;
    @Mock
    EventoPublisher eventos;
    @InjectMocks
    PagoService service;

    private PagoRequest solicitud(String monto) {
        return new PagoRequest(1L, "Consulta de ejemplo", new BigDecimal(monto), "TARJETA", "OP-0001");
    }

    @Test
    void rechazaMontoCero() {
        StepVerifier.create(service.registrar(solicitud("0"))).verifyError(ReglaNegocioException.class);
        verifyNoInteractions(repo, eventos);
    }

    @Test
    void rechazaMontoNegativo() {
        StepVerifier.create(service.registrar(solicitud("-15.50"))).verifyError(ReglaNegocioException.class);
        verifyNoInteractions(repo, eventos);
    }

    @Test
    void rechazaNumeroDeOperacionDuplicado() {
        when(repo.existsByNumeroOperacion("OP-0001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(solicitud("50"))).verifyError(ConflictoException.class);
        verify(repo, never()).save(any(Pago.class));
    }

    @Test
    void registraPagoValidoYPublicaEvento() {
        when(repo.existsByNumeroOperacion("OP-0001")).thenReturn(Mono.just(false));
        when(repo.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            return Mono.just(new Pago(7L, p.pacienteId(), p.concepto(), p.monto(), p.fechaPago(),
                    p.medioPago(), p.numeroOperacion(), p.estado()));
        });
        when(eventos.publicar(eq("pago.registrado"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(solicitud("50")))
                .assertNext(p -> assertEquals("REGISTRADO", p.estado()))
                .verifyComplete();
        verify(eventos).publicar(eq("pago.registrado"), any(EventoNegocio.class));
    }

    @Test
    void pagoInexistenteDevuelveNoEncontrado() {
        when(repo.findById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtener(5L)).verifyError(RecursoNoEncontradoException.class);
    }
}
