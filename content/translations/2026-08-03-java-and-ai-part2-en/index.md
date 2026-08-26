---
title: "☕️ Java in the AI Era 🤖 - part 2: memory"
description: "Those aren't your memories. They're somebody else's. ©Rick Deckard"
link: /2026-08-03-java-and-ai-part2-en
image: cover.webp
tags:
  - Java
  - AI
  - Code
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-08-03-java-and-ai-part2) 🇫🇷.

## TL;DR
> 🧠 Second article of the series about AI in your Java applications ☕️, this time dedicated to your chatbots' **memory**.  
> 🤖 Models are `stateless`: without memory on the client side, your chatbot forgets your first name from one question to the next.  
> 💸 Sending the whole conversation back on every call costs tokens: we look at how to cap the memory size, compress it or do some `prompt caching`.  
> ☕️ The same use case is implemented from the lowest level to the highest: Bash, plain Java, the OpenAI SDK, LangChain4j, Quarkus and Spring AI.  
> 🔀 Bonus: one memory per person thanks to LangChain4j's `@MemoryId`.  
> 🐙 All the examples use OVHcloud's [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) and are available [here](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

A quick recap of the previous episodes: this article is part of a series meant to explain how to integrate AI into the applications we develop in Java ☕️.
It follows the [previous article](/blog/2026-07-21-java-and-ai-part1-en) whose goal was to lay the groundwork on how to develop an application with AI, like a chatbot for example.

So I won't go back over the things you need to know to start your journey into AI.
The goal of this article is to see why the first thing you'll want to do with your chatbot is to manage its memory 💿.

> ℹ️ Given the length of the article, I'm splitting it in two: this one to show how to simply add memory, and another one dedicated to the storage alternatives (it will come later).

# 💿 But why manage memory?

Let's take one of our previous examples, say the [LangChain4j](https://docs.langchain4j.dev/) version (picked at random 😇).

Let's change the end, to ask two questions one after the other:

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
Nothing crazy here, we just added a question right after the answer to the first one (lines `9 to 17`).

Let's look at the result:
```bash
💬: My name is Stéphane

===== 🤖 ANSWER (streaming) 🤖 =====
Nice to meet you, Stéphane! How can I assist you today?
💬: What is my name?
🤖: I don’t know your name—could you tell me what it is?
```

Pretty odd, isn't it 🤔?
You can't really say the request is very complex, nor that the information goes very far back in the conversation 💬.

But then, why do your favourite chatbots seem to remember your whole conversation perfectly, while ours gets lost after only two exchanges 😳?

Quite simply because your favourite models have no state (so we say they are `stateless`).
And that's where you realize that without the client (the CLI, the web site, ...) your chatbot offers a pretty poor user experience.
So very often, it's the client (meaning the piece of software that calls the model) that will manage this memory.

I suggest we look at the different choices available to you, but also at the difficulties you'll have to solve when you want to add a bit of memory to your chatbot.

# 🔎 How does memory work?

In itself the memory mechanism for a model is quite simple: you just have to send it back the whole (or part of the) current conversation so that it can include it in its context and therefore generate the right tokens by taking into account not just the last question, but the whole conversation 📚.

## 💸 Money, money, money

And at this point a light should normally go on 🚨: words being tokens, tokens being billed...
The bigger your chatbot's memory, the more tokens you're going to send.
And that on every single question!

Yes 💸...

And even for a tiny little question.
Imagine you have your chatbot generate a 500-page book.
On every question afterwards, not only will your question be sent, but your 500-page book as well 😳.

We can see straight away that we'll need mechanisms to avoid that.

## 📦 Capping the memory size

It looks like a fairly obvious (even naive) approach, but one that works 👍.
Think of it as a fixed-size pile, FIFO (**F**irst **I**n **F**irst **O**ut).
And once the maximum size is reached, the oldest message is dropped.

Simple and effective.
Effective for the size, but potentially risky for the meaning and the accuracy of your future exchanges.

It may well be that the first messages are important to set the context and that without them, your model slowly loses the initial goal of the conversation.
Worse: imagine your pile has a size of 10 and that the last ten messages are completely unrelated to the beginning of your conversation; from the eleventh message on, your model will have _forgotten_ the reason for the exchange you're having with it 😣.

> 💡 While I'm at it, let me also insist on the fact that when you use your favourite chatbot, mixing up discussions is not a good idea.
> Most interfaces let you create several discussion threads, do it.
> One per topic, so that your model is as relevant as possible, whatever the way memory is managed.

