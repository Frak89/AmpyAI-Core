package com.ampyai.core;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only, user-triggered dictionary lookup from the Italian Wiktionary API. */
public final class FragWiktionaryTutor {
    public static final class Definition {
        public final String word, meaning, source;
        Definition(String word,String meaning,String source){
            this.word=word;this.meaning=meaning;this.source=source;
        }
    }

    public static Definition fetch(String candidate) throws Exception {
        String word=candidate.toLowerCase(Locale.ITALIAN).trim();
        if(!word.matches("[\\p{L}àèéìòù-]{2,35}"))
            throw new IllegalArgumentException("Scegli una sola parola di 2–35 lettere.");
        String url="https://it.wiktionary.org/w/api.php?action=parse"
                +"&prop=wikitext&page="+URLEncoder.encode(word,"UTF-8")
                +"&format=json";
        HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(9000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("User-Agent",
            "FrAgLanguageLab/0.3 (+https://github.com/Frak89/AmpyAI-Core; educational; dictionary user requested)");
        conn.setRequestProperty("Accept","application/json");
        try {
            if(conn.getResponseCode()!=200)
                throw new java.io.IOException("Dizionario HTTP "+conn.getResponseCode());
            ByteArrayOutputStream b=new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()) {
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1) {
                    if(b.size()+n>200000)throw new java.io.IOException("Pagina troppo grande.");
                    b.write(buffer,0,n);
                }
            }
            JSONObject data=new JSONObject(new String(b.toByteArray(),
                    StandardCharsets.UTF_8));
            JSONObject parse=data.optJSONObject("parse");
            if(parse==null)throw new java.io.IOException("Parola non presente sul Wikizionario.");
            Object wikitext=parse.opt("wikitext");
            String wiki=wikitext instanceof JSONObject
                    ? ((JSONObject)wikitext).optString("*","")
                    : (wikitext instanceof String?(String)wikitext:"");
            String meaning=extractDefinitions(wiki);
            if(meaning.isEmpty())throw new java.io.IOException(
                    "Nessuna definizione italiana semplice ricavabile dalla pagina.");
            return new Definition(word,meaning,
                "https://it.wiktionary.org/wiki/"+URLEncoder.encode(word,"UTF-8").replace("+","_"));
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Conservative subset parser. Returns NO content if a reliable Italian
     * definition cannot be identified. Wikitext templates are not learned
     * as though they were natural language.
     */
    public static String extractDefinitions(String wikitext) {
        if(wikitext==null||wikitext.isEmpty())return "";
        String[] lines=wikitext.split("\\r?\\n");
        boolean italian=true;
        boolean explicitLanguage=false;
        List<String> definitions=new ArrayList<>();
        Pattern heading=Pattern.compile("^==\\s*(.*?)\\s*==\\s*$");
        for(String line:lines) {
            Matcher m=heading.matcher(line.trim());
            if(m.matches()) {
                String tag=m.group(1).toLowerCase(Locale.ROOT);
                if(tag.contains("-it-")||tag.contains("italiano")||
                   tag.contains("{{it}}")||tag.equals("it")) {
                    italian=true;
                    explicitLanguage=true;
                } else if(tag.contains("-en-")||tag.contains("-fr-")||
                          tag.contains("-de-")||tag.contains("inglese")||
                          tag.contains("francese")||tag.contains("tedesco")) {
                    italian=false;
                    explicitLanguage=true;
                }
            }
            // Ordinary wiktionary definitions start with one hash + space.
            if(italian && line.matches("^#\\s+.+")) {
                String value=line.substring(1).trim();
                for(int i=0;i<6;i++) {
                    String previous=value;
                    value=value.replaceAll("\\{\\{[^{}]*\\}\\}"," ");
                    if(value.equals(previous))break;
                }
                value=value.replaceAll("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]","$2")
                           .replaceAll("\\[\\[([^\\]]+)\\]\\]","$1")
                           .replaceAll("<[^>]+>"," ")
                           .replaceAll("'{2,5}","")
                           .replaceAll("\\[[^\\[\\]]+\\]"," ")
                           .replaceAll("\\s+"," ").trim();
                if(value.length()>10 && value.length()<500
                        && !value.contains("{{") && !value.contains("}}")) {
                    definitions.add(value);
                    if(definitions.size()==2) break;
                }
            }
        }
        if(definitions.isEmpty())return "";
        String joined=String.join("; ",definitions);
        return joined.length()>600?joined.substring(0,600):joined;
    }
    private FragWiktionaryTutor(){}
}
