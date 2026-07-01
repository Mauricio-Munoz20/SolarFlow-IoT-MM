# Taller 5 BDNR - Plataforma IoT Industrial "SolarFlow"

Plataforma de Internet de las Cosas (IoT) para ingesta, procesamiento y visualizacion de telemetria de sensores en tiempo real.

Autores:
- Mauricio Muñoz (mauricio.munoz01@alumnos.ucn.cl - 21.542.213-5)
- Julian Honores (julian.honores@alumnos.ucn.cl - 21.328.088-0)

---

## Estructura del Proyecto

```text
SolarFlow-IoT-MM/
├── docs/                      # Documentacion tecnica completa
│   ├── arquitectura-sistema.md
│   ├── arquitectura-software.md
│   ├── guion_presentacion.md
│   ├── BACKEND.MD
│   ├── FRONTEND.MD
│   ├── GRAFANA.MD
│   ├── INFLUX.MD
│   └── MONGO.MD
├── infrastructure/            # Infraestructura de datos (Docker)
│   ├── grafana/               # Provisioning y dashboards de Grafana
│   ├── influxdb/              # Configuracion de InfluxDB v3 (IOx)
│   ├── mongodb/               # Scripts de inicializacion (colecciones + datos de prueba)
│   ├── .env                   # Variables de entorno (no versionado, usar .env.example)
│   └── docker-compose.yml     # Orquestador de contenedores
├── injection-gateway/         # Backend reactivo (Java 18 + Vert.x 4)
│   ├── src/main/java/com/solarflow/
│   │   ├── config/            # AppConfig (Record inmutable)
│   │   ├── exception/         # GlobalErrorHandler + ValidationException
│   │   ├── model/             # SensorData, Alert, ApiResponse (Records)
│   │   ├── repository/        # MongoRepository + InfluxRepository
│   │   ├── util/              # Constants, WaterLevelMapper
│   │   ├── web/               # Handlers (Sensor, Metrics, Health) + ApiRouter
│   │   ├── Main.java          # Punto de entrada
│   │   └── MainVerticle.java  # Orquestador de arranque asincrono
│   ├── src/main/resources/
│   │   ├── application.json   # Configuracion del backend
│   │   └── logback.xml        # Configuracion de logs
│   └── pom.xml                # Dependencias Maven
├── ops-console/               # Frontend React (Vite + TypeScript)
│   └── src/
│       ├── components/        # SensorCard, StatusBadge, GrafanaPanel
│       ├── hooks/             # useSensorData (polling cada 10s)
│       └── App.tsx            # Componente principal del dashboard
└── simulator.mjs              # Simulador ESP32 local (Node.js, opcional)
```

### Descripcion de carpetas

| Carpeta | Descripcion |
|---------|-------------|
| `docs/` | Documentacion de arquitectura, backend, frontend, bases de datos y Grafana. |
| `infrastructure/` | Bases de datos (MongoDB, InfluxDB) y visualizacion (Grafana). Se ejecuta con Docker Compose. |
| `injection-gateway/` | API REST en Java (Vert.x) que recibe telemetria y persiste en MongoDB e InfluxDB. |
| `ops-console/` | Frontend React con dashboard de telemetria en tiempo real y panel de Grafana integrado. |

---

## Requisitos Previos

| Herramienta | Version minima | Uso |
|:------------|:---------------|:----|
| Docker Desktop | v4.x | Levantar MongoDB, InfluxDB, Grafana y Cloudflare Tunnel |
| Java JDK | 18+ | Compilar y ejecutar el injection-gateway |
| Maven | 3.8+ | Gestionar dependencias Java |
| Node.js | 18+ | Ejecutar el frontend React (Vite) |
| pnpm | 8+ | Gestor de paquetes del frontend |

---

## Guia para levantar el sistema

### Paso 1: Levantar la infraestructura (Docker)

```bash
cd infrastructure
docker compose up -d
```

Esto levanta 3 contenedores:

| Contenedor | Puerto | Funcion |
|:-----------|:-------|:--------|
| `solarflow-mongodb` | `27017` | Base de datos documental (lecturas + alertas) |
| `solarflow-influxdb` | `8086` | Base de datos columnar (series de tiempo) |
| `solarflow-grafana` | `3000` | Dashboards de visualizacion |

Verificar estado:

```bash
docker compose ps
```

### Paso 2: Crear la base de datos en InfluxDB v3

InfluxDB v3 (IOx) usa Schema-on-Write, pero el database debe existir antes de escribir datos. Crearlo manualmente la primera vez:

```bash
docker exec solarflow-influxdb influxdb3 create database --host http://127.0.0.1:8086 solarflow_metrics
```

Verificar:

```bash
docker exec solarflow-influxdb influxdb3 show databases --host http://127.0.0.1:8086
```

Debe aparecer `solarflow_metrics` en la lista. Solo se hace una vez; el volumen `influxdb_data` persiste los datos entre reinicios.

### Paso 3: Sincronizar credenciales del backend

`infrastructure/.env` define las credenciales de los contenedores. `injection-gateway/src/main/resources/application.json` define las credenciales que usa el backend Java. Ambos deben coincidir.

| Variable | Valor en `.env` (Docker) | Clave en `application.json` |
|:---------|:-------------------------|:----------------------------|
| Usuario MongoDB | `admin` | `MONGO_USER` |
| Contrasena MongoDB | `solarflow_secure_123` | `MONGO_PASSWORD` |
| Base de datos Mongo | `solarflow` | `MONGO_DB` |
| Token de InfluxDB | `my-super-secret-auth-token-solarflow-2026` | `INFLUX_ADMIN_TOKEN` |
| URL de InfluxDB | `http://127.0.0.1:8086` | `INFLUX_URL` |
| Database de Influx | `solarflow_metrics` | `INFLUX_BUCKET` |

