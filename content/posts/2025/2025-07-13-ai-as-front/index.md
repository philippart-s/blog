---
title: "🤖 Et si les LLM étaient le nouveau front ? 🎨"
description: "On nous promet la fin des développeuses et développeurs, mais si au final l'IA n'était qu'une nouvelle façon de faire du front ?"
link: /2025-07-13-ai-as-front
tags: 
  - IA
  - Front
  - LLM
image: cover.jpg
figCaption: "@wildagsx"
author: wilda
---

L'intelligence artificielle va-t-elle remplacer toute une partie de métiers que l'on connaît actuellement ?
C'est une des théories que l'on entend le plus souvent ces derniers temps.
Et plus particulièrement dans notre microcosme, celui du développement, n'est-on pas en train de scier la branche sur laquelle on est assis ?

En toute honnêteté je n'en sais rien 😅 ... tout ça pour ça me direz-vous !

Ce qui a l'air plutôt sûr c'est que cela risque de changer la façon dont nous exerçons notre métier mais aussi la manière que les utilisatrices et utilisateurs interagissent avec les applications que nous créons.
Et c'est là que je veux en venir : le front va-t-il disparaître ?

Derrière cette question un brin racoleuse, on pourrait la nuancer : l'interface Homme - Machine va-t-elle se transformer avec l'arrivée massive des chatbots boostés à l'IA ?

## 🖥️ Le dialogue Homme - Machine, une vielle histoire

