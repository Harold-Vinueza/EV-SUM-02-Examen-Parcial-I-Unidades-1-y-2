package ec.edu.uteq.mercadoquevedo.pedidos.client;

import java.math.BigDecimal;

/** Representación mínima del producto tal como lo devuelve svc-catalogo. */
public record ProductoDTO(
        Long id,
        String sku,
        String nombre,
        BigDecimal precio,
        Integer stock
) {
}
