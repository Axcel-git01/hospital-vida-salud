package pe.edu.hospital.atenciones;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/atenciones")
public class AtencionController {

    private final AtencionService service;

    public AtencionController(AtencionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atencion> crear(@Valid @RequestBody AtencionRequest request) {
        return service.crear(request);
    }

    @GetMapping("/{id}")
    public Mono<Atencion> obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping
    public Flux<Atencion> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    @PatchMapping("/{id}/estado")
    public Mono<Atencion> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}
