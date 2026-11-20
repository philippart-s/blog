---
title: "☕️ Java in the AI Era 🤖 - part 5: function calling"
description: "Enhance 224 to 176. Enhance, stop. ©Rick Deckard"
link: /2026-11-16-java-and-ai-part5-en
image: cover.jpeg
tags:
  - Java
  - AI
  - Code
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-11-16-java-and-ai-part5) 🇫🇷.

## TL;DR
> 🛠️ Fifth article of the series about AI in your Java applications ☕️, this time dedicated to **function calling** (or _tool calling_).  
> 🤷 A model doesn't know data that changes (the capacity of a room, the weather, the status of an order) and can't do anything other than produce text.  
> 🧠 The principle: we describe our functions to the model, it doesn't run them, it **asks** us to call them, then writes its answer from the result.  
> 🔁 In practice, that's two calls to the model, linked by a tool call identifier (`tool_call_id`).  
> ☕️ The same use case is implemented from the lowest level to the highest: Bash, plain Java, the OpenAI SDK, LangChain4j, Quarkus and Spring AI.  
> 🐙 All the examples use OVHcloud's [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) and are available [here](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

A quick recap of the previous episodes: this article is part of a series meant to explain how to integrate AI into the applications we develop in Java ☕️.
After the basics ([part 1](/blog/2026-07-21-java-and-ai-part1-en)), memory ([part 2](/blog/2026-08-03-java-and-ai-part2-en) and [part 3](/blog/2026-08-27-java-and-ai-part3-en)) and RAG ([part 4](/blog/2026-10-28-java-and-ai-part4-en)), let's continue with another essential building block: **function calling**.

# 🤷 But why use function calling?

Let's go back to the fictional conference of part 4, **DevFest Atlantide 2027** 🌊.
This time, we'd like to know how many people the Nautilus room can hold.

Let's ask `gpt-oss-120b`, with nothing else:

```text
💬: What is the capacity of the Nautilus room?

🤖: The exact number of guests the Nautilus Room can hold depends on the venue and the layout
you choose (theater‑style, banquet, classroom, etc.). Here's a typical range for a medium‑size
"Nautilus"‑style meeting space:

| Layout | Approx. Capacity* |
|--------|-------------------|
| Theater (rows of chairs) | 100 – 130 people |
| Banquet (round tables) | 70 – 90 people |
...
```

A nice table, very self-confident, and entirely made up 🤥.
The model doesn't know this room, it can only guess.

We could say: "easy, let's do RAG with the description sheet of each room".
That works for information that doesn't change, but a lot of data is alive: the number of seats left, the weather, the status of an order, the content of a database, ...
And above all, RAG only lets the model **read**: it will never let the model **do** something, like booking a seat or sending an email.

This is where function calling comes in: giving the model the ability to **ask** for functions of our application to be run.

# 🔎 How does function calling work?

First important thing to understand: **the model never runs anything** ⚠️.
It has no access to your code, your network or your database.
All it can do is produce text... and, in the case we're interested in, a structured text that says "I'd like you to call this function with these parameters".

So the flow is the following:
 1. we send the model the question **and** the description of the functions it is allowed to ask for
 2. the model doesn't answer the question: it sends back a function call request (a _tool call_), with the name of the function and its arguments
 3. our application runs the function
 4. we send the whole conversation back to the model, plus the call request and its result
 5. the model finally writes its answer from this result

For the rest of the article, we'll use a `get_room_capacity` function that takes the name of a room and returns its number of seats.

> ℹ️ To be sure the answer really comes from our function and not from the model's imagination, the capacity is a random number between 50 and 500. The model can't guess it 🎲.

## 📝 The description of a function

