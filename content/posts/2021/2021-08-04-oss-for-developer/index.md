---
title: "L'Open Source au secours du développeur (et de l'architecte) ?"
description: ""
link: /2021-08-04-oss-for-developer
tags: 
  - Opensource
author: wilda
---

Ces dernières années (à peu près 6 ou 7 ans) je suis devenu architecte logiciel, mais aussi garant de la Plateforme d'Intégration Continue (PIC pour les intimes :wink:) et de la delivery de manière générale, appelée souvent à tort, chaîne DevOps mais peut être que cela parlera plus à certains sous cette terminologie.
La casquette d'architecte me permet de découvrir plein de nouvelles technologies à mettre en place, la dernière du moment, par exemple, consiste à la mise en place d'Openshift.
La casquette PIC consiste essentiellement au développement de pipelines Jenkins transverses à l'ensemble des équipes de développement (donc en groovy).

Pour ceux qui me connaissent je suis, à l'origine, développeur Java.
Mes deux activités professionnelles réduisent de plus en plus la part de développement pour de nombreuses raisons (la PIC est stable, le métier d'architecte est plus dans la phase de conception, l'organisation de ma société, ...).

Il me fallait donc quelque chose qui me permette de renouer avec le développement mais pas que.
Un besoin de partager, échanger, aider, ...
Retrouver ma définition du métier de développeur : un travail en équipe pour créer quelque chose.

## 🙄 Pourquoi l'Open Source ?

Pour moi c'était une évidence : j'ai essayé de créer des "sides projects" et il faut reconnaître que ce fut un échec !
J'ai un cimetière de projets inachevés impressionnant ! 

Ce qui me semblait une bonne chose avec l'Open Source : c'est que le projet est réel, existant, avec une communauté d'utilisateurs, ...

L'autre raison est que, comme beaucoup d'entre nous, je suis utilisateur de librairies Open Source depuis des années et j'aime l'idée de commencer à reverser un peu du côté Open Source ce que j'ai pu emprunter.

La dernière raison, comme indiqué en préambule de cet article, l'envie de revenir au développement. 
En effet, je me suis rendu compte que je suis resté bloqué plus ou moins à Java 8 (en gros comme ma société :wink:) et que même si je suis l'actualité, en mode opérationnel je n'ai presque pas manipulé les derniers JDK (et dieu sait comme cela change vite maintenant).

## 🗺️ Par où commencer ?

