# Fase 2 — Sincronizzazione multi-dispositivo con Firebase Firestore

Questa è la checklist per passare dallo storage **locale** (Room, fase 1) alla
**sincronizzazione real-time** tra i dispositivi del nucleo familiare. Il bello
dell'architettura: **la UI e il ViewModel non cambiano**. Si aggiunge solo una nuova
implementazione di [`ListRepository`](app/src/main/java/com/listaio/domain/ListRepository.kt)
e si cambia una riga in [`ServiceLocator`](app/src/main/java/com/listaio/di/ServiceLocator.kt).

---

## 1. Console Firebase (lo fai tu, serve il tuo account Google)

1. Vai su <https://console.firebase.google.com> → **Aggiungi progetto** (es. "Listaio").
   Puoi disattivare Google Analytics, non serve.
2. Nel progetto: **Build → Firestore Database → Crea database** → modalità **Production**,
   scegli una region europea (es. `eur3` / `europe-west`).
3. **Build → Authentication → Get started → Sign-in method → abilita "Anonimo"**.
4. **Project settings (⚙) → Le tue app → aggiungi app Android**:
   - Android package name: **`com.listaio`** (deve coincidere con `applicationId`).
   - Scarica **`google-services.json`** e mettilo in **`app/google-services.json`**.
     (È già in `.gitignore`: non finisce su git.)

---

## 2. Dipendenze e plugin (modifiche al progetto)

**`gradle/libs.versions.toml`** — aggiungi:
```toml
[versions]
googleServices = "4.4.2"
firebaseBom = "33.7.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

**`build.gradle.kts`** (root) — aggiungi `alias(libs.plugins.google.services) apply false`.

**`app/build.gradle.kts`**:
```kotlin
plugins {
    // ...esistenti...
    alias(libs.plugins.google.services)
}
dependencies {
    // ...esistenti...
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
}
```

---

## 3. Modello dati su Firestore

Struttura piatta (come da progetto iniziale):
```
lists/{listId}
  └── items/{itemId}
        ├── name: String
        ├── checked: Boolean
        ├── order: Double        ← stesso fractional index della fase 1
        ├── deleted: Boolean      ← soft-delete: true = nel cestino
        └── updatedAt: Timestamp
```
Il **cestino** mappa 1:1: `observeItems` filtra `whereEqualTo("deleted", false)`,
`observeTrash` filtra `deleted == true`; `moveToTrash`/`restore` aggiornano il campo
`deleted` (il restore rimette anche `checked = false`); `deleteForever` cancella il
documento. Stessa interfaccia `ListRepository` della fase 1.
Il `listId` è il **"codice stanza"** condiviso: il primo dispositivo lo crea (può essere
un UUID o un codice breve leggibile), gli altri lo inseriscono una volta per unirsi.
Salva il `listId` scelto in `SharedPreferences`/`DataStore` sul dispositivo.

---

## 4. `FirestoreListRepository` (nuova classe, implementa la stessa interfaccia)

Crea `app/src/main/java/com/listaio/data/remote/FirestoreListRepository.kt`. Scheletro:

```kotlin
class FirestoreListRepository(
    private val listId: String,
    private val db: FirebaseFirestore = Firebase.firestore,
) : ListRepository {

    private fun itemsCol() = db.collection("lists").document(listId).collection("items")

    override fun observeItems(): Flow<List<ShoppingItem>> = callbackFlow {
        val reg = itemsCol().orderBy("order")
            .addSnapshotListener { snap, err ->        // ← il listener real-time
                if (err != null) { close(err); return@addSnapshotListener }
                val items = snap?.documents?.map { d ->
                    ShoppingItem(
                        id = d.id,
                        name = d.getString("name").orEmpty(),
                        checked = d.getBoolean("checked") ?: false,
                        order = d.getDouble("order") ?: 0.0,
                        updatedAt = d.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                    )
                }.orEmpty()
                trySend(items)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun addItem(name: String) {
        val trimmed = name.trim(); if (trimmed.isEmpty()) return
        val max = itemsCol().orderBy("order", Query.Direction.DESCENDING).limit(1).get().await()
            .documents.firstOrNull()?.getDouble("order") ?: 0.0
        itemsCol().add(mapOf(
            "name" to trimmed, "checked" to false,
            "order" to max + 1024.0, "updatedAt" to FieldValue.serverTimestamp(),
        )).await()
    }

    override suspend fun setChecked(id: String, checked: Boolean) {
        itemsCol().document(id).update(
            mapOf("checked" to checked, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }
    override suspend fun rename(id: String, name: String) { /* update name */ }
    override suspend fun delete(id: String) { itemsCol().document(id).delete().await() }
    override suspend fun move(id: String, newOrder: Double) {
        itemsCol().document(id).update(
            mapOf("order" to newOrder, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }
    override suspend fun clearChecked() {
        val checked = itemsCol().whereEqualTo("checked", true).get().await()
        db.runBatch { b -> checked.documents.forEach { b.delete(it.reference) } }.await()
    }
}
```

Note:
- `kotlinx.coroutines.tasks.await()` arriva da `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`.
- **Persistenza offline**: attiva di default sugli SDK moderni — al supermercato senza
  campo l'app funziona e sincronizza al ritorno della rete. Niente codice extra.
- **last-write-wins**: va benissimo per una spesa condivisa, niente merge complessi.

---

## 5. Auth anonima + collegamento in `ServiceLocator`

In `ServiceLocator.create()`, prima di costruire il repository, assicura il login anonimo
e recupera il `listId` salvato:

```kotlin
// una tantum all'avvio
FirebaseAuth.getInstance().signInAnonymously()   // invisibile all'utente
val listId = prefs.getString("listId", null) ?: return // mostra schermata "crea/unisci"
return FirestoreListRepository(listId)
```

Aggiungi una piccola schermata iniziale "Crea nuova lista / Unisciti con codice" che
imposta il `listId` la prima volta. È l'unico pezzo di UI nuovo.

---

## 6. Security Rules (Firestore → Rules)

Versione minima: utente autenticato (anche anonimo) può leggere/scrivere solo gli item
delle liste. Per stringere davvero, tieni l'elenco dei membri sul documento `lists/{id}`
e verifica l'appartenenza:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /lists/{listId} {
      allow read, write: if request.auth != null;
      match /items/{itemId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```
(Per un nucleo familiare con codice-lista segreto è sufficiente; se vuoi blindare, aggiungi
un campo `members: [uid...]` su `lists/{listId}` e condiziona le regole a
`request.auth.uid in resource.data.members`.)

---

## 7. Switch finale

In `ServiceLocator` sostituisci `LocalListRepository(...)` con `FirestoreListRepository(listId)`.
**Fine.** ViewModel, schermata, riordino, swipe, checkbox: tutto invariato.

> Vuoi tenere entrambe (offline locale + cloud)? Si può fare un repository che combina le
> due fonti, ma per iniziare lo switch secco è più semplice e Firestore già gestisce
> l'offline da solo.

---

## Evoluzione cross-platform (iOS)
Una volta su Firestore, lo stesso progetto Firebase serve identico anche iOS. Due strade:
- **Riscrittura UI nativa SwiftUI** che parla con lo stesso Firestore (logica condivisa
  solo lato backend) — semplice ma duplichi la UI.
- **Riscrivere l'app in Flutter** (un solo codice per Android+iOS) usando `cloud_firestore`:
  il modello dati e le regole restano identici, cambia solo il linguaggio della UI. È la
  strada consigliata se l'obiettivo a tendere è davvero cross-platform.
