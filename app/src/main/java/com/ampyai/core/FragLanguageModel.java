package com.ampyai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A tiny ORIGINAL causal neural language model, trained from scratch.
 * Each prediction uses four preceding characters, a trainable ReLU layer,
 * and a softmax distribution over the next character.
 *
 * This is a genuine autoregressive neural language model, but NOT an LLM:
 * its four-character context cannot capture deep meaning or hold a conversation.
 */
public final class FragLanguageModel {
    public static final int CONTEXT = 4;
    public static final int HIDDEN = 64;
    public static final String ALPHABET =
            "\n abcdefghijklmnopqrstuvwxyzàèéìòù0123456789.,:;!?'-()";
    public static final int VOCAB = ALPHABET.length();

    public static final class Model {
        public final float[][] input = new float[HIDDEN][CONTEXT * VOCAB];
        public final float[] bias = new float[HIDDEN];
        public final float[][] output = new float[VOCAB][HIDDEN];
        public final float[] outputBias = new float[VOCAB];
        public long charactersSeen;
        public int sessions;
        public float lastLoss;
        public Model() {
            Random rnd = new Random(816L);
            for (float[] row : input)
                for (int j=0; j<row.length;j++)
                    row[j] = (float)(rnd.nextGaussian()*0.04);
            for (float[] row : output)
                for (int j=0; j<row.length;j++)
                    row[j] = (float)(rnd.nextGaussian()*0.06);
        }
    }

    public interface Progress {
        /** Called by the worker at the end of each epoch, never on the UI thread. */
        void epoch(int index, int total, float loss, Model snapshot);
    }

    public static Model copy(Model source) {
        Model result = new Model();
        for(int h=0;h<HIDDEN;h++) {
            System.arraycopy(source.input[h],0,result.input[h],0,CONTEXT*VOCAB);
            result.bias[h]=source.bias[h];
        }
        for(int v=0;v<VOCAB;v++) {
            System.arraycopy(source.output[v],0,result.output[v],0,HIDDEN);
            result.outputBias[v]=source.outputBias[v];
        }
        result.charactersSeen=source.charactersSeen;
        result.sessions=source.sessions;
        result.lastLoss=source.lastLoss;
        return result;
    }

    public static int parameterCount() {
        return HIDDEN*CONTEXT*VOCAB+HIDDEN+VOCAB*HIDDEN+VOCAB;
    }

    public static String normalize(String raw) {
        if(raw==null) return "";
        String source=raw.toLowerCase(Locale.ITALIAN).replace('\r','\n');
        StringBuilder b=new StringBuilder();
        for(int i=0;i<source.length() && b.length()<30000;i++) {
            char ch=source.charAt(i);
            if(ch=='\t') ch=' ';
            if(ALPHABET.indexOf(ch)<0) ch=' ';
            b.append(ch);
        }
        return b.toString();
    }

    private static int charId(char ch) {
        int id=ALPHABET.indexOf(ch);
        return id<0?1:id;
    }

    private static int[] context(String s, int offset) {
        int[] v=new int[CONTEXT];
        for(int i=0;i<CONTEXT;i++) {
            int at=offset-CONTEXT+i;
            v[i]= at<0 ? charId('\n') : charId(s.charAt(at));
        }
        return v;
    }

    private static void forward(Model model,int[] prev,float[] hidden,float[] logits) {
        for(int h=0;h<HIDDEN;h++) {
            float z=model.bias[h];
            for(int i=0;i<CONTEXT;i++) z+=model.input[h][i*VOCAB+prev[i]];
            hidden[h]=Math.max(0,z);
        }
        for(int v=0;v<VOCAB;v++) {
            float z=model.outputBias[v];
            for(int h=0;h<HIDDEN;h++) z+=model.output[v][h]*hidden[h];
            logits[v]=z;
        }
    }

    private static float[] softmax(float[] logits,float temperature) {
        float[] probs=new float[VOCAB];
        double sum=0;
        float high=logits[0];
        for(float value:logits) high=Math.max(high,value);
        for(int v=0;v<VOCAB;v++) {
            probs[v]=(float)Math.exp((logits[v]-high)/temperature);
            sum+=probs[v];
        }
        for(int v=0;v<VOCAB;v++) probs[v]/=(float)sum;
        return probs;
    }

