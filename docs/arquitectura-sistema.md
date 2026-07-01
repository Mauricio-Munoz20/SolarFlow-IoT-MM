# Arquitectura de Sistema (Infraestructura)

La plataforma SolarFlow-IoT-MM sigue un modelo de arquitectura IoT industrial de 5 capas. Cada componente funciona de forma independiente, desacoplada y contenerizada.

## Diagrama de capas

```text
[ Capa Perimetral ] ---> [ Capa de Red ] ---> [ Capa de Ingesta ] ---> [ Capa de Persistencia ] ---> [ Capa de Visualizacion ]
```

### 1. Capa Perimetral (Edge / Things)

| Elemento | Detalle |
|----------|---------|
| Dispositivos | Microcontroladores ESP32 (o simulador integrado) |
| Sensores | DHT11 (temperatura/humedad), ultrasonico (nivel de agua) |
| Accion | Empaquetar lecturas en JSON y enviarlas via HTTP POST al gateway |

### 2. Capa de Red (Network / Transport)

| Elemento | Detalle |
|----------|---------|
| Protocolo | HTTP/1.1 con payloads JSON |
| Exposicion | Cloudflare Tunnel (`cloudflared`) como tunel inverso |
| Comando | `docker compose --profile tunnel up -d --force-recreate` |
| Ver link | `docker logs solarflow-tunnel` |

El tunel permite exponer el gateway a Internet sin abrir puertos en el router local. El link cambia cada vez que se reinicia el contenedor.

### 3. Capa de Ingesta (Processing Gateway)

| Elemento | Detalle |
|----------|---------|
| Componente | `injection-gateway` |
| Tecnologia | Java 18 + Eclipse Vert.x |
| Puerto | 8080 (configurable en `application.json`) |
| Responsabilidad | Punto de entrada unico: validacion, persistencia paralela, consultas |

Endpoints principales: `POST /sensor-data`, `GET /metrics/recent`, `GET /health`.

La ruta raiz (`GET /`) incluye un simulador ESP32 integrado que genera datos aleatorios y los envia automaticamente cada 10 segundos.

### 4. Capa de Persistencia (Databases)

SolarFlow usa persistencia poliglota:

| Motor | Rol | Puerto |
|-------|-----|--------|
| MongoDB 8 | Lecturas crudas, metadatos de sensores (`sensors`), historial de alertas (`alerts_history`) | 27017 |
| InfluxDB IOx v3 | Series de tiempo optimizadas para agregaciones y graficas | 8086 |

MongoDB se inicializa con scripts en `infrastructure/mongodb/init/`. InfluxDB requiere crear el database `solarflow_metrics` manualmente la primera vez.

### 5. Capa de Visualizacion y Operacion

| Componente | Rol |
|------------|-----|
| Grafana 11 | Dashboards en tiempo real conectados a InfluxDB via FlightSQL |
| ops-console | Frontend React que muestra tarjetas de telemetria y embebe Grafana |

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
ESP32 / Simulador
  |  POST /sensor-data (JSON)
  v
Cloudflare Tunnel (opcional)
  |
  v
Injection Gateway (:8080)
  |---> MongoDB (:27017)     sensor_data, alerts_history, sensors
  └---> InfluxDB (:8086)     sensor_readings (Line Protocol)
            |
            v
       Grafana (:3000)       Dashboards provisionados
            |
            v
       ops-console (:5173)   Dashboard React con Grafana embebido
```

## Documentacion relacionada

| Archivo | Contenido |
|---------|-----------|
| `docs/MONGO.MD` | Esquema y colecciones MongoDB |
| `docs/INFLUX.MD` | Modelo Line Protocol |
| `docs/GRAFANA.MD` | Provisioning de dashboards |
| `docs/arquitectura-software.md` | Patrones internos del gateway |