For the model to know which functions exist, we describe them in the `tools` field of the request, with a [JSON Schema](https://json-schema.org/):

```json
{
  "type": "function",
  "function": {
    "name": "get_room_capacity",
    "description": "Returns the number of seats of a conference room",
    "parameters": {
      "type": "object",
      "properties": {
        "room": { "type": "string", "description": "Name of the room, e.g. Coral" }
      },
      "required": ["room"]
    }
  }
}
```

That's **all** the model knows about the function: its name, what it does and its parameters.
The code itself never leaves your machine.

So the descriptions aren't there just to look nice: it's by reading them that the model decides whether it should call the function and how to fill in its parameters.
A vague description, and the model won't call the function, or will call it badly 😅.

## 🛠️ The call request

When the model decides to use a function, its response doesn't contain any text (`content`), but a `tool_calls` array:

```json
"message": {
  "role": "assistant",
  "reasoning": "The user asks: \"What is the capacity of the Nautilus room?\" We need to get the capacity using the get_room_capacity function. We'll call it with room: \"Nautilus\".",
  "tool_calls": [
    {
      "id": "chatcmpl-tool-99db5c5b23562aeb",
      "type": "function",
      "function": {
        "name": "get_room_capacity",
        "arguments": "{\"room\": \"Nautilus\"}"
      }
    }
  ]
},
"finish_reason": "tool_calls"
```

A few things to note:
 - the `finish_reason` is `tool_calls` and not `stop`: the model isn't done, it's waiting for something from us
 - the arguments are a `JSON` document... inside a string, so they'll have to be parsed
 - the `id` will link the result to the call request during the second call
 - the `reasoning` field is specific to `gpt-oss-120b`, which is a reasoning model: we can see the model deciding to call the function 🧐

## 🔁 Sending the result back

Once the function has run, we call the model again with the whole conversation, to which we add two messages:
 - the assistant message containing the call request, as is
 - a message with the `tool` role, which contains the result and the identifier of the call (`tool_call_id`)

```json
{
  "role": "tool",
  "tool_call_id": "chatcmpl-tool-99db5c5b23562aeb",
  "content": "{\"room\":\"Nautilus\",\"capacity\":458}"
}
```

You'll recognize the principle from [part 2](/blog/2026-08-03-java-and-ai-part2-en): the model is _stateless_, so we have to send it the whole history for it to understand where this result comes from.

> 💸 So function calling means at least **two calls** to the model for a single question, and the description of the functions is sent each time. With dozens of functions, it ends up weighing on the bill and on the context window.

# 🖥️ The basics: Bash

As in the first articles, we start with `curl` and `jq`, to clearly see what goes over the wire.

{|
```bash
#!/usr/bin/env bash
#
# Function calling example calling OVHcloud AI Endpoints (gpt-oss-120b).
#
# The request describes a get_room_capacity function. The model does not answer
# the question: it asks for the function to be called. The script runs it, sends
# the result back, and the model then writes the final answer. Needs curl and jq.
#
# Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
# Docs: https://platform.openai.com/docs/guides/function-calling

set -a
source "$(dirname "$0")/../.env"
set +a

ENDPOINT="https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions"
MODEL="gpt-oss-120b"
QUESTION="What is the capacity of the Nautilus room?"

# The function the model is allowed to ask for. A random number stands in for a
# real lookup: the model cannot guess it, so the answer has to come from here.
get_room_capacity() {
  local capacity=$(( RANDOM % 451 + 50 ))

  # Printed on stderr: stdout is the result handed back to the model.
  echo "🐞 get_room_capacity called for room \"$1\", returning capacity $capacity" >&2
  echo >&2

  jq -nc --arg room "$1" --argjson capacity "$capacity" \
    '{ room: $room, capacity: $capacity }'
}

# The description of the function, as a JSON schema. This is all the model knows
# about it: the code above never leaves this machine.
TOOLS=$(jq -n '[
  {
    type: "function",
    function: {
      name: "get_room_capacity",
      description: "Returns the number of seats of a conference room",
      parameters: {
        type: "object",
        properties: {
          room: { type: "string", description: "Name of the room, e.g. Coral" }
        },
        required: ["room"]
      }
    }
  }
]')

MESSAGES=$(jq -n --arg content "$QUESTION" '[
  { role: "system", content: "provide a concise answer" },
  { role: "user", content: $content }
]')

# Sends the conversation, along with the functions available, and prints the response.
chat() {
  jq -n --arg model "$MODEL" --argjson messages "$MESSAGES" --argjson tools "$TOOLS" '{
    model: $model,
    messages: $messages,
    tools: $tools
  }' | curl -s "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${OVH_AI_ENDPOINTS_ACCESS_TOKEN}" \
    -d @-
}

# 1) First call: the question and the description of the function.
echo "===== ⬆️ JSON REQUEST (question + tools sent to the model) ⬆️ ====="
echo "$MESSAGES" | jq --argjson tools "$TOOLS" '{ messages: ., tools: $tools }'
echo

RESPONSE=$(chat)

echo "===== ⬇️ JSON RESPONSE ⬇️ ====="
echo "$RESPONSE" | jq .
echo

# 2) The model answered with a tool call instead of a text: a function name and
#    its arguments, themselves a JSON document carried in a string.
ASSISTANT_MESSAGE=$(echo "$RESPONSE" | jq '.choices[0].message')
TOOL_CALL=$(echo "$ASSISTANT_MESSAGE" | jq -c '.tool_calls[0] // empty')

# Asking for a function is a choice of the model, not an obligation: without a
# tool call its message already holds the answer and there is nothing to run.
if [ -z "$TOOL_CALL" ]; then
  echo "===== 🤖 ANSWER 🤖 ====="
  echo "$ASSISTANT_MESSAGE" | jq -r '.content'
  exit 0
fi

TOOL_CALL_ID=$(echo "$TOOL_CALL" | jq -r '.id')
FUNCTION_NAME=$(echo "$TOOL_CALL" | jq -r '.function.name')
ARGUMENTS=$(echo "$TOOL_CALL" | jq -r '.function.arguments')

echo "===== 🛠️ TOOL CALL (requested by the model) 🛠️ ====="
echo "$FUNCTION_NAME $ARGUMENTS"
echo

# 3) Run the function. The model only named it, calling it is up to the script,
#    which dispatches on the name instead of trusting it to be the right one.
if [ "$FUNCTION_NAME" != "get_room_capacity" ]; then
  echo "Unexpected function requested by the model: $FUNCTION_NAME" >&2
  exit 1
fi

ROOM=$(echo "$ARGUMENTS" | jq -r '.room')
RESULT=$(get_room_capacity "$ROOM")

echo "===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ ====="
echo "$RESULT"
echo

# 4) Second call: the conversation so far, plus the tool call of the model and
#    its result, tied together by the tool call id.
MESSAGES=$(echo "$MESSAGES" | jq \
  --argjson assistant "$ASSISTANT_MESSAGE" \
  --arg id "$TOOL_CALL_ID" \
  --arg result "$RESULT" \
  '. + [ $assistant, { role: "tool", tool_call_id: $id, content: $result } ]')

echo "===== ⬆️ JSON REQUEST (tool call + tool result sent to the model) ⬆️ ====="
echo "$MESSAGES" | jq '{ messages: . }'
echo

RESPONSE=$(chat)

echo "===== 🤖 ANSWER 🤖 ====="
echo "$RESPONSE" | jq -r '.choices[0].message.content'
```
|}

 - lines 22 to 31: our function, which returns a random number of seats. It also prints a 🐞 trace on the error output, to clearly see when it is called: the standard output is the result sent back to the model
 - lines 35 to 50: the description of the function, the one seen above
 - lines 58 to 67: the call to the model, which sends each time the conversation **and** the list of functions (`tools`)
 - line 74: the first call
 - lines 82 and 83: we get the call request from the response
 - lines 87 to 91: if the model didn't ask for a call, its answer is already there and there's nothing to run. Calling a function is a choice of the model, not an obligation
 - lines 93 to 95: the identifier of the request, the name of the function and its arguments
 - lines 103 to 106: we check the name of the requested function rather than trusting the model
 - lines 108 and 109: we parse the arguments and run the function, it's the script that calls the function, not the model 😉
 - lines 117 to 121: we add the assistant message and the result to the conversation, linked by the `tool_call_id`
 - line 127: the second call, which finally returns the answer

And here is the result:

```text
===== ⬆️ JSON REQUEST (question + tools sent to the model) ⬆️ =====
{
  "messages": [
    {
      "role": "system",
      "content": "provide a concise answer"
    },
    {
      "role": "user",
      "content": "What is the capacity of the Nautilus room?"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_room_capacity",
        "description": "Returns the number of seats of a conference room",
        "parameters": {
          "type": "object",
          "properties": {
            "room": {
              "type": "string",
              "description": "Name of the room, e.g. Coral"
            }
          },
          "required": [
            "room"
          ]
        }
      }
    }
  ]
}

===== ⬇️ JSON RESPONSE ⬇️ =====
{
  "id": "chatcmpl-a009df7c2be92bb2",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "reasoning": "The user asks: \"What is the capacity of the Nautilus room?\" We need to get the capacity. Use get_room_capacity with room \"Nautilus\". Let's call the function.",
        "tool_calls": [
          {
            "id": "chatcmpl-tool-9cca7b5467f74f3b",
            "type": "function",
            "function": {
              "name": "get_room_capacity",
              "arguments": "{\"room\": \"Nautilus\"}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ],
  "created": 1794988575,
  "model": "gpt-oss-120b",
  "object": "chat.completion",
  "usage": {
    "prompt_tokens": 149,
    "completion_tokens": 69,
    "total_tokens": 218,
    "completion_tokens_details": {
      "reasoning_tokens": 49
    }
  }
}

===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====
get_room_capacity {"room": "Nautilus"}

🐞 get_room_capacity called for room "Nautilus", returning capacity 430

===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====
{"room":"Nautilus","capacity":430}

===== ⬆️ JSON REQUEST (tool call + tool result sent to the model) ⬆️ =====
{
  "messages": [
    {
      "role": "system",
      "content": "provide a concise answer"
    },
    {
      "role": "user",
      "content": "What is the capacity of the Nautilus room?"
    },
    {
      "role": "assistant",
      "reasoning": "The user asks: \"What is the capacity of the Nautilus room?\" We need to get the capacity. Use get_room_capacity with room \"Nautilus\". Let's call the function.",
      "tool_calls": [
        {
          "id": "chatcmpl-tool-9cca7b5467f74f3b",
          "type": "function",
          "function": {
            "name": "get_room_capacity",
            "arguments": "{\"room\": \"Nautilus\"}"
          }
        }
      ]
    },
    {
      "role": "tool",
      "tool_call_id": "chatcmpl-tool-9cca7b5467f74f3b",
      "content": "{\"room\":\"Nautilus\",\"capacity\":430}"
    }
  ]
}

===== 🤖 ANSWER 🤖 =====
The Nautilus room can accommodate **430** people.
```

430 seats, drawn at random by our function, and used as is by the model 🎉.

> ⚠️ To keep the example readable, the script only handles the first call request. In real life, the model can ask for several at once, or ask for another one after seeing the result of the first. You then have to loop until you get a `finish_reason` set to `stop`.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/00_bash/00.05_FunctionCalling.sh) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="bash-function-calling.mov" type="video/quicktime">
</video>

