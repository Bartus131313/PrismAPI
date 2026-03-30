package com.bartekkansy.prism.api.util;

public class PrismConfig {
    public static final String VERSION      = "1.1.0";
    public static final DevState DEV_STATE  = DevState.Snapshot;

    public static String getVersion() {
        if (DEV_STATE == DevState.Release) return VERSION;

        return VERSION + "-" + DEV_STATE.name().toUpperCase();
    }

    private enum DevState {
        Snapshot, Dev,
        Beta, Alpha,
        Release
    }
}
