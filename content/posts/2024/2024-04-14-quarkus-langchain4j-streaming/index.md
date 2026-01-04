---
title: "🤖 Augmente les capacités de ton IA avec LangChain4j 🦜"
description: "Deuxième partie de la découverte de LangChain4j au travers de Quarkus."
link: /2024-04-14-quarkus-langchain4j-streaming
tags: 
  - Quarkus
  - LangChain4J
  - IA
image: robot-ia.jpg
figCaption: "@wildagsx"
author: wilda
---

Je vous propose la suite de l'[article précédent](/2024-04-01-quarkus-langchain4j}) nous ayant permis la découverte de [LangChain4j](https://docs.langchain4j.dev/intro/) au travers de [Quarkus](https://quarkus.io/).
> ℹ️ Je vous laisse donc y jeter un oeil pour toute la phase d'installation, de prérequis nécessaires à la bonne compréhension de cet article.

Lors de ce premier article, nous avons vu ensemble comment développer notre premier chat bot.
Celui-ci était très simple et il fallait attendre que le modèle distant _fabrique_ l'ensemble de la réponse avant de l'avoir en retour.
Pas très pratique et convivial.

Cette fois-ci, je vous propose d'ajouter quelques fonctionnalités rendant notre _chat bot_ plus "intelligent".  
On va donc le faire se comporter comme un _chat bot_ normal : streamer sa réponse.
On va aussi lui ajouter un peu de contexte afin qu'il connaisse plus de choses essentielles 😉 !


>ℹ️ L'ensemble du code source se trouve dans le repository GitHub [discover-langchain4j](https://github.com/philippart-s/discover-langchain4j)

## 🌊 Activation du mode streaming

C'est la première fonctionnalité que nous allons rajouter : cela permet de rendre le bot plus convivial et de ne pas avoir à attendre sans trop savoir quand il va nous répondre 😅.  
Pour activer cette fonctionnalité, c'est assez simple : nous allons utiliser [Mutiny](https://quarkus.io/guides/mutiny-primer).
Mais c'est quoi me direz-vous 🤨 ?  
En deux mots : cela vous permet d'ajouter une notion d'asynchronisme dans votre développement et de basculer dans ce que l'on appelle la programmation reactive.  

L'objectif ?
Permettre à notre IA d'envoyer son début de réponse avant même d'avoir envoyé l'ensemble de la réponse.

### 🔀 Ajout de l'asynchronisme

