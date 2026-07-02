# MercadoQuevedo — Prototipo de Microservicios (Java / Spring Boot)

Prototipo de arquitectura de microservicios para MercadoQuevedo (marketplace de
comerciantes locales), desarrollado para el Examen Práctico de Unidad 2
(EV-PR-U2) — Aplicaciones Distribuidas (ISR-701), UTEQ.

Esta es la versión **Java 21 + Spring Boot 3.3 + PostgreSQL 16**, siguiendo el
stack sugerido en las restricciones técnicas del examen. También existe una
versión equivalente en Python/FastAPI si la necesitas comparar.

## Arquitectura

Ver diagrama completo en [`docs/architecture.svg`](docs/architecture.svg).

```
Cliente → Nginx (reverse proxy, único puerto expuesto: 8080)
              ├── /api/v1/productos/*  → svc-catalogo (Spring Boot) → db-catalogo (PostgreSQL)
              └── /api/v1/pedidos/*    → svc-pedidos  (Spring Boot) → db-pedidos  (PostgreSQL)

svc-pedidos ──HTTP síncrono (RestClient, timeout 2s)──> svc-catalogo   (verifica existencia y stock)
```

- **Database per Service**: cada microservicio tiene su propia base de datos
  PostgreSQL; no existe ningún esquema ni tabla compartida.
- **Capas** dentro de cada servicio: `controller` → `service` → `repository` → `model`,
  más `dto` (contratos de entrada/salida) y `exception` (mapeo a códigos HTTP).
- **Stack**: Java 21 + Spring Boot 3.3 (Web, Data JPA, Validation) + Maven +
  PostgreSQL 16 + springdoc-openapi (Swagger UI) + Nginx + Docker Compose.

## Estructura del repositorio

```
mercadoquevedo-java/
├── svc-catalogo/
│   ├── src/main/java/ec/edu/uteq/mercadoquevedo/catalogo/
│   │   ├── CatalogoApplication.java
│   │   ├── model/Producto.java
│   │   ├── repository/ProductoRepository.java
│   │   ├── service/ProductoService.java
│   │   ├── controller/ProductoController.java, HealthController.java
│   │   ├── dto/ProductoRequest.java, ProductoResponse.java, ErrorResponse.java
│   │   └── exception/ (excepciones de dominio + GlobalExceptionHandler)
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile           # Multi-stage: build (Maven+JDK) → runtime (JRE, no-root)
├── svc-pedidos/
│   ├── src/main/java/ec/edu/uteq/mercadoquevedo/pedidos/
│   │   ├── client/CatalogoClient.java, ProductoDTO.java   # comunicación inter-servicio
│   │   └── ...                                             # misma estructura en capas
│   ├── pom.xml
│   └── Dockerfile
├── nginx/nginx.conf          # Reverse proxy / puerta de entrada única
├── docs/
│   ├── architecture.svg
│   └── api/                  # Especificación OpenAPI 3.0 (generar con scripts/exportar-openapi.sh)
├── scripts/exportar-openapi.sh
├── docker-compose.yml
├── ANALISIS.md
└── README.md
```

## Requisitos previos

- Docker Desktop instalado y en ejecución.
- Puerto **8080** libre en el host.
- (Opcional, solo para desarrollo local fuera de contenedores) JDK 21 + Maven 3.9+.

> **Nota:** este proyecto fue generado y revisado cuidadosamente línea por línea,
> pero no pudo compilarse en el entorno donde se escribió (sin acceso a Maven
> Central). La **primera vez** que ejecutes `docker compose up --build`, revisa
> con atención el log de la etapa `mvn package` de cada servicio por si hay que
> ajustar algún detalle menor de dependencias.

## Cómo levantar el sistema (un solo comando)

```bash
docker compose up --build
```

Esto construye ambas imágenes (Maven compila el `.jar` en la etapa `build`,
luego se descarta y solo queda el `.jar` sobre una imagen JRE en `runtime`),
levanta las dos bases PostgreSQL, espera a que estén `service_healthy`, arranca
`svc-catalogo` y `svc-pedidos` (Spring Boot tarda ~15-25 s en arrancar — por eso
el `start_period` del healthcheck es más largo que en una app FastAPI), y
finalmente Nginx. Verifica:

```bash
docker compose ps
```

## Flujo de demostración

