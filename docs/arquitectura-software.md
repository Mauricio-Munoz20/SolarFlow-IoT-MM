# Arquitectura de Software (Injection Gateway)

El backend de SolarFlow-IoT-MM (`injection-gateway`) esta construido sobre Eclipse Vert.x, un toolkit reactivo para la JVM basado en event loop no bloqueante. No utiliza frameworks monoliticos como Spring Boot.

## Paradigmas principales

1. Programacion reactiva asincrona: las operaciones de red y base de datos retornan `Future<T>` sin bloquear el hilo principal.
2. Inmutabilidad: el modelo de dominio usa Java Records (`SensorData`, `Alert`, `ApiResponse`, `AppConfig`). Los objetos son thread-safe por diseno.
3. Persistencia poliglota en paralelo: al recibir datos validos, el gateway escribe concurrentemente en MongoDB e InfluxDB.

## Patrones de diseno

### 1. Front Controller + Router

| Clase | Responsabilidad |
|-------|-----------------|
| `MainVerticle` | Orquesta arranque: config, clientes, repositorios, router, servidor HTTP |
| `ApiRouter` | Mapea rutas a handlers; registra `GlobalErrorHandler`; sirve pagina de bienvenida con simulador |

### 2. Handler Pattern

Los handlers reciben `RoutingContext` y contienen la logica de cada endpoint:

| Handler | Ruta | Funcion |
|---------|------|---------|
| `SensorDataHandler` | `POST /sensor-data` | Valida JSON, persiste en ambas BD, responde 202 |
| `MetricsHandler` | `GET /metrics/recent` | Consulta los ultimos 10 registros desde MongoDB |
| `HealthHandler` | `GET /health` | Verificacion de estado del API |

### 3. Repository Pattern

| Repositorio | Operaciones |
|-------------|-------------|
| `MongoRepository` | `saveSensorData`, `getRecent10`, persistencia de alertas |
| `InfluxRepository` | Escritura Line Protocol |

El resto de la aplicacion no depende de las librerias nativas de cada base de datos.

### 4. Global Error Handler

`GlobalErrorHandler` captura excepciones no manejadas del router:

- `ValidationException` -> respuesta 400/422 con JSON estandarizado
- Errores de BD u otros -> respuesta 500 con mensaje controlado

Evita bloques `try-catch` repetidos en cada handler.

## Flujo de una peticion POST /sensor-data

```text
ESP32 --POST JSON--> HttpServer (:8080)
                          |
                          v
                     ApiRouter
                          |
                          v
                  SensorDataHandler
                    |         |
         parse JSON v         v validar rangos
              SensorData (Record)
                    |
         +----------+----------+
         v                     v
  MongoRepository         InfluxRepository
  saveSensorData()        writeSensorData()
         |                     |
         +----------+----------+
                    v
              Future.all()
                    |
         +----------+----------+
         v                     v
    HTTP 202 Accepted    GlobalErrorHandler
                         (HTTP 400 / 500)
```

## Capas del codigo

```text
web/          -> HTTP (router, handlers)
repository/   -> Acceso a datos (Mongo, Influx)
model/        -> DTOs y entidades (Records)
config/       -> Configuracion centralizada
exception/    -> Manejo de errores
util/         -> Helpers (WaterLevelMapper, Constants)
```

## Tests

| Tipo | Ubicacion | Herramientas |
|------|-----------|--------------|
| Unitarios | `web/handler/*Test` | JUnit 5, Mockito |
| Router | `web/router/ApiRouterTest` | Vert.x JUnit5, WebClient |
| Integracion | `integration/*Test` | Vert.x Test, puertos efimeros |

Los tests de integracion levantan un verticle de prueba sin depender de Docker en runtime.

## Documentacion relacionada

| Archivo | Contenido |
|---------|-----------|
| `docs/BACKEND.MD` | Endpoints, configuracion, ejecucion |
| `docs/arquitectura-sistema.md` | Capas del sistema completo |
| `docs/MONGO.MD` | Esquema MongoDB |
| `docs/INFLUX.MD` | Modelo InfluxDB |
