package ec.edu.uteq.mercadoquevedo.pedidos.controller;

import ec.edu.uteq.mercadoquevedo.pedidos.dto.PedidoRequest;
import ec.edu.uteq.mercadoquevedo.pedidos.dto.PedidoResponse;
import ec.edu.uteq.mercadoquevedo.pedidos.model.Pedido;
import ec.edu.uteq.mercadoquevedo.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return service.listar().stream().map(PedidoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return PedidoResponse.from(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest datos) {
        Pedido creado = service.crear(datos);
        return ResponseEntity
                .created(URI.create("/api/v1/pedidos/" + creado.getId()))
                .body(PedidoResponse.from(creado));
    }
}
