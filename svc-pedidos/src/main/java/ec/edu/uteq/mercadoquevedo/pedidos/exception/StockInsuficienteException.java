package ec.edu.uteq.mercadoquevedo.pedidos.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(Long productoId, Integer disponible, Integer solicitado) {
        super("Stock insuficiente para producto " + productoId
                + ": disponible=" + disponible + ", solicitado=" + solicitado);
    }
}
