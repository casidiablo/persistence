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

package com.codeslap.test.hongo;

import com.codeslap.hongo.HongoConfig;
import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.bytecode.ClassHandler;
import org.robolectric.res.ResourceLoader;
import org.robolectric.res.ResourcePath;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowResources;
import java.lang.reflect.Method;

public class RobolectricSqliteRunner extends RobolectricTestRunner {

  public RobolectricSqliteRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override public void setupApplicationState(final Method testMethod) {
    boolean strictI18n = false;

    ResourcePath systemResourcePath = new ResourcePath(Object.class, null, null);
    ResourceLoader systemResourceLoader = createResourceLoader(systemResourcePath);
    ShadowResources.setSystemResources(systemResourceLoader);

    ClassHandler classHandler = getRobolectricContext().getClassHandler();
    classHandler.setStrictI18n(strictI18n);

    AndroidManifest appManifest = getRobolectricContext().getAppManifest();
    Robolectric.application = ShadowApplication.bind(createApplication(), appManifest,
        systemResourceLoader);
  }

  @Override public void afterTest(Method method) {
    HongoConfig.clear();
  }
}
