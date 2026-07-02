package ec.edu.uteq.mercadoquevedo.catalogo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Usado por el HEALTHCHECK de Docker Compose. */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "svc-catalogo");
    }
}
