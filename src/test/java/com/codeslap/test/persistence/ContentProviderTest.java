/*
 * Copyright 2013 CodeSlap
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
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import com.codeslap.persistence.BaseContentProvider;
import com.codeslap.persistence.PersistenceConfig;
import org.junit.Test;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author cristian
 */
public class ContentProviderTest extends SqliteTest {

    private TestProvider mProvider;

    @Override
    public void configure() {
        super.configure();
        mProvider = createMockBuilder(TestProvider.class).addMockedMethod("getContext").createMock();
        expect(mProvider.getContext()).andReturn(new Activity()).anyTimes();
        replay(mProvider);
        mProvider.onCreate();
    }

    @Test
    public void contentProviderTest() throws NoSuchFieldException, IllegalAccessException {
        Uri uri = BaseContentProvider.buildBaseUri("com.codeslap.test", ExampleAutoincrement.class);
        ContentValues values = new ContentValues();
        values.put("name", "Foo Bar");
        values.put("number", 111);
        values.put("decimal", 222f);
        values.put("bool", true);
        values.put("blob", "Foo bar baz".getBytes());
        Uri insert = mProvider.insert(uri, values);
        assertNotNull(insert);

        values.put("number", 333);
        int update = mProvider.update(uri, values, "name LIKE ?", new String[]{"Foo Bar"});
        assertEquals(1, update);

        Cursor query = mProvider.query(uri, new String[]{"name"}, "name LIKE ?", new String[]{"Foo Bar"}, null);
        assertNotNull(query);
        assertEquals(1, query.getCount());
        assertEquals(1, query.getColumnCount());
        assertTrue(query.moveToNext());
        assertEquals("Foo Bar", query.getString(0));

        int deleted = mProvider.delete(uri, "number = ?", new String[]{"333"});
        assertEquals(1, deleted);
    }

    @Test(expected = SQLException.class)
    public void shouldFailWithEmptyValuesTest() throws NoSuchFieldException, IllegalAccessException {
        Uri uri = BaseContentProvider.buildBaseUri("com.codeslap.test", ExampleAutoincrement.class);
        ContentValues values = new ContentValues();
        mProvider.insert(uri, values);
    }

    @Test(expected = SQLException.class)
    public void shouldFailWithNullValuesTest() throws NoSuchFieldException, IllegalAccessException {
        Uri uri = BaseContentProvider.buildBaseUri("com.codeslap.test", ExampleAutoincrement.class);
        mProvider.insert(uri, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void queryShouldFailWithNotRegisteredClass() throws NoSuchFieldException, IllegalAccessException {
        Uri uri = BaseContentProvider.buildBaseUri("com.codeslap.test", ContentProviderTest.class);
        mProvider.query(uri, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateShouldFailWithNotRegisteredClass() throws NoSuchFieldException, IllegalAccessException {
        Uri uri = BaseContentProvider.buildBaseUri("com.codeslap.test", ContentProviderTest.class);
        mProvider.update(uri, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertShouldFailWithNonRegisteredClass() {
        Uri uri = BaseContentProvider.buildBaseUri("com.codeslap.test", ContentProviderTest.class);
        ContentValues values = new ContentValues();
        mProvider.insert(uri, values);
    }

    static class TestProvider extends BaseContentProvider {

        @Override
        public String getDatabaseName() {
            return "test.db";
        }

        @Override
        public String getDatabaseSpecId() {
            return PersistenceConfig.DEFAULT_SPEC_ID;
        }

        @Override
        protected String getAuthority() {
            return "com.codeslap.test";
        }
    }
}
