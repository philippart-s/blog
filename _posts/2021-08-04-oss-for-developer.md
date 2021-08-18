---
title: "🧐 L'Open Source au secours du développeur (et de l'architecte) ?"
classes: wide
categories:
  - Articles
  - Dev
tags:
  - Opensource
---
Ces dernières années (à peu près 6 ou 7 ans) je suis devenu architecte logiciel mais aussi garant de la Plateforme d'Intégration Continue (PIC pour les intimes :wink:) et de la delivery de manière générale, appelé souvent à tord chaîne DevOps mais peut être que cela parlera plus à certains sous cette terminologie.
La casquette d'architecte me permet de découvrir pleins de nouvelles technologies à mettre en place, la dernière du moment par exemple consiste à la mise en place d'Openshift.
La casquette PIC consiste essentiellement au développement de pipelines Jenkins transverses à l'ensemble des équipes de développement (donc en groovy).

Pour ceux sui me connaissent je suis, à l'origine, développeur Java et ces deux activités, passionnantes, réduisent de plus en plus la part de développement pour de nombreuses raisons (la PIC est stable, le métier d'architecte est plus dans la phase d'avant conception, l'organisation de ma société, ...).

Il me fallait donc quelque chose qui me permette de renouer avec le développement mais pas que.
Un besoin de partager, échanger, aider, ...
Retrouver ma définition du métier de développeur : un travail en équipe pour créer quelque chose.

## Pourquoi l'Open Source ?

Pour moi c'était une évidence : j'ai essayé de créer des "sides projects" et il faut reconnaître que c'est un échec !
J'ai un cimetière de projets inachevés impressionnant ! 

Ce qui me semblait une bonne chose avec l'Open Source c'est que le projet est réel, existant, avec une communauté d'utilisateurs, ...

L'autre raison est que, comme beaucoup d'entre nous, je suis utilisateur de librairies Open Source depuis des années et j'aime l'approche de commencer à reverser un peu du côté Open Source ce que j'ai pu emprunter.

La dernière raison, comme indiqué en préambule de cet article, l'envie de revenir au développement. 
En effet, je me suis rendu compte que je suis resté bloqué plus ou moins à Java 8 (en gros comme ma société :wink:) et que si je suis l'actualité, en mode opérationnel je n'ai presque pas manipulé les derniers JDK (et dieu sait comme cela change vite maintenant).

## :compass: Par où commencer ?

J'avoue avoir été pas mal perdu pour choisir un projet pour commencer mon aventure Open Source.
J'ai donc commencé par me documenter à la recherche de recettes et autres modes opératoires, celui qui m'a les plus servi a été la [conférence](https://blog.jetbrains.com/idea/2020/08/jetbrains-technology-day-for-java-how-i-started-contributing-to-open-source-and-why-you-should-too/?utm_source=pocket_mylist){:target="_blank"} donnée par Helen Scott qui explique comment elle a débutée sa propre aventure Open Source. 

Me voilà donc lancé dans l'aventure, j'avais juste un petit problème : en effet, que ce soit dans la conférence d'Helen ou dans les divers articles que j'ai lu beaucoup conseils de partir sur un projet que l'on utilise tous les jours, que l'on connaît déjà bien, et auquel on souhait participer.

Mais voilà, souvenez-vous, un des objectifs pour moi est, de justement, retrouver un rythme de développement car je ne développe plus assez durant mes journées professionnelles donc c'est un peut le serpent qui se mord la queue 🤨.

J'ai d'abord regardé du côté des projets qui me parlent : [Open JDK](https://openjdk.java.net/contribute/?utm_source=pocket_mylist){:target="_blank"}, SpringBoot(https://github.com/spring-projects/spring-boot/blob/main/CONTRIBUTING.adoc){:target="_blank"} et [Jenkins](https://www.jenkins.io/participate/){:target="_blank"}, chacun ayant une page expliquant comment participer au projet.

J'ai aussi regardé du côté de certains sites qui regroupent des projets identifiés ouverts aux nouvelles participations : 
 - https://up-for-grabs.net
 - https://www.codetriage.com
 - https://www.firsttimersonly.com/

Ce qu'il faut voir c'est que tous fonctionnent à peu près de la même façon : ils appliquent des labels / tags aux issues qui sont identifiées comme ouvertes à la contribution et plus particulièrement celles qui sont conseillées aux nouveaux arrivants sur le projet.
Ok, jusque là tout va bien, du coup je regarde côté SpringBoot, OpenJDL et Jenkins ... 

Et c'est là que ça commence à se compliquer.

## :dart: Le choix du premier projet pour sa première contribution

Syndrome de l'imposteur ou non j'ai très vite abandonné l'idée de contribuer à SpringBoot et OpenJDK : trop complexe et les différentes issues très impressionnantes.
Je me suis donc tourné vers Jenkins que j'utilise tous les jours en développant pour mon travail des pipelines as code (en Groovy donc). J'ai naturellement été vers un projet que j'utilise tous les jours pour faire les pipelines [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit){:target="_blank"}.

Je ne l'ai pas retenu pour diverses raisons :
 - c'est du groovy et un de mes objectifs est de faire plus de Java,
 - la communauté est un peu en baisse de vitesse, les personnes issues de la société à l'origine de la librairie ont moins de temps à y consacré (même si ils l'utilisent toujours),
 - ça reste du pro et j'ai dans ma liste de _todo_ le fait de motiver ma société pour commencer à rendre à l'Open Source ce que l'on y a pris, du coup ce repo serait parfait :wink: