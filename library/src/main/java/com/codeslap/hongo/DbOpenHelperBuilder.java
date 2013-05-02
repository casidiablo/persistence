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

package com.codeslap.hongo;

import android.content.Context;

/** Use this to create a new {@link com.codeslap.hongo.DbOpenHelper} implementation */
public interface DbOpenHelperBuilder {
  /**
   * This method must return a new {@link com.codeslap.hongo.DbOpenHelper} implementation always.
   *
   * @param context the context used to create the open helper
   * @param name the name to provide to the open helper (database name)
   * @param version the version to provide to the open helper (database name)
   * @return new {@link com.codeslap.hongo.DbOpenHelper} implementation
   */
  DbOpenHelper buildOpenHelper(Context context, String name, int version);
}
