package org.eclipse.leshan.client.demo;

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyLocation extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(MyLocation.class);

    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 5);
    private static final Random RANDOM = new Random();

    private float latitude;
    private float longitude;
    private float scaleFactor;
    private Date timestamp;

    public MyLocation() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Location"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    readLocation();
                } catch (Exception e) {
                    System.out.println("Error reading location");
                    e.printStackTrace();
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

//        scheduler.scheduleAtFixedRate(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    readLocation();
//                } catch (Exception e) {
//                    System.out.println("Error reading sensor");
//                    e.printStackTrace();
//                }
//            }
//        }, 0, 1, TimeUnit.HOURS);
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        LOG.info("Read on Location Resource " + resourceid);
        switch (resourceid) {
            case 0:
                return ReadResponse.success(resourceid, getLatitude());
            case 1:
                return ReadResponse.success(resourceid, getLongitude());
            case 5:
                return ReadResponse.success(resourceid, getTimestamp());
            default:
                return super.read(identity, resourceid);
        }
    }

    private void readLocation() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("python3 /home/pi/Desktop/TempHumSensor/AdafruitDHT.py 11 4");
        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        if ((line = bri.readLine()) != null) {
            if (!(line.contains("Failed") || line.contains("Try again"))) {
                String[] data = line.split(" ");
                latitude = (float) Double.parseDouble(data[0]);
                longitude = (float) Double.parseDouble(data[1]);
            } else {
                System.out.println("Error reading sensor value");
            }
        }
        bri.close();
        p.waitFor();

        fireResourcesChange(0, 1, 5);
    }

    public void moveLocation(String nextMove) {
        switch (nextMove.charAt(0)) {
            case 'w':
                moveLatitude(1.0f);
                LOG.info("Move to North {}/{}", getLatitude(), getLongitude());
                break;
            case 'a':
                moveLongitude(-1.0f);
                LOG.info("Move to East {}/{}", getLatitude(), getLongitude());
                break;
            case 's':
                moveLatitude(-1.0f);
                LOG.info("Move to South {}/{}", getLatitude(), getLongitude());
                break;
            case 'd':
                moveLongitude(1.0f);
                LOG.info("Move to West {}/{}", getLatitude(), getLongitude());
                break;
        }
    }

    private void moveLatitude(float delta) {
        latitude = latitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(0, 5);
    }

    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourcesChange(1, 5);
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public Date getTimestamp() {
        return new Date();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
