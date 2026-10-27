package com.ampyai.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * First real neural component created from scratch for Fråg V2:
 * hashed word/subword features -> trainable ReLU hidden layer -> softmax intent.
 * Training uses supervised SGD/backpropagation on data provided by the user.
 * Not an LLM, not pretrained, not a general reasoner.
 */
public final class FragNeuralTrainer {
    public static final int FEATURES = 384;
    public static final int HIDDEN = 32;
    public static final String[] LABELS = {
        "button", "title", "text", "input", "card", "login", "dashboard"
    };
    private static final int OUTPUTS = LABELS.length;

    public static final class Example {
        public final String text;
        public final int label;
        public Example(String text, int label) {
            this.text = text;
            this.label = label;
        }
    }

    public static final class Model {
        public final float[][] hidden = new float[HIDDEN][FEATURES];
        public final float[] hiddenBias = new float[HIDDEN];
        public final float[][] output = new float[OUTPUTS][HIDDEN];
        public final float[] outputBias = new float[OUTPUTS];
        public int examples;
        public int epochs;
        public double validationAccuracy;

        public Model() {
            Random r = new Random(20261027L);
            for (int h = 0; h < HIDDEN; h++)
                for (int f = 0; f < FEATURES; f++)
                    hidden[h][f] = (float)(r.nextGaussian() * 0.055);
            for (int c = 0; c < OUTPUTS; c++)
                for (int h = 0; h < HIDDEN; h++)
                    output[c][h] = (float)(r.nextGaussian() * 0.10);
        }
    }

    public static final class Prediction {
        public final String label;
        public final double score;
        public final double margin;
        public Prediction(String label, double score, double margin) {
            this.label = label;
            this.score = score;
            this.margin = margin;
        }
    }

    public interface Listener { void progress(int epoch, int total, double trainLoss); }

    public static int label(String raw) {
        if (raw == null) return -1;
        switch (raw.toLowerCase(Locale.ROOT).trim()) {
            case "pulsante": case "bottone": case "tasto": case "button": return 0;
            case "titolo": case "heading": case "title": return 1;
            case "testo": case "scritta": case "text": return 2;
            case "campo": case "input": case "form": return 3;
            case "scheda": case "card": case "riquadro": return 4;
            case "login": case "accesso": return 5;
            case "dashboard": case "cruscotto": return 6;
            default: return -1;
        }
    }

    /**
     * Deterministic supervised holdout: stratified by class, approximately 20%.
     * Validation samples are NEVER used for gradient updates.
     */
    public static Model train(List<Example> provided, Listener listener) {
        if (provided.size() < 21) throw new IllegalArgumentException("Servono almeno 21 esempi.");
        List<Example> train = new ArrayList<>();
        List<Example> validation = new ArrayList<>();
        for (int c = 0; c < OUTPUTS; c++) {
            List<Example> group = new ArrayList<>();
            for (Example sample : provided) if (sample.label == c) group.add(sample);
            if (group.size() < 3) continue;
            Collections.shuffle(group, new Random(784L + c));
            int held = Math.max(1, group.size() / 5);
            validation.addAll(group.subList(0, held));
            train.addAll(group.subList(held, group.size()));
        }
        if (train.size() < 14 || validation.size() < 2)
            throw new IllegalArgumentException("Occorrono almeno 3 esempi per categoria e almeno 21 totali.");
        Model model = new Model();
        Random random = new Random(1414L);
        final int epochs = 65;
        for (int epoch = 1; epoch <= epochs; epoch++) {
            Collections.shuffle(train, random);
            double loss = 0;
            // Decay learning rate; small enough for stable training on-device.
            float rate = (float)(0.065 / (1.0 + (epoch - 1) * 0.015));
            for (Example example : train) {
                float[] input = vector(example.text);
                float[] hidden = new float[HIDDEN];
                float[] logits = new float[OUTPUTS];
                forward(model, input, hidden, logits);
                float[] probs = softmax(logits);
                loss += -Math.log(Math.max(1e-8f, probs[example.label]));

                // Backpropagate before modifying output weights.
                float[] outGrad = new float[OUTPUTS];
                for (int c = 0; c < OUTPUTS; c++)
                    outGrad[c] = probs[c] - (c == example.label ? 1f : 0f);
                float[] hiddenGrad = new float[HIDDEN];
                for (int h = 0; h < HIDDEN; h++) {
                    float grad = 0;
                    for (int c = 0; c < OUTPUTS; c++)
                        grad += model.output[c][h] * outGrad[c];
                    hiddenGrad[h] = hidden[h] > 0 ? grad : 0;
                }
                for (int c = 0; c < OUTPUTS; c++) {
                    for (int h = 0; h < HIDDEN; h++)
                        model.output[c][h] -= rate * outGrad[c] * hidden[h];
                    model.outputBias[c] -= rate * outGrad[c];
                }
                for (int h = 0; h < HIDDEN; h++) {
                    for (int f = 0; f < FEATURES; f++) {
                        if (input[f] != 0f)
                            model.hidden[h][f] -= rate * hiddenGrad[h] * input[f];
                    }
                    model.hiddenBias[h] -= rate * hiddenGrad[h];
                }
            }
            if (listener != null && (epoch == 1 || epoch % 10 == 0 || epoch == epochs))
                listener.progress(epoch, epochs, loss / train.size());
        }
        int right = 0;
        for (Example e : validation) {
            if (LABELS[e.label].equals(predict(model, e.text).label)) right++;
        }
        model.examples = provided.size();
        model.epochs = epochs;
        model.validationAccuracy = (double) right / validation.size();
        return model;
    }