```bash
# 1. Crear un producto en el catálogo
curl -X POST http://localhost:8080/api/v1/productos \
  -H "Content-Type: application/json" \
  -d '{"sku":"CAM-001","nombre":"Camisa artesanal","precio":18.50,"stock":10}'
# -> 201 Created, header Location: /api/v1/productos/1

# 2. Consultar el catálogo
curl http://localhost:8080/api/v1/productos

# 3. Crear un pedido válido (svc-pedidos consulta a svc-catalogo internamente)
curl -X POST http://localhost:8080/api/v1/pedidos \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":2}]}'
# -> 201 Created, subtotal calculado automáticamente

# 4. Provocar un pedido que falle por producto inexistente
curl -X POST http://localhost:8080/api/v1/pedidos \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":9999,"cantidad":1}]}'
# -> 404 Not Found, codigo: PRODUCTO_NO_EXISTE

# 5. Provocar un pedido que falle por stock insuficiente
curl -X POST http://localhost:8080/api/v1/pedidos \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":9999}]}'
# -> 409 Conflict, codigo: STOCK_INSUFICIENTE

# 6. Detener svc-catalogo y observar el comportamiento de svc-pedidos
docker compose stop svc-catalogo
curl -X POST http://localhost:8080/api/v1/pedidos \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":1,"cantidad":1}]}'
# -> 503 Service Unavailable, codigo: CATALOGO_NO_DISPONIBLE (tras 2s de timeout)

docker compose start svc-catalogo   # restaurar para seguir probando
```

## Documentación interactiva (Swagger UI)

- Catálogo: `http://localhost:8080/docs/catalogo/`
- Pedidos: `http://localhost:8080/docs/pedidos/`

## Exportar la especificación OpenAPI a `/docs/api/`

Con el stack ya corriendo:

```bash
bash scripts/exportar-openapi.sh
```

Esto guarda `docs/api/catalogo.json` y `docs/api/pedidos.json` (springdoc-openapi
los genera automáticamente en `/api/v1/openapi.json` de cada servicio).

## Diseño de la API REST — resumen (Nivel 2 de Richardson)

| Recurso | Método | URI | Código éxito | Códigos de error |
|---|---|---|---|---|
| Productos | POST | `/api/v1/productos` | 201 + `Location` | 400, 409, 422 |
| Productos | GET | `/api/v1/productos` | 200 | — |
| Productos | GET | `/api/v1/productos/{id}` | 200 | 404 |
| Productos | PUT | `/api/v1/productos/{id}` | 200 (idempotente) | 404, 409, 422 |
| Productos | DELETE | `/api/v1/productos/{id}` | 204 (idempotente) | 404 |
| Pedidos | POST | `/api/v1/pedidos` | 201 + `Location` | 400, 404, 409, 422, 503 |
| Pedidos | GET | `/api/v1/pedidos` | 200 | — |
| Pedidos | GET | `/api/v1/pedidos/{id}` | 200 | 404 |

URIs en plural, versionadas (`/api/v1/`), sin verbos en la ruta, IDs en el path.

**Cómo se logra cada código en Java/Spring** (útil para la defensa oral):
- `201 + Location`: `ResponseEntity.created(URI.create(...))` en el controller.
- `404`: excepción de dominio (`ProductoNoEncontradoException`, etc.) capturada
  por `@RestControllerAdvice`.
- `409`: `SkuDuplicadoException` / `StockInsuficienteException`.
- `400`: `HttpMessageNotReadableException` (Jackson no pudo parsear el JSON —
  ocurre *antes* de `@Valid`).
- `422`: `MethodArgumentNotValidException` (Bean Validation, `@Valid` falló una
  regla de negocio con JSON sintácticamente válido).
- `503`: `CatalogoNoDisponibleException`, lanzada por `CatalogoClient` al
  capturar `ResourceAccessException` (timeout/conexión rechazada) o
  `HttpServerErrorException` (5xx del catálogo).

## Justificación de la imagen base (Parte D)

- **Etapa build**: `maven:3.9-eclipse-temurin-21` — incluye el JDK completo y
  Maven, necesarios para compilar. Se descarta por completo.
- **Etapa runtime**: `eclipse-temurin:21-jre-alpine` — solo el JRE (no el JDK,
  no el compilador, no Maven) sobre Alpine Linux. Resultado: imagen final de
  ~180-220 MB en vez de ~500+ MB de la imagen de build, y superficie de ataque
  mínima (sin herramientas de compilación ni gestor de paquetes de build).
- Ambas imágenes ejecutan como usuario **no-root** (`app`), creado
  explícitamente en el Dockerfile, y exponen únicamente el puerto `8000`.

## Notas de la Parte E (Compose)

- Red `mercadoquevedo-net` (bridge) dedicada: ningún servicio interno tiene
  puertos publicados al host, salvo Nginx (`8080:80`).
- `depends_on` con `condition: service_healthy` en cascada: las bases de datos
  deben estar `healthy` antes de que arranquen los microservicios, y éstos deben
  estar `healthy` antes de que Nginx los reciba como upstream.
- Volúmenes nombrados (`catalogo_data`, `pedidos_data`) para persistencia entre
  reinicios del contenedor.
- `ddl-auto: update` en `application.yml` crea/actualiza el esquema
  automáticamente al arrancar (aceptable para un prototipo; en producción se
  usaría Flyway/Liquibase con migraciones versionadas).
