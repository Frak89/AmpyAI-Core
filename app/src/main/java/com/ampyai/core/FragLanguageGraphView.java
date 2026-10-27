package com.ampyai.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.Arrays;
import java.util.Locale;

/**
 * A sampled ACTUAL view of the tiny generative LM, separate from the UI classifier.
 * Each displayed weight is read directly from the language model matrices.
 */
public final class FragLanguageGraphView extends View {
    private final Paint paint=new Paint(3);
    private FragLanguageModel.Model model;
    private String phrase="ciao";
    private final float scale;
    public FragLanguageGraphView(Context ctx) {
        super(ctx);
        scale=ctx.getResources().getDisplayMetrics().density;
        setMinimumHeight(dp(330));
    }
    public void display(FragLanguageModel.Model m,String text) {
        model=m; phrase=text; invalidate();
    }
    @Override protected void onMeasure(int w,int h) {
        setMeasuredDimension(MeasureSpec.getSize(w),dp(338));
    }
    private int dp(float v){return (int)(v*scale+.5f);}
    @Override protected void onDraw(Canvas c) {
        c.drawColor(0xFF12212A);
        float W=getWidth();
        label(c,"FRÅG LANGUAGE CORE / GENERAZIONE",dp(12),dp(26),0xFF92E8C7,13);
        label(c,"4 caratteri → 64 neuroni ReLU → "+FragLanguageModel.VOCAB+
                " caratteri",dp(12),dp(48),0xFFD0E4DF,10);
        label(c,model==null?"NON ADDESTRATO":"PESI REALI DEL MODELLO",
                dp(12),dp(69),model==null?0xFFF0B495:0xFF8FE6B7,11);
        if(model==null) {
            label(c,"Scrivi «inizia linguaggio» per addestrarlo.",dp(12),dp(108),0xFFBCD7D8,12);
            return;
        }
        FragLanguageModel.Probe probe=FragLanguageModel.inspect(model,phrase);
        float left=dp(50),mid=W*.49f,right=W-dp(68),top=dp(108),step=dp(20);
        int[] topOutputs=sortedTop(probe.probabilities,7);
        for(int k=0;k<4;k++) {
            float y=top+k*step*2.5f;
            String name=FragLanguageModel.ALPHABET.substring(probe.inputIds[k],
                                probe.inputIds[k]+1).replace("\n","↵").replace(" ","␣");
            label(c,name,left-dp(28),y+dp(3),0xFFACD8FA,12);
            node(c,left,y,0xFF77B9ED);
            for(int h=0;h<12;h++) {
                float hh=top+h*dp(17);
                float weight=model.input[h][k*FragLanguageModel.VOCAB+probe.inputIds[k]];
                link(c,left,y,mid,hh,weight);
            }
        }
        for(int h=0;h<12;h++) {
            float y=top+h*dp(17);
            node(c,mid,y,probe.activations[h]>0?0xFFAE91E2:0xFF516077);
            for(int o=0;o<topOutputs.length;o++) {
                float yy=top+o*dp(29);
                link(c,mid,y,right,yy,model.output[topOutputs[o]][h]);
            }
        }
        for(int o=0;o<topOutputs.length;o++) {
            int id=topOutputs[o];
            float y=top+o*dp(29);
            node(c,right,y,0xFF6ED6AE);
            char ch=FragLanguageModel.ALPHABET.charAt(id);
            label(c,(ch=='\n'?"↵":ch==' '?"␣":String.valueOf(ch))
                      + " " + String.format(Locale.US,"%.0f%%",probe.probabilities[id]*100),
                      right+dp(8),y+dp(4),0xFFD9EEE8,10);
        }
        label(c,"Disegnati 12/64 neuroni e 7/"+FragLanguageModel.VOCAB+
                " output.",dp(12),dp(321),0xFFBDD2D6,10);
    }
    private int[] sortedTop(float[] probs,int count){
        Integer[] ids=new Integer[probs.length];for(int i=0;i<ids.length;i++)ids[i]=i;
        Arrays.sort(ids,(a,b)->Float.compare(probs[b],probs[a]));
        int[] out=new int[count];for(int i=0;i<count;i++)out[i]=ids[i];
        return out;
    }
    private void link(Canvas c,float x,float y,float xx,float yy,float weight){
        int alpha=Math.min(200,28+(int)(Math.abs(weight)*800));
        paint.setColor((alpha<<24)|(weight>=0?0x005BC59F:0x00DB7091));
        paint.setStrokeWidth(dp(.6f));
        c.drawLine(x,y,xx,yy,paint);
    }
    private void node(Canvas c,float x,float y,int color){
        paint.setColor(color);
        c.drawCircle(x,y,dp(3.6f),paint);
    }
    private void label(Canvas c,String s,float x,float y,int color,int size){
        paint.setColor(color);paint.setTextSize(dp(size));paint.setTypeface(
                android.graphics.Typeface.DEFAULT);
        c.drawText(s,x,y,paint);
    }
}
