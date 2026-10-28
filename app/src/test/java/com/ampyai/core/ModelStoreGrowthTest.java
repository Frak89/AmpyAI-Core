package com.ampyai.core;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public final class ModelStoreGrowthTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();

    @Test public void saveAndReloadOddNumberOfHiddenNeurons() throws Exception {
        ModelStore memory=new ModelStore(folder.newFolder());
        PrimitiveLM.Model original=PrimitiveLM.expanded(PrimitiveLM.blank(),777);
        original.input[0][15]=.423f;
        original.output[4][5]=-.56f;
        original.trainingSessions=3;
        original.processedCharacters=983L;
        memory.save(original);
        PrimitiveLM.Model restored=memory.load();
        assertEquals(777,restored.hidden);
        assertEquals(original.input[0][15],restored.input[0][15],0f);
        assertEquals(original.output[4][5],restored.output[4][5],0f);
        assertEquals(3,restored.trainingSessions);
        assertEquals(983L,restored.processedCharacters);
    }

    @Test public void canSaveLargerModelAndIncreaseAgain() throws Exception {
        ModelStore memory=new ModelStore(folder.newFolder());
        PrimitiveLM.Model first=PrimitiveLM.expanded(PrimitiveLM.blank(),1024);
        first.hb[111]=1.235f;
        memory.save(first);
        PrimitiveLM.Model updated=PrimitiveLM.expanded(memory.load(),1205);
        memory.save(updated);
        PrimitiveLM.Model restored=memory.load();
        assertEquals(1205,restored.hidden);
        assertEquals(1.235f,restored.hb[111],0f);
    }
}
