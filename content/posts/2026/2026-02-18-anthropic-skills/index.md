---
title: "🧑‍💻Les skills Anthropic, l'arme ultime du vibe-coder ? 🤖"
description: Toujours par deux ils vont, ni plus, ni moins… Le Maître et son Apprenti… © Yoda
link: /2026-02-18-anthropic-skills
image: anthropic-skills-cover.jpg
figCaption: "@R.D. Smith"
tags: 
  - Code
  - IA
author: wildagsx
---

🏴 You can find the English version of this article [here]({site.url}2026-02-10-llm-guardrails-en). 🏴


## 📖 TL;DR
TODO

<br/>

# 🤔 Le constat

Si vous avez suivi mes derniers articles, je suis tiraillé entre le fait de bosser dans l'IA et me demander ce que mon métier de développeur va devenir.
Alors spoiler non je ne vais pas répondre à cette question mais simplement vous livrer, ce qui a fait que récemment j'utilise plus l'IA dans ma vie de tous les jours de développeur.

Du coup pourquoi ce changement ?
Je le dois à deux personnes : 
 - Stéphane Trebel 
 - Philippe Charrière 

Et un peu moi qui me suis mis un coup de pied aux fesses pour arrêter de faire ma tête de mule vis-à-vis de l'IA et du dev.

En effet pour me convaincre que l'IA n'était qu'un outil (mauvais) pour les devs et que coder était quelque chose qui ne s'inventait pas je pense qu'inconsciemment j'utilisais l'IA avec quasiment tous les antipatterns.
Pourtant, à mon petit niveau, je sais comment cela fonctionne.

Mais voilà je me contentais d'un prompt vague et de tenter de lancer le résultat avec comme constat "ben oui ça marche pas forcément".

Alors qu'est-ce qui m'a fait changer d'avis ?
D'abord parce que je suis têtu et que j'aime comprendre ce que je ne comprends pas.
Et voir toute cette hype sur la génération de code m'interpellait.
Pourquoi, moi, je n'y arrivais pas ?

Alors qu'on se le dise tout de suite, non je ne change pas d'avis qu'il y a beaucoup de n'importe quoi dans les entreprises.
On s'imagine trop que l'IA est la silver bullet qui va résoudre tous les problèmes de dev et par la même remplacer cette coûteuse charge que sont les devs.
Bref, je ne vais pas participer au bruit ambiant des pros / contres ce n'est le but de cet article.

Revenons à pourquoi j'ai changé d'avis, ou plutôt, pourquoi maintenant j'ai moins de frustration à utiliser l'IA dans mon quotidien de dev.

# 🎬 Le déclic : Stéphane Trebel, le faux naïf

Si vous ne connaissez pas Stéphane allez voir ce qu'il fait sur sa chaîne YT et son LinkedIn, je suis sûr que ça vous donnera envie de l'avoir dans vos équipes ;).
Je vais vous parler de ses streams et plus particulièrement sur son créneau IA / Vibe coding.
En effet, Stéphane a décidé il y a quelques semaines de se lancer dans la génération de code avec l'approche vibe coding.

> à partir de maintenant je vais parler d'aide à la génération de code ou de génération de code, je pense que le terme vibe coding est un des responsables que je ne voulais pas utiliser l'IA gen pour le dev.
> il a trop été galvaudé par des personnes ne sachant pas de quoi elles parlent.

Et en mode faux naïf : il part de zéro et commence à installer des outils comme Claude code ou encore Opencode.
Puis essaye des approches comme BMad par exemple.

> pourquoi faux naïf ? Car Stéphane sait ce qu'il fait et c'est là que c'est très efficace qd on suit son stream.

