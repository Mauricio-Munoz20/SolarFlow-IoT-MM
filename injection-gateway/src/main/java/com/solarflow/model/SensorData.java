package com.solarflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)

public record SensorData(
    @JsonProperty("deviceId") String deviceId,

    @JsonProperty("temperatura") Double temperatura,

    @JsonProperty("humedad") Double humedad,

    @JsonProperty("nivelAgua") Integer nivelAgua,

    Long timestamp

) {
    public SensorData {
        if (timestamp == null) {
            timestamp = Instant.now().toEpochMilli();
        }
    }
}
