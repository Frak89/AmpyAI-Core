package com.ampyai.core;

import org.json.JSONArray;
import org.json.JSONObject;
import android.text.Html;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Explicit opt-in remote research with real URLs and short source excerpts.
 * Only fixed Wikipedia and GitHub endpoints are queried, never arbitrary hosts.
 * Runs on a background executor from MainActivity, never on the UI thread.
 */
public final class FragResearch {
    public static final class Hit {
        public final String provider;
        public final String title;
        public final String snippet;
        public final String url;
        public Hit(String provider, String title, String snippet, String url) {
            this.provider = provider;
            this.title = title;
            this.snippet = snippet;
            this.url = url;
        }
    }

    public interface Progress {
        void onStage(String stage);
    }

    public interface Provider {
        String name();
        List<Hit> search(String query) throws Exception;
    }

    private final List<Provider> providers = new ArrayList<>();
    public FragResearch() {
        providers.add(new WikipediaProvider());
        providers.add(new GitHubProvider());
    }

    public List<Hit> find(String query, Progress progress, FragKnowledgeStore knowledge) {
        List<Hit> collected = new ArrayList<>();
        for (Provider provider : providers) {
            progress.onStage("🔎 Ricerca reale: " + provider.name());
            try {
                List<Hit> hits = provider.search(query);
                progress.onStage("📚 " + provider.name() + ": " + hits.size() + " risultati verificabili.");
                for (Hit hit : hits) {
                    knowledge.save(query, hit);
                    collected.add(hit);
                }
            } catch (Exception error) {
                progress.onStage("⚠ " + provider.name() + " non disponibile: "
                        + error.getClass().getSimpleName());
            }
        }
        if (collected.isEmpty()) {
            progress.onStage("📂 Consulto l'indice locale, se esiste.");
            return knowledge.cached(query);
        }
        return collected;
    }

    private static String encode(String input) throws IOException {
        return URLEncoder.encode(input, "UTF-8");
    }

    private static JSONObject fetch(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "AmpyAI-Frag/0.2 (Android personal research prototype)");
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != 200) {
                throw new IOException("HTTP " + connection.getResponseCode());
            }
            int max = 350_000;
            try (BufferedInputStream stream = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192];
                int n;
                while ((n = stream.read(bytes)) != -1) {
                    if (out.size() + n > max) throw new IOException("Result too large");
                    out.write(bytes, 0, n);
                }
                return new JSONObject(out.toString("UTF-8"));
            } catch (org.json.JSONException invalid) {
                throw new IOException("Invalid JSON", invalid);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String clean(String html) {
        String value = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        value = value.replaceAll("\\s+", " ").trim();
        return value.length() <= 190 ? value : value.substring(0, 190) + "…";
    }

    private static final class WikipediaProvider implements Provider {
        @Override public String name() { return "Wikipedia (IT)"; }
        @Override public List<Hit> search(String query) throws Exception {
            String api = "https://it.wikipedia.org/w/api.php?action=query&format=json&list=search"
                    + "&srlimit=3&srnamespace=0&srsearch=" + encode(query);
            JSONObject root = fetch(api);
            JSONObject queryData = root.optJSONObject("query");
            List<Hit> result = new ArrayList<>();
            if (queryData == null) return result;
            JSONArray matches = queryData.optJSONArray("search");
            if (matches == null) return result;
            for (int i = 0; i < matches.length(); i++) {
                JSONObject row = matches.optJSONObject(i);
                if (row == null) continue;
                String title = row.optString("title");
                if (title.isEmpty()) continue;
                String link = "https://it.wikipedia.org/wiki/"
                        + encode(title.replace(' ', '_')).replace("+", "_");
                result.add(new Hit(name(), title, clean(row.optString("snippet")), link));
            }
            return result;
        }
    }

    private static final class GitHubProvider implements Provider {
        @Override public String name() { return "GitHub (repository pubbliche)"; }
        @Override public List<Hit> search(String query) throws Exception {
            String api = "https://api.github.com/search/repositories?per_page=3&q="
                    + encode(query);
            JSONArray items = fetch(api).optJSONArray("items");
            List<Hit> result = new ArrayList<>();
            if (items == null) return result;
            for (int i = 0; i < items.length(); i++) {
                JSONObject row = items.optJSONObject(i);
                if (row == null) continue;
                String url = row.optString("html_url");
                // Refuse to show arbitrary data-driven URLs.
                if (!url.startsWith("https://github.com/")) continue;
                result.add(new Hit(name(), row.optString("full_name"),
                        clean(row.optString("description", "")), url));
            }
            return result;
        }
    }
}
