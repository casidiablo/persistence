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

import android.content.ContentUris;
import android.net.Uri;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

@Implements(ContentUris.class)
public class ShadowContentUris {

    @Implementation
    public static Uri withAppendedId(Uri contentUri, long id) {
        return Uri.withAppendedPath(contentUri, String.valueOf(id));
    }

    @Implementation
    public static long parseId(Uri contentUri) {
        if (!contentUri.isHierarchical()) {
            throw new UnsupportedOperationException();
        }
        String path = contentUri.getLastPathSegment();
        if (path == null) return -1;
        return Long.parseLong(path);
    }

}