---
title: "☕️ Java in the AI Era 🤖 - part 1: the basics"
description: "I’m sorry, Dave. I’m afraid I can’t do that. ©HAL 9000"
link: /2026-07-21-java-and-ai-part1-en
image: cover.png
tags:
  - Java
  - AI
  - Code
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-07-21-java-and-ai-part1) 🇫🇷.

## TL;DR
> 🚀 First article of a series to learn how to **add generative AI (LLM) to your Java applications**.  
> 🧠 We first (re)lay the groundwork: `token`, context window, parameters/weights, `temperature` and API.  
> 💬 We build a simple chatbot (with a `system prompt` and streaming) that we'll grow throughout the series.  
> ☕️ The same use case is implemented from the lowest level to the highest: CURL, plain Java, the OpenAI SDK, LangChain4j, Quarkus and Spring AI.  
> 🔌 All the examples use OVHcloud's [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) and the open weight model `gpt-oss-120b`.  
> 🐙 Code and demo available [here](https://github.com/pigumax/Java-and-AI-Part1).
> 📖 Jump directly to [part 2](/blog/2026-08-03-java-and-ai-part2-en) about memory


<br/>

# 📜 Introduction
Ah, summer: its beaches, its drinks... and its good resolutions 🤣 !  
Mine will be to start a series of articles around Artificial Intelligence (AI) and Java! 🤖 ☕️
Why write such articles in 2026, you'll ask me?
Quite simply because, over the talks and workshops I give, I realize that AI remains a slightly blurry area for many developers when it comes to adding it into applications rather than just using it.

So I'm inviting you to take the grand tour, calmly.
We probably won't cover everything, but I'll try to give you as many elements as possible to start your journey into AI in Java.

So, we're going to talk mostly about generative AI and more specifically about Large Language Models (LLM).

> I'm going to take the time to write this series by hand, no slop 😆 ! 
> Not that I don't like AI, but I'll also take the opportunity to dig deeper into some concepts and therefore take the time to learn 😉.
> So don't be surprised by the time between articles ⏳.

# 🤖 A few basics about LLMs and how to use them

To get started, we'll take a few minutes to review together some essential elements needed to understand the code coming up in this article and the next ones.

## 🧩 A token

Let's start with a key notion in the world of LLMs: the `token`.
A `token` is a bit like the equivalent of a word in a sentence.
It's a way of splitting text so that it can be understood and analyzed by the model.
Except that here a word as we know it doesn't necessarily map to a single `token`.
For example, for French, it very often takes on average 4 `tokens` for 3 words (even if it's not an absolute rule and it can vary depending on the model).

> ℹ️ In reality, inside the model it isn't a token in the form of letters that is manipulated, but a vector of numbers.

And why does it matter 🤔 ?

Quite simply because that's what you'll be billed for 💸 ! 

So watch your consumption, especially since most of the time the billing differs between input and output.

## 🪟 The context window

It's a concept tied to that of tokens.
Namely, it's the maximum number of tokens the model can process during a request, whether in input or output.

And depending on the model, you'll have a larger or smaller size, which will let you send more or less data (but also receive more or less).

## 🎛️ The parameters (or weights)

We talk fairly interchangeably about weights and parameters for models (even if it's not exactly the same thing, it doesn't matter to us).

The number of parameters of a model is very often the number in the name preceding a B.
The B standing for billions, it therefore represents the number of parameters of the model in billions.
For example my-LLM-2B is a model that has 2 billion parameters.

But in the end, is it good to have a lot of parameters 🧐 ?

The number of parameters represents the model's capacity to learn more or less diverse and deep things.
Of course, in the end it will depend on the training data 😉.

So yes, having a lot of parameters is good, but for some tasks it isn't necessary.
And above all, beware of the hardware power required by big models (aka with lots of parameters).

A simple formula to compute the memory needed for a model: _2 bytes x Number of parameters_.
Then, add 20% for the activations.

For example, for a small model of 2 billion parameters, we're therefore at 4 GB of RAM to run the model 😱 !  
Take an example of a medium model with 35 billion parameters, we'll then need 84 GB of RAM 💥.  
And for a big model, with 200 billion parameters, we'll need 480 GB of RAM 😱 !  

So you'll have to find a compromise between learning capacity and the infrastructure needed for your model.

Nothing stops you from testing what I'm going to show you on smaller models in self hosting.
For everything related to this domain, I can't recommend enough all the work done by [Philippe Charrière](https://k33g.org/) on the subject 🤩.

> In this series of articles, I'm going to use OVHcloud's models through the [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) product.
> I'm lucky enough to work for OVHcloud and to have access to these models 😆.
 
For the various examples, I'm going to use OVHcloud's open weight model [oss-gpt-120B](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/).
It's a model that has good performance and lets us cover all our use cases with a "reasonable" size.

## 🌡️ The temperature

In the various code examples we're going to discover together, we'll set a parameter called `temperature`. 
In a nutshell, the `temperature` lets you make your model more or less creative and deterministic.
The more you increase the `temperature` (generally between 0 and 1.0 or 2.0 depending on the provider), the more creative the model will be, and conversely, the more you lower it towards 0, the more deterministic it will be.

## 🌐 API

Last notion I want to introduce: how to access your models (local or remote).
Here, pretty much everyone does the same thing: expose an API that consumes / produces a JSON payload.

> At OVHcloud, we made the choice to adopt OpenAI's API as the de facto standard.
> That's why most of the code examples you'll see afterwards will use compatible libraries.

# 💬 Use case

I won't be original for the use case of the examples, it'll be a chatbot.
This use case is admittedly very well known, but it will let me introduce over time the various notions you'll need to integrate AI into your developments.
It also lets me easily test different approaches depending on the chosen Frameworks.

So we'll build a simple chat with or without streaming.
With or without memory, RAG, function calling, MCP, agentic, skills,...

We'll come back to what all these barbaric terms mean in each part, which will cover how to implement them in your Java applications.

> I'll try to update this article as we go to have the links to each sub-article covering one of these notions.

So let's start with the basics: a simple chatbot with a user prompt and the option to answer with streaming or not.
This will be the only example where we use all the technologies / frameworks.
Indeed, sometimes it will be far too complex not to use frameworks.
That doesn't mean it's not possible, but my goal isn't to rewrite a full SDK 🙃.

The breakdown of this use case is therefore as follows:
 1. send a prompt and display the answer once it has been generated by the model
 2. use a `system prompt`
 3. enable streaming mode to see the answer arrive on the fly

But what is a `system prompt` 🤔 ?

Think of it as a _steering of the model's behavior_: we're going to point it in a certain direction to follow in its answers.  
For example, if you ask a model: "what is a plate for?".
There's a good chance it'll answer "for eating".
If you set a `system prompt` (we'll see how): "You are an aeronautics specialist".
There's a good chance it'll answer: "To stabilize a plane."

> Beyond "giving a direction", the system prompt is often used to define a tone, constraints, a response format, behavior rules, a persona, etc.

# 🖥️ The basics: CURL

Before diving into Java, let's look at a few simple requests in bash with CURL.
The idea isn't to do all the examples this way, it would be far too complex 😅.
But our first use case is doable with this approach, so let's make the most of it.

## 💬 Send a prompt and display the answer

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

Let's look in a bit more detail at what we do in this code.

Step 1️⃣ sets, among other things, the API key for AI Endpoints so as not to have a rate limit.
> Note that without this key, the example works, but you're limited to 2 requests per IP per minute.

Step 2️⃣ builds the payload we send, with a prompt like "Why is the sky blue? (provide a concise answer)":
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
Here we have a minimalist structure to send a request to our model:
 - `2.1` : the model we want to use, here `gpt-oss-120b`. Indeed, several models are available.
 - `2.2` : an array with a list of messages to send. For our first example, there's only one.
 - `2.3` : the role of the persona sending the message, here it's you (we'll see that this will change) with the role of type `user`
 - `2.4` : the prompt that will be used.

Step 3️⃣ consists of calling the endpoint by adding the token to the payload in the header.

Step 4️⃣ is the raw response:
```json
{
  "id": "chatcmpl-affdf154315ba79c",
  "choices": [                        (4.1)
    {
      "index": 0,
      "message": {                    (4.2)
        "role": "assistant",          (4.3)
(4.4)   "content": "The sky looks blue because molecules and tiny particles in Earth’s atmosphere scatter sunlight. Short‑wavelength (blue) light is scattered about 10 times more efficiently than red light (Rayleigh scattering), so when sunlight passes through the atmosphere, the blue component is redirected in all directions and reaches our eyes from every part of the sky. At sunrise or sunset the light travels through a longer atmospheric path, scattering away the blue and letting the red hues dominate.",
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

Let's look at what we have in this response (I'll deliberately skip some fields that we'll see in future articles):
 - `(4.1)` the response is in an array, in our case with a single element, but that will change quickly 😉
 - `(4.2)` as for sending, we have a `messages` structure that contains the response
 - `(4.3)` this time the `role` is `assistant`, it represents the model
 - `(4.4)` the model's answer (the generated tokens)
 - `(4.5)` the reasoning used by the model. Indeed, the model we use is a so-called "reasoning" model. In a nutshell: the prompt is split into sub-steps to get what we call the chain of thought. It's more effective, but slower and consumes more tokens. 
 - `(4.6)` we find the consumption in number of tokens for this simple prompt: 
   - the number of tokens sent (your prompt): 79
   - the number of tokens generated (the model's answer): 183 (⚠️ including the reasoning ⚠️)
   - so 262 tokens in total

Step 5️⃣ is quite simply the extraction of the answer to make it more readable.

### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Bash-SimpleChatbot.mov" type="video/quicktime">
</video>


## 📏 Enabling the system prompt

Now that we have the basics of our chatbot, let's add the notion of a system prompt.
We'll use it so we no longer have to repeat "(provide a concise answer)" on every request, while keeping this instruction systematically applied.

To do this, you need to modify step 2️⃣ by adding the system prompt as follows:
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

Which gives:
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
Here we see a new role that lets us add the system prompt: `system`.

### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Bash-SimpleChatbot-SystemPrompt.mov" type="video/quicktime">
</video>

## 📏 Enabling streaming

Streaming mode will let us display the tokens one after another as soon as the LLM has generated them, without waiting for it to finish generating the full answer.
This greatly improves the user experience 🤗.

To enable streaming, it's quite simple, you just add `"stream": true` to the payload.
The most complex change concerns displaying the result.
Indeed, each token arrives on the fly via SSE, so we'll have to modify the code to display them as they come.

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

Step 1️⃣ doesn't change, and the only modification we made in step 2️⃣ is enabling streaming mode.

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
 - `(2.1)` enabling streaming mode (false by default)

Step 3️⃣ changes a little bit due to the reception of each token.

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
This is only an extract of all the generated lines (here the beginning of the reasoning part).
 - `(3.1)` the generated token
 - `(3.2)` indicates that this is only a part of the generated text

### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Bash-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

# ☕️ Java

Now that we have a good idea of how it works without any outside help, let's move on to something more interesting: Java 🤩.

## ✍️ Pure Java

Let's start by simply porting the bash code without any outside help.

### 💬 Send a prompt and display the answer

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

Step 1️⃣ lets us use [JBang](https://www.jbang.dev/); the good news is that we're going to stay very close to what we do with a bash script.
Step 2️⃣, Java syntax aside, is the same thing as step 2️⃣ in Bash, it prepares the payload to send to the endpoint.
```json
{
  "model" : "gpt-oss-120b",
  "messages" : [ {
    "role" : "user",
    "content" : "Why is the sky blue? (provide a concise answer)"
  } ]
}
```

Step 3️⃣ is the same as step 3️⃣ in Bash, calling the API with the payload.
And finally step 4️⃣ retrieves the response and only prints the `content` part of the body.
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
I won't detail the response, it's exactly the same as in bash.

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-SimpleChatbot.mov" type="video/quicktime">
</video>

### 📏 Enabling the system prompt

You'll have guessed it, enabling the system prompt is going to be just like in bash.
Add the system prompt to the payload in step 2️⃣.
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

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-SimpleChatbot-SystemPrompt.mov" type="video/quicktime">
</video>

## 📏 Enabling streaming

I think you've got it by now: here again, for streaming, the only thing that changes is that the code is written in Java and not in Bash.
The logic is the same (adding `"stream": true` to the payload in step 2️⃣), and the payloads and body too.

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

Step 4️⃣ prints the tokens one after another, the received body is the same as in Bash.

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

## 🛠️ With the OpenAI SDK

In this part we're going to see what it looks like using an SDK.
So as not to be too repetitive, I suggest we directly build the final chatbot (streaming + system prompt).
I use OpenAI's [official SDK](https://github.com/openai/openai-java) since OVHcloud's endpoints are compatible with the OpenAI APIs.

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

Step 1️⃣ lets us initialize the client by choosing the model we want to use and setting some high-level parameters (like logs for example).
Step 2️⃣  lets us pass the various prompts and potentially other options we'll see later.
Speaking of logs, if we enable `debug` we can see what is sent:
```json
{"messages":[
  {"content":"provide a concise answer","role":"system"},     (1.1)
  {"content":"Why is the sky blue?","role":"user"}],          (1.2)
  "model":"gpt-oss-120b",                                     (1.3)
  "stream":true                                               (1.4)
}
```

We find what we've already seen:
 - `(1.1)` the system prompt
 - `(1.2)` the user prompt
 - `(1.3)` the model to use
 - `(1.4)` enabling streaming

At this point you may be wondering how streaming got enabled?
It happens at step 3️⃣: when we use the client to call the `createStreaming` method, the streaming flag is implicitly set to `true`.

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-SDK-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

## 🦜 With LangChain4j

We move forward in simplifying our code writing; this time we're going to use a higher-level Framework with [LangChain4j](https://docs.langchain4j.dev/).
It's my favorite Framework in Java for adding AI to an application.
As we'll see, it greatly simplifies the code.
As with the SDK, I'll build the most complete example right from the start of this sub-part.
I'm going to use the [AI Services](https://docs.langchain4j.dev/tutorials/ai-services) mode, which allows for a greater abstraction.

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

What's nice with LangChain4j is that we switch to a declarative / builder mode.
Of course, you can choose not to use the AI Services and have slightly more verbose code, but that you can customize more precisely.

Step 1️⃣ lets us specify how we want to talk to the model.
If I translate into plain English what this interface does:
 - we want to create an assistant (`interface Assistant`)
 - with, as a way to access the model, a `chat` method that takes a string as input (`String userMessage`) representing the user prompt, and that returns the answer as a stream (`TokenStream`)
 - and we set the system prompt thanks to the `@SystemMessage("provide a concise answer")` annotation

Then step 2️⃣ lets us configure the model we want to use.
Since we want to enable streaming mode, we use a `StreamingChatModel` then the `OpenAiStreamingChatModel` builder.
All that's left is to set the URL, the API key and the name of the model to use.
If you enable the logs, you see the detail of what is sent:
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

Step 3️⃣  assembles the definition of the model and of what we want to do with it by creating an assistant thanks to the AI Services.

Finally, step 4️⃣  makes the call to the model in streaming mode and prints the answer token by token. For this we use a `CompletableFuture` that will let us handle the end of the stream.

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-LangChain4j-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>


## ⚡️ With Quarkus

We go up one more notch to reach Frameworks we can call enterprise-grade.
The first will be [Quarkus](https://quarkus.io/) with the [quarkus-langchain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html) extension.

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
We're starting to have more-than-concise code, and yet it still does the same thing.

Step 1️⃣ loads the `application.properties` file into the classpath.
This file contains the configuration of the model used, we no longer do it in the code.
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

 - `(1.1)` denotes the URL to connect to
 - `(1.2)` specifies the model to use
 - `(1.3)` retrieves the API token via an environment variable

Step 2️⃣  configures how we're going to interact with the model through the use of the `AI Services`.
Enabling it is done by using the `@RegisterAiService` annotation.
Streaming is enabled by specifying `Multi<String>` as the return type.

Step 3️⃣ lets us use Quarkus in simple main mode (without launching a server).
Step 4️⃣ injects the service defined in 2️⃣  so we can use it in the main.

Finally step 5️⃣ prints the result of the call.

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-Quarkus-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

## ☘️ With Spring AI

Last implementation, so as not to make anyone jealous, [Spring AI](https://spring.io/projects/spring-ai).
The logic looks a lot like what we saw with Quarkus and LangChain4j, so I won't dwell on it too much.
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

Step 1️⃣ still lets us configure the model in an `application.properties` file.
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
And finally step 2️⃣ lets us configure the call by enabling streaming mode (`stream()`).

#### 📽️ Let's see it in action!
<video width="100%"  controls>
  <source src="Java-SpringAI-SimpleChatbot-SystemPromptStreaming.mov" type="video/quicktime">
</video>

# 🏁 Conclusion

And there we go!
This first article is far too long 😅.
But I had to lay the groundwork, and it also felt right to illustrate it straight away with a simple chatbot example.

For the future, we'll keep trying to add features to our chatbot.
We'll see if we manage to keep all the approaches shown here.

The next step will be to add a bit of memory to our chatbot 🧠.

You can find the full source code in the article's [GitHub repo](https://github.com/philippart-s/java-ai-area-blog).

If you made it this far, thanks for reading! If you spot any typos, feel free to open an [issue or PR](https://github.com/philippart-s/blog) 😊.