# ☕️ Java

## ✍️ Plain Java

Let's move on to the Java version without any AI library, with the JDK's `HttpClient` and Jackson.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.22.4
//
// Function calling example calling OVHcloud AI Endpoints (gpt-oss-120b),
// with the JDK HttpClient and Jackson.
//
// The request describes a get_room_capacity function. The model does not answer
// the question: it asks for the function to be called. The program runs it, sends
// the result back, and the model then writes the final answer.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://platform.openai.com/docs/guides/function-calling

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final String ENDPOINT = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions";
final String MODEL = "gpt-oss-120b";

// The token is read from OVH_AI_ENDPOINTS_ACCESS_TOKEN, exported by run.sh from .env.
final String TOKEN = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

final ObjectMapper MAPPER = new ObjectMapper();
final HttpClient CLIENT = HttpClient.newHttpClient();

// The function the model is allowed to ask for. A random number stands in for a
// real lookup: the model cannot guess it, so the answer has to come from here.
String getRoomCapacity(String room) {
    var capacity = ThreadLocalRandom.current().nextInt(50, 501);

    IO.println("🐞 getRoomCapacity called for room \"" + room + "\", returning capacity " + capacity);
    IO.println();

    return MAPPER.createObjectNode()
            .put("room", room)
            .put("capacity", capacity)
            .toString();
}

