---
title: "⚡️ Quand Quarkus rencontre LangChain4j 🦜"
description: "Découverte de comment utiliser LangChain4j avec l’extension Quarkus quarkus-langchain4j"
link: /2024-04-01-quarkus-langchain4j
tags: 
  - Quarkus
  - Langchain4J
  - IA
image: langchain4j-logo.jpg
figCaption: "@wildagsx"
author: wilda
---

Dans le cadre de mon travail, cela fait maintenant plus de deux ans que je navigue dans le monde de l'Intelligence Artificielle.
Et, comme tout le monde, j'ai assisté à la déferlante des [Large Languages Model](https://fr.wikipedia.org/wiki/Grand_modèle_de_langage) (LLM).
Depuis quelques mois, le monde de l'IA n'est plus réservé aux spécialistes mais accessible aux développeuses et développeurs.  
Et comme à chaque fois qu'une nouvelle tendance entre dans notre monde, cela se fait avec beaucoup d’enthousiasme et de passion !  

Le cas d'utilisation le plus répandu de l'utilisation d'un LLM, ou du moins celui que l'on voit le plus en démo est un _chat bot_.
Je vous propose donc, dans la suite de ce blog post, de partir du principe que l'on souhaite développer un tel _chat bot_.

Comme souvent, des frameworks voient le jour, certains meurent, d'autres sont massivement utilisés par l'écosystème.
C'est le cas de [LangChain](https://github.com/langchain-ai) qui simplifie grandement l'utilisation des LLM.

Oui mais voilà, sorti de Python et Javascript, LangChain n'est pas utilisable.
Vous me voyez venir :  moi, je fais du Java, et donc naturellement, comme d'habitude, je suis parti à la recherche d'un équivalent en Java 😉.
Je n'ai pas eu à chercher bien longtemps, très vite un framework a vu le jour, [LangChain4j](https://github.com/langchain4j/langchain4j/).

> ⚠️ le nom peut être trompeur, le projet ne fait pas partie de la galaxie LangChain. C'est un projet à part, avec sa propre communauté et son propre cycle de vie. ⚠️