Lors du [dernier talk](https://youtu.be/yEzKbvbOmTI?si=kYxuvHOrZYWLzhWA) de mon ami Thierry Chantier à Devoxx France 2025, il revient sur l'historique du dialogue homme-machine.
Du pourquoi, en passant au comment, jusqu'à nous montrer comment le réaliser avec une TUI.
Je vous laisse aller le voir pour avoir plus de détails sur cet historique et sur ce qu'est une TUI (Text User Interface) 😉.
Tout ça pour dire que, vous le constaterez, le dialogue homme-machine n'est pas tout jeune, voir il remonte aux années 60-70 où on avait besoin d’interagir avec les mainframes (pour les plus jeunes : l'équivalent d'une maison remplie de serveurs pour avoir à peine la puissance de votre téléphone 😅).

Donc oui, arriver à envoyer nos demandes, nos ordres à un ordinateur n'est pas une nouveauté.
Cela a juste évolué avec le temps, des terminaux passifs à nos téléphones, en passant par les sites web et applications sur les ordinateurs.

Quelque soit la technologie et le moment, toutes ces interactions avaient un point commun : passer par une interface conçue par une développeuse ou un développeur avec un chemin déterministe duquel on ne pouvait pas dévier.

## 💬 Le langage naturel, la nouvelle façon d'interagir

Ce qui est très étrange au final, c'est que l'on a passé toutes ces dernières années à faire apprendre à nos utilisatrices et utilisateurs une nouvelle façon de transcrire leurs besoins à travers des interfaces graphiques pour revenir à la base : le langage naturel.

Car oui, qui n'a jamais été frustré de pas pouvoir remplir un champs car trop court, ou avec des valeurs non prévues par les dévs ?

> Ca dépend, ça dépasse ...

C'est là, pour moi, le gros changement avec les LLM (Large Language Models) : nous pouvons simplement demander (avec des fautes en plus 😉), avec notre langue maternelle, ce dont nous avons besoin et la magie de l'IA fait le reste pour traduire cela et essayer d'en calculer (oui n’oubliez pas que ce ne sont que des statistiques 😉) la réponse la plus pertinente.

Si c'est très impressionnant, avec un chatbot nous n'avons réinventé que la façon de consommer du contenu web.
Au lieu d'aller sur Wikipedia, des sites webs ou de faire une recherche sur votre moteur de recherche vous demandez et le LLM se charge d'agréger les données les plus pertinentes pour vous.
Nous sommes donc, selon moi, dans un premier changement dans notre manière de mettre à disposition du contenu : pas de site web et de formulaires de recherche avec des listes déroulantes et autres cases à cocher.
Ici, on demande simplement dans notre langue préférée ce que l'on veut et l'affichage de la réponse (pertinente ou pas) arrive.

Notez ici, que ce mode a quelques inconvénients : 
 - 🗓️ la donnée utilisée est celle du moment de l’entraînement du modèle, cela peut varier de quelques jours à quelques années,
 - 🧮 cela reste des statistiques, donc la réponse n'est pas une certitude issue d'un seul contenu, mais certainement une agrégation de plusieurs contenus traitant du même sujet,
 - ⬇ tout ceci est en mode _pull_ : le modèle renvoie de la donnée en "lecture seule"

Je préfère aussi tout de suite aborder un point : je parle de remplacer le front, mais il faut bien afficher la réponse du modèle non ?
Oui c'est totalement vrai.
A la différence que, selon moi, au lieu de développer plusieurs applications ou sites web je ne développe que le chatbot qui va interagir avec le modèle.

## 🤯 Rendre le modèle intelligent, du moins actuel

Bon, c'est bien joli mais au final, du pattern CRUD (Create, Read, Update, Delete), ici je vous vends un Read du pauvre avec des données obsolètes et pas forcément valides 😂.

Très vite ce problème, tout le monde l'a constaté, et derrière la hype du moment on s'est rendu compte qu'avoir un super historien du web c'est bien mais que, pour avoir les nouvelles de la veille ou du jour ça ne marchait pas très bien 📰.

Pour palier à ça, on a vu apparaître deux grands mouvements : le fine tuning et le RAG (Retrieval-Augmented Generation).

Le fine tuning on peut le mettre dans la catégorie je crée mon modèle mais en plus simple.
Vous prenez un modèle existant et vous l’entraînez sur des données qui vous permettent de le spécialiser (par exemple pour les assistants de code avec des données représentant uniquement du code).
Cela fonctionne bien pour spécialiser un modèle sur des données qu'il n’aurait pas vues, mais c'est assez mauvais avec des données en faible quantité qui traiteraient des informations en temps réel.

Le RAG est plus prometteur : on sélectionne un corpus de données pertinentes, par exemple la version électronique du journal du jour, et on ajoute ce corpus de données à la requête (aussi appelée _prompt_) que l'on envoie au modèle en lui demandant de les utiliser pour donner sa réponse.
Cela fonctionne plutôt bien, mais cela demande d'avoir ces données dans un format le permettant et oblige certainement un premier filtre pour ne pas avoir à envoyer trop de données.
Merci à la recherche vectorielle qui permet d'avoir une phase de recherche sémantique dans les données à envoyer à votre modèle.

> 📺 Je vous conseille l'[excellent talk](https://youtu.be/kceR97PM_3k?si=C4mSewkKDwPaZfQ-) de Bertrand Nau qui explique comment fonctionne les bases de données vectorielles lors du dernier Devoxx France.

On a donc progressé : 
 - on peut spécialiser un modèle pour qu'il soit le plus pertinent possible (mais attention jamais à 100%)
 - on peut ajouter de la donnée pour l'aider à avoir des données plus actuelles avant de nous répondre

On a progressé dans notre refonte de l'IHM, mais on reste à ne pouvoir faire que de la lecture et il faut avoir préparé en amont les données dans le cas du RAG (pour le fine tuning aussi mais de façon différente avec des datasets), ce qui peut vite devenir fastidieux et coûteux.

> Si vous voulez voir comment implémenter du RAG, voici quelques liens d'articles que j'ai écrits dans le cadre de mon activité professionnelle 😉 :
>  - [RAG chatbot using AI Endpoints and LangChain4J](https://blog.ovhcloud.com/rag-chatbot-using-ai-endpoints-and-langchain4j/)
>  - [RAG chatbot using AI Endpoints and LangChain](https://blog.ovhcloud.com/rag-chatbot-using-ai-endpoints-and-langchain/)

## 🧰 Le function calling, l'IA enfin à jour et capable d'interagir ?

Le function calling est apparu pour pallier ce problème de : comment faire pour que mon modèle soit à jour avec n'importe quelle donnée dont il aurait besoin pour me répondre ?
Et quand je dis n'importe quelle, je ne parle pas que des données publiques mais aussi celles de votre entreprise ou de vos données personnelles.
Alors oui, en fait la partie lecture est déjà possible avec le RAG : au lieu de requêter une base de données vectorielle, rien ne vous empêche de faire une requête sur une API pour ajouter cette donnée au contexte.

Le function calling va permettre de faire cet ajout de données mais il va surtout permettre d'appeler n'importe quel outil et de faire n'importe quelle action : Create, Read, Update et Delete.

On y vient à mon interface Homme - Machine.

> **⚠️ Dès à présent pour la suite de l'article retenez bien une chose : ce n'est JAMAIS et non JAMAIS le modèle qui appelle les outils mais bien l'application appelante (celle qui fait la requête au modèle) qui le fait. ⚠️**

Il me semblait utile de faire cette mise au point 😉.

Le function calling va donc être simple : notre application met à disposition au modèle une liste d'outils que le modèle peut conseiller d'appeler avec une liste de paramètres qui correspond le plus probablement à ce que souhaite la personne qui utilise le chatbot.
Suite à cela le modèle va ou non indiquer quels outils et paramètres il faut utiliser, en fonction de la teneur du prompt, à l'application appelante et c'est elle qui va déclencher les appels.

Un petit exemple plus parlant : je souhaite créer un enregistrement `personne` dans ma base de données (accédée par mon application).
Je déclare donc une fonction `enregistrerPersonne` qui prend en paramètre des chaînes de caractères représentant le nom, le prénom et l'email de la personne.
J'indique à mon modèle que cette fonction est disponible, comment elle s'appelle, à quoi elle sert et quels paramètres elle utilise.
Suite à cela je vais pouvoir demander à mon modèle via mon chatbot : `Créer la personne Stéphane Philippart dans la base de données, son email est foo@bar.com`.

A la suite de cela le modèle va pouvoir indiquer en retour à mon application qu'il faut appeler la fonction `enregistrerPersonne` avec comme paramètres `"Stéphane", "Philippart", "foo@bar.com"`.
Et l'application va donc se charger de faire l'écriture en base de données (et c'est bien l'application et non le modèle 😉).

Bien entendu, on peut imaginer la même chose pour de la mise à jour ou de la suppression.

A ce stade, on avance dans notre recherche d'avoir une interface utilisateur où je suis libre de formuler mes demandes comme je le souhaite sans être dans un carcan d'une interface graphique rigide.
On peut aussi noter d'autres avantages : pas de traductions, pas d'évolutions quand la base de données ou les règles métiers changent.

Il reste toujours des problèmes, bien sûr : 
 - l'appel de la fonction n'est pas déterministe : même si vous pouvez jouer sur des paramètres lors de l'appel à votre modèle (comme la température ou le paramètre top_P par exemple), vous ne serez jamais sûr à 100% qu'il fasse les bons choix ... souvenez-vous des statistiques et tout ça 😉,
 - la sécurité : souhaitez-vous vraiment ne jamais intervenir dans le processus de décisions ? Pour une application de gestion ça va encore, pour des décisions plus grave ... 😱. Il existe des mécanismes de contrôles que vous pouvez implémenter mais, souvent, ils font appel à des LLM (mécanisme de LLM as judge par exemple). A ce stade pour les actions critiques il semble qu'une validation humaine en bout de chaîne soit une bonne idée 🚧.
 - la redondance de code : ma super fonction qui fait une action très bien, j'ai envie de pouvoir la partager en interne, là vous pouvez créer une lib. Mais si vous voulez éviter de tout déporter sur la machine cliente vous allez vite arriver à la question : mais comment déployer ça pour que mon outil soit utilisable de n'importe où 🤔 ?

> Si vous voulez voir comment implémenter du function calling, voici un lien vers un article que j'ai écrit dans le cadre de mon activité professionnelle 😉 : [Using Function Calling with OVHcloud AI Endpoints](https://blog.ovhcloud.com/using-function-calling-with-ovhcloud-ai-endpoints/)

## 📚 MCP, l'invention des architectures n-tiers par l'IA ?

Le [Model Context Protocol](https://modelcontextprotocol.io/introduction) (MCP) est une proposition d'[Anthropic](https://www.anthropic.com/) pour palier ce problème de distribution des outils.
Imaginons que je suis un fournisseur connu d'un service permettant de versionner du code et de déclarer des issues et des PR 🐙 ?
Je me rends compte que beaucoup d'entreprises développent leurs propres outils pour leurs chatbots leurs permettant de faire les issues, les PR, ...

Qui de mieux que moi pour fournir un tel outil, et le rendre disponible pour mes clients ?

C'est un très gros résumé de ce que propose MCP : la création d'un protocole vous permettant de récupérer, entre autre, la liste des outils d'un ou de plusieurs fournisseurs.
Et ensuite cela fonctionne comme dans le paragraphe précédent pour le function calling, sauf que l'application appelante n'a plus à coder l'outil c'est le fournisseur via MCP qui, non seulement fournit la liste des outils, mais aussi se charge de les appeler suite à la réponse du LLM.

Et tout ça ... tout le monde s'y engouffre, tout le monde veux exposer son super serveur MCP tout beau !

> 📺 Vous sentirez une pointe d'ironie dans cette phrase, et c'est vrai 😆. Je vous conseille [le talk](https://youtu.be/hICGtUH7K-4?si=i2hoVBkqiI2LrvDk) de Sébastien Blanc lors du dernier Devoxx UK qui démystifie très bien ce sujet. 

Car oui ... on s'extasie devant une architecture 3 tiers qui a plus de 20 ans ... Interface, logique métier, données.

Mais attention, loin de moi de dire que ce n'est pas une vrai avancée, le MCP et toutes ses variantes concurrentes apportent, pour moi, la dernière pierre pour que l'on repense totalement la façon dont l'Homme interagit avec la machine.

Bien sûr tout n'est pas rose : 
 - il reste des problèmes de véracité (les statistiques encore et encore)
 - la sécurité : à l'heure actuelle c'est un peu le Far West dans les serveurs MCP, et avant de donner vos identifiants et mots de passes, réfléchissez à comment ils sont envoyés, stockés, utilisés, ...

> Si vous voulez voir comment implémenter un serveur MCP, voici un lien vers un article que j'ai écrit dans le cadre de mon activité professionnelle 😉 : [Model Context Protocol (MCP) with OVHcloud AI Endpoints](https://blog.ovhcloud.com/model-context-protocol-mcp-with-ovhcloud-ai-endpoints/)

## 🧐 Alors le front est fini, tout comme les dévs front ?

Non bien sûr, et ce n'est pas le sujet de mon article.
J'espère que vous avez compris 🤗.

C'est juste, qu'après ces quelques années passées dans l'IA et plus particulièrement dans la gen AI avec les LLM je me rends compte que l'on se rapproche de plus en plus d'une nouvelle façon d'échanger avec nos machines.
Demain vous rajouterez la voix, mais n'est-ce déjà pas le cas avec les assistants personnels ?
La seule différence est qu'on lui demandera de commander un billet de train plutôt que de calculer le temps pour aller à la gare 😉.

En dehors des effets d'annonces et de démos faites pour faire briller les yeux, ne vous y trompez pas, il reste des challenges importants à relever.
La sécurité semble être le plus important.
Mais aussi la consommation induite par une sur-utilisation des gros LLM.

A ce sujet, je pense que le function calling avec ou sans MCP commence à être une réponse pour diminuer la taille des modèles, en les améliorant, à la demande, avec des outils (tient c'est marrant c'est ce qu'est sensée faire l'IA avec l'humain non ?).
N'hésitez pas non plus à [aller voir ce que fait](https://k33g.hashnode.dev/) Philippe Charrière, il donne un nombre impressionnants d'exemples sur le sujet des Small Languages Models (SML) mais aussi sur les différents sujets abordés dans cet article.

Vous l'avez compris, j'espère, le but de cet article n'est pas de vous expliquer dans le détail ce que sont et comment implémenter les RAG, MCP et autres fonction calling.
Je vous ai tout de même mis des liens vers d'autres articles que j'ai écrits qui, j'espère, vous aideront. 

Cet article n'avait pas pour but de révolutionner votre façon de travailler avec l'intelligence artificielle, mais il m'a permis de mettre des mots sur une idée qui me trottait en tête depuis un moment.

Si il vous a plu, ou même si j'en ai envie, je rajouterai peut être des articles permettant d'illustrer de manière programmatique comment concevoir un chatbot utilisant ces notions.
Vous avez déjà quelques liens pour commencer à sauter le pas 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