[Quarkus](https://quarkus.io/) et son extension [quarkus-langchain4j](https://github.com/quarkiverse/quarkus-langchain4j/) vont encore grandement nous aider.

> ℹ️ Nous repartons du code de l'[article précédent](2024-04-01-quarkus-langchain4j}), si vous souhaitez plus de détails n'hésitez pas à vous reporter à l'article 😉.

Nous allons donc modifier notre service _OllamaService.java_ pour qu'il supporte le mode streaming : 

```java
package fr.wilda.quarkus;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.smallrye.mutiny.Multi;

// AI service bean registration
@RegisterAiService
public interface OllamaAIService {
  
  // Context message
  @SystemMessage("You are an AI assistant.")  
  // Prompt customisation
  @UserMessage("Answer as best possible to the following question: { question}. The answer must be in a style of a virtual assistant and use emoji.")
  String askAQuestion(String question);

  // Context message
  @SystemMessage("You are an AI assistant.")  
  // Prompt customisation
  @UserMessage("Answer as best possible to the following question: { question}. The answer must be in a style of a virtual assistant and use emoji.")
  // Multi use is enough to activate streaming mode
  Multi<String> askAQuestionStreamingMode(String question);

}
```

⚠️ Notez-bien ici l'utilisation de l'interface `io.smallrye.mutiny.Multi` qui permet "d'activer" le mode streaming.
L'extension se charge de l'activer lors de ses requêtes au modèle 😉.  
A noter que seul le type `String` est, pour l'instant, supporté pour le mode streaming mais des études d'évolutions sont en cours ⚠️

Maintenant, nous allons faire évoluer notre partie API pour qu'elle puisse profiter de cette arrivée d'informations au fil de l'eau.

### 🧩 Modification de l'API Rest

L'idée est de proposer une ressource qui va être _streamée_ au fur et à mesure.
On met donc à jour la  classe `AIAssistant` qui expose le endpoint `hal9000`.

```java
package fr.wilda.quarkus;

import org.jboss.resteasy.reactive.RestQuery;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

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

  // Stream response
  @Path("streaming")
  @GET
  public Multi<String> streaming(@RestQuery("question") String question) {
    // Call the Mistral AI chat model
    return ollamaAIService.askAQuestionStreamingMode(question);
  }
}
```

On a rajouté une ressource `streaming` qui retourne `Multi<String>`, Quarkus fera le reste.  
Ensuite il suffit d'aller sur l'URL `http://localhost:8080/hal9000/streaming?question="What is the answer to the Ultimate Question of Life,the Universe, and Everything?"`

{% include video id="T0LbsThvaRY" provider="youtube"}

## 👌 Ajoutons un peu de contexte

L'intelligence des différents modèles que l'on utilise dépend grandement des données utilisées mais aussi de quand date la dernière _indexation_ de ces données (oui je sais que ce n'est pas le terme standard mais ça permet de se comprendre 😉).

Essayons ce prompt, voulez-vous : _Can you tell me more about Stéphane Philippart?_  

Voilà la réponse donnée:  
```
👋 Hello! Stéphane Philippart is a renowned figure in the tech industry, particularly in Belgium.
🇧🇪💻 He's known for his expertise in Information Technology (IT) and Digital Transformation. Stéphane has spent over two decades in the tech sector, holding various key positions in leading companies.
🕒✨ His achievements include co-founding several successful startups and contributing significantly to their growth. 
He's also a sought-after speaker at industry events, sharing his insights on digital transformation trends._
```

Flatteur mais très loin de la réalité non 🤨 ?

### 🗃️ Le RAG à la rescousse

Rien d'étonnant dans cette réponse :
 - je ne suis pas très connu et donc pas étonnant de pas avoir beaucoup de données sur moi
 - mon prénom, assez commun, fait que l'hallucination avec d'autres _Stéphane_ n'est pas étonnante
 - la plupart des éléments publiques datent de 2-3 ans et souvent les modèles ont été entraînés sur des données plus anciennes

En quoi le RAG va nous aider ?  
Le RAG ou encore _Retrieval Augmented Generation_ va vous permettre d'ajouter des données non connues de votre modèle pour lui donner un contexte qui correspond à votre domaine fonctionnel.
Pour ajouter ce contexte on va prendre une source de données, par exemple des fichiers, puis les transformer dans un format permettant une recherche par similitudes, dans ce cas une base de données vectorielle.  

Une fois ce contenu ajouté, lors de requêtes envoyées au modèle, celui-ci va pouvoir se baser sur ces données supplémentaires pour contextualiser sa réponse.

> Le RAG se différencie d'une autre technique, le transfert learning, par le fait que l'on utilise le modèle tel quel en lui ajoutant des données / du contexte.  
> Le transfert learning va consister à ré-entrainer un modèle pour un use case différent de celui d’origine, cela demande donc plus de calculs et une phase d’entraînement là où le RAG se fait au runtime.

### 📃 Les données à rajouter

Je vous l'ai dit, on peut choisir plusieurs types de données.
Dans mon cas, je vais choisir un fichier texte.

```
Stéphane Philippart is a world-renowned developer advocate in the field of cloud computing. He is also the CTO of Tours' biggest meetup.
Tours is well known for its famous rillettes.
```

### 🧩 Activation du RAG

Pour utiliser le RAG il va falloir deux choses : 
 - un moyen de transformer nos données en vecteurs
 - un moyen d'ajouter ces données dans la chaîne d'interrogation de notre modèle

Là encore, vous vous en doutez, Quarkus et LangChain4j vont nous être d'un grand secours !

```xml
<!-- To add RAG capabilities -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-easy-rag</artifactId>
    <version>0.13.0</version>
</dependency>  

<!-- Inner process embedding model -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-all-minilm-l6-v2-q</artifactId>
    <version>0.30.0</version>
</dependency>
```
Merci à l'extension Quarkus qui, par le simple ajout de la dépendance `quarkus-langchain4j-easy-rag`, active le mode [Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/easy-rag.html).

Notez l'ajout de la dépendance `dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2-q` qui me permet d'avoir un [_embedding model_ in process](https://docs.quarkiverse.io/quarkus-langchain4j/dev/in-process-embedding.html).
J'ai choisi cela plutôt que d'utiliser le endpoint d'embedding de Mistral car en mode Ollama il n'est pas accessible avec LangChain4j.

Au chargement de l'application on voit que le mode RAG est activé avec les bonnes données : 
```bash
2024-05-05 20:12:23,987 INFO  [io.qua.lan.eas.run.EasyRagRecorder] (Quarkus Main Thread) Ingesting documents from path: ./src/main/resources/rag/, path matcher = glob:**, recursive = true
```

Réessayons de demander au modèle s'il connaît des choses sur _Stéphane Philippart_ !  
L’appel à l'URL `http://localhost:8080/hal9000/streaming?question="Can you tell me more about Stéphane Philippart?"` donne cette fois : 
```
🌐 Hey there! Stéphane Philippart,👨‍💻 is a globally recognized developer advocate in the cloud computing domain! *claps* 
His expertise is highly sought after, making him a key figure in this innovative field.💡Moreover, Stéphane holds an impressive title as the CTO of Tours' biggest meetup!
🏢 This city in France, famously known for its scrumptious rillettes,🥓😋 is where he makes a significant impact`
```

Pas forcément plus vrai mais mieux quand même non ? 😉

**⚠️ Ce petit exemple doit vous faire allumer quelques alertes dans votre cerveau : on ne peut définitivement pas faire confiance aux résultats d'une IA que l'on peut si facilement biaiser en quelques lignes de codes ⚠️**

# En conclusion

J'espère que vous avez pu, simplement, vous rendre compte comme il est assez facile avec Quarkus et LangChain4j d'ajouter des capacités à notre application de _chat bot_.
Je vais continuer à ajouter quelques autres choses plus ou moins utiles dans les articles suivants 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.

L'ensemble des sources des exemples est disponible dans le repository GitHub [langchain4j-discovery](https://github.com/philippart-s/discover-langchain4j).