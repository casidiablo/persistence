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

package com.codeslap.test.persistence;

import android.app.Activity;
import com.codeslap.persistence.Persistence;
import com.codeslap.persistence.PreferencesAdapter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
public class PrefRetrieveTest extends PrefTest {
    @Test
    public void shouldReturnNullWhenRetrievingImpossibleBean() {
        PreferencesAdapter adapter = Persistence.getPreferenceAdapter(new Activity());
        PrefImpossibleBean retrieve = adapter.retrieve(PrefImpossibleBean.class);
        assertNull(retrieve);
    }

    @Test
    public void shouldReturnFullObjectWhenUsingAnnotations() {
        PreferencesAdapter adapter = Persistence.getPreferenceAdapter(new Activity());
        PrefWithAnnotations retrieve = adapter.retrieve(PrefWithAnnotations.class);
        assertEquals(Integer.MIN_VALUE, retrieve.number);
        assertEquals(Float.MIN_VALUE, retrieve.decimal, 0);
        assertEquals(Long.MIN_VALUE, retrieve.veryLong, 0);
        assertEquals("foo bar", retrieve.name);
        assertTrue(retrieve.bool);
    }
    @Test
    public void shouldReturnFullObjectWhenUsingAnnotationsFromSpecificPreference() {
        PreferencesAdapter adapter = Persistence.getPreferenceAdapter(new Activity(), "test.pref");
        PrefWithAnnotations retrieve = adapter.retrieve(PrefWithAnnotations.class);
        assertEquals(Integer.MIN_VALUE, retrieve.number);
        assertEquals(Float.MIN_VALUE, retrieve.decimal, 0);
        assertEquals(Long.MIN_VALUE, retrieve.veryLong, 0);
        assertEquals("foo bar", retrieve.name);
        assertTrue(retrieve.bool);
    }
}
