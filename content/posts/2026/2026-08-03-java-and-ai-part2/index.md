---
title: "☕️ Java à l'ère de l'IA 🤖 - partie 2 : la mémoire"
description: "I’m sorry, Dave. I’m afraid I can’t do that. ©HAL 9000"
link: /2026-08-03-java-and-ai-part2
image: cover.png
tags:
  - Java
  - IA
  - Code
author: wildagsx
---

🏴󠁧󠁢󠁥󠁮󠁧󠁿 You can find the English version of this article [here]({site.url}2026-07-21-java-and-ai-part1-en) 🏴󠁧󠁢󠁥󠁮󠁧󠁿.

## TL;DR
> 

<br/>

# 📜 Introduction

Petit rappel des épisodes précédent : cet article fait partie d'une série d'articles visant à expliquer comment intégrer de l'IA dans les applications que nous développons en Java ☕️.
Il est la suite de l'[article précédent](2026-07-21-java-and-ai-part1) qui avait pour but de poser les bases de comment développer une application avec de l'IA, comme un chatbot par exemple.

Je ne reviendrai donc pas sur les éléments à connaître pour débuter votre voyage dans l'IA.
L'objectif de cet article sera de voir pourquoi la première chose que vous allez vouloir faire avec votre chatbot c'est de gérer la mémoire 💿.

# 💿 Mais pourquoi gérer la mémoire ?

