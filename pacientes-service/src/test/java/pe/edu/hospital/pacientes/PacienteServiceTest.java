package pe.edu.hospital.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.hospital.common.ConflictoException;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ReglaNegocioException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    PacienteRepository repo;
    @InjectMocks
    PacienteService service;

    private PacienteRequest solicitud() {
        return new PacienteRequest("DNI", "10000001", "Ana", "Prueba", LocalDate.of(1990, 1, 1),
                "999000111", "ana@prueba.test", null);
    }

    @Test
    void registraPacienteValido() {
        when(repo.existsByTipoDocumentoAndNumeroDocumento("DNI", "10000001")).thenReturn(Mono.just(false));
        when(repo.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            return Mono.just(new Paciente(1L, p.tipoDocumento(), p.numeroDocumento(), p.nombres(), p.apellidos(),
                    p.fechaNacimiento(), p.telefono(), p.correo(), p.direccion(), p.fechaRegistro(), p.estado()));
        });

        StepVerifier.create(service.registrar(solicitud()))
                .assertNext(p -> {
                    assertEquals(1L, p.id());
                    assertEquals("ACTIVO", p.estado());
                })
                .verifyComplete();
    }

    @Test
    void rechazaPacienteDuplicado() {
        when(repo.existsByTipoDocumentoAndNumeroDocumento("DNI", "10000001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(solicitud())).verifyError(ConflictoException.class);
        verify(repo, never()).save(any(Paciente.class));
    }

    @Test
    void consultaDePacienteInexistenteDevuelveNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtener(99L)).verifyError(RecursoNoEncontradoException.class);
    }

    @Test
    void estadoInvalidoSeRechaza() {
        StepVerifier.create(service.cambiarEstado(1L, "SUSPENDIDO")).verifyError(ReglaNegocioException.class);
    }
}
