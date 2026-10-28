package com.ampyai.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.util.List;

public class FragHelpCatalogTest {
    @Test public void onlyRealBaselineCommandsAtBirth() {
        List<FragHelpCatalog.Entry> entries=FragHelpCatalog.basics();
        assertTrue(entries.stream().anyMatch(e->e.example.equals("Psicofråg")));
        assertTrue(entries.stream().anyMatch(e->e.example.equals("help")));
        assertTrue(entries.stream().allMatch(e->e.executable && !e.learned));
    }
    @Test public void acquiredConceptIsNotPretendedExecutable() {
        FragHelpCatalog.Entry learned=FragHelpCatalog.visited(
                "0123","Quadrato","https://example.org/quadrato",
                "Un quadrato ha quattro lati uguali",new File("/tmp/quadrato.png"));
        assertTrue(learned.learned);
        assertFalse(learned.executable);
        assertTrue(learned.matches("quadrato"));
        assertTrue(learned.example.startsWith("memoria: "));
        assertTrue(learned.imagePath.endsWith(".png"));
    }
    @Test public void savedFrapIsAnExecutableLimitedModule() {
        FragHelpCatalog.Entry p=FragHelpCatalog.frap("figura",
                "rect 0.1 0.1 0.8 0.8",null);
        assertTrue(p.executable);
        assertTrue(p.learned);
        assertEquals("frap: figura",p.example);
        assertTrue(p.matches("vettoriale"));
    }
    @Test public void personalLessonExposesImageAndSafeLookup() {
        FragHelpCatalog.Entry p=FragHelpCatalog.personal("parola","kalita",
                "PAROLA: kalita\nSIGNIFICATO: saluto\nPRONUNCIA: ka-li-ta",
                new File("/tmp/kalita.png"));
        assertFalse(p.executable);
        assertTrue(p.matches("saluto"));
        assertEquals("ricorda: parola/kalita",p.example);
    }
}