Prenons un de nos exemples précédents, par exemple la version [LangChain4j](https://docs.langchain4j.dev/) (au hasard 😇).

Modifions la fin de la sorte, pour poser 2 questions les unes après les autres :

```java
    var futureResponse = new CompletableFuture<ChatResponse>();

    IO.println("💬: My name is Stéphane");
    IO.println();
    IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
    assistant.chat("💬: My name is Stéphane")
            .onPartialResponse(IO::print)
            .onCompleteResponse(t -> {
                IO.println();
                IO.println("💬: What is my name?");
                IO.print("🤖: ");
                assistant.chat("What is my name?")
                    .onPartialResponse(IO::print)
                    .onCompleteResponse(futureResponse::complete)
                    .onError(futureResponse::completeExceptionally)
                    .start();
                }
            )
            .onError(futureResponse::completeExceptionally)
            .start();

    // Block until the stream completes (or throw if it failed).
    futureResponse.join();

    // Final newline once the stream is complete.
    IO.println();
```
Rien de bien fou ici, on a juste ajouté une question à la suite de la réponse à la première question (lignes `9 à 17`).

Voyons le résultat : 
```bash
💬: My name is Stéphane

===== 🤖 ANSWER (streaming) 🤖 =====
Nice to meet you, Stéphane! How can I assist you today?
💬: What is my name?
🤖: I don’t know your name—could you tell me what it is?
```

Plutôt étrange non 🤔? 
On ne peut pas dire que la demande soit très complexe et que l'information remonte à très loin dans la conversation 💬.

Mais alors pourquoi avec vos chatbots préférés ils semblent se souvenir parfaitement de toute votre conversation et pas le notre avec une conversation ayant simplement deux échanges 😳 ?

Tout simplement parce que vos modèles préférés n'ont pas d'état (on dit donc qu'ils sont `stateless`).
Et c'est là que l'on se rend compte que sans le client (la CLI, le site web,...) votre chatbot offre une bien piètre expérience utilisatrice et utilisateur.
Très souvent c'est donc côté client (au sens de la partie logicielle qui appelle le modèle) qui va gérer cette mémoire.

Je vous propose de voir les différents choix qui s'offrent à vous, mais aussi les difficultés qu'il vous faudra solutionner lorsque vous souhaitez ajouter un peu de mémoire à votre chatbot.

# 🔎 Comment fonctionne la mémoire ?

En soi le mécanisme de la mémoire pour un modèle est assez simple : il suffit de lui renvoyer l'ensemble (ou une partie) de la conversation courante pour qu'il puisse l'inclure dans son contexte et donc générer les bons tokens en prenant en compte non pas la dernière question mais l'ensemble de la conversation 📚.

## 💸 Money, money, money

Et là normalement une lumière s'allume 🚨: des mots étant des tokens, les tokens étant facturés... 
Plus la mémoire de votre chatbot sera grande, plus vous allez envoyer de tokens.
Et ce à chaque question !

Oui 💸...

Et même pour une toute petite question.
Imaginons que vous faites générer un livre de 500 pages par votre chatbot.
A chaque question ensuite, non seulement votre question sera envoyée mais aussi votre livre de 500 pages 😳.

On voit tout de suite qu'il va falloir mettre en place des mécanismes pour s'éviter cela.

## 📦 Limiter la taille de la mémoire 

Cela paraît être une approche assez évidente (voire naïve) mais qui fonctionne 👍.
Voyez ça comme une pile, LIFO (**L**ast **I**n **F**irst **O**ut) à taille fixe.
Et une fois la taille maximum atteinte, le message le plus ancien est supprimé.

Simple et efficace.
Efficace pour la taille, mais potentiellement risqué pour le sens et la précision de vos futurs échanges.

Il se peut que les premiers messages soient importants pour positionner le contexte et que sans eux, votre modèle perde petit à petit le but initial de la conversation.
Pire, imaginons, que votre pile ait une taille de 10, et que les dix derniers messages soient totalement sans rapport avec le début de votre conversation, à partir du onzième message votre modèle aura _oublié_ la raison de l'échange que vous avez avec lui 😣.

> J'en profite pour aussi insister sur le fait que lorsque vous utilisez votre chatbot préféré, mélanger les discussions n'est pas une bonne idée.
> La plupart des interfaces permettent de créer plusieurs fils de discussions, faites-le.
> Un par sujet afin que votre modèle soit le plus optimum possible et indépendant de la façon dont la mémoire est gérée.
 
Bien entendu, il y a des variantes.
Par exemple vous pouvez choisir ce que vous allez garder en mémoire en fonction de la pertinence de l'échange.
Ce choix peut être piloté par l'utilisatrice ou l'utilisateur ou via votre chatbot avec un calcul de pertinence de la réponse (souvent délégué à un autre modèle et donc consommant des tokens 💸).

## 🗜️Compresser la mémoire

Et si on avait un moyen de compresser la mémoire comme on le fait avec les fichiers par exemple 💡 ?

C'est effectivement un mécanisme que la plupart des clients implémentent.
Notamment quand vous voyez le message `compacting...`.
Mais attention ici, pas question de compression avec perte d'information pour ensuite recréer la totalité de l'information.
La compression fonctionne de manière différente : le client va déléguer (au modèle courant ou un autre) de lui résumer le fil de la conversation depuis la dernière fois qu'il a fait un résumé (en l'incluant bien entendu).

Cela fonctionne plutôt bien et est un bon compromis pour gagner des tokens sans perte totale d'information 🗃️.
Il y a, cependant, une limite.
Plus votre session va être longue, plus vous allez faire disparaître petit à petit des notions qui ne reviennent pas souvent dans votre discussion mais qui sont pour autant importantes 😶‍🌫️.
C'est d'ailleurs ce que vous constatez par vous-même avec vos chatbots habituels.

> Cette impression qu'à un moment donné le chatbot _tourne en rond_ ou répond de manière moins pertinente, voire sans rapport avec votre demande initiale de votre discussion 🔄.
> C'est souvent le signal pour repartir sur une nouvelle session en reposant quelques notions de base.

## 🥷 Le prompt caching

Ce n'est pas à proprement parler une façon de gérer la mémoire de votre conversation, mais plus un moyen d'optimiser celle-ci.
L'idée derrière est de stocker les prompts avec leurs réponses, puis de retourner directement la réponse déjà calculée si le même prompt (ou un approchant) est envoyé au modèle.
La difficulté étant de savoir quels couples _prompts / réponses_ stocker et comment déterminer qu'un prompt ressemble à un autre.
On verra plus tard l'utilisation de bases de données sémantiques (grâce aux vecteurs) qui peuvent être de bons candidats.

> Cela peut être aussi une alternative à la mémoire classique : on stocke tous les échanges dans une base de données sémantique.
> Puis à chaque requête on fait une recherche dans la base pour retourner les précédents échanges pertinents pour les injecter dans la mémoire que l'on envoie au modèle.

On peut aussi imaginer demander à un modèle plus petit de déterminer si deux prompts se ressemblent (cela sera souvent moins coûteux que de générer deux fois la même réponse avec un gros modèle).

## 🪟 La fenêtre de contexte à prendre en compte

Souvenez-vous, les modèles ont une taille maximum de nombre de tokens qu'ils peuvent recevoir, on appelle ça la `fenêtre de contexte` 🪟.
Il faut donc la prendre en compte pour la gestion de la mémoire.
Si votre mémoire est trop importante le risque est de la saturer très vite et de ne plus pouvoir rien envoyer ou recevoir 💥.
Là encore, si vous créez un chatbot permettant de se connecter à plusieurs modèles, il faudra être en capacité de récupérer la taille de cette fenêtre de contexte pour créer une mémoire en correspondance.

# 💿 Stockage des données

On a vu ensemble les différentes approches de gestion de la mémoire.
Maintenant, attardons-nous sur le stockage de cette donnée 💿.

## 🐣 L'approche naïve

En effet, l'approche naïve (mais qui fonctionne) est de tout simplement renvoyer l'ensemble de la conversation à chaque appel et de stocker cela de manière volatile dans la mémoire (votre heap pas celle du modèle 🤪).
Pour une démo, ou tout simplement un use case dont la perte de l'historique n'est pas importante c'est tout à fait concevable.

### #️⃣  Bash

Voyons notre approche naïve sans aucune aide.
{|
```bash
#!/usr/bin/env bash
set -a
source "$(dirname "$0")/../.env"
set +a

# OVHcloud AI Endpoints configuration.
ENDPOINT="https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions"
MODEL="gpt-oss-120b"

# The conversation memory: a JSON array of messages, seeded with the system
# message. Every user prompt and every model answer is appended to it.
MESSAGES=$(jq -n '[
  { role: "system", content: "provide a concise answer" }
]')

echo "===== 🧠 CHATBOT WITH MEMORY (type \"exit\" to quit) 🧠 ====="
echo

while true; do
  # Ask the user for a prompt.
  read -rp "⌨️  Your prompt: " USER_PROMPT

  echo

  # Leave the loop on "exit" or on an empty prompt.
  [ -z "$USER_PROMPT" ] && continue
  [ "$USER_PROMPT" = "exit" ] && break

  # 1) Append the user message to the memory.
  # jq safely encodes the user input into valid JSON.
  MESSAGES=$(echo "$MESSAGES" | jq --arg content "$USER_PROMPT" \
    '. + [ { role: "user", content: $content } ]')

  # 2) Build the JSON request body from the WHOLE memory
  BODY=$(jq -n --arg model "$MODEL" --argjson messages "$MESSAGES" '{
    model: $model,
    stream: true,
    messages: $messages
  }')

  # Print the JSON payload sent to the model, pretty-printed with jq
  echo "===== ⬆️ JSON REQUEST (memory sent to the model) ⬆️ ====="
  echo "$BODY" | jq .
  echo

  # 3) Send the request and store the raw response returned by the endpoint.
  RESPONSE=$(curl -s "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${OVH_AI_ENDPOINTS_ACCESS_TOKEN}" \
    -d "$BODY")

  # Extract only the answer from the JSON with jq.
  ANSWER=$(echo "$RESPONSE" | jq -r '.choices[0].message.content')

  echo "===== 🤖 ANSWER 🤖 ====="
  echo "$ANSWER"
  echo

  # 4) Append the model answer to the memory, so the next call gets the full
  # conversation: this is what makes the model look like it remembers.
  MESSAGES=$(echo "$MESSAGES" | jq --arg content "$ANSWER" \
    '. + [ { role: "assistant", content: $content } ]')
done

# Print the final memory: the whole conversation, kept client side.
echo "===== 🧠 FINAL MEMORY (the whole conversation) 🧠 ====="
echo "$MESSAGES" | jq .
```
|}

Par rapport à la première version de mon [article précédent](2026-07-21-java-and-ai-part1) la chose notable est la gestion manuelle de ma mémoire dans la variable `MESSAGES`.
En effet :
 - lignes 12 à 14 on initialise la liste des messages avec le prompt système
 - lignes 31 & 32 on ajoute le prompt utilisatrice ou utilisateur
 - ligne 61 & 62 on ajoute la réponse du modèle (c'est cette étape qui permet d'avoir la mémoire)
Puis on boucle à l'étape de la saisie du prompt utilisatrice / utilisateur.

Ce simple exemple nous permet de bien comprendre en regardant les payloads échangés :
{|
```json
$ ./00.03_ChatbotMemory.sh 
===== 🧠 CHATBOT WITH MEMORY (type "exit" to quit) 🧠 =====

⌨️  Your prompt: Mon nom est stéphane

===== ⬆️ JSON REQUEST (memory sent to the model) ⬆️ =====
{
  "model": "gpt-oss-120b",
  "messages": [
    {
      "role": "system",
      "content": "provide a concise answer"
    },
    {
      "role": "user",
      "content": "Mon nom est stéphane"
    }
  ]
}

===== 🤖 ANSWER 🤖 =====
Enchanté, Stéphane !

⌨️  Your prompt: Quel est mon nom ?

===== ⬆️ JSON REQUEST (memory sent to the model) ⬆️ =====
{
  "model": "gpt-oss-120b",
  "messages": [
    {
      "role": "system",
      "content": "provide a concise answer"
    },
    {
      "role": "user",
      "content": "Mon nom est stéphane"
    },
    {
      "role": "assistant",
      "content": "Enchanté, Stéphane !"
    },
    {
      "role": "user",
      "content": "Quel est mon nom ?"
    }
  ]
}

===== 🤖 ANSWER 🤖 =====
Votre nom est Stéphane.
```
|}

La mémoire est donc simplement un empilement des messages :
 - sur le premier envoi du payload : 
   - lignes 11 & 12 on met le prompt système
   - lignes 15 & 16 le prompt de la personne
 - sur le deuxième envoi du payload :
   - lignes 30 à 37 ce sont les messages du premier payload envoyé
   - lignes 39 & 40 ce sont la réponse du modèle
   - lignes 43 & 44 le nouveau prompt de la personne

Certes cet exemple et sa mise en pratique sont naïfs, mais on comprend de manière visuelle comment fonctionne la mémoire de nos chatbots et pourquoi ça consomme autant de tokens 💸.

Pour voir le source complet de cet exemple c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/00_bash/00.03_StreamingChatbotMemory.sh) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="bash-simple-memory.mov" type="video/quicktime">
</video>

