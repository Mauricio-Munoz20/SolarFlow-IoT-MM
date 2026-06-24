# Arquitectura de Software (Injection Gateway)

El backend de **SolarFlow-IoT-MM** (`injection-gateway`) está construido sobre **Eclipse Vert.x**, un toolkit reactivo para la JVM basado en event loop no bloqueante. No utiliza frameworks monolíticos como Spring Boot.

## Paradigmas principales

1. **Programación reactiva asíncrona:** las operaciones de red y base de datos retornan `Future<T>` sin bloquear el hilo principal.
2. **Inmutabilidad:** el modelo de dominio usa **Java Records** (`SensorData`, `Alert`, `ApiResponse`, `AppConfig`). Los objetos son thread-safe por diseño.
3. **Persistencia políglota en paralelo:** al recibir datos válidos, el gateway escribe concurrentemente en MongoDB e InfluxDB.

## Patrones de diseño

### 1. Front Controller + Router

| Clase | Responsabilidad |
|-------|-----------------|
| `MainVerticle` | Orquesta arranque: config, clientes, repositorios, router, servidor HTTP |
| `ApiRouter` | Mapea rutas a handlers; registra `GlobalErrorHandler` |

### 2. Handler Pattern

Los handlers reciben `RoutingContext` y contienen la lógica de cada endpoint:

| Handler | Ruta | Función |
|---------|------|---------|
| `SensorDataHandler` | `POST /sensor-data` | Valida JSON, persiste en ambas BD, responde 202 |
| `MetricsHandler` | `GET /metrics/recent`, `GET /metrics/average` | Consultas a MongoDB e InfluxDB |
| `HealthHandler` | `GET /health` | Verificación de estado del API |

### 3. Repository Pattern

| Repositorio | Operaciones |
|-------------|-------------|
| `MongoRepository` | `saveSensorData`, `getRecent10`, persistencia de alertas |
| `InfluxRepository` | Escritura Line Protocol, consultas SQL para promedios |

El resto de la aplicación no depende de las librerías nativas de cada base de datos.

### 4. Global Error Handler

`GlobalErrorHandler` captura excepciones no manejadas del router:

- `ValidationException` → respuesta 400/422 con JSON estandarizado
- Errores de BD u otros → respuesta 500 con mensaje controlado

Evita bloques `try-catch` repetidos en cada handler.

## Flujo de una petición POST /sensor-data

```text
ESP32 ──POST JSON──▶ HttpServer (:8080)
                          │
                          ▼
                     ApiRouter
                          │
                          ▼
                  SensorDataHandler
                    │         │
         parse JSON ▼         ▼ validar rangos
              SensorData (Record)
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
  MongoRepository         InfluxRepository
  saveSensorData()        writeSensorData()
         │                     │
         └──────────┬──────────┘
                    ▼
              Future.all()
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
    HTTP 202 Accepted    GlobalErrorHandler
                         (HTTP 400 / 500)
```

## Capas del código

```text
web/          → HTTP (router, handlers)
service/      → (reservado para lógica de negocio futura: alertas, cache)
repository/   → Acceso a datos (Mongo, Influx)
model/        → DTOs y entidades (Records)
config/       → Configuración centralizada
exception/    → Manejo de errores
util/         → Helpers (WaterLevelMapper, Constants)
```

## Tests

| Tipo | Ubicación | Herramientas |
|------|-----------|--------------|
| Unitarios | `web/handler/*Test` | JUnit 5, Mockito |
| Router | `web/router/ApiRouterTest` | Vert.x JUnit5, WebClient |
| Integración | `integration/*Test` | Vert.x Test, puertos efímeros |

Los tests de integración levantan un verticle de prueba sin depender de Docker en runtime (los repositorios se mockean o usan instancias de test según el caso).

## Documentación relacionada

| Archivo | Contenido |
|---------|-----------|
| `injection-gateway/BACKEND.MD` | Endpoints, configuración, ejecución |
| `docs/arquitectura-sistema.md` | Capas del sistema completo |
| `infrastructure/mongodb/MONGO.MD` | Esquema MongoDB |
| `infrastructure/influxdb/INFLUX.MD` | Modelo InfluxDB |
