package com.zhengyaozy.trace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
public class AttractionTest {
    @Test public void guidesHaveUniqueIdsAndReadableContent() {
        assertEquals(3, Attraction.SAMPLES.size());
        HashSet<String> ids = new HashSet<>();
        for (Attraction a : Attraction.SAMPLES) {
            assertTrue(ids.add(a.id));
            assertFalse(a.name.isBlank());
            assertTrue(a.narration.length() > 100);
            assertTrue(a.narration.contains("\n\n"));
            assertFalse(a.narration.contains("\\n"));
            assertTrue(a.latitude > 30 && a.latitude < 31);
            assertTrue(a.longitude > 120 && a.longitude < 121);
        }
    }
}
