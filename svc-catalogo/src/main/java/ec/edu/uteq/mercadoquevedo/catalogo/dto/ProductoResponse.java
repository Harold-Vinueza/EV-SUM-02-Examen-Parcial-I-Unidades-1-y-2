package ec.edu.uteq.mercadoquevedo.catalogo.dto;

import ec.edu.uteq.mercadoquevedo.catalogo.model.Producto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        BigDecimal precio,
        Integer stock,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getId(), p.getSku(), p.getNombre(), p.getPrecio(),
                p.getStock(), p.getCreadoEn(), p.getActualizadoEn()
        );
    }
}