J'avoue avoir été pas mal perdu pour choisir un projet pour commencer mon aventure Open Source.
J'ai donc commencé par me documenter à la recherche de recettes et autres modes opératoires, celui qui m'a le plus servi a été la [conférence](https://blog.jetbrains.com/idea/2020/08/jetbrains-technology-day-for-java-how-i-started-contributing-to-open-source-and-why-you-should-too/?utm_source=pocket_mylist) donnée par Helen Scott qui explique comment elle a débuté sa propre aventure Open Source. 

Me voilà donc lancé dans l'aventure, j'avais juste un petit problème. 
Que ce soit dans la conférence d'Helen ou dans les divers articles que j'ai lus, beaucoup conseille de partir sur un projet que l'on utilise tous les jours, que l'on connaît déjà bien, et auquel on souhait participer.

Mais voilà, souvenez-vous, un des objectifs pour moi est, de justement, retrouver un rythme de développement car je ne développe plus assez durant mes journées professionnelles donc c'est un peu le serpent qui se mord la queue 🤨.

J'ai d'abord regardé du côté des projets qui me parlent : [Open JDK](https://openjdk.java.net/contribute/?utm_source=pocket_mylist), [SpringBoot](https://github.com/spring-projects/spring-boot/blob/main/CONTRIBUTING.adoc) et [Jenkins](https://www.jenkins.io/participate/), chacun ayant une page expliquant comment participer au projet.

J'ai aussi regardé du côté de certains sites qui regroupent des projets identifiés ouverts aux nouvelles participations : 
 - [https://up-for-grabs.net](https://up-for-grabs.net)
 - [https://www.codetriage.com](https://www.codetriage.com)
 - [https://www.firsttimersonly.com/](https://www.firsttimersonly.com/)

Ce qu'il faut voir c'est que tous fonctionnent à peu près de la même façon : ils appliquent des labels / tags aux issues qui sont identifiées comme ouvertes à la contribution et plus particulièrement celles qui sont conseillées aux nouveaux arrivants sur le projet.
Ok, jusque là tout va bien, du coup je regarde côté SpringBoot, OpenJDK et Jenkins ... 

Et c'est là que ça commence à se compliquer :scream:.

## 🎯 Le choix du premier projet pour sa première contribution

Syndrome de l'imposteur ou non j'ai très vite abandonné l'idée de contribuer à SpringBoot et OpenJDK : trop complexes et les différentes issues très impressionnantes.
Je me suis donc tourné vers Jenkins que j'utilise tous les jours en développant, pour mon travail, des pipelines as code (en Groovy donc). J'ai naturellement été vers un projet que j'utilise tous les jours pour faire les pipelines [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit).

Je ne l'ai pas retenu pour diverses raisons :
 - c'est du groovy et un de mes objectifs est de faire plus de Java,
 - la communauté est un peu en baisse de vitesse, les personnes issues de la société à l'origine de la librairie ont moins de temps à y consacrer (même si ils l'utilisent toujours),
 - ça reste du pro et j'ai dans ma liste de _todo_ le fait de motiver ma société pour commencer à rendre à l'Open Source ce que l'on y a pris, du coup ce projet serait parfait :wink:

Tout de même, Jenkins est écrit en Java, ce serait dommage de ne pas aller regarder de ce côté pour trouver un projet sympa auquel contribuer.

Là encore j'ai été un peu perdu pour savoir par où commencer, même si la page de contribution de la documentation est très ben faite. 
On se rend vite compte qu'il y a plusieurs mondes (infra, core, plugins, ...) et que chacun a ses propres règles.
De mon côté ne me sentant pas assez légitime pour le core j'ai regardé du côté des plugins et j'ai trouvé un plugin que l'on utilise dans nos pipelines et qui me semblait assez simple pour aborder ce premier pas : [Ansicolor-plugin](https://github.com/jenkinsci/ansicolor-plugin).

> :point_up_2: Je tiens à préciser une chose : ce que je vais indiquer par la suite n'est pas une généralité du monde Open Source ou encore même une certitude de comment est organisée la communauté Jenkins.
C'est ce que j'en ai compris et comment je l'ai ressenti. 

Une spécificité de Jenkins est qu'il y a deux endroits possibles où l'on peut voir des issues : 
 - le [JIRA](https://issues.jenkins.io) historique
 - GitHub pour les projets plus récents ou ayant migrés 

Autant le dire tout suite : je ne suis vraiment pas un fan de JIRA (je trouve que ça rame, que ce n'est pas intuitif, ...) et en plus il y a peu de ménage de fait dans les issues dans JIRA et certaines ont plusieurs années sans que l'on sache si elles sont toujours actives ou non.
Bref c'est aussi cela qui m'a fait choisir mon premier projet : un projet qui gère ses issues directement dans GitHub.

Une fois ce choix fait me voilà plongé dans le code et pour cela clonons ! 
Ou plutôt forkons !
Selon les projets, la documentation de prise en main du projet pour le compiler mais aussi faire sa première PR est plus ou moins bien faite. 
Dans le cas du projet choisi je n'ai pas à me plaindre car elle est plutôt bien faite : [https://github.com/jenkinsci/ansicolor-plugin/blob/master/.github/CONTRIBUTING.md](https://github.com/jenkinsci/ansicolor-plugin/blob/master/.github/CONTRIBUTING.md).

Je fais une petite digression sur la partie documentation : lors de mes recherches de projet je me suis rendu compte que c'était souvent la partie faible du projet et que rentrer dans le projet sans l'avoir beaucoup utilisé voir parcouru les différentes classes est très ardu.
Un des conseils pour rentrer dans le monde Open Source est de, justement, participer à la documentation.
Oui mais voilà pour écrire la documentation il faut comprendre le projet ! 
C'est peut être une mauvaise habitude liée à mon métier mais je trouve qu'une petite documentation d'architecture ou de conception aiderait pour prendre en main le projet manière macro.

> 💡 TIP 💡 : regarder les tests unitaires, en règle générale les projets Open Source ont pas mal de tests, cela aide à comprendre comment le projet doit être utilisé :wink:.

Le seul hic est que le lien fourni sur le README était erroné ! 
Et voilà ma première contribution qui m'apparaît : corriger le lien et proposer la correction à l'équipe qui maintien le projet :smile:.

## 📝 La première contribution
La première étape est de faire compiler le projet, c'est toujours une étape un peu délicate mais ce projet est plutôt simple en configuration et il bénéficie de la documentation officielle de Jenkins qui explique comment configurer un projet qui se destine à être un plugin.
L'autre élément à faire est de bien la lire les règles de contribution : comment nommer les PR, les commits, le format des sources, ... 
Cela évite de se faire refuser la proposition pour des éléments connexes.
C'est aussi une bonne façon d'apprendre à ne pas s'accrocher à ses habitudes et dans mon cas aussi à suivre des choses mises en place par d'autres (en règle générale c'est le contraire dans mon métier :wink:).
La dernière chose non technique à bien prendre en compte : le "code of conduct".
En effet, chaque projet est adossé à sa communauté et cette communauté à ses règles, souvent emprunt de bon sens (politesse, ...).

C'est fait !
Le lien est corrigé et la PR soumise.
C'est une modification plus que mineure mais j'ai tout de même ressenti de la fierté et de la joie lorsque j'ai vu le message d'acceptation et de merge !

Cela ne paraît pas grand chose mais cette première contribution simple m'a permis de débloquer certains verrous qui m'empêchaient d'oser faire ce genre de contribution.

## 🚀 Le vrai début de l'aventure !
Maintenant que je sais que je peux faire des choses j'ai envie d'aller plus loin et de vraiment coder !
Je recherche donc un plugin où je me sens à l'aise.
J'avoue que l'architecture de développement n'est pas forcément clairement expliquée dans les documentations de développeurs des plugins (ou alors je ne l'ai pas trouvée :sweat_smile:).
Du coup c'est un peu ardu de trouver le projet qui convienne mais aussi de tenter de comprendre comment le développement est architecturé. A l'origine j'avais jeté mon dévolu sur le plugin Maven mais celui-ci utilise JIRA (qui n'est pas très à jour et est complexe), du coup je suis retombé dans le côté ce n'est pas pour moi :flushed:.

Mais j'ai trouvé mon projet : [https://github.com/jenkinsci/conventional-commits-plugin/](https://github.com/jenkinsci/conventional-commits-plugin/).
Plugin assez récent avec peu code et surtout pas releasé, cela me permet de bien voir comment est architecturé un plugin Jenkins et aussi d'avoir une petite communauté active.

> 💡 TIP 💡
Cet autre conseil que j'aurai tendance à donner : essayer de trouver un projet d'une taille pas trop grosse qui permet de faciliter sa prise en main mais aussi d'avoir une communauté réduite et donc simple à contacter et identifier (qui fait quoi, notamment les maintainers)

Et là je reconnais que j'ai eu de la chance car non seulement le projet est simple à prendre en main mais il fait partit du programme Google Student Of Code (GSoC) et donc les maintainers (mentors pour le GSoC) sont très didactiques et prennent le temps d'expliquer les choses.
L'autre élément sympa est que la communauté Jenkins à l'habitude d'avoir des salons gitters, c'est super cool car cela permet d'échanger un peu plus librement que sur les issues et notamment parler astuces de développements ou juste d'idées que l'on a pour améliorer le projet.

Et c'est parti après quelques échanges on m'affecte une première issue et je me lance !
Le code est assez simple, c'est du Java 8 (bon j'aurai préféré 11 mais c'est une contrainte de Jenkins).
C'est tout bête mais c'est cool, je code pour le plaisir, c'est à dire pour la raison pour laquelle j'ai commencé l'informatique et fait ce métier !
Et c'est là que le plaisir est plus grand car ce n'est pas professionnel avec tous les éléments déviants (budgets, managers, ...) mais juste pour le plaisir d'aider :heartbeat:.

Depuis le début de mon aventure j'en suis à quelques issues (5-6) et PR avec l'impression de vraiment aider.

> 💡 TIP 💡
Il ne faut pas hésiter à créer la PR très tôt en indiquant clairement ce que l'on veut faire, cela évite de coder pour rien mais surtout permet de montrer à l'ensemble de la communauté ce que l'on compte faire et avoir leurs retours très tôt.

## ⏰ Rythme de travail
Lorsque l'on développe en entreprise si il y a une chose qui marque beaucoup de gens c'est le rythme, en résumé on est toujours en retard !
C'est, pour moi, générateur de beaucoup de frustrations (pas le temps de faire comme on veut) et de pénibilité.
L'idée était de ne pas avoir cela aussi sur le temps perso et c'était une petite appréhension.
Eh bien j'ai eu là dessus aussi une très bonne surprise car pas de stress ou de question pour savoir quand est-ce que le développement doit être terminé, même pour les petits développements.
C'est quelque chose que je n'avais pas forcément anticipé : les projets Open Source reposent sur le fait que des anonymes offrent du temps personnel pour travailler sur le projet, et le temps personnel par définition une fois enlevé le temps de travail et de sommeil, eh bien il n'en reste pas beaucoup (et je ne parle pas de la vie de famille :wink:).
A ce jour j'ai pu avoir le rythme que je voulais, je code une feature en moyenne en une ou deux semaine et les reviews se font entre 3 et 5 jours après la fin du développement.

> 💡 TIP 💡
Avant de se lancer dans l'aventure de participer à un projet Open Source il faut s'assurer que l'on a le temps pour cela une fois déduites toutes les activités habituelles (travail, sommeil, famille, ...).
Si il n'y a pas de grosses pressions, s'engager sur des implémentations de features et ne pas le faire par manque de temps pénalise le projet mais aussi les personnes qui auraient pu travailler sur ces features.

Pour ma part j'y travaille essentiellement le WE, durant les pauses déjeuners ou encore en temps partagé devant une série avec ma chérie :kissing_heart:.
A titre d'exemple mon premier commit date du 6 juillet et j'y ai consacré 30h, ce qui fait au moment de la rédaction de cet article une moyenne de 3.75h / semaine consacré au projet.

> 💡 TIPS 💡
Sauf si vous avez la chance d'avoir une entreprise qui vous laisse faire ce genre de chose dans votre cadre professionnel attention à ne pas trop se faire déborder et regarder le projet auquel on participe lors de ses journées de travail, on a vite fait de préférer quelque chose que l'on fait pour la plaisir par rapport à des contraintes professionnelles :wink:.
Pour ma part je ne clone pas les projets sur mon ordinateur professionnel, comme ça ne je suis pas tenté de regarder.
>
J'ai décidé aussi de ne participer, pour l'instant, qu'à un seul projet.
Cela simplifie la rentrée dans le monde Open Source mais aussi de Jenkins.
Cela me permet aussi de ne pas devoir switcher de contexte trop souvent (c'est une chose que je dois faire énormément au travail et que je trouve pénible :rage:).
Et puis, dans le cas ou il y a un peu de latence dans les réponses, cela permet aussi de couper un peu avec l'informatique !

Il faut aussi accepter le rythme de travail des autres, comme vous, ce n'est pas leur travail de valider les PR ou répondre aux questions, soyez patients et proposez votre aide pour review les autres PR c'est aussi très instructif !
## 🎁 Bonus
Le fait de travailler sur un projet Open Source m'a permis aussi de grandement améliorer mon anglais.
C'est tout bête mais cela m'a obligé à bien faire attention à mes tournures de phrases pour être sûr de ne pas faire passer le message inverse de ce que je pensais ! :sweat_smile:

:sunglasses: Petite fierté personnelle au bout de quelques semaines les maintainers m'ont promu maintainer :wink:.

## En conclusion

J'ai atteint mon premier objectif : reprendre un rythme de développement en Java.
C'est cool et en plus j'apprends encore des choses ... même en Java 8 !

Je suis tombé sur des personnes très bienveillantes et pédagogues, c'est très important pour ne pas retomber un dans n-ième syndrôme de l'imposteur.

J'ai encore plein d'envies : participer au plugin Maven, au core de Jenkuins, à un projet plus gros, trouver un projet en JDK 11 ou 17, ....

En tout cas je vous conseille vraiment de tenter l'expérience, en espérant que je vous ai un peu donné envie, voire donné quelques astuces.

Et parce que je n'y résiste pas c'est cool de voir ça sur la page de release d'un projet Open Source :grinning:

![release du plugin](release.png)