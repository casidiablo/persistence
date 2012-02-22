package com.codeslap.persistence;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PersistenceLogManager {
    private static final List<Logger> loggers = new ArrayList<Logger>();

    public static void register(final String tag) {
        register(new PersistenceLogManager.Logger() {
            @Override
            public String getTag() {
                return tag;
            }

            @Override
            public boolean active() {
                return true;
            }
        });
    }

    static void register(Logger logger) {
        loggers.add(logger);
    }

    public static void clear() {
        loggers.clear();
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
