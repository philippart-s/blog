---
title: "☕️ Java in the AI Era 🤖 - part 4: simple RAG"
description: "I've seen things you people wouldn't believe. ©Roy Batty"
link: /2026-10-28-java-and-ai-part4-en
image: cover.jpeg
tags:
  - Java
  - AI
  - Code
  - RAG
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-10-28-java-and-ai-part4) 🇫🇷.

## TL;DR
> 📚 Fourth article of the series about AI in your Java applications ☕️, this time dedicated to **RAG** (_Retrieval Augmented Generation_).  
> 🤷 A model only knows its training data: your internal documents, your conference schedule or your project docs, it has never seen them.  
> 🧮 The principle: turn your documents and the question into vectors (`embeddings`), find the document closest to the question, then add it to the context.  
> 📐 The "closeness" is computed with a simple cosine similarity, a few lines of Java are enough.  
> ☕️ The same use case is implemented from the lowest level to the highest: plain Java, the OpenAI SDK, LangChain4j, Quarkus and Spring AI.  
> 🐙 All the examples use OVHcloud's [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) (`gpt-oss-120b` and `bge-m3`) and are available [here](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

A quick recap of the previous episodes: this article is part of a series meant to explain how to integrate AI into the applications we develop in Java ☕️.
The first three articles laid the foundations ([part 1](/blog/2026-07-21-java-and-ai-part1-en)), then added memory to our chatbot ([part 2](/blog/2026-08-03-java-and-ai-part2-en)) and finally persisted that memory ([part 3](/blog/2026-08-27-java-and-ai-part3-en)).

So I won't come back to the notions of token, context window or memory.
The goal of this article is to tackle another thorny topic: **RAG**, for _Retrieval Augmented Generation_ 📚.

We'll stick to the simplest possible version, the one that lets you understand what happens under the hood.
The more advanced versions (document splitting, vector databases, ...) will come in a future article.

# 🤷 But why do RAG?

A model, however big, only knows what it saw during its training.
Everything that happened after its training cut-off date, or everything that was never public, is completely unknown to it.
And that's often exactly what matters in our applications: our project documentation, the company's internal procedures, our product catalogue, ...

