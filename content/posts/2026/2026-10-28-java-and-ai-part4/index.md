---
title: "☕️ Java à l'ère de l'IA 🤖 - partie 4 : le RAG simple"
description: "I've seen things you people wouldn't believe. ©Roy Batty"
link: /2026-10-28-java-and-ai-part4
image: cover.webp
tags:
  - Java
  - IA
  - Code
  - RAG
author: wildagsx
---

## TL;DR
> 📚 Quatrième article de la série sur l'IA dans vos applications Java ☕️, cette fois consacré au **RAG** (_Retrieval Augmented Generation_).  
> 🤷 Un modèle ne connaît que ses données d'entraînement : vos documents internes, le programme de votre conférence ou votre doc projet, il ne les a jamais vus.  
> 🧮 Le principe : transformer vos documents et la question en vecteurs (`embeddings`), trouver le document le plus proche de la question, puis le rajouter dans le contexte.  
> 📐 La « proximité » se calcule avec une simple similarité cosinus, quelques lignes de Java suffisent.  
> ☕️ Le même use case est implémenté du plus bas niveau au plus haut : Java pur, le SDK OpenAI, LangChain4j, Quarkus et Spring AI.  
> 🐙 Tous les exemples utilisent [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) d'OVHcloud (`gpt-oss-120b` et `bge-m3`) et sont disponibles [ici](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

Petit rappel des épisodes précédents : cet article fait partie d'une série d'articles visant à expliquer comment intégrer de l'IA dans les applications que nous développons en Java ☕️.
Les trois premiers articles ont posé les bases ([partie 1](/blog/2026-07-21-java-and-ai-part1)), puis ajouté de la mémoire à notre chatbot ([partie 2](/blog/2026-08-03-java-and-ai-part2)) et enfin persisté cette mémoire ([partie 3](/blog/2026-08-27-java-and-ai-part3)).

Je ne reviendrai donc pas sur les notions de token, de fenêtre de contexte ou de mémoire.
L'objectif de cet article sera de s'attaquer à un autre sujet épineux : le **RAG**, pour _Retrieval Augmented Generation_ 📚.

On va rester sur la version la plus simple possible, celle qui permet de comprendre ce qui se passe sous le capot.
Les versions plus évoluées (découpage des documents, bases vectorielles, ...) viendront dans un prochain article.

# 🤷 Mais pourquoi faire du RAG ?

Un modèle, aussi gros soit-il, ne connaît que ce qu'il a vu lors de son entraînement.
Tout ce qui est arrivé après sa date de fin d'apprentissage, ou tout ce qui n'a jamais été public, lui est totalement inconnu.
Et c'est justement souvent ce qui nous intéresse dans nos applications : la documentation de notre projet, les procédures internes de l'entreprise, le catalogue de nos produits, ...

