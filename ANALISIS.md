# ANÁLISIS.md — Componente de Análisis y Defensa Técnica (versión Java)

## 1. ¿Cuál restricción REST de Fielding identifica con mayor claridad?

La más clara en el prototipo es la **interfaz uniforme** (*uniform interface*),
en particular sus sub-restricciones de *identificación de recursos* y
*manipulación de recursos a través de representaciones*.

Evidencia en código — `svc-catalogo/.../controller/ProductoController.java`:

```java
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest datos) {
        Producto creado = service.crear(datos);
        return ResponseEntity
                .created(URI.create("/api/v1/productos/" + creado.getId()))
                .body(ProductoResponse.from(creado));
    }
}
```

`{id}` identifica el recurso solo por URI (sin verbos en la ruta), y el cliente
lo manipula únicamente a través de representaciones (`ProductoRequest`/`ProductoResponse`,
records inmutables), nunca accediendo a la entidad JPA directamente — de hecho
`Producto` (la entidad) nunca sale del `service`/`controller` hacia el cliente,
siempre se traduce a `ProductoResponse`. El `ResponseEntity.created(URI...)` es
un mensaje autodescriptivo: le dice al cliente exactamente dónde vive el nuevo
recurso.

También cumplimos **stateless**: ningún `@RestController` guarda sesión ni
contexto entre peticiones — cada llamada de `CatalogoClient.obtenerProducto()`
es una petición HTTP completa e independiente, sin estado compartido entre una
petición y la siguiente.

## 2. ¿En qué nivel del modelo de madurez de Richardson se ubica la API?

**Nivel 2.** Se identifican recursos por URI (`/api/v1/productos`,
`/api/v1/pedidos`), los verbos HTTP (`@GetMapping`, `@PostMapping`, `@PutMapping`,
`@DeleteMapping`) se usan de forma disciplinada, y los códigos de estado son
semánticamente correctos — pero no hay hipermedia (HATEOAS) más allá del header
`Location`, por lo que no alcanza el Nivel 3.

Ejemplo concreto — recurso, verbo y código:

```
POST /api/v1/productos
Body: {"sku":"CAM-001","nombre":"Camisa","precio":18.5,"stock":10}
→ 201 Created, Location: /api/v1/productos/1
```

```java
@ExceptionHandler(SkuDuplicadoException.class)
public ResponseEntity<ErrorResponse> manejarSkuDuplicado(SkuDuplicadoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getMessage(), "SKU_DUPLICADO"));
}
```
```
POST /api/v1/productos   (mismo SKU otra vez)
→ 409 Conflict, {"detalle": "...", "codigo": "SKU_DUPLICADO"}
```

Si usáramos `200 OK` para todo (éxito y error) con un único endpoint tipo
`/api/procesarProducto`, estaríamos en Nivel 0/1. Aquí, en cambio, cada
`@ExceptionHandler` en `GlobalExceptionHandler` mapea una situación de negocio
distinta a un código HTTP distinto.

## 3. Acoplamiento residual entre `svc-catalogo` y `svc-pedidos` (Newman)

Aunque los datos están desacoplados (Database per Service — cada uno con su
propio `application.yml`/`datasource`, sin esquema compartido), existen dos
acoplamientos residuales:

1. **Acoplamiento temporal (runtime coupling)**: `PedidoService.crear()` llama
   a `catalogoClient.obtenerProducto(...)` de forma **síncrona y bloqueante**
   en el hilo de la petición. Si `svc-catalogo` no está disponible,
   `svc-pedidos` tampoco puede completar su propia responsabilidad — ambos
   deben estar vivos *al mismo tiempo*.
2. **Acoplamiento de contrato (interface coupling)**: `svc-pedidos` conoce y
   depende de la forma exacta del JSON que devuelve `svc-catalogo`, deserializado
   directamente en `ProductoDTO` (`client/ProductoDTO.java`). Un cambio de
   esquema no versionado en `svc-catalogo` (p. ej. renombrar `stock`) rompería
   la deserialización de `svc-pedidos` en tiempo de ejecución, sin aviso en
   tiempo de compilación (Jackson simplemente pondría `null` o fallaría).

