package com.ampyai.core;
import org.junit.Test;
import static org.junit.Assert.*;
public final class FrapEngineTest {
    @Test public void validSvgFromProgram(){
        FrapEngine.Program p=FrapEngine.parse("line 0.1 0.2 0.8 0.9\ncircle 0.5 0.5 0.2");
        assertEquals(2,p.shapes.size());
        assertTrue(p.toSvg().contains("<circle"));
        assertTrue(p.toSvg().contains("<line"));
        assertTrue(p.toSvg().contains("</svg>"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void noArbitraryCode(){
        FrapEngine.parse("Runtime.exec dangerous");
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectOutOfBounds(){
        FrapEngine.parse("circle 0.2 0.3 0.8");
    }
    @Test public void blankProgramsNotValid(){
        try{FrapEngine.parse("");fail("Expected rejection");}
        catch(IllegalArgumentException expected){}
    }
}