Voyons maintenant cet exemple simple avec un langage un peu plus évolué 😉.

### ✍️ Pure Java

Comme pour mon article précédent, l'approche pure Java va ressembler à ce que l'on fait en Bash.

{|
```java
/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  // OVHcloud AI Endpoints configuration.
  final String endpoint = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
  final String model = "gpt-oss-120b";
  final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

  var mapper = new ObjectMapper();

  // The JSON request body is built once, and its messages array is the
  // conversation memory: it is seeded with the system message, then every user
  // prompt and every model answer is appended to it.
  ObjectNode body = mapper.createObjectNode();
  body.put("model", model);
  body.put("stream", true);
  var messages = body.putArray("messages");
  messages.addObject()
      .put("role", "system")
      .put("content", "provide a concise answer");

  IO.println("===== 🧠 CHATBOT WITH MEMORY (type \"exit\" to quit) 🧠 =====");
  IO.println();

  // The same client is reused for every turn of the conversation. It is only
  // closed when the chat is over: each streamed response is fully read inside
  // the loop, so closing it here never cuts a stream short.
  try (var client = HttpClient.newHttpClient()) {
    while (true) {
      // Ask the user for a prompt.
      var userPrompt = IO.readln("⌨️  Your prompt: ");
      IO.println();

      // Leave the loop on "exit", or on end of input (Ctrl+D).
      if (userPrompt == null || userPrompt.equals("exit")) break;
      if (userPrompt.isBlank()) continue;

      // 1) Append the user message to the memory.
      messages.addObject()
          .put("role", "user")
          .put("content", userPrompt);

      // 2) Print the JSON payload sent to the model, pretty-printed: notice how
      // the messages array grows at each turn. The WHOLE memory is sent again.
      IO.println("===== ⬆️  JSON REQUEST (memory sent to the model) ⬆️  =====");
      IO.println(mapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(body));
      IO.println();

      // 3) Send the request. BodyHandlers.ofInputStream() lets us read the SSE
      // stream ourselves, line by line, as chunks arrive.
      var request = HttpRequest.newBuilder(URI.create(endpoint))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + token)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
          .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      // Print the answer token by token, while rebuilding the full answer:
      // we need it complete to store it in the memory.
      IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
      var answer = new StringBuilder();
      try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          // SSE lines look like: "data: {json}" and end with "data: [DONE]".
          if (!line.startsWith("data: ")) continue;   // skip empty / keep-alive lines
          var data = line.substring("data: ".length());
          if (data.equals("[DONE]")) break;           // end of the stream: stop before reading further

          // Extract the incremental text from this chunk, print it without
          // newline and append it to the answer being rebuilt.
          var content = mapper.readTree(data)
              .at("/choices/0/delta/content");
          if (!content.isMissingNode()) {
            IO.print(content.asText());
            answer.append(content.asText());
          }
        }
      }

      // Final newline once the stream is complete.
      IO.println();
      IO.println();

      // 4) Append the model answer to the memory, so the next call gets the full
      // conversation: this is what makes the model look like it remembers.
      messages.addObject()
          .put("role", "assistant")
          .put("content", answer.toString());
    }
  }

  // Print the final memory: the whole conversation, kept client side.
  IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
  IO.println(mapper.writerWithDefaultPrettyPrinter()
      .writeValueAsString(messages));
}
```
|}

