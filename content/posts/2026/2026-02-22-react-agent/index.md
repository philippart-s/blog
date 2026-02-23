---
title: "🔄 Apprendre à son agent à réfléchir avec le pattern ReAct et LangChain4j 🤖"
description: "I’m not a man. I’m something that evolved. @Skynet"
link: /2026-02-22-react-agent
image: cover.jpg
figCaption: ©Hemdale
tags: 
  - Code
  - IA
author: wildagsx
---

## TL;DR
> 🔄 Implémentation du pattern ReAct (Reasoning and Acting) avec LangChain4j  
> 🏞️ Cas d'usage : génération d'images avec Stable Diffusion XL  
> 🔁 Utilisation du loop builder de [LangChain4j](https://docs.langchain4j.dev/) pour boucler entre agents  
> 🐙 Le [code source](https://gist.github.com/philippart-s/fdfbddbfb20bd795563dadad66315f05) du gist  

<br/>

# 📜 Précédemment dans le monde des agents...

Dans mon [précédent article]({site.url}/2026-01-22-ai-agents) sur les agents, je mentionnais le pattern ReAct sans pour autant entrer dans les détails de son implémentation.
Je vous propose, dans cet article, de découvrir comment implémenter ce pattern avec [LangChain4j](https://docs.langchain4j.dev/).

Je ne vais donc pas revenir sur les bases de ce qu'est un agent (pour ça, je vous invite à lire le précédent article).

## 🔄 Le pattern ReAct

ReAct, pour Reasoning and Acting, c'est l'idée d'introduire une **boucle de feedback** entre votre agent et son LLM.
L'objectif : maximiser la qualité de la réponse en permettant au LLM d'itérer sur son propre travail.

En résumé, le LLM ne se contente pas de répondre en une fois, il **raisonne**, **agit**, **observe** le résultat, et **recommence** si nécessaire.

![](./react-agents.png)

Pour rappel :
- L'agent (🤖) envoie la liste des outils (🛠️) et documents potentiellement utilisables par le LLM (📜), en plus de la demande (prompt)  
- Le LLM commence à élaborer son analyse (💭) pour répondre au mieux au prompt
- Si besoin, le LLM déclenche une nouvelle boucle (🔁) d'échange avec l'agent pour affiner son analyse
- Le LLM _estime_ avoir trouvé la réponse (✅), elle est renvoyée à l'utilisatrice / utilisateur (🥳)  
- Alternative : le LLM ne parvient pas à aller au bout de son raisonnement (❌)  

# 🏞️ Mon use case : la génération d'images avec Stable Diffusion XL

Pour illustrer le pattern ReAct, j'ai choisi un cas d'usage plutôt fun : la **génération d'images** avec Stable Diffusion XL.

> J'utilise le SDXL mis à disposition par OVHcloud via [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/stable-diffusion-xl/).

L'idée est simple : vous décrivez une image en langage naturel, et une boucle d'agents va :
1. **📝 Raffiner** votre description en un prompt optimisé pour Stable Diffusion XL
2. **🏞️ Générer** l'image avec ce prompt
3. **🔎 Critiquer** l'image générée (via un modèle de vision)
4. **🔄 Recommencer** si la critique n'est pas satisfaisante (score < 0.8)

# 🤖 Les agents

## 📝 PromptRefiner : l'expert en prompt SDXL

Le premier agent est un spécialiste du prompt engineering pour Stable Diffusion XL.
Son rôle : prendre la description de l'utilisateur (et éventuellement le feedback du critique) et produire un prompt optimisé.

Commençons par le record qui va porter le résultat de cet agent :

{|
```java
public record SdxlPrompts(String prompt, String negativePrompt) {
}
```
|}

Rien de fou, un prompt positif (ce qu'on veut) et un prompt négatif (ce qu'on ne veut pas).
Si vous n'êtes pas familier avec Stable Diffusion, le negative prompt permet d'exclure des artefacts indésirables (flou, mauvaise anatomie, etc.).

Et voici l'agent lui-même :

{|
```java
public interface PromptRefiner {
  @SystemMessage("""
      You are an expert prompt engineer for Stable Diffusion XL.
      Your job is to create or refine a detailed prompt and negative prompt for image generation.
      When given feedback from a critic, incorporate that feedback to improve the prompts.
      Respond with ONLY a JSON object (no markdown, no code fences) in this exact format:
      {"prompt": "detailed SDXL prompt here", "negativePrompt": "negative prompt here"}
      The prompt should be highly detailed with style, lighting, quality keywords.
      The negative prompt should exclude common artifacts and unwanted elements.
      """)
  @Agent(description = "Creates or refines Stable Diffusion XL prompts from a user request and optional critic feedback", outputKey = "sdxlPrompts")
  @UserMessage("""
      User request: "{{userRequest}}"
      Previous critic feedback: "{{feedback}}"
      Create optimized Stable Diffusion XL prompts for this request.
      """)
  SdxlPrompts refinePrompt(@V("userRequest") String userRequest, @V("feedback") String feedback);
}
```
|}

On retrouve l'annotation `@Agent` avec son `outputKey` qui permet de stocker le résultat dans le [contexte agentique](https://docs.langchain4j.dev/tutorials/agents#introducing-the-agenticscope).
Le point intéressant ici : l'agent prend en entrée le `feedback` du critique.
Lors de la première itération, ce feedback sera vide, mais lors des itérations suivantes, il contiendra les remarques du critique pour améliorer le prompt.

> Notez aussi que le retour est typé `SdxlPrompts` : LangChain4j se charge de parser la réponse JSON du LLM pour en faire un objet Java.

## 🏞️ ImageGenerator : le générateur d'images

Cet agent est un peu particulier car ce n'est **pas un agent basé sur un LLM**.
Il utilise tout de même un modèle de génération d'images (Stable Diffusion XL).

> Je n'utilise pas LangChain4j car StableDiffusion n'est pas intégré dans la liste des modèles supportés.
> Bien que j'aurais pu utiliser la compatibilité OpenAI, il me manquerait la partie "negative prompt" qui est essentielle pour la qualité de l'image générée.


{|
```java
public class ImageGenerator {

  @Agent(value = "Agent to create an image with Stable Diffusion XL given a prompt and a negative prompt.", outputKey = "imageBase64")
  public ImageContent generateImage(@V("sdxlPrompts") SdxlPrompts sdxlPrompts) throws IOException, InterruptedException {
    IO.println("🏞️ Generating image with SDXL prompts...");
    HttpRequest httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(System.getenv("OVH_AI_ENDPOINTS_SD_URL")))
        .POST(HttpRequest.BodyPublishers.ofString("""
                        {"prompt": "%s", 
                         "negative_prompt": "%s"}
                        """.formatted(sdxlPrompts.prompt, sdxlPrompts.negativePrompt)))
        .header("accept", "application/octet-stream")
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN"))
        .build();

    HttpResponse<byte[]> response = HttpClient.newHttpClient()
        .send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

    Files.write(Path.of("generated-image.jpeg"), response.body());
    return ImageContent.from(Base64.getEncoder().encodeToString(response.body()), "image/jpeg");
  }
}
```
|}

Vous l'avez vu, c'est une **classe** et non pas une interface.
L'annotation `@Agent` est posée directement sur la méthode, et LangChain4j comprend qu'il s'agit d'un agent "programmatique".
Il récupère le `SdxlPrompts` du contexte agentique (via `@V("sdxlPrompts")`), appelle l'API Stable Diffusion XL, sauvegarde l'image localement, et renvoie le résultat en base64 via un objet `ImageContent`.

## 🧑‍⚖️ VisionCritic : le critique d'art

Le dernier agent est celui qui va permettre de déterminer si l'image générée correspond bien à la demande de l'utilisateur.
Il utilise un **modèle de vision** (VLLM) pour analyser l'image générée et déterminer si elle correspond bien à la demande de l'utilisateur.

> Dans mon cas j'utilise [Qwen2.5-VL-72B-Instruct](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/qwen-2-5-vl-72b-instruct/) d'AI Endpoints.

Comme précédemment, afin de typer la réponse du critique, on définit un record `Critique` qui contient un score et un feedback textuel :

{|
```java
public record Critique(double score, String feedback) {
}
```
|}

Et l'agent critique :

{|
```java
public interface VisionCritic {
  @SystemMessage("""
                You are an expert image critic with deep knowledge of visual composition, aesthetics, and prompt adherence.
                You will receive a base64-encoded image and the original user request.
                Analyze how well the generated image matches the user's request.
                Respond with ONLY a JSON object (no markdown, no code fences) in this exact format (value are examples, not fixed):
                {"score": "", "feedback": ""}
                The score must be between 0.0 (terrible match) and 1.0 (perfect match).
                Be constructive in your feedback - explain what should be improved for the next iteration.
                """)
  @Agent(description = "Critiques a generated image against the original user request and provides a score and feedback", outputKey = "critique")
  @UserMessage("""
                Original user request: "{{userRequest}}"
                Please critique this image and provide a score and feedback.
                """)
  Critique critique(@V("userRequest") String userRequest, @UserMessage("{{imageBase64}}") ImageContent imageBase64);
}
```
|}

Le `@UserMessage("{{imageBase64}}")` sur le paramètre `ImageContent` permet de passer l'image générée directement au modèle de vision pour qu'il l'analyse.
Le critique va ensuite noter l'image et fournir un feedback qui sera, si nécessaire, réinjecté dans le `PromptRefiner` pour la prochaine itération.

# 🔁 La boucle ReAct : le loop builder

Maintenant qu'on a nos trois agents, il faut les assembler dans une boucle.
C'est là qu'intervient le [loop workflow](https://docs.langchain4j.dev/tutorials/agents#loop-workflow) de LangChain4j.

## 🏗️ Construction des agents

Avant de construire la boucle, il faut instancier nos agents avec `AgenticServices.agentBuilder()` :

{|
```java
PromptRefiner promptRefiner =  AgenticServices.agentBuilder(PromptRefiner.class)
    .chatModel(chatModel)
    .listener(new AgentListener() {
      @Override
      public void beforeAgentInvocation(AgentRequest request) {
        IO.println("📝 Invoking promptRefiner");
      }
    })
    .outputKey("sdxlPrompts")
    .build();

VisionCritic visionCritic = AgenticServices.agentBuilder(VisionCritic.class)
    .chatModel(visionModel)
    .listener(new AgentListener() {
      @Override
      public void beforeAgentInvocation(AgentRequest request) {
        IO.println("🧑‍⚖️ Invoking visionCritic");
      }
    })
    .outputKey("critique")
    .build();
```
|}

Chaque agent est construit avec son propre modèle (chat ou vision), un listener pour le debug, et son `outputKey` pour le contexte agentique.

> ℹ️ Notez que le `ImageGenerator` n'a pas besoin de builder puisqu'il n'utilise pas de LLM, un simple `new ImageGenerator()` suffit.

## 🔄 La boucle

Et voici le coeur du sujet, la construction de la boucle ReAct :

{|
```java
UntypedAgent agent = AgenticServices.loopBuilder()
    .maxIterations(3)
    .subAgents(promptRefiner, new ImageGenerator(), visionCritic)
    .testExitAtLoopEnd(true)
    .exitCondition((scope, loopCounter) -> {
      Critique critique = (Critique) scope.readState("critique");
      if (critique == null)
        return false;
      try {
        IO.println("🧑‍⚖️ Critic score: %s".formatted(critique.score));
        IO.println("📊 Feedback: %s".formatted(critique.feedback));

        scope.writeState("feedback", critique.feedback);

        return critique.score >= 0.8;
      } catch (Exception e) {
        IO.println("💥 Could not parse critic score, continuing loop 💥");
        return false;
      }
    })
    .build();
```
|}

Voyons de plus près ce qui se passe :

- **🔄 `maxIterations(3)`** : on limite à 3 itérations maximum. C'est une sécurité pour ne pas boucler indéfiniment (et ne pas exploser votre consommation de tokens 💸)
- **👮 `subAgents(promptRefiner, new ImageGenerator(), visionCritic)`** : l'ordre est important ! À chaque itération, les agents sont appelés dans cet ordre : refiner ➡️ generator ➡️ critic
- **🛑 `testExitAtLoopEnd(true)`** : la condition de sortie est évaluée à la fin de chaque itération (après que les trois agents ont fait leur travail, sinon c'est effectué après chaque exécution d'agent)
- **🔎 `exitCondition(...)`** : on récupère la critique du contexte agentique via `scope.readState("critique")`, on vérifie le score, et s'il est >= 0.8, on sort de la boucle. Sinon, on écrit le feedback dans le contexte (`scope.writeState("feedback", ...)`) pour que le `PromptRefiner` puisse l'utiliser à l'itération suivante

## 🚀 L'exécution

{|
```java
Object result = agent.invoke(Map.of(
    "userRequest", userRequest,
    "feedback", "No previous feedback - this is the first iteration.",
    "imageBase64", ""));
```
|}

On initialise le contexte avec la demande utilisateur, un feedback vide pour la première itération, et une image vide.

# 🤗 En conclusion

Et voilà pour la partie ReAct de votre développement agentique.
À vous de voir si vous préférez le mode supervisor comme décrit dans l'article précédent ou cette approche.
Dans les deux cas, vous allez donner de l'autonomie à votre agent pour itérer sur son travail et maximiser la qualité de ses réponses.
Attention donc à la consommation de vos tokens 😉.

Le code complet est disponible sous forme de gist [ici](https://gist.github.com/philippart-s/fdfbddbfb20bd795563dadad66315f05).

Si vous êtes arrivé.es jusque-là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
