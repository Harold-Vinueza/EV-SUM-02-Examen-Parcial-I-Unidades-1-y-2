package ec.edu.uteq.mercadoquevedo.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PedidoRequest(

        @NotEmpty(message = "el pedido debe tener al menos un item")
        @Valid
        List<ItemPedidoRequest> items
) {
}
