package ec.edu.uteq.mercadoquevedo.pedidos.client;

import ec.edu.uteq.mercadoquevedo.pedidos.exception.CatalogoNoDisponibleException;
import ec.edu.uteq.mercadoquevedo.pedidos.exception.ProductoInexistenteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia svc-catalogo. Encapsula la comunicación síncrona
 * inter-servicio (REST) exigida en la Parte C del examen: timeout explícito
 * y traducción de fallos de red/servidor a excepciones de dominio.
 */
@Component
public class CatalogoClient {

    private final RestClient restClient;
    private final long timeoutSeconds;

    public CatalogoClient(
            @Value("${catalogo.base-url}") String baseUrl,
            @Value("${catalogo.timeout-seconds}") long timeoutSeconds
    ) {
        this.timeoutSeconds = timeoutSeconds;

        // Timeout explícito y corto: si el catálogo no responde a tiempo, no
        // queremos bloquear el hilo de svc-pedidos indefinidamente (evita
        // cascading failure). En milisegundos para SimpleClientHttpRequestFactory.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) (timeoutSeconds * 1000);
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Devuelve el producto o lanza:
     * - ProductoInexistenteException si el catálogo respondió 404
     * - CatalogoNoDisponibleException si hubo timeout, conexión rechazada
     *   u otro error de red / error 5xx del catálogo
     */
    public ProductoDTO obtenerProducto(Long productoId) {
        try {
            return restClient.get()
                    .uri("/api/v1/productos/{id}", productoId)
                    .retrieve()
                    .body(ProductoDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductoInexistenteException(productoId);
        } catch (HttpServerErrorException e) {
            throw new CatalogoNoDisponibleException(
                    "svc-catalogo respondió con error del servidor: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            // Cubre tanto timeout (SocketTimeoutException) como conexión
            // rechazada (ConnectException) — svc-catalogo caído o colgado.
            throw new CatalogoNoDisponibleException(
                    "svc-catalogo no respondió en " + timeoutSeconds + "s: " + e.getMessage());
        }
    }
}
