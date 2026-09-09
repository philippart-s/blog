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

```


# 🏁 Conclusion

Si vous êtes arrivés jusque-là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
