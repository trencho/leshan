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

public class Particulates extends Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(Particulates.class);

    private static final String UNIT_CONCENTRATION = "μg/m3";
    private static final List<Integer> supportedResources = Arrays.asList(SENSOR_VALUE, UNITS, MIN_MEASURED_VALUE,
            MAX_MEASURED_VALUE, MAX_RANGE_VALUE, APPLICATION_TYPE, RESET_MIN_MAX_MEASURED_VALUES, MEASURED_PARTICLE_SIZE
    );

    private String applicationType = "";

    public Particulates(String applicationType) {
        this.applicationType = applicationType;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Particulates Sensor"));
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
        LOG.info("Read on Particulates resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case MIN_MEASURED_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case MAX_MEASURED_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case MAX_RANGE_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxRangeValue));
            case SENSOR_VALUE:
                return ReadResponse.success(resourceId, getTwoDigitValue(currentValue));
            case UNITS:
                return ReadResponse.success(resourceId, UNIT_CONCENTRATION);
            case APPLICATION_TYPE:
                return ReadResponse.success(resourceId, applicationType);
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        LOG.info("Execute on Particulates resource /{}/{}/{}", getModel().id, getId(), resourceId);
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
                currentValue = Double.parseDouble(line);
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
