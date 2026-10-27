package com.ampyai.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FragLanguageModelTest {
    @Test public void neuralLanguageReallyLearnsAndGenerates() {
        FragLanguageModel.Model original=new FragLanguageModel.Model();
        float old=original.output[0][0];
        String sample=FragLanguageSeeds.TEXT;
        FragLanguageModel.Model trained=FragLanguageModel.train(
                original,sample,3,null);
        assertEquals(1,trained.sessions);
        assertEquals((long)FragLanguageModel.normalize(sample).length()*3,
                trained.charactersSeen);
        assertTrue(Float.isFinite(trained.lastLoss));
        assertTrue(trained.lastLoss>0);
        assertNotEquals(old,trained.output[0][0],1e-7);
        String generated=FragLanguageModel.generate(trained,"ciao",70,13L);
        assertTrue(generated.length()>0);
        assertTrue(generated.length()<=70);
        FragLanguageModel.Probe state=FragLanguageModel.inspect(trained,"ciao");
        assertEquals(4,state.inputIds.length);
        assertEquals(64,state.activations.length);
        assertEquals(FragLanguageModel.VOCAB,state.probabilities.length);
        float total=0;for(float p:state.probabilities) total+=p;
        assertEquals(1f,total,0.001f);
    }

    @Test public void newTrainingContinuesExistingWeights() {
        FragLanguageModel.Model original=new FragLanguageModel.Model();
        FragLanguageModel.Model first=FragLanguageModel.train(
                original,FragLanguageSeeds.TEXT,1,null);
        FragLanguageModel.Model copy=FragLanguageModel.copy(first);
        assertEquals(first.charactersSeen,copy.charactersSeen);
        assertEquals(first.input[10][14],copy.input[10][14],0f);
        FragLanguageModel.Model second=FragLanguageModel.train(
                copy,FragLanguageSeeds.TEXT,1,null);
        assertEquals(2,second.sessions);
        assertEquals(first.charactersSeen*2,second.charactersSeen);
        assertNotEquals(first.output[0][0],second.output[0][0],1e-8);
    }

    @Test public void dictionaryParserRejectsMissingDefinition() {
        assertEquals("",FragWiktionaryTutor.extractDefinitions("== {{-it-}} ==\n=== note ===\n"));
    }

    @Test public void dictionaryParserExtractsPlainMeaningAndAttributionReady() {
        String wikitext="== {{-it-}} ==\n{{-sost-}}\n# [[capacità]] di ricordare\n" +
                        "# parte dell'[[intelletto]] che conserva esperienze\n";
        String out=FragWiktionaryTutor.extractDefinitions(wikitext);
        assertTrue(out.contains("capacità di ricordare"));
        assertTrue(out.contains("intelletto"));
    }
}
