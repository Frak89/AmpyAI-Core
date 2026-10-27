package com.ampyai.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public final class FragNeuralTrainerTest {
    @Test public void recognizesHumanLabels() {
        assertEquals(0, FragNeuralTrainer.label("pulsante"));
        assertEquals(5, FragNeuralTrainer.label("LOGIN"));
        assertEquals(6, FragNeuralTrainer.label("cruscotto"));
        assertEquals(-1, FragNeuralTrainer.label("something-unknown"));
    }

    @Test public void performsRealWeightUpdatesAndValidPrediction() {
        String[] phrases = {
            "aggiungi pulsante con scritto", "metti titolo con scritto",
            "scrivi testo con contenuto", "campo compilabile chiamato",
            "aggiungi scheda informativa chiamata", "pagina login accesso per",
            "pagina dashboard riepilogo con"
        };
        List<FragNeuralTrainer.Example> examples = new ArrayList<>();
        for (int label = 0; label < phrases.length; label++) {
            for (int j = 0; j < 6; j++) {
                examples.add(new FragNeuralTrainer.Example(
                    phrases[label] + " dato" + j, label));
            }
        }
        FragNeuralTrainer.Model initial = new FragNeuralTrainer.Model();
        float original = initial.output[0][0];
        FragNeuralTrainer.Model trained = FragNeuralTrainer.train(examples, null);
        assertEquals(42, trained.examples);
        assertEquals(65, trained.epochs);
        assertTrue(Float.isFinite(trained.output[0][0]));
        assertNotEquals(original, trained.output[0][0], 1e-8);
        assertTrue(trained.validationAccuracy >= 0.0);
        assertTrue(trained.validationAccuracy <= 1.0);
        FragNeuralTrainer.Prediction prediction =
            FragNeuralTrainer.predict(trained, "aggiungi pulsante con scritto Conferma");
        assertNotNull(prediction.label);
        assertTrue(prediction.score > 0);
        assertTrue(prediction.score <= 1);
        assertTrue(prediction.margin >= 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesTooSmallDataset() {
        FragNeuralTrainer.train(new ArrayList<>(), null);
    }
}
