package ec.edu.uteq.mercadoquevedo.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(

        @NotNull(message = "productoId es obligatorio")
        @Positive(message = "productoId debe ser positivo")
        Long productoId,

        @NotNull(message = "la cantidad es obligatoria")
        @Positive(message = "la cantidad debe ser mayor a 0")
        Integer cantidad
) {
}