Comme vous le constatez le fonctionnement est très proche de la version bash, mais cette fois écrit en Java, merci [JBang](https://www.jbang.dev/) 😍.
 - lignes 26 à 29 : initialisation de notre mémoire en ajoutant le prompt système (_rôle_ `system`)
 - lignes 48 à 50 : ajout du prompt utilisateur (_rôle_ `user`)
 - lignes 61 à 66 : envoie au modèle et réception de la réponse
 - lignes 72 à 89 : extrait de la réponse
 - lignes 97 à 99 : ajout de la réponse à la mémoire (_rôle_ `assistant`)

Le source complet est disponible [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-simple-memory.mov" type="video/quicktime">
</video>

Il est temps de faire moins de choses manuellement et de s'aider avec quelques outils 🛠️.

### 🛠️ Avec le SDK OpenAI

On a vu comment ajouter, à la main, de la mémoire à notre modèle 💿.
Voyons comment se simplifier la vie, maintenant que nous savons comment cela fonctionne 🔎.

Premier outil à être utilisé, le [SDK officiel d'OpenAI](https://github.com/openai/openai-java).
> ℹ️ Souvenez-vous que j'utilise les outils OpenAI car les modèles OVHcloud sont compatibles avec les routes OpenAI.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.openai:openai-java:4.42.0

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.helpers.ChatCompletionAccumulator;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

void main() {
   // OVHcloud AI Endpoints configuration.
   final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
   final String model = "gpt-oss-120b";
   final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

   // Build the SDK client, pointed at OVHcloud AI Endpoints.
   OpenAIClient client = OpenAIOkHttpClient.builder()
           .baseUrl(baseUrl)
           .apiKey(token)
           //.logLevel(LogLevel.DEBUG)
           .build();

   // The params builder is the conversation memory: it is seeded with the
   // system message, then every user prompt and every model answer is added to
   // it. build() is called at each turn and returns the WHOLE conversation.
   var paramsBuilder = ChatCompletionCreateParams.builder()
           .model(model)
           .addSystemMessage("provide a concise answer");

   IO.println("===== 🧠 CHATBOT WITH MEMORY (type \"exit\" to quit) 🧠 =====");
   IO.println();

   while (true) {
      // Ask the user for a prompt.
      var userPrompt = IO.readln("⌨️  Your prompt: ");
      IO.println();

      // Leave the loop on "exit", or on end of input (Ctrl+D).
      if (userPrompt == null || userPrompt.equals("exit")) break;
      if (userPrompt.isBlank()) continue;

      // 1) Append the user message to the memory.
      paramsBuilder.addUserMessage(userPrompt);

      // 2) Print the messages of the request
      var params = paramsBuilder.build();
      IO.println("===== ⬆️ REQUEST (memory sent to the model) ⬆️ =====");
      params.messages().forEach(IO::println);
      IO.println();

      // 3) Call the endpoint in streaming mode and print the answer token by
      // token. 
      IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
      var accumulator = ChatCompletionAccumulator.create();
      try (StreamResponse<ChatCompletionChunk> stream =
                   client.chat().completions().createStreaming(params)) {
         stream.stream()
                 .peek(accumulator::accumulate)
                 .flatMap(chunk -> chunk.choices().stream())
                 .flatMap(choice -> choice.delta().content().stream())
                 .forEach(IO::print);
      }

      // Newlines once the stream is complete.
      IO.println();
      IO.println();

      // 4) Append the model answer to the memory, so the next call gets the
      // full conversation.
      paramsBuilder.addMessage(accumulator.chatCompletion()
              .choices()
              .getFirst()
              .message());
   }

   // Print the final memory: the whole conversation, kept client side.
   IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
   paramsBuilder.build().messages().forEach(IO::println);
}
```
|}

Le principe est le même : on enrichit le message que l'on envoie au modèle à chaque A/R 📚.
La seule différence est que le SDK nous permet d'éviter la construction manuelle de la structure JSON 😎.
 - lignes 28 à 30 : on prépare ce que l'on va envoyer avec le système prompt, ce sera aussi notre _objet_ qui représentera la mémoire,
 - ligne 45 : on ajoute le prompt utilisatrice ou utilisateur
 - lignes 56 à 64 : on envoie la requête, affiche la réponse et stocke dans un _accumulator_ la réponse pour pouvoir l'ajouter par la suite à la mémoire
 - lignes 72 à 76 : on ajoute la réponse stockée dans l'accumulator dans notre mémoire pour qu'elle soit envoyée lors de la prochaine requête.

Le source complet est disponible [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-sdk-simple-memory.mov" type="video/quicktime">
</video>

Je pense que l'on peut encore faire plus simple 😎.

### 🦜 LangChain4j

Eh oui passons à mon Framework préféré : [LangChain4j](https://docs.langchain4j.dev/intro/) 🦜.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j:1.18.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.18.0
//DEPS  org.slf4j:slf4j-simple:2.0.17
//

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.CompletableFuture;


interface Assistant {
    @SystemMessage("provide a concise answer")
    TokenStream chat(String userMessage);
}

void main() {
    // OVHcloud AI Endpoints configuration.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Build the underlying LangChain4j streaming model, pointed at OVHcloud AI Endpoints.
    StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName(model)
            .logRequests(false)
            //.logResponses(true)
            .build();

    // The conversation memory: a sliding window of the 10 last messages.
    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

    // Create the AI Service backed by the streaming model, with memory.
    Assistant assistant = AiServices.builder(Assistant.class)
            .streamingChatModel(chatModel)
            .chatMemory(chatMemory)
            .build();

    IO.println("===== 🧠 CHATBOT WITH MEMORY (type \"exit\" to quit) 🧠 =====");
    IO.println();

    while (true) {
        // Ask the user for a prompt.
        var userPrompt = IO.readln("⌨️  Your prompt: ");
        IO.println();

        // Leave the loop on "exit", or on end of input (Ctrl+D).
        if (userPrompt == null || userPrompt.equals("exit")) break;
        if (userPrompt.isBlank()) continue;

        // 1) Print the memory as it is BEFORE the call.
        IO.println("===== 🧠 MEMORY (resent to the model with the prompt) 🧠 =====");
        chatMemory.messages().forEach(IO::println);
        IO.println();

        // 2) Call the endpoint in streaming.
        IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
        var futureResponse = new CompletableFuture<ChatResponse>();
        assistant.chat(userPrompt)
                .onPartialResponse(IO::print)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();
        futureResponse.join();

        // Newlines once the stream is complete.
        IO.println();
        IO.println();
    }

    // Print the final memory: the whole conversation (up to the window size),
    // this time including the system message added by LangChain4j itself.
    IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
    chatMemory.messages().forEach(IO::println);
}
```

Notre code commence à être beaucoup plus simple non 😎 ?
- ligne 41 : on initialise notre mémoire avec une taille de 10. Notez que nous n'avions pas capé la taille dans les exemples précédents. Tout simplement car il aurait fallu le gérer à la main, là c'est offert par LangChain4j donc autant ne pas s'en priver 🤗
- Ligne 46 : on ajoute la fonctionnalité de mémoire à notre modèle
- lignes 69 à 73 : on affiche la réponse du modèle

> ℹ️ les portions de code du style `chatMemory.messages().forEach(IO::println);` ne sont là qu'à des fins de debug / explications

Le source complet est disponible [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j_03_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="langchain4j-simple-memory.mov" type="video/quicktime">
</video>

Peut-être qu'en lisant le code une chose vous chiffonne 🤔.
Que se passe-t-il si j'ai plusieurs utilisatrices / utilisateurs ?  
Iels partagent la même mémoire 🔀 ?  
Avec le code existant oui 🥺.
Heureusement LangChain4j nous permet facilement d'avoir une séparation des mémoires par personne.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j:1.18.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.18.0
//DEPS  org.slf4j:slf4j-simple:2.0.17

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.util.List;
import java.util.concurrent.CompletableFuture;

interface Assistant {
    @SystemMessage("provide a concise answer")
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

void main() {
    // OVHcloud AI Endpoints configuration.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Build the underlying LangChain4j streaming model, pointed at OVHcloud AI Endpoints.
    StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName(model)
            .logRequests(false)
            //.logResponses(true)
            .build();

    // Where the conversations are kept. 
    ChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

    // Create the AI Service backed by the streaming model, with memory.
    // Because of @MemoryId there is not ONE memory but one per conversation, so
    // we register a chatMemoryProvider instead of a single chatMemory.
    Assistant assistant = AiServices.builder(Assistant.class)
            .streamingChatModel(chatModel)
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(10)
                    .chatMemoryStore(memoryStore)
                    .build())
            .build();

    // The two conversations, told apart by their memory id only.
    final String stephane = "stephane";
    final String fanny = "fanny";

    // The scripted conversation: who is talking, and what they say. Both
    // introduce themselves, then both ask the same question.
    ask(assistant, stephane, "My name is Stéphane");
    ask(assistant, fanny, "My name is Fanny");
    ask(assistant, stephane, "What is my name?");
    ask(assistant, fanny, "What is my name?");

    // Print both memories: same assistant, same model, but two conversations
    // that never saw each other. Each one only knows what was said to it.
    for (var sessionId : List.of(stephane, fanny)) {
        IO.println("===== 🧠 MEMORY of \"" + sessionId + "\" 🧠 =====");
        memoryStore.getMessages(sessionId).forEach(IO::println);
        IO.println();
    }

    // A real application would also delete a conversation when it ends,
    // otherwise the store keeps one entry per memory id forever:
    //memoryStore.deleteMessages(stephane);
}

// One turn of conversation: send the prompt on behalf of a session and print the
// answer token by token. 
void ask(Assistant assistant, String sessionId, String userPrompt) {
    IO.println("===== 💬 \"" + sessionId + "\" says: " + userPrompt + " 💬 =====");

    var futureResponse = new CompletableFuture<ChatResponse>();
    assistant.chat(sessionId, userPrompt)
            .onPartialResponse(IO::print)
            .onCompleteResponse(futureResponse::complete)
            .onError(futureResponse::completeExceptionally)
            .start();
    futureResponse.join();

    // Newlines once the stream is complete.
    IO.println();
    IO.println();
}
```
|}

On voit ici les quelques différences avec une mémoire sans `id`.
 - ligne 24 : on ajoute l'annotation `@MemoryId` afin de pouvoir identifier une mémoire de manière unique
 - ligne 43 : on déclare le _memory store_ en mémoire (cette étape est facultative car par défaut mais nous permet ensuite d'afficher le contenu de la mémoire)
 - lignes 50 à 54 : on déclare un `chatMemoryProvider` pour pouvoir avoir la gestion par identifiant de chaque mémoire
 - lignes 63 à 66 : ce sont des exemples de comment utiliser notre chatbot maintenant avec l'id de mémoire
 - ligne 78 : avec l'accès au _memory store_ on peut aussi gérer le nettoyage de celui-ci 💡

> - 💡 bien entendu on aurait pu avoir le même comportement en `Bash` ou en version Java pure / SDK mais cela aurait nécessité plus de code 👩‍💻
> - 🗑️ il y a pas mal de code afin de debug ou d'explication, si vous le retirez vous arrivez à quelque chose de plus concis

Le source complet est disponible [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_04_StreamingChatbotMultiSessionMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="langchain4j-simple-memory-id.mov" type="video/quicktime">
</video>


### ⚡️ Quarkus et LangChain4j

Avançons encore un peu dans la simplification avec l'ajout de [Quarkus](https://quarkus.io/).
Pour notre exemple c'est un peu overkill mais cela nous permet de voir comment l'ajouter dans une application Quarkus existante.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
// jboss-threads needs java.lang opened on Java 24+ (thread-local reset capability).
//JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED
//DEPS io.quarkus.platform:quarkus-bom:3.33.2@pom
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai:1.12.0
//FILES application.properties

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

// 1) The AI Service. 
@RegisterAiService
interface Assistant {
    @SystemMessage("provide a concise answer")
    Multi<String> chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

// 2) Command-mode entry point: @QuarkusMain + QuarkusApplication run in a shell.
@QuarkusMain
public class _04_03_StreamingChatbotMemory implements QuarkusApplication {

    // Memory Id.
    private static final String SESSION_ID = "cli-session";

    // 3) The generated AI service is a CDI bean, injected here.
    @Inject
    Assistant assistant;

    // Default chat memory store.
    @Inject
    ChatMemoryStore memoryStore;

    // @ActivateRequestContext makes the CDI request scope available for the call.
    @Override
    @ActivateRequestContext
    public int run(String... args) {
        IO.println("===== 🧠 CHATBOT WITH MEMORY (type \"exit\" to quit) 🧠 =====");
        IO.println();

        while (true) {
            // Ask the user for a prompt.
            var userPrompt = IO.readln("⌨️  Your prompt: ");
            IO.println();

            // Leave the loop on "exit", or on end of input (Ctrl+D).
            if (userPrompt == null || userPrompt.equals("exit")) break;
            if (userPrompt.isBlank()) continue;

            // 4) Print the memory as it is BEFORE the call.
            IO.println("===== 🧠 MEMORY (resent to the model with the prompt) 🧠 =====");
            memoryStore.getMessages(SESSION_ID).forEach(IO::println);
            IO.println();

            // 5) Call the endpoint in streaming mode and print the answer token
            // by token.
            IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
            assistant.chat(SESSION_ID, userPrompt)
                    .subscribe().asStream()
                    .forEach(IO::print);

            // Newlines once the stream is complete.
            IO.println();
            IO.println();
        }

        // Print the final memory.
        IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
        memoryStore.getMessages(SESSION_ID).forEach(IO::println);

        // Clean memory if needed
        //memoryStore.deleteMessages(SESSION_ID);

        return 0;
    }
}
```
|}

Encore une fois le code pourrait être plus concis mais j'ai rajouté un peu de debug.   
> - ℹ️ Il n'y a rien à rajouter de particulier pour gérer la mémoire, l'extension Quarkus active la mémoire par défaut 
> - ⚠️ il faut donc penser à la désactiver si vous ne la souhaitez pas en positionnant `chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class` dans `@RegisterAiService`

- lignes 21 à 25 : on déclare le _AIService_ (avec la mémoire par défaut) et l'id de mémoire avec `@MemoryId`
- lignes 39 & 40 : on injecte le _memory store_ uniquement pour avoir du debug, il est créé par défaut sinon
- ligne 66 : on appelle le chatbot avec l'id de mémoire pour éviter les conflits

> ℹ️ Le code utilise l'identifiant de mémoire mais au final l'exemple est mono-utilisatrice / utilisateur. Cela permet juste d'avoir un code plus robuste au cas où.

Le source complet est disponible [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="quarkus-simple-memory-id.mov" type="video/quicktime">
</video>

### ☘️ Avec Spring AI

Allons jusqu'au bout de nos stacks avec [Spring AI](https://spring.io/projects/spring-ai).

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.0@pom
//DEPS org.springframework.ai:spring-ai-bom:2.0.0@pom
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//DEPS org.aspectj:aspectjweaver:1.9.25.1
//DEPS org.springframework.ai:spring-ai-starter-model-openai:2.0.0
//DEPS jakarta.servlet:jakarta.servlet-api:6.1.0

// 1) Include application.properties as a classpath resource so Spring reads it.
//FILES application.properties

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

// NOT @SpringBootApplication: this single file lives in the default package, and
// its @ComponentScan would scan the whole classpath (breaking Spring's internal
// config). We only need auto-config + the local @Bean, so we drop the scan.
@SpringBootConfiguration
@EnableAutoConfiguration
public class _05_03_StreamingChatbotMemory {

    // A single conversation here, so a constant id is enough. A web chatbot
    // would use one id per user or per session instead.
    private static final String CONVERSATION_ID = "cli-session";

    public static void main(String[] args) {
        SpringApplication.run(_05_03_StreamingChatbotMemory.class, args);
    }

    // CommandLineRunner is the Spring Boot equivalent of a command-mode entry
    // point: it runs after the context starts, then the app exits (no web server).
    @Bean
    CommandLineRunner run(ChatClient.Builder builder) {
        return args -> {
            // 2) The conversation memory: a sliding window of the 10 last messages,
            // kept in the default in-memory repository. Declaring the repository
            // explicitly changes nothing for the client (it is the default), we do
            // it only to show where the conversations actually live.
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(10)
                    .build();

            // 3) Build the ChatClient with the memory advisor registered by default,
            // so every call made with this client goes through it.
            var chatClient = builder
                    .defaultSystem("provide a concise answer")
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .build();

            IO.println("===== 🧠 CHATBOT WITH MEMORY (type \"exit\" to quit) 🧠 =====");
            IO.println();

            while (true) {
                // Ask the user for a prompt.
                var userPrompt = IO.readln("⌨️  Your prompt: ");
                IO.println();

                // Leave the loop on "exit", or on end of input (Ctrl+D).
                if (userPrompt == null || userPrompt.equals("exit")) break;
                if (userPrompt.isBlank()) continue;

                // 4) Print the memory as it is BEFORE the call.
                IO.println("===== 🧠 MEMORY (resent to the model with the prompt) 🧠 =====");
                chatMemory.get(CONVERSATION_ID).forEach(IO::println);
                IO.println();

                // 5) Call the endpoint in streaming mode and print the answer token
                // by token. 
                IO.println("===== 🤖 ANSWER (streaming) 🤖 =====");
                chatClient.prompt()
                        .user(userPrompt)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
                        .stream()
                        .content()
                        .doOnNext(IO::print)
                        .blockLast();

                // Newlines once the stream is complete.
                IO.println();
                IO.println();
            }

            // Print the final memory: the whole conversation (up to the window size),
            // as stored by the advisor.
            IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
            chatMemory.get(CONVERSATION_ID).forEach(IO::println);

            // A real application would also delete the conversation when it ends,
            // otherwise the repository keeps one entry per conversation id forever:
            //chatMemory.clear(CONVERSATION_ID);
        };
    }
}
```

On voit, au final, que le code entre les différents Frameworks se ressemble beaucoup avec l'approche simple d'une gestion volatile en mémoire.
 - lignes 48 à 51 : création de la mémoire avec une taille de `10`
 - ligne 57 : activation de la mémoire pour notre chatbot
 - ligne 82 : gestion de la multi-conversation avec un `id`

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="spring-simple-memory-id.mov" type="video/quicktime">
</video>

# 🏁 Conclusion

Si vous êtes arrivés là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
