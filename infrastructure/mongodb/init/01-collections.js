// collections.js: Este script inicializa la base de datos MongoDB creando las colecciones necesarias para el proyecto SolarFlow IoT.
// Ejecutar: mongosh < collections.js

db = db.getSiblingDB('solarflow');
print("Iniciando creación de colecciones de Solarflow IoT");

// 1. Colección de metadatos y alertas de sensores = Guardamos la información estática del ESP32 (IP, WiFi, fabricante) y alertas
db.createCollection("sensors", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["deviceId", "status", "alertConfig"],
            properties: {
                deviceId: { bsonType: "string" },
                status: { bsonType: "string", enum: ["active", "inactive", "maintenance"] },
                alertConfig: {
                    bsonType: "object", required: ["tempMax", "waterMin"],
                    properties: {
                        tempMax: { bsonType: "number", description: "Temperatura máxima antes de alertar" },
                        waterMin: { bsonType: "int", minimum: 0, maximum: 4, description: "Nivel mínimo de agua antes de alertar (0-4)" },
                    }
                }
            }
        }
    }
});
db.sensors.createIndex({ "deviceId": 1 }, { unique: true });
print(" 'sensors': colección con esquema validador y alertas embebidas");

// 2. sensor_data = Lecturas crudas cada 10s (JSON del ESP32)
db.createCollection("sensor_data", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["deviceId", "temperatura", "humedad", "nivelAgua", "timestamp"],
            properties: {
                deviceId: { bsonType: "string" },
                temperatura: { bsonType: "number", minimum: -20, maximum: 85 },
                humedad: { bsonType: "number", minimum: 0, maximum: 100 },
                nivelAgua: { bsonType: "int", minimum: 0, maximum: 4 },
                timestamp: { bsonType: "date" }
            }
        }
    }
});
db.sensor_data.createIndex({ "deviceId": 1, "timestamp": -1 });
print(" 'sensor_data': validación de esquema + índice compuesto");

// 3. alerts_history: Registro de eventos disparados
db.createCollection("alerts_history");
db.alerts_history.createIndex({ "deviceId": 1, "timestamp": -1 });
print(" 'alert_history': índice para consultas temporales de alertas");

print(" Estructura de MongoDB creada exitosamente")


