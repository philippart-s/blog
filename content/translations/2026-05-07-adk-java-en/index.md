---
title: "🤖 Discovering Google's ADK Java"
description: "There is no spoon. ©The Oracle"
link: /2026-05-07-adk-java-en
image: cover.jpg
figCaption: © Warner Bros. Pictures
tags:
  - Code
  - IA
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-05-07-adk-java) 🇫🇷.

## TL;DR
> 🤖 Discovering **[ADK Java](https://github.com/google/adk-java)**, Google's Agent Development Kit, in Java  
> 🌉 Using **[LangChain4j](https://docs.langchain4j.dev/)** as a bridge to **[OVHcloud AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/)** models  
> 🔁 Implementing a **LoopAgent** (ReAct pattern) with a custom (non-LLM) agent calling **Stable Diffusion XL**  
> 🐙 The [full code](https://gist.github.com/philippart-s/2ee45a9b69276395ed7ca4b30d746074)

<br/>

# 📜 Previously, in the world of agents...

In my [previous article]({site.url}/2026-02-22-react-agent-en) I introduced the ReAct pattern implemented with **LangChain4j**.
The use case: generate an image with Stable Diffusion XL, have it critiqued by a vision model, and loop until the critic is happy.

Today I'd like to **redo the exact same exercise** with another framework: **[ADK Java](https://github.com/google/adk-java)** (Agent Development Kit), Google's agentic framework.

> ℹ️ If you're not familiar with the ReAct pattern, I invite you to read [my previous article]({site.url}/2026-02-22-react-agent-en) before going further. ℹ️

# 🤔 Why ADK Java?

ADK was released on the Python side a few months ago and made quite some noise.
The Java version, more recent, came with a simple promise: **the same concepts, in Java 🥳**.
Because yes, by default ADK pushes Google's models.
But there is an official module **`google-adk-langchain4j`** that lets you plug in any model supported by LangChain4j.

And so... 🥁... we can use it with [OVHcloud AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) 🎉.

ADK's concepts are pretty standard if you've already played with other agentic frameworks:
- **`LlmAgent`**: an agent whose logic is carried by an LLM (with a prompt, tools, etc.)
- **`BaseAgent`**: a custom agent where you write the logic yourself (useful for API calls that don't go through an LLM)
- **`LoopAgent`**, **`SequentialAgent`**, **`ParallelAgent`**: orchestration agents to chain / loop / parallelize
- **`Session`** and **session state**: a context shared between agents (the equivalent of LangChain4j's `AgenticScope`)
- **`Tool`**: functions callable by the LLM (function calling)

# 🏞️ The use case (same as before)

As a reminder, I want to:
1. **📝 Refine** a user description into SDXL prompts (positive + negative)
2. **🏞️ Generate** an image with Stable Diffusion XL
3. **🧑‍⚖️ Critique** the result with a vision model and give a score
4. **🔁 Repeat** if the score is too low, **🛑 exit** otherwise

# 🧑‍💻 Let's code!

To keep things simple and self-contained, I'm using **[JBang](https://www.jbang.dev/)**: a single `.java` file, _all batteries included_ 🙃.

```java
//DEPS com.google.adk:google-adk:1.0.0
//DEPS com.google.adk:google-adk-langchain4j:1.0.0
//DEPS dev.langchain4j:langchain4j-open-ai:1.12.2
//DEPS ch.qos.logback:logback-classic:1.5.6
//FILES logback.xml
```

Three dependencies: ADK Java, the LangChain4j adapter for ADK, and LangChain4j's OpenAI integration (compatible with AI Endpoints).

## 🔌 Plugging AI Endpoints via LangChain4j

ADK works with its own model abstraction, so we need a small adaptation layer.

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

We first create a regular LangChain4j `ChatModel` (just like in my previous posts).
Then we create an ADK `LangChain4j` wrapper that lets us use the OVHcloud model.

I do the same for the vision model (a VLM, in my case [Qwen2.5-VL-72B-Instruct](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/qwen-2-5-vl-72b-instruct/)).

## 📝 PromptRefiner: a classic LlmAgent

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

A few interesting points:
- **`instruction(...)`**: this is the system prompt. Note the **`{{sdxl_prompts}}`** and **`{{critic_feedback}}`** placeholders, they reference ADK's session state. The framework injects the values automatically before calling the LLM.
- **`outputKey("sdxl_prompts")`**: the agent's output is stored in session state under this key. Exactly like LangChain4j's `outputKey` 👌.
- **`includeContents(NONE)`**: we don't replay the message history. Each call is independent, the context only comes from the prompt and the state. That's intentional here since everything is driven by the state.

> ⚠️ Small disappointment compared to LangChain4j: ADK Java does **not** automatically deserialize LLM output into a Java record (like a `SdxlPrompts`).
> I tried with `outputSchema(...)`, which forces a JSON `response_format` on the provider side, but the result still ends up as a `Map<String, Object>` in the state.
> For this example I just use a `PROMPT: ... / NEGATIVE_PROMPT: ...` text format and parse it by hand with a regex. ⚠️

## 🏞️ SdxlImageGenerator: a custom BaseAgent

This is where it gets interesting.
SDXL is not an LLM, so I have nothing to feed an `LlmAgent`.
Luckily, ADK provides `BaseAgent` to write a **purely programmatic** agent.

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

A few things to note:
- **`runAsyncImpl`** returns a **`Flowable<Event>`** (RxJava 3). A bit confusing if you're not used to reactive, but it lets you chain real async (here `HttpClient.sendAsync` via `Flowable.fromCompletionStage`).
- **`runLiveImpl`** is mandatory (abstract method) but we don't need it outside of "live" mode (bidirectional streaming), so we throw an `UnsupportedOperationException`.
- The agent _reads_ `sdxl_prompts` from the state (`ctx.session().state().get(...)`) and _writes_ `generation_result` into the state so the critic can use it.
- The returned `Event` is what the parent `LoopAgent` will see and what gets propagated in the event stream.

## 🧑‍⚖️ Critic: an LlmAgent + an exit tool

The critic agent receives the prompts and the generation result, gives a score and writes feedback.
**If satisfied** (score ≥ 0.87 in my case), it must trigger the loop exit.

ADK ships a ready-made tool for this: **`ExitLoopTool.INSTANCE`**.
You just stick it in `.tools(...)` and ask the LLM to call it when it's happy.

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

> ⚠️ **Mind the model**: for this to work, you need a model that does **function calling**.
> With a VLM that can't call functions, you'd loop until `maxIterations` every single time 😬.

> ℹ️ I also tried asking the model to **emit the SCORE/FEEDBACK AND call `exit_loop` in the same response**.
> In theory it's possible.
> In practice, on the model I was using, it picked one or the other.
> So I kept the "either you write text, or you call the tool" approach. ℹ️

## 🔁 The LoopAgent

And here we are, gluing it all together:

{|
```java
LoopAgent loopAgent = LoopAgent.builder()
    .name("ImageGenLoop")
    .maxIterations(3)
    .subAgents(promptRefiner, imageGenerator, criticAgent)
    .build();
```
|}

On each iteration, ADK calls the sub-agents **in order**: refiner ➡️ generator ➡️ critic.

The loop stops:
- either because `maxIterations` is reached (a token-budget safety net 💸)
- or because a sub-agent emits an `EventActions.escalate(true)`

## 🚀 Running it

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

A few details:
- **The initial state** must contain all the keys referenced in the agents' `instruction(...)` blocks (`sdxl_prompts`, `critic_feedback`, `generation_result`).
  Otherwise ADK shouts a `Context variable not found` on the first call (template injection is strict).
  Hence the placeholder values 😅.
- **The event stream** gives you full control: you can filter by `author`, read the `Content`, inspect `functionCalls`...
- **The score that triggers the exit**: on the iteration that calls `exit_loop`, the event contains only the function call, no text.
  So the SCORE/FEEDBACK that triggered the exit isn't written into the state (the previous one stays).
  If you want to capture it precisely, write your own `FunctionTool exitLoop(double score, String feedback)` instead of using `ExitLoopTool.INSTANCE`.

# 🤗 Wrapping up

ADK Java is a young framework but already very usable, with a real consistency on its concepts.
The fact that it cleanly interfaces with **LangChain4j** is very handy: it means we can use it with **AI Endpoints** without sacrificing anything.

Many thanks to **Guillaume LAFORGE** for his help with the code and getting started with ADK 🤗.

As usual in this ecosystem, things move fast, so the code in this article may already feel old by the time you read it 😅.

The full code is available as a [gist](https://gist.github.com/philippart-s/2ee45a9b69276395ed7ca4b30d746074).

If you've made it this far, thanks for reading, and if you spot typos don't hesitate to open an [issue or PR](https://github.com/philippart-s/blog) 😊.
