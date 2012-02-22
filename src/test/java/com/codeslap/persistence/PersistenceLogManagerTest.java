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

import com.codeslap.robolectric.RobolectricSimpleRunner;
import com.xtremelabs.robolectric.shadows.ShadowLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author cristian
 */
@RunWith(RobolectricSimpleRunner.class)
public class PersistenceLogManagerTest {

    @Before
    public void before() {
        ShadowLog.getLogs().clear();
    }
    
    @Test
    public void testLogManagerWithLogger() {
        PersistenceLogManager.register(new PersistenceLogManager.Logger() {
            @Override
            public String getTag() {
                return "test";
            }

            @Override
            public boolean active() {
                return true;
            }
        });

        PersistenceLogManager.d("foo", "bar");
        assertEquals("test:persistence:foo", ShadowLog.getLogs().get(0).tag);
        assertEquals("bar", ShadowLog.getLogs().get(0).msg);
        assertEquals(ShadowLog.LogType.debug, ShadowLog.getLogs().get(0).type);

        ShadowLog.getLogs().clear();

        PersistenceLogManager.e("bar", "foo");
        assertEquals("test:persistence:bar", ShadowLog.getLogs().get(0).tag);
        assertEquals("foo", ShadowLog.getLogs().get(0).msg);
        assertEquals(ShadowLog.LogType.error, ShadowLog.getLogs().get(0).type);
    }

    @Test
    public void testLogManagerWithLoggerInactive() {
        PersistenceLogManager.clear();
        PersistenceLogManager.register(new PersistenceLogManager.Logger() {
            @Override
            public String getTag() {
                return "test";
            }

            @Override
            public boolean active() {
                return false;
            }
        });

        PersistenceLogManager.d("foo", "bar");
        assertEquals(0, ShadowLog.getLogs().size());

        PersistenceLogManager.e("bar", "foo");
        assertEquals(0, ShadowLog.getLogs().size());
    }

    @Test
    public void testDirectLogManager() {
        PersistenceLogManager.register("test");
        PersistenceLogManager.register("test");

        PersistenceLogManager.d("foo", "bar");
        assertEquals("test:persistence:foo", ShadowLog.getLogs().get(0).tag);
        assertEquals("bar", ShadowLog.getLogs().get(0).msg);
        assertEquals(ShadowLog.LogType.debug, ShadowLog.getLogs().get(0).type);

        ShadowLog.getLogs().clear();

        PersistenceLogManager.e("bar", "foo");
        assertEquals("test:persistence:bar", ShadowLog.getLogs().get(0).tag);
        assertEquals("foo", ShadowLog.getLogs().get(0).msg);
        assertEquals(ShadowLog.LogType.error, ShadowLog.getLogs().get(0).type);
    }
}
