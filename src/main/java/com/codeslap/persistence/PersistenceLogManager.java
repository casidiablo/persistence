package com.codeslap.persistence;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PersistenceLogManager {
    private static final List<Logger> loggers = new ArrayList<Logger>();

    public static void register(final String tag, final boolean active) {
        register(new PersistenceLogManager.Logger() {
            @Override
            public String getTag() {
                return tag;
            }

            @Override
            public boolean active() {
                return active;
            }
        });
    }

    static void register(Logger logger) {
        if (!loggers.contains(logger)) {
            loggers.add(logger);
        }
    }

    interface Logger {
        String getTag();
        boolean active();
    }

    /**
     * Sends a debug message to the log
     *
     * @param tag Tag to use
     * @param msg Message to send
     */
    static void d(String tag, String msg) {
        for (Logger logger : loggers) {
            if (logger.active()) {
                Log.d(String.format("%s:persistence:%s", logger.getTag(), tag), msg);
            }
        }
    }

    /**
     * Send an error message to the log
     *
     * @param tag Tag to use
     * @param msg Message to send
     */
    static void e(String tag, String msg) {
        for (Logger logger : loggers) {
            if (logger.active()) {
                Log.e(String.format("%s:persistence:%s", logger.getTag(), tag), msg);
            }
        }
    }
}
