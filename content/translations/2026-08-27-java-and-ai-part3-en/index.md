---
title: "☕️ Java in the AI Era 🤖 - part 3: memory storage"
description: "All those moments will be lost in time, like tears in rain. ©Roy Batty"
link: /2026-08-27-java-and-ai-part3-en
image: cover.webp
tags:
  - Java
  - AI
  - Code
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-08-27-java-and-ai-part3) 🇫🇷.

## TL;DR
> 🗃️ Third article of the series about AI in your Java applications ☕️, a direct follow-up to the one about **memory**: this time we make it **survive a restart**.  
> 📄 The simplest approach to understand how it works: one `JSON` file per conversation, the format is already the one we send to the model.  
> ☕️ The same use case is implemented from the lowest level to the highest: Bash, plain Java, the OpenAI SDK, LangChain4j, Quarkus and Spring AI.  
> 🔌 With the frameworks, you only have to implement one interface (`ChatMemoryStore` for LangChain4j, `ChatMemoryRepository` for Spring AI) and the rest of the code doesn't change.  
> ⚡️ Quarkus pitfall: the extension clears the memory at the end of the request, you have to take back control so the file isn't deleted on every shutdown.  
> 🔭 The file approach is not _prod ready_, but the logic is the same for PostgreSQL, Redis, Cassandra or Neo4j: implementations already exist in every framework.  
> 🐙 All the examples use OVHcloud's [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) and are available [here](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

A quick recap of the previous episodes: this article is part of a series meant to explain how to integrate AI into the applications we develop in Java ☕️.
It follows the [previous article](/blog/2026-08-03-java-and-ai-part2-en) whose goal was to explain how memory works in a chatbot.

So I won't go back over the things you need to know to start your journey into AI.
The goal of this article is to go further on the memory side: which architectures to pick to persist that memory 🗃️.

# 📄 Using a JSON file

It's the simple and naive approach, but one that survives a restart of the process.
The `JSON` format avoids conversions since, in the end, that's the format in which it will be sent to the endpoint.

### #️⃣  Bash

For now, it's still not too costly to do it in Bash.

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

 - lines 14 to 16: the file representing the memory will be stored in a `.memory` directory
 - lines 21 to 25: we initialize the memory from the file (if it exists)
 - line 93: saving the memory to the file

As always, this approach is naive but it gives a clear understanding of how it works and why it's more convenient to use the `JSON` format 😎.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/00_bash/00.04_StreamingChatbotFileMemory.sh) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="bash-file-memory.mov" type="video/quicktime">
</video>

### ✍️ Pure Java

Let's move on to the Java version without any particular framework.

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

As with the previous examples of the other articles, the pure Java approach looks very much like the Bash one.

 - lines 20 to 23: creating the `JSON` file to store the memory
 - lines 36 & 37: loading the conversation history
 - lines 112 to 120: saving the exchanged messages

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_04_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-file-memory.mov" type="video/quicktime">
</video>

### 🛠️ With the OpenAI SDK

Let's move on to the [official OpenAI SDK](https://github.com/openai/openai-java) to see whether we can make our life a bit easier 😉.

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

Well, I'm not sure we gain much in simplicity.
Mainly because the SDK provides nothing for persistence, so we handle it by hand and then send the whole history back 😨.

 - lines 23 to 25: we initialize the file memory
 - line 38: the `params builder` remains the storage structure of the memory
 - lines 42 to 55: loading the memory (if it exists), thanks to `Jackson` the serialization / deserialization is in the right format
 - lines 61 to 94: still the same loop to interact with the model
 - lines 97 to 107: adding the messages to the memory and saving to the file

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_04_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-sdk-file-memory.mov" type="video/quicktime">
</video>

### 🦜 LangChain4j

Let's see how [LangChain4j](https://docs.langchain4j.dev/intro/) lets us manage a memory persisted in a file.

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

