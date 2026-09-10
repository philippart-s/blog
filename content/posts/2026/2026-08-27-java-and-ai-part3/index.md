---
title: "☕️ Java à l'ère de l'IA 🤖 - partie 3 : le stockage de la mémoire"
description: ""
link: /2026-08-27-java-and-ai-part3
image: cover.webp
tags:
  - Java
  - IA
  - Code
author: wildagsx
---

🏴󠁧󠁢󠁥󠁮󠁧󠁿 You can find the English version of this article [here]({site.url}2026-08-03-java-and-ai-part2-en) 🏴󠁧󠁢󠁥󠁮󠁧󠁿.

## TL;DR
> 

<br/>

# 📜 Introduction

Petit rappel des épisodes précédents : cet article fait partie d'une série d'articles visant à expliquer comment intégrer de l'IA dans les applications que nous développons en Java ☕️.
Il est la suite de l'[article précédent](/blog/2026-08-03-java-and-ai-part2) qui avait pour but d'expliquer comment fonctionne la mémoire dans un chatbot.

Je ne reviendrai donc pas sur les éléments à connaître pour débuter votre voyage dans l'IA.
L'objectif de cet article sera d'aller plus loin dans la partie mémoire : quelles architectures prendre pour persister cette mémoire 🗃️.

# 📄 Utiliser un fichier JSON

C'est l'approche simple et naïve mais qui permet de survivre à un redémarrage du processus.
Le format, `JSON`, permet d'éviter des conversions puisque, au final, cela va être envoyé dans ce format au endpoint.

### #️⃣  Bash

Pour l'instant, ce n'est pas encore trop coûteux de le faire en Bash.

{|
```bash
#!/usr/bin/env bash

# Load the access token from the .env file located at the project root.
# The .env file is expected to contain: OVH_AI_ENDPOINTS_ACCESS_TOKEN=...
set -a
source "$(dirname "$0")/../.env"
set +a

# OVHcloud AI Endpoints configuration.
ENDPOINT="https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions"
MODEL="gpt-oss-120b"

# Where the conversation is stored, next to this script. 
SESSION_ID="cli-session"
MEMORY_DIR="$(dirname "$0")/.memory"
MEMORY_FILE="$MEMORY_DIR/$SESSION_ID.json"

mkdir -p "$MEMORY_DIR"

# 1) Load the memory: the whole conversation is the file.
if [ -s "$MEMORY_FILE" ]; then
  MESSAGES=$(cat "$MEMORY_FILE")
  echo "===== 🧠 MEMORY RESTORED FROM DISK ($(echo "$MESSAGES" | jq 'length') messages) 🧠 ====="
  echo "$MESSAGES" | jq .
  echo
else
  MESSAGES=$(jq -n '[
    { role: "system", content: "provide a concise answer" }
  ]')
  echo "===== 🧠 NO MEMORY YET, STARTING A NEW CONVERSATION 🧠 ====="
  echo
fi

echo "===== 🧠 CHATBOT WITH PERSISTENT MEMORY (type \"exit\" to quit) 🧠 ====="
echo "💾 stored in $MEMORY_FILE"
echo

while true; do
  # Ask the user for a prompt.
  read -rp "⌨️  Your prompt: " USER_PROMPT

  echo

  # Leave the loop on "exit" or on an empty prompt.
  [ -z "$USER_PROMPT" ] && continue
  [ "$USER_PROMPT" = "exit" ] && break

  # 2) Append the user message to the memory.
  # jq safely encodes the user input into valid JSON.
  MESSAGES=$(echo "$MESSAGES" | jq --arg content "$USER_PROMPT" \
    '. + [ { role: "user", content: $content } ]')

  # 3) Build the JSON request body from the WHOLE memory
  BODY=$(jq -n --arg model "$MODEL" --argjson messages "$MESSAGES" '{
    model: $model,
    stream: true,
    messages: $messages
  }')

  # Print the JSON payload sent to the model, pretty-printed with jq.
  echo "===== ⬆️ JSON REQUEST (memory sent to the model) ⬆️ ====="
  echo "$BODY" | jq .
  echo

  # 4) Send the request and stream the answer token by token.
  echo "===== 🤖 ANSWER (streaming) 🤖 ====="
  ANSWER=""
  while IFS= read -r line; do
    # SSE lines look like: "data: {json}" and end with "data: [DONE]".
    line="${line#data: }"                 # strip the "data: " prefix
    [ -z "$line" ] && continue            # skip empty keep-alive lines
    [ "$line" = "[DONE]" ] && break       # end of the stream

    # Extract the incremental text from this chunk, print it without newline
    # and append it to the answer being rebuilt.
    CHUNK=$(echo "$line" | jq -rj '.choices[0].delta.content // empty')
    printf '%s' "$CHUNK"
    ANSWER="${ANSWER}${CHUNK}"
  done < <(curl -sN "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${OVH_AI_ENDPOINTS_ACCESS_TOKEN}" \
    -d "$BODY")

  # Final newline once the stream is complete.
  echo
  echo

  # 5) Append the model answer to the memory.
  MESSAGES=$(echo "$MESSAGES" | jq --arg content "$ANSWER" \
    '. + [ { role: "assistant", content: $content } ]')

  # 6) Save the memory.
  echo "$MESSAGES" > "$MEMORY_FILE"
  echo "💾 memory saved to $MEMORY_FILE ($(echo "$MESSAGES" | jq 'length') messages)"
  echo
done

# Print the final memory: the whole conversation, now kept on disk. Run the
# script again and this is exactly what it will start from.
echo "===== 🧠 FINAL MEMORY (kept in $MEMORY_FILE) 🧠 ====="
echo "$MESSAGES" | jq .
echo
echo "🗑️  Delete $MEMORY_FILE to start a fresh conversation."
```
|}

 - lignes 14 à 16 : le fichier représentant la mémoire sera donc stocké dans un répertoire `.memory`
 - lignes 21 à 25 : on initialise la mémoire avec le fichier (s'il est présent)
 - ligne 93 : sauvegarde de la mémoire dans le fichier

Comme toujours cette approche est naïve mais permet de bien comprendre comment cela fonctionne et pourquoi c'est plus avantageux d'utiliser le format `JSON` 😎.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/00_bash/00.04_StreamingChatbotFileMemory.sh) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="bash-file-memory.mov" type="video/quicktime">
</video>

