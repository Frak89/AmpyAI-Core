package com.ampyai.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FragDrawingEngineTest {
    @Test public void learnsSquareFromARealSentence() {
        FragDrawingEngine.Fact f=FragDrawingEngine.extract(
                "Un quadrato è una figura geometrica con quattro lati uguali e quattro angoli retti.");
        assertEquals("quadrato",f.name);
        assertEquals(4,f.sides);
        assertTrue(f.equal);
        assertTrue(f.right);
        FragDrawingEngine.Concept c=new FragDrawingEngine.Concept(f.name);
        assertTrue(c.merge(f));
        FragDrawingEngine.Program p=FragDrawingEngine.compile(c);
        assertEquals(4,p.points.size());
        assertTrue(p.path().startsWith("M "));
        assertTrue(p.path().endsWith(" Z"));
        assertTrue(p.javaCode(30,30,180).contains("canvas.drawPath"));
        assertTrue(p.javaCode(30,30,180).contains("path.close()"));
    }
    @Test public void associatesFactsAcrossSeparateLessons() {
        FragDrawingEngine.Concept c=new FragDrawingEngine.Concept("quadrato");
        c.merge(FragDrawingEngine.extract("Il quadrato possiede quattro lati."));
        assertTrue(FragDrawingEngine.limitation(c).contains("quattro lati uguali"));
        c.merge(FragDrawingEngine.extract("Il quadrato ha quattro angoli retti."));
        assertTrue(FragDrawingEngine.limitation(c).contains("quattro lati uguali"));
        c.merge(FragDrawingEngine.extract("Nel quadrato tutti i lati sono uguali."));
        assertEquals("",FragDrawingEngine.limitation(c));
        assertEquals(3,c.observations);
        FragDrawingEngine.Program p=FragDrawingEngine.compile(c);
        FragDrawingEngine.verify(p,c);
    }
    @Test public void learnsNewNamedPolygonFromText() {
        FragDrawingEngine.Fact f=FragDrawingEngine.extract(
            "La figura stellina ha cinque lati uguali ed è un poligono regolare.");
        assertEquals("stellina",f.name);
        assertEquals(5,f.sides);
        assertTrue(f.regular);
        FragDrawingEngine.Concept c=new FragDrawingEngine.Concept("stellina");
        c.merge(f);
        assertEquals(5,FragDrawingEngine.compile(c).points.size());
    }
    @Test(expected=IllegalStateException.class)
    public void rejectsSquareWithoutAngles() {
        FragDrawingEngine.Concept c=new FragDrawingEngine.Concept("quadrato");
        c.merge(FragDrawingEngine.extract("Un quadrato ha quattro lati uguali."));
        FragDrawingEngine.compile(c);
    }
    @Test(expected=IllegalArgumentException.class)
    public void refusesContradictoryInput() {
        FragDrawingEngine.Concept c=new FragDrawingEngine.Concept("quadrato");
        c.merge(FragDrawingEngine.extract("Un quadrato ha quattro lati uguali."));
        c.merge(FragDrawingEngine.extract("Un quadrato ha sei lati uguali."));
    }
    @Test public void deterministicGeneralizationToSize() {
        FragDrawingEngine.Concept c=new FragDrawingEngine.Concept("quadrato");
        c.merge(FragDrawingEngine.extract("Un quadrato ha 4 lati uguali e 4 angoli retti."));
        String small=FragDrawingEngine.compile(c).javaCode(10,10,80);
        String big=FragDrawingEngine.compile(c).javaCode(10,10,280);
        assertNotEquals(small,big);
        assertTrue(big.contains("canvas.drawPath"));
    }
}
