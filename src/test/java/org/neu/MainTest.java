package org.neu;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
    @Test
    public void testNoArguments() {
        assertThrows(RuntimeException.class, () -> {
            Main.handleArgs(new String[]{});
        });
    }

    @Test
    public void testArgDoesNotStartWithDoubleHyphen() {
        assertThrows(RuntimeException.class, () -> {
            Main.handleArgs(new String[]{"help"});
        });
    }

    @Test
    public void testArgStartsWithSingleHyphen() {
        assertThrows(RuntimeException.class, () -> {
            Main.handleArgs(new String[]{"-help"});
        });
    }

    @Test
    public void testConfigHasIncorrectKey() {
        assertThrows(RuntimeException.class, () -> {
            // current valid keys: async, sync
            Main.handleArgs(new String[]{"--asyyync=4000"});
        });
    }

    @Test
    public void testConfigValueIsNotNumber() {
        assertThrows(NumberFormatException.class, () -> {
            Main.handleArgs(new String[]{"--async=true"});
        });
    }

    @Test
    public void testConfigSetup() {
        String key1 = "async";
        Long value1 = 40000L;

        String arg1 = String.format("--%s=%d", key1, value1);

        Main.handleArgs(new String[]{arg1});

        RuntimeConfig runtimeConfig = RuntimeConfig.getInstance();
        assertTrue(runtimeConfig.asyncMode);
        assertEquals(runtimeConfig.asyncTime, value1);

        assertFalse(runtimeConfig.syncMode);
        assertEquals(runtimeConfig.syncTime, -1);
    }
}
