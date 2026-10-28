package com.ampyai.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FragNeuronGrowthTest {
    @Test public void exactManualTargetsAreNotMultiplesOnly(){
        PrimitiveLM.Model m=PrimitiveLM.expanded(PrimitiveLM.blank(),777);
        assertEquals(777,m.hidden);
        assertEquals(777,PrimitiveLM.expanded(m,777).hidden);
        assertTrue(FragNeuronGrowth.safeMaximum(768L*1024*1024)>256);
    }
    @Test public void manualRetainsOldWeightsAndCount(){
        PrimitiveLM.Model m=PrimitiveLM.blank();
        float old=m.input[5][2],other=m.output[7][5];
        m.processedCharacters=234;m.trainingSessions=4;
        PrimitiveLM.Model grown=PrimitiveLM.expanded(m,513);
        assertEquals(513,grown.hidden);
        assertEquals(old,grown.input[5][2],0);
        assertEquals(other,grown.output[7][5],0);
        assertEquals(234,grown.processedCharacters);
        assertEquals(4,grown.trainingSessions);
    }
    @Test public void autosizeGrowsAbove256(){
        int target=FragNeuronGrowth.conceptTarget(256,90,4096);
        assertEquals(376,target);
        assertEquals(1024,FragNeuronGrowth.conceptTarget(512,10000,1024));
    }
    @Test public void autoRequiresTwoPlateaus(){
        FragNeuronGrowth.Decision one=FragNeuronGrowth.afterTraining(512,2048,0,3.0f,2.99f);
        assertEquals(512,one.target);
        assertEquals(1,one.stalled);
        FragNeuronGrowth.Decision next=FragNeuronGrowth.afterTraining(512,2048,one.stalled,3.0f,2.99f);
        assertTrue(next.target>512);
        assertEquals(0,next.stalled);
        FragNeuronGrowth.Decision improved=FragNeuronGrowth.afterTraining(512,2048,1,3.0f,2.1f);
        assertEquals(512,improved.target);
        assertEquals(0,improved.stalled);
    }
    @Test public void automaticNeverExceedsLimit(){
        FragNeuronGrowth.Decision out=FragNeuronGrowth.afterTraining(999,1000,1,3.2f,3.2f);
        assertEquals(1000,out.target);
    }
    @Test(expected=IllegalArgumentException.class)
    public void refuseDestructiveShrink(){FragNeuronGrowth.requireManual(60,300,2048);}
}
