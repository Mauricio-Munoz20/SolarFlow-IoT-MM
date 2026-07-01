import { useState, useEffect } from 'react';
import { ApiResponse, SensorData } from '../types';

export function useSensorData(deviceId: string = 'ESP32_001', pollingInterval: number = 5000) {
    const [data, setData] = useState<SensorData | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

    useEffect(() => {
        let isMounted = true;

        const fetchData = async () => {
            try {
                const response = await fetch(`/metrics/recent?deviceId=${deviceId}`);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const result: ApiResponse<SensorData[]> = await response.json();

                if (isMounted) {
                    // El backend devuelve una lista de los más recientes. Tomamos el primero.
                    if (result.data && result.data.length > 0) {
                        setData(result.data[0]);
                        setLastUpdated(new Date());
                    }
                    setError(null);
                }
            } catch (err) {
                if (isMounted) {
                    setError(err instanceof Error ? err.message : 'Error desconocido al conectar con Gateway');
                }
            } finally {
                if (isMounted) {
                    setLoading(false);
                }
            }
        };

        // Primera llamada inmediata
        fetchData();

        // Configurar Polling
        const intervalId = setInterval(fetchData, pollingInterval);

        return () => {
            isMounted = false;
            clearInterval(intervalId);
        };
    }, [deviceId, pollingInterval]);

    return { data, loading, error, lastUpdated };
}