Of course, there are variants.
For example you can choose what you're going to keep in memory depending on how relevant the exchange is.
That choice can be driven by the user or by your chatbot with a relevance score on the answer (often delegated to another model and therefore consuming tokens 💸).

## 🗜️ Compressing the memory

What if we had a way to compress the memory like we do with files for instance 💡?

That is indeed a mechanism most clients implement.
Typically when you see the `compacting...` message.
But careful, there's no question here of a lossless compression that would let you recreate the whole information afterwards.
Compression works differently: the client will delegate (to the current model or to another one) the summary of the conversation thread since the last time it made one (including that summary, of course).

It works pretty well and is a good trade-off to save tokens without a total loss of information 🗃️.
There is, however, a limit.
The longer your session, the more you'll slowly make notions disappear that don't come up often in your discussion but are nonetheless important 😶‍🌫️.
And that's exactly what you notice for yourself with your usual chatbots.

> That feeling that at some point the chatbot is _going round in circles_ or answers in a less relevant way, or even with no relation to the initial request of your discussion 🔄.
> It's often the signal to start over on a new session, laying down a few basics again.

## 🥷 Prompt caching

This isn't strictly speaking a way of managing your conversation's memory, but rather a way of optimizing it.
The idea behind it is to store the prompts with their answers, then to return the already computed answer directly if the same prompt (or a close one) is sent to the model.
The difficulty being to know which _prompt / answer_ pairs to store and how to determine that a prompt looks like another one.
We'll see later the use of semantic databases (thanks to vectors) which can be good candidates.

> It can also be an alternative to classic memory: you store all the exchanges in a semantic database.
> Then on every request you search the database to return the relevant previous exchanges and inject them into the memory you send to the model.

We can also imagine asking a smaller model to determine whether two prompts look alike (that will often be cheaper than generating the same answer twice with a big model).

## 🪟 The context window to take into account

Remember, models have a maximum number of tokens they can receive, that's what we call the `context window` 🪟.
So you have to take it into account when managing memory.
If your memory is too big, the risk is to saturate it very quickly and to no longer be able to send or receive anything 💥.
Here again, if you build a chatbot that can connect to several models, you'll need to be able to retrieve the size of that context window to create a matching memory.

# 💿 Storing the data

We've looked together at the different approaches to memory management.
Now, let's dwell on how this data is stored 💿.

## 🐣 The naive approach

Indeed, the naive approach (but one that works) is to simply send the whole conversation back on every call and to store that in a volatile way in memory (your heap, not the model's 🤪).
For a demo, or simply a use case where losing the history doesn't matter, that's perfectly conceivable.

### #️⃣  Bash

Let's look at our naive approach without any help.
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

Compared to the first version in my [previous article](/blog/2026-07-21-java-and-ai-part1-en), the notable thing is the manual management of my memory in the `MESSAGES` variable.
Indeed:
 - lines 12 to 14: we initialize the list of messages with the system prompt
 - lines 31 & 32: we add the user prompt
 - lines 61 & 62: we add the model's answer (this is the step that gives us the memory)

Then we loop back to the user prompt input step.

This simple example lets us understand clearly by looking at the exchanged payloads:
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

So the memory is simply a stack of messages:
 - on the first payload sent:
   - lines 11 & 12: we put the system prompt
   - lines 15 & 16: the person's prompt
 - on the second payload sent:
   - lines 30 to 37: these are the messages of the first payload sent
   - lines 39 & 40: this is the model's answer
   - lines 43 & 44: the person's new prompt

Sure, this example and its implementation are naive, but you get a visual understanding of how our chatbots' memory works and why it consumes so many tokens 💸.

To see the full source of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/00_bash/00.03_StreamingChatbotMemory.sh) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="bash-simple-memory.mov" type="video/quicktime">
</video>

Now let's look at this simple example with a slightly more evolved language 😉.

### ✍️ Pure Java

As in my previous article, the pure Java approach is going to look like what we do in Bash.

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

