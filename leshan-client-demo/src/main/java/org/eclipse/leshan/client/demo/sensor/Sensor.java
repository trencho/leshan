package org.eclipse.leshan.client.demo.sensor;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Sensor extends BaseInstanceEnabler {

    protected static final int SENSOR_VALUE = 5700;
    protected static final int UNITS = 5701;
    protected static final int MIN_MEASURED_VALUE = 5601;
    protected static final int MAX_MEASURED_VALUE = 5602;
    protected static final int MIN_RANGE_VALUE = 5603;
    protected static final int MAX_RANGE_VALUE = 5604;
    protected static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    protected static final int APPLICATION_TYPE = 5750;
    protected static final int MEASURED_PARTICLE_SIZE = 6043;

    protected static double currentValue = 0;
    protected static double minMeasuredValue = currentValue;
    protected static double maxMeasuredValue = currentValue;
    protected static final double maxRangeValue = currentValue;

    protected double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    protected abstract void readValue() throws Exception;

    protected synchronized Integer adjustMinMaxMeasuredValue(double newValue) {
        if (newValue > maxMeasuredValue) {
            maxMeasuredValue = newValue;
            return MAX_MEASURED_VALUE;
        } else if (newValue < minMeasuredValue) {
            minMeasuredValue = newValue;
            return MIN_MEASURED_VALUE;
        } else {
            return null;
        }
    }

    protected void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentValue;
        maxMeasuredValue = currentValue;
    }
}
