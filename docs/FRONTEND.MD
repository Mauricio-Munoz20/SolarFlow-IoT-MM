# SolarFlow Ops-Console (Frontend)

Frontend de monitoreo en tiempo real para la plataforma SolarFlow-IoT. Construido con React, TypeScript y Vite. Muestra los datos de telemetria que llegan al backend y embebe un panel de Grafana para el historico.

## Tecnologias

| Componente | Version / herramienta |
|------------|----------------------|
| React | 18 |
| TypeScript | 5 |
| Vite | 5 |
| Grafana (embebido) | 11 |
| Gestor de paquetes | pnpm |

## Ejecutar

```bash
cd ops-console
pnpm install
pnpm run dev
```

El servidor de desarrollo se levanta en `http://localhost:5173`.

## Funcionalidades

- Tarjetas de estado actual: temperatura, humedad y nivel de agua con los ultimos valores recibidos.
- Indicador visual de estado del sistema (ONLINE/OFFLINE).
- Indicadores de color por umbrales (verde para valores normales, rojo para valores criticos).
- Panel de Grafana integrado (iframe) que muestra el historico de la ultima hora desde InfluxDB.
- Polling automatico cada 10 segundos al endpoint `GET /metrics/recent` del backend.
- Ultima actualizacion visible en pantalla.

## Estructura

```text
ops-console/
├── src/
│   ├── components/
│   │   ├── SensorCard.tsx      # Tarjeta individual de sensor
│   │   ├── StatusBadge.tsx     # Indicador ONLINE/OFFLINE
│   │   └── GrafanaPanel.tsx    # Panel Grafana embebido
│   ├── hooks/
│   │   └── useSensorData.ts    # Hook de polling al backend
│   ├── types/
│   │   └── index.ts            # Tipos TypeScript
│   ├── App.tsx                 # Componente principal
│   ├── main.tsx                # Punto de entrada
│   └── index.css               # Estilos globales
├── vite.config.ts              # Proxy al backend (localhost:8080)
├── package.json
└── tsconfig.json
```

## Proxy de desarrollo

Vite esta configurado para redirigir las peticiones `/sensor-data`, `/metrics` y `/health` al backend en `http://localhost:8080`. Esto evita problemas de CORS en desarrollo.

## Dependencia del backend

El frontend requiere que el injection-gateway este corriendo en el puerto 8080 para poder mostrar datos. Sin el backend, la interfaz mostrara un estado de carga o error de conexion.
