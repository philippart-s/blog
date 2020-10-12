---
title: "GitHub API for Java"
#excerpt: 
classes: wide
categories:
  - Articles
  - code
tags:
  - Java
  - GitHub 
---
Récemment j'ai eu besoin d'accéder aux éléments de mes repositories via une application codée en Java.

Le premier réflexe : utiliser les API [REST](https://docs.github.com/en/free-pro-team@latest/rest){:target="_blank"} ou [GraphQL](https://docs.github.com/en/free-pro-team@latest/graphql){:target="_blank"} documentées sur le [site](https://docs.github.com/en/free-pro-team@latest/developers){:target="_blank"} développeur de GitHub.

Le graphQL c'est bien (il paraît :wink:) mais, je trouve, avec un ticket d'entrée assez élevé.
Du coup je me suis tourné vers la version REST, estampillée v3, qui semblait dans un premier temps vouée à être abandonnées ou profit de la v4 (graphQL) mais les équipes GitHub ont dû se rendre compte que ce n'était pas une si bonne idée et la v3 continue de vivre tranquillement et d'évoluer :smiley:. 

En creusant un peu les docs côté GitHub on tombe sur une [page](https://developer.github.com/v3/libraries/){:target="_blank"} qui liste des librairies permettant une intégration plus aisée dans le code.

GitHub ne fournit pas nativement une librairie pour Java mais donne une liste de quelques librairies tierces, c'est là qu'est indiquée celle qui nous intéresse : [GitHub for Java](http://github-api.kohsuke.org/){:target="_blank"}.

La documentation présente sur le [site](https://github-api.kohsuke.org/){:target="_blank"} est essentiellement pour se connecter à GitHub (sur une organisation  ou non) et pour le reste c'est simplement la [javadoc](https://github-api.kohsuke.org/apidocs/index.html){:target="_blank"} de l'API, à l'ancienne :wink:. A noter que le nom des classes et méthodes permettent aussi de facilement retrouver ce que font les méthodes dans le détail en regardant la documentation de l'API REST correspondante sur le site de GitHub.

A noter que la [mailing](https://groups.google.com/forum/#!forum/github-api){:target="_blank"} list a l'air plus ou moins abandonnée au profit des issues du [repository](https://github.com/hub4j/github-api){:target="_blank"} GitHub.

## Activer les dépendances
Bon les présentations sont faites, place à l'action et on commence par activer la dépendance (maven dans mon cas).

```xml
    <!-- GitHubAPI for Java -->
    <dependency>
      <groupId>org.kohsuke</groupId>
      <artifactId>github-api</artifactId>
      <version>1.116</version>
    </dependency>
```

La librairie ne vient pas avec des classes utilitaires pour les tests unitaires, pour ma part je suis parti sur l'utilisation de [mock server](https://www.mock-server.com/). Cela tombe bien car en regardant les tests unitaires de la librairie ils utilisent aussi cette librairie pour *mocker* les accès à GitHub et vérifier que les requêtes envoyées à GitHub sont bien formées.

Il faut donc ajouter mock server à nos dépendances de tests.

```xml
    <!-- Mock serveur HTTP. -->
    <dependency>
      <groupId>org.mock-server</groupId>
      <artifactId>mockserver-netty</artifactId>
      <version>5.11.1</version>
      <scope>test</scope>
    </dependency>
    <!-- Pour annotation dans les tests avec MockServer -->
    <dependency>
      <groupId>org.mock-server</groupId>
      <artifactId>mockserver-junit-jupiter</artifactId>
      <version>5.11.1</version>
      <scope>test</scope>
    </dependency>
```

## Premières utilisations
La première chose à faire est s'authentifier, bien entendu utiliser un token est fortement conseillé :wink:. Il y a beaucoup de mode d'authentification (login et password, juste token, ...), pour l'exemple j'utilise la connexion anonyme car le repository visé est public.

```java
GitHub.connectAnonymously();
```

La librairie a été pensée pour être utilisée comme une sorte de DSL en Java : chaque classe possède des méthode statiques qui renvoient un objet permettant lui-même d'appeler des méthodes renvoyant un autre objet avec ses propres méthodes, ...

Par exemple pour lister les repositories publics de l'utilisateur *philippart-s* il suffit d'ajouter à notre instruction de connexion:
```java
int nbRepositories = GitHub.connectAnonymously()
  .getUser("philippart-s")
  .getPublicRepoCount();

System.out.println(String.format("nommbre de repositories de philippart-s : %s", nbRepositories));
```

