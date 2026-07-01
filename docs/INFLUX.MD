# InfluxDB IOx - Base de Datos de Series de Tiempo

InfluxDB IOx (v3) almacena metricas de telemetria optimizadas para agregaciones temporales y visualizacion en Grafana.

## Conexion

| Parametro | Valor por defecto |
|-----------|-------------------|
| URL HTTP | `http://localhost:8086` |
| Flight SQL | `localhost:8087` |
| Database | `solarflow_metrics` |
| Token | Ver `INFLUX_ADMIN_TOKEN` en `infrastructure/.env` |
| Contenedor | `solarflow-influxdb` |

## Creacion del database

A diferencia de MongoDB, InfluxDB v3 no ejecuta scripts de init en Docker. El database debe crearse manualmente la primera vez:

```bash
docker exec solarflow-influxdb influxdb3 create database --host http://127.0.0.1:8086 solarflow_metrics
```

Verificar:

```bash
docker exec solarflow-influxdb influxdb3 show databases --host http://127.0.0.1:8086
```

El volumen `influxdb_data` persiste el database entre reinicios del contenedor.

## Modelo Schema-on-Write

No hay esquema fijo previo. Las tablas se crean al escribir el primer punto de datos. El injection-gateway define la estructura al enviar Line Protocol.

## Estructura de datos (Line Protocol)

| Elemento | Valor | Descripcion |
|----------|-------|-------------|
| Measurement | `sensor_readings` | Equivalente a tabla |
| Tag | `deviceId` | Identificador del ESP32 (string, indexado) |
| Field | `temperatura` | float |
| Field | `humedad` | float |
| Field | `nivelAgua` | integer |
| Timestamp | nanosegundos | Fecha de la lectura |

Ejemplo interno:

```text
sensor_readings,deviceId=ESP32_001 temperatura=25.5,humedad=60.0,nivelAgua=4i 1718485200000000000
```

## Consultas

Grafana usa FlightSQL con el plugin `influxdata-flightsql-datasource` (provisionado en `grafana/provisioning/datasources/influxdb.yaml`) para consultar los datos de series de tiempo.

## Configuracion del backend

Claves en `injection-gateway/src/main/resources/application.json`:

| Clave | Descripcion |
|-------|-------------|
| `INFLUX_URL` | URL del servicio IOx |
| `INFLUX_ADMIN_TOKEN` | Token de autenticacion |
| `INFLUX_BUCKET` | Nombre del database (`solarflow_metrics`) |

Nota: IOx v3 usa el concepto de "database", no org/bucket de InfluxDB 2.x. Las variables `INFLUX_ORG` en `application.json` se mantienen por compatibilidad del cliente pero el database efectivo es `INFLUX_BUCKET`.

## Interfaz web

InfluxDB v3 no incluye UI propia. La visualizacion se realiza en Grafana (http://localhost:3000).
