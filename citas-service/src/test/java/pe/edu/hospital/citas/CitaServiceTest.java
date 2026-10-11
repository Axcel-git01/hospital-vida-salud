package pe.edu.hospital.citas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CitaServiceTest {

    @Mock
    CitaRepository repo;
    @Mock
    PacientesClient pacientes;
    @Mock
    EventoPublisher eventos;
    @InjectMocks
    CitaService service;

    private CitaRequest solicitud() {
        return new CitaRequest(1L, "Medicina General", LocalDateTime.now().plusDays(2), "PROF-001", "Control");
    }

    private Cita cita(EstadoCita estado) {
        LocalDateTime ahora = LocalDateTime.now();
        return new Cita(5L, "CIT-TEST0001", 1L, "Medicina General", ahora.plusDays(2), "PROF-001", "Control",
                estado, ahora, ahora);
    }

    @Test
    void creaCitaParaPacienteValidoYPublicaEvento() {
        when(pacientes.obtener(1L)).thenReturn(Mono.just(new PacienteDto(1L, "Ana", "Prueba", "ACTIVO")));
        when(repo.existsByProfesionalCodigoAndFechaHoraAndEstadoNot(anyString(), any(LocalDateTime.class),
                eq(EstadoCita.CANCELADA))).thenReturn(Mono.just(false));
        when(repo.existsByPacienteIdAndFechaHoraAndEstadoNot(anyLong(), any(LocalDateTime.class),
                eq(EstadoCita.CANCELADA))).thenReturn(Mono.just(false));
        when(repo.save(any(Cita.class))).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            return Mono.just(new Cita(5L, c.codigo(), c.pacienteId(), c.especialidad(), c.fechaHora(),
                    c.profesionalCodigo(), c.motivo(), c.estado(), c.fechaCreacion(), c.fechaActualizacion()));
        });
        when(eventos.publicar(eq("cita.pendiente"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.crear(solicitud()))
                .assertNext(c -> {
                    assertEquals(EstadoCita.PENDIENTE, c.estado());
                    assertEquals(5L, c.id());
                })
                .verifyComplete();
        verify(eventos).publicar(eq("cita.pendiente"), any(EventoNegocio.class));
    }

    @Test
    void noCreaCitaSiElPacienteNoExiste() {
        when(pacientes.obtener(1L)).thenReturn(Mono.error(new RecursoNoEncontradoException("Paciente 1 no existe")));

        StepVerifier.create(service.crear(solicitud())).verifyError(RecursoNoEncontradoException.class);
        verify(repo, never()).save(any(Cita.class));
    }

    @Test
    void noCreaCitaSiElPacienteEstaInactivo() {
        when(pacientes.obtener(1L)).thenReturn(Mono.just(new PacienteDto(1L, "Ana", "Prueba", "INACTIVO")));

        StepVerifier.create(service.crear(solicitud())).verifyError(ReglaNegocioException.class);
        verify(repo, never()).save(any(Cita.class));
    }

    @Test
    void confirmarCitaPendientePublicaEventoCitaConfirmada() {
        when(repo.findById(5L)).thenReturn(Mono.just(cita(EstadoCita.PENDIENTE)));
        when(repo.save(any(Cita.class))).thenAnswer(inv -> Mono.just((Cita) inv.getArgument(0)));
        when(eventos.publicar(eq("cita.confirmada"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.cambiarEstado(5L, EstadoCita.CONFIRMADA))
                .assertNext(c -> assertEquals(EstadoCita.CONFIRMADA, c.estado()))
                .verifyComplete();
        verify(eventos).publicar(eq("cita.confirmada"), any(EventoNegocio.class));
    }

    @Test
    void rechazaTransicionNoPermitida() {
        when(repo.findById(5L)).thenReturn(Mono.just(cita(EstadoCita.CANCELADA)));

        StepVerifier.create(service.cambiarEstado(5L, EstadoCita.CONFIRMADA))
                .verifyError(ReglaNegocioException.class);
        verify(repo, never()).save(any(Cita.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void citaInexistenteDevuelveNoEncontrado() {
        when(repo.findById(77L)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtener(77L)).verifyError(RecursoNoEncontradoException.class);
    }

    @Test
    void reprogramaCitaVigenteConservaEstadoYPublicaEvento() {
        LocalDateTime nueva = LocalDateTime.now().plusDays(5);
        when(repo.findById(5L)).thenReturn(Mono.just(cita(EstadoCita.CONFIRMADA)));
        when(repo.existsByProfesionalCodigoAndFechaHoraAndEstadoNotAndIdNot(
                "PROF-001", nueva, EstadoCita.CANCELADA, 5L)).thenReturn(Mono.just(false));
        when(repo.existsByPacienteIdAndFechaHoraAndEstadoNotAndIdNot(
                1L, nueva, EstadoCita.CANCELADA, 5L)).thenReturn(Mono.just(false));
        when(repo.save(any(Cita.class))).thenAnswer(inv -> Mono.just((Cita) inv.getArgument(0)));
        when(eventos.publicar(eq("cita.reprogramada"), any(EventoNegocio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.reprogramar(5L, new ReprogramarRequest(nueva, null)))
                .assertNext(c -> {
                    assertEquals(nueva, c.fechaHora());
                    assertEquals(EstadoCita.CONFIRMADA, c.estado());
                    assertEquals("PROF-001", c.profesionalCodigo());
                })
                .verifyComplete();

        ArgumentCaptor<EventoNegocio> captor = ArgumentCaptor.forClass(EventoNegocio.class);
        verify(eventos).publicar(eq("cita.reprogramada"), captor.capture());
        assertEquals("REPROGRAMADA", captor.getValue().detalle());
        assertEquals("CONFIRMADA", captor.getValue().estadoNuevo());
    }

    @Test
    void noReprogramaCitaCancelada() {
        when(repo.findById(5L)).thenReturn(Mono.just(cita(EstadoCita.CANCELADA)));

        StepVerifier.create(service.reprogramar(5L, new ReprogramarRequest(LocalDateTime.now().plusDays(5), null)))
                .verifyError(ReglaNegocioException.class);
        verify(repo, never()).save(any(Cita.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void noReprogramaSiElProfesionalYaTieneCitaEnEseHorario() {
        LocalDateTime nueva = LocalDateTime.now().plusDays(5);
        when(repo.findById(5L)).thenReturn(Mono.just(cita(EstadoCita.PENDIENTE)));
        when(repo.existsByProfesionalCodigoAndFechaHoraAndEstadoNotAndIdNot(
                "PROF-001", nueva, EstadoCita.CANCELADA, 5L)).thenReturn(Mono.just(true));

        StepVerifier.create(service.reprogramar(5L, new ReprogramarRequest(nueva, null)))
                .verifyError(ConflictoException.class);
        verify(repo, never()).save(any(Cita.class));
        verifyNoInteractions(eventos);
    }

    @Test
    void noReprogramaSiNoHayCambioReal() {
        Cita actual = cita(EstadoCita.PENDIENTE);
        when(repo.findById(5L)).thenReturn(Mono.just(actual));

        StepVerifier.create(service.reprogramar(5L, new ReprogramarRequest(actual.fechaHora(), null)))
                .verifyError(ReglaNegocioException.class);
        verify(repo, never()).save(any(Cita.class));
    }
}
