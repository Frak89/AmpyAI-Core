package com.ampyai.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Fråg from ZERO.
 * Only text command: Psicofråg. Otherwise input is a web search or https URL.
 * Web pages are read only when the user clicks their links, or taps STUDIA.
 * Untrusted page text is NOT executed as instructions or code.
 */
public final class MainActivity extends Activity {
    private static final int CREATE_ZIP=402;
    private final ExecutorService learning=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean(false);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ConceptStore concepts;
    private ModelStore modelStore;
    private volatile PrimitiveLM.Model model;
    private LinearLayout root;
    private TextView status, activity, webTitle;
    private EditText searchInput;
    private WebView browser;
    private boolean learnFromNextClick;
    private volatile boolean destroyed;
    private String loadingUrl="";

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(0xFF08131B);
        getWindow().setNavigationBarColor(0xFF08131B);
        concepts=new ConceptStore(getFilesDir());
        modelStore=new ModelStore(getFilesDir());
        try {model=modelStore.load();}
        catch(IOException ex) {
            model=PrimitiveLM.blank();
        }
        int needed=PrimitiveLM.widthForConcepts(concepts.count());
        if(model.hidden<needed)model=PrimitiveLM.expanded(model,needed);

        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF08131B);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                Insets keyboard=insets.getInsets(WindowInsets.Type.ime());
                root.setPadding(0,bars.top,0,Math.max(bars.bottom,keyboard.bottom));
            } else {
                root.setPadding(0,insets.getSystemWindowInsetTop(),0,
                        insets.getSystemWindowInsetBottom());
            }
            return insets;
        });

        TextView heading=label("🐸   FRÅG   /   ZERO CORE",20,0xFF81EFC1);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(dp(15),dp(14),0,dp(6));
        root.addView(heading);

        status=label("",13,0xFFC0D8D1);
        status.setPadding(dp(16),0,dp(12),dp(9));
        root.addView(status);

        LinearLayout bar=new LinearLayout(this);
        bar.setPadding(dp(10),dp(4),dp(10),dp(4));
        searchInput=new EditText(this);
        searchInput.setTextColor(Color.WHITE);
        searchInput.setHintTextColor(0xFF8C9DAB);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14f);
        searchInput.setHint("Cerca sul Web o scrivi Psicofråg");
        bar.addView(searchInput,new LinearLayout.LayoutParams(0,dp(53),1));
        Button go=button("CERCA");
        bar.addView(go,new LinearLayout.LayoutParams(dp(90),dp(53)));
        go.setOnClickListener(v->go(searchInput.getText().toString()));
        searchInput.setOnEditorActionListener((v,action,event)->{
            go(searchInput.getText().toString());return true;
        });
        root.addView(bar);

        LinearLayout actions=new LinearLayout(this);
        actions.setPadding(dp(9),0,dp(9),dp(5));
        Button psico=button("Psicofråg");
        Button learn=button("Studia pagina");
        Button memory=button("Memoria");
        actions.addView(psico,new LinearLayout.LayoutParams(0,dp(44),1));
        actions.addView(learn,new LinearLayout.LayoutParams(0,dp(44),1));
        actions.addView(memory,new LinearLayout.LayoutParams(0,dp(44),1));
        root.addView(actions);
        psico.setOnClickListener(v->showBrain());
        learn.setOnClickListener(v->captureCurrentPage());
        memory.setOnClickListener(v->showMemory());

        webTitle=label("Sfoglia Internet. Fråg impara dalle pagine che apri.",11,
                0xFF96ACB7);
        webTitle.setPadding(dp(12),dp(2),dp(12),dp(5));
        root.addView(webTitle);
        browser=new WebView(this);
        browser.setBackgroundColor(Color.WHITE);
        WebSettings settings=browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                if(!request.isForMainFrame())return false;
                String address=request.getUrl().toString();
                if(!address.startsWith("https://"))return true;
                if(request.hasGesture() && !isSearchPage(address))
                    learnFromNextClick=true;
                return false;
            }
            @Override public void onPageFinished(WebView view,String url) {
                webTitle.setText(url);
                if(learnFromNextClick && allowedToStudy(url)) {
                    learnFromNextClick=false;
                    handler.postDelayed(()->{
                        if(!destroyed && browser!=null && url.equals(browser.getUrl()))
                            captureCurrentPage();
                    },800);
                }
            }
        });
        root.addView(browser,new LinearLayout.LayoutParams(-1,0,1f));

        activity=label("Vuoto: nessun testo preaddestrato, nessun concetto memorizzato.",11,
                0xFF8DE3BE);
        activity.setPadding(dp(12),dp(8),dp(12),dp(12));
        root.addView(activity);
        refreshStatus();
        root.requestApplyInsets();
    }

    private void go(String raw) {
        String query=raw.trim();
        if(query.isEmpty())return;
        if(query.equalsIgnoreCase("psicofråg")||query.equalsIgnoreCase("psicofrag")) {
            showBrain();return;
        }
        learnFromNextClick=false;
        if(query.startsWith("https://")) {
            learnFromNextClick=true; // User explicitly opened a page to read.
            browser.loadUrl(query);
        } else if(query.contains("://")) {
            log("Solo indirizzi HTTPS. Non carico schemi esterni.");return;
        } else {
            String searchUrl="https://www.bing.com/search?q="+Uri.encode(query);
            browser.loadUrl(searchUrl);
            log("Ricerca sul Web: "+query+" · tocca un risultato per studiarlo.");
        }
    }

    private boolean isSearchPage(String url) {
        Uri parsed=Uri.parse(url);
        String host=parsed.getHost();
        if(host==null)return true;
        host=host.toLowerCase(Locale.ROOT);
        String path=parsed.getPath();
        return (host.endsWith("bing.com") && path!=null && path.startsWith("/search"))
            || (host.endsWith("google.com") && path!=null && path.startsWith("/search"))
            || (host.endsWith("duckduckgo.com") && path!=null && path.startsWith("/"))
            || url.equals("about:blank");
    }
    private boolean allowedToStudy(String url) {
        if(url==null || !url.startsWith("https://") || isSearchPage(url))return false;
        Uri uri=Uri.parse(url);
        String hostname=uri.getHost();
        if(hostname==null)return false;
        String path=(uri.getPath()==null?"":uri.getPath()).toLowerCase(Locale.ROOT);
        return !(path.contains("login")||path.contains("signin")||
                 path.contains("account")||path.contains("checkout")||
                 path.contains("password")||path.contains("oauth")||
                 path.contains("private")||path.contains("payment"));
    }

    private void captureCurrentPage(){
        if(browser==null)return;
        String url=browser.getUrl();
        if(!allowedToStudy(url)) {
            log("Non studio questa pagina: usa un articolo pubblico HTTPS.");return;
        }
        if(!busy.compareAndSet(false,true)) {
            log("Sto già apprendendo una pagina.");return;
        }
        loadingUrl=url;
        // Read-only extraction. No action or code from page text is executed.
        String javascript="(function(){"+
                "if(document.querySelector('input[type=password]'))return JSON.stringify({blocked:true});"+
                "const clean=s=>(s||'').trim().slice(0,2300);"+
                "const body=document.body?document.body.innerText:'';"+
                "const headings=Array.from(document.querySelectorAll('h1,h2'))"+
                ".slice(0,2).map(el=>({title:clean(el.innerText).slice(0,90),"+
                "text:clean(el.parentElement?el.parentElement.innerText:body)}));"+
                "return JSON.stringify({title:clean(document.title).slice(0,90),"+
                "text:clean(body),headings:headings});})()";
        browser.evaluateJavascript(javascript, result->{
            try {
                Object parsed=new JSONTokener(result).nextValue();
                if(!(parsed instanceof String))throw new IllegalArgumentException("Pagina non leggibile.");
                JSONObject page=new JSONObject((String)parsed);
                if(page.optBoolean("blocked"))
                    throw new IllegalArgumentException("Pagina con password: non acquisita.");
                String fullText=page.optString("text","");
                if(fullText.length()<120)
                    throw new IllegalArgumentException("La pagina non contiene testo sufficiente.");
                // Actual PNG screenshot of the currently visible WebView, not a fabricated image.
                int w=Math.max(1,browser.getWidth()),h=Math.max(1,browser.getHeight());
                float scale=Math.min(1f,Math.min(900f/w,800f/h));
                Bitmap shot=Bitmap.createBitmap(Math.max(1,(int)(w*scale)),
                        Math.max(1,(int)(h*scale)),Bitmap.Config.ARGB_8888);
                Canvas screenshot=new Canvas(shot);screenshot.scale(scale,scale);
                browser.draw(screenshot);
                JSONArray headings=page.optJSONArray("headings");
                String title=page.optString("title","Pagina visitata");
                List<String[]> observations=new ArrayList<>();
                observations.add(new String[]{title,fullText});
                if(headings!=null) {
                    for(int i=0;i<Math.min(2,headings.length());i++) {
                        JSONObject obj=headings.optJSONObject(i);
                        if(obj==null)continue;
                        String heading=obj.optString("title","");
                        String excerpt=obj.optString("text","");
                        if(heading.length()>=3 && excerpt.length()>=100 &&
                           !heading.equalsIgnoreCase(title))
                            observations.add(new String[]{heading,excerpt});
                    }
                }
                log("Sto osservando la pagina e fotografando il contenuto visibile...");
                learning.execute(()->learnPage(url,observations,shot,fullText));
            } catch(Exception ex) {
                busy.set(false);
                log("Non acquisita: "+ex.getMessage());
            }
        });
    }

    private void learnPage(String url,List<String[]> observations,Bitmap image,String pageText){
        try {
            ByteArrayOutputStream png=new ByteArrayOutputStream();
            if(!image.compress(Bitmap.CompressFormat.PNG,100,png))
                throw new IOException("Screenshot PNG non disponibile.");
            image.recycle();
            byte[] pngBytes=png.toByteArray();
            int discovered=0;
            for(String[] observation:observations)
                if(concepts.save(observation[0],url,observation[1],pngBytes))discovered++;
            int actual=concepts.count();
            int previous=model.hidden;
            int desired=PrimitiveLM.widthForConcepts(actual);
            PrimitiveLM.Model worker=PrimitiveLM.expanded(model,Math.max(previous,desired));
            if(discovered>0) {
                int newFound=discovered;
                log("Nuovi concetti: "+newFound+" · neuroni "+previous+
                        " → "+worker.hidden+"; addestramento dai testi...");
                PrimitiveLM.train(worker,pageText,3,(e,n,loss,neurons)->{
                    log("Epoca "+e+"/"+n+" · loss "+String.format(Locale.ITALY,"%.3f",loss)+
                            " · neuroni "+neurons);
                });
            } else {
                log("Questi concetti erano già presenti. Nessun neurone aggiunto.");
            }
            modelStore.save(worker);
            model=worker;
            int newCount=discovered;
            runOnUiThread(()-> {
                if(destroyed)return;
                refreshStatus();
                log("Pagina studiata. Nuovi concetti: "+newCount+
                        ". Ogni cartella contiene concept.txt e page.png.");
            });
        } catch(Exception ex) {
            log("Errore nel salvataggio/apprendimento: "+ex.getMessage());
        } finally {
            busy.set(false);
        }
    }

    private void showBrain() {
        PrimitiveLM.Model snapshot=model;
        int count=concepts.count();
        ScrollView scroll=new ScrollView(this);
        LinearLayout layout=new LinearLayout(this);
        layout.setPadding(dp(12),dp(14),dp(12),dp(20));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF10202A);
        scroll.addView(layout);
        TextView intro=label("PSICOFRÅG · CERVELLO REALE\n\n"+
                "Concetti studiati: "+count+"\n"+
                "Neuroni nascosti: "+snapshot.hidden+" / "+PrimitiveLM.MAX_HIDDEN+"\n"+
                "Parametri trainabili: "+PrimitiveLM.parameters(snapshot)+"\n"+
                "Sessioni di training: "+snapshot.trainingSessions+"\n"+
                "Caratteri elaborati: "+snapshot.processedCharacters+"\n"+
                "Ultima loss: "+String.format(Locale.ITALY,"%.3f",snapshot.lastLoss)+"\n"+
                "Finestra contestuale: "+PrimitiveLM.CONTEXT+" caratteri\n"+
                (snapshot.trainingSessions==0?
                 "\nNon ho ancora imparato nulla. I miei pesi sono casuali.\n":
                 "\nHo appreso distribuzioni di caratteri, non fatti verificati.\n"),
                13,Color.WHITE);
        layout.addView(intro);
        BrainGraphView graph=new BrainGraphView(this);
        graph.display(snapshot,"fråg",count);
        layout.addView(graph,new LinearLayout.LayoutParams(-1,dp(420)));
        EditText prompt=new EditText(this);
        prompt.setHint("Prova generazione: scrivi un inizio...");
        prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(0xFF8DA4AB);
        layout.addView(prompt);
        TextView result=label("",13,0xFF96E9BB);
        Button generate=button("GENERA DAL MODELLO");
        generate.setEnabled(snapshot.trainingSessions>0);
        generate.setOnClickListener(v->{
            result.setText("Testo grezzo, non una risposta intelligente:\n"+
                    PrimitiveLM.generate(model,prompt.getText().toString(),100,
                            System.nanoTime()));
            graph.display(model,prompt.getText().toString(),concepts.count());
        });
        layout.addView(generate);
        layout.addView(result);
        TextView note=label("\nOgni nuovo concetto espande realmente lo strato "+
                "nascosto (+4 neuroni fino a 256)."+
                "\n\nIl modello è un piccolo LM a caratteri: non è un LLM "+
                "grande, non interpreta semanticamente un sito e non sa "+
                "ancora autoprogrammarsi.",12,0xFFB4C9CD);
        layout.addView(note);
        Button gallery=button("APRI MEMORIA TXT / PNG");
        layout.addView(gallery);
        gallery.setOnClickListener(v->showMemory());
        Button export=button("ESPORTA MEMORIA .ZIP");
        layout.addView(export);
        export.setOnClickListener(v->exportZip());
        new AlertDialog.Builder(this).setTitle("🐸 Psicofråg")
                .setView(scroll).setPositiveButton("Chiudi",null).show();
    }

    private void showMemory() {
        List<ConceptStore.Concept> stored=concepts.list();
        if(stored.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Memoria vuota")
                    .setMessage("Nessun concetto: cerca sul Web e apri un articolo. "+
                            "La prima pagina visitata creerà file TXT e PNG.")
                    .setPositiveButton("OK",null).show();
            return;
        }
        ScrollView list=new ScrollView(this);
        LinearLayout column=new LinearLayout(this);
        column.setPadding(dp(12),dp(10),dp(12),dp(10));
        column.setOrientation(LinearLayout.VERTICAL);list.addView(column);
        for(ConceptStore.Concept c:stored) {
            Button button=button("🧠 "+c.title);
            button.setOnClickListener(v->showConcept(c));
            column.addView(button);
        }
        Button export=button("ESPORTA TUTTI I FILE ZIP");
        export.setOnClickListener(v->exportZip());
        column.addView(export);
        new AlertDialog.Builder(this).setTitle("Memoria ("+stored.size()+" concetti)")
                .setView(list).setPositiveButton("Chiudi",null).show();
    }

    private void showConcept(ConceptStore.Concept c) {
        try {
            ScrollView scroll=new ScrollView(this);
            LinearLayout content=new LinearLayout(this);
            content.setPadding(dp(12),dp(12),dp(12),dp(12));
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(content);
            Bitmap image=BitmapFactory.decodeFile(c.png.getAbsolutePath());
            if(image!=null) {
                ImageView picture=new ImageView(this);
                picture.setImageBitmap(image);
                picture.setAdjustViewBounds(true);
                content.addView(picture,new LinearLayout.LayoutParams(-1,dp(230)));
            }
            String text=new String(Files.readAllBytes(c.text.toPath()),
                    StandardCharsets.UTF_8);
            TextView description=label(text,12,0xFF15202B);
            description.setTextIsSelectable(true);
            description.setPadding(dp(5),dp(10),dp(5),dp(12));
            content.addView(description);
            new AlertDialog.Builder(this)
                    .setTitle(c.title+" · TXT + PNG")
                    .setView(scroll).setPositiveButton("Chiudi",null).show();
        } catch(IOException ex) {
            log("Impossibile leggere la memoria: "+ex.getMessage());
        }
    }

    private void exportZip() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE,"Frag_Memoria_"+
                System.currentTimeMillis()+".zip");
        startActivityForResult(intent,CREATE_ZIP);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=CREATE_ZIP || result!=RESULT_OK || data==null ||
                data.getData()==null)return;
        Uri destination=data.getData();
        learning.execute(()->{
            try(ZipOutputStream zip=new ZipOutputStream(
                    getContentResolver().openOutputStream(destination))) {
                for(ConceptStore.Concept item:concepts.list()) {
                    copyToZip(zip,item.text,"concepts/"+item.id+"/concept.txt");
                    copyToZip(zip,item.png,"concepts/"+item.id+"/page.png");
                }
                File weights=new File(getFilesDir(),"brain.bin");
                if(weights.isFile())copyToZip(zip,weights,"brain.bin");
                log("Memoria esportata (testi, PNG e pesi neurali).");
            } catch(Exception ex) { log("Esportazione fallita: "+ex.getMessage());}
        });
    }
    private void copyToZip(ZipOutputStream zip,File file,String name)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try(FileInputStream in=new FileInputStream(file)){
            byte[] b=new byte[8192];int size;
            while((size=in.read(b))!=-1)zip.write(b,0,size);
        }
        zip.closeEntry();
    }

    private void refreshStatus() {
        if(status==null)return;
        PrimitiveLM.Model m=model;
        status.setText("CONCETTI: "+concepts.count()+"   ·   NEURONI: "+m.hidden+
                "   ·   SESSIONI: "+m.trainingSessions+
                "   ·   PESI: "+PrimitiveLM.parameters(m));
    }
    private void log(String text) {
        runOnUiThread(()->{
            if(!destroyed && activity!=null)activity.setText(text);
        });
    }
    private TextView label(String str,int size,int color) {
        TextView t=new TextView(this);t.setText(str);t.setTextSize(size);t.setTextColor(color);
        return t;
    }
    private Button button(String title) {
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);
        b.setTextSize(12);
        return b;
    }
    private int dp(float n){
        return (int)(n*getResources().getDisplayMetrics().density+0.5f);
    }
    @Override public void onBackPressed() {
        if(browser!=null && browser.canGoBack())browser.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        destroyed=true;
        handler.removeCallbacksAndMessages(null);
        learning.shutdown();
        if(browser!=null) {
            browser.stopLoading();
            browser.destroy();
            browser=null;
        }
        super.onDestroy();
    }
}
