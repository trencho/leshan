package org.eclipse.leshan.client.demo.sensor;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Humidity extends Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(Humidity.class);

    private static final String SENSOR_UNITS = "%";
    private static final List<Integer> supportedResources = Arrays.asList(SENSOR_VALUE, UNITS, MAX_MEASURED_VALUE,
            MIN_MEASURED_VALUE, RESET_MIN_MAX_MEASURED_VALUES, MAX_RANGE_VALUE, MIN_RANGE_VALUE);

    private static final double minHumidValue = 20;
    private static final double maxHumidValue = 90;

    public Humidity() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Humidity Sensor"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    readValue();
                } catch (Exception e) {
                    System.out.println("Error reading sensor");
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on Humidity resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case SENSOR_VALUE:
            return ReadResponse.success(resourceId, getTwoDigitValue(currentValue));
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
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        LOG.info("Execute on Humidity resource /{}/{}/{}", getModel().id, getId(), resourceId);
        if (resourceId == RESET_MIN_MAX_MEASURED_VALUES) {
            resetMinMaxMeasuredValues();
            return ExecuteResponse.success();
        }
        return super.execute(identity, resourceId, params);
    }

    protected void readValue() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("python3 /home/pi/Desktop/TempHumSensor/AdafruitDHT.py 11 4");
        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        if ((line = bri.readLine()) != null) {
            if (!(line.contains("Failed") || line.contains("Try again"))) {
                String[] data = line.split(" ");
                currentValue = Double.parseDouble(data[1]);
            } else {
                System.out.println("Error reading sensor value");
            }
        }
        bri.close();
        p.waitFor();

        Integer changedResource = adjustMinMaxMeasuredValue(currentValue);
        if (changedResource != null) {
            fireResourcesChange(SENSOR_VALUE, changedResource);
        } else {
            fireResourcesChange(SENSOR_VALUE);
        }
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
