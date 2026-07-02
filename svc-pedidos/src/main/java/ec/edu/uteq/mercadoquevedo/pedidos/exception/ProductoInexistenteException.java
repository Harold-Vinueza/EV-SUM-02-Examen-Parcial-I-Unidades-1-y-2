package ec.edu.uteq.mercadoquevedo.pedidos.exception;

/** El catálogo respondió correctamente, pero el producto solicitado no existe (404). */
public class ProductoInexistenteException extends RuntimeException {
    public ProductoInexistenteException(Long productoId) {
        super("Producto " + productoId + " no existe en el catálogo");
    }
}
