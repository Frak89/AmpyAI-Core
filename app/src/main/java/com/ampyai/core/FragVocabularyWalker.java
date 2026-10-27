package com.ampyai.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tiny transparent curriculum selector. Looks at saved dictionary definitions
 * and chooses unfamiliar words to request NEXT, in a strictly bounded session.
 * This is heuristic exploration, not emergent agency or understanding.
 */
public final class FragVocabularyWalker {
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
        "della","delle","degli","dello","dalla","dalle","dallo",
        "questo","questa","questi","queste","quello","quella",
        "essere","avere","viene","sono","anche","come","dove","quando",
        "molto","usato","usata","delle","dell","sulla","sulle","sullo",
        "relativo","relativa","parte","alcuni","alcune","senza",
        "perché","mentre","altri","altre","dopo","prima","dentro",
        "quindi","della","delle","dallo","quale","quali","delle",
        "attraverso","significa","indica","detto","detta"
    ));
    private static final String[] START = {
        "parola","memoria","linguaggio","mente","pensiero","significato",
        "apprendimento","conoscenza","definizione"
    };

    public static List<String> suggest(List<String> definitions,Set<String> known,int max) {
        if(max<1 || max>8)throw new IllegalArgumentException("Max 8 parole");
        Set<String> excluded=new HashSet<>();
        for(String term:known) excluded.add(term.toLowerCase(Locale.ITALIAN));
        Map<String,Integer> counts=new HashMap<>();
        for(String definition:definitions) {
            if(definition==null)continue;
            for(String word:definition.toLowerCase(Locale.ITALIAN).split("[^\\p{L}]+")) {
                if(word.length()<5||word.length()>17||STOP.contains(word)
                      || excluded.contains(word)) continue;
                counts.put(word,counts.getOrDefault(word,0)+1);
            }
        }
        List<String> ranked=new ArrayList<>(counts.keySet());
        ranked.sort((a,b)-> {
            int score=Integer.compare(counts.get(b),counts.get(a));
            return score==0?a.compareTo(b):score;
        });
        List<String> result=new ArrayList<>();
        for(String w:ranked) {
            result.add(w);
            if(result.size()==max)return result;
        }
        for(String seed:START) {
            if(!excluded.contains(seed)&&!result.contains(seed))result.add(seed);
            if(result.size()==max)break;
        }
        return result;
    }
    private FragVocabularyWalker(){}
}
