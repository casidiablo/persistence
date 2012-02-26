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

import com.codeslap.persistence.suggestions.SuggestionInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author cristian
 */
public class SuggestionInfoTest {
    @Test
    public void suggestionInfoTest() {
        SuggestionInfo info = new SuggestionInfo.Builder()
                .setId(1)
                .setDescription("Description")
                .setText("Text")
                .build();
        assertEquals(1, info.getId());
        assertEquals("Description", info.getDescription());
        assertEquals("Text", info.getText());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenIdIsNotSet() {
        new SuggestionInfo.Builder()
                .setDescription("Description")
                .setText("Text")
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWhenTextIsNotSet() {
        new SuggestionInfo.Builder()
                .setId(1)
                .setDescription("Some description")
                .build();
    }
}
