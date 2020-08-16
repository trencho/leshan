package org.eclipse.leshan.client.demo.enums;

import org.eclipse.leshan.client.demo.sensor.Humidity;
import org.eclipse.leshan.client.demo.sensor.Particulates;
import org.eclipse.leshan.client.demo.sensor.Sensor;
import org.eclipse.leshan.client.demo.sensor.Temperature;

public enum SensorTypeEnum {
    HUMIDITY {
        public Sensor createSensor() {
            return new Humidity();
        }
    },
    PM10 {
        public Sensor createSensor() {
            return new Particulates(ParticulateEnum.PM10.getName());
        }
    },
    PM25 {
        public Sensor createSensor() {
            return new Particulates(ParticulateEnum.PM25.getName());
        }
    },
    TEMPERATURE {
        public Sensor createSensor() {
            return new Temperature();
        }
    };

    public abstract Sensor createSensor();
}
