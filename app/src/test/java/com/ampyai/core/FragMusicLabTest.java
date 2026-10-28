package com.ampyai.core;
import org.junit.Test;
import static org.junit.Assert.*;
public final class FragMusicLabTest {
    @Test public void a4HasCorrectFrequency() {
        assertEquals(440,FragMusicLab.pitch("A4"),.01);
    }
    @Test public void allInstrumentsSynthesizeSamples() {
        for(String name:FragMusicLab.INSTRUMENTS) {
            short[] output=FragMusicLab.synthesize(name,"C3 E3 G3");
            assertEquals(3*FragMusicLab.RATE/4,output.length);
        }
    }
    @Test public void variationsUseSeedNotes() {
        String result=FragMusicLab.variation("C3 E3 G3",17);
        for(String note:result.split(" "))
            assertTrue(note.equals("C3")||note.equals("E3")||note.equals("G3"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void unknownNotesRejected() {FragMusicLab.pitch("Z9");}
}