    /** Fine-tunes EXISTING weights. Caller should pass copy(model) to avoid UI races. */
    public static Model train(Model model, String corpus, int epochs, Progress listener) {
        if(epochs<1 || epochs>30) throw new IllegalArgumentException("Epoche: 1..30");
        String text=normalize(corpus);
        if(text.length()<80) throw new IllegalArgumentException(
                "Occorrono almeno 80 caratteri di testo per addestrare il Language Core.");
        if(text.length()>30000) throw new IllegalArgumentException("Testo troppo lungo.");
        int[] ids=new int[text.length()];
        for(int i=0;i<ids.length;i++) ids[i]=charId(text.charAt(i));
        float[] hidden=new float[HIDDEN];
        float[] logits=new float[VOCAB];
        float[] dhidden=new float[HIDDEN];
        float[] grad=new float[VOCAB];
        for(int ep=0;ep<epochs;ep++) {
            double loss=0;
            float lr=(float)(0.016/(1+ep*0.14));
            for(int pos=0;pos<ids.length;pos++) {
                int[] prev=context(text,pos);
                forward(model,prev,hidden,logits);
                float[] prob=softmax(logits,1.0f);
                loss-=Math.log(Math.max(1e-8f,prob[ids[pos]]));
                for(int v=0;v<VOCAB;v++)
                    grad[v]=prob[v]-(v==ids[pos]?1f:0f);
                for(int h=0;h<HIDDEN;h++) {
                    float sum=0;
                    for(int v=0;v<VOCAB;v++) sum+=model.output[v][h]*grad[v];
                    dhidden[h]=hidden[h]>0?sum:0;
                }
                for(int v=0;v<VOCAB;v++) {
                    for(int h=0;h<HIDDEN;h++) model.output[v][h]-=lr*grad[v]*hidden[h];
                    model.outputBias[v]-=lr*grad[v];
                }
                for(int h=0;h<HIDDEN;h++) {
                    for(int k=0;k<CONTEXT;k++)
                        model.input[h][k*VOCAB+prev[k]]-=lr*dhidden[h];
                    model.bias[h]-=lr*dhidden[h];
                }
            }
            model.lastLoss=(float)(loss/ids.length);
            if(listener!=null) listener.epoch(ep+1,epochs,model.lastLoss,model);
        }
        model.charactersSeen+=(long)text.length()*epochs;
        model.sessions++;
        return model;
    }

    /**
     * Samples new characters ONLY from the neural distribution.
     * Words are not copied from a database; this is a model-generated continuation.
     */
    public static String generate(Model model,String prompt,int length,long seed) {
        if(length<1 || length>240) throw new IllegalArgumentException("Max 240 caratteri.");
        String prefix=normalize(prompt);
        StringBuilder history=new StringBuilder(prefix);
        StringBuilder output=new StringBuilder();
        Random random=new Random(seed);
        float[] h=new float[HIDDEN];
        float[] logits=new float[VOCAB];
        for(int i=0;i<length;i++) {
            int[] prev=context(history.toString(),history.length());
            forward(model,prev,h,logits);
            float[] dist=softmax(logits,0.83f);
            // Avoid generating a long run of blank spaces.
            float cumulative=0f;
            float chosen=random.nextFloat();
            int selected=VOCAB-1;
            for(int v=0;v<VOCAB;v++) {
                cumulative+=dist[v];
                if(chosen<=cumulative) {selected=v;break;}
            }
            char next=ALPHABET.charAt(selected);
            output.append(next);
            history.append(next);
            if(history.length()>400) history.delete(0,history.length()-80);
        }
        return output.toString().trim();
    }

    public static final class Probe {
        public final int[] inputIds;
        public final float[] activations;
        public final float[] probabilities;
        public Probe(int[] ids,float[] h,float[] probs) {
            this.inputIds=ids;this.activations=h;this.probabilities=probs;
        }
    }

    public static Probe inspect(Model model,String prompt) {
        String normalized=normalize(prompt);
        int[] ids=context(normalized,normalized.length());
        float[] h=new float[HIDDEN];
        float[] logits=new float[VOCAB];
        forward(model,ids,h,logits);
        return new Probe(ids,h,softmax(logits,1.0f));
    }
    private FragLanguageModel() {}
}
