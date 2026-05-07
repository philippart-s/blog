---
title: "🤖 Découverte d'ADK Java de Google"
description: "There is no spoon. ©The Oracle"
link: /2026-05-07-adk-java
image: cover.jpg
figCaption: © Warner Bros. Pictures
tags:
  - Code
  - IA
author: wildagsx
---

🏴󠁧󠁢󠁥󠁮󠁧󠁿 You can find the English version of this article [here]({site.url}2026-05-07-adk-java-loop-agent-en) 🏴󠁧󠁢󠁥󠁮󠁧󠁿.

## TL;DR
> 🤖 Découverte d'**[ADK Java](https://github.com/google/adk-java)**, l'Agent Development Kit de Google, en Java  
> 🌉 Utilisation de **[LangChain4j](https://docs.langchain4j.dev/)** comme pont vers les modèles d'**[OVHcloud AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/)**  
> 🔁 Implémentation d'un **LoopAgent** (pattern ReAct) avec un agent custom (non-LLM) pour appeler **Stable Diffusion XL**  
> 🐙 Le [code complet](https://gist.github.com/philippart-s/2ee45a9b69276395ed7ca4b30d746074)

<br/>

# 📜 Précédemment dans le monde des agents...

Dans mon [précédent article]({site.url}/2026-02-22-react-agent) je vous présentais le pattern ReAct implémenté avec **LangChain4j**.
Le use case : générer une image avec Stable Diffusion XL, la faire critiquer par un modèle de vision, et boucler tant que le critique n'est pas content.

Je vous propose aujourd'hui de **refaire exactement le même exercice**, mais avec un autre framework : **[ADK Java](https://github.com/google/adk-java)** (Agent Development Kit), le framework agentique de Google.

> ℹ️ Si vous n'êtes pas familier avec le pattern ReAct, je vous invite à lire [mon précédent article]({site.url}/2026-02-22-react-agent) avant de continuer. ℹ️

# 🤔 Pourquoi ADK Java ?

ADK est sorti côté Python il y a quelques mois et a fait pas mal de bruit.
La version Java, plus récente, est arrivée avec une promesse simple : **les mêmes concepts, en Java 🥳**.
Parce que oui, par défaut, ADK pousse les modèles de Google.
Mais il y a un module officiel **`google-adk-langchain4j`** qui permet de brancher n'importe quel modèle supporté par LangChain4j.

Et donc... 🥁... on peut l'utiliser avec [OVHcloud AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) 🎉.

Les concepts d'ADK sont assez classiques si vous avez déjà joué avec d'autres frameworks agentiques :
- **`LlmAgent`** : un agent dont la logique est portée par un LLM (avec un prompt, des outils, etc.)
- **`BaseAgent`** : un agent custom où vous écrivez la logique vous-même (utile pour des appels d'API qui ne passent pas par un LLM)
- **`LoopAgent`**, **`SequentialAgent`**, **`ParallelAgent`** : des agents d'orchestration pour chaîner / boucler / paralléliser
- **`Session`** et **session state** : un contexte partagé entre les agents (l'équivalent de l'`AgenticScope` de LangChain4j)
- **`Tool`** : des fonctions appelables par le LLM (function calling)

# 🏞️ Le use case (le même qu'avant)

Pour rappel, je veux :
1. **📝 Raffiner** une description utilisateur en prompts SDXL (positif + négatif)
2. **🏞️ Générer** une image avec Stable Diffusion XL
3. **🧑‍⚖️ Critiquer** le résultat avec un modèle de vision et donner un score
4. **🔁 Recommencer** si le score est trop bas, **🛑 sortir** sinon

# 🧑‍💻 Du code !

Pour faire simple et auto-portant, j'utilise **[JBang](https://www.jbang.dev/)** : un seul fichier `.java` _all batteries included_ 🙃.

```java
//DEPS com.google.adk:google-adk:1.0.0
//DEPS com.google.adk:google-adk-langchain4j:1.0.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.12.2
//DEPS ch.qos.logback:logback-classic:1.5.6
//FILES logback.xml
```

Trois dépendances : ADK Java, l'adaptateur LangChain4j pour ADK, et l'intégration OpenAI de LangChain4j (compatible avec AI Endpoints).

## 🔌 Brancher AI Endpoints via LangChain4j

ADK travaille avec sa propre abstraction de modèle, donc il faut une petite couche d'adaptation.

{|
```java
ChatModel l4jChatModel = OpenAiChatModel.builder()
    .apiKey(System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN"))
    .baseUrl(System.getenv("OVH_AI_ENDPOINTS_MODEL_URL"))
    .modelName(System.getenv("OVH_AI_ENDPOINTS_MODEL_NAME"))
    .temperature(0.0)
    .build();

LangChain4j adkChatModel = LangChain4j.builder()
    .chatModel(l4jChatModel)
    .modelName(System.getenv("OVH_AI_ENDPOINTS_MODEL_NAME"))
    .build();
```
|}

On crée d'abord un `ChatModel` LangChain4j classique (comme dans mes posts précédents).
Puis on crée un wrapper ADK `LangChain4j`  qui va permettre d'utiliser le modèle OVHcloud.

Je fais la même chose pour le modèle de vision (un VLLM, dans mon cas [Qwen2.5-VL-72B-Instruct](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/qwen-2-5-vl-72b-instruct/)).

## 📝 PromptRefiner : un LlmAgent classique

{|
```java
LlmAgent promptRefiner = LlmAgent.builder()
    .model(adkChatModel)
    .name("PromptRefiner")
    .description("Refines SDXL prompts based on critic feedback")
    .instruction("""
        You are an expert prompt engineer for Stable Diffusion XL.
        Your job is to refine the prompts based on the critic's feedback.

        Previous prompts: {{sdxl_prompts}}
        Critic feedback: {{critic_feedback}}

        Improve the Stable Diffusion XL prompts based on the feedback.
        The prompt should be highly detailed with style, lighting, quality keywords.
        The negative prompt should exclude common artifacts and unwanted elements.

        Output your response as a clear text with exactly this format:
        PROMPT: <your detailed prompt>
        NEGATIVE_PROMPT: <your negative prompt>
        """)
    .outputKey("sdxl_prompts")
    .includeContents(NONE)
    .build();
```
|}

Plusieurs points intéressants :
- **`instruction(...)`** : c'est le system prompt. Notez les **`{{sdxl_prompts}}`** et **`{{critic_feedback}}`** qui sont des références au session state d'ADK. Le framework va injecter automatiquement les valeurs avant d'appeler le LLM.
- **`outputKey("sdxl_prompts")`** : la sortie de l'agent est stockée dans le session state sous cette clé. C'est exactement comme l'`outputKey` de LangChain4j 👌.
- **`includeContents(NONE)`** : on ne réinjecte pas l'historique des messages. Chaque appel est indépendant, le contexte vient uniquement du prompt et du state. C'est volontaire ici puisque tout est piloté par le state.

> ⚠️ Petite déception par rapport à LangChain4j : ADK Java ne fait **pas** de désérialisation automatique de la sortie LLM vers un record Java (comme un `SdxlPrompts`).
> J'ai testé avec `outputSchema(...)`, ça force le `response_format` JSON côté provider, mais le résultat reste un `Map<String, Object>` dans le state.
> Pour cet exemple, je me contente d'un format texte `PROMPT: ... / NEGATIVE_PROMPT: ...` que je parse à la main avec une regex. ⚠️

## 🏞️ SdxlImageGenerator : un BaseAgent custom

C'est ici que ça devient intéressant.
SDXL n'est pas un LLM, je n'ai donc rien à donner à manger à un `LlmAgent`.
Heureusement, ADK propose `BaseAgent` qui permet d'écrire un agent **purement programmatique**.

{|
```java
class SdxlImageGenerator extends BaseAgent {

  private static final Pattern PROMPT_RE = Pattern.compile(
      "(?is)\\bPROMPT\\s*:\\s*(.+?)(?=\\bNEGATIVE[_ ]?PROMPT\\s*:|$)");
  private static final Pattern NEGATIVE_RE = Pattern.compile(
      "(?is)\\bNEGATIVE[_ ]?PROMPT\\s*:\\s*(.+?)$");

  SdxlImageGenerator() {
    super("SdxlImageGenerator",
        "Calls the SDXL endpoint to generate an image from prompts in session state",
        List.of(), null, null);
  }

  @Override
  protected Flowable<Event> runLiveImpl(InvocationContext ctx) {
    return Flowable.error(new UnsupportedOperationException("runLive is not supported"));
  }

  @Override
  protected Flowable<Event> runAsyncImpl(InvocationContext ctx) {
    var sdxlPrompts = (String) ctx.session().state().get("sdxl_prompts");
    var prompt = extract(PROMPT_RE, sdxlPrompts);
    var negativePrompt = extract(NEGATIVE_RE, sdxlPrompts);

    var httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(System.getenv("OVH_AI_ENDPOINTS_SD_URL")))
        .POST(HttpRequest.BodyPublishers.ofString("""
            {"prompt": "%s", "negative_prompt": "%s"}
            """.formatted(prompt, negativePrompt)))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + System.getenv("OVH_AI_ENDPOINTS_ACCESS_TOKEN"))
        .timeout(Duration.ofMinutes(2))
        .build();

    return Flowable
        .fromCompletionStage(HttpClient.newHttpClient()
            .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray()))
        .map(response -> {
          if (response.statusCode() != 200) {
            return resultEvent(ctx, "❌ SDXL returned error status " + response.statusCode());
          }
          Files.write(Path.of("generated-loop-image.jpeg"), response.body());
          return resultEvent(ctx, "✅ Image generated and saved");
        })
        .onErrorReturn(t -> resultEvent(ctx, "❌ Failed: " + t.getMessage()));
  }
}
```
|}

Quelques points à noter :
- **`runAsyncImpl`** retourne un **`Flowable<Event>`** (RxJava 3). C'est un peu déroutant si vous n'êtes pas habitués au reactif, mais ça permet d'enchaîner du vrai async (ici `HttpClient.sendAsync`  via `Flowable.fromCompletionStage`).
- **`runLiveImpl`** est obligatoire (méthode abstraite) mais on n'en a pas l'usage hors mode "live" (streaming bidirectionnel), on lève donc une `UnsupportedOperationException`.
- L'agent _récupère_ `sdxl_prompts` depuis le state (`ctx.session().state().get(...)`) et _positionne_ le `generation_result` dans le state pour que le critique puisse l'utiliser.
- L'`Event` retourné est ce qui sera vu par le `LoopAgent` parent et propagé dans le stream d'événements.

## 🧑‍⚖️ Critic : un LlmAgent + un outil de sortie

L'agent critique reçoit les prompts et le résultat de génération, attribue un score et rédige un feedback.
**S'il est satisfait** (score ≥ 0.87 dans mon cas), il doit déclencher la sortie de la boucle.

ADK fournit un outil tout fait pour ça : **`ExitLoopTool.INSTANCE`**.
Il suffit de le coller en `.tools(...)` et de demander au LLM de l'appeler quand il est content.

{|
```java
LlmAgent criticAgent = LlmAgent.builder()
    .name("Critic")
    .model(adkVisionModel)
    .description("Evaluates the image generation result and provides a score and feedback")
    .instruction("""
        You are an expert image critic.
        SDXL prompts used: {{sdxl_prompts}}
        Generation result: {{generation_result}}

        Provide:
        1. A SCORE between 0.0 and 1.0
        2. Constructive FEEDBACK on what should be improved

        Format your response EXACTLY as:
        SCORE: <value>
        FEEDBACK: <your feedback>

        IMPORTANT — Loop control:
        If your SCORE is >= 0.87, you MUST call the `exit_loop` function
        to stop the refinement loop. Otherwise, do not call any function
        and just output the SCORE/FEEDBACK so the next iteration can refine
        the prompts.
        """)
    .tools(ExitLoopTool.INSTANCE)
    .outputKey("critic_feedback")
    .includeContents(NONE)
    .build();
```
|}

> ⚠️ **Attention au modèle** : pour que ça marche, il faut un modèle qui sait faire du **function calling**.
> Avec un VLM qui ne sait pas appeler de fonctions, vous boucleriez jusqu'à `maxIterations` à chaque fois 😬.

> ℹ️ J'ai aussi tenté de demander au modèle d'**émettre le SCORE/FEEDBACK ET d'appeler `exit_loop` dans la même réponse**.
> En théorie c'est possible.
> En pratique, sur le modèle que j'utilisais, il choisissait soit l'un soit l'autre.
> J'ai donc gardé la version "ou bien tu écris du texte, ou bien tu appelles l'outil". ℹ️

## 🔁 Le LoopAgent

Et voilà, on assemble tout ça :

{|
```java
LoopAgent loopAgent = LoopAgent.builder()
    .name("ImageGenLoop")
    .maxIterations(3)
    .subAgents(promptRefiner, imageGenerator, criticAgent)
    .build();
```
|}

À chaque itération, ADK appelle les sous-agents **dans l'ordre** : refiner ➡️ generator ➡️ critic.

La boucle s'arrête :
- soit parce que `maxIterations` est atteint (sécurité 💸)
- soit parce qu'un sous-agent émet un `EventActions.escalate(true)`

## 🚀 Exécution

{|
```java
void main() {
  InMemoryRunner runner = new InMemoryRunner(loopAgent, "Image generator");
  Content userMessage = Content.fromParts(
      Part.fromText("A red cat sitting on a windowsill, watching the rain outside"));

  Map<String, Object> state = Map.of(
      "sdxl_prompts", userMessage.text(),
      "critic_feedback", "user prompt, not yet analyzed by critic agent",
      "generation_result", "no image generated yet"
  );
  Session session = runner.sessionService()
      .createSession("Image generator", "user", state, "session")
      .blockingGet();

  Flowable<Event> eventStream = runner.runAsync("user", session.id(), userMessage);

  eventStream.blockingForEach(event -> {
    if (!"Critic".equals(event.author())) return;
    event.content()
        .map(Content::text)
        .filter(t -> t != null && !t.isBlank())
        .ifPresent(t -> IO.println("📝 Critic:\n" + t));
    if (!event.functionCalls().isEmpty()) {
      IO.println("🛑 Critic called exit_loop — score threshold reached.");
    }
  });
}
```
|}

Quelques détails :
- **Le state initial** doit contenir toutes les clés référencées dans les `instruction(...)` des agents (`sdxl_prompts`, `critic_feedback`, `generation_result`).
  Sinon, ADK râle avec un `Context variable not found` au premier appel (l'injection de template est stricte).
  D'où les valeurs "placeholder" 😅.
- Le **stream d'événements** vous donne la main : on peut filtrer par `author`, lire le `Content`, voir les `functionCalls`...
- **Le score qui déclenche la sortie** : sur l'itération qui appelle `exit_loop`, l'event ne contient que la function call, pas de texte.
  Le SCORE/FEEDBACK qui a déclenché la sortie n'est donc pas écrit dans le state (c'est le précédent qui y reste).
  Si vous voulez le récupérer précisément, il faut faire votre propre `FunctionTool exitLoop(double score, String feedback)` au lieu d'utiliser `ExitLoopTool.INSTANCE`.

# 🤗 En conclusion

ADK Java est un framework jeune mais déjà très exploitable, avec une vraie cohérence sur les concepts.
Le fait qu'il s'interface proprement avec **LangChain4j** est très pratique : ça veut dire qu'on peut l'utiliser avec **AI Endpoints** sans rien sacrifier.

Merci à **Guillaume LAFORGE** pour son aide sur le code et la prise en main d'ADK 🤗.

Comme d'habitude dans cet écosystème, ça bouge vite, donc le code de cet article aura peut-être déjà des rides quand vous le lirez 😅.

Le code complet de l'exemple est disponible sous forme de [gist](https://gist.github.com/philippart-s/2ee45a9b69276395ed7ca4b30d746074).

Si vous êtes arrivé.es jusque-là, merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
