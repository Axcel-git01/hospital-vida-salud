package pe.edu.hospital.auditoria;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<RegistroAuditoria> listar() {
        return service.listar();
    }

    @GetMapping("/{recurso}/{id}")
    public Flux<RegistroAuditoria> historial(@PathVariable String recurso, @PathVariable Long id) {
        return service.historial(recurso, id);
    }
}
