package com.codeslap.persistence;

import android.app.Activity;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author cristian
 */
public class PrefHelperTest extends PrefTest {
    @Test
    public void prefHelperTest() {
        PreferencesAdapter adapter = Persistence.getPreferenceAdapter(new Activity());
        assertNotNull(adapter);

        adapter = Persistence.getPreferenceAdapter(new Activity(), "test.pref");
        assertNotNull(adapter);
    }
}