void main() throws Exception {
    var question = "What is the capacity of the Nautilus room?";

    // The description of the function, as a JSON schema. This is all the model
    // knows about it: the Java code above never leaves this machine.
    var tools = MAPPER.readTree("""
            [
              {
                "type": "function",
                "function": {
                  "name": "get_room_capacity",
                  "description": "Returns the number of seats of a conference room",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "room": { "type": "string", "description": "Name of the room, e.g. Coral" }
                    },
                    "required": ["room"]
                  }
                }
              }
            ]""");

    // The same body is sent twice: only its messages array grows in between.
    var body = MAPPER.createObjectNode();
    body.put("model", MODEL);
    var messages = body.putArray("messages");
    messages.addObject()
            .put("role", "system")
            .put("content", "provide a concise answer");
    messages.addObject()
            .put("role", "user")
            .put("content", question);
    body.set("tools", tools);

    // 1) First call: the question and the description of the function.
    IO.println("===== ⬆️ JSON REQUEST (question + tools sent to the model) ⬆️ =====");
    IO.println(body.toPrettyString());
    IO.println();

    JsonNode response = post(body.toString());

    IO.println("===== ⬇️ JSON RESPONSE ⬇️ =====");
    IO.println(response.toPrettyString());
    IO.println();

    // 2) The model answered with a tool call instead of a text: a function name and
    //    its arguments, themselves a JSON document carried in a string.
    JsonNode assistantMessage = response.at("/choices/0/message");
    JsonNode toolCall = assistantMessage.at("/tool_calls/0");

    // Asking for a function is a choice of the model, not an obligation: without a
    // tool call its message already holds the answer and there is nothing to run.
    if (toolCall.isMissingNode()) {
        IO.println("===== 🤖 ANSWER 🤖 =====");
        IO.println(assistantMessage.at("/content").asText());
        return;
    }

    var functionName = toolCall.at("/function/name").asText();
    var arguments = toolCall.at("/function/arguments").asText();

    IO.println("===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====");
    IO.println(functionName + " " + arguments);
    IO.println();

    // 3) Run the function. The model only named it, calling it is up to the program,
    //    which dispatches on the name instead of trusting it to be the right one.
    if (!"get_room_capacity".equals(functionName)) {
        IO.println("Unexpected function requested by the model: " + functionName);
        return;
    }

    var room = MAPPER.readTree(arguments).get("room").asText();
    var result = getRoomCapacity(room);

    IO.println("===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====");
    IO.println(result);
    IO.println();

    // 4) Second call: the conversation so far, plus the tool call of the model and
    //    its result, tied together by the tool call id.
    messages.add(assistantMessage);
    messages.addObject()
            .put("role", "tool")
            .put("tool_call_id", toolCall.get("id").asText())
            .put("content", result);

    IO.println("===== ⬆️ JSON REQUEST (tool call + tool result sent to the model) ⬆️ =====");
    IO.println(body.toPrettyString());
    IO.println();

    response = post(body.toString());

    IO.println("===== 🤖 ANSWER 🤖 =====");
    IO.println(response.at("/choices/0/message/content").asText());
}

JsonNode post(String body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + TOKEN)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return MAPPER.readTree(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body());
}
```
|}

 - lines 33 to 43: our function, a simple Java method, with its 🐞 trace
 - lines 50 to 66: the description of the function, written by hand in `JSON`
 - line 78: the functions are added to the request body, which will be sent twice
 - line 85: the first call
 - lines 93 to 105: reading the call request from the response, with the case where the model answers directly (lines 98 to 102)
 - lines 113 to 116: checking the name of the requested function
 - lines 118 and 119: parsing the arguments and running the function
 - lines 127 to 131: adding the assistant message and the result to the conversation
 - line 137: the second call

It's exactly the same thing as in Bash, with the same problem: nothing links the `getRoomCapacity` method and its `JSON` description.
If we add a parameter to one without changing the other, we'll only find out at runtime 💥.

At runtime, we get the same flow:

```text
===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====
get_room_capacity {"room": "Nautilus"}

🐞 getRoomCapacity called for room "Nautilus", returning capacity 367

===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====
{"room":"Nautilus","capacity":367}

...

