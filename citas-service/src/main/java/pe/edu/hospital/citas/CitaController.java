package pe.edu.hospital.citas;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Cita> crear(@Valid @RequestBody CitaRequest request) {
        return service.crear(request);
    }

    @GetMapping("/{id}")
    public Mono<Cita> obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping
    public Flux<Cita> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    @PatchMapping("/{id}/estado")
    public Mono<Cita> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @PatchMapping("/{id}/reprogramar")
    public Mono<Cita> reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarRequest request) {
        return service.reprogramar(id, request);
    }
}
