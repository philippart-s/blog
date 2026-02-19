---
title: "🧑‍💻 Anthropic Skills, the ultimate weapon for the vibe-coder? 🤖"
description: Always two there are, no more, no less… A Master and an Apprentice… © Yoda
link: /2026-02-18-anthropic-skills-en
image: anthropic-skills-cover.jpg
figCaption: "@R.D. Smith"
tags: 
  - Code
  - IA
author: wildagsx
---

## 📖 TL;DR
- 🧠 Anthropic Skills are super system prompts that allow you to guide the model in a more precise and efficient way.
- 📚 https://resources.anthropic.com/hubfs/The-Complete-Guide-to-Building-Skill-for-Claude.pdf
- 📺 Stéphane Trebel's YouTube channel, aka [permacodeur](https://www.youtube.com/@permacodeur).
- 📖 Philippe Charrière's [skill](https://codeberg.org/ai-skills/methodical-dev)
- 🐙 My GitHub repository with my [Java skill](https://github.com/philippart-s/ai-skills/tree/main/java-dev).

<br/>

# 🤔 The starting point

If you've been following my [latest articles]({site.url}/2025-10-13-ai-as-artisan), I've been torn between working in AI and wondering what my job as a developer is going to become 🤯.
So spoiler, no, I'm not going to answer that question 🧐.
I'm simply going to share with you what made me recently use AI more in my everyday life as a developer.

So, why the change?

I owe it to two people:
 - [Stéphane Trebel](https://bsky.app/profile/permacodeur.fr)
 - [Philippe Charrière](https://bsky.app/profile/k33gorg.bsky.social)

And a little bit to myself for giving myself a kick in the butt to stop being so stubborn about AI and development 😅.

The thing is, I had convinced myself that AI was just a (bad) tool for developers 😡.
That coding was something you couldn't just make up, and I think unconsciously, I was using AI with pretty much every antipattern out there.
And yet, at my small level, I know how it works 😅.

But there I was, typing a vague prompt and trying to run the result...
With the conclusion: "well yeah, of course it doesn't work!".

A bit of bad faith, maybe 😆.

So what made me change my mind?
First, because I'm stubborn and I like to understand things I don't understand.
And seeing all this hype around code generation was bugging me.
Why couldn't I make it work?

Let me be clear right away, no, I haven't changed my mind: there's a lot of nonsense happening in companies.
People imagine too much that AI is the silver bullet that will solve all dev problems and at the same time replace this expensive cost that developers represent 🤑.
Anyway, I'm not going to participate in the noise of the pros / cons debate, that's not the point of this article.

Let's get back to why I changed my mind, or rather, why I now have less frustration using AI in my daily dev life 🧑‍💻.

# 🎬 The trigger: Stéphane Trebel, the fake beginner

If you don't know Stéphane, go check out what he does on his [YouTube channel](https://www.youtube.com/@permacodeur), I'm sure it will make you want to have him on your team 😉.
I'm going to talk about his streams, and more specifically about his AI / vibe coding sessions.
Stéphane decided a few weeks ago to jump into code generation with the vibe coding approach.

> From now on, I'll talk about code generation assistance or code generation. I think the term vibe coding is one of the reasons I didn't want to use generative AI for development 😡.
> It's been too much misused by people who don't know what they're talking about 🤡.

And in fake beginner mode: he starts from scratch and begins installing tools like [Claude code](https://claude.com/product/claude-code) or [Opencode](https://opencode.ai/).
Then tries approaches like [BMad](https://docs.bmad-method.org/) for example.

> Why fake beginner? Because Stéphane knows what he's doing and that's what makes it very effective when you follow his stream.

As he goes along, Stéphane takes his time.
He reads the answer, argues, challenges, tests, modifies... basically a real exchange that lasts several hours 💬.
And that's the big difference with everything we see where people promise us magic in 15-20 minutes. Spoiler: the only magic you get as a result is 💩 (sorry).
And that was my case: I wanted to try, but I wasn't giving the tool a fair chance.

## 👥 Pair programming as the way to use it

Watching Stéphane's streams, I understood that not only can you use these tools as simple tools, but above all, it has to be a dialogue.
More than the rubber duck mode or the "talking to a junior" approach that you can sometimes read about.

> I want to comment on that last sentence "as if you were talking to a junior": stop with this analogy, it's reductive and degrading for beginners and completely wrong.
> I can't count the number of things where young developers taught me something, me the old developer that I am.

In fact, I understood one thing: what works is the pair programming mode.
It became obvious when I watched Stéphane's streams.
You take turns between a code generator role (human or AI) and a reviewer role (human or AI).
Sure, the AI generates a bit more but, in my case, it evens out in the end.

And you get, in my opinion, all the advantages of pair programming:
 - 🗺️ having to explain what you want to do before doing it
 - 💬 the exchange of ideas
 - 🧑‍🏫 learning by example
 - 🔎 challenging a decision

And it works for both: the AI and you.
You, because if you reread what's generated, try to understand, you progress.
The AI because, as the exchanges go on, it will better understand what you want to do, and therefore generate better quality code.

On my side, I use [Opencode](https://opencode.ai/) which perfectly fits my needs.
One session per project.

I insist, but it's really this way of using it that made me switch to a useful use of code generation.

And so, the first observation: no, it's not 10 times faster ⚡️.
Like pair programming, it takes a bit of time at the beginning to get used to it.
But then, for some tasks, you're faster and above all the quality is really there, as well as the knowledge transfer and learning.

Compared to a real person, I was missing one thing: having to re-explain every time the type of pair coder I wanted (Java, the dev style,...).

# 🧠 The second game changer: Philippe Charrière and Anthropic skills

And that's where the second game changer arrived: [Philippe Charrière](https://bsky.app/profile/k33gorg.bsky.social) and his [Anthropic skills](https://claude.com/blog/skills).
Philippe is very active in generative AI and I can only recommend you follow his work (articles, code, talks, streams, ...).

The one I'm talking about starts from a [casual message](https://bsky.app/profile/k33gorg.bsky.social/post/3me6zw6klkk2d) about him sharing a Skill for [Claude code](https://claude.com/product/claude-code).
Oh, I had heard about it, but I hadn't made the effort to look into it yet (remember, code generation and me didn't get along) 🧐.
But this time, it's Philippe so I'm going to have a look anyway.

I'll let you go see in detail what skills are, but in short, they are super system prompts to give a real direction to your model.

And precisely, Philippe created a great skill to turn the model into a development companion: [methodological-dev](https://codeberg.org/ai-skills/methodical-dev).

And good news, it's compatible with [Opencode](https://opencode.ai/) 🥳.

To install a skill with [Opencode](https://opencode.ai/), you just have to create a `skills` directory inside the `.opencode` directory then create a directory with the name of your skill ⚙️.  
For example for Philippe's skill: `.opencode/skills/methodical-dev/SKILL.md` and there you go, your skill is ready to use.

## ☕️ My Java skill

So, I created my own for Java.
I also took the opportunity to add the fact that it corrects my English during our exchanges, like I would do with an English-speaking colleague 🏴󠁧󠁢󠁥󠁮󠁧󠁿.
I now have a co-dev who speaks Java like me and always suggests the same approach.

How is my skill structured?
It's pretty simple: I ask it to follow the same approach as Philippe's, but I added the use of Java 25, Quarkus and JBang.
To follow best practices, my skill has references, think of them as sub-skills that help avoid overloading the context.

> 📚 I recommend reading Anthropic's [documentation](https://resources.anthropic.com/hubfs/The-Complete-Guide-to-Building-Skill-for-Claude.pdf) which explains best practices for creating a skill.

Because, let's talk about it now, using skills will significantly increase the context of your prompt 💥.
So be careful to have the right model with the right subscription 💸.
For my part, I'm lucky enough to use [Opencode](https://opencode.ai/) with [AI Endpoints from OVHcloud](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/) and [Qwen3-Coder-30B-A3B-Instruct](https://www.ovhcloud.com/en/public-cloud/ai-endpoints/catalog/qwen-3-coder-30b-a3b-instruct/) without any limits 😇.

To see the details of my skill, it's here: https://github.com/philippart-s/ai-skills/tree/main/java-dev
Each reference allows to detail a part of the skill, for example for Java, I have the main guidelines in the skill then a reference that really details the best practices.
This reference will then be loaded by [Opencode](https://opencode.ai/) when needed.

## 🏆 The winning combo

With this combo skill + pair programming, I end up with an experience that, I think, allows me to better deal with code generation.

But then why not do it with a real human?
That's a very good question, and the answer is simple: I've never had the chance to work in a company that practices and believes in pair programming.
And now that I'm no longer on projects, it's even more complicated.
I'm not saying we should replace all pair programming pairs.
In cases where pair programming is not possible, I think it's a good alternative.
And in cases where you already do pair programming, why not give yourself the possibility of turning pair sessions into mob programming by adding generative AI 🤖.

# 🎓 What about learning?

So in the end, will generative AI replace us or augment us 🧐 ?
I have no idea. What I know is that I've stopped wanting to fight against it and at the same time being scared that it would put an end to a job I love, or on the other side telling myself it's essential and that I'm missing something huge.

I want to take this opportunity to talk about this aspect: learning.

We're starting to ask ourselves how our young developers will learn to code with generative AI.
Well, I don't claim to have the answer, but this pair programming approach seems like a good lead to me 🤝.
Just as we use pair programming to upskill beginners, we might as well use generative AI to do the same.

And above all, not turn beginners in a technology into "prompters" (I'm starting to get this kind of feedback).
I'm going to try to take on this beginner role in the coming weeks for the frontend part, which is clearly not my cup of tea.
I think I'll create a skill that clearly states that I'm a beginner and that the goal is to level up, I'll see if it holds up.

# 💡 In conclusion

This is the last piece of advice I'll allow myself to give you: don't just endure it, nothing forces you to use generative AI (except maybe your boss 😉) but if you're interested in it, it can only be an extra asset for you.
Either you'll know why you don't want to use it, or you'll know how to use it to save time and above all to progress.

And also a small message to all those who swear only by AI: have some perspective too, and don't impose your ideas as absolute truths on those who have doubts; it only increases their doubts 🤔.

If you've made it this far, thank you for reading and if there are any typos don't hesitate to open an [issue or PR](https://github.com/philippart-s/blog) 😊.

