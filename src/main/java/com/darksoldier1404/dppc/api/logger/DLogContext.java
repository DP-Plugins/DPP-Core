package com.darksoldier1404.dppc.api.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class DLogContext {
    private final long nanoTime = System.nanoTime();
    private final Date timestamp = new Date();
    private final String context;
    private final Level logLevel;

    public DLogContext(String context, Level logLevel) {
        this.context = context;
        this.logLevel = logLevel;
    }

    public static DLogContext of(String context, Level logLevel) {
        return new DLogContext(context, logLevel);
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getContext() {
        return context;
    }

    public String getFormatedTimestamp() {
        String nanoTimeStr = String.valueOf(nanoTime);
        return new SimpleDateFormat("HH-mm-ss").format(getTimestamp()) + "-" + nanoTimeStr.substring(nanoTimeStr.length() - 6);
    }

    public String getFormatedContext() {
        return "[" + logLevel.getName() + "] " + context;
    }

    public String getFormatedFullContext() {
        return "[" + logLevel.getName() + "] " + "[" + getFormatedTimestamp() + "] " + context;
    }
}