### ✍️ Pure Java

Passons à la version Java sans framework particulier.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  // OVHcloud AI Endpoints configuration.
  final String endpoint = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
  final String model = "gpt-oss-120b";
  final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

  // Where the conversation is stored. 
  final String sessionId = "cli-session";
  final var memoryFile = Path.of(".memory", sessionId + ".json");

  Files.createDirectories(memoryFile.getParent());

  var mapper = new ObjectMapper();

  // The JSON request body is built once, and its messages array is the
  // conversation memory.
  ObjectNode body = mapper.createObjectNode();
  body.put("model", model);
  body.put("stream", true);

  // 1) Load the memory: the whole conversation is the file.
  ArrayNode messages;
  if (Files.exists(memoryFile) && Files.size(memoryFile) > 0) {
    messages = (ArrayNode) mapper.readTree(Files.readString(memoryFile));
    body.set("messages", messages);
    IO.println("===== 🧠 MEMORY RESTORED FROM DISK (" + messages.size() + " messages) 🧠 =====");
    IO.println(mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(messages));
    IO.println();
  } else {
    messages = body.putArray("messages");
    messages.addObject()
        .put("role", "system")
        .put("content", "provide a concise answer");
    IO.println("===== 🧠 NO MEMORY YET, STARTING A NEW CONVERSATION 🧠 =====");
    IO.println();
  }

  IO.println("===== 🧠 CHATBOT WITH PERSISTENT MEMORY (type \"exit\" to quit) 🧠 =====");
  IO.println("💾 stored in " + memoryFile.toAbsolutePath());
  IO.println();

  // The same client is reused for every turn of the conversation. 
  try (var client = HttpClient.newHttpClient()) {
    while (true) {
      // Ask the user for a prompt.
      var userPrompt = IO.readln("⌨️  Your prompt: ");
      IO.println();

      // Leave the loop on "exit", or on end of input (Ctrl+D).
      if (userPrompt == null || userPrompt.equals("exit")) break;
      if (userPrompt.isBlank()) continue;

      // 2) Append the user message to the memory.
      messages.addObject()
          .put("role", "user")
          .put("content", userPrompt);

      // 3) Print the JSON payload sent to the model.
      IO.println("===== ⬆️  JSON REQUEST (memory sent to the model) ⬆️  =====");
      IO.println(mapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(body));
      IO.println();

      // 4) Send the request. 
      var request = HttpRequest.newBuilder(URI.create(endpoint))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + token)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
          .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      // Print the answer token by token.
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

      // 5) Append the model answer to the memory.
      messages.addObject()
          .put("role", "assistant")
          .put("content", answer.toString());

      // 6) Save the memory. 
      Files.writeString(memoryFile, mapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(messages));
      IO.println("💾 memory saved to " + memoryFile + " (" + messages.size() + " messages)");
      IO.println();
    }
  }

  // Print the final memory.
  IO.println("===== 🧠 FINAL MEMORY (kept in " + memoryFile + ") 🧠 =====");
  IO.println(mapper.writerWithDefaultPrettyPrinter()
      .writeValueAsString(messages));
  IO.println();
  IO.println("🗑️  Delete " + memoryFile + " to start a fresh conversation.");
}
```
|}

Comme pour les exemples précédents des autres articles l'approche Java pure ressemble beaucoup à celle en Bash. 

 - lignes 20 à 23 : création du fichier `JSON` pour stocker la mémoire
 - lignes 36 & 37 : chargement de l'historique de la conversation
 - lignes 112 à 120 : sauvegarde des messages échangés

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-file-memory.mov" type="video/quicktime">
</video>

### 🛠️ Avec le SDK OpenAI

Passons au [SDK officiel d'OpenAI](https://github.com/openai/openai-java) pour voir si on ne peut pas un peu se simplifier la vie 😉.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.openai:openai-java:4.52.0

import com.fasterxml.jackson.core.type.TypeReference;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.ObjectMappers;
import com.openai.core.http.StreamResponse;
import com.openai.helpers.ChatCompletionAccumulator;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;

void main() throws Exception {
    // OVHcloud AI Endpoints configuration.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Where the conversation is stored. 
    final String sessionId = "cli-session";
    final var memoryFile = Path.of(".memory", sessionId + ".json");

    Files.createDirectories(memoryFile.getParent());

    // The SDK's own Jackson mapper, from com.openai.core. 
    var mapper = ObjectMappers.jsonMapper();

    // Build the SDK client, pointed at OVHcloud AI Endpoints.
    OpenAIClient client = OpenAIOkHttpClient.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            //.logLevel(LogLevel.DEBUG)
            .build();

    // Conversation.
    var paramsBuilder = ChatCompletionCreateParams.builder()
            .model(model);

    // 1) Load the memory. 
    if (Files.exists(memoryFile) && Files.size(memoryFile) > 0) {
        List<ChatCompletionMessageParam> restored = mapper.readValue(
                Files.readString(memoryFile),
                new TypeReference<List<ChatCompletionMessageParam>>() {});
        paramsBuilder.messages(restored);
        IO.println("===== 🧠 MEMORY RESTORED FROM DISK (" + restored.size() + " messages) 🧠 =====");
        restored.forEach(IO::println);
        IO.println();
    } else {
        // First run: start a new conversation seeded with the system message.
        paramsBuilder.addSystemMessage("provide a concise answer");
        IO.println("===== 🧠 NO MEMORY YET, STARTING A NEW CONVERSATION 🧠 =====");
        IO.println();
    }

    IO.println("===== 🧠 CHATBOT WITH PERSISTENT MEMORY (type \"exit\" to quit) 🧠 =====");
    IO.println("💾 stored in " + memoryFile.toAbsolutePath());
    IO.println();

    while (true) {
        // Ask the user for a prompt.
        var userPrompt = IO.readln("⌨️  Your prompt: ");
        IO.println();

        // Leave the loop on "exit", or on end of input (Ctrl+D).
        if (userPrompt == null || userPrompt.equals("exit")) break;
        if (userPrompt.isBlank()) continue;

        // 2) Append the user message to the memory.
        paramsBuilder.addUserMessage(userPrompt);

        // 3) Print the messages of the request.
        var params = paramsBuilder.build();
        IO.println("===== ⬆️ REQUEST (memory sent to the model) ⬆️ =====");
        params.messages().forEach(IO::println);
        IO.println();

        // 4) Call the endpoint in streaming mode and print the answer token by
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

        // 5) Append the model answer to the memory.
        paramsBuilder.addMessage(accumulator.chatCompletion()
                .choices()
                .getFirst()
                .message());

        // 6) Save the memory.
        var updated = paramsBuilder.build().messages();
        Files.writeString(memoryFile, mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(updated));
        IO.println("💾 memory saved to " + memoryFile + " (" + updated.size() + " messages)");
        IO.println();
    }

    // Print the final memory: the whole conversation, now kept on disk. 
    IO.println("===== 🧠 FINAL MEMORY (kept in " + memoryFile + ") 🧠 =====");
    paramsBuilder.build().messages().forEach(IO::println);
    IO.println();
    IO.println("🗑️  Delete " + memoryFile + " to start a fresh conversation.");
}
```

