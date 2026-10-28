package com.ampyai.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * User-authored lessons and conlang dictionary in private storage.
 * Nothing is seeded. These are explicitly authored, not silently scraped.
 */
public final class FragPersonalMemory {
    private final File lessons,lexicon,observations,poems;
    private static final Pattern KEY=Pattern.compile("[a-z0-9_\\-]{1,32}");
    public FragPersonalMemory(File root) {
        File home=new File(root,"frag_memory/personal");
        lessons=new File(home,"lessons");
        lexicon=new File(home,"lexicon");
        observations=new File(home,"observations");
        poems=new File(home,"poems");
        if(!lessons.exists()&&!lessons.mkdirs())
            throw new IllegalStateException("Cartella lezioni non disponibile");
        if(!lexicon.exists()&&!lexicon.mkdirs())
            throw new IllegalStateException("Cartella parole non disponibile");
        if(!observations.exists()&&!observations.mkdirs())
            throw new IllegalStateException("Cartella immagini non disponibile");
        if(!poems.exists()&&!poems.mkdirs())
            throw new IllegalStateException("Cartella poesie non disponibile");
    }
    public static final class Lexeme {
        public final String word,meaning,pronunciation;
        public Lexeme(String w,String m,String p) {
            word=w;meaning=m;pronunciation=p;
        }
    }
    private static String name(String input) {
        String s=input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");
        if(!KEY.matcher(s).matches())
            throw new IllegalArgumentException("Usa un nome di 1-32 caratteri");
        return s;
    }
    private static void write(File file,String text) throws IOException {
        File temp=new File(file.getParentFile(),file.getName()+".tmp");
        try(FileOutputStream out=new FileOutputStream(temp)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        if(file.exists()&&!file.delete())throw new IOException("Impossibile aggiornare: "+file.getName());
        if(!temp.renameTo(file))throw new IOException("Impossibile salvare: "+file.getName());
    }
    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
    }
    public synchronized boolean lesson(String tag,String text) throws IOException {
        if(text==null||text.trim().length()<80||text.length()>4000)
            throw new IllegalArgumentException("Lezione di 80–4000 caratteri.");
        File target=new File(lessons,name(tag)+".txt");
        boolean newItem=!target.exists();
        write(target,text.trim());
        return newItem;
    }
    public synchronized String lessonText(String tag) throws IOException {
        File f=new File(lessons,name(tag)+".txt");
        return f.exists()?read(f):null;
    }
    public synchronized boolean teachWord(String word,String meaning,String pron) throws IOException {
        String n=name(word);
        if(meaning==null||meaning.trim().isEmpty()||meaning.length()>500||
            pron==null||pron.trim().isEmpty()||pron.length()>120)
            throw new IllegalArgumentException("Definizione o pronuncia mancante.");
        File file=new File(lexicon,n+".txt");
        boolean fresh=!file.exists();
        write(file,"PAROLA: "+word.trim()+"\nSIGNIFICATO: "+meaning.trim().replace('\n',' ') +
                "\nPRONUNCIA GUIDATA: "+pron.trim().replace('\n',' ')+"\n");
        return fresh;
    }
    public synchronized Lexeme word(String search) throws IOException {
        File f=new File(lexicon,name(search)+".txt");
        if(!f.isFile())return null;
        String text=read(f);
        String[] lines=text.split("\n");
        if(lines.length<3)return null;
        return new Lexeme(lines[0].replaceFirst("^PAROLA: ",""),
                lines[1].replaceFirst("^SIGNIFICATO: ",""),
                lines[2].replaceFirst("^PRONUNCIA GUIDATA: ",""));
    }
    public synchronized int count(){
        return countFiles(lessons)+countFiles(lexicon)+countFiles(observations);
    }
    public synchronized boolean saveObservation(String label,byte[] png) throws IOException {
        if(png==null||png.length<100)throw new IOException("Foto PNG vuota.");
        String id=name(label);
        File txt=new File(observations,id+".txt");
        boolean fresh=!txt.exists();
        write(txt,"ETICHETTA UTENTE: "+label+"\n"+
                "OSSERVAZIONE: foto etichettata, non classificazione automatica.\n");
        File image=new File(observations,id+".png");
        try(FileOutputStream out=new FileOutputStream(image))out.write(png);
        return fresh;
    }
    public synchronized void savePoem(String text) throws IOException {
        if(text==null||text.isEmpty())return;
        write(new File(poems,"poem_"+System.currentTimeMillis()+".txt"),text);
    }
    public synchronized String lastPoem() throws IOException {
        File[] all=poems.listFiles((dir,n)->n.endsWith(".txt"));
        if(all==null||all.length==0)return null;
        java.util.Arrays.sort(all,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
        return read(all[0]);
    }
    private static int countFiles(File root) {
        File[] a=root.listFiles((dir,s)->s.endsWith(".txt"));return a==null?0:a.length;
    }
    public synchronized List<File> files(){
        List<File> list=new ArrayList<>();
        File[] a=lessons.listFiles((dir,s)->s.endsWith(".txt"));
        File[] b=lexicon.listFiles((dir,s)->s.endsWith(".txt"));
        if(a!=null)java.util.Collections.addAll(list,a);
        if(b!=null)java.util.Collections.addAll(list,b);
        File[] c=observations.listFiles();
        File[] d=poems.listFiles((dir,n)->n.endsWith(".txt"));
        if(c!=null)java.util.Collections.addAll(list,c);
        if(d!=null)java.util.Collections.addAll(list,d);
        return list;
    }
}
