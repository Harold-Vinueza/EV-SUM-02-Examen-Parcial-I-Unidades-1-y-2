package ec.edu.uteq.mercadoquevedo.pedidos.exception;

/**
 * svc-catalogo no respondió a tiempo o no se pudo conectar.
 * Es el gatillo que, en producción, abriría un Circuit Breaker (ver ANALISIS.md).
 */
public class CatalogoNoDisponibleException extends RuntimeException {
    public CatalogoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
