package ec.edu.uteq.mercadoquevedo.catalogo.controller;

import ec.edu.uteq.mercadoquevedo.catalogo.dto.ProductoRequest;
import ec.edu.uteq.mercadoquevedo.catalogo.dto.ProductoResponse;
import ec.edu.uteq.mercadoquevedo.catalogo.model.Producto;
import ec.edu.uteq.mercadoquevedo.catalogo.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Recurso REST /api/v1/productos — Nivel 2 del modelo de madurez de Richardson:
 * URIs con sustantivos en plural, versionadas, sin verbos; verbos HTTP y
 * códigos de estado semánticamente correctos.
 */
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return service.listar().stream().map(ProductoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return ProductoResponse.from(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest datos) {
        Producto creado = service.crear(datos);
        // 201 Created + Location: convención REST para señalar dónde vive el nuevo recurso
        return ResponseEntity
                .created(URI.create("/api/v1/productos/" + creado.getId()))
                .body(ProductoResponse.from(creado));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        // PUT = reemplazo completo del recurso -> idempotente por diseño
        return ProductoResponse.from(service.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
