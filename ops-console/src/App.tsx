import { Thermometer, Droplets, Waves, ServerCrash } from 'lucide-react';
import { useSensorData } from './hooks/useSensorData';
import { StatusBadge } from './components/StatusBadge';
import { SensorCard } from './components/SensorCard';
import { GrafanaPanel } from './components/GrafanaPanel';

function App() {
  const { data, loading, error, lastUpdated } = useSensorData('ESP32_001', 10000);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-200 selection:bg-emerald-500/30">
      
      {/* Background gradients for premium feel */}
      <div className="fixed inset-0 z-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-indigo-500/10 blur-[120px]"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-emerald-500/10 blur-[120px]"></div>
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        {/* Header */}
        <header className="flex flex-col md:flex-row md:items-center justify-between mb-10 gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-white flex items-center gap-3">
              SolarFlow <span className="text-emerald-400">Ops-Console</span>
            </h1>
            <p className="text-slate-400 mt-1">Monitoreo de Telemetría IoT en Tiempo Real</p>
          </div>
          <div className="flex items-center gap-4 bg-slate-900/50 p-3 rounded-2xl border border-slate-800">
            <div className="text-right">
              <p className="text-xs text-slate-500 uppercase font-semibold tracking-wider">Dispositivo</p>
              <p className="text-sm font-medium text-slate-300">ESP32_001</p>
            </div>
            <div className="h-8 w-px bg-slate-700"></div>
            <StatusBadge isOnline={!error} lastUpdated={lastUpdated} />
          </div>
        </header>

        {/* Error State */}
        {error && (
          <div className="mb-8 p-4 bg-rose-500/10 border border-rose-500/20 rounded-2xl flex items-start gap-3">
            <ServerCrash className="text-rose-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="text-rose-400 font-medium">Error de Conexión</h3>
              <p className="text-sm text-rose-300/80 mt-1">{error}</p>
              <p className="text-xs text-slate-500 mt-2">Asegúrate de que el backend de Vert.x esté corriendo en el puerto 8080 y no haya problemas de CORS.</p>
            </div>
          </div>
        )}

        {/* Main Grid Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Left Column: Real-time Cards */}
          <div className="lg:col-span-4 flex flex-col gap-6">
            <h2 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-indigo-500"></span>
              Estado Actual
            </h2>
            
            <SensorCard 
              title="Temperatura" 
              value={data ? data.temperatura.toFixed(1) : '--'} 
              unit="°C" 
              icon={Thermometer} 
              colorClass={data && data.temperatura > 30 ? "bg-red-500/20 text-red-400 border-red-500/50" : "bg-orange-500/20 text-orange-400 border-orange-500/50"} 
            />
            <SensorCard 
              title="Humedad" 
              value={data ? data.humedad.toFixed(1) : '--'} 
              unit="%" 
              icon={Droplets} 
              colorClass="bg-emerald-500/20 text-emerald-400 border-emerald-500/50" 
            />
            <SensorCard 
              title="Nivel de Agua" 
              value={data ? data.nivelAgua : '--'} 
              unit="mm" 
              icon={Waves} 
              colorClass={data && data.nivelAgua < 20 ? "bg-red-500/20 text-red-400 border-red-500/50" : "bg-blue-500/20 text-blue-400 border-blue-500/50"} 
            />

            <div className="mt-auto p-4 rounded-2xl bg-slate-800/30 border border-slate-700/30 text-center">
              <p className="text-xs text-slate-500">
                Última actualización: {lastUpdated ? lastUpdated.toLocaleTimeString() : 'Esperando...'}
              </p>
            </div>
          </div>

          {/* Right Column: Historical Grafana Panel */}
          <div className="lg:col-span-8 flex flex-col">
            <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Análisis Histórico
            </h2>
            
            {/* Panel embebido de Grafana generado por el usuario */}
            <div className="flex-1 min-h-[500px]">
              <GrafanaPanel 
                title="Telemetría de la última hora (InfluxDB IOx)"
                panelUrl="http://localhost:3000/d-solo/bfqqulpibcsu8b/telematria-iot?from=now-1h&to=now&timezone=browser&orgId=1&panelId=1&__feature.dashboardSceneSolo&theme=dark" 
              />
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

export default App;
