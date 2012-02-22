package com.codeslap.persistence;

import android.app.Activity;
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