This time, we get the comfort of LangChain4j back: persistence is no longer in the conversation loop but in an implementation of `ChatMemoryStore` that the framework calls for us 😎.
We don't necessarily save many lines because we have the implementation of our `FileChatMemoryStore` (which in real life would live in another class) and quite a lot of debug output.

 - lines 29 to 75: our `FileChatMemoryStore`, one `JSON` file per memory id (⚠️ to keep the code simple I don't use the id afterwards ⚠️)
   - lines 42 to 53: loading the memory from the file, called by LangChain4j before every request
   - lines 56 to 64: saving the memory to the file, called by LangChain4j once the answer is complete
   - lines 67 to 74: deleting the file when the conversation ends
   - lines 49 & 60: the serialization / deserialization of the messages is provided by LangChain4j through `ChatMessageSerializer` and `ChatMessageDeserializer`
 - lines 97 to 102: we plug our _memory store_ into the memory, it's the only difference with the volatile in-memory version
 - line 107: we add the memory to our chatbot, as before
 - line 162: with access to the _memory store_ we can delete the file 💡

> ℹ️ lines 110 to 118 only print the restored memory, they are not needed for it to work.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_05_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="langchain4j-file-memory.mov" type="video/quicktime">
</video>

### ⚡️ Quarkus and LangChain4j

Now let's see how Quarkus fares at managing memory in a file.

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

So yes, there is more code than in the LangChain4j-only version, even though Quarkus is supposed to make our life easier 😅.
The explanation is simple: the Quarkus extension manages the memory on its own and, by default, clears it at the end of the request.
What is a desirable behaviour for volatile memory becomes a problem when the memory is a file: it would be deleted on every shutdown 😱.
So we have to take back control of how the memory is created.

 - lines 37 to 42: the _AIService_ with the `@MemoryId` memory id, nothing changes compared to the previous article
 - lines 45 to 89: our `FileChatMemoryStore`, the same as in the LangChain4j version, but declared as a CDI bean with `@ApplicationScoped` so that Quarkus uses it instead of the default one
   - lines 62 & 74: here too the serialization / deserialization is provided by LangChain4j
 - lines 92 to 120: a simple _wrapper_ around the memory whose `clear()` method does nothing, this is what prevents Quarkus from deleting the file at the end of the request
 - lines 123 to 141: the `ChatMemoryProvider` that creates the memory with our _memory store_, here too it's a CDI bean that replaces the extension's default one
   - lines 129 to 131: the window size is read from the extension's configuration, with `10` as default
 - lines 155 & 156: we inject the _memory store_ only to print the memory
 - lines 192 to 194: the call to the chatbot with the memory id, as before
 - line 208: dropping the conversation becomes an explicit act, no longer a side effect of the end of the session 💡

> ℹ️ lines 162 to 168 only print the restored memory, they are not needed for it to work.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_04_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="quarkus-file-memory.mov" type="video/quicktime">
</video>

### ☘️ With Spring AI

Let's finish our tour of the stacks with [Spring AI](https://spring.io/projects/spring-ai).
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
The principle is the same as with LangChain4j: we replace the volatile in-memory _repository_ with an implementation of `ChatMemoryRepository` that writes to a file, the rest of the code doesn't change 😎.
One small difference though: Spring AI doesn't provide a `JSON` serializer for its messages, so we have to handle the conversion ourselves, here with a simple _record_ and `Jackson`.

 - line 39: the _record_ representing a message in the file, the type (`SYSTEM`, `USER`, `ASSISTANT`) and the text
 - lines 42 to 126: our `FileChatMemoryRepository`, one `JSON` file per conversation id
   - lines 58 to 70: the list of existing conversations, a method LangChain4j's _store_ doesn't have, here it's simply the content of the directory
   - lines 75 to 88: loading the conversation from the file, called by the _advisor_ before every request
   - lines 93 to 105: saving the conversation to the file, called by the _advisor_ once the answer is complete
   - lines 109 to 115: deleting the file when the conversation ends
   - lines 118 to 125: rebuilding the right Spring AI message type from what is stored
 - lines 145 to 149: we plug our _repository_ into the memory, it's the only difference with the volatile in-memory version
 - line 154: we add the memory to our chatbot through the _advisor_, as before
 - line 191: the call to the chatbot with the conversation id
 - line 209: dropping the conversation stays an explicit act 💡

> ℹ️ lines 158 to 164 and 169 only print the restored memory and the conversations present on disk, they are not needed for it to work.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_04_StreamingChatbotFileMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="spring-file-memory.mov" type="video/quicktime">
</video>

# 🔭 What's next?

Let's be honest, storing in a file is not the most relevant option.
It's useful to explain how it works, but it's not really _prod ready_.

In the end, the logic will be the same for other kinds of storage, only more _sophisticated_: relational or NoSQL database, cloud storage or _key value_ store for instance.
For that, there are two approaches: either there is an existing implementation provided by the library, or you'll have to build it yourself.

For the existing implementations, I'll let you check the docs of each framework, but for example you'll find PostgreSQL, Cassandra, Redis, Neo4j, etc.
You'll find the details here:
- [LangChain4j](https://docs.langchain4j.dev/integrations/chat-memory-stores/)
- [Quarkus LangChain4j](https://quarkus.io/extensions/?search-regex=memory%20store)
- [Spring AI](https://docs.spring.io/spring-ai/reference/api/chat-memory.html#_memory_storage)

And if you can't find what you're looking for, as you've seen, most of the time you only have to implement an interface and you're done 😎.


# 🏁 Conclusion

That's it for this decidedly too long part about memory management for a chatbot.
But I thought it was worth taking the time to show you how to get closer to what you usually have with your favourite assistant.

The next article will most likely be dedicated to another thorny topic: RAG (for _Retrieval Augmented Generation_).
But that's another story.

If you made it this far, thanks for reading! If you spot any typos, feel free to open an [issue or PR](https://github.com/philippart-s/blog) 😊.