**Mitigación en producción:**
- Reducir el acoplamiento temporal migrando la verificación de stock a un
  modelo **orientado a eventos** (EDA): `svc-catalogo` publica eventos
  `ProductoActualizado`/`StockReducido` a un broker (Kafka/RabbitMQ vía
  Spring Cloud Stream), y `svc-pedidos` mantiene una réplica de solo lectura
  del stock (patrón CQRS). Esto elimina la dependencia síncrona del camino crítico.
- Reducir el acoplamiento de contrato con **pruebas de contrato** (p. ej. Spring
  Cloud Contract o Pact) sobre el OpenAPI publicado por springdoc, y versionando
  la API (`/api/v1/` → `/api/v2/`) ante cambios incompatibles.
- Mientras se mantenga síncrono, proteger `CatalogoClient` con **Circuit
  Breaker** (Resilience4j, ver pregunta 6) para que el acoplamiento temporal no
  se convierta en fallo en cascada.

## 4. Justificación de microservicios pese al hallazgo de Blinowski et al.

Blinowski et al. (2022) muestran que, **sobre una sola máquina**, un monolito
puede superar en throughput/latencia a su equivalente en microservicios, porque
evita el overhead de red, serialización y el propio arranque de la JVM por
cada servicio. Ese hallazgo es correcto pero mide **rendimiento crudo**, no las
razones reales por las que MercadoQuevedo migraría:

- **Escalado independiente**: en fechas de alta demanda, la carga sobre
  `svc-pedidos` puede crecer mucho más que sobre `svc-catalogo` (o viceversa);
  el monolito obliga a escalar todo el sistema (y toda su huella de memoria JVM)
  en bloque.
- **Aislamiento de fallos**: el propio prototipo lo demuestra — si
  `svc-catalogo` cae, `svc-pedidos` responde `503` controlado (ver `GlobalExceptionHandler`)
  en vez de que *todo* el sistema se caiga con él.
- **Autonomía organizacional**: el enunciado describe una empresa en
  crecimiento con un equipo pequeño hoy, pero el objetivo de la PoC es
  habilitar que, cuando existan varios equipos, cada uno posea un dominio de
  punta a punta (Newman).
- **Costo aceptado conscientemente**: como advierte el propio Newman, la
  complejidad operativa de los microservicios "solo se paga cuando sus
  beneficios se necesitan". Aquí se paga *deliberadamente* como prueba de
  concepto — el requerimiento explícito del examen es demostrar la capacidad
  arquitectónica, no optimizar throughput en una sola máquina.

En resumen: la migración no se justifica por rendimiento bruto (ahí Blinowski
tiene razón, y en Java el costo de arranque de dos JVMs es incluso más notorio
que con Python), sino por escalabilidad diferenciada, tolerancia a fallos y
autonomía de equipos a futuro.

## 5. REST vs. gRPC entre `svc-pedidos` y `svc-catalogo`

**Qué se ganaría:**
- **Rendimiento**: Protocol Buffers (binario) sobre HTTP/2 reduce la latencia y
  el tamaño de payload frente a JSON sobre HTTP/1.1 (el `RestClient` actual usa
  `SimpleClientHttpRequestFactory`, HTTP/1.1 puro).
- **Tipado fuerte del contrato**: un archivo `.proto` compartido generaría
  stubs cliente/servidor tipados en tiempo de compilación (con `protoc` +
  `protobuf-maven-plugin`), eliminando el riesgo de deserialización silenciosa
  descrito en la pregunta 3 — un cambio de campo rompería la *compilación*, no
  la ejecución.
- **Streaming nativo**, útil si `svc-pedidos` necesitara suscribirse a cambios
  de stock en tiempo real.

