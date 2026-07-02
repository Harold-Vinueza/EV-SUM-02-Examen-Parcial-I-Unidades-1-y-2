package ec.edu.uteq.mercadoquevedo.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de entrada para crear/actualizar un producto (POST y PUT).
 * Bean Validation dispara MethodArgumentNotValidException si algo falla,
 * que el GlobalExceptionHandler mapea a 422 Unprocessable Entity.
 */
public record ProductoRequest(

        @NotBlank(message = "el SKU es obligatorio")
        @Size(max = 64, message = "el SKU no puede exceder 64 caracteres")
        String sku,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 200, message = "el nombre no puede exceder 200 caracteres")
        String nombre,

        @NotNull(message = "el precio es obligatorio")
        @DecimalMin(value = "0.01", message = "el precio debe ser mayor a 0")
        BigDecimal precio,

        @NotNull(message = "el stock es obligatorio")
        @Min(value = 0, message = "el stock no puede ser negativo")
        Integer stock
) {
}
