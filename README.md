# 🧊 Scado

Un'app Android per ricordarti i cibi che stanno per scadere, con un **frigo
condiviso** tra le persone di casa.

## Cosa fa

- **«Ho fatto la spesa»**: quando torni a casa tocchi il pulsante (o la
  scorciatoia tenendo premuta l'icona dell'app) e Scado ti chiede **un prodotto
  alla volta**: nome e data di scadenza. La data si sceglie con il selettore
  di date nativo di Android (Material 3), oppure con le scorciatoie rapide
  (domani, 3 giorni, 1 settimana…). «Avanti» salva e passa al prodotto
  successivo, «Ho finito» chiude.
- **Più notifiche per prodotto**: per ogni prodotto puoi scegliere quando
  essere avvisato (il giorno stesso, 1, 2, 3, 7 giorni prima, oppure un numero
  di giorni a piacere). I promemoria proposti di default e l'orario delle
  notifiche si cambiano nelle Impostazioni.
- **Frigo condiviso e sincronizzato**: ogni nucleo familiare ha il suo frigo
  (un piccolo database). Solo i membri ne vedono il contenuto; per entrare
  serve il codice invito di 6 caratteri, che trovi nelle Impostazioni.
  Funziona anche offline: le modifiche si sincronizzano appena torna la rete.
- La lista è ordinata per scadenza e colorata per urgenza. Scorri un prodotto
  di lato per toglierlo dal frigo (con «Annulla»), toccalo per modificarlo.

## Come è fatta

| Parte | Tecnologia |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Account | Firebase Authentication (email + password) |
| Database e sincronizzazione | Cloud Firestore (cache offline inclusa) |
| Notifiche | WorkManager, programmate localmente su ogni telefono |

Struttura dei dati su Firestore:

```
users/{uid}                        profilo + frigo di appartenenza
households/{id}                    il frigo: nome, membri, codice invito
households/{id}/products/{pid}     i prodotti
households/{id}/joins/{uid}        prova che il membro conosceva il codice
invites/{codice}                   codice invito → frigo
```

Le regole di sicurezza sono in [`firestore.rules`](firestore.rules): sono loro
a garantire che ogni frigo sia visibile solo ai suoi membri.

Le notifiche sono calcolate su ogni telefono: quando la lista cambia (anche
perché l'altro membro ha aggiunto qualcosa) l'app riprogramma i promemoria, e
ogni 6 ore un lavoro in background riscarica la lista anche se l'app non è
stata aperta.

## Configurazione (una volta sola)

Serve un progetto Firebase. Il piano gratuito (Spark) basta e avanza.

1. Vai su <https://console.firebase.google.com> e crea un progetto
   (Google Analytics non serve).
2. **Authentication** → *Inizia* → *Metodo di accesso* → abilita
   **Email/password**.
3. **Firestore Database** → *Crea database* → scegli una località europea
   (es. `eur3`) → modalità produzione.
4. In Firestore apri la scheda **Regole**, incolla il contenuto di
   [`firestore.rules`](firestore.rules) e pubblica.
5. Nelle impostazioni del progetto (⚙️) → *Aggiungi app* → **Android** con
   nome del pacchetto `it.scado`. Scarica il file `google-services.json`.
6. Metti il file nell'app in uno di questi due modi:
   - **Build con GitHub Actions** (consigliato): nel repository vai su
     *Settings → Secrets and variables → Actions → New repository secret*, nome
     `GOOGLE_SERVICES_JSON`, valore: tutto il contenuto del file.
   - **Build in locale con Android Studio**: copia il file in
     `app/google-services.json` (è già in `.gitignore`).

## Compilare e installare

**Con GitHub Actions**: a ogni push la action *Build APK* compila l'app.
Apri l'esecuzione nella scheda *Actions* e scarica l'artifact `scado-apk`:
contiene l'APK da installare sui telefoni (va permessa l'installazione da
origini sconosciute). Senza il secret `GOOGLE_SERVICES_JSON` l'APK si compila
lo stesso, ma non si collega a Firebase.

**In locale**: apri la cartella con Android Studio e premi *Run*, oppure:

```bash
./gradlew assembleDebug
```

L'APK firmato con la chiave inclusa nel repository (`app/debug.keystore`) si
installa sopra le versioni precedenti senza dover disinstallare.

## Primo utilizzo in due

1. La prima persona crea un account e un frigo («Casa di …»).
2. Da *Impostazioni → Invita* manda il codice all'altra persona.
3. La seconda persona crea il suo account e sceglie *Entra nel frigo* con il
   codice. Da quel momento vedete gli stessi prodotti.

Se in futuro altre famiglie useranno l'app, ognuna creerà il proprio frigo con
il proprio codice: i contenuti restano separati.
