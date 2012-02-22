package com.codeslap.persistence;

import android.database.Cursor;
import com.codeslap.robolectric.RobolectricSimpleRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
@RunWith(RobolectricSimpleRunner.class)
public class SuggestionsCursorTest {
    @Test
    public void suggestionsCursorTest() {
        Cursor cursor = new SuggestionsCursor(Arrays.asList(
                new SuggestionInfo.Builder().setId(1).setText("Foo").setDescription("Baz").build(),
                new SuggestionInfo.Builder().setId(2).setText("Bar").setDescription("Bal").build()));
        assertEquals(2, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getLong(SuggestionsCursor.COLUMN_INDEX_ID));
        assertEquals("Foo", cursor.getString(SuggestionsCursor.COLUMN_INDEX_TEXT));
        assertEquals("Baz", cursor.getString(SuggestionsCursor.COLUMN_INDEX_DESCRIPTION));

        assertTrue(cursor.moveToNext());
        assertEquals(2, cursor.getLong(SuggestionsCursor.COLUMN_INDEX_ID));
        assertEquals("Bar", cursor.getString(SuggestionsCursor.COLUMN_INDEX_TEXT));
        assertEquals("Bal", cursor.getString(SuggestionsCursor.COLUMN_INDEX_DESCRIPTION));

        assertEquals(0, cursor.getShort(0));
        assertEquals(0, cursor.getInt(0));
        assertEquals(0, cursor.getFloat(0), 0);
        assertEquals(false, cursor.isNull(0));
        assertEquals(0, cursor.getDouble(0), 0);

        assertFalse(cursor.moveToNext());
    }

    @Test
    public void suggestionsCursorNoDescriptionTest() {
        Cursor cursor = new SuggestionsCursor(Arrays.asList(
                new SuggestionInfo.Builder().setId(1).setText("Foo").build(),
                new SuggestionInfo.Builder().setId(2).setText("Bar").build()));
        assertEquals(2, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getLong(SuggestionsCursor.COLUMN_INDEX_ID));
        assertEquals("Foo", cursor.getString(SuggestionsCursor.COLUMN_INDEX_TEXT));

        assertTrue(cursor.moveToNext());
        assertEquals(2, cursor.getLong(SuggestionsCursor.COLUMN_INDEX_ID));
        assertEquals("Bar", cursor.getString(SuggestionsCursor.COLUMN_INDEX_TEXT));

        assertFalse(cursor.moveToNext());
    }
}
