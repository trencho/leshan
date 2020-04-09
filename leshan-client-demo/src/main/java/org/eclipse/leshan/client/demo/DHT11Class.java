package org.eclipse.leshan.client.demo;

import org.eclipse.leshan.core.util.NamedThreadFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class DHT11Class extends Observable {

    static final double minHumiValue = 20;
    static final double maxHumiValue = 90;

    private static double humidity = 0;
    private static double temperature = 0;

    public DHT11Class() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DHT11"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    readTemperatureAndHumidity();
                } catch (Exception e) {
                    System.out.println("Error reading sensor");
                    e.printStackTrace();
                }
            }
        }, 2, 10, TimeUnit.SECONDS);
    }

    private synchronized void readTemperatureAndHumidity() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("python3 /home/pi/Desktop/TempHumSensor/AdafruitDHT.py 11 4");
        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        if ((line = bri.readLine()) != null) {
            if (!(line.contains("Failed") || line.contains("Try again"))) {
                String[] data = line.split(" ");
                temperature = Double.parseDouble(data[0]);
                humidity = Double.parseDouble(data[1]);
            } else {
                System.out.println("Error reading sensor value");
            }
        }
        bri.close();
        p.waitFor();

        setChanged();
        notifyObservers();
    }

    synchronized double getTemperature() {
        return temperature;
    }

    synchronized double getHumidity() {
        return humidity;
    }

}