To illustrate this, let's try to get information about a fictional conference: **DevFest Atlantide 2027** 🌊 (if it ever exists, I'll gladly take some royalties 😇).
Its schedule and practical information are described in two Markdown files, in the [documents](https://github.com/philippart-s/java-ai-area-blog/tree/main/documents) directory of the examples repository:

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

Now let's ask our favourite model, `gpt-oss-120b`, the question without giving it anything else:

```text
💬: Who is speaking about quantum compilers at DevFest Atlantide 2027, and in which room?

🤖: I'm not able to retrieve the specific agenda for DevFest Atlantide 2027, so I don't
have the name of the speaker (or the room) for the "Quantum Compilers" session.
...
If you manage to locate the schedule and would like help interpreting it, feel free to
paste the relevant excerpt here—I'll be happy to clarify!
```

A rather honest answer (which isn't always the case with LLMs 🤥).
And above all, the model gives us the solution itself: _paste the relevant excerpt here_.
That's exactly what RAG does: add the right excerpt of our documents to the context, before asking the question 😎.

# 🔎 How does RAG work?

If we break down the acronym, we get three steps:
 - **Retrieval**: find, among all our documents, the ones related to the question
 - **Augmented**: add these documents to the context, on top of the question
 - **Generation**: let the model generate the answer from this enriched context

We already know how to do the last two steps: it's string concatenation and a model call, like in the previous articles.
All the difficulty lies in the first step: how do we know which document talks about the question being asked 🤔?

## 🔤 Why not a keyword search?

The first idea that comes to mind is to look for the words of the question in the documents, like a good old `grep`.
Except that in our example, the question talks about _quantum compilers_ and the schedule about _Compiling for the Quantum Edge_ and _quantum co-processors_.
Not a single _compilers_ in the document 😅.

What we care about is not that the words are identical, but that the **meaning** is close.
And to compare meaning, we have to turn it into something we know how to compare: numbers.

## 🧮 Embeddings

Remember the note in [part 1](/blog/2026-07-21-java-and-ai-part1-en): inside the model, what is manipulated is not a `token` made of letters, but a vector of numbers.
**Embeddings** rely on the same idea, but for a whole text: a specialised model, called an embedding model, turns a text into a fixed-size vector.

In our examples, we'll use the [bge-m3](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/bge-m3/) model, also available on AI Endpoints, which produces vectors of **1024 numbers**.
Whether the text is three words or three paragraphs long, we always get a vector of 1024 numbers.

The interesting property of these vectors is that two texts talking about the same thing will give two vectors "pointing" in the same direction, even if they don't use the same words.

> ℹ️ The embedding model doesn't generate text: it takes a text as input and only returns a vector. It is therefore much smaller, faster and cheaper than an LLM 💸.

The call goes to the same OpenAI-compatible endpoint as the chat, with the `/v1/embeddings` path:

```json
{
  "model": "bge-m3",
  "input": "Who is speaking about quantum compilers, and in which room?"
}
```

And the response contains our vector (shortened here, I'll spare you the other 1020 numbers 😉):

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

## 📐 Cosine similarity

Once we have vectors, we still need to measure how much they "point" in the same direction.
The most widely used measure for that is **cosine similarity**: the cosine of the angle between the two vectors.

Without giving a full maths lesson (my high school memories are as blurry as yours 🫣):
 - two vectors going in the same direction have a similarity close to `1`
 - two unrelated vectors have a similarity close to `0`
 - two opposite vectors have a similarity close to `-1`

And the computation fits in a few lines: the dot product of the two vectors divided by the product of their norms.

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

## 🧩 Simple RAG, in a nutshell

We now have all the building blocks, so here is what every example of this article will do:
 1. read each file of the `documents` directory and compute its embedding (one vector per file)
 2. compute the embedding of the question
 3. compute the cosine similarity between the question and each document, and sort them
 4. take the closest document and paste it into the prompt with the question
 5. send this prompt to the model and print the answer

> ⚠️ This is deliberately the most naive version: one whole document per vector, everything in memory, recomputed on every run. With two small files it works very well, with your company's entire documentation much less so 😅. We'll see how to do better later.

Unlike the previous articles, there won't be a Bash version.
Computing a cosine similarity on 1024-number vectors with `jq` is possible, but do I really want to 🙈?

# ☕️ Java

## ✍️ Plain Java

As usual, we start without any AI library: the JDK `HttpClient` and Jackson for the `JSON`.

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

 - line 20: a `record` to keep together the file name, its content and its vector
 - lines 23 and 25: a second endpoint and a second model, the ones dedicated to embeddings
 - lines 40 to 48: step 1, each Markdown file is turned into a vector
 - line 55: step 2, the question is turned into a vector with the same model (⚠️ this matters, vectors coming from two different models are not comparable)
 - lines 58 to 60: step 3, the documents are sorted from the closest to the furthest from the question
 - line 66: step 4, we only keep the best document...
 - lines 71 to 78: ... which we paste into the prompt, with the instruction to answer only from this context
 - line 81: step 5, a completely standard model call
 - lines 85 to 98: the call to the embedding model, the response is an array of numbers in `data[0].embedding`
 - lines 100 to 108: the cosine similarity, as seen above

> ℹ️ There is neither streaming nor memory in this example: one fixed question, one answer, and we stop. The goal is to focus only on RAG, but nothing prevents you from combining them 😉.

And here is what it looks like at runtime:

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

The schedule does come out ahead of the practical information, even though neither of them contains the word _compilers_ 🎉.
And this time, the model knows the answer: Maya Okonkwo, Nautilus room.

Also note that the practical information is not at `0`: both documents are about the same conference, so there is some closeness.
The score is not an absolute truth, it's a relative ranking between documents.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_05_SimpleRag.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-simple-rag.mov" type="video/quicktime">
</video>

## 🛠️ With the OpenAI SDK

Let's move on to the official OpenAI SDK which, as in the previous articles, will rid us of building the `JSON`.

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

 - line 19: the vector is now a `List<Float>`, the type returned by the SDK
 - lines 23 to 26: a single client for both models, chat and embedding
 - lines 83 to 94: the call to the embedding model, with typed objects instead of `JSON` to walk through
 - lines 96 to 104: the cosine similarity, still up to us
 - lines 106 to 119: the call to the chat model, as in the previous articles

The `main` is identical to the plain Java version: the SDK simplifies the HTTP calls, but it knows nothing about RAG.
Reading the documents, comparing the vectors, picking the best one and building the prompt, all of that is still our job 😅.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_05_SimpleRag.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-sdk-simple-rag.mov" type="video/quicktime">
</video>

## 🦜 With LangChain4j

With [LangChain4j](https://docs.langchain4j.dev/tutorials/rag), we move up a level: RAG is one of the framework's features.

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

 - lines 52 to 56: the embedding model, configured like the chat model
 - line 58: the **embedding store**, where the vectors are kept (in memory here, it's the equivalent of our `ArrayList<Document>`)
 - line 63: loading all the files of the `documents` directory
 - lines 65 to 69: the **ingestor** computes the vector of each document and stores it in the _store_, that's our step 1
 - lines 74 to 78: the **content retriever** computes the vector of the question, looks for the closest document in the _store_ and keeps only one (`maxResults(1)`), those are our steps 2 to 4
 - line 82: we plug the _retriever_ into the AI Service, the same way we plugged in the memory in part 2
 - line 90: the call to the assistant, which only takes the question

No more cosine similarity, no more sorting, no more prompt to build by hand: LangChain4j does all of it on every `chat` call 😎.

> ℹ️ Since the similarity is computed by the framework, this example no longer prints the scores or the retrieved document. To see what is actually sent to the model, you can enable `.logRequests(true)` on the `ChatModel` (replacing `slf4j-nop` with `slf4j-simple`): the question is followed by `Answer using the following information:` and then the content of the closest document.

> ⚠️ By default, LangChain4j doesn't split documents if it has no `DocumentSplitter`: we therefore get one vector per file, as in the previous examples. It's intended here to compare like with like, but in practice documents are almost always split into chunks (more on that later 😉).

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_06_SimpleRag.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="langchain4j-simple-rag.mov" type="video/quicktime">
</video>

## ⚡️ With Quarkus

Quarkus goes even further thanks to the [Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html) extension of `quarkus-langchain4j`.
As its name suggests, its goal is to make RAG simple, even invisible.

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
// Docs: https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html

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

Yes, that's all 🤯.
No document loading, no embedding model, no _store_ and no _retriever_: the code only contains the AI Service and the question.

 - line 9: the Easy RAG extension, which does all the work
 - lines 5, 6 and 10 to 12: Easy RAG ships [Apache Tika](https://tika.apache.org/) to read all kinds of documents (PDF, Word, ...), which requires a few tweaks so that everything goes smoothly with Java 26 and the logs
 - line 26: the configuration lives in a dedicated file, `rag.properties`, mounted as `application.properties`
 - lines 35 to 39: a completely standard AI Service, nothing tells you it does RAG
 - line 58: the call, with only the question

Everything else happens in the configuration:

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

 - line 10: the embedding model, which uses the same configuration (URL and key) as the chat model
 - lines 15 and 16: the directory to ingest at startup, and only the Markdown files
 - line 18: Easy RAG splits documents by default (into 300-token chunks), so we increase the maximum chunk size to keep one vector per file
 - line 19: we only keep the closest document, as in the other examples

> ℹ️ Ingestion happens on every application startup: if you add a document to the directory, it will be taken into account on the next run, without changing a single line of code.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_05_SimpleRag.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="quarkus-simple-rag.mov" type="video/quicktime">
</video>

## ☘️ With Spring AI

Finally, let's see how [Spring AI](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html) handles RAG.
As with memory, it goes through an _advisor_.

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

 - lines 9 and 10: two extra dependencies, one for the in-memory _vector store_ and one for the RAG _advisor_
 - line 27: as with Quarkus, a dedicated configuration, which only adds the embedding model (`spring.ai.openai.embedding.options.model=bge-m3`)
 - lines 61 and 62: the RAG _advisor_ leaves threads behind that prevent the JVM from stopping, hence the `System.exit` 🙄
 - lines 73 to 78: loading the files with a `TextReader`, one document per file
 - lines 81 and 82: the in-memory **vector store**, which computes the vector of each document when it is added, that's our step 1
 - lines 87 to 92: the RAG _advisor_, which looks for the document closest to the question (`topK(1)`) and adds it to the prompt, those are our steps 2 to 4
 - line 96: we plug the _advisor_ into the `ChatClient`, as for the memory in part 2
 - lines 104 to 107: the call, with only the question

We find exactly the same concepts as with LangChain4j, with a different vocabulary: _vector store_ instead of _embedding store_, _advisor_ instead of a _retriever_ plugged into the AI Service.

The prompt built by the _advisor_ (through its `ContextualQueryAugmenter`) is also more directive than ours:

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

Asking the model to say it doesn't know rather than making things up is a good habit to limit hallucinations.
Funny side effect: combined with our _provide a concise answer_, the answer becomes downright terse 😅.

```text
===== 🤖 ANSWER 🤖 =====
Maya Okonkwo, Nautilus.
```

> ℹ️ This prompt can be customised by passing your own `ContextualQueryAugmenter` to the _advisor_ with `.queryAugmenter(...)`.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_05_SimpleRag.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="spring-simple-rag.mov" type="video/quicktime">
</video>

# 🔭 What's next?

Let's be honest, this RAG is deliberately simplistic and won't survive production:
 - **one vector per document**: with a 200-page document, the vector represents an "average meaning" of the whole document and the search becomes much less accurate, not to mention that the whole document may not fit in the model's context window
 - **everything is in memory**: the vectors are recomputed on every run, which costs time and calls to the embedding model 💸
 - **a single retrieved document**: if the answer is spread across several documents, the model will only get part of the information

The answers to these problems exist, and the frameworks already provide them:
 - **splitting** documents into pieces (_chunks_ or _segments_), with some overlap between them so as not to cut a piece of information in half
 - **vector databases** to store the vectors once and for all: PostgreSQL with `pgvector`, Qdrant, Milvus, Elasticsearch, Redis, ...
 - tuning the **number of results** and a **minimum score** to keep only the truly relevant documents

You'll find the details here:
- [LangChain4j](https://docs.langchain4j.dev/tutorials/rag)
- [Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag.html)
- [Spring AI](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)

# 🏁 Conclusion

That's it for this first look at RAG.
Behind the somewhat intimidating acronym, there are in the end only three ideas: turning text into vectors, comparing these vectors, and pasting the result into the prompt.
A hundred lines of Java without any library are enough to do it, and the frameworks reduce it to a few lines, or even to a few configuration properties with Quarkus.

The next article will most likely be dedicated to a more realistic RAG: document splitting and storing the vectors in a real database.
But that's another story.

All the source code is available in the GitHub repository [java-ai-area-blog](https://github.com/philippart-s/java-ai-area-blog).

If you made it this far, thanks for reading! If you spot any typos, feel free to open an [issue or PR](https://github.com/philippart-s/blog) 😊.