**Qué se perdería:**
- **Accesibilidad para consumidores externos**: el examen exige que
  `svc-catalogo` exponga una API REST pública consumible con `curl`/Postman y
  documentada en OpenAPI/Swagger (`springdoc-openapi-starter-webmvc-ui`). gRPC
  no es nativamente accesible desde un navegador ni fácilmente inspeccionable
  sin herramientas especializadas (`grpcurl`) — rompería la exposición pública
  actual.
- **Debuggability simple** durante la demostración en vivo del examen, que
  depende de peticiones `curl` legibles.

**Conclusión práctica** (regla vista en clase: *"REST hacia afuera, gRPC/mensajería
hacia adentro"*): mantendría **REST público** en `svc-catalogo` (vía Spring
Web + springdoc) para el cliente externo/gateway, y evaluaría gRPC (Spring gRPC
o `grpc-java`) **únicamente** para la llamada interna `svc-pedidos → svc-catalogo`
si el volumen de tráfico interno lo justificara.

## 6. Si `svc-catalogo` se detiene: código HTTP y Circuit Breaker

**Código recibido:** `503 Service Unavailable`, con cuerpo
`{"detalle": "...", "codigo": "CATALOGO_NO_DISPONIBLE"}`.

**Por qué:** `CatalogoClient.obtenerProducto()` (`client/CatalogoClient.java`)
configura un `SimpleClientHttpRequestFactory` con `connectTimeout` y
`readTimeout` explícitos de 2 segundos:

```java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
int timeoutMillis = (int) (timeoutSeconds * 1000);
factory.setConnectTimeout(timeoutMillis);
factory.setReadTimeout(timeoutMillis);
```

Si el proceso de `svc-catalogo` está detenido, la conexión TCP es rechazada de
inmediato (`java.net.ConnectException`); si estuviera vivo pero colgado,
expiraría el `readTimeout`. Ambos casos los envuelve Spring en
`ResourceAccessException`, que capturamos explícitamente:

```java
} catch (ResourceAccessException e) {
    throw new CatalogoNoDisponibleException(
            "svc-catalogo no respondió en " + timeoutSeconds + "s: " + e.getMessage());
}
```

`GlobalExceptionHandler.manejarCatalogoNoDisponible()` mapea esa excepción a
`503` — nunca dejamos que el error de red se propague como un `500` genérico
de Spring, ni que la petición se cuelgue indefinidamente esperando al catálogo.

**Cómo introduciría un Circuit Breaker:**

```
CLOSED (normal) → tras N fallos consecutivos → OPEN (rechaza inmediato, sin red)
                                                     │
                                     tras cooldown ──┘
                                          ↓
                                    HALF-OPEN (deja pasar 1 petición de prueba)
                                    ├── éxito → CLOSED
                                    └── falla → OPEN otra vez
```

En Spring, la forma idiomática es **Resilience4j** (`spring-boot-starter-actuator`
+ `resilience4j-spring-boot3`), anotando el método del cliente:

```java
@CircuitBreaker(name = "catalogo", fallbackMethod = "catalogoNoDisponibleFallback")
public ProductoDTO obtenerProducto(Long productoId) { ... }

private ProductoDTO catalogoNoDisponibleFallback(Long productoId, Throwable t) {
    throw new CatalogoNoDisponibleException("Circuit breaker abierto: " + t.getMessage());
}
```

configurando en `application.yml` el umbral de fallos (`failure-rate-threshold`),
la ventana deslizante (`sliding-window-size`) y el tiempo en `OPEN`
(`wait-duration-in-open-state`). La diferencia clave frente al `try/catch` +
timeout actual: con el breaker en estado **OPEN**, las peticiones fallan
**inmediatamente** (sin esperar los 2s de timeout ni consumir un hilo del pool
HTTP por cada intento) mientras `svc-catalogo` está caído y el sistema recibe
tráfico sostenido — eso es lo que realmente protege a `svc-pedidos` de una
falla en cascada. Es exactamente el patrón visto en clase (Semana 3, Tema 4):
`CLOSED → OPEN → HALF-OPEN → CLOSED`.
