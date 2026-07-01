package com.solarflow.web.router;

import com.solarflow.config.AppConfig;
import com.solarflow.exception.GlobalErrorHandler;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import com.solarflow.web.handler.HealthHandler;
import com.solarflow.web.handler.MetricsHandler;
import com.solarflow.web.handler.SensorDataHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;
import java.util.Set;

public class ApiRouter {

    public static Router create(Vertx vertx, AppConfig config,
            MongoRepository mongoRepo, InfluxRepository influxRepo) {
        Router router = Router.router(vertx);

        // CORS a prueba de balas (Permitir todo)
        router.route().handler(ctx -> {
            ctx.response().putHeader("Access-Control-Allow-Origin", "*");
            ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ctx.response().putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            if (ctx.request().method() == HttpMethod.OPTIONS) {
                ctx.response().setStatusCode(204).end();
            } else {
                ctx.next();
            }
        });

        // Middleware global para parsear cuerpos JSON (Obligatorio para POST)
        router.route().handler(BodyHandler.create());

        // Instanciar Handlers
        SensorDataHandler sensorHandler = new SensorDataHandler(mongoRepo, influxRepo);
        MetricsHandler metricsHandler = new MetricsHandler(mongoRepo, influxRepo);
        HealthHandler healthHandler = new HealthHandler();

        router.post("/sensor-data").handler(sensorHandler::handle);
        router.get("/metrics/recent").handler(metricsHandler::getRecent);
        router.get("/health").handler(healthHandler::handle);

        // Ruta raíz: Mensaje de bienvenida + Simulador de ESP32 Integrado
        router.get("/").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "text/html; charset=UTF-8")
                    .end("<html><head><title>SolarFlow IoT</title></head>"
                            + "<body style='font-family:sans-serif;background:#0f172a;color:#e2e8f0;display:flex;justify-content:center;align-items:center;height:100vh;margin:0;flex-direction:column;'>"
                            + "<div style='text-align:center'>"
                            + "<h1 style='color:#34d399;margin-bottom:5px'>☀ SolarFlow IoT Gateway</h1>"
                            + "<p style='margin-top:0'>API activa. <b>Simulador ESP32 Iniciado.</b></p>"
                            + "<div style='margin-top:30px;padding:20px;background:#1e293b;border-radius:10px;border:1px solid #334155;'>"
                            + "<h3 style='margin-top:0;color:#94a3b8'>Dato inyectado automáticamente:</h3>"
                            + "<p id='lastData' style='font-size:24px;font-weight:bold;color:#38bdf8'>Iniciando inyección...</p>"
                            + "</div>"
                            + "</div>"
                            + "<script>"
                            + "  async function simulateESP32() {"
                            + "    const t = (Math.random() * (35 - 15) + 15).toFixed(1);"
                            + "    const h = (Math.random() * (80 - 40) + 40).toFixed(1);"
                            + "    const a = Math.floor(Math.random() * 7);" // Genera 0 a 6
                            + "    const payload = { deviceId: 'ESP32_001', temperatura: parseFloat(t), humedad: parseFloat(h), nivelAgua: a };"
                            + "    try {"
                            + "      await fetch('/sensor-data', {"
                            + "        method: 'POST',"
                            + "        headers: { 'Content-Type': 'application/json' },"
                            + "        body: JSON.stringify(payload)"
                            + "      });"
                            + "      document.getElementById('lastData').innerHTML = t + ' °C | ' + h + ' % | ' + a + ' mm';"
                            + "    } catch(e) { console.error('Error enviando data', e); }"
                            + "  }"
                            + "  simulateESP32();"
                            + "  setInterval(simulateESP32, 10000);" // POST cada 10 segundos exactos
                            + "</script>"
                            + "</body></html>");
        });

        router.route().failureHandler(new GlobalErrorHandler());
        return router;
    }
}
