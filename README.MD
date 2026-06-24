# Taller 5 BDNR - Plataforma IoT Industrial "SolarFlow"

Plataforma de Internet de las Cosas (IoT) para ingesta, procesamiento y visualización de telemetría de sensores en tiempo real.

Autores:
- Mauricio Muñoz (mauricio.munoz01@alumnos.ucn.cl - 21.542.213-5)
- Julian Honores (julian.honores@alumnos.ucn.cl - 21.328.088-0)

---

## Estructura del Proyecto

```text
SolarFlow-IoT-MM/
├── docs/                      # Documentación técnica y arquitectónica
├── infrastructure/            # Infraestructura de datos (Docker)
│   ├── grafana/               # Provisioning y dashboards de Grafana
│   ├── influxdb/              # Documentación de InfluxDB v3 (IOx)
│   ├── mongodb/               # Scripts de inicialización (colecciones + datos de prueba)
│   ├── .env                   # Variables de entorno (credenciales, no versionado. Usar .env.example para crear el .env)
│   └── docker-compose.yml     # Orquestador de contenedores
├── injection-gateway/         # Backend reactivo (Java 18 + Vert.x 4)
│   ├── src/main/java/com/solarflow/
│   │   ├── config/            # AppConfig (Record inmutable)
│   │   ├── exception/         # GlobalErrorHandler + ValidationException
│   │   ├── model/             # SensorData, Alert, ApiResponse (Records)
│   │   ├── repository/        # MongoRepository + InfluxRepository
│   │   ├── util/              # Constants, WaterLevelMapper
│   │   ├── web/               # Handlers (Sensor, Metrics, Health) + ApiRouter
│   │   ├── Main.java          # Punto de entrada (public static void main)
│   │   └── MainVerticle.java  # Orquestador de arranque asíncrono
│   ├── src/main/resources/
│   │   ├── application.json   # Configuración del backend (puertos, credenciales)
│   │   └── logback.xml        # Configuración de logs
│   ├── src/test/java/com/solarflow/
│   │   ├── integration/       # Pruebas end-to-end con WebClient
│   │   └── web/               # Pruebas unitarias de handlers y router
│   ├── BACKEND.MD             # Documentación detallada del gateway
│   └── pom.xml                # Dependencias Maven
└── ops-console/               # (Futuro) Frontend / consola de operaciones
```

### Descripción de carpetas

| Carpeta | Descripción |
|---------|-------------|
| `docs/` | Documentación de arquitectura de sistema y de software. |
| `infrastructure/` | Bases de datos (MongoDB, InfluxDB) y visualización (Grafana). Se ejecuta con Docker Compose. |
| `injection-gateway/` | API REST en Java (Vert.x) que recibe telemetría del ESP32 y persiste en MongoDB e InfluxDB. |
| `ops-console/` | Frontend planificado para monitoreo y operación del sistema. |

---

## Requisitos Previos

| Herramienta | Versión mínima | Uso |
|:------------|:---------------|:----|
| Docker Desktop | v4.x | Levantar MongoDB, InfluxDB y Grafana |
| Java JDK | 18+ | Compilar y ejecutar el injection-gateway |
| Maven | 3.8+ | Gestionar dependencias Java |

---

## Guía para levantar el sistema

### Paso 1: Levantar la infraestructura (Docker)

```bash
cd infrastructure
docker compose up -d
```

Esto levanta 3 contenedores bajo el proyecto `infrastructure-taller5-minor`:

| Contenedor | Puerto | Función |
|:-----------|:-------|:--------|
| `solarflow-mongodb` | `27017` | Base de datos documental (lecturas + alertas) |
| `solarflow-influxdb` | `8086` | Base de datos columnar (series de tiempo) |
| `solarflow-grafana` | `3000` | Dashboards de visualización |

Verificar estado:

```bash
docker compose ps
```

**Verificación Grafana + InfluxDB:**

InfluxDB v3 no tiene interfaz web propia. Para comprobar la conexión:

