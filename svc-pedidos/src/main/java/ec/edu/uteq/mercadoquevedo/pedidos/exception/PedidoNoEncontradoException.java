package ec.edu.uteq.mercadoquevedo.pedidos.exception;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(Long pedidoId) {
        super("Pedido " + pedidoId + " no existe");
    }
}
