---
title: "A la découverte de Picocli 🖥️"
description: "Faire une CLI simplement avec Java"
link: /2023-08-03-discover-picocli
tags: 
  - code
  - java
  - cli
  - Picocli
image: cli.jpg
figCaption: "Gabriel Heinzer"
author: wilda
---

Il y a quelques temps, j'ai écrit un [article](/2020-12-10-Cli-with-go) qui explique comment rapidement faire une CLI en Go.
Mais voilà, chassez le naturel et il revient au galop : je préfère le Java ☕️.  
Je vous propose donc, durant cet article, de découvrir comment écrire rapidement et efficacement une CLI 🧑‍💻 en Java.

## 🧐 C'est quoi une CLI ?

![Question mark image](question.jpg)
[Emily Morter](https://unsplash.com/fr/photos/8xAA0f9yQnE?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

CLI, pour **C**ommand **L**ine **I**nterface, ou comment interagir avec une utilisatrice ou un utilisateur au sein d'un terminal et non d'une interface graphique.  
J'ai l'avantage (ou pas 🤣) d'avoir connu le monde du développement et de l'informatique au siècle dernier.
A cette époque les interactions dans le terminal étaient légions et les interfaces graphiques pas si répandues.
Je vous parle d'un temps où les terminaux passifs étaient la norme (ainsi que le système d'exploitation Solaris 😅).  
On peut donc, sans trop se tromper, se dire que la CLI est l’ancêtre des interactions Homme-Machine depuis que l'on est en capacité de saisir de la donnée (aux alentours de 1960 avec l'apparition des _teletypes_).

En résumé, une CLI doit permettre d'enchaîner une ou plusieurs actions à la suite d'une ligne de commande composée d'options et ou de paramètres ou d'un simple prompt.
L'informatique étant cyclique, les CLI sont redevenues à la mode notamment avec Kubernetes ou Docker qui proposent des CLI très avancées pour simplifier l'utilisation de leurs produits.
L'autre avantage est qu'elle permet de scripter simplement des actions sans passer par des appels API parfois un peu rébarbatifs.  
En parlant API, souvent, les CLI ne sont que de _simples_ encapsulations des appels à ces API ou du SDK (**S**oftware **D**evelopment **K**it ) utilisé par les API.

Il existe plein de moyens et de technos pour créer une CLI : de la plus minimaliste (des scripts bash ou Windows par ex) jusqu'à l'utilisation de langages dits _haut niveau_ comme Go ou Java par exemple.
Comme indiqué en introduction, nous allons utiliser Java, mais pas que.
En effet, nous sommes loin des années 90 et des terminaux passifs, il existe maintenant bon nombre d'outils et de Frameworks facilitant la vie des développeuses et des développeurs.

## 🧩 Frameworks et librairies utilisées

