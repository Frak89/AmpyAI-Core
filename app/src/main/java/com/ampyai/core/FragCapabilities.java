package com.ampyai.core;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Auditable registry: installed commands, what they actually do and what they cannot do. */
public final class FragCapabilities {
    public static final class Capability {
        public final String key, description, example, scope;
        Capability(String key,String description,String example,String scope){
            this.key=key;this.description=description;this.example=example;this.scope=scope;
        }
    }
    public static final List<Capability> ALL = Arrays.asList(
        new Capability("Cervello","Apre la mappa del Neural Core con struttura, pesi salvati e stato di addestramento.",
               "Cervello", "Ispezione, non apprendimento"),
        new Capability("Psicofråg","Visualizza input numerici reali, attivazioni dello strato nascosto, probabilità delle 7 uscite e campioni di connessioni pesate. Durante il training aggiornamenti per epoca.",
               "Psicofråg", "Visualizza solo neuroni campionati, mostra totali esatti"),
        new Capability("Lista cervello","Elenca le capacità realmente programmate ed esempi di comandi.",
               "Lista cervello","Registro statico del software, non conoscenza scoperta dalla rete"),
        new Capability("Approfondisci X","Spiega la capacità X oppure mostra esempi etichettati e fonti locali su un argomento. Se non sa, lo dichiara.",
               "Approfondisci pulsante","Ricerche solo nell'archivio locale, nessuna invenzione"),
        new Capability("Modella cervello","Configura per il prossimo training 8–128 neuroni nascosti e 10–200 epoche; l'architettura cambia dopo riaddestramento.",
               "Modella cervello 48 100","Non amplia le 7 classi né crea nuovi moduli"),
        new Capability("Esempi base","Carica 70 esempi etichettati inclusi nel progetto.",
               "esempi base","7 classi, non dataset generale"),
        new Capability("Insegna","Aggiunge un esempio e l'etichetta corretta nel database per il successivo training.",
               "insegna: metti il tasto Conferma => pulsante","Non aggiorna i pesi finché non rilanci il training"),
        new Capability("Importa dataset","Importa un array JSON o righe JSONL con text e label.",
               "importa dataset","Massimo 1 MB e 5000 esempi a file"),
        new Capability("Allena Fråg","Allena offline la rete con SGD/backpropagation e valida su esempi esclusi.",
               "allena Fråg","Solo classificazione delle istruzioni UI"),
        new Capability("Statistiche training","Mostra esempi, epoche e accuratezza di validazione dello specifico dataset.",
               "statistiche training","L'accuratezza non misura intelligenza generale"),
        new Capability("Prova AI","Classifica una frase senza modificare il canvas.",
               "prova AI: inserisci un tasto","Risposta probabilistica; può essere errata"),
        new Capability("Esegui AI","Classifica e applica istruzione UI solo se punteggio e parametri bastano.",
               "esegui AI: crea pulsante con scritto Ok","Non genera codice o componenti arbitrari"),
        new Capability("Cerca online","Esegue ricerca esplicita su Wikipedia Italia e repository GitHub, indicizzando fonti.",
               "cerca online interfacce Android","Non è un browser generale"),
        new Capability("Proponi stile","Anteprime Cyberpunk, Minimal, Sci-Fi, Retro, Organic e feedback persistente.",
               "proponi uno stile per foto","Selezione guidata da preferenze simboliche"),
        new Capability("Editor UI","Aggiunge titolo, testo, pulsante, campo, scheda, sfondo e template.",
               "crea pulsante Conferma","Widget limitati, niente drag and drop"),
        new Capability("Annulla","Ripristina l'ultima modifica di scena mantenuta nello stack.",
               "annulla","Massimo 20 stati in memoria"),
        new Capability("Inizia linguaggio","Crea un corpus italiano originale e addestra da zero un modello neurale autoregressivo su caratteri.",
               "inizia linguaggio","Contesto di 4 caratteri, non è un LLM"),
        new Capability("Insegna lingua","Registra testo fornito dall'utente e lo conserva come corpus.",
               "insegna lingua: la memoria conserva una parola","Per aggiornare i pesi occorre allena linguaggio"),
        new Capability("Allena linguaggio","Prosegue l'addestramento dei pesi linguistici esistenti usando un corpus locale limitato.",
               "allena linguaggio","Modello minuscolo, non impara significati complessi"),
        new Capability("Studia parola","Consulta su richiesta Wikizionario, salva definizioni con URL e addestra il piccolo nucleo linguistico.",
               "studia parola: memoria","Richiede Internet; se non trova una definizione, non la inventa"),
        new Capability("Esplora dizionario","Studia al massimo quattro parole prestabilite dal Wikizionario con fonti.",
               "esplora dizionario","Non è un crawler autonomo"),
        new Capability("Definisci","Restituisce solo la definizione realmente salvata e la fonte.",
               "definisci memoria","Recupero archivio, non generazione neurale"),
        new Capability("Parla","Genera una continuazione originale campionando la previsione del carattere successivo.",
               "parla: ciao","Contesto di 4 caratteri: il risultato può essere privo di senso")
    );
    public static Capability find(String raw) {
        String q=canonical(raw);
        if (q.isEmpty()) return null;
        for(Capability cap:ALL){
            String k=canonical(cap.key);
            if(k.equals(q) || k.startsWith(q+" ") || q.startsWith(k+" ")) return cap;
        }
        if(q.equals("pulsante")||q.equals("titolo")||q.equals("testo")||
           q.equals("campo")||q.equals("scheda")||q.equals("login")||
           q.equals("dashboard")) return null; // handled using labeled examples
        if(q.equals("brain")) return ALL.get(0);
        if(q.equals("training")) return ALL.get(8);
        if(q.equals("ricerca")) return ALL.get(12);
        if(q.equals("stile")) return ALL.get(13);
        return null;
    }
    public static String canonical(String v){
        return v.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
    }
    public static String list(){
        StringBuilder sb=new StringBuilder("🐸 LISTA CERVELLO — funzioni realmente disponibili:\n");
        for(Capability c:ALL)
            sb.append("\n• ").append(c.key).append("\n  Es.: ").append(c.example);
        sb.append("\n\n«Approfondisci X» per i dettagli. Capacità neurali: 7 categorie UI; non è un LLM.");
        return sb.toString();
    }
    private FragCapabilities(){}
}