    public static Prediction predict(Model model, String text) {
        float[] hidden = new float[HIDDEN];
        float[] logits = new float[OUTPUTS];
        forward(model, vector(text), hidden, logits);
        float[] probabilities = softmax(logits);
        int best = 0;
        float runnerUp = 0f;
        for (int i = 1; i < OUTPUTS; i++) {
            if (probabilities[i] > probabilities[best]) best = i;
        }
        for (int i = 0; i < OUTPUTS; i++) {
            if (i != best) runnerUp = Math.max(runnerUp, probabilities[i]);
        }
        return new Prediction(LABELS[best], probabilities[best],
                probabilities[best] - runnerUp);
    }

    private static void forward(Model model, float[] x, float[] hidden, float[] logits) {
        for (int h = 0; h < HIDDEN; h++) {
            float z = model.hiddenBias[h];
            for (int f = 0; f < FEATURES; f++)
                if (x[f] != 0f) z += x[f] * model.hidden[h][f];
            hidden[h] = Math.max(0f, z);
        }
        for (int c = 0; c < OUTPUTS; c++) {
            float z = model.outputBias[c];
            for (int h = 0; h < HIDDEN; h++) z += model.output[c][h] * hidden[h];
            logits[c] = z;
        }
    }

    private static float[] softmax(float[] logits) {
        float peak = logits[0];
        for (float v : logits) peak = Math.max(peak, v);
        double sum = 0;
        float[] values = new float[OUTPUTS];
        for (int i = 0; i < OUTPUTS; i++) {
            values[i] = (float)Math.exp(logits[i] - peak);
            sum += values[i];
        }
        for (int i = 0; i < OUTPUTS; i++) values[i] /= (float)sum;
        return values;
    }

    private static float[] vector(String source) {
        String normalized = Normalizer.normalize(source.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String[] words = normalized.split("[^a-z0-9]+");
        Set<String> features = new LinkedHashSet<>();
        String previous = "";
        for (String word : words) {
            if (word.length() < 2 || word.length() > 40) continue;
            features.add("w:" + word);
            features.add("p:" + word.substring(0, Math.min(4, word.length())));
            if (!previous.isEmpty()) features.add("b:" + previous + "+" + word);
            for (int i = 0; i + 3 <= word.length(); i++)
                features.add("c:" + word.substring(i, i + 3));
            previous = word;
            if (features.size() >= 96) break;
        }
        float[] result = new float[FEATURES];
        for (String feature : features) {
            int position = (feature.hashCode() & 0x7fffffff) % FEATURES;
            result[position] += 1f;
        }
        float norm = 0;
        for (float value : result) norm += value * value;
        norm = (float)Math.sqrt(Math.max(1, norm));
        for (int i = 0; i < result.length; i++) result[i] /= norm;
        return result;
    }

    private FragNeuralTrainer() {}
}