===== 🤖 ANSWER 🤖 =====
The Nautilus room can accommodate **367 people**.
```

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_06_FunctionCalling.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-function-calling.mov" type="video/quicktime">
</video>

## 🛠️ With the OpenAI SDK

The OpenAI SDK brings a first real simplification: the description of the function is generated from a Java class.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS com.openai:openai-java:4.94.0
//
// Function calling example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through the official OpenAI Java SDK (https://github.com/openai/openai-java).
//
// The request describes a get_room_capacity function. The model does not answer
// the question: it asks for the function to be called. The program runs it, sends
// the result back, and the model then writes the final answer.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://github.com/openai/openai-java#function-calling-with-json-schemas

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;

// The function the model is allowed to ask for. The record is its description
// (name, purpose, parameters) and call() is its code. A random number stands in
// for a real lookup: the model cannot guess it, so the answer has to come from here.
@JsonTypeName("get_room_capacity")
@JsonClassDescription("Returns the number of seats of a conference room")
record GetRoomCapacity(@JsonPropertyDescription("Name of the room, e.g. Coral") String room) {
    RoomCapacity call() {
        var capacity = ThreadLocalRandom.current().nextInt(50, 501);

        IO.println("🐞 GetRoomCapacity called for room \"" + room + "\", returning capacity " + capacity);
        IO.println();

        return new RoomCapacity(room, capacity);
    }
}

record RoomCapacity(String room, int capacity) {}

void main() {
    // baseUrl must include the /v1 suffix. The token is read from
    // OVH_AI_ENDPOINTS_ACCESS_TOKEN, exported by run.sh from .env.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    var question = "What is the capacity of the Nautilus room?";

    OpenAIClient client = OpenAIOkHttpClient.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            //.logLevel(LogLevel.DEBUG)
            .build();

    // The same builder serves both calls: only its messages grow in between.
    var paramsBuilder = ChatCompletionCreateParams.builder()
            .model(model)
            .addSystemMessage("provide a concise answer")
            .addUserMessage(question)
            // The SDK derives the JSON schema of the function from the record.
            .addTool(GetRoomCapacity.class);

    // 1) First call: the question and the description of the function.
    var params = paramsBuilder.build();
    IO.println("===== ⬆️ REQUEST (question + tools sent to the model) ⬆️ =====");
    params.messages().forEach(IO::println);
    params.tools().orElseThrow().forEach(IO::println);
    IO.println();

    ChatCompletion response = client.chat().completions().create(params);

    IO.println("===== ⬇️ RESPONSE (SDK object) ⬇️ =====");
    IO.println(response.toString());
    IO.println();

    // 2) The model answered with a tool call instead of a text: a function name and
    //    its arguments, themselves a JSON document carried in a string.
    ChatCompletionMessage assistantMessage = response.choices().getFirst().message();
    var toolCalls = assistantMessage.toolCalls().orElse(List.of());

    // Asking for a function is a choice of the model, not an obligation: without a
    // tool call its message already holds the answer and there is nothing to run.
    if (toolCalls.isEmpty()) {
        IO.println("===== 🤖 ANSWER 🤖 =====");
        IO.println(assistantMessage.content().orElse(""));
        return;
    }

    var toolCall = toolCalls.getFirst().asFunction();

    IO.println("===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====");
    IO.println(toolCall.function().name() + " " + toolCall.function().arguments());
    IO.println();

    // 3) Run the function. The model only named it, calling it is up to the program,
    //    which dispatches on the name instead of trusting it to be the right one.
    //    arguments(Class) parses the JSON arguments into the record.
    if (!"get_room_capacity".equals(toolCall.function().name())) {
        IO.println("Unexpected function requested by the model: " + toolCall.function().name());
        return;
    }

    var result = toolCall.function().arguments(GetRoomCapacity.class).call();

    var toolMessage = ChatCompletionToolMessageParam.builder()
            .toolCallId(toolCall.id())
            .contentAsJson(result)
            .build();

    IO.println("===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====");
    IO.println(toolMessage.content().asText());
    IO.println();

    // 4) Second call: the conversation so far, plus the tool call of the model and
    //    its result, tied together by the tool call id.
    paramsBuilder
            .addMessage(assistantMessage)
            .addMessage(toolMessage);

    params = paramsBuilder.build();
    IO.println("===== ⬆️ REQUEST (tool call + tool result sent to the model) ⬆️ =====");
    params.messages().forEach(IO::println);
    IO.println();

    response = client.chat().completions().create(params);

    IO.println("===== 🤖 ANSWER 🤖 =====");
    response.choices().stream()
            .flatMap(choice -> choice.message().content().stream())
            .forEach(IO::println);
}
```
|}

 - lines 28 to 39: the function is a `record`: its Jackson annotations give the name (`@JsonTypeName`), the description (`@JsonClassDescription`) and the description of the parameter (`@JsonPropertyDescription`), its `call()` method contains the code (and the 🐞 trace)
 - line 64: `addTool` generates the JSON Schema of the function from the `record`
 - line 73: the first call
 - lines 81 to 92: reading the call request, with the case where the model answers directly (lines 86 to 90)
 - lines 101 to 104: checking the name of the requested function
 - line 106: `arguments(GetRoomCapacity.class)` directly turns the `JSON` arguments into a `record`, no need to parse them by hand anymore
 - lines 108 to 111: the `tool` message, with the result serialized to `JSON` by the SDK
 - lines 119 to 121: adding the two messages to the conversation
 - line 128: the second call

The description and the code of the function are now in the same place, and the compiler makes sure they stay consistent 😎.
On the other hand, the orchestration is still up to us: we're still the ones reading the call request, running the function and calling the model again.

