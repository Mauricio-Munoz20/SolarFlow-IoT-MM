export interface SensorData {
  deviceId: string;
  temperatura: number;
  humedad: number;
  nivelAgua: number;
  timestamp: number;
}

export interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
}
