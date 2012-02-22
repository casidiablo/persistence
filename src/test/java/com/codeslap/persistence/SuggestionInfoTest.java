package com.codeslap.persistence;

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
