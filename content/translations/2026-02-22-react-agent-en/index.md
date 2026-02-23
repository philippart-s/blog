---
title: "🔄 Teaching your agent to think with the ReAct pattern and LangChain4j 🤖"
description: "I'm not a man. I'm something that evolved. @Skynet"
link: /2026-02-22-react-agent-en
image: cover.jpg
figCaption: ©Hemdale
tags: 
  - Code
  - IA
author: wildagsx
---

## TL;DR
> 🔄 Implementation of the ReAct (Reasoning and Acting) pattern with LangChain4j  
> 🏞️ Use case: image generation with Stable Diffusion XL  
> 🔁 Using the loop builder from [LangChain4j](https://docs.langchain4j.dev/) to loop between agents  
> 🐙 The [source code](https://gist.github.com/philippart-s/fdfbddbfb20bd795563dadad66315f05) gist  

<br/>

# 📜 Previously in the world of agents...

In my [previous article]({site.url}/2026-01-22-ai-agents) about agents, I mentioned the ReAct pattern without really going into the details of its implementation.
In this article, I suggest we discover how to implement this pattern with [LangChain4j](https://docs.langchain4j.dev/).

I'm not going to go back over the basics of what an agent is (for that, I invite you to read the previous article).

## 🔄 The ReAct pattern

ReAct, which stands for Reasoning and Acting, is the idea of introducing a **feedback loop** between your agent and its LLM.
The goal: maximise the quality of the response by allowing the LLM to iterate on its own work.

In short, the LLM doesn't just answer in one shot, it **reasons**, **acts**, **observes** the result, and **starts over** if needed.

![](./react-agents.png)

As a reminder:
- The agent (🤖) sends the list of tools (🛠️) and documents potentially usable by the LLM (📜), on top of the request (prompt)  
- The LLM starts building its analysis (💭) to best answer the prompt
- If needed, the LLM triggers a new exchange loop (🔁) with the agent to refine its analysis
- The LLM _considers_ it has found the answer (✅), it's sent back to the user (🥳)  
- Alternative: the LLM fails to complete its reasoning (❌)  

# 🏞️ My use case: image generation with Stable Diffusion XL

To illustrate the ReAct pattern, I chose a rather fun use case: **image generation** with Stable Diffusion XL.

> I'm using the SDXL provided by OVHcloud through [AI Endpoints](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/stable-diffusion-xl/).

The idea is simple: you describe an image in natural language, and a loop of agents will:
1. **📝 Refine** your description into an optimised prompt for Stable Diffusion XL
2. **🏞️ Generate** the image with this prompt
3. **🔎 Critique** the generated image (through a vision model)
4. **🔄 Start over** if the critique is not satisfactory (score < 0.8)

# 🤖 The agents

## 📝 PromptRefiner: the SDXL prompt expert

The first agent is a specialist in prompt engineering for Stable Diffusion XL.
Its role: take the user's description (and possibly the critic's feedback) and produce an optimised prompt.

Let's start with the record that will hold this agent's result:

{|
```java
public record SdxlPrompts(String prompt, String negativePrompt) {
}
```
|}

Nothing fancy, a positive prompt (what we want) and a negative prompt (what we don't want).
If you're not familiar with Stable Diffusion, the negative prompt allows you to exclude unwanted artefacts (blur, bad anatomy, etc.).

And here's the agent itself:

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

We find the `@Agent` annotation with its `outputKey` which stores the result in the [agentic context](https://docs.langchain4j.dev/tutorials/agents#introducing-the-agenticscope).
The interesting point here: the agent takes the critic's `feedback` as input.
On the first iteration, this feedback will be empty, but on the following iterations, it will contain the critic's remarks to improve the prompt.

> Also note that the return type is `SdxlPrompts`: LangChain4j takes care of parsing the LLM's JSON response into a Java object.

## 🏞️ ImageGenerator: the image generator

This agent is a bit special because it's **not an LLM-based agent**.
It still uses an image generation model though (Stable Diffusion XL).

> I'm not using LangChain4j because StableDiffusion is not included in the list of supported models.
> Even though I could have used the OpenAI compatibility, I would have been missing the "negative prompt" part which is essential for the quality of the generated image.


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

As you can see, it's a **class** and not an interface.
The `@Agent` annotation is placed directly on the method, and LangChain4j understands that it's a "programmatic" agent (without LLM).
It retrieves the `SdxlPrompts` from the agentic context (via `@V("sdxlPrompts")`), calls the Stable Diffusion XL API, saves the image locally, and returns the result in base64 through an `ImageContent` object.

## 🧑‍⚖️ VisionCritic: the art critic

The last agent is the one that will determine whether the generated image matches the user's request.
It uses a **vision model** (VLLM) to analyse the generated image and determine if it matches the user's request.

> In my case, I'm using [Qwen2.5-VL-72B-Instruct](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/qwen-2-5-vl-72b-instruct/) from AI Endpoints.

As before, to type the critic's response, we define a `Critique` record that contains a score and a textual feedback:

{|
```java
public record Critique(double score, String feedback) {
}
```
|}

And the critic agent:

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

The `@UserMessage("{{imageBase64}}")` on the `ImageContent` parameter allows passing the generated image directly to the vision model for analysis.
The critic will then score the image and provide feedback that will, if needed, be injected back into the `PromptRefiner` for the next iteration.

# 🔁 The ReAct loop: the loop builder

Now that we have our three agents, we need to assemble them into a loop.
That's where the [loop workflow](https://docs.langchain4j.dev/tutorials/agents#loop-workflow) from LangChain4j comes in.

## 🏗️ Building the agents

Before building the loop, we need to instantiate our agents with `AgenticServices.agentBuilder()`:

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

Each agent is built with its own model (chat or vision), a listener for debugging, and its `outputKey` for the agentic context.

> ℹ️ Note that the `ImageGenerator` doesn't need a builder since it doesn't use an LLM, a simple `new ImageGenerator()` is enough.

## 🔄 The loop

And here's the heart of the matter, building the ReAct loop:

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

Let's take a closer look at what's going on:

- **🔄 `maxIterations(3)`**: we limit to 3 iterations maximum. It's a safety measure to avoid looping forever (and to not blow up your token consumption 💸)
- **👮 `subAgents(promptRefiner, new ImageGenerator(), visionCritic)`**: the order matters! At each iteration, the agents are called in this order: refiner ➡️ generator ➡️ critic
- **🛑 `testExitAtLoopEnd(true)`**: the exit condition is evaluated at the end of each iteration (after all three agents have done their job, otherwise it's checked after each individual agent execution)
- **🔎 `exitCondition(...)`**: we retrieve the critique from the agentic context via `scope.readState("critique")`, we check the score, and if it's >= 0.8, we exit the loop. Otherwise, we write the feedback into the context (`scope.writeState("feedback", ...)`) so that the `PromptRefiner` can use it on the next iteration

## 🚀 The execution

{|
```java
Object result = agent.invoke(Map.of(
    "userRequest", userRequest,
    "feedback", "No previous feedback - this is the first iteration.",
    "imageBase64", ""));
```
|}

We initialise the context with the user's request, an empty feedback for the first iteration, and an empty image.

# 🤗 In conclusion

And that's it for the ReAct part of your agentic development.
It's up to you to decide whether you prefer the supervisor mode as described in the previous article or this approach.
In both cases, you'll give your agent the autonomy to iterate on its work and maximise the quality of its responses.
So watch out for your token consumption 😉.

The full code is available as a gist [here](https://gist.github.com/philippart-s/fdfbddbfb20bd795563dadad66315f05).

If you've made it this far, thank you for reading and if there are any typos don't hesitate to open an [issue or PR](https://github.com/philippart-s/blog) 😊.
