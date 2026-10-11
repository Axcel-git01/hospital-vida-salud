package pe.edu.hospital.laboratorio;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/laboratorio")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SolicitudLaboratorio> crear(@Valid @RequestBody SolicitudRequest request) {
        return service.crear(request);
    }

    @GetMapping("/{id}")
    public Mono<SolicitudLaboratorio> obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping
    public Flux<SolicitudLaboratorio> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }

    @PatchMapping("/{id}/estado")
    public Mono<SolicitudLaboratorio> cambiarEstado(@PathVariable Long id,
                                                    @Valid @RequestBody CambioEstadoRequest request) {
        return service.cambiarEstado(id, request.estado(), request.resultadoFicticio());
    }
}
