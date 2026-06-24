# Arquitectura de Sistema (Infraestructura)

La plataforma **SolarFlow-IoT-MM** sigue un modelo de arquitectura IoT industrial de 5 capas. Cada componente funciona de forma independiente, desacoplada y contenerizada.

## Diagrama de capas

```text
[ Capa Perimetral ] ---> [ Capa de Red ] ---> [ Capa de Ingesta ] ---> [ Capa de Persistencia ] ---> [ Capa de Visualización ]
```

### 1. Capa Perimetral (Edge / Things)

| Elemento | Detalle |
|----------|---------|
| Dispositivos | Microcontroladores ESP32 |
| Sensores | DHT11 (temperatura/humedad), ultrasónico (nivel de agua) |
| Acción | Empaquetar lecturas en JSON y enviarlas vía HTTP POST al gateway |

En el futuro el transporte puede migrar a MQTT sin cambiar la capa de persistencia.

### 2. Capa de Red (Network / Transport)

| Elemento | Detalle |
|----------|---------|
| Protocolo | HTTP/1.1 con payloads JSON |
| Exposición | Cloudflare Tunnel (`cloudflared`) como túnel inverso |
| Perfil Docker | `docker compose --profile tunnel up -d` |

El túnel permite exponer el gateway a Internet sin abrir puertos en el router local. Requiere `CLOUDFLARE_TUNNEL_TOKEN` en `infrastructure/.env`.

### 3. Capa de Ingesta (Processing Gateway)

| Elemento | Detalle |
|----------|---------|
| Componente | `injection-gateway` |
| Tecnología | Java 18 + Eclipse Vert.x |
| Puerto | 8080 (configurable en `application.json`) |
| Responsabilidad | Punto de entrada único: validación, persistencia paralela, consultas |

Endpoints principales: `POST /sensor-data`, `GET /metrics/recent`, `GET /metrics/average`, `GET /health`.

### 4. Capa de Persistencia (Databases)

SolarFlow usa **persistencia políglota**:

| Motor | Rol | Puerto |
|-------|-----|--------|
| **MongoDB 8** | Lecturas crudas, metadatos de sensores (`sensors`), historial de alertas (`alerts_history`) | 27017 |
| **InfluxDB IOx v3** | Series de tiempo optimizadas para agregaciones y gráficas | 8086 |

MongoDB se inicializa con scripts en `infrastructure/mongodb/init/`. InfluxDB requiere crear el database `solarflow_metrics` manualmente la primera vez.

### 5. Capa de Visualización y Operación

| Componente | Rol |
|------------|-----|
| **Grafana 11** | Dashboards en tiempo real conectados a InfluxDB vía FlightSQL |
| **ops-console** (planificado) | Frontend React para operadores; consumirá el API del injection-gateway |

## Servicios Docker

Definidos en `infrastructure/docker-compose.yml`:

| Servicio | Contenedor | Estado |
|----------|------------|--------|
| MongoDB | `solarflow-mongodb` | Siempre activo |
| InfluxDB IOx | `solarflow-influxdb` | Siempre activo |
| Grafana | `solarflow-grafana` | Siempre activo |
| Cloudflare Tunnel | `solarflow-tunnel` | Perfil opcional `tunnel` |

## Flujo de datos completo

```text
ESP32
  │  POST /sensor-data (JSON)
  ▼
Cloudflare Tunnel (opcional)
  │
  ▼
Injection Gateway (:8080)
  ├──▶ MongoDB (:27017)     sensor_data, alerts_history, sensors
  └──▶ InfluxDB (:8086)     sensor_readings (Line Protocol)
            │
            ▼
       Grafana (:3000)       Dashboards provisionados
            │
            ▼
       ops-console (futuro)   Panel de operaciones
```

## Documentación relacionada

| Archivo | Contenido |
|---------|-----------|
| `infrastructure/mongodb/MONGO.MD` | Esquema y colecciones MongoDB |
| `infrastructure/influxdb/INFLUX.MD` | Modelo Line Protocol |
| `infrastructure/grafana/GRAFANA.MD` | Provisioning de dashboards |
| `docs/arquitectura-software.md` | Patrones internos del gateway |
