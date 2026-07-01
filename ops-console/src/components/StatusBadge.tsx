interface StatusBadgeProps {
  isOnline: boolean;
  lastUpdated: Date | null;
}

export const StatusBadge = ({ isOnline, lastUpdated }: StatusBadgeProps) => {
  // Calculamos si está "Offline" si ha pasado más de 15 segundos sin datos
  const isStale = lastUpdated ? (new Date().getTime() - lastUpdated.getTime()) > 15000 : false;
  const statusOk = isOnline && !isStale;

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full border shadow-sm transition-all duration-500
      ${statusOk 
        ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' 
        : 'bg-rose-500/10 border-rose-500/20 text-rose-400'}`}
    >
      <span className="relative flex h-2.5 w-2.5">
        {statusOk && (
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
        )}
        <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${statusOk ? 'bg-emerald-500' : 'bg-rose-500'}`}></span>
      </span>
      <span className="text-sm font-medium tracking-wide">
        {statusOk ? 'SYSTEM ONLINE' : 'OFFLINE'}
      </span>
    </div>
  );
};
