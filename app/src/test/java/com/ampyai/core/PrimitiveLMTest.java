package com.ampyai.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PrimitiveLMTest {
    @Test public void startsBlankNotPretrained() {
        PrimitiveLM.Model m=PrimitiveLM.blank();
        assertEquals(16,m.hidden);
        assertEquals(0,m.trainingSessions);
        assertEquals(0,m.processedCharacters);
    }
    @Test public void growthPreservesPreviousWeights() {
        PrimitiveLM.Model m=PrimitiveLM.blank();
        float before=m.input[1][3],out=m.output[0][1];
        PrimitiveLM.Model expanded=PrimitiveLM.expanded(m,PrimitiveLM.widthForConcepts(3));
        assertEquals(28,expanded.hidden);
        assertEquals(before,expanded.input[1][3],0);
        assertEquals(out,expanded.output[0][1],0);
        assertEquals(16+PrimitiveLM.GROWTH_PER_CONCEPT*3,expanded.hidden);
        assertEquals(256,PrimitiveLM.widthForConcepts(10000));
    }
    @Test public void trainingMakesActualWeightChanges() {
        PrimitiveLM.Model m=PrimitiveLM.blank();
        float old=m.ob[2];
        String corpus="il quadrato ha quattro lati. il quadrato e una forma. "+
                "la memoria ricorda parole. la rana osserva una pagina.";
        PrimitiveLM.train(m,corpus,3,null);
        assertEquals(1,m.trainingSessions);
        assertTrue(m.lastLoss>0 && Float.isFinite(m.lastLoss));
        assertNotEquals(old,m.ob[2],1e-7);
        assertTrue(PrimitiveLM.generate(m,"rana",50,29L).length()>0);
    }
    @Test public void inspectProbabilitiesSumToOne() {
        PrimitiveLM.Model m=PrimitiveLM.blank();
        PrimitiveLM.Probe p=PrimitiveLM.inspect(m,"ciao");
        assertEquals(m.hidden,p.hidden.length);
        float sum=0;for(float q:p.probs)sum+=q;
        assertEquals(1f,sum,.001f);
    }
    @Test public void statsDerivedFromRealArchitecture(){
        PrimitiveLM.Model m=PrimitiveLM.expanded(PrimitiveLM.blank(),32);
        int expected=32*PrimitiveLM.CONTEXT*PrimitiveLM.VOCAB+
                32+PrimitiveLM.VOCAB*32+PrimitiveLM.VOCAB;
        assertEquals(expected,PrimitiveLM.parameters(m));
    }
}
