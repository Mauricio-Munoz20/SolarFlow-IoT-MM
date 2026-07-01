# Grafana - Visualizacion de Datos

Esta carpeta contiene la configuracion de aprovisionamiento automatico (provisioning) de Grafana. Al levantar el contenedor, los datasources y dashboards se cargan sin configuracion manual.

## Conexion

| Parametro | Valor por defecto |
|-----------|-------------------|
| URL | http://localhost:3000 |
| Usuario | `admin` |
| Contrasena | `solarflow_admin` |
| Contenedor | `solarflow-grafana` |

## Estructura de directorios

```text
infrastructure/grafana/
├── json_dashboards/
│   └── solarflow-main.json      # Dashboard exportado desde la UI
├── provisioning/
│   ├── datasources/
│   │   └── influxdb.yaml        # Conexion automatica a InfluxDB IOx
│   └── dashboards/
│       └── dashboard.yaml       # Ruta de lectura de json_dashboards/
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

1. Entrar a http://localhost:3000 con las credenciales del `.env`.
2. Crear o editar el dashboard en la interfaz grafica.
3. Ir a Share o Dashboard settings.
4. Seleccionar Export > Save to file.
5. Guardar el JSON en `json_dashboards/` (sobrescribir o crear archivo nuevo).
6. Hacer commit en Git.

Cualquier entorno que levante Docker Compose tendra los mismos dashboards automaticamente.

Importante: no editar los archivos `.json` a mano. Son generados por Grafana y son dificiles de mantener manualmente. Usar siempre la interfaz visual.

## Plugins instalados

Definidos en `docker-compose.yml` bajo `GF_INSTALL_PLUGINS`:

- `grafana-clock-panel`
- `grafana-piechart-panel`
- `influxdata-flightsql-datasource`

## Verificar conexion a InfluxDB

1. Connections > Data sources > InfluxDB-IOx
2. Confirmar token en el campo correspondiente
3. Save and Test

Un mensaje de exito confirma que Grafana puede consultar `solarflow_metrics`.

## Integracion con el frontend

El frontend React (ops-console) embebe paneles de Grafana via iframe apuntando a `http://localhost:3000`. Para que funcione, Grafana debe permitir embedding anonimo, configurado en `docker-compose.yml` con las variables `GF_AUTH_ANONYMOUS_*` y `GF_SECURITY_ALLOW_EMBEDDING`.