Bon, je ne suis pas sûr que l'on y gagne en simplicité.
Principalement du fait que le SDK ne prévoit rien pour la peristence et que l'on la gère cela à la main pour ensuite réenvoyer tout l'historique 😨.

 - lignes 23 à 25 : on init la mémoire fichier
 - ligne 38 : le `params bulder` reste la structure de stockage de la mémoire
 - lignes 42 à 55 : chargement de la mémoire (si elle existe), grâce à `Jackson` la sérialisation / désérialisation est dans le bon format
 - lignes 61 à 94 : toujours la même boucle pour interragir avec le modèle
 - lignes 97 à 107 : ajout des messages dans la mémoire et sauvegarde dans le fichier

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_04_StreamingChatbotFileMemory) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-sdk-file-memory.mov" type="video/quicktime">
</video>

### 🦜 LangChain4j

Voyons comment [LangChain4j](https://docs.langchain4j.dev/intro/) nous permet de gérer une mémoire persister dans un fichier.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j:1.18.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.18.0
//DEPS org.slf4j:slf4j-simple:2.0.17

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.concurrent.CompletableFuture;


interface Assistant {
    @SystemMessage("provide a concise answer")
    TokenStream chat(String userMessage);
}

// Helper to save messages in a file.
class FileChatMemoryStore implements ChatMemoryStore {

    private final Path directory;

    FileChatMemoryStore(Path directory) {
        this.directory = directory;
    }

    private Path fileFor(Object memoryId) {
        return directory.resolve(memoryId + ".json");
    }

    // Called before every request.
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        var file = fileFor(memoryId);
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                return List.of();
            }
            return ChatMessageDeserializer.messagesFromJson(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the memory of " + memoryId, e);
        }
    }

    // Called once the answer is complete.
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            Files.createDirectories(directory);
            Files.writeString(fileFor(memoryId), ChatMessageSerializer.messagesToJson(messages));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the memory of " + memoryId, e);
        }
    }

    // Called when a conversation is dropped. 
    @Override
    public void deleteMessages(Object memoryId) {
        try {
            Files.deleteIfExists(fileFor(memoryId));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete the memory of " + memoryId, e);
        }
    }
}

