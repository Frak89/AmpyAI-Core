package com.ampyai.core;

import java.text.Normalizer;
import java.util.Random;

/**
 * Tiny causal neural language model: no built-in facts, training text or pre-trained weights.
 * Width can grow as public, user-visited concepts are saved. Growth alone is not knowledge.
 * Context window: exactly 6 characters, not a full transformer or a large LM.
 */
public final class PrimitiveLM {
    public static final int CONTEXT = 6;
    public static final int START_HIDDEN = 16;
    public static final int GROWTH_PER_CONCEPT = 4;
    public static final int MAX_HIDDEN = 256;
    public static final String ALPHABET =
            "\n abcdefghijklmnopqrstuvwxyz0123456789.,:;!?'-()";
    public static final int VOCAB = ALPHABET.length();

    public static final class Model {
        public final int hidden;
        public final float[][] input;
        public final float[] hb;
        public final float[][] output;
        public final float[] ob;
        public long processedCharacters;
        public int trainingSessions;
        public float lastLoss;
        Model(int hidden, long seed) {
            this.hidden=hidden;
            input=new float[hidden][CONTEXT*VOCAB];
            hb=new float[hidden];
            output=new float[VOCAB][hidden];
            ob=new float[VOCAB];
            Random rng=new Random(seed);
            for(int h=0;h<hidden;h++)
                for(int i=0;i<CONTEXT*VOCAB;i++)
                    input[h][i]=(float)(rng.nextGaussian()*0.025);
            for(int v=0;v<VOCAB;v++)
                for(int h=0;h<hidden;h++)
                    output[v][h]=(float)(rng.nextGaussian()*0.035);
        }
    }

    public static Model blank(){return new Model(START_HIDDEN,81284L);}
    public static int parameters(Model m){
        return m.hidden*CONTEXT*VOCAB+m.hidden+VOCAB*m.hidden+VOCAB;
    }
    public static Model expanded(Model old,int target) {
        if(target<old.hidden || target>MAX_HIDDEN || target%4!=0)
            throw new IllegalArgumentException("Dimensioni neurali non valide.");
        if(target==old.hidden)return old;
        Model n=new Model(target, 1800L+target);
        for(int h=0;h<old.hidden;h++){
            System.arraycopy(old.input[h],0,n.input[h],0,CONTEXT*VOCAB);
            n.hb[h]=old.hb[h];
        }
        for(int v=0;v<VOCAB;v++){
            System.arraycopy(old.output[v],0,n.output[v],0,old.hidden);
            n.ob[v]=old.ob[v];
        }
        n.processedCharacters=old.processedCharacters;
        n.trainingSessions=old.trainingSessions;
        n.lastLoss=old.lastLoss;
        return n;
    }

