---
title: "A la découverte de Picocli"
excerpt: "Faire des CLI simplement avec Java"
classes: wide
date: 2023-04-06
categories:
  - Articles
  - Dev
  - Java
tags:
  - code
  - java
  - cli
---
<meta content="{{ site.url }}{{ site.baseurl }}/assets/images/discover-picocli/cli.jpg" property="og:image">

![CLI image]({{ site.url }}{{ site.baseurl }}/assets/images/discover-picocli/cli.jpg){: .align-center}
[Gabriel Heinzer](https://unsplash.com/photos/xbEVM6oJ1Fs?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink){:style="font-size: smaller"}{:target="_blank"}{: .align-right}

Il y a quelques temps j'avais écrit un [article]({{ site.baseurl }}{% post_url 2020-12-10-Cli-with-go %}) qui explique comment rapidement faire une CLI en Go.
Mais voilà, chassez le naturel et il revient au galop, je préfère le Java ☕️.  
Je vous propose donc, durant cet article de découvrir comment écrire rapidement et efficacement une CLI 🧑‍💻.

## 🧩 Frameworks et librairies utilisées

En 2023 je pense que l'on ne code plus tout de A à Z, et avant de demander à ChatGPT on va passer par la case intermédiaire : utiliser des Frameworks et ou librairies.  
Dans mon cas je vais donc utiliser : 
 - un JDK Java, la dernière version LTS, à ce jour la 17.
 - un Framework qu'il est cool pour écrire des applications : [Quarkus](https://quarkus.io/){:target="_blank"}.
 - un librairie qu'elle est cool pour faire des CLI en Java : [Picocli](https://picocli.info/){:target="_blank"}.

 > ℹ️ Note :  
 > Bien entendu, Picocli peut très bien être utilisé sans Quarkus, c'est juste que j'adore ce Framework 🤩.

## 🎉 Initialisation du projet

La première chose est d'initialiser le projet, et là tout de suite Quarkus va aider 🤩.

```bash
% quarkus create app fr.wilda:discover-picocli:1.0.0
Looking for the newly published extensions in registry.quarkus.io
-----------

applying codestarts...
📚  java
🔨  maven
📦  quarkus
📝  config-properties
🔧  dockerfiles
🔧  maven-wrapper
🚀  resteasy-reactive-codestart

-----------
[SUCCESS] ✅  quarkus project has been successfully generated in:
--> /Users/stef/xxx/xxx/discover-picocli
-----------
Navigate into this directory and get started: quarkus dev
```

Cela génère une arborescence prête à l'emploi : 
```bash
% exa -T   
.
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src
   ├── main
   │  ├── docker
   │  │  ├── Dockerfile.jvm
   │  │  ├── Dockerfile.legacy-jar
   │  │  ├── Dockerfile.native
   │  │  └── Dockerfile.native-micro
   │  ├── java
   │  │  └── fr
   │  │     └── wilda
   │  │        └── GreetingResource.java
   │  └── resources
   │     ├── application.properties
   │     └── META-INF
   │        └── resources
   │           └── index.html
   └── test
      └── java
         └── fr
            └── wilda
               ├── GreetingResourceIT.java
               └── GreetingResourceTest.java
```

A ce stade on va rajouter l'extension Picocli : 
```bash
% quarkus ext list --concise -i -s picocli
Current Quarkus extensions installable: 

✬ ArtifactId                                         Extension Name
✬ quarkus-picocli                                    Picocli

To list only extensions from specific category, append `--category "categoryId"` to your command line.

Add an extension to your project by adding the dependency to your pom.xml or use `quarkus extension add "artifactId"`

% quarkus ext add Picocli          
[SUCCESS] ✅  Extension io.quarkus:quarkus-picocli has been installed
```

Cela a comme principale action de rajouter les bonnes dépendances dans le _pom.xml_.

## ✨ Création de sa première CLI

La première chose à faire est de définir le point d'entrée de la CLI avec l'annotation `@Command` : 

```java
package fr.wilda;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command
public class Hello implements Runnable {

  @Option(names = {"-n", "--name"}, description = "Give a cool name!", defaultValue = "🌍")
  String name;

  public Hello() { 
  }

  @Override
  public void run() {
      System.out.println("👋, " + name + "!");
  }
}
```

Un petit zoom sur les points clefs :
 - `@Command` : permet de déclarer le point d'entrée de la CLI lorsque l'on a qu'une seule commande,
 - `@Option` : permet de déclarer les options de la CLI. 
    L'option est automatiquement stockée dans la variable correspondante et il est possible de déclarer une valeur par défaut.
    Il est aussi possible de regrouper plusieurs options ensemble, ici `-n` et `--name`.


Ce que j'aime avec Quarkus c'est le _dev mode_ qui permet de tester au fur et à mesure ce que l'on code.
Il suffit de lancer `quarkus dev` puis une fois l'application Quarkus appuyer sur la touche `e` pour simuler le fait d'appeler la CLI avec des paramètres.

```bash
quarkus dev
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------------------< fr.wilda:discover-picocli >----------------------
[INFO] Building discover-picocli 1.0.0
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- quarkus-maven-plugin:2.16.6.Final:dev (default-cli) @ discover-picocli ---

Listening for transport dt_socket at address: 5005

__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-04-16 19:06:24,885 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli 1.0.0 on JVM (powered by Quarkus 2.16.6.Final) started in 0.969s. Listening on: http://localhost:8080

2023-04-16 19:06:24,893 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2023-04-16 19:06:24,893 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, picocli, resteasy-reactive, smallrye-context-propagation, vertx]
👋, 🌍!
2023-04-16 19:06:24,942 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli stopped in 0.004s

2023-04-16 19:06:30,957 INFO  [io.qua.dep.dev.RuntimeUpdatesProcessor] (Aesh InputStream Reader) Restarting as requested by the user.
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-04-16 19:06:31,147 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli 1.0.0 on JVM (powered by Quarkus 2.16.6.Final) started in 0.184s. Listening on: http://localhost:8080

2023-04-16 19:06:31,148 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2023-04-16 19:06:31,148 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, picocli, resteasy-reactive, smallrye-context-propagation, vertx]
2023-04-16 19:06:31,148 INFO  [io.qua.dep.dev.RuntimeUpdatesProcessor] (Aesh InputStream Reader) Live reload total time: 0.198s 
👋, Stef!
2023-04-16 19:06:31,151 INFO  [io.quarkus] (Quarkus Main Thread) discover-picocli stopped in 0.001s

--
Tests paused
Press [space] to restart, [e] to edit command line args (currently '-n Stef'), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```
## En conclusion
 
Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog){:target="_blank"} 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.