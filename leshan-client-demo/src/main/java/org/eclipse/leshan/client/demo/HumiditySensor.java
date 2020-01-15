package org.eclipse.leshan.client.demo;

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.util.NamedThreadFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HumiditySensor extends BaseInstanceEnabler {

    private static final int SENSOR_VALUE = 5700;
    private static final int UNITS = 5701;
    private static final int TIMESTAMP = 5702;
    private static final int MIN_MEASURED_VALUE = 5601;
    private static final int MAX_MEASURED_VALUE = 5602;
    private static final int MIN_RANGE_VALUE = 5603;
    private static final int MAX_RANGE_VALUE = 5604;
    private static final String SENSOR_UNITS = "%";
    private static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    private static final List<Integer> supportedResources = Arrays.asList(SENSOR_VALUE, UNITS, TIMESTAMP, MAX_MEASURED_VALUE,
            MIN_MEASURED_VALUE, RESET_MIN_MAX_MEASURED_VALUES, MAX_RANGE_VALUE, MIN_RANGE_VALUE);

    static final double minHumidValue = 20;
    static final double maxHumidValue = 90;
    private static double currentHumidity = 0;
    private double minMeasuredValue = currentHumidity;
    private double maxMeasuredValue = currentHumidity;

    public HumiditySensor() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Humidity Sensor"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    adjustHumidity();
                } catch (Exception e) {
                    System.out.println("Error reading sensor");
                    e.printStackTrace();
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

//        scheduler.scheduleAtFixedRate(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    adjustHumidity();
//                } catch (Exception e) {
//                    System.out.println("Error reading sensor");
//                    e.printStackTrace();
//                }
//            }
//        }, 0, 1, TimeUnit.HOURS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        switch (resourceId) {
        case SENSOR_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(currentHumidity));
        case MIN_MEASURED_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
        case MAX_MEASURED_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
        case MIN_RANGE_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(minHumidValue));
        case MAX_RANGE_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(maxHumidValue));
        case UNITS:
            return ReadResponse.success(resourceId, SENSOR_UNITS);
        case TIMESTAMP:
            return ReadResponse.success(resourceId, getCurrentTime());
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        if (resourceId == RESET_MIN_MAX_MEASURED_VALUES) {
            resetMinMaxMeasuredValues();
            return ExecuteResponse.success();
        }
        return super.execute(identity, resourceId, params);
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustHumidity() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("python3 /home/pi/Desktop/TempHumSensor/AdafruitDHT.py 11 4");
        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        if ((line = bri.readLine()) != null) {
            if (!(line.contains("Failed") || line.contains("Try again"))) {
                String[] data = line.split(" ");
                currentHumidity = Double.parseDouble(data[1]);
            } else {
                System.out.println("Error reading sensor value");
            }
        }
        bri.close();
        p.waitFor();

        Integer changedResource = adjustMinMaxMeasuredValue(currentHumidity);
        if (changedResource != null) {
            fireResourcesChange(SENSOR_VALUE, changedResource);
        } else {
            fireResourcesChange(SENSOR_VALUE);
        }
    }

    private synchronized Integer adjustMinMaxMeasuredValue(double newHumidity) {
        if (newHumidity > maxMeasuredValue) {
            maxMeasuredValue = newHumidity;
            return MAX_MEASURED_VALUE;
        } else if (newHumidity < minMeasuredValue) {
            minMeasuredValue = newHumidity;
            return MIN_MEASURED_VALUE;
        } else {
            return null;
        }
    }

    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentHumidity;
        maxMeasuredValue = currentHumidity;
    }

    private Date getCurrentTime() {
        return new Date();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