### Paso 4: Ejecutar el injection-gateway

Desde terminal:

```bash
cd injection-gateway
mvn clean compile exec:java "-Dexec.mainClass=com.solarflow.Main"
```

Salida esperada:

```text
Iniciando secuencia de arranque de Vert.x...
Configuracion cargada - Puerto 8080, MongoDB: [oculto], InfluxDB: http://127.0.0.1:8086
Mongo DB conectado exitosamente a solarflow
Influx DB IOx configurado en http://127.0.0.1:8086
Servidor HTTP desplegado y escuchando en el puerto 8080
Despliegue exitoso.
```

### Paso 5: Ejecutar el frontend (ops-console)

```bash
cd ops-console
pnpm install
pnpm run dev
```

El frontend se levanta en `http://localhost:5173`. Muestra:
- Tarjetas con los valores actuales de temperatura, humedad y nivel de agua.
- Indicadores visuales (verde/rojo) segun umbrales.
- Un panel de Grafana integrado con el historico de la ultima hora.
- Polling automatico cada 10 segundos al endpoint `/metrics/recent`.

### Paso 6: Exponer el backend con Cloudflare Tunnel (para el profesor)

Para que el profesor pueda acceder al sistema desde Internet:

```bash
cd infrastructure
docker compose --profile tunnel up -d --force-recreate
```

Obtener el link publico generado:

```bash
docker logs solarflow-tunnel
```

En los logs aparecera una linea como:

```text
Your quick Tunnel has been created! Visit it at:
https://xxxx-yyyy-zzzz-wwww.trycloudflare.com
```

Ese es el link que se entrega.

---

## Probar los endpoints

Con el backend en el puerto 8080:

### Health Check (GET)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get
```

Respuesta esperada:

```json
{"status": "success", "message": "API SolarFlow Operational"}
```

### Enviar datos de sensor (POST)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/sensor-data" -Method Post -ContentType "application/json" -Body '{"deviceId": "ESP32_001", "temperatura": 28.5, "humedad": 65.0, "nivelAgua": 3}'
```

Respuesta esperada: `Datos ingeridos correctamente` (HTTP 202).

### Ultimos registros (GET)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/metrics/recent?deviceId=ESP32_001" -Method Get
```

---

## Acceso a los servicios

| Servicio | URL | Credenciales |
|:---------|:----|:-------------|
| Frontend (React) | http://localhost:5173 | Sin autenticacion |
| Grafana | http://localhost:3000 | `admin` / `solarflow_admin` |
| InfluxDB | http://localhost:8086 (API / FlightSQL) | Token: ver `infrastructure/.env` |
| MongoDB | `mongodb://admin:solarflow_secure_123@localhost:27017/` | MongoDB Compass u otro cliente |
| Injection Gateway | http://localhost:8080 | Sin autenticacion |

---

## Flujo de datos

```text
ESP32 / Simulador (JSON) --POST--> Injection Gateway (:8080)
                                          |
                                +---------+---------+
                                v                   v
                          MongoDB (:27017)    InfluxDB (:8086)
                          (Lecturas crudas    (Series de tiempo
                           + alertas)         para graficas)
                                                    |
                                                    v
                                              Grafana (:3000)
                                              (Dashboards en
                                               tiempo real)
                                                    |
                                                    v
                                            ops-console (:5173)
                                            (Dashboard React con
                                             Grafana embebido)
```

---

## Simulador ESP32 integrado

El link de Cloudflare Tunnel incluye un simulador automatico. Cuando el profesor abre el link en su navegador, la pagina genera datos aleatorios de temperatura (15-35 C), humedad (40-80%) y nivel de agua (1-30 mm), y los envia automaticamente al endpoint `POST /sensor-data` cada 10 segundos.

El frontend local (`localhost:5173`) refleja estos datos en tiempo real gracias al polling cada 10 segundos.

Opcionalmente, se puede usar el simulador local de Node.js:

```bash
node simulator.mjs
```

---

## Notas

- InfluxDB v3 no tiene interfaz web. Se interactua via API HTTP y FlightSQL. Grafana usa el plugin `influxdata-flightsql-datasource`.
- Warnings de `sun.misc.Unsafe` al arrancar Java son normales (aviso de Netty en Vert.x) y no afectan el funcionamiento.
- Errores de conexion a MongoDB: verificar contenedores con `docker compose ps`.
- El link de Cloudflare Tunnel cambia cada vez que se reinicia el contenedor del tunel.

---

## Documentacion adicional

Toda la documentacion tecnica se encuentra en la carpeta `docs/`:

| Archivo | Contenido |
|---------|-----------|
| `docs/arquitectura-sistema.md` | Capas del sistema IoT (edge, red, ingesta, persistencia, visualizacion) |
| `docs/arquitectura-software.md` | Patrones y flujo interno del injection-gateway |
| `docs/BACKEND.MD` | Endpoints, configuracion y ejecucion del gateway |
| `docs/FRONTEND.MD` | Documentacion del frontend React |
| `docs/MONGO.MD` | Colecciones e indices de MongoDB |
| `docs/INFLUX.MD` | Modelo de datos Line Protocol en IOx |
| `docs/GRAFANA.MD` | Provisioning y edicion de dashboards |
| `docs/guion_presentacion.md` | Guion para la presentacion oral |

---

## Tests

```bash
cd injection-gateway
mvn test
```

Salida esperada: `BUILD SUCCESS` con 8 tests pasados.
