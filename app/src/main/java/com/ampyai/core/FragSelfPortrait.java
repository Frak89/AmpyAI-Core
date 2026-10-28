package com.ampyai.core;

/**
 * Transparent geometry template: user can replace it with Frap code.
 * Evolves in complexity with acquired memory but does not claim artistic reasoning.
 */
public final class FragSelfPortrait {
    public static String script(int concepts,int lessons){
        StringBuilder b=new StringBuilder();
        b.append("# Fråg · autoritratto procedurale, non immagine imparata\n");
        b.append("circle 0.50 0.51 0.32\n");
        b.append("circle 0.34 0.30 0.09\n");
        b.append("circle 0.66 0.30 0.09\n");
        b.append("circle 0.34 0.30 0.035\n");
        b.append("circle 0.66 0.30 0.035\n");
        b.append("line 0.33 0.64 0.50 0.70\n");
        b.append("line 0.50 0.70 0.67 0.64\n");
        int marks=Math.min(8,(concepts+lessons)/3);
        for(int i=0;i<marks;i++){
            float x=.20f+i*.085f;
            b.append(String.format(java.util.Locale.US,"line %.3f 0.85 %.3f 0.91\n",x,x));
        }
        return b.toString();
    }
    private FragSelfPortrait(){}
}
