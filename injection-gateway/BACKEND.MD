# SolarFlow Injection Gateway

Backend principal del proyecto SolarFlow-IoT, construido con **Eclipse Vert.x** (Java reactivo). Recibe la telemetría de los sensores ESP32, valida el payload, persiste en MongoDB e InfluxDB de forma concurrente y expone endpoints de consulta.

## Tecnologías

| Componente | Versión / herramienta |
|------------|----------------------|
| Java | 18+ |
| Framework | Eclipse Vert.x 4 (Web, Config, Mongo Client) |
| Bases de datos | MongoDB 8, InfluxDB IOx v3 |
| Cliente Influx | influxdb-client-java |
| Tests | JUnit 5, Mockito, Vert.x JUnit5 |
| Build | Maven |

## Estructura del código

```text
src/main/java/com/solarflow/
├── config/             # AppConfig (Record, carga application.json + ENV)
├── exception/          # ValidationException, GlobalErrorHandler
├── model/              # SensorData, Alert, ApiResponse (Records)
├── repository/         # MongoRepository, InfluxRepository
├── util/               # Constants, WaterLevelMapper
├── web/
│   ├── handler/        # SensorDataHandler, MetricsHandler, HealthHandler
│   └── router/         # ApiRouter (mapeo de rutas)
├── Main.java           # Launcher (public static void main)
└── MainVerticle.java   # Orquestador de arranque

src/main/resources/
├── application.json    # Credenciales y puerto del servidor
└── logback.xml         # Configuración de logs (SLF4J)

src/test/java/com/solarflow/
├── integration/        # SensorDataIntegrationTest, MetricsIntegrationTest
└── web/
    ├── handler/        # SensorDataHandlerTest, MetricsHandlerTest
    └── router/         # ApiRouterTest
```

## Requisitos previos

Antes de ejecutar el gateway, la infraestructura Docker debe estar levantada. Ver `README.MD` en la raíz del repositorio (Pasos 1 a 3).

## Ejecutar el servidor

**Desde el IDE (recomendado):**

Abrir `src/main/java/com/solarflow/Main.java`, clic derecho, **Run 'Main.main()'**.

**Desde terminal (PowerShell):**

```powershell
cd injection-gateway
mvn clean compile exec:java "-Dexec.mainClass=com.solarflow.Main"
```

El servidor queda escuchando en el puerto configurado en `application.json` (por defecto `8080`).

## Endpoints

| Método | Ruta | Descripción | Código |
|--------|------|-------------|--------|
| POST | `/sensor-data` | Ingesta de telemetría del ESP32 | 202 |
| GET | `/metrics/recent?deviceId=` | Últimos 10 registros desde MongoDB | 200 |
| GET | `/metrics/average?deviceId=` | Promedio horario desde InfluxDB | 200 |
| GET | `/health` | Estado operativo del API | 200 |

### Ejemplos (PowerShell)

**POST /sensor-data**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/sensor-data" -Method Post -ContentType "application/json" -Body '{"deviceId": "ESP32_001", "temperatura": 28.5, "humedad": 65.0, "nivelAgua": 3}'
```

Respuesta: `Datos ingeridos correctamente`.

**GET /metrics/recent**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/metrics/recent?deviceId=ESP32_001" -Method Get
```

**GET /metrics/average**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/metrics/average?deviceId=ESP32_001" -Method Get
```

**GET /health**

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

## Configuración

Archivo: `src/main/resources/application.json`

| Clave | Descripción |
|-------|-------------|
| `SERVER_PORT` | Puerto HTTP del gateway |
| `MONGO_USER` / `MONGO_PASSWORD` / `MONGO_DB` | Conexión a MongoDB |
| `INFLUX_URL` | URL de InfluxDB IOx |
| `INFLUX_ADMIN_TOKEN` | Token de autenticación |
| `INFLUX_BUCKET` | Nombre del database IOx (`solarflow_metrics`) |

Los valores deben coincidir con `infrastructure/.env`.

## Tests

```bash
cd injection-gateway
mvn test
```

Cobertura actual:

- **Unitarios (Mockito):** handlers aislados con repositorios mockeados.
- **Router (Vert.x Test):** códigos HTTP y rutas registradas.
- **Integración:** flujo HTTP completo con WebClient en puertos efímeros.

Salida esperada: `Tests run: 9, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

## Flujo interno (POST /sensor-data)

1. `ApiRouter` enruta la petición a `SensorDataHandler`.
2. El JSON se mapea a `SensorData` (Record inmutable).
3. Se ejecutan en paralelo `MongoRepository.saveSensorData()` e `InfluxRepository.writeSensorData()`.
4. Si ambas operaciones completan, responde HTTP 202.
5. Errores no capturados los maneja `GlobalErrorHandler`.

Documentación de patrones y arquitectura: `docs/arquitectura-software.md`.
