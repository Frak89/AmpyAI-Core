# 🐸 Fråg — ZERO / Primordial LM

Fråg is a completely fresh, modular native Android experiment. **There are no pretrained weights, no teaching corpus, no canned facts, no previous UI classifier, no old drawing tools.** The only text command interpreted by the app is **`Psicofråg`**. Anything else typed into the box becomes a web search or direct HTTPS URL.

### Workflow: watch a real small neural model grow

1. Install and launch. Brain starts at **16 hidden neurons**, with random, untrained weights and **0 concepts**.
2. Enter a search term and press **CERCA**. A real WebView opens Bing Search results. Other search engines and article links can be reached by following URLs.
3. Tap a search-result link. Fråg follows the same visited web page you see, reads a limited portion of the page's rendered text with `document.body.innerText`, the title and up to two headings, and records a **real PNG screenshot of the visible browser viewport**. It does **not** learn from private/login/password pages.
4. Each new title/heading that has sufficient text is saved as its **own folder** in `filesDir/frag_memory/concepts/<sha256id>/` with:
   - `concept.txt` — title, URL, captured text and automatically extracted lexical associations.
   - `page.png` — the true visible screenshot of the page.
5. For each new recorded concept, the actual neural layer expands **by 4 hidden neurons**: 16 → 20 → 24 → 28 … **up to 256**; the old weight matrices and biases are preserved while the new neurons are randomly initialized. The model is trained with SGD and backprop on up to 1600 captured characters of the visited page, and weights are saved to `brain.bin`.
6. Type **`Psicofråg`** to inspect the REAL hidden neuron count, parameter count, number of concepts, training sessions, loss, sampled real neural weights and next-character probabilities. You can generate primitive character text from the locally trained model in the inspector.
7. Tap **Memoria** to open saved TXT + screenshot PNG for each concept; use **Esporta memoria .ZIP** to take every `concept.txt`, `page.png` and weights file off the phone.

You can also press **Studia pagina** to retry reading an already displayed public webpage if its navigation wasn't detected as a click.

### Critical limitations

- This **is a real but extremely small, scratch-trained character-level language model**, not a **large** language model, transformer or meaningful chat assistant. It has a **6-character context**; it cannot understand web pages semantically, summarize accurately, or reason like a modern LLM. Generated prose will usually be low quality or gibberish.
- TXT records and lexical associations are **unverified excerpts**, not independently validated knowledge or evidence of semantic understanding.
- The visible neuron growth is mathematically real (new weights and biases), but **adding neurons does not necessarily improve language quality**. The fixed vocabulary and context remain limited; growth stops at 256 neurons even though the file memory may continue expanding.
- All browser content remains **untrusted data**: no page text can issue model commands or write executable code. There is **no self-programming in this revision**. Future code generation should require tests, sandboxing, user approval and safe source control.
- Browsing is **user-driven**, not background scraping. It never intentionally reads forms with password fields or pages with login/account/payment paths. Only HTTPS is accepted.
- Web search depends on availability of the chosen search engine from Android WebView; some external sites may block WebView or DOM extraction.
- PNG is a screenshot of the **visible viewport**, not a full-page offline archive, nor a downloaded site image. The same viewport screenshot can accompany multiple concepts from one article.
- The memory is stored inside the app's private files directory, with optional user-controlled ZIP export. Deleting app data deletes it.
- Existing Git history remains recoverable, but the new branch's tracked tree contains **only this from-scratch implementation** and replaces the old project files.

### Build

Android SDK 36 · minSdk 26 · Java 17 · AGP 8.10.1 · Gradle 8.13

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK from GitHub Actions → `Frag-Primordial-debug-APK`. CI unit tests cover initial blank model, real neuron growth preserving original weights, training changes, generation and probabilities.
