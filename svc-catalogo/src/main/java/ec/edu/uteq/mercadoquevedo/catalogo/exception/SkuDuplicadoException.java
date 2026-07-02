package ec.edu.uteq.mercadoquevedo.catalogo.exception;

public class SkuDuplicadoException extends RuntimeException {
    public SkuDuplicadoException(String sku) {
        super("El SKU '" + sku + "' ya está registrado");
    }
}
