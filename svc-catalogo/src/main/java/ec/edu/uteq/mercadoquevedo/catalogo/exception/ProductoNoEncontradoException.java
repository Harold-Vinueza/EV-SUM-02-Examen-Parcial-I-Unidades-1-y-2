package ec.edu.uteq.mercadoquevedo.catalogo.exception;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException(Long productoId) {
        super("Producto " + productoId + " no existe");
    }
}
