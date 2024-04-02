---
title: "⚡️ Quand Quarkus rencontre LangChain4j 🦜"
classes: wide
excerpt: "Découverte de comment utiliser LangChain4j avec l’extension Quarkus quarkus-langchain4j"
categories:
  - Code
  - Java
  - IA
  
tags:
  - Quarkus
  - Langchain4J
  - IA

---
**TODO** image d'un perroquet qui fait de l'IA
<br/>

Dans le cadre de mon travail, cela fait maintenant plus de deux ans que je navigue dans le monde de l'Intelligence Artificielle.
Et, comme tout le monde, j'ai assisté à la déferlante des [Large Languages Model](https://fr.wikipedia.org/wiki/Grand_modèle_de_langage){:target="_blank"} (LLM).
Depuis quelques mois, le monde de l'IA n'est plus réservé aux spécialistes mais accessibles aux développeuses et développeurs.
Et comme à chaque fois qu'une nouvelle tendance entre dans notre monde, cela se fait avec beaucoup d’enthousiasme et de passion !
Vous n'êtes, certainement, pas sans savoir que la communauté de développement s'est passionné pour un domaine en particulier : le [Retrieval Augmented Generation](https://huggingface.co/docs/transformers/model_doc/rag){:target="_blank"} (RAG).
Pour faire simple : comment spécialiser un LLM avec vos données à vous.

Comme souvent, des frameworks voient le jour, certains meurs, d'autres sont massivement utilisés par l'écosystème.
C'est le cas de [LangChain](https://github.com/langchain-ai){:target="_blank"} qui simplifie grandement l'utilisation de LLM et plus particulièrement de la partie RAG.

Oui mais voilà sorti de Python et Javascript, langchain n'est pas utilisable.
Vous me voyez venir, moi je fais du Java, et donc naturellement, comme d'habitude, je suis parti à la recherche d'un équivalent en Java 😉.
Je n'ai pas eu à chercher bien longtemps, très vite un framework a vu le jour, [LangChain4j](https://github.com/langchain4j/langchain4j/){:target="_blank"}.

> ⚠️ le nom peut être trompeur, le projet ne fait pas partie de la galaxy LangChain. C'est un projet à part, avec sa propre communauté et son propre cycle de vie. ⚠️

Et comme toujours, parce que [Quarkus](https://quarkus.io/){:target="_blank"} c'est cool, j'ai là aussi eu le plaisir de voir qu'une extension a vite vue le jour : [quarkus-langchain4j](https://github.com/quarkiverse/quarkus-langchain4j){:target="_blank"} 🤩.

Le décor est posé, je vous propose de souter dans le terrier avec moi et de voir comment faire de l'IA, plus particulièrement utiliser un LLM, avec LangChain4j au travers de Quarkus 🐇 !

**TODO** image terrier alice lapin et perroquet


## 🧠 Les modèles "compatibles"

La plus part des modèles possèdent des API, ce qui les rends _compatibles_ puisqu'il suffit de [coder un client REST](https://quarkus.io/guides/rest-client){:target="_blank"} pour les utiliser.
Cependant, les ressources JSON, les endpoints et tous le reste peut être vite rébarbatif.
Et je ne parle pas de la partie RAG, c'est là où les frameworks vont vous faciliter la vie.

Pour notre première fois avec LangChain4j je vous propose de commencer simple : développer un chat bot intelligent.
D'autres blog posts suivront par la suite pour aller dans des cas d'usages plus complexes.

Sélectionnons donc le modèle que l'on veut utiliser, dans notre cas ce sera [Ollama](https://ollama.com/){:target="_blank"}, ce n'est pas à proprement parlé un modèle mais une façon de faire tourner des LLM en local.
Pourquoi me direz vous alors que la plupart des acteurs du marché proposent une API ?
Tout simplement que ces API sont facturées à l'appel 😉, oui, et cela n'a rien de choquant, il faut bien, à un moment donnée, que les société gagnent de l'argent.
Mais ce qui est bien c'est que nombreuses entre elles fournissent leurs modèles en open source.
Libre à vous de les utiliser tant est que vous soyez en capacité de les déployer et exécuter.
C'est là où Ollama nous sauve la mise en facilitant grandement la récupération et la mise à disposition d'un modèle sous forme d'API.

Je vous laisse aller voir la documentation d'Ollama, mais au final cela se résume à [installer une CLI](https://ollama.com/download){:target="_blank"} puis de [choisir le modèle](https://ollama.com/library){:target="_blank"} que vous souhaitez utiliser 😎.

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
Vous le voyez ici, le CLI permet d'avoir un prompt interactif.
Ce qui m’intéresse c'est de pouvoir faire des call API, Ollama expose aussi un endpoint.

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

Ca c'est fait, nous avons notre LLM à disposition pour nos tests, passons aux choses agréable : coder !

## ⚡️ + 🦜

Créons notre projet Quarkus en activant l'extension quarkus-langchain4j-ollama : `quarkus create app fr.wilda.quarkus:discover-langchain4j --extension='quarkus-langchain4j-ollama'`.
A ce stade, on reste dans du classique avec Quarkus : le projet est initialisé avec tout ce qui va bien en termes d'arborescences et de configuration de dépendances dans le pom.xml.

Pour faire notre chat bot, la première chose que je vous conseille est d'aller voir la documentation de l'extension.
Elle liste les différents modèles supportés ainsi que les configurations à activer.
Cela permet aussi d'avoir quelques exemples de code.

> ⚠️ Il se peut que le code pour utiliser un modèle soit présent et pas le documentation. C'était le cas au moment où j'écris cet article pour Mistral par exemple. N'hésitez pas à jeter un oeil directement au code pour vous en assurer 😉.

### 🤖 Le service 

Le [service](https://docs.quarkiverse.io/quarkus-langchain4j/dev/ai-services.html){:target="_blank"} est la base de votre code pour interagir avec le modèle.
Il va vous permettre de définir un contexte mais aussi de paramétrer vos interactions avec le modèle par le biais de variables.

```java
@RegisterAiService
public interface OllamaAIService {
  
  @SystemMessage("You are an AI assistant.")  
  @UserMessage("Answer as best possible to the following question: {question}. The answer must be in a style of a virtual assistant ans use emoji.")
  String askAQuestion(String question);
}
```
Ici on donne un peu de contexte à notre LLM afin qu'il nous réponde dans le style que l'on souhaite.
On peut le faire via le [system message](https://docs.quarkiverse.io/quarkus-langchain4j/dev/ai-services.html#_system_message){:target="_blank"} et le [user message](https://docs.quarkiverse.io/quarkus-langchain4j/dev/ai-services.html#_user_message_prompt){:target="_blank"}.

A cela il faut ajouter quelques éléments de configuration (portionnables aussi en variables d’environnement ou via programmation).
```java
quarkus.langchain4j.ollama.chat-model.enabled=true
quarkus.langchain4j.ollama.log-requests=true
quarkus.langchain4j.ollama.timeout=60s
quarkus.langchain4j.ollama.embedding-model.enabled=false
quarkus.langchain4j.ollama.base-url=http://localhost:11434/
quarkus.langchain4j.ollama.chat-model.model-id=mistral
```

### 🤖 La classe principale




# En conclusion


Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog){:target="_blank"} 😊.