Pour illustrer cela, je vous propose d'essayer d'avoir des informations sur une conférence fictive : le **DevFest Atlantide 2027** 🌊 (si un jour il existe, je veux bien des royalties 😇).
Son programme et ses informations pratiques sont décrits dans deux fichiers Markdown, dans le répertoire [documents](https://github.com/philippart-s/java-ai-area-blog/tree/main/documents) du repository d'exemples :

```markdown
# DevFest Atlantide 2027 — Programme

## Compiling for the Quantum Edge

- Speaker: Maya Okonkwo
- Room: Nautilus
- Slot: Thursday 14 October, 09:30 – 10:20

How to target quantum co-processors from a classical toolchain, and why the
usual optimisation passes have to be rewritten from scratch.

## Zero-Copy Serialisation in Kotlin Multiplatform
...
```

Posons maintenant la question à notre modèle préféré, `gpt-oss-120b`, sans rien lui donner d'autre :

```text
💬: Who is speaking about quantum compilers at DevFest Atlantide 2027, and in which room?

🤖: I'm not able to retrieve the specific agenda for DevFest Atlantide 2027, so I don't
have the name of the speaker (or the room) for the "Quantum Compilers" session.
...
If you manage to locate the schedule and would like help interpreting it, feel free to
paste the relevant excerpt here—I'll be happy to clarify!
```

Plutôt honnête comme réponse (ce qui n'est pas toujours le cas avec les LLM 🤥).
Et surtout, le modèle nous donne lui-même la solution : _paste the relevant excerpt here_.
C'est exactement ce que va faire le RAG : ajouter dans le context le bon extrait de nos documents, avant de poser la question 😎.

# 🔎 Comment fonctionne le RAG ?

Si on décompose l'acronyme, on a trois étapes :
 - **Retrieval** : retrouver, parmi tous nos documents, ceux qui ont un rapport avec la question
 - **Augmented** : ajouter ces documents dans le context, en plus de la question
 - **Generation** : laisser le modèle générer la réponse à partir de ce context enrichi

Les deux dernières étapes, on sait déjà faire : c'est de la concaténation de chaînes de caractères et un appel au modèle comme dans les articles précédents.
Toute la difficulté est dans la première étape : comment savoir quel document parle de la question posée 🤔 ?

## 🔤 Pourquoi pas une recherche par mots-clés ?

La première idée qui vient en tête, c'est de chercher les mots de la question dans les documents, comme un bon vieux `grep`.
Sauf que dans notre exemple, la question parle de _quantum compilers_ et le programme de _Compiling for the Quantum Edge_ et de _quantum co-processors_.
Pas un seul _compilers_ dans le document 😅.

Ce qui nous intéresse, ce n'est pas que les mots soient identiques, c'est que le **sens** soit proche.
Et pour comparer du sens, il faut le transformer en quelque chose que l'on sait comparer : des nombres.

## 🧮 Les embeddings

Vous vous souvenez de la remarque de la [partie 1](/blog/2026-07-21-java-and-ai-part1) : dans le modèle, ce n'est pas un `token` sous forme de lettres qui est manipulé, mais un vecteur de nombres.
Les **embeddings** reposent sur la même idée, mais pour un texte entier : un modèle spécialisé, appelé modèle d'embedding, transforme un texte en un vecteur de taille fixe.

Dans nos exemples, on va utiliser le modèle [bge-m3](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/bge-m3/), lui aussi disponible sur AI Endpoints, qui produit des vecteurs de **1024 nombres**.
Que le texte fasse trois mots ou trois paragraphes, on obtient toujours un vecteur de 1024 nombres.

La propriété intéressante de ces vecteurs, c'est que deux textes qui parlent de la même chose vont donner deux vecteurs qui « pointent » dans la même direction, même s'ils n'utilisent pas les mêmes mots.

> ℹ️ Le modèle d'embedding ne génère pas de texte : il prend un texte en entrée et renvoie uniquement un vecteur. Il est donc beaucoup plus petit, plus rapide et moins cher qu'un LLM 💸.

L'appel se fait sur le même endpoint compatible OpenAI que le chat, avec le chemin `/v1/embeddings` :

```json
{
  "model": "bge-m3",
  "input": "Who is speaking about quantum compilers, and in which room?"
}
```

Et la réponse contient notre vecteur (raccourci ici, je vous épargne les 1020 autres nombres 😉) :

```json
{
  "object": "list",
  "data": [
    {
      "object": "embedding",
      "index": 0,
      "embedding": [-0.0468, -0.0149, -0.0176, 0.0041, ...]
    }
  ],
  "model": "bge-m3"
}
```

## 📐 La similarité cosinus

Une fois que l'on a des vecteurs, il reste à mesurer à quel point ils « pointent » dans la même direction.
Pour cela, la mesure la plus utilisée est la **similarité cosinus** : le cosinus de l'angle entre les deux vecteurs.

Sans refaire un cours de maths (mes souvenirs de terminale sont aussi flous que les vôtres 🫣) :
 - deux vecteurs qui vont dans la même direction ont une similarité proche de `1`
 - deux vecteurs qui n'ont rien à voir ont une similarité proche de `0`
 - deux vecteurs opposés ont une similarité proche de `-1`

Et le calcul tient en quelques lignes : le produit scalaire des deux vecteurs divisé par le produit de leurs normes.

```java
double cosine(double[] a, double[] b) {
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

## 🧩 Le RAG simple, en résumé

On a maintenant toutes les briques, voici donc ce que vont faire tous les exemples de cet article :
 1. lire chaque fichier du répertoire `documents` et calculer son embedding (un vecteur par fichier)
 2. calculer l'embedding de la question
 3. calculer la similarité cosinus entre la question et chaque document, et les trier
 4. prendre le document le plus proche et le coller dans le prompt avec la question
 5. envoyer ce prompt au modèle et afficher la réponse

> ⚠️ C'est volontairement la version la plus naïve : un document entier par vecteur, tout en mémoire, recalculé à chaque lancement. Avec deux petits fichiers ça passe très bien, avec la documentation complète de votre entreprise beaucoup moins 😅. On verra comment faire mieux plus tard.

Contrairement aux articles précédents, il n'y aura pas de version Bash.
Faire un calcul de similarité cosinus sur des vecteurs de 1024 nombres avec `jq`, c'est possible, mais en ai-je vraiment envie 🙈 ?

# ☕️ Java

## ✍️ Pure Java

On commence, comme d'habitude, sans aucune librairie d'IA : le `HttpClient` du JDK et Jackson pour le `JSON`.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.22.4
//
// Simple RAG example calling OVHcloud AI Endpoints (gpt-oss-120b),
// with the JDK HttpClient and Jackson.
//
// Every markdown file of ../documents is turned into a vector, the question is
// turned into a vector too, and the closest document is pasted into the prompt.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

record Document(String name, String text, double[] vector) {}

final String CHAT_ENDPOINT = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
final String EMBEDDINGS_ENDPOINT = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/embeddings";
final String MODEL = "gpt-oss-120b";
final String EMBEDDING_MODEL = "bge-m3";

// The token is read from OVH_AI_ENDPOINTS_ACCESS_TOKEN, exported by run.sh from .env.
final String TOKEN = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

final ObjectMapper MAPPER = new ObjectMapper();
final HttpClient CLIENT = HttpClient.newHttpClient();

void main() throws Exception {
    // Relative to the current directory: run.sh cd's into the example directory.
    var documentsDir = Path.of("..", "documents");
    var question = "Who is speaking about quantum compilers, and in which room?";

    IO.println("===== 📚 DOCUMENTS (one vector each) 📚 =====");
    var documents = new ArrayList<Document>();
    try (var files = Files.list(documentsDir)) {
        for (var file : files.filter(p -> p.toString().endsWith(".md")).sorted().toList()) {
            var text = Files.readString(file);
            var vector = embed(text);
            documents.add(new Document(file.getFileName().toString(), text, vector));

            IO.println("  " + file.getFileName() + " -> " + vector.length + " floats, starting with "
                    + Arrays.toString(Arrays.copyOf(vector, 4)));
        }
    }
    IO.println();

    IO.println("===== ❓ QUESTION ❓ =====");
    IO.println(question);
    IO.println();
    var questionVector = embed(question);

    IO.println("===== 📐 SIMILARITY (closest first) 📐 =====");
    var ranked = documents.stream()
            .sorted(Comparator.comparingDouble((Document d) -> cosine(d.vector(), questionVector)).reversed())
            .toList();
    ranked.forEach(d -> IO.println(
            String.format(Locale.ROOT, "  %.3f  %s", cosine(d.vector(), questionVector), d.name())));
    IO.println();

    // Only the best document is handed to the model.
    var best = ranked.getFirst();
    IO.println("===== 📄 RETRIEVED DOCUMENT 📄 =====");
    IO.println(best.name());
    IO.println();

    var prompt = """
            Answer the question using only the context below.

            # Context
            %s

            # Question
            %s""".formatted(best.text(), question);

    IO.println("===== 🤖 ANSWER 🤖 =====");
    IO.println(chat(prompt));
}

// Turns a text into a vector.
double[] embed(String text) throws Exception {
    var body = MAPPER.createObjectNode();
    body.put("model", EMBEDDING_MODEL);
    body.put("input", text);

    JsonNode json = post(EMBEDDINGS_ENDPOINT, body.toString());
    JsonNode values = json.at("/data/0/embedding");

    var vector = new double[values.size()];
    for (int i = 0; i < vector.length; i++) {
        vector[i] = values.get(i).asDouble();
    }
    return vector;
}

double cosine(double[] a, double[] b) {
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

String chat(String prompt) throws Exception {
    var body = MAPPER.createObjectNode();
    body.put("model", MODEL);
    var messages = body.putArray("messages");
    messages.addObject()
            .put("role", "system")
            .put("content", "provide a concise answer");
    messages.addObject()
            .put("role", "user")
            .put("content", prompt);

    return post(CHAT_ENDPOINT, body.toString())
            .at("/choices/0/message/content")
            .asText();
}

JsonNode post(String endpoint, String body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + TOKEN)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return MAPPER.readTree(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body());
}
```
|}

 - ligne 20 : un `record` pour garder ensemble le nom du fichier, son contenu et son vecteur
 - lignes 23 et 25 : un deuxième endpoint et un deuxième modèle, ceux dédiés aux embeddings
 - lignes 40 à 48 : étape 1, chaque fichier Markdown est transformé en vecteur
 - ligne 55 : étape 2, la question est transformée en vecteur avec le même modèle (⚠️ c'est important, des vecteurs issus de deux modèles différents ne sont pas comparables)
 - lignes 58 à 60 : étape 3, les documents sont triés du plus proche au plus éloigné de la question
 - ligne 66 : étape 4, on ne garde que le meilleur document...
 - lignes 71 à 78 : ... que l'on colle dans le prompt, avec la consigne de ne répondre qu'à partir de ce contexte
 - ligne 81 : étape 5, un appel au modèle tout ce qu'il y a de plus classique
 - lignes 85 à 98 : l'appel au modèle d'embedding, la réponse est un tableau de nombres dans `data[0].embedding`
 - lignes 100 à 108 : la similarité cosinus, telle que vue plus haut

> ℹ️ Il n'y a ni streaming ni mémoire dans cet exemple : une question fixe, une réponse, et on s'arrête. Le but est de se concentrer uniquement sur le RAG, mais rien ne vous empêche de combiner les deux 😉.

Et voici ce que cela donne à l'exécution :

```text
===== 📚 DOCUMENTS (one vector each) 📚 =====
  practical-info.md -> 1024 floats, starting with [0.0257415771484375, 0.0024433135986328125, -0.0141143798828125, 0.04815673828125]
  programme.md -> 1024 floats, starting with [-0.04681396484375, -0.01496124267578125, -0.0176544189453125, 0.004177093505859375]

===== ❓ QUESTION ❓ =====
Who is speaking about quantum compilers, and in which room?

===== 📐 SIMILARITY (closest first) 📐 =====
  0.614  programme.md
  0.421  practical-info.md

===== 📄 RETRIEVED DOCUMENT 📄 =====
programme.md

===== 🤖 ANSWER 🤖 =====
Maya Okonkwo is the speaker, and the talk takes place in the Nautilus room.
```

Le programme ressort bien devant les informations pratiques, alors qu'aucun des deux ne contient le mot _compilers_ 🎉.
Et cette fois, le modèle connaît la réponse : Maya Okonkwo, salle Nautilus.

Notez aussi que les informations pratiques ne sont pas à `0` : les deux documents parlent de la même conférence, il y a donc une certaine proximité.
Le score n'est pas une vérité absolue, c'est un classement relatif entre les documents.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_05_SimpleRag.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-simple-rag.mov" type="video/quicktime">
</video>

## 🛠️ Avec le SDK OpenAI

Passons au SDK officiel d'OpenAI, qui, comme dans les articles précédents, va nous débarrasser de la construction du `JSON`.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.openai:openai-java:4.94.0
//
// Simple RAG example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through the official OpenAI Java SDK (https://github.com/openai/openai-java).
//
// Every markdown file of ../documents is turned into a vector, the question is
// turned into a vector too, and the closest document is pasted into the prompt.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://platform.openai.com/docs/api-reference/embeddings

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.embeddings.EmbeddingCreateParams;

record Document(String name, String text, List<Float> vector) {}

// baseUrl must include the /v1 suffix. The token is read from
// OVH_AI_ENDPOINTS_ACCESS_TOKEN, exported by run.sh from .env.
final OpenAIClient CLIENT = OpenAIOkHttpClient.builder()
        .baseUrl("https://oai.endpoints.kepler.ai.cloud.ovh.net/v1")
        .apiKey(System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN"))
        .build();

final String MODEL = "gpt-oss-120b";
final String EMBEDDING_MODEL = "bge-m3";

void main() throws Exception {
    // Relative to the current directory: run.sh cd's into the example directory.
    var documentsDir = Path.of("..", "documents");
    var question = "Who is speaking about quantum compilers, and in which room?";

    IO.println("===== 📚 DOCUMENTS (one vector each) 📚 =====");
    var documents = new ArrayList<Document>();
    try (var files = Files.list(documentsDir)) {
        for (var file : files.filter(p -> p.toString().endsWith(".md")).sorted().toList()) {
            var text = Files.readString(file);
            var vector = embed(text);
            documents.add(new Document(file.getFileName().toString(), text, vector));

            IO.println("  " + file.getFileName() + " -> " + vector.size() + " floats, starting with "
                    + vector.subList(0, 4));
        }
    }
    IO.println();

    IO.println("===== ❓ QUESTION ❓ =====");
    IO.println(question);
    IO.println();
    var questionVector = embed(question);

    IO.println("===== 📐 SIMILARITY (closest first) 📐 =====");
    var ranked = documents.stream()
            .sorted(Comparator.comparingDouble((Document d) -> cosine(d.vector(), questionVector)).reversed())
            .toList();
    ranked.forEach(d -> IO.println(
            String.format(Locale.ROOT, "  %.3f  %s", cosine(d.vector(), questionVector), d.name())));
    IO.println();

    // Only the best document is handed to the model.
    var best = ranked.getFirst();
    IO.println("===== 📄 RETRIEVED DOCUMENT 📄 =====");
    IO.println(best.name());
    IO.println();

    var prompt = """
            Answer the question using only the context below.

            # Context
            %s

            # Question
            %s""".formatted(best.text(), question);

    IO.println("===== 🤖 ANSWER 🤖 =====");
    IO.println(chat(prompt));
}

// Turns a text into a vector.
List<Float> embed(String text) {
    var params = EmbeddingCreateParams.builder()
            .model(EMBEDDING_MODEL)
            .input(text)
            .build();

    return CLIENT.embeddings()
            .create(params)
            .data()
            .getFirst()
            .embedding();
}

double cosine(List<Float> a, List<Float> b) {
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.size(); i++) {
        dot += a.get(i) * b.get(i);
        normA += a.get(i) * a.get(i);
        normB += b.get(i) * b.get(i);
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

String chat(String prompt) {
    var params = ChatCompletionCreateParams.builder()
            .model(MODEL)
            .addSystemMessage("provide a concise answer")
            .addUserMessage(prompt)
            .build();

    return CLIENT.chat().completions().create(params)
            .choices()
            .getFirst()
            .message()
            .content()
            .orElse("");
}
```
|}

 - ligne 19 : le vecteur est maintenant une `List<Float>`, c'est le type renvoyé par le SDK
 - lignes 23 à 26 : un seul client pour les deux modèles, le chat et l'embedding
 - lignes 83 à 94 : l'appel au modèle d'embedding, avec des objets typés plutôt que du `JSON` à parcourir
 - lignes 96 à 104 : la similarité cosinus, toujours à notre charge
 - lignes 106 à 119 : l'appel au modèle de chat, comme dans les articles précédents

Le `main` est identique à celui de la version pure Java : le SDK simplifie les appels HTTP, mais il ne sait rien du RAG.
Lire les documents, comparer les vecteurs, choisir le meilleur et construire le prompt, tout cela reste notre travail 😅.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_05_SimpleRag.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-sdk-simple-rag.mov" type="video/quicktime">
</video>

## 🦜 Avec LangChain4j

Avec [LangChain4j](https://docs.langchain4j.dev/tutorials/rag), on change de niveau : le RAG fait partie des fonctionnalités du framework.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j:1.22.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.22.0
//DEPS org.slf4j:slf4j-nop:2.0.20
//
// Simple RAG example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through LangChain4j AI Services (https://github.com/langchain4j/langchain4j).
//
// The markdown files of ../documents are loaded into an in-memory embedding
// store, and a content retriever is registered on the AI Service: the question
// is matched against the store and the best document is added to the prompt
// before the model sees it.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://docs.langchain4j.dev/tutorials/rag

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

interface Assistant {
    @SystemMessage("provide a concise answer")
    String chat(String userMessage);
}

void main() {
    // baseUrl must include the /v1 suffix. The token is read from
    // OVH_AI_ENDPOINTS_ACCESS_TOKEN, exported by run.sh from .env.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Relative to the current directory: run.sh cd's into the example directory.
    var documentsDir = Path.of("..", "documents");
    var question = "Who is speaking about quantum compilers, and in which room?";

    ChatModel chatModel = OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName("gpt-oss-120b")
            .build();

    EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName("bge-m3")
            .build();

    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

    // No documentSplitter: one segment per file, so a whole document is matched
    // and retrieved at once.
    IO.println("===== 📚 DOCUMENTS 📚 =====");
    var documents = FileSystemDocumentLoader.loadDocuments(documentsDir);
    documents.forEach(d -> IO.println("  " + d.metadata().getString("file_name")));
    EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(store)
            .build()
            .ingest(documents);
    IO.println();

    // Registered on the AI Service: every call goes through it, the matching
    // document is prepended to the prompt and the model never sees the rest.
    var retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .maxResults(1)
            .build();

    Assistant assistant = AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .contentRetriever(retriever)
            .build();

    IO.println("===== ❓ QUESTION ❓ =====");
    IO.println(question);
    IO.println();

    IO.println("===== 🤖 ANSWER 🤖 =====");
    IO.println(assistant.chat(question));
}
```
|}

 - lignes 52 à 56 : le modèle d'embedding, configuré comme le modèle de chat
 - ligne 58 : l'**embedding store**, l'endroit où sont rangés les vecteurs (ici en mémoire, c'est l'équivalent de notre `ArrayList<Document>`)
 - ligne 63 : le chargement de tous les fichiers du répertoire `documents`
 - lignes 65 à 69 : l'**ingestor** calcule le vecteur de chaque document et le range dans le _store_, c'est notre étape 1
 - lignes 74 à 78 : le **content retriever** calcule le vecteur de la question, cherche le document le plus proche dans le _store_ et n'en garde qu'un (`maxResults(1)`), ce sont nos étapes 2 à 4
 - ligne 82 : on branche le _retriever_ sur l'AI Service, comme on branchait la mémoire dans la partie 2
 - ligne 90 : l'appel à l'assistant, qui ne prend que la question

Plus de similarité cosinus, plus de tri, plus de prompt à construire à la main : tout cela est fait par LangChain4j à chaque appel de `chat` 😎.

> ℹ️ Comme le calcul de similarité est fait par le framework, cet exemple n'affiche plus les scores ni le document retenu. Pour voir ce qui est réellement envoyé au modèle, vous pouvez activer `.logRequests(true)` sur le `ChatModel` (en remplaçant `slf4j-nop` par `slf4j-simple`) : la question est suivie de `Answer using the following information:` puis du contenu du document le plus proche.

> ⚠️ Par défaut, LangChain4j ne découpe pas les documents s'il n'a pas de `DocumentSplitter` : on a donc un vecteur par fichier, comme dans les exemples précédents. C'est voulu ici pour comparer ce qui est comparable, mais en pratique on découpe presque toujours les documents en morceaux (on en reparlera 😉).

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_06_SimpleRag.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="langchain4j-simple-rag.mov" type="video/quicktime">
</video>

## ⚡️ Avec Quarkus

Quarkus va encore plus loin grâce à l'extension [Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html) de `quarkus-langchain4j`.
Comme son nom l'indique, son but est de rendre le RAG simple, voire invisible.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
// jboss-threads needs java.lang opened on Java 24+ (thread-local reset capability).
//JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED
// easy-rag pulls the whole Apache Tika suite, which loads a native library.
//JAVA_OPTIONS --enable-native-access=ALL-UNNAMED
//DEPS io.quarkus.platform:quarkus-bom:3.33.4.1@pom
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai:1.14.2
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-easy-rag:1.14.2
// Tika logs through log4j-api, which ships without a binding: route it into the
// Quarkus log manager instead of letting it print "could not find a provider".
//DEPS org.apache.logging.log4j:log4j-to-slf4j:2.25.5
//
// Simple RAG example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through Quarkus + quarkus-langchain4j
// (https://github.com/quarkiverse/quarkus-langchain4j).
//
// Single-file Quarkus application run by JBang. The easy-rag extension ingests
// ../documents at startup and registers the retriever on the AI service, so the
// only thing left here is the question. Everything is in rag.properties.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://docs.quarkiverse.io/quarkus-langchain4j/dev/easy-rag.html

// Mounted as application.properties, so the other examples keep theirs.
//FILES application.properties=rag.properties

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

@RegisterAiService
interface Assistant {
    @SystemMessage("provide a concise answer")
    String chat(String userMessage);
}

@QuarkusMain
public class _04_05_SimpleRag implements QuarkusApplication {

    @Inject
    Assistant assistant;

    // @ActivateRequestContext makes the CDI request scope available for the call.
    @Override
    @ActivateRequestContext
    public int run(String... args) {
        var question = "Who is speaking about quantum compilers, and in which room?";

        IO.println("===== ❓ QUESTION ❓ =====");
        IO.println(question);
        IO.println();

        IO.println("===== 🤖 ANSWER 🤖 =====");
        IO.println(assistant.chat(question));
        return 0;
    }
}
```
|}

Oui, c'est tout 🤯.
Pas de chargement de documents, pas de modèle d'embedding, pas de _store_ ni de _retriever_ : le code ne contient que l'AI Service et la question.

 - ligne 9 : l'extension Easy RAG, c'est elle qui fait tout le travail
 - lignes 5, 6 et 10 à 12 : Easy RAG embarque [Apache Tika](https://tika.apache.org/) pour lire tous types de documents (PDF, Word, ...), ce qui demande quelques réglages pour que tout se passe bien avec Java 26 et les logs
 - ligne 26 : la configuration est dans un fichier dédié, `rag.properties`, monté en tant que `application.properties`
 - lignes 35 à 39 : un AI Service tout ce qu'il y a de plus classique, rien n'indique qu'il fait du RAG
 - ligne 58 : l'appel, avec uniquement la question

Tout le reste se passe dans la configuration :

{|
```properties
# Used by the RAG example only, mounted as application.properties by its //FILES
# directive. The other examples of this directory keep application.properties.

quarkus.banner.enabled=false
quarkus.log.level=WARN

quarkus.langchain4j.openai.base-url=https://oai.endpoints.kepler.ai.cloud.ovh.net/v1
quarkus.langchain4j.openai.api-key=${OVH_AI_ENDPOINTS_ACCESS_TOKEN}
quarkus.langchain4j.openai.chat-model.model-name=gpt-oss-120b
quarkus.langchain4j.openai.embedding-model.model-name=bge-m3

# Everything below replaces the loading, splitting, embedding, storing and
# retrieving code: the extension ingests the directory at startup into an
# in-memory store and registers the retriever on every AI service.
quarkus.langchain4j.easy-rag.path=../documents
quarkus.langchain4j.easy-rag.path-matcher=glob:**.md
# Large enough to keep one segment per file.
quarkus.langchain4j.easy-rag.max-segment-size=2000
quarkus.langchain4j.easy-rag.max-results=1
```
|}

 - ligne 10 : le modèle d'embedding, qui utilise la même configuration (URL et clé) que le modèle de chat
 - lignes 15 et 16 : le répertoire à ingérer au démarrage, et uniquement les fichiers Markdown
 - ligne 18 : Easy RAG découpe les documents par défaut (en morceaux de 300 tokens), on augmente donc la taille maximum d'un morceau pour garder un vecteur par fichier
 - ligne 19 : on ne garde que le document le plus proche, comme dans les autres exemples

> ℹ️ L'ingestion est faite à chaque démarrage de l'application : si vous ajoutez un document dans le répertoire, il sera pris en compte au prochain lancement, sans changer une ligne de code.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_05_SimpleRag.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="quarkus-simple-rag.mov" type="video/quicktime">
</video>

## ☘️ Avec Spring AI

Pour finir, voyons comment [Spring AI](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html) gère le RAG.
Comme pour la mémoire, cela va passer par un _advisor_.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.2@pom
//DEPS org.springframework.ai:spring-ai-bom:2.0.2@pom
//DEPS org.springframework.boot:spring-boot-starter:4.1.2
// AspectJ weaver, required by Spring AI's ChatClient advisors (AOP).
//DEPS org.aspectj:aspectjweaver:1.9.26
//DEPS org.springframework.ai:spring-ai-starter-model-openai:2.0.2
//DEPS org.springframework.ai:spring-ai-vector-store:2.0.2
//DEPS org.springframework.ai:spring-ai-rag:2.0.2
// Servlet API present only so Boot's web-servlet config classes resolve during
// bean introspection; the app stays non-web (spring.main.web-application-type=none).
//DEPS jakarta.servlet:jakarta.servlet-api:6.1.0
//
// Simple RAG example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through Spring AI (https://docs.spring.io/spring-ai/reference/).
//
// The markdown files of ../documents are read into an in-memory vector store,
// and a retrieval advisor is registered on the ChatClient: the question is
// matched against the store and the best document is added to the prompt
// before the model sees it.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html

// Mounted as application.properties, so the other examples keep theirs.
//FILES application.properties=rag.properties

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// NOT @SpringBootApplication: this file lives in the default package, so its
// @ComponentScan would scan the whole classpath and break Spring's own config.
@SpringBootConfiguration
@EnableAutoConfiguration
public class _05_05_SimpleRag {

    // Relative to the current directory: run.sh cd's into the example directory.
    private static final Path DOCUMENTS_DIR = Path.of("..", "documents");

    public static void main(String[] args) {
        // The retrieval advisor runs on a thread pool whose workers are not
        // daemons and never expire when idle, so the JVM would never leave on
        // its own. exit() closes the context, System.exit() leaves.
        System.exit(SpringApplication.exit(
                SpringApplication.run(_05_05_SimpleRag.class, args)));
    }

    @Bean
    CommandLineRunner run(ChatClient.Builder builder, EmbeddingModel embeddingModel) {
        return args -> {
            var question = "Who is speaking about quantum compilers, and in which room?";

            // TextReader keeps one document per file, no splitting.
            IO.println("===== 📚 DOCUMENTS 📚 =====");
            var documents = new ArrayList<Document>();
            try (var files = Files.list(DOCUMENTS_DIR)) {
                for (var file : files.filter(p -> p.toString().endsWith(".md")).sorted().toList()) {
                    IO.println("  " + file.getFileName());
                    documents.addAll(new TextReader(new FileSystemResource(file)).get());
                }
            }

            // add() embeds every document and keeps the vectors in memory.
            var vectorStore = SimpleVectorStore.builder(embeddingModel).build();
            vectorStore.add(documents);
            IO.println();

            // Registered as a default advisor: every call searches the store and
            // prepends what it finds to the prompt.
            var advisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .topK(1)
                            .build())
                    .build();

            var chatClient = builder
                    .defaultSystem("provide a concise answer")
                    .defaultAdvisors(advisor)
                    .build();

            IO.println("===== ❓ QUESTION ❓ =====");
            IO.println(question);
            IO.println();

            IO.println("===== 🤖 ANSWER 🤖 =====");
            IO.println(chatClient.prompt()
                    .user(question)
                    .call()
                    .content());
        };
    }
}
```
|}

 - lignes 9 et 10 : deux dépendances en plus, une pour le _vector store_ en mémoire et une pour l'_advisor_ dédié au RAG
 - ligne 27 : comme pour Quarkus, une configuration dédiée, qui ajoute uniquement le modèle d'embedding (`spring.ai.openai.embedding.options.model=bge-m3`)
 - lignes 61 et 62 : l'_advisor_ de RAG laisse derrière lui des threads qui empêchent la JVM de s'arrêter, d'où le `System.exit` 🙄
 - lignes 73 à 78 : le chargement des fichiers avec un `TextReader`, un document par fichier
 - lignes 81 et 82 : le **vector store** en mémoire, qui calcule le vecteur de chaque document à l'ajout, c'est notre étape 1
 - lignes 87 à 92 : l'_advisor_ de RAG, qui cherche le document le plus proche de la question (`topK(1)`) et l'ajoute au prompt, ce sont nos étapes 2 à 4
 - ligne 96 : on branche l'_advisor_ sur le `ChatClient`, comme pour la mémoire dans la partie 2
 - lignes 104 à 107 : l'appel, avec uniquement la question

On retrouve exactement les mêmes concepts qu'avec LangChain4j, avec un vocabulaire différent : _vector store_ au lieu d'_embedding store_, _advisor_ au lieu de _retriever_ branché sur l'AI Service.

Le prompt construit par l'_advisor_ (via son `ContextualQueryAugmenter`) est d'ailleurs plus directif que le nôtre :

{|
```text
Context information is below.

---------------------
{context}
---------------------

Given the context information and no prior knowledge, answer the query.

Follow these rules:

1. If the answer is not in the context, just say that you don't know.
2. Avoid statements like "Based on the context..." or "The provided information...".

Query: {query}

Answer:
```
|}

Demander au modèle de dire qu'il ne sait pas plutôt que d'inventer est un bon réflexe pour limiter les hallucinations.
Effet de bord amusant : avec en plus notre _provide a concise answer_, la réponse devient franchement laconique 😅.

```text
===== 🤖 ANSWER 🤖 =====
Maya Okonkwo, Nautilus.
```

> ℹ️ Ce prompt se personnalise en passant votre propre `ContextualQueryAugmenter` à l'_advisor_ avec `.queryAugmenter(...)`.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_05_SimpleRag.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="spring-simple-rag.mov" type="video/quicktime">
</video>

# 🔭 Et après ?

On ne va pas se mentir, ce RAG est volontairement simpliste et ne passera pas l'épreuve de la production :
 - **un vecteur par document** : avec un document de 200 pages, le vecteur représente un « sens moyen » de tout le document et la recherche devient beaucoup moins précise, sans compter que le document entier ne tiendra peut-être pas dans la fenêtre de contexte du modèle
 - **tout est en mémoire** : les vecteurs sont recalculés à chaque lancement, ce qui coûte du temps et des appels au modèle d'embedding 💸
 - **un seul document retenu** : si la réponse est répartie dans plusieurs documents, le modèle n'aura qu'une partie de l'information

Les réponses à ces problèmes existent, et les frameworks les proposent déjà :
 - le **découpage** des documents en morceaux (_chunks_ ou _segments_), avec un peu de recouvrement entre eux pour ne pas couper une information en deux
 - les **bases de données vectorielles** pour stocker les vecteurs une fois pour toutes : PostgreSQL avec `pgvector`, Qdrant, Milvus, Elasticsearch, Redis, ...
 - le réglage du **nombre de résultats** et d'un **score minimum** pour ne garder que les documents réellement pertinents

Vous retrouverez les détails ici :
- [LangChain4j](https://docs.langchain4j.dev/tutorials/rag)
- [Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag.html)
- [Spring AI](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)

# 🏁 Conclusion

Voilà pour cette première approche du RAG.
Derrière l'acronyme un peu intimidant, il n'y a au final que trois idées : transformer du texte en vecteurs, comparer ces vecteurs, et coller le résultat dans le prompt.
Une centaine de lignes de Java sans librairie suffit pour le faire, et les frameworks le réduisent à quelques lignes, voire à quelques propriétés de configuration avec Quarkus.

Le prochain article sera certainement consacré à un RAG plus réaliste : découpage des documents et stockage des vecteurs dans une vraie base de données.
Mais ça, c'est une autre histoire.

L'ensemble du code source est disponible dans le dépôt GitHub [java-ai-area-blog](https://github.com/philippart-s/java-ai-area-blog).

Si vous êtes arrivés jusque-là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
