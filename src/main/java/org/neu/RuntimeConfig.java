package org.neu;

public class RuntimeConfig {
    private RuntimeConfig() {}

    public static RuntimeConfig getInstance() {
        if (instance == null) {
            instance = new RuntimeConfig();
        }
        return instance;
    }

    @Override
    public String toString() {
        return "RuntimeConfig {" +
                "asyncMode=" + asyncMode +
                ", asyncTime=" + asyncTime +
                ", syncMode=" + syncMode +
                ", syncTime=" + syncTime +
                '}';
    }

    public boolean asyncMode = false;
    public long asyncTime = -1;

    public boolean syncMode = false;
    public long syncTime = -1;

    private static RuntimeConfig instance;
}
