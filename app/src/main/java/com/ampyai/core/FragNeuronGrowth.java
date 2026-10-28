package com.ampyai.core;

/**
 * Neural growth policy. Manual input is an exact integer, not only multiples of 4.
 * Auto mode expands on newly indexed concepts and, when learning stalls, by a step
 * proportional to its current size. This measures training loss, not intelligence.
 */
public final class FragNeuronGrowth {
    public static final int DEFAULT_AUTO_LIMIT=2048;
    public static final int MIN=PrimitiveLM.START_HIDDEN;
    public static final int MAX=PrimitiveLM.MAX_HIDDEN;

    public static final class Decision {
        public final int target;
        public final int stalled;
        public final String why;
        Decision(int target,int stalled,String why){
            this.target=target;this.stalled=stalled;this.why=why;
        }
    }

    public static int safeMaximum(long javaHeapBytes){
        // Expansion holds old and new weights; reserve the rest for UI/WebView, PNG and Android.
        long budget=Math.max(8L*1024*1024,javaHeapBytes/5);
        for(int n=MAX;n>=MIN;n--){
            if(PrimitiveLM.estimatedBytes(n)<=budget)return n;
        }
        return MIN;
    }

    public static void requireManual(int requested,int current,int safeMax) {
        if(requested<MIN || requested>MAX)
            throw new IllegalArgumentException("Scegli da "+MIN+" a "+MAX+" neuroni.");
        if(requested<current)
            throw new IllegalArgumentException("Il modello ne ha già "+current+
                    ". Per conservarne i pesi non posso ridurli senza un reset.");
        if(requested>safeMax)
            throw new IllegalArgumentException(
                "Limite di memoria stimato su questo telefono: "+safeMax+
                ". Scegli un numero inferiore.");
    }

    public static int conceptTarget(int current,int concepts,int autoCap){
        return Math.max(current,Math.min(autoCap,PrimitiveLM.widthForConcepts(concepts)));
    }

    /**
     * No growth while the recent data are learning sufficiently.
     * Two successive slow-improvement sessions with substantial remaining
     * loss are needed; after a step the counter resets.
     */
    public static Decision afterTraining(int current,int cap,int previousStalled,
                                          float before,float after) {
        if(!Float.isFinite(before)||!Float.isFinite(after)||before<=0||after<0)
            return new Decision(current,0,"Loss non affidabile");
        float gain=(before-after)/Math.max(0.001f,before);
        boolean plateau=after>1.8f && gain<0.025f;
        int count=plateau?Math.min(2,previousStalled+1):0;
        if(!plateau)return new Decision(current,0,"Sta migliorando sui testi");
        if(count<2)return new Decision(current,count,"Primo stallo osservato");
        if(current>=cap)return new Decision(current,2,"Limite configurato raggiunto");
        int increment=Math.max(16,((current+19)/20));
        int target=Math.min(cap,current+increment);
        return new Decision(target,0,"Due sessioni di training quasi ferme: +"+
                (target-current)+" neuroni");
    }

    private FragNeuronGrowth(){}
}
