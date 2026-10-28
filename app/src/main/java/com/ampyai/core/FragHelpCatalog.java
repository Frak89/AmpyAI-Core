package com.ampyai.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Truthful self-populating help index. Only the built-in entries are executable
 * capabilities. User/web learning adds SOURCED MEMORY entries; FRAP source
 * files add runnable, validated vector-module examples.
 *
 * No page can register an arbitrary Android, shell or Java command.
 */
public final class FragHelpCatalog {
    public static final String CAPABILITY="COMANDI DISPONIBILI";
    public static final String MODULE="MODULI FRAP SALVATI";
    public static final String KNOWLEDGE="CONCETTI E MEMORIE ACQUISITE";

    public static final class Entry {
        public final String group,title,description,example,detail,imagePath;
        public final boolean executable,learned;
        public Entry(String group,String title,String description,String example,
                     String detail,String imagePath,boolean executable,boolean learned) {
            this.group=group;this.title=title;this.description=description;
            this.example=example;this.detail=detail;this.imagePath=imagePath;
            this.executable=executable;this.learned=learned;
        }
        public boolean matches(String q) {
            String n=q==null?"":q.toLowerCase(Locale.ITALIAN).trim();
            return n.isEmpty() || (title+" "+description+" "+example+" "+detail+" "+group)
                    .toLowerCase(Locale.ITALIAN).contains(n);
        }
    }

    private static Entry command(String title,String description,String example,String detail){
        return new Entry(CAPABILITY,title,description,example,detail,null,true,false);
    }

    public static List<Entry> basics() {
        List<Entry> all=new ArrayList<>();
        all.add(command("Psicofråg","Cervello vivo: neuroni, loss, pesi e generazione.",
                "Psicofråg","Modello neurale a caratteri: la crescita fisica non dimostra comprensione."));
        all.add(command("Help / Aiuto","Apre questa guida dinamica, anche senza rete.",
                "help","La guida viene ricostruita dai moduli e dai file davvero presenti."));
        all.add(command("Cerca Internet","Cerca una parola nel browser integrato.",
                "quadrato geometria","Clicca un risultato per tentare l'acquisizione di pagina, fonte e screenshot."));
        all.add(command("Apri pagina HTTPS","Visita e studia un articolo pubblico.",
                "https://it.wikipedia.org/wiki/Quadrato",
                "Solo siti HTTPS: pagine login/password e moduli privati vengono escluse per quanto possibile."));
        all.add(command("Studia pagina","Legge il sito pubblico attualmente visibile.",
                "studia pagina","Registra estratti di testo e un PNG dello schermo; il testo è fonte non verificata."));
        all.add(command("Memoria","Mostra le pagine visitate con immagini e fonti.",
                "memoria","L'archivio delle pagine è distinto dal modello neurale."));
        all.add(command("Laboratorio","Apre gli esperimenti modulari.",
                "laboratorio","Le funzioni seguenti esistono come moduli separati."));
        all.add(command("Insegnami","Aggiungi un testo scritto da te e osserva il training.",
                "insegna","Scrivi il concetto e una lezione di almeno 80 caratteri."));
        all.add(command("Lingua inventata","Associa parole, significato e pronuncia guidata.",
                "lingua","La lettura TTS della pronuncia è approssimativa."));
        all.add(command("Parliamo","Prova la generazione linguistica grezza.",
                "parliamo","Il modello ha una memoria contestuale molto breve, non comprende davvero il dialogo."));
        all.add(command("Poesia","Genera quattro versi sperimentali dal piccolo LM.",
                "poesia","I versi potrebbero essere insensati; quelli salvati appaiono nella memoria."));
        all.add(command("Pixel / Telefono","Legge dati tecnici Android, batteria e lingua.",
                "pixel","Nessun accesso automatico ai dati personali o alla posizione."));
        all.add(command("FRAP / Disegno vettoriale","Editor codice geometrico e visualizzazione SVG.",
                "frap","Esempio programma:\nrect 0.15 0.15 0.7 0.7\ncircle 0.5 0.5 0.2"));
        all.add(command("Autoritratto","Apri un ritratto parametrico di Fråg da modificare.",
                "autoritratto","Template geometrico con dettagli legati al numero di concetti, non disegno neurale."));
        all.add(command("Oggetto 3D","Ruota un cubo wireframe con il dito.",
                "3d","Rendering geometrico semplice, non ricostruzione da immagini."));
        all.add(command("Musica","Timbri sintetici e variazioni da note scritte da te.",
                "musica","Esempio note nel laboratorio: C3 E3 G3, scegliendo basso, chitarra, batteria, pianoforte o sax."));
        all.add(command("Fotocamera","Fotografa un oggetto e assegna un'etichetta.",
                "fotocamera","Salva una foto etichettata; non riconosce automaticamente gli oggetti."));
        return all;
    }

    public static Entry visited(String id,String title,String url,String text,File picture) {
        return new Entry(KNOWLEDGE,"🌍 "+title,
                "Pagina visitata · fonte: "+url,
                "memoria: "+id,
                trim(text,1150)+"\n\nASSOCIAZIONI: estratte automaticamente dal testo, non verificate.",
                picture!=null?picture.getAbsolutePath():null,false,true);
    }

    public static Entry personal(String type,String name,String content,File picture) {
        String display=type.equals("lezione")?"📖 Lezione: ":
            type.equals("parola")?"🗣️ Parola inventata: ":
            type.equals("foto")?"📷 Foto etichettata: ":"✍️ Poesia: ";
        return new Entry(KNOWLEDGE,display+name,
            "Memoria scritta dall'utente · "+type,
            "ricorda: "+type+"/"+name,
            trim(content,1000),
            picture!=null && picture.isFile()?picture.getAbsolutePath():null,
            false,true);
    }

    public static Entry frap(String name,String source,File svg) {
        return new Entry(MODULE,"🧩 FRAP: "+name,
            "Modulo vettoriale salvato, apribile nell'editor.",
            "frap: "+name, "Codice FRAP:\n"+trim(source,1500)+
                "\n\nLa sintassi viene verificata prima dell'esecuzione.",
                null,true,true);
    }

    public static String trim(String raw,int max) {
        if(raw==null)return "";
        return raw.length()>max?raw.substring(0,max)+"…":raw;
    }

    private FragHelpCatalog(){}
}
