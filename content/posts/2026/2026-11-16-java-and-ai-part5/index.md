---
title: "☕️ Java à l'ère de l'IA 🤖 - partie 5 : le function calling"
description: "Enhance 224 to 176. Enhance, stop. ©Rick Deckard"
link: /2026-11-16-java-and-ai-part5
image: cover.jpeg
tags:
  - Java
  - IA
  - Code
author: wildagsx
---

🏴󠁧󠁢󠁥󠁮󠁧󠁿 You can find the English version of this article [here]({site.url}2026-11-16-java-and-ai-part5-en) 🏴󠁧󠁢󠁥󠁮󠁧󠁿.

## TL;DR
> 🛠️ Cinquième article de la série sur l'IA dans vos applications Java ☕️, cette fois consacré au **function calling** (ou _tool calling_).  
> 🤷 Un modèle ne connaît pas les données qui changent (la capacité d'une salle, la météo, l'état d'une commande) et ne sait rien faire d'autre que produire du texte.  
> 🧠 Le principe : on décrit nos fonctions au modèle, il ne les exécute pas, il **demande** qu'on les appelle, puis il rédige sa réponse à partir du résultat.  
> 🔁 Concrètement, ce sont deux appels au modèle, reliés par un identifiant d'appel d'outil (`tool_call_id`).  
> ☕️ Le même use case est implémenté du plus bas niveau au plus haut : Bash, Java pur, le SDK OpenAI, LangChain4j, Quarkus et Spring AI.  
> 🐙 Tous les exemples utilisent [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) d'OVHcloud et sont disponibles [ici](https://github.com/philippart-s/java-ai-area-blog).

<br/>

# 📜 Introduction

Petit rappel des épisodes précédents : cet article fait partie d'une série d'articles visant à expliquer comment intégrer de l'IA dans les applications que nous développons en Java ☕️.
Après les bases ([partie 1](/blog/2026-07-21-java-and-ai-part1)), la mémoire ([partie 2](/blog/2026-08-03-java-and-ai-part2) et [partie 3](/blog/2026-08-27-java-and-ai-part3)) et le RAG ([partie 4](/blog/2026-10-28-java-and-ai-part4)), on continue avec une autre brique essentielle : le **function calling**.

# 🤷 Mais pourquoi faire du function calling ?

Reprenons notre conférence fictive de la partie 4, le **DevFest Atlantide 2027** 🌊.
Cette fois, on aimerait savoir combien de personnes peut accueillir la salle Nautilus.

Posons la question à `gpt-oss-120b`, sans rien d'autre :

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

Un joli tableau, très sûr de lui, et entièrement inventé 🤥.
Le modèle ne connaît pas cette salle, il ne peut que deviner.

On pourrait se dire : « facile, on fait du RAG avec la fiche de chaque salle ».
Ça marche pour des informations qui ne bougent pas, mais beaucoup de données sont vivantes : le nombre de places restantes, la météo, l'état d'une commande, le contenu d'une base de données, ...
Et surtout, le RAG ne permet que de **lire** : il ne permettra jamais au modèle de **faire** quelque chose, comme réserver une place ou envoyer un mail.

C'est là qu'intervient le function calling : donner au modèle la possibilité de **demander** l'exécution de fonctions de notre application.

# 🔎 Comment fonctionne le function calling ?

Première chose importante à comprendre : **le modèle n'exécute jamais rien** ⚠️.
Il n'a pas accès à votre code, à votre réseau ou à votre base de données.
Tout ce qu'il sait faire, c'est produire du texte... et, dans le cas qui nous intéresse, un texte structuré qui dit « j'aimerais que tu appelles telle fonction avec tels paramètres ».

Le déroulement est donc le suivant :
 1. on envoie au modèle la question **et** la description des fonctions qu'il a le droit de demander
 2. le modèle ne répond pas à la question : il renvoie une demande d'appel de fonction (un _tool call_), avec le nom de la fonction et ses arguments
 3. notre application exécute la fonction
 4. on renvoie au modèle toute la conversation, plus la demande d'appel et son résultat
 5. le modèle rédige enfin sa réponse à partir de ce résultat

Pour la suite de l'article, on va utiliser une fonction `get_room_capacity` qui prend le nom d'une salle et renvoie son nombre de places.

> ℹ️ Pour être sûr que la réponse vient bien de notre fonction et pas de l'imagination du modèle, la capacité est un nombre aléatoire entre 50 et 500. Le modèle ne peut pas le deviner 🎲.

## 📝 La description d'une fonction

Pour que le modèle sache quelles fonctions existent, on les lui décrit dans le champ `tools` de la requête, avec un [JSON Schema](https://json-schema.org/) :

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

C'est **tout** ce que le modèle connaît de la fonction : son nom, ce qu'elle fait et ses paramètres.
Le code, lui, ne quitte jamais votre machine.

Les descriptions ne sont donc pas là pour faire joli : c'est en les lisant que le modèle décide s'il doit appeler la fonction et comment remplir ses paramètres.
Une description floue, et le modèle n'appellera pas la fonction, ou l'appellera mal 😅.

## 🛠️ La demande d'appel

Quand le modèle décide d'utiliser une fonction, sa réponse ne contient pas de texte (`content`), mais un tableau `tool_calls` :

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

Quelques points à noter :
 - le `finish_reason` vaut `tool_calls` et non `stop` : le modèle n'a pas terminé, il attend quelque chose de notre part
 - les arguments sont un document `JSON`... dans une chaîne de caractères, il faudra donc les parser
 - l'`id` permettra de relier le résultat à la demande d'appel lors du deuxième appel
 - le champ `reasoning` est propre à `gpt-oss-120b`, qui est un modèle de raisonnement : on y voit le modèle décider d'appeler la fonction 🧐

## 🔁 Le renvoi du résultat

Une fois la fonction exécutée, on rappelle le modèle avec toute la conversation, à laquelle on ajoute deux messages :
 - le message de l'assistant contenant la demande d'appel, tel quel
 - un message avec le rôle `tool`, qui contient le résultat et l'identifiant de l'appel (`tool_call_id`)

```json
{
  "role": "tool",
  "tool_call_id": "chatcmpl-tool-99db5c5b23562aeb",
  "content": "{\"room\":\"Nautilus\",\"capacity\":458}"
}
```

Vous avez reconnu le principe de la [partie 2](/blog/2026-08-03-java-and-ai-part2) : le modèle est _stateless_, il faut donc lui renvoyer tout l'historique pour qu'il comprenne d'où vient ce résultat.

> 💸 Le function calling, c'est donc au minimum **deux appels** au modèle pour une seule question, et la description des fonctions est envoyée à chaque fois. Avec des dizaines de fonctions, cela finit par peser sur la facture et sur la fenêtre de contexte.

# 🖥️ La base : Bash

Comme pour les premiers articles, on commence avec `curl` et `jq`, pour bien voir ce qui transite.

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

 - lignes 22 à 31 : notre fonction, qui renvoie un nombre de places aléatoire. Elle affiche aussi une trace 🐞 sur la sortie d'erreur, pour bien voir quand elle est appelée : la sortie standard, elle, est le résultat renvoyé au modèle
 - lignes 35 à 50 : la description de la fonction, celle vue plus haut
 - lignes 58 à 67 : l'appel au modèle, qui envoie à chaque fois la conversation **et** la liste des fonctions (`tools`)
 - ligne 74 : le premier appel
 - lignes 82 et 83 : on récupère la demande d'appel dans la réponse
 - lignes 87 à 91 : si le modèle n'a pas demandé d'appel, sa réponse est déjà là et il n'y a rien à exécuter. Appeler une fonction est un choix du modèle, pas une obligation
 - lignes 93 à 95 : l'identifiant de la demande, le nom de la fonction et ses arguments
 - lignes 103 à 106 : on vérifie le nom de la fonction demandée plutôt que de faire confiance au modèle
 - lignes 108 et 109 : on parse les arguments et on exécute la fonction, c'est le script qui appelle la fonction, pas le modèle 😉
 - lignes 117 à 121 : on ajoute à la conversation le message de l'assistant et le résultat, reliés par le `tool_call_id`
 - ligne 127 : le deuxième appel, qui renvoie enfin la réponse

Et voici le résultat :

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

430 places, tirées au hasard par notre fonction, et reprises telles quelles par le modèle 🎉.

> ⚠️ Pour garder l'exemple lisible, le script ne traite que la première demande d'appel. Dans la vraie vie, le modèle peut en demander plusieurs d'un coup, ou en redemander une après avoir vu le résultat de la première. Il faut alors boucler jusqu'à obtenir un `finish_reason` à `stop`.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/00_bash/00.05_FunctionCalling.sh) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="bash-function-calling.mov" type="video/quicktime">
</video>

# ☕️ Java

## ✍️ Pure Java

Passons à la version Java sans librairie d'IA, avec le `HttpClient` du JDK et Jackson.

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

 - lignes 33 à 43 : notre fonction, une simple méthode Java, avec sa trace 🐞
 - lignes 50 à 66 : la description de la fonction, écrite à la main en `JSON`
 - ligne 78 : les fonctions sont ajoutées au corps de la requête, qui sera envoyé deux fois
 - ligne 85 : le premier appel
 - lignes 93 à 105 : la lecture de la demande d'appel dans la réponse, avec le cas où le modèle répond directement (lignes 98 à 102)
 - lignes 113 à 116 : la vérification du nom de la fonction demandée
 - lignes 118 et 119 : le parsing des arguments et l'exécution de la fonction
 - lignes 127 à 131 : l'ajout du message de l'assistant et du résultat dans la conversation
 - ligne 137 : le deuxième appel

C'est exactement la même chose qu'en Bash, avec le même problème : rien ne relie la méthode `getRoomCapacity` et sa description `JSON`.
Si on ajoute un paramètre à l'une sans modifier l'autre, c'est au moment de l'exécution qu'on s'en rendra compte 💥.

À l'exécution, on retrouve le même déroulé :

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

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/01_pure_java/_01_06_FunctionCalling.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-function-calling.mov" type="video/quicktime">
</video>

## 🛠️ Avec le SDK OpenAI

Le SDK OpenAI apporte une première vraie simplification : la description de la fonction est générée à partir d'une classe Java.

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

 - lignes 28 à 39 : la fonction est un `record` : ses annotations Jackson donnent le nom (`@JsonTypeName`), la description (`@JsonClassDescription`) et la description du paramètre (`@JsonPropertyDescription`), sa méthode `call()` contient le code (et la trace 🐞)
 - ligne 64 : `addTool` génère le JSON Schema de la fonction à partir du `record`
 - ligne 73 : le premier appel
 - lignes 81 à 92 : la lecture de la demande d'appel, avec le cas où le modèle répond directement (lignes 86 à 90)
 - lignes 101 à 104 : la vérification du nom de la fonction demandée
 - ligne 106 : `arguments(GetRoomCapacity.class)` transforme directement les arguments `JSON` en `record`, plus besoin de les parser à la main
 - lignes 108 à 111 : le message `tool`, avec le résultat sérialisé en `JSON` par le SDK
 - lignes 119 à 121 : l'ajout des deux messages dans la conversation
 - ligne 128 : le deuxième appel

La description et le code de la fonction sont maintenant au même endroit, et le compilateur veille à ce qu'ils restent cohérents 😎.
En revanche, l'orchestration reste à notre charge : c'est toujours nous qui lisons la demande d'appel, exécutons la fonction et rappelons le modèle.

En regardant la description envoyée, on voit d'ailleurs que le SDK génère un schéma un peu plus strict que le nôtre (`strict=true`, `additionalProperties=false`) :

{|
```text
ChatCompletionTool{function=ChatCompletionFunctionTool{function=FunctionDefinition{name=get_room_capacity, description=Returns the number of seats of a conference room, parameters={$schema=https://json-schema.org/draft/2020-12/schema, type=object, properties={room={type=string, description=Name of the room, e.g. Coral}}, required=[room], additionalProperties=false}, strict=true, additionalProperties={}}, type=function, additionalProperties={}}}
```
|}

Et à la fin :

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

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/02_sdk_java/_02_06_FunctionCalling.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="java-sdk-function-calling.mov" type="video/quicktime">
</video>

## 🦜 Avec LangChain4j

Avec [LangChain4j](https://docs.langchain4j.dev/tutorials/tools), on passe un cap : le framework prend en charge toute l'orchestration.

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

 - lignes 34 à 44 : la fonction est une méthode Java annotée avec `@Tool` (sa description) et `@P` (la description du paramètre), avec sa trace 🐞
 - ligne 67 : on branche les outils sur l'AI Service, comme la mémoire dans la partie 2 ou le _retriever_ dans la partie 4
 - lignes 68 à 77 : deux _callbacks_, appelés avant et après l'exécution de l'outil, qui ne servent qu'à afficher ce qui se passe
 - ligne 85 : l'appel à l'assistant, qui ne prend que la question

Et c'est tout 🤯.
Pendant l'appel à `chat`, LangChain4j envoie la question et la description de l'outil, détecte la demande d'appel, exécute la méthode, renvoie le résultat au modèle et nous rend la réponse finale.
Et contrairement à nos exemples précédents, il boucle tant que le modèle demande des outils.

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

Notez que la fonction s'appelle maintenant `getRoomCapacity` : par défaut, LangChain4j utilise le nom de la méthode Java comme nom de fonction.

> ℹ️ Pour voir les requêtes réellement envoyées au modèle, il suffit de décommenter `.logRequests(true)` et `.logResponses(true)` (en remplaçant `slf4j-nop` par `slf4j-simple`) : on y retrouve les deux appels et le message `tool`.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/03_langchain4j/_03_07_FunctionCalling.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="langchain4j-function-calling.mov" type="video/quicktime">
</video>

## ⚡️ Avec Quarkus

Avec Quarkus, l'outil devient un bean CDI, déclaré sur l'AI Service.

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

 - lignes 41 à 52 : l'outil, le même qu'avec LangChain4j, mais en tant que bean CDI (`@ApplicationScoped`)
 - ligne 56 : les outils sont déclarés directement dans l'annotation `@RegisterAiService`
 - ligne 79 : l'appel, avec uniquement la question
 - lignes 87 à 95 : un _observer_ CDI de l'événement `ToolExecutedEvent`, émis après chaque exécution d'outil, qui ne sert qu'à l'affichage

La configuration est celle des articles précédents (`application.properties`), il n'y a rien à ajouter pour le function calling.

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

Vous remarquerez que la trace 🐞 apparaît **avant** la demande d'appel : l'événement `ToolExecutedEvent` (on verra qu'il y a d'autres moyens de faire des actions avant l'appel de l'outil) n'est émis qu'une fois l'outil exécuté, l'_observer_ affiche donc la demande et le résultat après coup.

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/04_quarkus/_04_06_FunctionCalling.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="quarkus-function-calling.mov" type="video/quicktime">
</video>

## ☘️ Avec Spring AI

Pour finir, voyons comment [Spring AI](https://docs.spring.io/spring-ai/reference/api/tools.html) gère le function calling.

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

 - lignes 45 à 55 : l'outil, avec les annotations `@Tool` et `@ToolParam` de Spring AI (même principe, autre package 😉), et toujours la trace 🐞
 - lignes 59 à 78 : un `ToolCallback` qui enveloppe l'outil pour afficher l'appel et le résultat, il n'est pas nécessaire au fonctionnement
 - ligne 99 : on branche l'outil sur le `ChatClient`, sans l'affichage, un simple `.defaultTools(new RoomTools())` suffirait
 - lignes 109 à 112 : l'appel, avec uniquement la question

Comme avec LangChain4j, c'est le framework qui orchestre tout pendant l'appel : le `ChatClient` détecte la demande d'appel, exécute l'outil (via son `ToolCallingManager`), renvoie le résultat au modèle et boucle jusqu'à obtenir la réponse finale.

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

Pour voir le source complet de cet exemple, c'est [ici](https://github.com/philippart-s/java-ai-area-blog/blob/main/05_spring/_05_06_FunctionCalling.java) 📜.

#### 📽️ Voyons ça en action !
<video controls class="video-centered">
  <source src="spring-function-calling.mov" type="video/quicktime">
</video>

# 🔭 Et après ?

Notre exemple est volontairement minimaliste : une fonction, un paramètre, un appel.
Dans une vraie application, quelques points méritent votre attention :
 - **plusieurs outils** : on peut en déclarer autant que l'on veut, c'est le modèle qui choisit lequel appeler en fonction de leur description, d'où l'importance de bien les rédiger
 - **plusieurs appels** : le modèle peut demander plusieurs fonctions d'un coup, ou enchaîner les appels (récupérer la liste des salles, puis la capacité de chacune) ; les frameworks gèrent cette boucle pour vous, pas nos exemples Bash, Java et SDK
 - **la sécurité** ⚠️ : ce sont les arguments générés par le modèle qui sont passés à votre code, il faut donc les traiter comme n'importe quelle entrée utilisateur (validation, droits d'accès, ...), surtout si la fonction modifie des données
 - **le coût** 💸 : chaque outil déclaré est envoyé à chaque appel, et chaque demande d'appel ajoute un aller-retour avec le modèle

Vous retrouverez les détails sur oe function calling ici :
- [LangChain4j](https://docs.langchain4j.dev/tutorials/tools)
- [Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/function-calling.html)
- [Spring AI](https://docs.spring.io/spring-ai/reference/api/tools.html)

Et si on pousse le principe un peu plus loin :
 - laisser le modèle enchaîner les appels d'outils en autonomie jusqu'à atteindre un objectif, c'est le principe des **agents** 🤖
 - partager des outils entre applications plutôt que de les coder dans chacune, c'est le principe de [MCP](https://modelcontextprotocol.io/) (_Model Context Protocol_) 🔌

Ce sera l'objet d'articles suivants 😆.

# 🏁 Conclusion

Voilà pour cette introduction au function calling.
Derrière le nom, le mécanisme est assez simple : décrire des fonctions au modèle, le laisser demander leur exécution, et lui renvoyer le résultat.
Le modèle ne fait jamais que produire du texte, c'est votre application qui garde la main sur ce qui est réellement exécuté.

C'est aussi la brique de base de tout ce qui fait parler de l'IA en ce moment : agents, MCP, assistants de code...
Mais ça, c'est une autre histoire.

L'ensemble du code source est disponible dans le dépôt GitHub [java-ai-area-blog](https://github.com/philippart-s/java-ai-area-blog).

Si vous êtes arrivés jusque-là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
