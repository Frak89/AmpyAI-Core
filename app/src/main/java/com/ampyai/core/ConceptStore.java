package com.ampyai.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * One learned concept = a real directory with concept.txt + page.png.
 * These are source-carrying samples, NOT verified factual knowledge.
 */
public final class ConceptStore {
    private final File home;
    private static final Set<String> STOP=new HashSet<>();
    static {
        for(String s:("della delle degli dello essere quando mentre questa quello cosa "+
                "tutti tutte anche della della come sono dagli delle hanno nella sullo "+
                "delle senza delle dalle italiano lingua segue parte attraverso "+
                "questa questi quello degli dopo prima quindi altri altro alcuni").split(" "))
            STOP.add(s);
    }
    public ConceptStore(File filesDir) {
        home=new File(filesDir,"frag_memory/concepts");
        if(!home.exists()&&!home.mkdirs())
            throw new IllegalStateException("Cartella memoria non scrivibile.");
    }
    public File home(){return home;}
    private static String hash(String v) {
        try {
            MessageDigest dig=MessageDigest.getInstance("SHA-256");
            byte[] bytes=dig.digest(v.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(int i=0;i<12;i++)out.append(String.format(Locale.US,"%02x",bytes[i]&0xff));
            return out.toString();
        } catch(Exception ex) {throw new IllegalStateException(ex);}
    }
    public static final class Concept {
        public final String id,title,url;
        public final File text,png;
        Concept(String id,String title,String url,File text,File png) {
            this.id=id;this.title=title;this.url=url;this.text=text;this.png=png;
        }
    }
    public synchronized boolean save(String title,String url,String excerpt,byte[] png)
            throws IOException {
        if(title==null||title.trim().length()<3||
           excerpt==null||excerpt.trim().length()<100 ||
           url==null||!url.startsWith("https://")||
           png==null||png.length<100)
            return false;
        String name=title.replaceAll("[\\r\\n\\t]+"," ").trim();
        if(name.length()>100)name=name.substring(0,100);
        String id=hash(url.trim()+"|"+name.toLowerCase(Locale.ROOT));
        File folder=new File(home,id);
        boolean fresh=!new File(folder,"concept.txt").exists() ||
                !new File(folder,"page.png").exists();
        if(!fresh)return false;
        if(!folder.exists()&&!folder.mkdirs())
            throw new IOException("Impossibile creare la cartella del concetto.");
        File text=new File(folder,"concept.txt"),image=new File(folder,"page.png");
        // Snapshot and text from the same page: do not create fake imagery.
        String content="CONCETTO: "+name+"\nFONTE: "+url+"\nACQUISITO: "+
                new java.util.Date().toString()+
                "\n\nTESTO VISITATO (non verificato):\n"+
                excerpt.substring(0,Math.min(2500,excerpt.length()))+
                "\n\nASSOCIAZIONI LESSICALI:\n"+associations(excerpt)+"\n";
        try(FileOutputStream out=new FileOutputStream(text)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        try(FileOutputStream out=new FileOutputStream(image)) {
            out.write(png);
        }
        return true;
    }
    public synchronized List<Concept> list() {
        List<Concept> result=new ArrayList<>();
        File[] directories=home.listFiles();
        if(directories==null)return result;
        for(File folder:directories) {
            if(!folder.isDirectory())continue;
            File txt=new File(folder,"concept.txt"),png=new File(folder,"page.png");
            if(!txt.isFile()||!png.isFile())continue;
            try {
                String content=new String(Files.readAllBytes(txt.toPath()),StandardCharsets.UTF_8);
                String[] lines=content.split("\n",4);
                if(lines.length<2||!lines[0].startsWith("CONCETTO: ")||
                   !lines[1].startsWith("FONTE: "))continue;
                result.add(new Concept(folder.getName(),
                        lines[0].substring(10),lines[1].substring(7),txt,png));
            } catch(IOException ignored) {}
        }
        result.sort((a,b)->Long.compare(b.text.lastModified(),a.text.lastModified()));
        return result;
    }
    public synchronized int count(){return list().size();}
    public static String associations(String excerpt) {
        String source=PrimitiveLM.normalize(excerpt);
        Map<String,Integer> freq=new HashMap<>();
        for(String s:source.split("[^a-z]+")) {
            if(s.length()<5||s.length()>22||STOP.contains(s))continue;
            freq.put(s,freq.getOrDefault(s,0)+1);
        }
        List<String> rank=new ArrayList<>(freq.keySet());
        rank.sort((a,b)->{
            int result=Integer.compare(freq.get(b),freq.get(a));
            return result==0?a.compareTo(b):result;
        });
        return String.join(", ",rank.subList(0,Math.min(12,rank.size())));
    }
}
