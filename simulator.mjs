const url = 'http://localhost:8080/sensor-data';

console.log("Iniciando simulador ESP32...");
console.log("Enviando datos cada 5 segundos a " + url);

setInterval(async () => {
  const t = parseFloat((Math.random() * (35 - 15) + 15).toFixed(1)); // Temp 15 a 35
  const h = parseFloat((Math.random() * (80 - 40) + 40).toFixed(1)); // Humedad 40 a 80
  const a = Math.floor(Math.random() * 7); // Agua 0 a 6 (0-4 valido, 5-6 fallara intencionalmente)

  const data = { 
    deviceId: 'ESP32_001', 
    temperatura: t, 
    humedad: h, 
    nivelAgua: a 
  };

  try {
    const res = await fetch(url, { 
      method: 'POST', 
      headers: {'Content-Type': 'application/json'}, 
      body: JSON.stringify(data) 
    });
    console.log('Enviado:', data, '->', res.status);
  } catch (e) {
    console.error('Error:', e.message);
  }
}, 5000);
