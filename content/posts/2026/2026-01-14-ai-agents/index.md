---
title: "🤖L'IA et ses agents, comment ça marche ? 🔀"
description: Mr. Anderson, welcome back. We missed you. © Agent Smith
link: /2026-01-14-ai-agents
image: agent-smith-with-agents-in-the-matrix.jpg
figCaption: © Warner Bros. Pictures
tags: 
  - Code
  - IA
author: wildagsx
---

## 📖 TL;DR
> 🤖 Les agents sont une des dernières nouveautés dans le domaine de l'IA générative.
> 🔀 Il existe plusieurs moyens de les orchestrer / chainer
> 🧑‍💻 Le code pour illustrer les concepts est en Java (Quarkus / LangChain4j)
> 🐙 Le [code source](https://github.com/philippart-s/blog)

<br/>

# 📜 Un peu de documentation

On a beaucoup de documentation disponible sur les agents, je vous propose de commencer par ces dernières :
 - 📜 l'article d'Anthropic [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)
 - 📚 la documentation de LangChain4j sur les [agents](https://docs.langchain4j.dev/tutorials/agents#agentic-systems)
 - 🎙️ l'excellent talk de Guillaume LAFORGE à TADX : [Agents intelligents, la nouvelle frontière des LLMs](https://youtu.be/0nqxN3fyg1E)
 - 📝 de manière plus générale, les [nombreux blog posts](https://glaforge.dev/) de Guillaume 🙂.

Et bien sûr, plein d'autres ressources comme les [replays de Devoxx France](https://www.youtube.com/@DevoxxFRvideos) par exemple.

# 🧑‍🏫 Oui mais toi Stéphane, c'est quoi ta définition d'un agent ?

Ah mais nous y voilà !
En toute honneteté, j'ai mis pas mal de temps à comprendre ce qu'était un agent, et surtout en quoi c'était différent d'un simple appel à un LLM.
Ce qui n'aide pas non plus, comme souvent avec l'IA, c'est que chaque framework / librairie a sa propre définition d'un agent.
Et comme je n'aime pas comprendre un truc, je me suis documenté, testé, pris des murs, et j'ai fini par me faire ma propre définition.
C'est donc celle-ci que je vous propose, en toute humidité (comme dirait Perceval ⚔️).

> ⚠️ On est bien d'accord que c'est une définition personnelle, et que je ne prétends pas qu'elle soit universelle.
> A ce titre, n'hésitez pas à me faire des retours si vous n'êtes pas d'accord. ⚠️

Si j'étais taquin, je dirais qu'une fois de plus nos amis dans l'écosystème de l'IA ont réinventé une chose vielle comme le monde dans le développement logiciel 😈.
En effet, tout le monde s'émeut autour du fait que l'on a maintenant la capacité de faire de petits modules autonomes appelable, potentiellement, par des applications 🤪.
J'en conviens, je suis moqueur mais après avoir réinventer les services distants (MCP), l'appel de fonctions utilitaires (function calling) maintenant, on réinvente l'approche modulaire avec des agents 🙃.

## ☝️️ Ma définition d'un agent

C'est bien beau de se moquer, mais au final, c'est quoi un agent ?
Un agent va vous permettre de spécialiser l'utilisation d'un LLM pour une tâche précise.

Par exemple, si vous avez une application qui a besoin d'un LLM spécialisé dans la médecine et un autre dans le chinois.
Tout faire avec un seul modèle va vous forcer à utiliser un très gros modèle (donc cher).
Le modèle ne sera peut-être pas optimal dans les deux besoins (le fameux dilemne du combi DVD-magnétoscope 📼).

C'est là où l'approche agentique va vous permettre de créer des agents spécialisés.
Imaginez, que, chaque agent est une mini application (appelez ça un microservice si vous voulez 😆) qui va encapsuler un LLM avec des prompts, des outils, des mémoires, etc. pour répondre à un besoin précis.

> ⚠️ Mon analogie avec les microservices peut-être trompeuse, car la plupart des agents ne sont pas déployés en tant que services distants, mais exécutés localement dans votre application.
> Pour vraiment avoir le même paradigme, il faut utiliser [A2A](https://github.com/a2aproject/A2A) de Google. 
> N'ayant pas joué avec A2A, je n'aborderai pas ce sujet dans cet article. ⚠️

Vous le voyez, la grosse différence avec un simple appel à un LLM, c'est que l'agent va encapsuler toute la logique nécessaire pour répondre à un besoin précis.
Et qu'il sera possible d'orchestrer plusieurs agents pour répondre à des besoins plus complexes.

## 🔀 Orchestrer, si ... alors ... sinon

Encore une fois, ne vous attendez pas à quelque chose de révolutionnaire 😅.
Mais, une fois que l'on a tous ces agents, il va bien falloir les appeler dans le bon ordre, partager les entrées / sorties des "appels", ...
Oui, je vous vois venir, euh on parle de workflow, d'algorithmique, ... bref de développement de règles de gestion 🫣.

Oui, mais avec des agents donc ça change tout !

Dans la suite de l'article, je vais vous montrer les différentes façons d'orchestrer les agents : de la plus stricte vers la plus autonome.
Ce que l'on croise le plus pour expliquer l'orchestration d'agent est le pattern ReAct (Reasoning -> Acting).

Non, je vous vois venir : en gros c'est une boucle de feedback où l'agent va réfléchir (Reasoning) et agir (Acting).
Exactement !

Mais nous allons voir, maintenant, qu'il existe plusieurs niveaux.
Et que le dernier va vous surprendre 😁.


TODO : ready to serve 👾

# 🤗 En conclusion

Si vous êtes arrivé•es jusque-là merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

> 🔗 Resources