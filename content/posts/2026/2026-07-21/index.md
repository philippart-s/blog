---
title: "☕️ Java à l'ère de l'IA 🤖 - partie 1 : les bases"
description: "I’m sorry, Dave. I’m afraid I can’t do that. ©HAL 9000"
link: /2026-07-21-java-and-ai-part1
#image: cover.svg
tags:
  - Java
  - AI
  - Code
author: wildagsx
---

🏴󠁧󠁢󠁥󠁮󠁧󠁿 You can find the English version of this article [here]({site.url}2026-07-21-java-and-ai-part1-en) 🏴󠁧󠁢󠁥󠁮󠁧󠁿.

## TL;DR
> 🚀 Premier article d'une série pour apprendre à **ajouter de l'IA générative (LLM) dans vos applications Java**.
> 🧠 On (re)pose d'abord les notions de base : `token`, fenêtre de contexte, paramètres/poids, `température` et API.
> 💬 On construit un chat bot simple (avec `prompt système` et streaming) que l'on fera évoluer tout au long de la série.
> ☕️ Le même use case est implémenté du plus bas niveau au plus haut : CURL, Java pur, le SDK OpenAI, LangChain4j, Quarkus et Spring AI.
> 🔌 Tous les exemples utilisent [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) d'OVHcloud et le modèle open weight `gpt-oss-120b`.
> 🐙 Code et démo disponibles [ici](https://github.com/pigumax/Java-and-AI-Part1).

<br/>

# 📜 Introduction
Ah l'été, ses plages, ses apéros... et ses bonnes résolutions 🤣 !
La mienne sera de commencer une série d'articles autour de l'Intelligence Artificielle (IA) et de Java ! 🤖 ☕️
Pourquoi me direz-vous faire de tels articles en 2026 ?
Tout simplement parce que je me rends compte avec les talks et ateliers que je donne, que l'IA reste une zone un peu floue pour bon nombre de développeuses et développeurs lorsqu'il s'agit de l'ajouter dans les applications plutôt que de l'utiliser.

Je vous propose donc de faire le tour du propriétaire, calmement.
On ne verra peut-être pas tout, mais je vais essayer de vous donner le plus d'éléments possibles pour commencer votre voyage au sein de l'IA en Java.

Alors, on va parler essentiellement d'IA générative et plus particulièrement de Large Language Model (LLM).

> Je vais prendre le temps d'écrire cette série à la main, pas de slop 😆 ! 
> Non pas que je n'aime pas l'IA, mais je vais en profiter aussi pour approfondir certains concepts et donc prendre le temps d'apprendre 😉.
> Ne vous étonnez donc pas du temps entre les articles ⏳.

# 🤖 Quelques notions de base sur les LLM et leurs utilisations

Pour bien commencer, on va prendre quelques minutes pour revoir ensemble quelques éléments essentiels pour comprendre le code à venir dans cet article et les suivants.

## 🧩 Un token

Commençons par une notion clé dans le monde des LLM : le `token`.
Un `token` c'est un peu l'équivalent d'un mot dans une phrase.
C'est une manière de découper le texte pour qu'il puisse être compréhensible et analysable par le modèle.
Sauf qu'ici un mot tel qu'on le connait ne correspond pas forcément à un `token`.
Par exemple pour le français très souvent, il faut en moyenne 4 `tokens` pour 3 mots (même si ce n'est pas une règle absolue qui peut varier selon les modèles).

> ℹ️ En réalité dans le modèle ce n'est pas un token sous forme de lettres qui est manipulé, mais un vecteur de nombres.

Et pourquoi est-ce important 🤔 ?

Tout simplement parce que c'est ce qui va vous être facturé 💸 ! 

Attention donc à votre consommation, d'autant que la plupart du temps la facturation est différente entre l'input et l'output.

## 🪟 La fenêtre de contexte

C'est un concept qui est lié à celui des tokens.
À savoir que c'est le nombre de tokens maximum que le modèle peut traiter lors d'une requête que ce soit en input ou en output.

Et selon les modèles, vous allez avoir une taille plus ou moins grande, qui va vous permettre d'envoyer plus ou moins de données (mais aussi d'en recevoir).

## 🎛️ Les paramètres (ou poids)

On parle de manière assez similaire de poids et de paramètres pour les modèles (même si ce n'est pas tout à fait la même chose, cela n'a pas d'importance pour nous).

Le nombre de paramètres d'un modèle est très souvent le nombre dans le nom qui précède un B.
Le B étant pour milliards, cela représente donc le nombre de paramètres du modèle en milliards.
Par exemple mon-LLM-2B est un modèle qui a 2 milliards de paramètres.

Mais au final, c'est bien d'avoir beaucoup de paramètres 🧐 ?

Le nombre de paramètres va représenter la capacité du modèle à apprendre des choses plus ou moins différentes et profondes.
Bien entendu, cela dépendra au final des données d'apprentissage 😉.

Donc oui avec beaucoup de paramètres, c'est bien, mais pour certaines tâches ce n'est pas nécessaire et surtout attention à la puissance matérielle nécessaire pour des gros modèles (aka avec beaucoup de paramètres).

Une formule simple pour calculer la mémoire nécessaire pour un modèle : _2 octets x Nombre de paramètres_.
Puis, on ajoute 20% pour les activations.

Par exemple pour un petit modèle de 2 milliards de paramètres, on est donc à 4 Go de RAM pour faire tourner le modèle 😱 !
Prenons un exemple de modèle moyen avec 35 milliards de paramètres, il nous faudra donc 84 Go de RAM 💥.
Et pour un gros modèle, avec 200 milliards de paramètres, il nous faudra 480 Go de RAM 😱 !

Il va donc falloir trouver un compromis entre la capacité d'apprentissage et l'infrastructure nécessaire pour votre modèle.

Rien ne vous empêche de tester ce que je vais vous présenter sur des plus petits modèles en self hosting.
Pour tout ce qui touche ce domaine, je ne saurais trop vous conseiller tout le travail effectué par [Philippe Charrière](https://k33g.org/) sur le sujet 🤩.

> Dans cette série d'articles, je vais utiliser les modèles d'OVHcloud à travers le produit [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/).
> J'ai la chance de travailler pour OVHcloud et d'avoir accès à ces modèles 😆.
 
Pour les différents exemples, je vais utiliser le modèle open weight [oss-gpt-120B](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/) d'OVHcloud.
C'est un modèle qui a de bonnes performances et permet de couvrir tous nos use cases avec une taille "raisonnable".

## 🌡️ La température

Dans les différents exemples de code que l'on va découvrir ensemble, on positionnera un paramètre nommé `temperature`. 
En deux mots la `température` va vous permettre de rendre votre modèle plus ou moins créatif et déterministe.
Plus vous allez augmenter la `température` (généralement entre 0 et 1.0 ou 2.0 selon le fournisseur), plus le modèle sera créatif, et inversement, plus vous la baissez vers 0, plus il sera déterministe.

## 🌐 API

Dernière notion que je veux présenter : comment accéder à vos modèles (locaux ou distants).
Là, à peu près tout le monde fait la même chose : exposer une API qui consomme / produit un payload JSON.

> Chez OVHcloud, on a fait le choix de prendre comme standard de fait l'API d'OpenAI.
> C'est pourquoi la majorité des exemples de code que vous verrez par la suite utiliseront des librairies compatibles.

# 💬 Use case

Je ne vais pas être original pour le use case des exemples, ce sera un chat bot.
Si ce use case est très connu, cela me permettra de rajouter au fil du temps les différentes notions dont vous aurez besoin lors de vos ajouts d'IA dans vos développements.
Cela me permet aussi de tester simplement différentes approches selon les Frameworks choisis.

On fera donc un chat simple avec ou sans streaming.
Avec ou sans mémoire, RAG, function calling, MCP, agentic, skills,...

On reviendra sur ce que sont tous ces termes barbares à chaque partie qui couvrira comment les implémenter dans vos applications Java.

> J'essaierai de modifier cet article au fur et à mesure pour avoir les liens vers chaque sous article qui portera sur une de ces notions.

Commençons donc par la base : un chat bot simple avec un prompt utilisatrice ou utilisateur et la possibilité de faire une réponse en streaming ou non.
Ce sera le seul exemple où l'on utilisera toutes les technos / frameworks.
En effet, parfois, il sera beaucoup trop complexe de ne pas utiliser des frameworks.
Cela ne veut pas dire que ce n'est pas possible, mais mon objectif n'est pas de réécrire un SDK complet 🙃.

Le découpage de ce use case est donc le suivant :
 1. envoi d'un prompt et affichage de la réponse une fois celle-ci générée par le modèle
 2. utilisation d'un `prompt système`
 3. activation du mode streaming pour voir la réponse arriver au fil de l'eau

Mais c'est quoi un `prompt système` 🤔 ?
Voyez ça comme une _orientation du comportement_ du modèle : on va lui indiquer une certaine direction à suivre dans ses réponses.
Par exemple, si vous demandez à un modèle : "à quoi sert une assiette ?".
Il y a de grandes chances qu'il vous réponde "à manger".
Si vous positionnez un `prompt système` (on verra comment) : "Tu es un spécialiste en aéronautique".
Il y a de grandes chances qu'il réponde : "À stabiliser un avion."

> Au-delà de "donner une orientation", le prompt système sert souvent à définir un ton, des contraintes, un format de réponse, des règles de comportement, un persona, etc.

# 🖥️ La base : CURL

Avant de se lancer en Java, voyons quelques requêtes simples dans un bash avec CURL.
L'idée n'est pas de faire tous les exemples avec cette manière, ce serait beaucoup trop complexe 😅.
Mais notre premier use case est réalisable avec cette approche donc autant en profiter.

## 💬 Envoi d'un prompt et affichage de la réponse

{|
```bash
#!/usr/bin/env bash
# Load the access token from the .env file located at the project root.
set -a
source "$(dirname "$0")/../.env"
set +a

# 1️⃣ OVHcloud AI Endpoints configuration.
ENDPOINT="https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions"
MODEL="gpt-oss-120b"

# Ask the user for a prompt.
read -rp "⌨️ Your prompt: " USER_PROMPT

echo

# 2️⃣ Build the JSON request body.
# jq safely encodes the user input into valid JSON.
BODY=$(jq -n --arg model "$MODEL" --arg content "$USER_PROMPT" '{
  model: $model,
  messages: [
    { role: "user", content: $content }
  ]
}')

# Print the JSON payload sent to the model, pretty-printed with jq.
echo "===== ⬆️ JSON REQUEST (payload sent to the model) ⬆️ ====="
echo "$BODY" | jq .
echo

# 3️⃣ Send the request and store the raw response returned by the endpoint. (3)
RESPONSE=$(curl -s "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${OVH_AI_ENDPOINTS_ACCESS_TOKEN}" \
  -d "$BODY")

# 4️⃣ Print the JSON response, pretty-printed with jq for readability. (4)
echo "===== ⬇️ JSON RESPONSE ⬇️ ====="
echo "$RESPONSE" | jq .

# 5️⃣ Print only the answer, extracted from the JSON with jq.
echo
echo "===== 🤖 ANSWER 🤖 ====="
echo "$RESPONSE" | jq -r '.choices[0].message.content'
```
|}

Voyons un peu dans le détail ce que l'on fait dans ce code.

L'étape 1️⃣ positionne notamment l'API key pour AI Endpoints afin de ne pas avoir de rate limite.
> À noter que sans cette clef, l'exemple fonctionne, mais vous êtes limité à 2 requêtes par IP par minute.

L'étape 2️⃣ construit le payload que l'on envoie, avec un prompt du type "Why is the sky blue? (provide a concise answer)":
```json
{
  "model": "gpt-oss-120b",  (2.1)
  "messages": [             (2.2)
    {
      "role": "user",       (2.3)
      "content": "Why is the sky blue? (provide a concise answer)"  (2.4)
    }
  ]
}
```
On a ici une structure minimaliste pour envoyer une requête à notre modèle :
 - `2.1` : le modèle que l'on veut utiliser, ici `gpt-oss-120b`. En effet, plusieurs modèles sont disponibles.
 - `2.2` : un tableau avec une liste de messages à envoyer. Pour notre premier exemple, il n'y en a qu'un seul.
 - `2.3` : le rôle du persona qui envoie le message, ici, c'est vous (on verra que ça va changer) avec le role de type `user`
 - `2.4` : le prompt qui va être utilisé.

L'étape 3️⃣ consiste à appeler le endpoint en ajoutant au payload le token dans le header.

L'étape 4️⃣ est la réponse brute :
```json
{
  "id": "chatcmpl-affdf154315ba79c",
  "choices": [                        (4.1)
    {
      "index": 0,
      "message": {                    (4.2)
        "role": "assistant",          (4.3)
(4.4)   "content": "The sky looks blue because molecules and tiny particles in Earth’s atmosphere scatter sunlight. Short‑wavelength (blue) light is scattered about 10 times more efficiently than red light (Rayleigh scattering), so when sunlight passes through the atmosphere, the blue component is redirected in all directions and reaches our eyes from every part of the sky. At sunrise or sunset the light travels through a longer atmospheric path, scattering away the blue and letting the red hues dominate.",
(4.5)   "reasoning": "User asks: \"Why is the sky blue? (provide a concise answer)\". Need concise answer. Probably 2-3 sentences. Provide explanation: scattering of short-wavelength (blue) light by atmosphere (Rayleigh scattering). Mention sun's white light, scattering more efficient for shorter wavelengths, gives blue appearance. Also mention sunrise/sunset reddening due to longer path. Provide concise answer.",   
        "tool_calls": []
      },
      "finish_reason": "stop"       
    }
  ],
  "created": 1784652024,
  "model": "gpt-oss-120b",
  "object": "chat.completion",
  "usage": {                        (4.6)
    "prompt_tokens": 79,
    "completion_tokens": 183,
    "total_tokens": 262
  }
}
```

Voyons ce que l'on a dans cette réponse (je passerai de manière volontaire sur certains champs que l'on verra dans les prochains articles) :
 - `(4.1)` la réponse est dans un tableau, dans notre cas avec un seul élément, mais ça va vite changer 😉
 - `(4.2)` comme pour l'envoi, on a une structure `messages` qui contient la réponse
 - `(4.3)` cette fois le `role` est `assistant`, cela représente le modèle
 - `(4.4)` la réponse du modèle (les tokens générés)
 - `(4.5)` le raisonnement utilisé par le modèle. En effet, le modèle que l'on utilise est un modèle dit avec "raisonnement". En deux mots : le prompt va être découpé en sous-étapes pour avoir ce que l'on appelle la chaîne de pensée. C'est plus efficace, mais plus lent et consomme plus de tokens. 
 - `(4.6)` on retrouve la consommation en nombre de tokens pour ce simple prompt : 
   - le nombre de tokens envoyés (votre prompt) : 79
   - le nombre de tokens générés (réponse du modèle) : 183 (⚠️ y compris le raisonnement ⚠️)
   - donc au total 262 tokens

L'étape 5️⃣ est tout simplement l'extraction de la réponse pour que ce soit plus lisible.

### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Bash-SimpleChatbot.mov" type="video/quicktime">
</video>


## 📏 Activation du prompt système

Maintenant que l'on a la base de notre chatbot ajoutons la notion de prompt système.
On va se servir du prompt système pour éviter de devoir rajouter la notion "(provide a concise answer)" et qu'à chaque demande elle soit tout de même appliquée.

Pour cela, il faut modifier l'étape 2️⃣ en rajoutant le prompt système comme suit :
```bash
# Build the JSON request body.
# jq safely encodes the user input into valid JSON.
BODY=$(jq -n --arg model "$MODEL" --arg content "$USER_PROMPT" '{
  model: $model,
  messages: [
    { role: "system", content: "provide a concise answer" },
    { role: "user", content: $content }
  ]
}')
```

Ce qui donne :
```json
{
  "model": "gpt-oss-120b",
  "messages": [
    {             
      "role": "system",                      
      "content": "provide a concise answer"
    },
    {
      "role": "user",
      "content": "Why is the sky blue?"
    }
  ]
}
```
On voit ici un nouveau rôle qui permet d'ajouter le prompt système : `system`.

### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Bash-SimpleChatbot-SystemPrompt.mov" type="video/quicktime">
</video>

## 📏 Activation du streaming

Le mode streaming va nous permettre d'afficher les tokens les uns après les autres dès que le LLM les a générés sans attendre qu'il ait fini de générer la réponse complètement.
Cela améliore grandement l'expérience utilisatrice et utilisateur 🤗.

Pour activer le streaming, c'est assez simple, il suffit d'ajouter dans le payload `"stream": true`.
La modification la plus complexe concerne l'affichage du résultat.
En effet, chaque token va arriver au fil de l'eau via SSE, il va donc falloir modifier le code pour afficher ces tokens au fil de l'eau.

{|
```bash
#!/usr/bin/env bash

# Load the access token from the .env file located at the project root.
# The .env file is expected to contain: OVH_AI_ENDPOINTS_ACCESS_TOKEN=...
set -a
source "$(dirname "$0")/../.env"
set +a

# 1️⃣ OVHcloud AI Endpoints configuration.
ENDPOINT="https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions"
MODEL="gpt-oss-120b"

# Ask the user for a prompt.
read -rp "⌨️  Your prompt: " USER_PROMPT

echo

# 2️⃣ Build the JSON request body.
# The key difference with the simple version is "stream": true.
# jq safely encodes the user input into valid JSON.
BODY=$(jq -n --arg model "$MODEL" --arg content "$USER_PROMPT" '{
  model: $model,
  stream: true,
  messages: [
    { role: "system", content: "provide a concise answer" },
    { role: "user", content: $content }
  ]
}')

# Print the JSON payload sent to the model, pretty-printed with jq.
echo "===== ⬆️ JSON REQUEST (payload sent to the model) ⬆️ ====="
echo "$BODY" | jq .
echo

# 3️⃣ Send the request and stream the answer.
# -N (--no-buffer) disables curl output buffering so chunks are printed live.
echo "===== 🤖 ANSWER (streaming) 🤖 ====="
curl -sN "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${OVH_AI_ENDPOINTS_ACCESS_TOKEN}" \
  -d "$BODY" \
| while IFS= read -r line; do
    # DEBUG: uncomment to print the raw SSE line exactly as received.
    # echo "$line"

    # SSE lines look like: "data: {json}" and end with "data: [DONE]".
    line="${line#data: }"                 # strip the "data: " prefix
    [ -z "$line" ] && continue            # skip empty keep-alive lines
    [ "$line" = "[DONE]" ] && break       # end of the stream

    # Extract the incremental text from this chunk and print it without newline.
    printf '%s' "$(echo "$line" | jq -rj '.choices[0].delta.content // empty')"
  done

# Final newline once the stream is complete.
echo
```
|}

L'étape 1️⃣ ne change pas et la seule modification que l'on a faite dans l'étape 2️⃣ est l'activation du mode streaming.

```json
{
  "model": "gpt-oss-120b",
  "stream": true,           (2.1)
  "messages": [
    {
      "role": "system",
      "content": "provide a concise answer"
    },
    {
      "role": "user",
      "content": "Why is the sky blue?"
    }
  ]
}
```
 - `(2.1)` activation du mode streaming (false par défaut)

L'étape 3️⃣ change un petit peu de par la réception de chaque token.

```json
data: {
  "id":"chatcmpl-bb5885faaeb2adf7",
  "choices":[
    {
      "index":0,
      "delta":
      {
        "role":"assistant","content":""}    (3.1)
      }
  ],
  "created":1784709442,
  "model":"gpt-oss-120b",
  "object":"chat.completion.chunk"}         (3.2)

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":"The","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":" user","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":" asks","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":":","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":" \"","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":"Why","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}

data: {"id":"chatcmpl-bb5885faaeb2adf7","choices":[{"index":0,"delta":{"role":"assistant","reasoning":" is","name":"assistant"}}],"created":1784709442,"model":"gpt-oss-120b","object":"chat.completion.chunk"}
```
Ce n'est qu'un extrait de toutes les lignes générées (ici le début de la partie raisonnement).
 - `(3.1)` le token généré
 - `(3.2)` indique que ce n'est qu'une partie du texte généré

### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Bash-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

# ☕️ Java

Maintenant que l'on a une bonne idée de comment cela fonctionne sans aide extérieure passons à quelque chose de plus intéressant, Java 🤩.

## ✍️ Pure Java

Commençons par transposer simplement le code bash sans aide extérieure.

### 💬 Envoi d'un prompt et affichage de la réponse

{|
```java
/// usr/bin/env jbang "$0" "$@" ; exit $?                   1️⃣
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  final String endpoint = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
  final String model = "gpt-oss-120b";
  final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

  var mapper = new ObjectMapper();

  // Ask the user for a prompt.
  var userPrompt = IO.readln("⌨️ Your prompt: ");
  IO.println();

  // 2️⃣ Build the JSON request body with Jackson.
  ObjectNode body = mapper.createObjectNode();
  body.put("model", model);
  var messages = body.putArray("messages");
  messages.addObject()
      .put("role", "user")
      .put("content", userPrompt);

  // Print the JSON payload sent to the model, pretty-printed.
  IO.println("===== ⬆️ JSON REQUEST (payload sent to the model) ⬆️ =====");
  IO.println(mapper.writerWithDefaultPrettyPrinter()
      .writeValueAsString(body));
  IO.println();

  // 3️⃣ Send the request and get the full response as a string.
  HttpResponse<String> response;
  try (var client = HttpClient.newHttpClient()) {
    var request = HttpRequest.newBuilder(URI.create(endpoint))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + token)
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
        .build();
    response = client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  // Print the JSON response, pretty-printed for readability.
  JsonNode json = mapper.readTree(response.body());
  IO.println("===== ⬇️ JSON RESPONSE ⬇️ =====");
  IO.println(mapper.writerWithDefaultPrettyPrinter()
      .writeValueAsString(json));

  // 4️⃣ Print only the answer, extracted from the JSON.
  IO.println();
  IO.println("===== 🤖 ANSWER 🤖 =====");
  IO.println(json.at("/choices/0/message/content")
      .asText());
}
```
|}

L'étape 1️⃣ nous permet d'utiliser [JBang](https://www.jbang.dev/), la bonne nouvelle est que l'on va être très proche de ce que l'on fait avec un script bash.
L'étape 2️⃣, à la syntaxe Java près, est la même chose que l'étape 2️⃣ en Bash, cela prépare le payload à envoyer au endpoint.
```json
{
  "model" : "gpt-oss-120b",
  "messages" : [ {
    "role" : "user",
    "content" : "Why is the sky blue? (provide a concise answer)"
  } ]
}
```

L'étape 3️⃣ est la même que pour l'étape 3️⃣ en Bash, appeler l'API avec le payload.
Et enfin l'étape 4️⃣ récupère la réponse et n'affiche que la partie `content` du body.
```json
{
  "id" : "chatcmpl-92a3f9eb6921559e",
  "choices" : [ {
    "index" : 0,
    "message" : {
      "role" : "assistant",
      "content" : "The sky appears blue because molecules in the Earth’s atmosphere scatter short‑wavelength (blue) light from the Sun far more efficiently than longer‑wavelength (red) light—a phenomenon called **Rayleigh scattering**. When we look in any direction away from the Sun, the scattered blue light reaches our eyes from all parts of the sky, giving it its characteristic color.",
      "reasoning" : "User asks for concise answer why sky blue. Provide explanation: scattering of sunlight by molecules, Rayleigh scattering, shorter wavelengths scatter more, etc. Concise. Probably 2-3 sentences.",
      "tool_calls" : [ ]
    },
    "finish_reason" : "stop"
  } ],
  "created" : 1784724572,
  "model" : "gpt-oss-120b",
  "object" : "chat.completion",
  "usage" : {
    "prompt_tokens" : 79,
    "completion_tokens" : 124,
    "total_tokens" : 203
  }
}
```
Je ne détaille pas la réponse qui est exactement la même qu'en bash.

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-SimpleChatbot.mov" type="video/quicktime">
</video>

### 📏 Activation du prompt système

Vous l'aurez deviné, l'activation du prompt système va être comme en bash.
Ajouter le prompt système au payload à l'étape 2️⃣.
```java
  // 2️⃣ Build the JSON request body with Jackson.
  ObjectNode body = mapper.createObjectNode();
  body.put("model", model);
  var messages = body.putArray("messages");
   messages.addObject().put("role", "system").put("content", "provide a concise answer");  // 👈🏼 Add System Prompt
   messages.addObject()
      .put("role", "user")
      .put("content", userPrompt);
  // ...
```

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-SimpleChatbot-SystemPrompt.mov" type="video/quicktime">
</video>

## 📏 Activation du streaming

Je pense que vous l'avez compris maintenant, là encore pour le streaming c'est juste le fait que le code est écrit en Java et non en Bash qui change.
La logique est la même (ajout de `"stream": true` au payload dans l'étape 2️⃣), et les payloads et body aussi.

{|
```java
/// usr/bin/env jbang "$0" "$@" ; exit $? 1️⃣
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  final String endpoint = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
  final String model = "gpt-oss-120b";
  final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

  var mapper = new ObjectMapper();

  // Ask the user for a prompt.
  var userPrompt = IO.readln("⌨️ Your prompt: ");
  IO.println();

  // 2️⃣ Build the JSON request body. The key difference is "stream": true.
  ObjectNode body = mapper.createObjectNode();
  body.put("model", model);
  body.put("stream", true);
  var messages = body.putArray("messages");
  messages.addObject()
      .put("role", "system")
      .put("content", "provide a concise answer");
  messages.addObject()
      .put("role", "user")
      .put("content", userPrompt);

  // Print the JSON payload sent to the model, pretty-printed.
  IO.println("===== ⬆️  JSON REQUEST (payload sent to the model) ⬆️  =====");
  IO.println(mapper.writerWithDefaultPrettyPrinter()
      .writeValueAsString(body));
  IO.println();

  // 3️⃣ Send the request. BodyHandlers.ofInputStream() lets us read the SSE
  // stream ourselves, line by line, as chunks arrive.
  var client = HttpClient.newHttpClient();
  var request = HttpRequest.newBuilder(URI.create(endpoint))
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer " + token)
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
      .build();
  var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

  // 4️⃣ Print the answer token by token.
  IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
  try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
    String line;
    while ((line = reader.readLine()) != null) {
      // DEBUG: uncomment to print the raw SSE line exactly as received.
      // IO.println(line);

      // SSE lines look like: "data: {json}" and end with "data: [DONE]".
      if (!line.startsWith("data: ")) continue;   
      var data = line.substring("data: ".length());
      if (data.equals("[DONE]")) break;           

      // Extract the incremental text from this chunk and print it without newline.
      var content = mapper.readTree(data)
          .at("/choices/0/delta/content");
      if (!content.isMissingNode()) {
        IO.print(content.asText());
      }
    }
  }

  // Final newline once the stream is complete.
  IO.println();
}
```
|}

L'étape 4️⃣ affiche les tokens les uns après les autres, le body reçu est le même qu'en Bash.

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

## 🛠️ Avec le SDK OpenAI

Dans cette partie on va voir ce que cela donne avec l'utilisation d'un SDK.
Afin de ne pas être trop répétitif je vous propose de créer directement le chat bot final (streaming + prompt système).
J'utilise le [SDK officiel](https://github.com/openai/openai-java) d'OpenAI comme les endpoints d'OVHcloud sont compatibles avec les API OpenAI.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.openai:openai-java:4.42.0

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.LogLevel;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

void main() {
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Ask the user for a prompt.
    var userPrompt = IO.readln("⌨️ Your prompt: ");
    IO.println();

    // 1️⃣ Build the SDK client, pointed at OVHcloud AI Endpoints.
    OpenAIClient client = OpenAIOkHttpClient.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
        //    .logLevel(LogLevel.DEBUG)
            .build();

    // 2️⃣ Build the request with the SDK's typed builder (no manual JSON).
    var params = ChatCompletionCreateParams.builder()
            .model(model)
            .addSystemMessage("provide a concise answer")
            .addUserMessage(userPrompt)
            .build();

    // 3️⃣ Call the endpoint in streaming mode and print the answer token by token.
    IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
    try (StreamResponse<ChatCompletionChunk> stream =
                 client.chat().completions().createStreaming(params)) {
        stream.stream()
                .flatMap(chunk -> chunk.choices().stream())
                .flatMap(choice -> choice.delta().content().stream())
                .forEach(IO::print);
    }

    // Final newline once the stream is complete.
    IO.println();
}
```

L'étape 1️⃣ nous permet d'initialiser le client en choisissant le modèle que l'on souhaite utiliser et en positionnant certains paramètres de haut niveau (comme les logs par exemple).
L'étape 2️⃣  nous permet de passer les différents prompts et potentiellement d'autres options que l'on verra plus tard.
En parlant des logs, si on active le `debug` on peut voir ce qui est envoyé :
```json
{"messages":[
  {"content":"provide a concise answer","role":"system"},     (1.1)
  {"content":"Why is the sky blue?","role":"user"}],          (1.2)
  "model":"gpt-oss-120b",                                     (1.3)
  "stream":true                                               (1.4)
}
```

On retrouve ce que l'on a déjà vu :
 - `(1.1)` le prompt système
 - `(1.2)` le prompt utilisatrice ou utilisateur
 - `(1.3)` le modèle à utiliser
 - `(1.4)` l'activation du streaming

À ce stade vous vous demandez peut-être comment le streaming a pu être activé ?
C'est à l'étape 3️⃣ que cela passe, lorsque l'on utilise le client pour appeler la méthode `createStreaming` le flag de streaming est implicitement activé à `true`.

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-SDK-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

## 🦜 Avec LangChain4j

On avance dans notre simplification d'écriture de code, cette fois on va utiliser un Framework de plus haut niveau avec [LangChain4j](https://docs.langchain4j.dev/).
C'est mon Framework préféré en Java pour l'ajout d'IA dans une application.
On va le voir cela simplifie grandement le code.
Comme pour le SDK, je vais faire l'exemple le plus complet dès le début de cette sous-partie.
Je vais utiliser le mode [AI Services](https://docs.langchain4j.dev/tutorials/ai-services) qui permet une plus grande abstraction.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j:1.18.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.18.0
//DEPS  org.slf4j:slf4j-simple:2.0.17

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.CompletableFuture;

// 1️⃣ The AI Service: a plain interface returning a TokenStream for streaming.
interface Assistant {
    @SystemMessage("provide a concise answer")
    TokenStream chat(String userMessage);
}

void main() {
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Ask the user for a prompt.
    var userPrompt = IO.readln("⌨️  Your prompt: ");
    IO.println();

    // 2️⃣ Build the underlying LangChain4j streaming model, pointed at OVHcloud AI Endpoints.
    StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName(model)
            //.logRequests(true)
            //.logResponses(true)
            .build();

    // 3️⃣ Create the AI Service backed by the streaming model.
    Assistant assistant = AiServices.create(Assistant.class, chatModel);

    // Streaming is asynchronous: the callbacks run on another thread. Following
    // the LangChain4j docs, we bridge that back to main() with a CompletableFuture
    // that is completed on onCompleteResponse (or failed on onError).
    var futureResponse = new CompletableFuture<ChatResponse>();

    // 4️⃣ Call the endpoint in streaming mode and print the answer token by token.
    IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
    assistant.chat(userPrompt)
            .onPartialResponse(IO::print)
            .onCompleteResponse(futureResponse::complete)
            .onError(futureResponse::completeExceptionally)
            .start();

    // Block until the stream completes (or throw if it failed).
    futureResponse.join();

    // Final newline once the stream is complete.
    IO.println();
}
```
|}

Ce qui est bien avec LangChain4j est que l'on va passer en mode déclaratif / builder.
Bien entendu, vous pouvez ne pas utiliser les AI Services et avoir un code un peu plus verbeux mais que vous pouvez customiser plus précisément.

L'étape 1️⃣ nous permet d'indiquer comment on souhaite dialoguer avec le modèle.
Si je traduis en français ce que fait cette interface :
 - on souhaite créer un assistant (`interface Assistant`)
 - avec comme moyen d'accéder au modèle une méthode `chat` qui prend une chaîne de caractères en entrée (`String userMessage`) qui représente le prompt utilisatrice ou utilisateur. Et qui renvoie la réponse en streaming (`TokenStream`)
 - et on positionne le prompt système grâce à l'annotation `@SystemMessage("provide a concise answer")`

Ensuite l'étape 2️⃣ nous permet de configurer le modèle que l'on souhaite utiliser.
Comme nous souhaitons activer le mode streaming, on utilise un  `StreamingChatModel` puis le builder `OpenAiStreamingChatModel`.
Il ne nous reste plus qu'à positionner l'URL, la clé d'API et le nom du modèle à utiliser.
Si vous activez les logs, vous voyez le détail de ce qui est envoyé :
```json
{
  "model" : "gpt-oss-120b",
  "messages" : [ {
    "role" : "system",
    "content" : "provide a concise answer"
  }, {
    "role" : "user",
    "content" : "Why is the sky blue?"
  } ],
  "stream" : true,
  "stream_options" : {
    "include_usage" : true
  }
}
```

L'étape 3️⃣  permet d'assembler la définition du modèle et de ce que l'on veut en faire en créant un assistant grâce aux AI Services.

Enfin, l'étape 4️⃣  permet de faire l'appel au modèle en streaming et d'afficher la réponse token par token. Pour cela on utilise un `CompletableFuture` qui va permettre de gérer la fin du stream.

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-Quarkus-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>


## ⚡️ Avec Quarkus

On remonte encore d'un cran pour aller vers des Frameworks que l'on peut qualifier d'entreprise.
Le premier sera [Quarkus](https://quarkus.io/) avec l'extension [quarkus-langchain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html).

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
// jboss-threads needs java.lang opened on Java 24+ (thread-local reset capability).
//JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED
//DEPS io.quarkus.platform:quarkus-bom:3.33.2@pom
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai:1.12.0

// 1️⃣ Include application.properties as a classpath resource so Quarkus reads it.
//FILES application.properties

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

// 2️⃣ The AI Service: returning Multi<String> switches quarkus-langchain4j to
// streaming mode. 
@RegisterAiService
interface Assistant {
  @SystemMessage("provide a concise answer")
  Multi<String> chat(String userMessage);
}

// 3️⃣ Command-mode entry point: @QuarkusMain + QuarkusApplication run in a shell.
@QuarkusMain
public class _04_02_StreamingChatbot implements QuarkusApplication {

  // 4️⃣ The generated AI service is a CDI bean, injected here.
  @Inject
  Assistant assistant;

  // @ActivateRequestContext makes the CDI request scope available for the call.
  @Override
  @ActivateRequestContext
  public int run(String... args) {
    // Ask the user for a prompt.
    var userPrompt = IO.readln("⌨️  Your prompt: ");
    IO.println();

    // 5️⃣ Call the endpoint in streaming mode and print the answer token by token.
    // asStream() blocks this thread until the reactive stream completes.
    IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
    assistant.chat(userPrompt)
        .subscribe().asStream()
        .forEach(IO::print);

    return 0;
  }
}
```
|}
On commence à avoir un code plus que concis et pourtant cela fait toujours la même chose.

L'étape 1️⃣ charge le fichier `application.properties` dans le classpath.
Ce fichier contient la configuration du modèle utilisé, on ne le fait plus dans le code.
{|
```properties
# Keep the console output clean for a CLI chatbot.
quarkus.banner.enabled=false
quarkus.log.level=WARN

# Point the OpenAI-compatible client at OVHcloud AI Endpoints and pick the model.
quarkus.langchain4j.openai.base-url=https://oai.endpoints.kepler.ai.cloud.ovh.net/v1    (1.1)
quarkus.langchain4j.openai.chat-model.model-name=gpt-oss-120b                           (1.2)
quarkus.langchain4j.openai.api-key=${OVH_AI_ENDPOINTS_ACCESS_TOKEN}                     (1.3)

# Uncomment to log the raw requests/responses:
#quarkus.langchain4j.openai.log-requests=true
#quarkus.langchain4j.openai.log-responses=true
```
|}

 - `(1.1)` dénote l'URL à laquelle se connecter
 - `(1.2)` précise le modèle à utiliser
 - `(1.3)` récupère le token d'API via une variable d'environnement

L'étape 2️⃣  configure comment on va interagir avec le modèle via l'utilisation des `AI Services`.
L'activation se fait par le biais de l'utilisation de l'annotation `@RegisterAiService`.
Le streaming est activé en indiquant comme type de retour `Multi<String>`

L'étape 3️⃣ nous permet d'utiliser Quarkus en mode main simple (sans lancer de serveur).
L'étape 4️⃣ injecte le service défini en 2️⃣  pour pouvoir l'utiliser dans le main.

Enfin l'étape 5️⃣ permet d'afficher le résultat de l'appel.

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-Quarkus-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

## ☘️ Avec Spring AI

Dernière implémentation, pour ne pas faire de jaloux, [Spring AI](https://spring.io/projects/spring-ai).
La logique ressemble beaucoup à ce que l'on a vu avec Quarkus et LangChain4j, je ne vais donc pas trop m'attarder.
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.0@pom
//DEPS org.springframework.ai:spring-ai-bom:2.0.0@pom
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//DEPS org.aspectj:aspectjweaver:1.9.25.1
//DEPS org.springframework.ai:spring-ai-starter-model-openai:2.0.0
//DEPS jakarta.servlet:jakarta.servlet-api:6.1.0

// 1️⃣ Include application.properties as a classpath resource so Spring reads it.
//FILES application.properties

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

@SpringBootConfiguration
@EnableAutoConfiguration
public class _05_02_StreamingChatbot {

    public static void main(String[] args) {
        SpringApplication.run(_05_02_StreamingChatbot.class, args);
    }

    @Bean
    CommandLineRunner run(ChatClient.Builder builder) {
        return args -> {
            var chatClient = builder.build();

            // Ask the user for a prompt.
            var userPrompt = IO.readln("⌨️  Your prompt: ");
            IO.println();

            // 2️⃣ Call the endpoint in streaming mode and print the answer token by token.
            IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
            Flux<String> stream = chatClient.prompt()
                    .system("provide a concise answer")
                    .user(userPrompt)
                    .stream()
                    .content();

            stream.doOnNext(IO::print)
                    .blockLast();
        };
    }
}
```

L'étape 1️⃣ permet toujours de configurer le modèle dans un fichier `application.properties`.
{|
```properties
# Command-line app: no web server, no banner, quiet logs.
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.level.root=WARN
# The single file lives in the default package; silence the harmless warning about
# @EnableAutoConfiguration there (we scan no repositories/entities anyway).
logging.level.org.springframework.boot.autoconfigure.AutoConfigurationPackages=ERROR

spring.ai.openai.base-url=https://oai.endpoints.kepler.ai.cloud.ovh.net/v1
spring.ai.openai.chat.options.model=gpt-oss-120b
spring.ai.openai.api-key=${OVH_AI_ENDPOINTS_ACCESS_TOKEN}
```
|}
Et enfin l'étape 2️⃣ permet de configurer l'appel en activant le mode streaming (`stream()`).

#### 📽️ Voyons ça en action !
<video width="100%"  controls>
  <source src="Java-SpringAI-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

# 🏁 Conclusion

Et voilà !
Ce premier article est beaucoup trop long 😅.
Mais je me devais de poser les bases et il me semblait bon aussi de tout de suite illustrer cela par un exemple simple de chat bot.

Pour la suite on continuera à essayer d'ajouter des fonctionnalités dans notre chat bot.
On verra si on sera capable de conserver toutes les approches illustrées ici.

La prochaine étape va consister à rajouter un peu de mémoire à notre chat bot 🧠.

Vous pouvez retrouver l'ensemble du code source dans le [repo GitHub](https://github.com/philippart-s/java-ai-area-blog) de l'article.

Si vous êtes arrivés jusque-là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