Looking at the description that is sent, we can also see that the SDK generates a slightly stricter schema than ours (`strict=true`, `additionalProperties=false`):

{|
```text
ChatCompletionTool{function=ChatCompletionFunctionTool{function=FunctionDefinition{name=get_room_capacity, description=Returns the number of seats of a conference room, parameters={$schema=https://json-schema.org/draft/2020-12/schema, type=object, properties={room={type=string, description=Name of the room, e.g. Coral}}, required=[room], additionalProperties=false}, strict=true, additionalProperties={}}, type=function, additionalProperties={}}}
```
|}

And at the end:

```text
===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====
get_room_capacity {"room": "Nautilus"}

🐞 GetRoomCapacity called for room "Nautilus", returning capacity 272

===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====
{"room":"Nautilus","capacity":272}

...

===== 🤖 ANSWER 🤖 =====
The Nautilus room can accommodate **272 people**.
```

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_06_FunctionCalling.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="java-sdk-function-calling.mov" type="video/quicktime">
</video>

## 🦜 With LangChain4j

With [LangChain4j](https://docs.langchain4j.dev/tutorials/tools), we take a big step: the framework handles the whole orchestration.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
//DEPS dev.langchain4j:langchain4j:1.22.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.22.0
//DEPS org.slf4j:slf4j-nop:2.0.20
//
// Function calling example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through LangChain4j AI Services (https://github.com/langchain4j/langchain4j).
//
// A method annotated with @Tool is registered on the AI Service. The model does
// not answer the question: it asks for the method to be called. LangChain4j runs
// it, sends the result back, and the model then writes the final answer.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://docs.langchain4j.dev/tutorials/tools

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

interface Assistant {
    @SystemMessage("provide a concise answer")
    String chat(String userMessage);
}

record RoomCapacity(String room, int capacity) {}

// The function the model is allowed to ask for. The annotations are its
// description, sent to the model as a JSON schema. A random number stands in for a
// real lookup: the model cannot guess it, so the answer has to come from here.
class RoomTools {
    @Tool("Returns the number of seats of a conference room")
    RoomCapacity getRoomCapacity(@P("Name of the room, e.g. Coral") String room) {
        var capacity = ThreadLocalRandom.current().nextInt(50, 501);

        IO.println("🐞 getRoomCapacity called for room \"" + room + "\", returning capacity " + capacity);
        IO.println();

        return new RoomCapacity(room, capacity);
    }
}

void main() {
    // baseUrl must include the /v1 suffix. The token is read from
    // OVH_AI_ENDPOINTS_ACCESS_TOKEN, exported by run.sh from .env.
    final String baseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1";
    final String model = "gpt-oss-120b";
    final String token = System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN");

    var question = "What is the capacity of the Nautilus room?";

    ChatModel chatModel = OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(token)
            .modelName(model)
            // .logRequests(true)
            // .logResponses(true)
            .build();

    // The AI Service calls the model, runs the tool the model asks for and calls
    // the model again with the result. The two callbacks only show it happening.
    Assistant assistant = AiServices.builder(Assistant.class)
            .chatModel(chatModel)
            .tools(new RoomTools())
            .beforeToolExecution(before -> {
                IO.println("===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====");
                IO.println(before.request().name() + " " + before.request().arguments());
                IO.println();
            })
            .afterToolExecution(after -> {
                IO.println("===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====");
                IO.println(after.result());
                IO.println();
            })
            .build();

    IO.println("===== ❓ QUESTION ❓ =====");
    IO.println(question);
    IO.println();

    // The tool banners are printed during this call, hence the answer kept aside.
    var answer = assistant.chat(question);

    IO.println("===== 🤖 ANSWER 🤖 =====");
    IO.println(answer);
}
```
|}

 - lines 34 to 44: the function is a Java method annotated with `@Tool` (its description) and `@P` (the description of the parameter), with its 🐞 trace
 - line 67: we plug the tools into the AI Service, like the memory in part 2 or the _retriever_ in part 4
 - lines 68 to 77: two _callbacks_, called before and after the tool runs, which are only there to show what's happening
 - line 85: the call to the assistant, which only takes the question

And that's it 🤯.
During the call to `chat`, LangChain4j sends the question and the description of the tool, detects the call request, runs the method, sends the result back to the model and gives us the final answer.
And unlike our previous examples, it loops as long as the model asks for tools.

```text
===== ❓ QUESTION ❓ =====
What is the capacity of the Nautilus room?

===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====
getRoomCapacity {"room": "Nautilus"}

🐞 getRoomCapacity called for room "Nautilus", returning capacity 95

===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====
{"room":"Nautilus","capacity":95}

===== 🤖 ANSWER 🤖 =====
The Nautilus room can accommodate **95 people**.
```

Note that the function is now called `getRoomCapacity`: by default, LangChain4j uses the name of the Java method as the function name.

> ℹ️ To see the requests actually sent to the model, just uncomment `.logRequests(true)` and `.logResponses(true)` (and replace `slf4j-nop` with `slf4j-simple`): you'll find the two calls and the `tool` message.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_07_FunctionCalling.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="langchain4j-function-calling.mov" type="video/quicktime">
</video>

## ⚡️ With Quarkus

With Quarkus, the tool becomes a CDI bean, declared on the AI Service.

{|
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 26+
// jboss-threads needs java.lang opened on Java 24+ (thread-local reset capability).
//JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED
//DEPS io.quarkus.platform:quarkus-bom:3.33.4.1@pom
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai:1.14.2
//
// Function calling example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through Quarkus + quarkus-langchain4j
// (https://github.com/quarkiverse/quarkus-langchain4j).
//
// Single-file Quarkus application run by JBang. A bean method annotated with
// @Tool is declared on the AI service. The model does not answer the question:
// it asks for the method to be called. The extension runs it, sends the result
// back, and the model then writes the final answer.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html

//FILES application.properties

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.concurrent.ThreadLocalRandom;

record RoomCapacity(String room, int capacity) {}

// The function the model is allowed to ask for. The annotations are its
// description, sent to the model as a JSON schema. A random number stands in for a
// real lookup: the model cannot guess it, so the answer has to come from here.
@ApplicationScoped
class RoomTools {
    @Tool("Returns the number of seats of a conference room")
    RoomCapacity getRoomCapacity(@P("Name of the room, e.g. Coral") String room) {
        var capacity = ThreadLocalRandom.current().nextInt(50, 501);

        IO.println("🐞 getRoomCapacity called for room \"" + room + "\", returning capacity " + capacity);
        IO.println();

        return new RoomCapacity(room, capacity);
    }
}

// The AI service calls the model, runs the tool the model asks for and calls the
// model again with the result.
@RegisterAiService(tools = RoomTools.class)
interface Assistant {
    @SystemMessage("provide a concise answer")
    String chat(String userMessage);
}

@QuarkusMain
public class _04_06_FunctionCalling implements QuarkusApplication {

    @Inject
    Assistant assistant;

    // @ActivateRequestContext makes the CDI request scope available for the call.
    @Override
    @ActivateRequestContext
    public int run(String... args) {
        var question = "What is the capacity of the Nautilus room?";

        IO.println("===== ❓ QUESTION ❓ =====");
        IO.println(question);
        IO.println();

        // The tool banners are printed during this call, hence the answer kept aside.
        var answer = assistant.chat(question);

        IO.println("===== 🤖 ANSWER 🤖 =====");
        IO.println(answer);
        return 0;
    }

    // Fired by the extension once a tool has run: it only shows what happened.
    void onToolExecuted(@Observes ToolExecutedEvent event) {
        IO.println("===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====");
        IO.println(event.request().name() + " " + event.request().arguments());
        IO.println();

        IO.println("===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====");
        IO.println(event.resultText());
        IO.println();
    }
}
```
|}

 - lines 41 to 52: the tool, the same as with LangChain4j, but as a CDI bean (`@ApplicationScoped`)
 - line 56: the tools are declared directly in the `@RegisterAiService` annotation
 - line 79: the call, with only the question
 - lines 87 to 95: a CDI _observer_ of the `ToolExecutedEvent` event, fired after each tool execution, which is only there for display