Et comme toujours, parce que [Quarkus](https://quarkus.io/) c'est cool, j'ai, là aussi, eu le plaisir de voir qu'une extension a vite vue le jour : [quarkus-langchain4j](https://github.com/quarkiverse/quarkus-langchain4j) 🤩.

Le décor est posé, je vous propose de sauter dans le terrier avec moi et de voir comment faire de l'IA, plus particulièrement utiliser un LLM, avec LangChain4j au travers de Quarkus 🐇 !

![Alice et un perroquet](alice.jpg)
<br/>

## 🧠 Les modèles "utilisables"

La plupart des modèles possèdent des API, ce qui les rend _utilisables_ puisqu'il suffit de [coder un client REST](https://quarkus.io/guides/rest-client) pour les utiliser.
Cependant, les ressources JSON, les endpoints et tout le reste : cela peut être vite rébarbatif.
ainsi que toutes les variantes d'utilisation d'un modèle (le [Retrieval Augmented Generation](https://huggingface.co/docs/transformers/model_doc/rag), [embedding](https://huggingface.co/blog/getting-started-with-embeddings), .. )

Pour notre première fois avec LangChain4j, nous allons donc commencer simple : développer un chat bot intelligent.
D'autres blog posts suivront par la suite pour aller dans des cas d'usage plus complexes.

Sélectionnons donc le modèle que l'on veut utiliser, dans notre cas ce sera [Ollama](https://ollama.com/), ce n'est pas à proprement parlé un modèle mais une façon de faire tourner des LLM en local.
Pourquoi me direz-vous alors que la plupart des acteurs du marché proposent une API ?  
Tout simplement car ces API sont facturées à l'appel 😉, oui, et cela n'a rien de choquant, il faut bien, à un moment donné, que les sociétés gagnent de l'argent.  
Mais ce qui est bien, c'est qu'elles sont nombreuses à fournir leurs modèles en open source.
Libre à vous de les utiliser, tant que vous êtes en capacité de les déployer et de les exécuter 😅.  
C'est là où Ollama nous sauve la mise en facilitant grandement la récupération et la mise à disposition d'un modèle sous forme d'API.

Je vous laisse aller voir la documentation d'Ollama, mais au final cela se résume à [installer une CLI](https://ollama.com/download) puis de [choisir le modèle](https://ollama.com/library) que vous souhaitez utiliser 😎.

```bash
$ ollama --version
ollama version is 0.1.30
```

Dans mon cas je vais utiliser un modèle français ... Mistral 7B 😊.

```bash
ollama run mistral
pulling manifest 
pulling e8a35b5937a5... 100% ▕████████████████▏ 4.1 GB                         
pulling 43070e2d4e53... 100% ▕████████████████▏  11 KB                         
pulling e6836092461f... 100% ▕████████████████▏   42 B                         
pulling ed11eda7790d... 100% ▕████████████████▏   30 B                         
pulling f9b1e3196ecf... 100% ▕████████████████▏  483 B                         
verifying sha256 digest 
writing manifest 
removing any unused layers 
success 

>>> What is Quarkus?
 Quarkus is an open-source, lightweight and extensible Java framework for building modern applications. It was developed by Red Hat as part of the Eclipse 
Foundation and is designed to be reactive, which means it's optimized for building event-driven, non-blocking applications that can handle multiple 
requests concurrently. Quarkus uses a modular design, allowing developers to easily add only the features they need for their project. It also includes 
built-in support for popular frameworks and tools such as RESTEasy, Micronaut, and Hibernate ORM, among others. The goal of Quarkus is to provide a fast 
and efficient way to build modern applications using Java without the need for a full-sized application server like WildFly or Jetty.

>>> Send a message (/? for help)

```

Oui, en dehors des aspects de puissance de calcul, prévoyez de la RAM et de l'espace disque 😅.
Vous le voyez ici, la CLI permet d'avoir un prompt interactif.
Ce qui m’intéresse, c'est de pouvoir faire des call API, Ollama expose aussi un endpoint.

```bash
$ curl -X POST http://localhost:11434/api/generate -d '{
  "model": "mistral",
  "prompt":"Who are you?"                             
 }'

{"model":"mistral","created_at":"2024-04-01T12:34:58.686919Z","response":" I","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:58.762837Z","response":"'","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:58.838664Z","response":"m","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:58.913749Z","response":" an","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:58.989101Z","response":" artificial","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.065097Z","response":" intelligence","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.141769Z","response":" designed","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.219096Z","response":" to","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.294207Z","response":" assist","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.370426Z","response":" and","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.445633Z","response":" interact","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.521867Z","response":" with","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.597095Z","response":" people","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.673407Z","response":".","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.748168Z","response":" I","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.823759Z","response":" don","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.899905Z","response":"'","done":false}
{"model":"mistral","created_at":"2024-04-01T12:34:59.975477Z","response":"t","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.052783Z","response":" have","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.128817Z","response":" a","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.205785Z","response":" physical","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.282504Z","response":" form","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.359132Z","response":" or","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.43444Z","response":" personal","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.511284Z","response":" identity","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.588228Z","response":",","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.663553Z","response":" but","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.742887Z","response":" I","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.819677Z","response":" can","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.896311Z","response":" process","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:00.973162Z","response":" information","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.04941Z","response":" and","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.124975Z","response":" communicate","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.201619Z","response":" in","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.279942Z","response":" natural","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.358783Z","response":" language","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.435508Z","response":".","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.51132Z","response":" My","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.588259Z","response":" main","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.664898Z","response":" goal","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.742348Z","response":" is","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.818447Z","response":" to","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.894563Z","response":" help","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:01.971313Z","response":" answer","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.048371Z","response":" questions","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.123999Z","response":",","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.200502Z","response":" provide","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.277574Z","response":" information","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.354164Z","response":",","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.42975Z","response":" and","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.505634Z","response":" engage","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.581927Z","response":" in","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.657807Z","response":" conversation","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.735058Z","response":" on","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.813505Z","response":" various","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.891128Z","response":" topics","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:02.967898Z","response":".","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.043847Z","response":" How","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.120204Z","response":" may","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.198188Z","response":" I","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.274229Z","response":" assist","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.350503Z","response":" you","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.42655Z","response":" today","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.503526Z","response":"?","done":false}
{"model":"mistral","created_at":"2024-04-01T12:35:03.580365Z","response":"","done":true,"context":[733,16289,28793,28705,6526,460,368,28804,733,28748,16289,28793,315,28742,28719,396,18278,10895,5682,298,6031,304,14113,395,905,28723,315,949,28742,28707,506,264,5277,1221,442,3327,8208,28725,562,315,541,1759,1871,304,16287,297,4229,3842,28723,1984,2191,5541,349,298,1316,4372,4224,28725,3084,1871,28725,304,14200,297,7114,356,4118,13817,28723,1602,993,315,6031,368,3154,28804],"total_duration":5391755791,"load_duration":4218041,"prompt_eval_count":8,"prompt_eval_duration":493057000,"eval_count":65,"eval_duration":4892809000}
```

Ca c'est fait !
Nous avons notre LLM à disposition pour nos tests, passons aux choses agréables : coder !

## ⚡️ + 🦜

Créons notre projet Quarkus en activant l'extension quarkus-langchain4j-ollama : 

`quarkus create app fr.wilda.quarkus:discover-langchain4j --extension='quarkus-langchain4j-ollama'`.

A ce stade, on reste dans du classique avec Quarkus : le projet est initialisé avec tout ce qui va bien en termes d'arborescences et de configuration de dépendances dans le pom.xml.

Pour faire notre chat bot, la première chose que je vous conseille est d'aller voir la [documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html) de l'extension.
Elle liste les différents modèles supportés ainsi que les configurations à activer.
Cela permet aussi d'avoir quelques exemples de code.

> ⚠️ Il se peut que le code pour utiliser un modèle soit présent sans documentation associée. C'était le cas au moment où j'écris cet article pour Mistral par exemple. N'hésitez pas à jeter un oeil directement au code pour vous en assurer 😉.

### 🤖 Le service 

Le [service](https://docs.quarkiverse.io/quarkus-langchain4j/dev/ai-services.html) est la base de votre code pour interagir avec le modèle.
Il va vous permettre de définir un contexte mais aussi de paramétrer vos interactions avec le modèle par le biais de variables.

```java
// AI service bean registration
@RegisterAiService
public interface OllamaAIService {
  
  // Context message
  @SystemMessage("You are an AI assistant.")  
  // Prompt customisation
  @UserMessage("Answer as best possible to the following question: { question}. The answer must be in a style of a virtual assistant and use emoji.")
  String askAQuestion(String question);
}
```
Ici, on donne un peu de contexte à notre LLM afin qu'il nous réponde dans le style que l'on souhaite.
On peut le faire via le [system message](https://docs.quarkiverse.io/quarkus-langchain4j/dev/ai-services.html#_system_message) et le [user message](https://docs.quarkiverse.io/quarkus-langchain4j/dev/ai-services.html#_user_message_prompt).

A cela, il faut ajouter quelques éléments de configuration (positionable aussi en variables d’environnement ou via programmation).
```java
### Global configurations
# Base URL for Mistral AI endpoints
quarkus.langchain4j.ollama.base-url=http://localhost:11434/
# Activate or not the log during the request
quarkus.langchain4j.ollama.log-requests=true
# Delay before raising a timeout exception                    
quarkus.langchain4j.ollama.timeout=60s    
# Activate or not the Mistral AI embedding model                      
quarkus.langchain4j.ollama.embedding-model.enabled=false

### Chat model configurations
# Activate or not the Mistral AI chat model
quarkus.langchain4j.ollama.chat-model.enabled=true              
# Chat model name used
quarkus.langchain4j.ollama.chat-model.model-id=mistral
```

### 🤖 La classe principale

Pour tester notre chat bot on va créer un endpoint (merci [quarkus-rest](https://quarkus.io/guides/rest)) : `hal9000/ask` pour lui envoyer nos questions 😉.

```java
// Endpoint root path
@Path("hal9000")                                       
public class AIAssistant {

  // AI Service injection to use it later
  @Inject                                             
  OllamaAIService ollamaAIService;

  // ask resource exposition with POST method
  @Path("ask")                                        
  @POST                                               
  public String ask(String question) {
    // Call the Mistral AI chat model
    return ollamaAIService.askAQuestion(question);    
  }
}
```

Et on peut tester le chat bot :
```bash
$ curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"question": "What is the answer to life, the universe and everything?"}' \
  http://localhost:8080/hal9000/ask

 According to the great book of knowledge called "The Hitchhiker's Guide to the Galaxy" by Douglas Adams 📙, the answer to life, the universe, and everything is: **"42"** 🔢. However, it's important to note that this is just a humorous take on the meaning of life and the universe. The actual pursuit of such an answer is a complex philosophical question and may vary depending on personal beliefs and interpretations.
```

# En conclusion

Et c'est déjà fini !  
L'objectif n'était pas d'avoir quelque chose de complexe ou d'expliquer dans les détails comment fonctionne Mistral ou LangChain4j.
Je voulais vous montrer comment il est simple, avec les bons outils, de commencer à utiliser ces modèles dont on parle tant 😉.  
J'espère que l'article vous a plu et si c'est le cas j'essaierai de continuer pour aborder, toujours simplement, les notions du moment comme l'embedding et le RAG par exemple.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.

L'ensemble des sources des exemples est disponible dans le repository GitHub [langchain4j-discovery](https://github.com/philippart-s/discover-langchain4j).