En 2023, je pense, que l'on ne code plus tout de A à Z, et avant de demander à ChatGPT on va passer par la case intermédiaire : utiliser des Frameworks et ou librairies.  
Dans mon cas je vais donc utiliser : 
 - un JDK Java, la dernière version LTS, à ce jour la 17.
 - un Framework qu'il est cool pour écrire des applications : [Quarkus](https://quarkus.io/).
 - une librairie qu'elle est cool pour faire des CLI en Java : [Picocli](https://picocli.info/).
 - [GraalVM](https://www.graalvm.org/) histoire de générer un "vrai" exécutable

Mais faisons connaissance avec les trois briques principales de notre CLI.

### 🧩 Picocli

[Picocli](https://picocli.info/) est un Framework permettant de, simplement, créer des CLI.
L'idée principale de ce Framework étant de minimiser le code _technique_ pour écrire la CLI en se concentrant sur le code _fonctionnel_.  
On le verra plus tard dans le code, mais tout cela est permis par l'utilisation massive d'annotations Java.
Il sera alors simple de faire une jolie aide en ligne, avoir de la coloration dans celle-ci, faire de la validation de paramètres, ...

Les premières releases sous GitHub datent de 2017 et la dernière (au moment de l'écriture de cet article) date de juin 2023.

### 🧩 Quarkus

[Quarkus](https://quarkus.io/), comme [SpringBoot](https://spring.io/projects/spring-boot/), fait partie de la famille des Frameworks qui sont arrivés après l'ère du JEE (J2E pour les plus anciens comme moi 🥲) qui nécessitait un serveur d'application.
L'objectif est simple : vous faciliter la vie dans la conception d'une application et de la distribution de celle-ci.
A noter que Quarkus a été pensé (par [RedHat](https://www.redhat.com)) pour être pleinement optimisé pour les applications Cloud Natives.

 > ℹ️ Note :  
 > Bien entendu, [Picocli](https://picocli.info/) peut très bien être utilisé sans [Quarkus](https://quarkus.io/), c'est juste que j'adore ce Framework 🤩.

### 🧩 GraalVM

Lorsque je donne des conférences et que je parle de [GraalVM](https://www.graalvm.org/), j'aime souvent indiquer qu'enfin en tant que développeuses et développeurs Java on ne doit plus baisser la tête lorsque l'on parle de performances d'exécution !
Projet créé par Oracle, il a bouleversé la vision que l'on pouvait avoir d'une application Cloud Native, résiliente, et écrite en Java.
Pour notre besoin, c'est essentiellement la partie génération d'un exécutable de [GraalVM](https://www.graalvm.org/) qui va nous intéresser : générer un exécutable rapide, pas (trop) gros et facilement distribuable.

## 🎉 Initialisation du projet

![loading image](loading.jpg)
[Mike van den Bos](https://unsplash.com/fr/photos/jf1EomjlQi0?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

La première chose est d'initialiser le projet, et là tout de suite Quarkus va aider 🤩.  
Devinez quoi ?  
Et oui : Quarkus propose sa propre CLI et avec celle-ci, il est possible d'initialiser un projet pour créer une CLI qui est basée sur l'extension Picocli disponible dans l'écosystème Quarkus.

```bash
$ quarkus create cli fr.wilda.article:discover-picocli-article:0.0.1-SNAPSHOT
Looking for the newly published extensions in registry.quarkus.io
-----------
selected extensions: 
- io.quarkus:quarkus-picocli


applying codestarts...
📚  java
🔨  maven
📦  quarkus
📝  config-properties
🔧  dockerfiles
🔧  maven-wrapper
🚀  picocli-codestart

-----------
[SUCCESS] ✅  quarkus project has been successfully generated in:
--> /Users/stef/discover-picocli-article
-----------
Navigate into this directory and get started: quarkus dev
```

Cela génère une arborescence prête à l'emploi : 
```bash
$ exa -T   
.
├── LICENSE
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src
   └── main
      ├── docker
      │  ├── Dockerfile.jvm
      │  ├── Dockerfile.legacy-jar
      │  ├── Dockerfile.native
      │  └── Dockerfile.native-micro
      ├── java
      │  └── fr
      │     └── wilda
      │        └── article
      │           └── GreetingCommand.java
      └── resources
         └── application.properties
```

Regardons avec un peu plus d'attention ce que l'on a comme fichiers :  
  - tout ce qui est nécessaire pour compiler et builder le projet avec Maven (même si on verra plus tard que l'on utilisera directement la CLI de Quarkus pour faire ces actions) dans le `pom.xml`,
  - tout ce qui est nécessaire pour construire une image Docker dans `src/main/docker`,
  - un exemple de code dans `src/main/java`,
  - un fichier de configuration dans `src/main/resources`


## ✨ Création de sa première CLI

![Java code image](code.jpg)
[Markus Spiske](https://unsplash.com/fr/photos/AaEQmoufHLk?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

Intéressons-nous à la classe `GreetingCommand.java` qui a été générée: 
```java
package fr.wilda.article;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "greeting", mixinStandardHelpOptions = true)
public class GreetingCommand implements Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli",
        description = "Your name.")
    String name;

    @Override
    public void run() {
        System.out.printf("Hello %s, go go commando!\n", name);
    }
}
```

Un petit zoom sur les points clefs :
 - `@Command` : permet de déclarer le point d'entrée de la CLI lorsque l'on a qu'une seule commande,
 - `@Parameter` : permet de spécifier les paramètres que l'on passe à la CLI (ce qui est différent des _options_ mais nous y reviendrons plus tard).
    Le paramètre est automatiquement stocké dans la variable correspondante et il est possible de déclarer une valeur par défaut (option `defaultValue`).
    A noter aussi, qu'il est possible et même conseillé de positionner l'index du paramètre avec l'option `index` lorsqu'il y en a plusieurs. En l'absence d'index, l'ordre est calculé par réflexion, ce qui peut poser des problèmes ou donner des résultats différents selon les compilateurs.
    Enfin, dans l'exemple fourni, l'option `paramLabel` permet de spécifier quel label sera utilisé dans le message d'aide pour identifier la valeur à positionner pour le paramètre.
  - ensuite, une méthode `run` sans argument et sans paramètre : c'est cette méthode qui est le point d'entrée principal de notre CLI.

Vous pouvez donc en déduire que le travail de notre CLI est de : 
 - récupérer le premier paramètre pour le stocker dans le champ `name`
 - si il n'y a pas d'attribut : de stocker la valeur `picocli`
 - puis d'afficher `Hello <valeur du paramètre>, go go commando!` ou `Hello picocli, go go commando!`

On devrait donc avoir une utilisation de la forme :
```bash
$ discover-picocli-article Stef
Hello Stef, go go commando!
```

Allons vérifier cela tout de suite.

## ⚡️ Exécution de la CLI

![Laptop image](laptop.jpg)
[Simon Hattinga Verschure](https://unsplash.com/fr/photos/WNevBlZWCKA?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

Ce que j'aime avec Quarkus c'est le _developer mode_ qui permet de tester au fur et à mesure ce que l'on code sans avoir à relancer explicitement la compilation et le run.
```bash
$ quarkus dev                                                                

[INFO] Scanning for projects...
[INFO] 
[INFO] -------------< fr.wilda.article:discover-picocli-article >--------------
[INFO] Building discover-picocli-article 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- quarkus-maven-plugin:3.2.3.Final:dev (default-cli) @ discover-picocli-article ---

__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-08-03 16:50:55,297 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli-article 0.0.1-SNAPSHOT on JVM (powered by Quarkus 3.2.3.Final) started in 0.523s. 

2023-08-03 16:50:55,307 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2023-08-03 16:50:55,307 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, picocli]
Hello picocli, go go commando!

2023-08-03 16:50:55,346 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli-article stopped in 0.002s

--
Tests paused
Press [space] to restart, [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```

On le voit sur la sortie de Quarkus ci-dessus, la CLI est appelée sans paramètre au lancement, d'où le message `Hello picocli, go go commando!`.
Mais cela ne s'arrête pas là, en effet le mode developer de Quarkus va nous permettre d’interagir avec les paramètres (et options) que l'on passerait à notre CLI sans quitter le mode developer.  
Notamment avec la touche `e` comme mentionné en bas de la sortie de Quarkus : `[e] to edit command line args (currently '')`

Essayons de passer un paramètre, et voyons le résultat : 
```bash
2023-08-03 18:10:34,954 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli-article stopped in 0.002s


--
Tests paused
Stef

2023-08-03 18:11:11,211 INFO  [io.qua.dep.dev.RuntimeUpdatesProcessor] (Aesh InputStream Reader) Live reload total time: 0.144s 
Hello Stef, go go commando!

2023-08-03 18:11:11,213 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli-article stopped in 0.000s

--
Tests paused
Press [space] to restart, [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```

Pas mal mais au final une CLI qui doit être lancée par une CLI ce n'est pas vraiment ce que l'on veut je pense 🤔.  
On va voir ensemble comment on pourrait transformer notre projet en une _vraie_ CLI 🤘.

## 🚀 Une CLI dans mon terminal

Commençons certainement par le plus évident, l'utilisation de la commande `java`.
Avec cette commande on peut lancer une application packagée en fichier `JAR` avec la commande : `java -jar <nom de mon jar> <options> <paramètres>`.
Et encore une fois, Quarkus va nous aider à packager l'application pour ne pas avoir à créer les différents manifests nécessaires.
Pour cela, il faudra lancer la commande `quarkus build`.  
Cela a pour effet de générer le livrable `quarkus-run.jar` dans `./target/quarkus-app`.
C'est ce livrable que nous allons pouvoir utiliser, c'est aussi simple que cela 😎.  
Il ne reste plus qu'à tester : 
```bash
$ java -jar ./target/quarkus-app/quarkus-run.jar Stef

__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-08-03 18:24:24,404 INFO  [io.quarkus] (main) discover-picocli-article 0.0.1-SNAPSHOT on JVM (powered by Quarkus 3.2.3.Final) started in 0.399s. 
2023-08-03 18:24:24,420 INFO  [io.quarkus] (main) Profile prod activated. 
2023-08-03 18:24:24,420 INFO  [io.quarkus] (main) Installed features: [cdi, picocli]
Hello Stef, go go commando!
2023-08-03 18:24:24,465 INFO  [io.quarkus] (main) discover-picocli-article stopped in 0.005s

```

Et voilà : on a une belle CLI opérationnelle ! 

![A-Team image](a-team.jpg)  
[NBC](https://www.nbc.com/)

Vous ne m'avez pas l'air convaincu•e•s 😇 ?  

Oui il y a encore deux ou trois trucs à améliorer, déjà le côté utilisation : pas très simple et intuitif !
Ensuite, il y a beaucoup d'informations qui, au final, ne m’intéressent pas pour le _fonctionnel_ de ma CLI.

### 🔡 Suppression des logs

Attaquons-nous à ce problème en premier, les logs.
Avant de tout désactiver, j'aimerai quand même les avoir en mode _developer_ mais plus en mode _production_.
Vous commencez à vous en douter, Quarkus va, encore une fois, nous aider.
Pour cela on va positionner le paramétrage des logs à `OFF` mais uniquement pour le mode _production_ dans le fichier `application.properties` dans l'arborescence `./src/main/resources` : 
```java
%prod.quarkus.log.level=OFF
%prod.quarkus.banner.enabled=false
```
Testons de nouveau notre CLI : 
```bash
$ java -jar ./target/quarkus-app/quarkus-run.jar Stef

Hello Stef, go go commando!
```

### 🖥️ Une vrai CLI dans le terminal

On l'a vu, la commande `java` est un peu verbeuse et on se perd un peu sur ce qui est propre à la CLI que l'on développe et ce qui est propre à la commande `java`.
Pour cela, la première amélioration que l'on peut faire c'est encapsuler la commande, par exemple dans un script bash `my-cli` dans le répertoire `./src/main/bin` :
```bash
#!/bin/bash

java -jar $PATH_TO_CLI/quarkus-run.jar $1
```

> ℹ️ Note : l'utilisation de la variable d'environnement ici me permet de ne pas mettre de chemin en dur, il faudrait bien sûr le spécifier dans la belle documentation d'installation de notre CLI 😉.

On le rend exécutable : `chmod +x my-cli`.
Puis on le rajoute dans le `PATH` de la machine (ou on le copie dans un répertoire qui est déjà le PATH de la machine).  
Testons notre CLI finale : 
```bash
$ my-cli Stef

Hello Stef, go go commando!
```

Bon, cette fois ça y est, j'ai ma CLI et je l'utilise directement en ligne de commande dans un terminal 🎉.

Alors pari gagné ? 🤔

Non, effectivement et pour différentes raisons : 
 - l'utilisation d'un script bash n'est qu'un _artifice_ pour masquer l'utilisation de la commande `java`
 - cela nécessite toujours d'avoir une [JVM](https://www.java.com/fr/download/manual.jsp) d'installée 
 - cela ajoute donc cette empreinte supplémentaire en termes d’exécution et de consommation mémoire et processeur
 - ce n'est pas un _vrai_ exécutable comme on peut l'imaginer pour une CLI

Voyons comment remédier à cela 💥.

## 🚀 GraalVM, le dernier chaînon manquant

![Graal Kamelott image](graal.png)
[M6](https://www.6play.fr/m6)

Pour moi une CLI c'est un exécutable et c'est tout !
On a vu précédemment, que pour avoir un seul exécutable, on doit tricher et que l'installation n'est pas fluide (installer un JRE, ...).
Et je ne reviens pas sur les coûts de performances.
Heureusement, depuis quelques années, il est maintenant possible de _générer_ des exécutables à partir d'un développement Java.

Comment ?

Grâce à [GraalVM](https://www.graalvm.org/) !

### ⚒️ Installation

Pour cela, rien de plus simple, il faut suivre le [tutoriel](https://www.graalvm.org/latest/docs/getting-started/macos/) de la documentation.  
Ensuite, pour que la compilation avec Quarkus se déroule bien, il faut ajouter la variable d'environnement `GRAALVM_HOME` avec le chemin d'installation de GraalVM, par exemple `export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-17.0.8+9.1/Contents/Home` dans mon cas.  
Et voilà GraalVM est installé et prêt à être utilisé avec Quarkus.

### ⚙️ Génération de l'exécutable

Je pense que, maintenant, vous en avez l'habitude, mais encore un fois Quarkus va grandement nous faciliter la vie pour générer notre exécutable.
Il suffit d'ajouter l'option `--native` à la commande `quarkus build`, et le tour est joué !

`quarkus build --native`

Quarkus (avec l'aide de GraalVM) génère un exécutable dans le répertoire `./target` de la forme `artifactId-version-runner`.
Dans notre cas cela donne `discover-picocli-0.0.1-SNAPSHOT-runner`.
Un petit renommage en `my-cli` et le tour est joué `mv discover-picocli-article-0.0.1-SNAPSHOT-runner my-cli`.

Il ne reste plus qu'à tester tout ça : 
```
$ my-cli Stef

Hello Stef, go go commando!
```

>**⚠️ Quelques points d'attention ⚠️**  
> - Pour juste ce _hello, world!_, le binaire a une taille de 35 Mo sur un MacBook M1.
>C'est certes assez gros mais il ne faut pas oublier que cela embarque aussi les libs nécessaires à l'exécution, à voir comment cela évolue avec le code _métier_ de la CLI 😉.  
> - Sauf si je suis passé à côté, mais la génération du binaire dépend de la machine sur laquelle vous générez ce binaire, c'est pour moi un gros manque par rapport à ce qu'il est possible de faire en Go avec COBRA.

## 👨‍💻 Pour aller plus loin

![Buzz image](buzz.jpg)
[Brian McGowan](https://unsplash.com/fr/photos/857R--_CvP0?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

Cet article n'est que le début de la création d'une CLI avec Picocli, Quarkus et GraalVM.
Nous n'avons pas du tout exploré toute la puissance de Picocli pour créer notre CLI.
Il nous reste à jouer avec les options, les paramètres, l'aide, ...  
Ce sera pour la partie 2 de cet article que je trouve déjà bien assez long 😅.

## 🔎 En conclusion
 
Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.

Vous trouverez l'ensemble du code source dans le repository [discover-picocli-article](https://github.com/philippart-s/discover-picocli-article).