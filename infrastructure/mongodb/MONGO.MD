# MongoDB - Base de Datos Documental

MongoDB almacena información estructurada que no es estrictamente de series de tiempo: metadatos de dispositivos, respaldo de lecturas y historial de alertas.

## Conexión

| Parámetro | Valor por defecto |
|-----------|-------------------|
| Host | `localhost:27017` |
| Usuario | `admin` |
| Contraseña | `solarflow_secure_123` |
| Base de datos | `solarflow` |
| Contenedor | `solarflow-mongodb` |

URI de ejemplo:

```text
mongodb://admin:solarflow_secure_123@localhost:27017/solarflow?authSource=admin
```

## Inicialización automática

Los scripts en `mongodb/init/` se ejecutan al crear el contenedor por primera vez:

| Script | Acción |
|--------|--------|
| `01-collections.js` | Crea colecciones con validadores JSON Schema e índices |
| `02-sample-data.js` | Inserta sensores y lecturas de prueba |

Si el volumen `mongodb_data` ya existe, los scripts no se vuelven a ejecutar. Para reinicializar: `docker compose down -v` (elimina datos).

## Colecciones

### 1. `sensors`

Metadatos y configuración de alertas por dispositivo ESP32.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `deviceId` | string | Identificador único (ej. `ESP32_001`). Índice único. |
| `status` | string | `active`, `inactive` o `maintenance` |
| `alertConfig.tempMax` | number | Temperatura máxima antes de alertar |
| `alertConfig.waterMin` | int (0-4) | Nivel mínimo de agua antes de alertar |

### 2. `sensor_data`

Respaldo crudo de lecturas. InfluxDB es la fuente principal para métricas; MongoDB mantiene copia consultable.

| Campo | Tipo | Rango |
|-------|------|-------|
| `deviceId` | string | - |
| `temperatura` | number | -20 a 85 |
| `humedad` | number | 0 a 100 |
| `nivelAgua` | int | 0 a 4 |
| `timestamp` | date | Fecha de la lectura |

Índice compuesto: `deviceId` + `timestamp` (descendente).

### 3. `alerts_history`

Registro de alertas detectadas por el injection-gateway.

Índice: `deviceId` + `timestamp` (descendente).

## Comandos útiles

Conectarse con mongosh:

```bash
docker compose exec mongodb mongosh -u admin -p solarflow_secure_123 --authenticationDatabase admin solarflow
```

Listar colecciones:

```javascript
db.getCollectionNames()
```

Contar lecturas de prueba:

```javascript
db.sensor_data.countDocuments()
```

Consultar sensores activos:

```javascript
db.sensors.find({ status: "active" })
```

## Relación con el injection-gateway

El gateway escribe en `sensor_data` vía `MongoRepository.saveSensorData()` y consulta los últimos registros con `getRecent10()`. Las alertas futuras se persistirán en `alerts_history`.

Configuración de conexión en `injection-gateway/src/main/resources/application.json` (`MONGO_USER`, `MONGO_PASSWORD`, `MONGO_DB`).