1. Abrir Grafana en [http://localhost:3000](http://localhost:3000).
2. Iniciar sesión (`admin` / `solarflow_admin` por defecto).
3. Ir a **Connections** > **Data sources**.
4. Seleccionar **InfluxDB-IOx** (preconfigurado por provisioning).
5. Verificar que el token coincida con `INFLUX_ADMIN_TOKEN` en `infrastructure/.env`.
6. Clic en **Save & Test**. Un mensaje de éxito confirma la conexión.

Los scripts en `mongodb/init/` se ejecutan automáticamente la primera vez que arranca MongoDB. Crean la base `solarflow` con las colecciones `sensors`, `sensor_data` y `alerts_history`.

---

### Paso 2: Crear la base de datos en InfluxDB v3

InfluxDB v3 (IOx) usa **Schema-on-Write**, pero el database debe existir antes de escribir datos. Crearlo manualmente la primera vez:

```bash
docker exec solarflow-influxdb influxdb3 create database --host http://127.0.0.1:8086 solarflow_metrics
```

Verificar:

```bash
docker exec solarflow-influxdb influxdb3 show databases --host http://127.0.0.1:8086
```

Debe aparecer `solarflow_metrics` en la lista.

A diferencia de MongoDB, InfluxDB v3 no soporta scripts de init en Docker. Solo se hace una vez; el volumen `influxdb_data` persiste los datos entre reinicios.

---

### Paso 3: Sincronizar credenciales del backend

`infrastructure/.env` define las credenciales de los contenedores. `injection-gateway/src/main/resources/application.json` define las credenciales que usa el backend Java. **Ambos deben coincidir.**

| Variable | Valor en `.env` (Docker) | Clave en `application.json` |
|:---------|:-------------------------|:------------------------------|
| Usuario MongoDB | `admin` | `MONGO_USER` |
| Contraseña MongoDB | `solarflow_secure_123` | `MONGO_PASSWORD` |
| Base de datos Mongo | `solarflow` | `MONGO_DB` |
| Token de InfluxDB | `my-super-secret-auth-token-solarflow-2026` | `INFLUX_ADMIN_TOKEN` |
| URL de InfluxDB | `http://127.0.0.1:8086` | `INFLUX_URL` |
| Database de Influx | `solarflow_metrics` | `INFLUX_BUCKET` |

Si las credenciales no coinciden, el backend arranca pero fallará al conectarse a las bases de datos.

---

### Paso 4: Ejecutar el injection-gateway

**Desde el IDE (recomendado):**

1. Tener los contenedores Docker en ejecución (Paso 1).
2. Abrir `injection-gateway/src/main/java/com/solarflow/Main.java`.
3. Ejecutar **Run 'Main.main()'**.

**Desde terminal:**

```bash
cd injection-gateway
mvn clean compile exec:java "-Dexec.mainClass=com.solarflow.Main"
```

Salida esperada:

```text
Iniciando secuencia de arranque de Vert.x...
Configuración cargada - Puerto 8080, MongoDB: [oculto], InfluxDB: http://127.0.0.1:8086
Mongo DB conectado exitosamente a solarflow
Influx DB IOx configurado en http://127.0.0.1:8086
Servidor HTTP desplegado y escuchando en el puerto 8080
Despliegue exitoso.
```

Documentación adicional del backend: `injection-gateway/BACKEND.MD`.

---

### Paso 5: Probar los endpoints

Con el backend en el puerto `8080`:

#### Health Check (GET)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get
```

Respuesta esperada:

```json
{"status": "success", "message": "API SolarFlow Operational"}
```

#### Enviar datos de sensor (POST)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/sensor-data" -Method Post -ContentType "application/json" -Body '{"deviceId": "ESP32_001", "temperatura": 28.5, "humedad": 65.0, "nivelAgua": 3}'
```

Respuesta esperada: `Datos ingeridos correctamente` (HTTP 202).

#### Últimos registros (GET)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/metrics/recent?deviceId=ESP32_001" -Method Get
```

#### Promedio horario (GET)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/metrics/average?deviceId=ESP32_001" -Method Get
```

---

## Acceso a los servicios

| Servicio | URL | Credenciales |
|:---------|:----|:-------------|
| Grafana | http://localhost:3000 | `admin` / `solarflow_admin` |
| InfluxDB | http://localhost:8086 (API / FlightSQL) | Token: ver `infrastructure/.env` |
| MongoDB | `mongodb://admin:solarflow_secure_123@localhost:27017/` | MongoDB Compass u otro cliente |
| Injection Gateway | http://localhost:8080 | Sin autenticación |

---

## Flujo de datos

```text
ESP32 (JSON) ──POST──▶ Injection Gateway (:8080)
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
              MongoDB (:27017)    InfluxDB (:8086)
              (Lecturas crudas    (Series de tiempo
               + alertas)         para gráficas)
                                        │
                                        ▼
                                  Grafana (:3000)
                                  (Dashboards en
                                   tiempo real)
```

---

## Notas

- **InfluxDB v3** no tiene interfaz web. Se interactúa vía API HTTP y FlightSQL. Grafana usa el plugin `influxdata-flightsql-datasource`.
- **Warnings de `sun.misc.Unsafe`** al arrancar Java son normales (aviso de Netty en Vert.x) y no afectan el funcionamiento.
- **Errores de conexión a MongoDB:** verificar contenedores con `docker compose ps`.
- **Cloudflare Tunnel:** disponible como perfil opcional en `docker-compose.yml` (`docker compose --profile tunnel up -d`). Requiere `CLOUDFLARE_TUNNEL_TOKEN` en `.env`.

---

## Documentación adicional

| Archivo | Contenido |
|---------|-----------|
| `docs/arquitectura-sistema.md` | Capas del sistema IoT (edge, red, ingesta, persistencia, visualización) |
| `docs/arquitectura-software.md` | Patrones y flujo interno del injection-gateway |
| `infrastructure/mongodb/MONGO.MD` | Colecciones e índices de MongoDB |
| `infrastructure/influxdb/INFLUX.MD` | Modelo de datos Line Protocol en IOx |
| `infrastructure/grafana/GRAFANA.MD` | Provisioning y edición de dashboards |

## ⚠⚠⚠ Tareas Pendientes ⚠⚠⚠

1. Implementar el frontend (ops-console) utilizando React, Typescript y TailwindCSS. Preferir polling sobre WebSockets.
2. El endpoint relacionado con **GET /metrics/average** está fallando. Solucionarlo para que muestre los promedios horario
3. Implementar el tunel con Cloudfare.
4. Revisar cada salida de los endpoint y corregir si ocurren problemas o presenta datos incorrectos.

