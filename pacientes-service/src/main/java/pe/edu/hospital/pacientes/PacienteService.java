package pe.edu.hospital.pacientes;

import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import pe.edu.hospital.common.ConflictoException;
import pe.edu.hospital.common.RecursoNoEncontradoException;
import pe.edu.hospital.common.ReglaNegocioException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PacienteService {

    static final String MSG_DUPLICADO = "Ya existe un paciente con ese tipo y número de documento";
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "INACTIVO");

    private final PacienteRepository repo;

    public PacienteService(PacienteRepository repo) {
        this.repo = repo;
    }

    public Mono<Paciente> registrar(PacienteRequest r) {
        return validarDocumentoLibre(r.tipoDocumento(), r.numeroDocumento())
                .then(Mono.defer(() -> repo.save(new Paciente(null, r.tipoDocumento(), r.numeroDocumento(),
                        r.nombres(), r.apellidos(), r.fechaNacimiento(), r.telefono(), r.correo(),
                        r.direccion(), LocalDateTime.now(), "ACTIVO"))))
                .onErrorMap(DuplicateKeyException.class, e -> new ConflictoException(MSG_DUPLICADO));
    }

    public Mono<Paciente> obtener(Long id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNoEncontradoException("Paciente " + id + " no existe")));
    }

    public Flux<Paciente> listar() {
        return repo.findAll();
    }

    public Mono<Paciente> actualizar(Long id, PacienteRequest r) {
        return obtener(id).flatMap(actual -> {
            boolean cambiaDocumento = !actual.tipoDocumento().equals(r.tipoDocumento())
                    || !actual.numeroDocumento().equals(r.numeroDocumento());
            Mono<Void> validacion = cambiaDocumento
                    ? validarDocumentoLibre(r.tipoDocumento(), r.numeroDocumento())
                    : Mono.empty();
            return validacion.then(Mono.defer(() -> repo.save(new Paciente(actual.id(), r.tipoDocumento(),
                    r.numeroDocumento(), r.nombres(), r.apellidos(), r.fechaNacimiento(), r.telefono(),
                    r.correo(), r.direccion(), actual.fechaRegistro(), actual.estado()))));
        }).onErrorMap(DuplicateKeyException.class, e -> new ConflictoException(MSG_DUPLICADO));
    }

    public Mono<Paciente> cambiarEstado(Long id, String estado) {
        if (estado == null || !ESTADOS.contains(estado)) {
            return Mono.error(new ReglaNegocioException("Estado inválido. Valores permitidos: ACTIVO, INACTIVO"));
        }
        return obtener(id).flatMap(p -> repo.save(new Paciente(p.id(), p.tipoDocumento(), p.numeroDocumento(),
                p.nombres(), p.apellidos(), p.fechaNacimiento(), p.telefono(), p.correo(), p.direccion(),
                p.fechaRegistro(), estado)));
    }

    private Mono<Void> validarDocumentoLibre(String tipo, String numero) {
        return repo.existsByTipoDocumentoAndNumeroDocumento(tipo, numero)
                .flatMap(existe -> existe
                        ? Mono.<Void>error(new ConflictoException(MSG_DUPLICADO))
                        : Mono.<Void>empty());
    }
}
