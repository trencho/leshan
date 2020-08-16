package org.eclipse.leshan.client.demo.enums;

public enum ParticulateEnum {
    PM25 ("PM2.5"),
    PM10 ("PM10");

    private final String name;

    ParticulateEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static ParticulateEnum fromString(String particulateMatter) {
        for (ParticulateEnum particulateEnum : ParticulateEnum.values()) {
            if (particulateEnum.name.equalsIgnoreCase(particulateMatter)) {
                return particulateEnum;
            }
        }
        throw new IllegalArgumentException("No constant with text " + particulateMatter + " found");
    }
}
