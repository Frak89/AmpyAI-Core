package com.ampyai.core;

import java.util.Locale;
import java.util.Random;

/**
 * Educational procedural PCM sound generator. No copyrighted melodies/presets.
 * The timbres are approximations, not recordings of real instruments.
 */
public final class FragMusicLab {
    public static final int RATE=22050;
    public static final String[] INSTRUMENTS={
        "basso","chitarra","batteria","pianoforte","sax tenore","sax contralto"
    };
    private static final String[] PITCHES={
        "C","C#","D","D#","E","F","F#","G","G#","A","A#","B"
    };
    public static double pitch(String note){
        String s=note.trim().toUpperCase(Locale.ROOT);
        if(!s.matches("[A-G]#?[1-6]"))
            throw new IllegalArgumentException("Nota non valida: "+note+
                    ". Esempio: C3 E3 G3.");
        int octave=s.charAt(s.length()-1)-'0';
        String key=s.substring(0,s.length()-1);
        int degree=-1;
        for(int i=0;i<PITCHES.length;i++)if(key.equals(PITCHES[i]))degree=i;
        if(degree<0)throw new IllegalArgumentException("Nota non valida.");
        int midi=(octave+1)*12+degree;
        return 440.0*Math.pow(2.0,(midi-69)/12.0);
    }
    public static String[] parseNotes(String sequence){
        if(sequence==null||sequence.length()>200)
            throw new IllegalArgumentException("Sequenza troppo lunga.");
        String[] tokens=sequence.trim().split("[\\s,;]+");
        if(tokens.length<1||tokens.length>24)
            throw new IllegalArgumentException("Servono 1-24 note.");
        for(String token:tokens)pitch(token);
        return tokens;
    }
    public static String variation(String sequence,int variationSeed){
        String[] notes=parseNotes(sequence);
        Random random=new Random(variationSeed);
        StringBuilder out=new StringBuilder();
        // Recombine only provided notes, never pretend to have composed
        // an independently learned musical style.
        for(int i=0;i<Math.min(12,notes.length+4);i++){
            if(i>0)out.append(" ");
            out.append(notes[random.nextInt(notes.length)].toUpperCase(Locale.ROOT));
        }
        return out.toString();
    }
    public static short[] synthesize(String instrument,String sequence) {
        int voice=-1;
        for(int i=0;i<INSTRUMENTS.length;i++)
            if(INSTRUMENTS[i].equalsIgnoreCase(instrument.trim()))voice=i;
        if(voice<0)throw new IllegalArgumentException("Strumento non supportato.");
        String[] notes=parseNotes(sequence);
        int samplesPerNote=RATE/4;
        short[] pcm=new short[notes.length*samplesPerNote];
        Random noise=new Random(92);
        for(int n=0;n<notes.length;n++){
            double frequency=pitch(notes[n]);
            for(int i=0;i<samplesPerNote;i++){
                double time=(double)i/RATE;
                double phase=2*Math.PI*frequency*time;
                double env=Math.pow(1.0-(double)i/samplesPerNote,voice==0?2.0:0.8);
                double wave;
                switch(voice){
                    case 0: wave=Math.sin(phase)+.18*Math.sin(2*phase);break;
                    case 1: wave=Math.sin(phase)+.45*Math.sin(2*phase)+.25*Math.sin(3*phase);break;
                    case 2: wave=(noise.nextDouble()*2-1)*Math.exp(-time*28)
                                  +.2*Math.sin(phase*.5)*Math.exp(-time*16);break;
                    case 3: wave=Math.sin(phase)+.26*Math.sin(2*phase)+.10*Math.sin(3*phase);break;
                    case 4: wave=.65*Math.sin(phase)+.3*Math.sin(2*phase)+.22*Math.sin(3*phase);break;
                    default:wave=.62*Math.sin(phase)+.43*Math.sin(2*phase)+.31*Math.sin(3*phase);
                }
                double amplitude=Math.max(-1,Math.min(1,wave*env*.30));
                pcm[n*samplesPerNote+i]=(short)Math.round(amplitude*Short.MAX_VALUE);
            }
        }
        return pcm;
    }
    private FragMusicLab(){}
}