The configuration is the one from the previous articles (`application.properties`), there's nothing to add for function calling.

```text
===== ❓ QUESTION ❓ =====
What is the capacity of the Nautilus room?

🐞 getRoomCapacity called for room "Nautilus", returning capacity 391

===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====
getRoomCapacity {"room": "Nautilus"}

===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====
{
  "room" : "Nautilus",
  "capacity" : 391
}

===== 🤖 ANSWER 🤖 =====
The Nautilus room can accommodate **391** people.
```

You'll notice that the 🐞 trace appears **before** the call request: the `ToolExecutedEvent` event (we'll see there are other ways to do things before the tool is called) is only fired once the tool has run, so the _observer_ displays the request and the result after the fact.

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_06_FunctionCalling.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="quarkus-function-calling.mov" type="video/quicktime">
</video>

## ☘️ With Spring AI

To finish, let's see how [Spring AI](https://docs.spring.io/spring-ai/reference/api/tools.html) handles function calling.

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
// Servlet API present only so Boot's web-servlet config classes resolve during
// bean introspection; the app stays non-web (spring.main.web-application-type=none).
//DEPS jakarta.servlet:jakarta.servlet-api:6.1.0
//
// Function calling example calling OVHcloud AI Endpoints (gpt-oss-120b)
// through Spring AI (https://docs.spring.io/spring-ai/reference/).
//
// Single-file Spring Boot application run by JBang. A method annotated with
// @Tool is handed to the ChatClient. The model does not answer the question: it
// asks for the method to be called. Spring AI runs it, sends the result back,
// and the model then writes the final answer.
//
// Docs: https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/gpt-oss-120b/
// Docs: https://docs.spring.io/spring-ai/reference/api/tools.html

//FILES application.properties

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ThreadLocalRandom;

record RoomCapacity(String room, int capacity) {}

// The function the model is allowed to ask for. The annotations are its
// description, sent to the model as a JSON schema. A random number stands in for a
// real lookup: the model cannot guess it, so the answer has to come from here.
class RoomTools {
    @Tool(description = "Returns the number of seats of a conference room")
    RoomCapacity getRoomCapacity(@ToolParam(description = "Name of the room, e.g. Coral") String room) {
        var capacity = ThreadLocalRandom.current().nextInt(50, 501);

        IO.println("🐞 getRoomCapacity called for room \"" + room + "\", returning capacity " + capacity);
        IO.println();

        return new RoomCapacity(room, capacity);
    }
}

