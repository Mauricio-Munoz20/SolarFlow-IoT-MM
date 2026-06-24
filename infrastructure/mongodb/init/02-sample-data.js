db = db.getSiblingDB('solarflow');
print("Insertando datos de prueba para Solarflow IoT");

// 1. Insertar sensores con el esquema correcto (status y alertConfig)
db.sensors.insertMany([
    {
        deviceId: "ESP32_001",
        status: "active",
        alertConfig: {
            tempMax: 45.0,
            waterMin: 1
        }
    },
    {
        deviceId: "ESP32_002",
        status: "inactive",
        alertConfig: {
            tempMax: 60.0,
            waterMin: 0
        }
    },
    {
        deviceId: "ESP32_003",
        status: "active",
        alertConfig: {
            tempMax: 50.0,
            waterMin: 4
        }
    }
]);
print("Sensores de prueba insertados!");

// 2. Insertar historial de lecturas válidas
const now = new Date();

db.sensor_data.insertMany([
    // ESP32_001 (active)
    {
        deviceId: "ESP32_001",
        temperatura: 25.5,
        humedad: 60,
        nivelAgua: 4,
        timestamp: new Date(now.getTime() - 20000)
    },
    // ESP32_002 (inactive)
    {
        deviceId: "ESP32_002",
        temperatura: 28.0,
        humedad: 58,
        nivelAgua: 3,
        timestamp: new Date(now.getTime() - 10000)
    },
    // ESP32_003 (active)
    {
        deviceId: "ESP32_003",
        temperatura: 26.2,
        humedad: 62,
        nivelAgua: 2,
        timestamp: now
    }
]);
print("Historial de lecturas de prueba insertado!");