void main() {
    // OVHcloud AI Endpoints configuration.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    // Where the conversations are stored. 
    final String sessionId = "cli-session";
    final var memoryDir = Path.of(".memory");

    // Build the underlying LangChain4j streaming model, pointed at OVHcloud AI Endpoints.
    StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName(model)
            .logRequests(false)
            //.logResponses(true)
            .build();

    // 1) The memory. 
    ChatMemoryStore memoryStore = new FileChatMemoryStore(memoryDir);
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .id(sessionId)
            .maxMessages(10)
            .chatMemoryStore(memoryStore)
            .build();

    // Create the AI Service backed by the streaming model, with memory.
    Assistant assistant = AiServices.builder(Assistant.class)
            .streamingChatModel(chatModel)
            .chatMemory(chatMemory)
            .build();

    // 2) Only for explanation purpose, not mandatory.
    var restored = chatMemory.messages();
    if (restored.isEmpty()) {
        IO.println("===== 🧠 NO MEMORY YET, STARTING A NEW CONVERSATION 🧠 =====");
    } else {
        IO.println("===== 🧠 MEMORY RESTORED FROM DISK (" + restored.size() + " messages) 🧠 =====");
        restored.forEach(IO::println);
    }
    IO.println();

    IO.println("===== 🧠 CHATBOT WITH PERSISTENT MEMORY (type \"exit\" to quit) 🧠 =====");
    IO.println("💾 stored in " + memoryDir.resolve(sessionId + ".json").toAbsolutePath());
    IO.println();

    while (true) {
        // Ask the user for a prompt.
        var userPrompt = IO.readln("⌨️  Your prompt: ");
        IO.println();

        // Leave the loop on "exit", or on end of input (Ctrl+D).
        if (userPrompt == null || userPrompt.equals("exit")) break;
        if (userPrompt.isBlank()) continue;

        // 3) Print the memory as it is BEFORE the call.
        IO.println("===== 🧠 MEMORY (resent to the model with the prompt) 🧠 =====");
        chatMemory.messages().forEach(IO::println);
        IO.println();

        // 4) Call the endpoint in streaming mode and print the answer token by
        // token. 
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

    // Print the final memory: the whole conversation .
    IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
    chatMemory.messages().forEach(IO::println);
    IO.println();

    // A real application would delete a conversation when it ends. It matters
    // more now than it did in _03_04: the entries are files, and they outlive
    // the process.
    //memoryStore.deleteMessages(sessionId);
    IO.println("🗑️  Delete " + memoryDir.resolve(sessionId + ".json")
            + " to start a fresh conversation.");
}
```

Cette fois, on retrouve le confort de LangChain4j : la persistance n'est plus dans la boucle de conversation mais dans une implémentation de `ChatMemoryStore` que le framework appelle pour nous 😎.
On n'économise pas forcément beaucoup de lignes car on a l'implémentation de notre `FileChatMemoryStore` (qui dans la vraie vie serait dans une autre classe) et du debug assez présent.

 - lignes 29 à 75 : notre `FileChatMemoryStore`, un fichier `JSON` par identifiant de mémoire (⚠️ pour simplifier le code je n'utilise pas l'identifant par la suite ⚠️)
   - lignes 42 à 53 : chargement de la mémoire depuis le fichier, appelé par LangChain4j avant chaque requête
   - lignes 56 à 64 : sauvegarde de la mémoire dans le fichier, appelé par LangChain4j une fois la réponse complète
   - lignes 67 à 74 : suppression du fichier quand la conversation se termine
   - lignes 49 & 60 : la sérialisation / désérialisation des messages est fournie par LangChain4j via `ChatMessageSerializer` et `ChatMessageDeserializer`
 - lignes 97 à 102 : on branche notre _memory store_ sur la mémoire, c'est la seule différence avec la version en mémoire volatile
 - ligne 107 : on ajoute la mémoire à notre chatbot, comme précédemment
 - ligne 162 : avec l'accès au _memory store_ on peut supprimer le fichier 💡

> ℹ️ les lignes 110 à 118 ne servent qu'à afficher la mémoire restaurée, elles ne sont pas nécessaires au fonctionnement.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_05_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="langchain4j-file-memory.mov" type="video/quicktime">
</video>

### ⚡️ Quarkus et LangChain4j

Voyons maintenant comment Quarkus s'en sort pour la gestion de la mémoire dans un fichier.

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
// jboss-threads needs java.lang opened on Java 24+ (thread-local reset capability).
//JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED
//DEPS io.quarkus.platform:quarkus-bom:3.33.2@pom
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai:1.12.0

// 1) Include application.properties as a classpath resource so Quarkus reads it.
//FILES application.properties

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// 2) The AI Service. 
@RegisterAiService
interface Assistant {
    @SystemMessage("provide a concise answer")
    Multi<String> chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

// 3) The persistent store. 
@ApplicationScoped
class FileChatMemoryStore implements ChatMemoryStore {

    private final Path directory = Path.of(".memory");

    private Path fileFor(Object memoryId) {
        return directory.resolve(memoryId + ".json");
    }

    // Called before every request, to build the context sent to the model.
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        var file = fileFor(memoryId);
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                return List.of();
            }
            return ChatMessageDeserializer.messagesFromJson(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the memory of " + memoryId, e);
        }
    }

    // Called once the answer is complete, with the memory already updated and
    // already trimmed to the configured window.
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            Files.createDirectories(directory);
            Files.writeString(fileFor(memoryId), ChatMessageSerializer.messagesToJson(messages));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the memory of " + memoryId, e);
        }
    }

    // Delete the all messages from the memory
    @Override
    public void deleteMessages(Object memoryId) {
        try {
            Files.deleteIfExists(fileFor(memoryId));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete the memory of " + memoryId, e);
        }
    }
}

// 4) Needed to avoid to delete memory after each restart
final class PersistentChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    PersistentChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object id() {
        return delegate.id();
    }

    @Override
    public void add(ChatMessage message) {
        delegate.add(message);
    }

    @Override
    public List<ChatMessage> messages() {
        return delegate.messages();
    }

    // Deliberately empty. Dropping a conversation stays possible, but only
    // explicitly, through ChatMemoryStore.deleteMessages().
    @Override
    public void clear() {
    }
}

// 5) The provider that hands out those memories. 
@Singleton
class PersistentChatMemoryProvider implements ChatMemoryProvider {

    @Inject
    ChatMemoryStore store;

    @ConfigProperty(name = "quarkus.langchain4j.chat-memory.memory-window.max-messages",
            defaultValue = "10")
    int maxMessages;

    @Override
    public ChatMemory get(Object memoryId) {
        return new PersistentChatMemory(MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(store)
                .build());
    }
}

// 6) Command-mode entry point: @QuarkusMain + QuarkusApplication run in a shell.
@QuarkusMain
public class _04_04_StreamingChatbotFileMemory implements QuarkusApplication {

    // A single conversation here, so a constant id is enough. 
    private static final String SESSION_ID = "cli-session";

    // 7) The generated AI service is a CDI bean, injected here.
    @Inject
    Assistant assistant;

    // The store, injected only so we can print the conversation. only for debug purppose.
    @Inject
    ChatMemoryStore memoryStore;

    @Override
    @ActivateRequestContext
    public int run(String... args) {
        // 8) Nothing to restore: reading the store already reads the file.
        var restored = memoryStore.getMessages(SESSION_ID);
        if (restored.isEmpty()) {
            IO.println("===== 🧠 NO MEMORY YET, STARTING A NEW CONVERSATION 🧠 =====");
        } else {
            IO.println("===== 🧠 MEMORY RESTORED FROM DISK (" + restored.size() + " messages) 🧠 =====");
            restored.forEach(IO::println);
        }
        IO.println();

        IO.println("===== 🧠 CHATBOT WITH PERSISTENT MEMORY (type \"exit\" to quit) 🧠 =====");
        IO.println("💾 stored in " + Path.of(".memory", SESSION_ID + ".json").toAbsolutePath());
        IO.println();

        while (true) {
            // Ask the user for a prompt.
            var userPrompt = IO.readln("⌨️  Your prompt: ");
            IO.println();

            // Leave the loop on "exit", or on end of input (Ctrl+D).
            if (userPrompt == null || userPrompt.equals("exit")) break;
            if (userPrompt.isBlank()) continue;

            // 9) Print the memory as it is BEFORE the call.
            IO.println("===== 🧠 MEMORY (resent to the model with the prompt) 🧠 =====");
            memoryStore.getMessages(SESSION_ID).forEach(IO::println);
            IO.println();

            // 10) Call the endpoint in streaming mode and print the answer token
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
        IO.println();

        // Dropping the conversation is now an explicit act, not a side effect of
        // quitting:
        //memoryStore.deleteMessages(SESSION_ID);
        IO.println("🗑️  Delete " + Path.of(".memory", SESSION_ID + ".json")
                + " to start a fresh conversation.");

        return 0;
    }
}
```

Alors oui, il y a plus de code que dans la version LangChain4j seule, alors que Quarkus est censé nous simplifier la vie 😅.
L'explication est simple : l'extension Quarkus gère la mémoire toute seule et, par défaut, la vide à la fin de la requête.
Ce qui est un comportement souhaitable en mémoire volatile devient gênant quand la mémoire est un fichier : on le supprimerait à chaque arrêt 😱.
Il faut donc reprendre la main sur la création de la mémoire.

 - lignes 37 à 42 : l'_AIService_ avec l'identifiant de mémoire `@MemoryId`, rien ne change par rapport à l'article précédent
 - lignes 45 à 89 : notre `FileChatMemoryStore`, le même que dans la version LangChain4j, mais déclaré comme bean CDI avec `@ApplicationScoped` pour que Quarkus l'utilise à la place de celui par défaut
   - lignes 62 & 74 : là aussi la sérialisation / désérialisation est fournie par LangChain4j
 - lignes 92 à 120 : un simple _wrapper_ autour de la mémoire dont la méthode `clear()` ne fait rien, c'est lui qui empêche Quarkus de supprimer le fichier en fin de requête
 - lignes 123 à 141 : le `ChatMemoryProvider` qui crée la mémoire avec notre _memory store_, ici aussi c'est un bean CDI qui remplace celui par défaut de l'extension
   - lignes 129 à 131 : la taille de la fenêtre est lue depuis la configuration de l'extension, avec `10` par défaut
 - lignes 155 & 156 : on injecte le _memory store_ uniquement pour l'affichage de la mémoire
 - lignes 192 à 194 : l'appel au chatbot avec l'identifiant de mémoire, comme précédemment
 - ligne 208 : la suppression de la conversation devient un acte explicite, plus un effet de bord de la fin de la session 💡

> ℹ️ les lignes 162 à 168 ne servent qu'à afficher la mémoire restaurée, elles ne sont pas nécessaires au fonctionnement.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_04_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="quarkus-file-memory.mov" type="video/quicktime">
</video>

### ☘️ Avec Spring AI

Terminons notre tour des stacks avec [Spring AI](https://spring.io/projects/spring-ai).
{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.0@pom
//DEPS org.springframework.ai:spring-ai-bom:2.0.0@pom
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//DEPS org.aspectj:aspectjweaver:1.9.25.1
//DEPS org.springframework.ai:spring-ai-starter-model-openai:2.0.0
//DEPS jakarta.servlet:jakarta.servlet-api:6.1.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.21.4

// 1) Include application.properties as a classpath resource so Spring reads it.
//FILES application.properties

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// 2) What we store. 
record StoredMessage(String type, String text) {}

// 3) The persistent repository. 
class FileChatMemoryRepository implements ChatMemoryRepository {

    private final Path directory;
    private final ObjectMapper mapper = new ObjectMapper();

    FileChatMemoryRepository(Path directory) {
        this.directory = directory;
    }

    private Path fileFor(String conversationId) {
        return directory.resolve(conversationId + ".json");
    }

    // The method LangChain4j's store does not have. With one file per
    // conversation, the answer is the directory listing.
    @Override
    public List<String> findConversationIds() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the conversations", e);
        }
    }

    // Called by the advisor before every request, to build the context sent to
    // the model. An unknown conversation is not an error: it is an empty one.
    @Override
    public List<Message> findByConversationId(String conversationId) {
        var file = fileFor(conversationId);
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                return List.of();
            }
            List<StoredMessage> stored = mapper.readValue(Files.readString(file),
                new TypeReference<>() {
                });
            return stored.stream().map(FileChatMemoryRepository::toMessage).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the conversation " + conversationId, e);
        }
    }

    // Called by the advisor once the answer is complete, with the conversation
    // already trimmed to the window size.
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            Files.createDirectories(directory);
            var stored = messages.stream()
                    .map(message -> new StoredMessage(message.getMessageType().name(),
                            message.getText()))
                    .toList();
            Files.writeString(fileFor(conversationId), mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(stored));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the conversation " + conversationId, e);
        }
    }

    // Called only when the application asks for it.
    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            Files.deleteIfExists(fileFor(conversationId));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete the conversation " + conversationId, e);
        }
    }

    // Rebuilding the right implementation from the stored type. 
    private static Message toMessage(StoredMessage stored) {
        return switch (stored.type()) {
            case "SYSTEM" -> new SystemMessage(stored.text());
            case "USER" -> new UserMessage(stored.text());
            case "ASSISTANT" -> new AssistantMessage(stored.text());
            default -> throw new IllegalStateException("Unsupported message type: " + stored.type());
        };
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
public class _05_04_StreamingChatbotFileMemory {

   private static final String CONVERSATION_ID = "cli-session";

   private static final Path MEMORY_DIR = Path.of(".memory");

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
                SpringApplication.run(_05_04_StreamingChatbotFileMemory.class, args)));
    }

    @Bean
    CommandLineRunner run(ChatClient.Builder builder) {
        return args -> {
            // 4) The conversation memory. 
            ChatMemoryRepository repository = new FileChatMemoryRepository(MEMORY_DIR);
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(repository)
                    .maxMessages(10)
                    .build();

            // 5) Build the ChatClient with the memory advisor registered by default.
            var chatClient = builder
                    .defaultSystem("provide a concise answer")
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .build();

            // 6) Nothing to restore: reading the memory already reads the file.
            var restored = chatMemory.get(CONVERSATION_ID);
            if (restored.isEmpty()) {
                IO.println("===== 🧠 NO MEMORY YET, STARTING A NEW CONVERSATION 🧠 =====");
            } else {
                IO.println("===== 🧠 MEMORY RESTORED FROM DISK (" + restored.size() + " messages) 🧠 =====");
                restored.forEach(IO::println);
            }
            IO.println();

            IO.println("===== 🧠 CHATBOT WITH PERSISTENT MEMORY (type \"exit\" to quit) 🧠 =====");
            IO.println("💾 stored in " + MEMORY_DIR.resolve(CONVERSATION_ID + ".json").toAbsolutePath());
            IO.println("🗂️  conversations on disk: " + repository.findConversationIds());
            IO.println();

            while (true) {
                // Ask the user for a prompt.
                var userPrompt = IO.readln("⌨️  Your prompt: ");
                IO.println();

                // Leave the loop on "exit", or on end of input (Ctrl+D).
                if (userPrompt == null || userPrompt.equals("exit")) break;
                if (userPrompt.isBlank()) continue;

                // 7) Print the memory as it is BEFORE the call.
                IO.println("===== 🧠 MEMORY (resent to the model with the prompt) 🧠 =====");
                chatMemory.get(CONVERSATION_ID).forEach(IO::println);
                IO.println();

                // 8) Call the endpoint in streaming mode and print the answer token
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

            // Print the final memory: the whole conversation (up to the window
            // size), as stored by the advisor. 
            IO.println("===== 🧠 FINAL MEMORY (the whole conversation) 🧠 =====");
            chatMemory.get(CONVERSATION_ID).forEach(IO::println);
            IO.println();

            // Dropping the conversation stays an explicit act.
            //chatMemory.clear(CONVERSATION_ID);
            IO.println("🗑️  Delete " + MEMORY_DIR.resolve(CONVERSATION_ID + ".json")
                    + " to start a fresh conversation.");
        };
    }
}
```
|}
Le principe est le même qu'avec LangChain4j : on remplace le _repository_ en mémoire volatile par une implémentation de `ChatMemoryRepository` qui écrit dans un fichier, le reste du code ne bouge pas 😎.
Petite différence tout de même : Spring AI ne fournit pas de sérialiseur `JSON` pour ses messages, il faut donc gérer soi-même la conversion, ici avec un simple _record_ et `Jackson`.

 - ligne 39 : le _record_ qui représente un message dans le fichier, le type (`SYSTEM`, `USER`, `ASSISTANT`) et le texte
 - lignes 42 à 126 : notre `FileChatMemoryRepository`, un fichier `JSON` par identifiant de conversation
   - lignes 58 à 70 : la liste des conversations existantes, méthode qui n'existe pas dans le _store_ de LangChain4j, ici c'est simplement le contenu du répertoire
   - lignes 75 à 88 : chargement de la conversation depuis le fichier, appelé par l'_advisor_ avant chaque requête
   - lignes 93 à 105 : sauvegarde de la conversation dans le fichier, appelé par l'_advisor_ une fois la réponse complète
   - lignes 109 à 115 : suppression du fichier quand la conversation se termine
   - lignes 118 à 125 : reconstruction du bon type de message Spring AI à partir de ce qui est stocké
 - lignes 145 à 149 : on branche notre _repository_ sur la mémoire, c'est la seule différence avec la version en mémoire volatile
 - ligne 154 : on ajoute la mémoire à notre chatbot via l'_advisor_, comme précédemment
 - ligne 191 : l'appel au chatbot avec l'identifiant de conversation
 - ligne 209 : la suppression de la conversation reste un acte explicite 💡

> ℹ️ les lignes 158 à 164 et 169 ne servent qu'à afficher la mémoire restaurée et les conversations présentes sur le disque, elles ne sont pas nécessaires au fonctionnement.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_04_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="spring-file-memory.mov" type="video/quicktime">
</video>

# 🏁 Conclusion

Si vous êtes arrivés jusque-là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
