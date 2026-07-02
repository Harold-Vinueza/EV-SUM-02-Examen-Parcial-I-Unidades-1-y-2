package ec.edu.uteq.mercadoquevedo.catalogo.exception;

import ec.edu.uteq.mercadoquevedo.catalogo.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones de dominio/framework a los códigos HTTP exigidos por
 * la rúbrica: 404 (no encontrado), 409 (conflicto/SKU duplicado),
 * 400 (JSON malformado) y 422 (validación de dominio fallida).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(ProductoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), "PRODUCTO_NO_ENCONTRADO"));
    }

    @ExceptionHandler(SkuDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarSkuDuplicado(SkuDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), "SKU_DUPLICADO"));
    }

    /**
     * Se dispara cuando @Valid falla una regla de Bean Validation (dominio) —
     * el cuerpo SÍ era JSON válido, pero violó una restricción de negocio.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(detalle, "VALIDACION_FALLIDA"));
    }

    /**
     * Se dispara ANTES de @Valid, cuando Jackson ni siquiera pudo parsear el
     * cuerpo como JSON (sintaxis inválida) -> 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonMalformado(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("El cuerpo de la petición no es JSON válido", "JSON_MALFORMADO"));
    }
}
