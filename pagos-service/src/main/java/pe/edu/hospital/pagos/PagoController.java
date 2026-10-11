package pe.edu.hospital.pagos;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Pago> registrar(@Valid @RequestBody PagoRequest request) {
        return service.registrar(request);
    }

    @GetMapping("/{id}")
    public Mono<Pago> obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping
    public Flux<Pago> listar(@RequestParam(required = false) Long pacienteId) {
        return service.listar(pacienteId);
    }
}
