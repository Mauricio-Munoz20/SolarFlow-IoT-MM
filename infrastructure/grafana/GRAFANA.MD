# Grafana - Visualización de Datos

Esta carpeta contiene la configuración de **aprovisionamiento automático (provisioning)** de Grafana. Al levantar el contenedor, los datasources y dashboards se cargan sin configuración manual.

## Conexión

| Parámetro | Valor por defecto |
|-----------|-------------------|
| URL | [http://localhost:3000](http://localhost:3000) |
| Usuario | `admin` |
| Contraseña | `solarflow_admin` |
| Contenedor | `solarflow-grafana` |

## Estructura de directorios

```text
grafana/
├── json_dashboards/
│   └── solarflow-main.json      # Dashboard exportado desde la UI
├── provisioning/
│   ├── datasources/
│   │   └── influxdb.yaml        # Conexión automática a InfluxDB IOx
│   └── dashboards/
│       └── dashboard.yaml       # Ruta de lectura de json_dashboards/
└── GRAFANA.MD                   # Esta documentación
```

## Datasource provisionado

Archivo: `provisioning/datasources/influxdb.yaml`

| Campo | Valor |
|-------|-------|
| Nombre | InfluxDB-IOx |
| Tipo | influxdata-flightsql-datasource |
| Host interno | `solarflow-influxdb:8086` |
| Database | `solarflow_metrics` |
| Token | `${INFLUX_ADMIN_TOKEN}` desde `infrastructure/.env` |

## Editar o agregar dashboards

1. Entrar a [http://localhost:3000](http://localhost:3000) con las credenciales del `.env`.
2. Crear o editar el dashboard en la interfaz gráfica.
3. Ir a **Share** o **Dashboard settings**.
4. Seleccionar **Export** > **Save to file**.
5. Guardar el JSON en `json_dashboards/` (sobrescribir o crear archivo nuevo).
6. Hacer commit en Git.

Cualquier entorno que levante Docker Compose tendrá los mismos dashboards automáticamente.

**Importante:** no editar los archivos `.json` a mano. Son generados por Grafana y son difíciles de mantener manualmente. Usar siempre la interfaz visual.

## Dashboard actual

`solarflow-main.json` contiene un panel de bienvenida placeholder. Cuando el ESP32 envíe datos reales, agregar paneles de temperatura, humedad y nivel de agua consultando InfluxDB vía FlightSQL.

## Plugins instalados

Definidos en `docker-compose.yml` bajo `GF_INSTALL_PLUGINS`:

- `grafana-clock-panel`
- `grafana-piechart-panel`
- `influxdata-flightsql-datasource`

## Verificar conexión a InfluxDB

1. **Connections** > **Data sources** > **InfluxDB-IOx**
2. Confirmar token en el campo correspondiente
3. **Save & Test**

Un mensaje de éxito confirma que Grafana puede consultar `solarflow_metrics`.