    public static int widthForConcepts(int concepts) {
        return Math.min(MAX_HIDDEN, START_HIDDEN+
                Math.max(0,concepts)*GROWTH_PER_CONCEPT);
    }
    public static String normalize(String value) {
        if(value==null)return "";
        String decomposed=Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}","");
        StringBuilder out=new StringBuilder();
        for(int i=0;i<decomposed.length() && out.length()<5000;i++){
            char c=decomposed.charAt(i);
            if(c=='\r' || c=='\t')c=' ';
            if(ALPHABET.indexOf(c)<0)c=' ';
            out.append(c);
        }
        return out.toString();
    }
    private static int id(char c){
        int v=ALPHABET.indexOf(c);
        return v<0?1:v;
    }
    private static int[] context(String s,int pos){
        int[] ids=new int[CONTEXT];
        for(int j=0;j<CONTEXT;j++){
            int at=pos-CONTEXT+j;
            ids[j]=at>=0?id(s.charAt(at)):0;
        }
        return ids;
    }
    private static void forward(Model m,int[] prior,float[] h,float[] z){
        for(int n=0;n<m.hidden;n++){
            float x=m.hb[n];
            for(int j=0;j<CONTEXT;j++)
                x+=m.input[n][j*VOCAB+prior[j]];
            h[n]=Math.max(0,x);
        }
        for(int v=0;v<VOCAB;v++){
            float x=m.ob[v];
            for(int n=0;n<m.hidden;n++)x+=m.output[v][n]*h[n];
            z[v]=x;
        }
    }
    private static float[] softmax(float[] z,float temperature){
        float max=z[0];for(float v:z)max=Math.max(max,v);
        float[] p=new float[VOCAB];double sum=0;
        for(int v=0;v<VOCAB;v++) {
            p[v]=(float)Math.exp((z[v]-max)/temperature);
            sum+=p[v];
        }
        for(int v=0;v<VOCAB;v++)p[v]/=(float)sum;
        return p;
    }
    public interface Listener {
        void onEpoch(int epoch, int epochs, float loss, int neurons);
    }
    /** Backprop learns only sequential next-character probabilities from the captured text. */
    public static void train(Model m,String raw,int epochs,Listener listener) {
        String sample=normalize(raw);
        if(sample.length()<80)throw new IllegalArgumentException("Pagina troppo breve.");
        if(epochs<1||epochs>10)throw new IllegalArgumentException("Epoche fuori limite.");
        int len=Math.min(sample.length(),1600);
        sample=sample.substring(0,len);
        float[] activations=new float[m.hidden],logits=new float[VOCAB];
        float[] grad=new float[VOCAB], hiddenGradient=new float[m.hidden];
        for(int ep=0;ep<epochs;ep++){
            double totalLoss=0;
            final float learningRate=0.015f/(1f+ep*.22f);
            for(int pos=0;pos<len;pos++){
                int[] prev=context(sample,pos),target=id(sample.charAt(pos));
                forward(m,prev,activations,logits);
                float[] p=softmax(logits,1.0f);
                totalLoss-=Math.log(Math.max(1e-8,p[target]));
                for(int v=0;v<VOCAB;v++)
                    grad[v]=p[v]-(v==target?1f:0f);
                for(int h=0;h<m.hidden;h++){
                    float g=0;
                    for(int v=0;v<VOCAB;v++)g+=grad[v]*m.output[v][h];
                    hiddenGradient[h]=activations[h]>0?g:0;
                }
                for(int v=0;v<VOCAB;v++){
                    for(int h=0;h<m.hidden;h++)
                        m.output[v][h]-=learningRate*grad[v]*activations[h];
                    m.ob[v]-=learningRate*grad[v];
                }
                for(int h=0;h<m.hidden;h++){
                    for(int j=0;j<CONTEXT;j++)
                        m.input[h][j*VOCAB+prev[j]]-=learningRate*hiddenGradient[h];
                    m.hb[h]-=learningRate*hiddenGradient[h];
                }
            }
            m.lastLoss=(float)(totalLoss/len);
            if(listener!=null)listener.onEpoch(ep+1,epochs,m.lastLoss,m.hidden);
        }
        m.trainingSessions++;
        m.processedCharacters+=(long)len*epochs;
    }
    public static final class Probe {
        public final int[] ids;
        public final float[] hidden;
        public final float[] probs;
        Probe(int[] ids,float[] hidden,float[] probs){
            this.ids=ids;this.hidden=hidden;this.probs=probs;
        }
    }
    public static Probe inspect(Model model,String seed){
        String normalized=normalize(seed);
        int[] ctx=context(normalized,normalized.length());
        float[] hidden=new float[model.hidden],logits=new float[VOCAB];
        forward(model,ctx,hidden,logits);
        return new Probe(ctx,hidden,softmax(logits,1f));
    }
    public static String generate(Model model,String seed,int count,long randomSeed){
        if(count<1||count>240)throw new IllegalArgumentException("Generazione troppo lunga.");
        StringBuilder history=new StringBuilder(normalize(seed)),out=new StringBuilder();
        Random random=new Random(randomSeed);
        float[] h=new float[model.hidden], z=new float[VOCAB];
        for(int k=0;k<count;k++){
            forward(model,context(history.toString(),history.length()),h,z);
            float[] p=softmax(z,.82f);float r=random.nextFloat(),total=0;int selected=VOCAB-1;
            for(int v=0;v<VOCAB;v++){
                total+=p[v];if(r<=total){selected=v;break;}
            }
            char c=ALPHABET.charAt(selected);
            out.append(c);history.append(c);
            if(history.length()>64)history.delete(0,history.length()-32);
        }
        return out.toString().trim();
    }
    private PrimitiveLM(){}
}
