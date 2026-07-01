# SolarFlow Injection Gateway

Backend principal del proyecto SolarFlow-IoT, construido con Eclipse Vert.x (Java reactivo). Recibe la telemetria de los sensores ESP32, valida el payload, persiste en MongoDB e InfluxDB de forma concurrente y expone endpoints de consulta.

## Tecnologias

| Componente | Version / herramienta |
|------------|----------------------|
| Java | 18+ |
| Framework | Eclipse Vert.x 4 (Web, Config, Mongo Client) |
| Bases de datos | MongoDB 8, InfluxDB IOx v3 |
| Cliente Influx | influxdb-client-java |
| Tests | JUnit 5, Mockito, Vert.x JUnit5 |
| Build | Maven |

## Estructura del codigo

```text
src/main/java/com/solarflow/
├── config/             # AppConfig (Record, carga application.json + ENV)
├── exception/          # ValidationException, GlobalErrorHandler
├── model/              # SensorData, Alert, ApiResponse (Records)
├── repository/         # MongoRepository, InfluxRepository
├── util/               # Constants, WaterLevelMapper
├── web/
│   ├── handler/        # SensorDataHandler, MetricsHandler, HealthHandler
│   └── router/         # ApiRouter (mapeo de rutas + simulador ESP32 integrado)
├── Main.java           # Launcher (public static void main)
└── MainVerticle.java   # Orquestador de arranque

src/main/resources/
├── application.json    # Credenciales y puerto del servidor
└── logback.xml         # Configuracion de logs (SLF4J)

src/test/java/com/solarflow/
├── integration/        # SensorDataIntegrationTest, MetricsIntegrationTest
└── web/
    ├── handler/        # SensorDataHandlerTest, MetricsHandlerTest
    └── router/         # ApiRouterTest
```

## Requisitos previos

Antes de ejecutar el gateway, la infraestructura Docker debe estar levantada. Ver `README.MD` en la raiz del repositorio (Pasos 1 a 3).

## Ejecutar el servidor

Desde terminal (PowerShell):

```powershell
cd injection-gateway
mvn clean compile exec:java "-Dexec.mainClass=com.solarflow.Main"
```

El servidor queda escuchando en el puerto configurado en `application.json` (por defecto 8080).

## Endpoints

| Metodo | Ruta | Descripcion | Codigo |
|--------|------|-------------|--------|
| GET | `/` | Pagina de bienvenida con simulador ESP32 integrado (genera POST cada 10s) | 200 |
| POST | `/sensor-data` | Ingesta de telemetria del ESP32 | 202 |
| GET | `/metrics/recent?deviceId=` | Ultimos 10 registros desde MongoDB | 200 |
| GET | `/health` | Estado operativo del API | 200 |

### Ejemplos (PowerShell)

POST /sensor-data

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/sensor-data" -Method Post -ContentType "application/json" -Body '{"deviceId": "ESP32_001", "temperatura": 28.5, "humedad": 65.0, "nivelAgua": 3}'
```

Respuesta: `Datos ingeridos correctamente`.

GET /metrics/recent

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/metrics/recent?deviceId=ESP32_001" -Method Get
```

GET /health

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get
```

Respuesta: `{"status": "success", "message": "API SolarFlow Operational"}`.

## Payload de ingesta

```json
{
  "deviceId": "ESP32_001",
  "temperatura": 28.5,
  "humedad": 65.0,
  "nivelAgua": 3
}
```

| Campo | Tipo | Rango |
|-------|------|-------|
| `deviceId` | string | Identificador del dispositivo |
| `temperatura` | number | -20 a 85 |
| `humedad` | number | 0 a 100 |
| `nivelAgua` | integer | 0 a 4 |

## Configuracion

Archivo: `src/main/resources/application.json`

| Clave | Descripcion |
|-------|-------------|
| `SERVER_PORT` | Puerto HTTP del gateway |
| `MONGO_USER` / `MONGO_PASSWORD` / `MONGO_DB` | Conexion a MongoDB |
| `INFLUX_URL` | URL de InfluxDB IOx |
| `INFLUX_ADMIN_TOKEN` | Token de autenticacion |
| `INFLUX_BUCKET` | Nombre del database IOx (`solarflow_metrics`) |

Los valores deben coincidir con `infrastructure/.env`.

## Tests

```bash
cd injection-gateway
mvn test
```

Cobertura actual:

- Unitarios (Mockito): handlers aislados con repositorios mockeados.
- Router (Vert.x Test): codigos HTTP y rutas registradas.
- Integracion: flujo HTTP completo con WebClient en puertos efimeros.

Salida esperada: `Tests run: 8, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

## Flujo interno (POST /sensor-data)

1. `ApiRouter` enruta la peticion a `SensorDataHandler`.
2. El JSON se mapea a `SensorData` (Record inmutable).
3. Se ejecutan en paralelo `MongoRepository.saveSensorData()` e `InfluxRepository.writeSensorData()`.
4. Si ambas operaciones completan, responde HTTP 202.
5. Errores no capturados los maneja `GlobalErrorHandler`.

Documentacion de patrones y arquitectura: `docs/arquitectura-software.md`.
