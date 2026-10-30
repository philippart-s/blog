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
> 🧮 Le principe : transformer vos documents et la question en vecteurs (`embeddings`), trouver le document le plus proche de la question, puis le coller dans le prompt.  
> 📐 La « proximité » se calcule avec une simple similarité cosinus, quelques lignes de Java suffisent.  
> ☕️ Le même use case est implémenté du plus bas niveau au plus haut : Java pur, le SDK OpenAI, LangChain4j, Quarkus et Spring AI.  
> 🐙 Tous les exemples utilisent [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) d'OVHcloud (`gpt-oss-120b` et `bge-m3`) et sont disponibles [ici](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

Petit rappel des épisodes précédents : cet article fait partie d'une série d'articles visant à expliquer comment intégrer de l'IA dans les applications que nous développons en Java ☕️.
Les trois premiers articles ont posé les bases ([partie 1](/blog/2026-07-21-java-and-ai-part1)), puis ajouté de la mémoire à notre chatbot ([partie 2](/blog/2026-08-03-java-and-ai-part2)) et enfin persisté cette mémoire ([partie 3](/blog/2026-08-27-java-and-ai-part3)).

Je ne reviendrai donc pas sur les notions de `token`, de fenêtre de contexte ou de mémoire.
L'objectif de cet article sera de s'attaquer à un autre sujet épineux, promis à la fin de l'article précédent : le **RAG**, pour _Retrieval Augmented Generation_ 📚.

On va rester sur la version la plus simple possible, celle qui permet de comprendre ce qui se passe sous le capot.
Les versions plus évoluées (découpage des documents, bases vectorielles, ...) viendront dans un prochain article.

# 🤷 Mais pourquoi faire du RAG ?

Un modèle, aussi gros soit-il, ne connaît que ce qu'il a vu lors de son entraînement.
Tout ce qui est arrivé après sa date de fin d'apprentissage, ou tout ce qui n'a jamais été public, lui est totalement inconnu.
Et c'est justement souvent ce qui nous intéresse dans nos applications : la documentation de notre projet, les procédures internes de l'entreprise, le catalogue de nos produits, ...

Pour illustrer cela, j'ai inventé une conférence : le **DevFest Atlantide 2027** 🌊 (si un jour il existe, je veux bien des royalties 😇).
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

Plutôt honnête comme réponse (on a déjà vu des modèles inventer un speaker et une salle avec un aplomb remarquable 🤥).
Et surtout, le modèle nous donne lui-même la solution : _paste the relevant excerpt here_.
C'est exactement ce que va faire le RAG : coller dans le prompt le bon extrait de nos documents, avant de poser la question 😎.

# 🔎 Comment fonctionne le RAG ?

Si on décompose l'acronyme, on a trois étapes :
 - **Retrieval** : retrouver, parmi tous nos documents, ceux qui ont un rapport avec la question
 - **Augmented** : ajouter ces documents dans le prompt, en plus de la question
 - **Generation** : laisser le modèle générer la réponse à partir de ce prompt enrichi

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
Faire un calcul de similarité cosinus sur des vecteurs de 1024 nombres avec `jq`, c'est possible, mais je vais garder un peu d'amour-propre 🙈.

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

<!-- TODO : SDK OpenAI, LangChain4j, Quarkus (Easy RAG), Spring AI, conclusion -->
