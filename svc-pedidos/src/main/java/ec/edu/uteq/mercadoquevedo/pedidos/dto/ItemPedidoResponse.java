package ec.edu.uteq.mercadoquevedo.pedidos.dto;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long productoId,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotalItem
) {
}
