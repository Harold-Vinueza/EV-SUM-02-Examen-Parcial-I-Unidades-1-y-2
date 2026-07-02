package ec.edu.uteq.mercadoquevedo.pedidos.dto;

import ec.edu.uteq.mercadoquevedo.pedidos.model.Pedido;
import ec.edu.uteq.mercadoquevedo.pedidos.model.PedidoItem;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String estado,
        BigDecimal subtotal,
        OffsetDateTime creadoEn,
        List<ItemPedidoResponse> items
) {
    public static PedidoResponse from(Pedido p) {
        List<ItemPedidoResponse> items = p.getItems().stream()
                .map(PedidoResponse::itemFrom)
                .toList();
        return new PedidoResponse(p.getId(), p.getEstado(), p.getSubtotal(), p.getCreadoEn(), items);
    }

    private static ItemPedidoResponse itemFrom(PedidoItem i) {
        return new ItemPedidoResponse(i.getProductoId(), i.getCantidad(), i.getPrecioUnitario(), i.getSubtotalItem());
    }
}