As you can see, it works very much like the Bash version, but this time written in Java, thanks [JBang](https://www.jbang.dev/) 😍.
 - lines 26 to 29: initialization of our memory by adding the system prompt (_role_ `system`)
 - lines 48 to 50: adding the user prompt (_role_ `user`)
 - lines 61 to 66: sending to the model and receiving the answer
 - lines 72 to 89: extracting the answer
 - lines 97 to 99: adding the answer to the memory (_role_ `assistant`)

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-simple-memory.mov" type="video/quicktime">
</video>

It's time to do fewer things manually and to get some help from a few tools 🛠️.

### 🛠️ With the OpenAI SDK

We've seen how to add memory to our model by hand 💿.
Let's see how to make our life easier, now that we know how it works 🔎.

First tool to be used, the [official OpenAI SDK](https://github.com/openai/openai-java).
> ℹ️ Remember that I use the OpenAI tools because the OVHcloud models are compatible with the OpenAI routes.

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

The principle is the same: we enrich the message we send to the model on every round trip 📚.
The only difference is that the SDK lets us avoid building the JSON structure by hand 😎.
 - lines 28 to 30: we prepare what we're going to send with the system prompt; this will also be the _object_ representing the memory,
 - line 45: we add the user prompt
 - lines 56 to 64: we send the request, we print the answer and we store it in an _accumulator_ so we can add it to the memory afterwards
 - lines 72 to 76: we add the answer stored in the _accumulator_ to our memory so that it's sent on the next request.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-sdk-simple-memory.mov" type="video/quicktime">
</video>

I think we can make it even simpler 😎.

### 🦜 LangChain4j

Well yes, let's move on to my favourite framework: [LangChain4j](https://docs.langchain4j.dev/intro/) 🦜.

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

Our code is starting to be much simpler, isn't it 😎?
- line 41: we initialize our memory with a size of 10. Note that we hadn't capped the size in the previous examples, quite simply because we would have had to handle it by hand. Here, it's offered by LangChain4j, so let's not deprive ourselves of it 🤗
- line 46: we add the memory feature to our model
- lines 69 to 73: we print the model's answer

> ℹ️ the bits of code like `chatMemory.messages().forEach(IO::println);` are only there for debug / explanation purposes.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="langchain4j-simple-memory.mov" type="video/quicktime">
</video>

Maybe while reading the code, one thing bothers you 🤔.
What happens if I have several users?  
Do they share the same memory 🔀?  
With the existing code, yes 🥺.
Luckily, LangChain4j easily lets us separate the memories per person.

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

Here we see the few differences with a memory without an `id`.
 - line 24: we add the `@MemoryId` annotation in order to identify a memory uniquely
 - line 43: we declare the in-memory _memory store_ (this step is optional since it's the default behaviour, but it then lets us print the memory content)
 - lines 50 to 54: we declare a `chatMemoryProvider` to get per-id management of each memory
 - lines 63 to 66: examples showing how to use our chatbot with the memory id
 - line 78: with access to the _memory store_ we can also handle cleaning it up 💡

> - 💡 of course we could have had the same behaviour in `Bash` or in the pure Java / SDK version but that would have required more code 👩‍💻
> - 🗑️ there's quite a lot of code for debug or explanation purposes: if you remove it, you end up with something far more concise

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_04_StreamingChatbotMultiSessionMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="langchain4j-simple-memory-id.mov" type="video/quicktime">
</video>


### ⚡️ Quarkus and LangChain4j

Let's push simplification a bit further by adding [Quarkus](https://quarkus.io/).
For our example, it's a bit overkill, but it lets us see how to add it into an existing Quarkus application.

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

Once again, the code could be more concise, but I've added a bit of debug.   
> - ℹ️ There's nothing particular to add to manage memory, the Quarkus extension enables memory by default.
> - ⚠️ so remember to disable it if you don't want it by setting `chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class` in `@RegisterAiService`

- lines 21 to 25: we declare the _AIService_ (with the default memory) and the memory id with `@MemoryId`
- lines 39 & 40: we inject the _memory store_ only to get some debug, it's created by default otherwise
- line 66: we call the chatbot with the memory id to avoid conflicts

> ℹ️ The code uses the memory id, but in the end the example is single-user. It just makes the code more robust, just in case.

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_03_StreamingChatbotMemory.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="quarkus-simple-memory-id.mov" type="video/quicktime">
</video>

### ☘️ With Spring AI

Let's go all the way through our stacks with [Spring AI](https://spring.io/projects/spring-ai).

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

In the end, we can see that the code looks very much alike from one framework to another with the simple approach of volatile in-memory management.
 - lines 48 to 51: creating the memory with a size of `10`
 - line 57: enabling memory for our chatbot
 - line 82: handling multi-conversation with an `id`

The full source is available [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_03_StreamingChatbotMemory.java) 📜.


#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="spring-simple-memory-id.mov" type="video/quicktime">
</video>

# 🏁 Conclusion

This article is already far too long and I don't want to rush the end, in which I wanted to talk about the memory storage alternatives (to get out of the simple in-memory use case).
So I'll stop here, and I'll dedicate a next article to that follow-up, to take the time and make it more digestible 🤗.

If you made it this far, thanks for reading! If you spot any typos, feel free to open an [issue or PR](https://github.com/philippart-s/blog) 😊.
