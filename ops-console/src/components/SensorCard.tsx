import type { LucideIcon } from 'lucide-react';

interface SensorCardProps {
  title: string;
  value: number | string;
  unit: string;
  icon: LucideIcon;
  trend?: 'up' | 'down' | 'stable';
  colorClass: string;
}

export const SensorCard = ({ title, value, unit, icon: Icon, trend, colorClass }: SensorCardProps) => {
  return (
    <div className="relative overflow-hidden bg-slate-800/40 backdrop-blur-xl border border-slate-700/50 rounded-2xl p-6 shadow-2xl transition-all duration-300 hover:bg-slate-800/60 hover:border-slate-600/50 group">
      
      {/* Glow effect en el fondo */}
      <div className={`absolute -right-6 -top-6 w-24 h-24 rounded-full opacity-10 blur-2xl group-hover:opacity-20 transition-opacity ${colorClass.replace('text-', 'bg-')}`}></div>

      <div className="flex justify-between items-start mb-4">
        <h3 className="text-slate-400 font-medium text-sm uppercase tracking-wider">{title}</h3>
        <div className={`p-2.5 rounded-xl bg-slate-900/50 border border-slate-800 shadow-inner ${colorClass}`}>
          <Icon size={20} strokeWidth={2.5} />
        </div>
      </div>
      
      <div className="flex items-baseline gap-2">
        <span className="text-4xl font-bold text-white tracking-tight">{value}</span>
        <span className="text-slate-400 font-medium">{unit}</span>
      </div>
      
      {trend && (
        <div className="mt-4 flex items-center gap-2 text-xs font-medium">
          <span className={`px-2 py-1 rounded-md ${
            trend === 'up' ? 'bg-rose-500/20 text-rose-400' : 
            trend === 'down' ? 'bg-emerald-500/20 text-emerald-400' : 
            'bg-slate-700/50 text-slate-400'
          }`}>
            {trend === 'up' ? '↑ Subiendo' : trend === 'down' ? '↓ Bajando' : '→ Estable'}
          </span>
          <span className="text-slate-500">Última lectura</span>
        </div>
      )}
    </div>
  );
};
