/*
 * Copyright 2012 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    /**
     * Sends an error message to the log
     *
     * @param tag Tag to use
     * @param msg Message to send
     * @param t   a throwable to show in the log
     */
    static void e(String tag, String msg, Throwable t) {
        for (Logger logger : loggers) {
            if (logger.active()) {
                Log.e(String.format("%s:persistence:%s", logger.getTag(), tag), msg, t);
            }
        }
    }
}
