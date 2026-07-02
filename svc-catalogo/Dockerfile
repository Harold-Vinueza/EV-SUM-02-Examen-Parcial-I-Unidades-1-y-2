# ---------- Etapa 1: build ----------
# Contiene el JDK completo + Maven, necesarios para compilar y empaquetar.
# Esta etapa NO llega a la imagen final.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copiamos primero solo el pom.xml para aprovechar la caché de capas de Docker:
# si el código cambia pero las dependencias no, esta capa se reutiliza.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
# Imagen final: solo el JRE (no el JDK) + el .jar ya compilado.
# Sin Maven, sin compilador, sin código fuente -> imagen mucho más liviana
# y con menor superficie de ataque.
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /build/target/svc-catalogo.jar app.jar

USER app

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
    CMD curl -f http://localhost:8000/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
