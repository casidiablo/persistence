package com.codeslap.persistence;

import org.junit.Test;

/**
 * // TODO write description
 *
 * @author cristian
 */
public class Deleteme {
    @Test
    public void testDeleteme() {
        System.out.println(new Del());
    }

    private static class Del{
        Long pepe;

        @Override
        public String toString() {
            return "Del{" +
                    "pepe=" + pepe +
                    '}';
        }
    }
}