Au fur et à mesure Stéphane prend le temps.
Il lit la réponse, argumente, challenge, teste, modifie ... bref un vrai échange qui dure plusieurs heures.
Et c'est là la grosse différence avec tout ce que l'on voit où on nous promet de la magie en 15-20 mins, spoiler la seule magie que l'on a comme résultat c'est 💩 (désolé).
Et c'était mon cas : je voulais tester mais je ne donnais pas sa chance au produit.

## 👥 Le pair programming comme mode d'utilisation

En regardant les streams de Stéphane, j'ai compris que non seulement on peut prendre ces outils comme de simples outils mais surtout il faut que ce soit un dialogue.
Et plus que le mode canard comme on peut voir parfois ou comme si on parlait à un junior.

> je profite de cette dernière phrase "comme si on parlait à un junior" : arrêtez avec cette analogie, c'est réducteur et dégradant pour les personnes débutantes et complètement faux
> je ne compte plus le nombre de choses où des jeunes développeuses ou développeurs m'ont appris quelque chose à moi le vieux développeur que je suis

En fait j'ai compris un truc : ce qui marche c'est le mode pair programming.
C'est devenu une évidence quand j'ai vu les streams de Stéphane. (TODO bouquin Fanny)
On a, à tour de rôle, un rôle de générateur de code (humain ou IA) et un rôle de relecteur (humain ou IA).
Alors certes l'IA génère un peu plus mais, pour mon cas, cela s'équilibre sur la fin.

Et on a, pour moi, tous les avantages du pair programming : 
 - le mode de devoir expliquer ce que l'on veut faire avant de le faire
 - le débat d'idée
 - apprendre par l'exemple
 - challenger une décision

Et cela marche pour les 2 : l'IA et vous.
Vous car si vous relisez ce qui est généré, essayez de comprendre vous progressez.
L'IA car au fur et à mesure des échanges, elle va mieux comprendre ce que vous voulez faire et ainsi générer du code de meilleure qualité.

De mon côté j'utilise Opencode qui répond parfaitement à mon besoin.
Une session par projet.

J'insiste, mais c'est vraiment ce mode d'utilisation qui m'a fait basculer vers une utilisation de la génération de code utile pour moi.

Et du coup le premier constat : non ce n'est pas 10 fois plus rapide.
Comme pour le pair cela demande un peu de temps au début pour s'y habituer, mais ensuite pour certaines tâches on est plus rapide et surtout la qualité est vraiment au rendez-vous ainsi que le passage de connaissance et l'apprentissage.

Par rapport à une vraie personne il me manquait un truc : devoir réexpliquer à chaque fois le type de pair codeur que je voulais (Java, le style dev, ...).

# 🧠 Le deuxième game changer : Philippe Charrière et les skills Anthropic

Et c'est là que le deuxième game changer est arrivé : Philippe Charrière et ses skills Anthropic.
Philippe est très présent sur la gen IA et je ne peux que vous conseiller de suivre ses productions (articles, code, conférences, stream, ...)

Celle dont je parle part d'un message anodin sur le fait qu'il mettait à disposition une Skill pour Claude code.
Tiens, j'en avais entendu parler mais je n'avais pas encore fait l'effort de m'y intéresser (souvenez-vous la gen de code et moi ça faisait 2).
Mais là, c'est Philippe donc je vais qd même regarder.

Je vous laisserai aller voir dans le détail ce que sont les skills mais en résumé ce sont des supers system prompt pour donner une vraie direction à votre modèle.
TODO mettre lien vers docs anthropic skills

Et justement Philippe a créé une super skill pour créer un dev raisonné et très documenté : methodological-dev.

Et bonne nouvelle c'est compatible Opencode.

Pour installer une skill, avec opencode, vous n'avez qu'à créer un répertoire `skills` dans le répertoire `.opencode` puis de créer un répetoire avec le nom de votre skill.
Par exemple pour la skill de Philippe : `.opencode/skills/methodical-dev/SKILL.md` et voilà votre skill est prête à être utilisée.

## 🛠️ Ma skill Java

