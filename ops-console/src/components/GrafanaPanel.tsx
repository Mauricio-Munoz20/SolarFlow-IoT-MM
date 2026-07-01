interface GrafanaPanelProps {
  panelUrl: string;
  title: string;
}

export const GrafanaPanel = ({ panelUrl, title }: GrafanaPanelProps) => {
  return (
    <div className="h-full min-h-[400px] bg-slate-800/40 backdrop-blur-xl border border-slate-700/50 rounded-2xl shadow-2xl overflow-hidden flex flex-col">
      <div className="px-6 py-4 border-b border-slate-700/50 bg-slate-900/20">
        <h2 className="text-lg font-semibold text-slate-200">{title}</h2>
        <p className="text-xs text-slate-500 mt-1">Fuente: InfluxDB IOx via FlightSQL</p>
      </div>
      <div className="flex-1 w-full bg-[#111217]">
        <iframe 
          src={panelUrl} 
          width="100%" 
          height="100%" 
          className="w-full h-full border-0"
          title="Grafana Dashboard"
          style={{ minHeight: '400px' }}
        />
      </div>
    </div>
  );
};