// Wraps a tool to print what goes in and what comes out. Not needed for the tool
// to work: it only shows the call happening.
record LoggedTool(ToolCallback tool) implements ToolCallback {
    @Override
    public ToolDefinition getToolDefinition() {
        return tool.getToolDefinition();
    }

    @Override
    public String call(String arguments) {
        IO.println("===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====");
        IO.println(getToolDefinition().name() + " " + arguments);
        IO.println();

        var result = tool.call(arguments);

        IO.println("===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====");
        IO.println(result);
        IO.println();
        return result;
    }
}

// NOT @SpringBootApplication: this file lives in the default package, so its
// @ComponentScan would scan the whole classpath and break Spring's own config.
@SpringBootConfiguration
@EnableAutoConfiguration
public class _05_06_FunctionCalling {

    public static void main(String[] args) {
        SpringApplication.run(_05_06_FunctionCalling.class, args);
    }

    @Bean
    CommandLineRunner run(ChatClient.Builder builder) {
        return args -> {
            var question = "What is the capacity of the Nautilus room?";

            // ToolCallbacks.from() turns the @Tool methods into callbacks. Without the
            // logging wrapper, .defaultTools(new RoomTools()) does the same in one line.
            var chatClient = builder
                    .defaultSystem("provide a concise answer")
                    .defaultTools(new LoggedTool(ToolCallbacks.from(new RoomTools())[0]))
                    .build();

            IO.println("===== ❓ QUESTION ❓ =====");
            IO.println(question);
            IO.println();

            // The ChatClient calls the model, runs the tool the model asks for and
            // calls the model again with the result. The tool banners are printed
            // during this call, hence the answer kept aside.
            var answer = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();

            IO.println("===== 🤖 ANSWER 🤖 =====");
            IO.println(answer);
        };
    }
}
```
|}

 - lines 45 to 55: the tool, with Spring AI's `@Tool` and `@ToolParam` annotations (same principle, different package 😉), and still the 🐞 trace
 - lines 59 to 78: a `ToolCallback` that wraps the tool to display the call and the result, it's not needed for it to work
 - line 99: we plug the tool into the `ChatClient`, without the display a simple `.defaultTools(new RoomTools())` would be enough
 - lines 109 to 112: the call, with only the question

As with LangChain4j, the framework orchestrates everything during the call: the `ChatClient` detects the call request, runs the tool (through its `ToolCallingManager`), sends the result back to the model and loops until it gets the final answer.

```text
===== ❓ QUESTION ❓ =====
What is the capacity of the Nautilus room?

===== 🛠️ TOOL CALL (requested by the model) 🛠️ =====
getRoomCapacity {"room": "Nautilus"}

🐞 getRoomCapacity called for room "Nautilus", returning capacity 446

===== 🛠️ TOOL RESULT (sent back to the model) 🛠️ =====
{"room":"Nautilus","capacity":446}

===== 🤖 ANSWER 🤖 =====
The Nautilus room can accommodate **446** people.
```

To see the full source code of this example, it's [here](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_06_FunctionCalling.java) 📜.

#### 📽️ Let's see it in action!
<video controls class="video-centered">
  <source src="spring-function-calling.mov" type="video/quicktime">
</video>

# 🔭 What's next?

Our example is deliberately minimalist: one function, one parameter, one call.
In a real application, a few points deserve your attention:
 - **several tools**: you can declare as many as you want, it's the model that chooses which one to call based on their description, hence the importance of writing them well
 - **several calls**: the model can ask for several functions at once, or chain calls (get the list of rooms, then the capacity of each one); the frameworks handle this loop for you, our Bash, Java and SDK examples don't
 - **security** ⚠️: it's the arguments generated by the model that are passed to your code, so you have to treat them like any user input (validation, access rights, ...), especially if the function modifies data
 - **cost** 💸: each declared tool is sent with every call, and each call request adds a round trip with the model

You'll find more details about function calling here:
- [LangChain4j](https://docs.langchain4j.dev/tutorials/tools)
- [Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html)
- [Spring AI](https://docs.spring.io/spring-ai/reference/api/tools.html)

And if we push the principle a bit further:
 - letting the model chain tool calls on its own until it reaches a goal is the principle of **agents** 🤖
 - sharing tools between applications rather than coding them in each one is the principle of [MCP](https://modelcontextprotocol.io/) (_Model Context Protocol_) 🔌

These will be the topics of upcoming articles 😆.

# 🏁 Conclusion

That's it for this introduction to function calling.
Behind the name, the mechanism is fairly simple: describe functions to the model, let it ask for them to be run, and send it back the result.
The model never does anything but produce text, it's your application that stays in control of what is actually run.

It's also the basic building block of everything that makes AI the talk of the town right now: agents, MCP, coding assistants...
But that's another story.

All the source code is available in the GitHub repository [java-ai-area-blog](https://github.com/philippart-s/java-ai-area-blog).

If you made it this far, thanks for reading! If you spot any typos, feel free to open an [issue or PR](https://github.com/philippart-s/blog) 😊.