Du coup j'ai créé la mienne pour Java, ajouté des astuces pour qu'il me corrige mon anglais.
J'ai maintenant un co-dev qui parle comme moi Java et me propose toujours la même approche.

Comment ma skill est structurée ?
C'est assez simple : je lui demande de suivre la même approche que celle de Philippe mais j'ai rajouté le fait d'utiliser Java 25, Quarkus et JBang.
Afin de respecter les bonnes pratiques ma skill a des références, voyez ça comme des sous skills qui permettent d'éviter de trop charger le contexte.

Car, parlons-en maintenant, l'utilisation des skills va vous faire énormément gonfler le contexte de votre prompt.
Donc attention à avoir le bon modèle avec le bon abonnement.
Pour ma part j'ai la chance de pouvoir utiliser Opencode avec AI Endpoints d'OVHcloud et Qwen Coder sans contraintes de limites 😇. 

Pour voir le détail de ma skill, c'est ici que ça se passe : https://github.com/philippart-s/ai-skills/tree/main/java-dev
Chaque référence permet de détailler une partie de la skill, par exemple pour Java j'ai les grandes lignes directrices dans la skill puis une référence qui détaille vraiment les bonnes pratiques.
Cette référence sera chargée ensuite par la CLI au besoin.

## 🏆 Le combo gagnant

Avec ce combo skill + pair programming, je me retrouve avec une expérience, qui, je trouve, me permet de mieux vivre la génération de code.

Mais alors pourquoi ne pas le faire avec un vrai humain ?
C'est une très bonne question, et la réponse est simple : je n'ai jamais eu la chance de travailler dans une entreprise qui pratique et croit dans le pair programming.
Et maintenant que je ne suis plus sur des projets c'est encore plus compliqué.
Je ne dis pas qu'il faut remplacer tous les binômes de pair programming mais se donner la possibilité de transformer les binômes de pair en mob programming en rajoutant de l'IA gen.

# 🎓 Et l'apprentissage dans tout ça ?

Alors au final l'IA gen va nous remplacer ou nous augmenter ?
Je n'en sais rien, ce que je sais c'est que j'ai arrêté de vouloir aller contre et un coup me faire peur que cela allait mettre fin à un métier que j'aime ou encore me dire que c'est essentiel.
J'ai trouvé ma façon d'avoir une relation "saine" qui m'apporte quelque chose et qui continue de me faire progresser.

J'en profite pour justement parler de cet aspect : l'apprentissage.
On commence à se poser la question de comment nos jeunes développeuses et développeurs vont apprendre à coder avec l'IA gen.
Eh bien, je ne prétends pas avoir la réponse, mais cette approche de pair programming me semble une piste.
Tout comme on se sert du pair pour faire monter en compétence les débutantes et débutants autant utiliser l'IA gen pour faire de même.
Et surtout ne pas transformer les débutantes et débutants dans une techno en prompteuses et prompteurs (je commence à avoir ce genre de retour).
Je vais m'essayer d'endosser ce rôle de débutant dans les semaines à venir pour la partie front qui n'est clairement pas ma tasse de thé.
Je pense créer une skill qui indique clairement que je suis un débutant et que le but est de monter en compétence, je verrai si cela tient la route.

# 💡 En conclusion

C'est le dernier conseil que je peux donner : ne pas subir, rien ne vous oblige à utiliser l'IA gen (sauf votre patron peut-être ;)) mais si vous vous y intéressez, cela ne pourra qu'être un atout en plus pour vous.
Soit vous saurez pourquoi vous ne voulez pas l'utiliser, soit vous saurez comment l'utiliser pour vous faire gagner du temps et surtout progresser.

Et aussi un petit message à toutes celles et ceux qui ne jurent que par l'IA : ayez aussi un peu de recul, et n'imposez pas vos idées comme des vérités absolues à celles et ceux qui doutent cela ne fait que 
augmenter leurs doutes :).
