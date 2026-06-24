# Listaio

App Android per la **lista della spesa condivisa** del nucleo familiare: articoli
spuntabili, riordinabili con drag & drop, eliminabili con swipe. Pensata per essere
condivisa in tempo reale tra più dispositivi.

## Stato

- **Fase 1 (questa) — locale e funzionante.** Tutto gira in locale con persistenza Room.
  Nessuna configurazione cloud richiesta: apri, builda, usa.
- **Fase 2 — sincronizzazione multi-dispositivo** via Firebase Firestore: vedi
  [FIRESTORE.md](FIRESTORE.md). La migrazione tocca **solo** l'implementazione del
  repository, non la UI.

## Architettura

Kotlin + Jetpack Compose (Material 3) + MVVM. Tutto l'accesso ai dati passa da
un'unica interfaccia, [`ListRepository`](app/src/main/java/com/listaio/domain/ListRepository.kt) —
il "seam" che in fase 2 verrà reimplementato su Firestore senza toccare il resto.

```
ui (Compose, ViewModel)  ─►  domain (ShoppingItem, ListRepository)  ◄─  data
                                                                         └─ local: Room (LocalListRepository)  [fase 1]
                                                                         └─ remote: Firestore                  [fase 2]
```

**Ordinamento**: ogni articolo ha un `order` in virgola mobile (*fractional index*).
Spostare un elemento riscrive un solo record (il punto medio tra i due vicini), niente
rinumerazione dell'intera lista e robusto ai riordini concorrenti.

## Come eseguirla

1. Apri la cartella in **Android Studio** → attendi il Gradle Sync.
2. Premi **Run ▶** su un emulatore o un telefono (minSdk 26 / Android 8.0+).

Da riga di comando:
```bash
./gradlew :app:assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug       # installa su dispositivo/emulatore collegato
```

## Funzioni

- ➕ Aggiungi articoli dalla barra in basso (invio o pulsante +).
- 🔎 Mentre digiti, la barra mostra in anteprima gli articoli già in lista che combaciano,
  e **blocca i duplicati** (nome uguale a meno di maiuscole/accenti/spazi): il pulsante +
  si disabilita e compare l'avviso «… è già nella lista».
- ✓ Tap su una riga per spuntare/despuntare (testo barrato quando spuntato).
- ↕ Trascina dalla maniglia per riordinare.
- 🗑 Swipe sulla riga per spostare il singolo articolo nel cestino (con conferma).
- 🧹 "Svuota spuntati" nella barra in alto sposta in blocco gli articoli spuntati nel
  cestino (con conferma).
- ♻️ **Cestino** (icona in alto, con badge del conteggio): gli articoli rimossi non sono
  persi. Selezione multipla per **ripristinarli** (tornano nella lista, despuntati) o
  **eliminarli definitivamente** (con conferma, irreversibile).
- ✏ Long-press su una riga per rinominare.
- 💾 I dati persistono tra i riavvii (Room).
- 📐 La barra di inserimento si adatta alla navigation bar di Android e alla tastiera.
