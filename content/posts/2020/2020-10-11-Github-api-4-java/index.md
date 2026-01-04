---
title: "GitHub API for Java"
link: /2020-10-11-Github-api-4-java
tags: 
  - Java
  - GitHub
author: wilda
---

Récemment j'ai eu besoin d'accéder aux éléments de mes repositories GitHub via une application codée en Java.

Le premier réflexe : utiliser les API [REST](https://docs.github.com/en/free-pro-team@latest/rest) ou [GraphQL](https://docs.github.com/en/free-pro-team@latest/graphql) documentées sur le [site](https://docs.github.com/en/free-pro-team@latest/developers) développeur de GitHub.

Le graphQL c'est bien (il paraît :wink:) mais, je trouve, avec un ticket d'entrée assez élevé.
Du coup je me suis tourné vers la version REST, estampillée v3, qui semblait dans un premier temps vouée à être abandonnée au profit de la v4 (graphQL) mais les équipes GitHub ont dû se rendre compte que ce n'était pas une si bonne idée et la v3 continue de vivre tranquillement et d'évoluer :smiley:. 

En creusant un peu les docs côté GitHub on tombe sur une [page](https://developer.github.com/v3/libraries/) qui liste des librairies permettant une intégration plus aisée dans le code.

GitHub ne fournit pas nativement une librairie pour Java mais donne une liste de quelques librairies tierces, c'est là qu'est indiquée celle qui nous intéresse : [GitHub for Java](http://github-api.kohsuke.org/).

La documentation présente sur le [site](https://github-api.kohsuke.org/) est essentiellement pour se connecter à GitHub (sur une organisation  ou non) et pour le reste c'est simplement la [javadoc](https://github-api.kohsuke.org/apidocs/index.html) de l'API, à l'ancienne :wink:. A noter que le nom des classes et méthodes permet aussi de facilement retrouver ce que font les méthodes dans le détail en regardant la documentation de l'API REST correspondante sur le site de GitHub.

A noter que la [mailing](https://groups.google.com/forum/#!forum/github-api) list a l'air plus ou moins abandonnée au profit des issues du [repository](https://github.com/hub4j/github-api) GitHub.

## Activer les dépendances

Bon, les présentations sont faites, place à l'action et on commence par activer la dépendance (maven dans mon cas).

```xml
    <!-- GitHubAPI for Java -->
    <dependency>
      <groupId>org.kohsuke</groupId>
      <artifactId>github-api</artifactId>
      <version>1.116</version>
    </dependency>
```

La librairie ne vient pas avec des classes utilitaires pour les tests unitaires, pour ma part je suis parti sur l'utilisation de [mock server](https://www.mock-server.com/). Cela tombe bien car en regardant les tests unitaires de la librairie, ils utilisent aussi cette librairie pour *mocker* les accès à GitHub et vérifier que les requêtes envoyées à GitHub sont bien formées.


## Premières utilisations
La première chose à faire est s'authentifier, bien entendu utiliser un token est fortement conseillé :wink:. Il y a beaucoup de mode d'authentification (login et password, juste token, ...), pour l'exemple j'utilise la connexion anonyme car le repository visé est public.

```java
GitHub.connectAnonymously();
```

La librairie a été pensée pour être utilisée comme une sorte de DSL en Java : chaque classe possède des méthodes statiques qui renvoient un objet permettant lui-même d'appeler des méthodes renvoyant un autre objet avec ses propres méthodes, ...

Par exemple pour lister les repositories publics de l'utilisateur *philippart-s* il suffit d'ajouter à notre instruction de connexion:
```java
int nbRepositories = GitHub.connectAnonymously()
  .getUser("philippart-s")
  .getPublicRepoCount();

System.out.println(String.format("nommbre de repositories de philippart-s : %s", nbRepositories));
```

Plutôt simple et efficace ! :sunglasses:

Bien entendu il est possible de faire des modifications sur le contenu du repository, les issues, les pull requests, ...

Certaines de ces actions ne seront pas possibles sans être authentifié avec un token.

```java
GitHub.connect("philippart-s", args[0]) //arg[0] est le token associé au compte
        .getUser("philippart-s")
        .getRepository("githubApi4Java")
        .createIssue("Issue créée avec GitHub API for Java !!!")
        .body("Cette issue est vraiment cool ! :wink:")
        .assignee("philippart-s")
        .create();
```

Comme je l'indiquais il est aussi possible d'ajouter des fichiers directement au repository en effectuant un *commit*, là encore l'API nous guide pour le faire simplement.

```java
GitHub.connect("philippart-s", args[0]) //arg[0] est le token associé au compte
        .getUser("philippart-s")
        .getRepository("githubApi4Java")
        .createContent()
        .content("Hello workd !!!")
        .path("HelloWorld.md")
        .message(":tada: My first commit !")
        .commit();
```

## Utilisation avancée
⚠️ Tous les appels effectués sont *décomptés* du nombre de requêtes autorisées par API (rate limite) qui est de 5000 requêtes par heure en mode authentifié. Il y a aussi des variations qui sont apportées selon le mode entreprise, anonyme, ... 

La [documentation](https://docs.github.com/en/free-pro-team@latest/rest/overview/resources-in-the-rest-api#rate-limiting) GitHub explique cela beaucoup mieux que moi :wink:.

C'est à prendre en compte si on souhaite effectuer de nombreuses requêtes, notamment de commit de fichiers. Pour cela il est possible d'utiliser des notions git de plus bas niveau : [Git DataBase](https://docs.github.com/en/free-pro-team@latest/rest/reference/git).

Cela va permettre, notamment, de préparer une arborescence complète de fichier et de la commiter en une requête comme on le ferait avec un push qui embarque plusieurs commits sauf que dans notre cas il y aura un seul commit.

Le code s'en retrouve un peu plus verbeux et moins accessible mais cela fonctionne plutôt bien !
```java
GHRepository repo = GitHub.connect("philippart-s", args[0]) //arg[0] est le token associé au compte
                            .getUser("philippart-s")
                            .getRepository("githubApi4Java");

// Référence vers la branche main
GHRef mainRef = repo.getRef("heads/main");

// SHA1 du dernier commit sur la main
String mainTreeSha = repo
        .getTreeRecursive("main", 1)
        .getSha();

// Création de l'arbre à commiter
GHTreeBuilder ghTreeBuilder = repo.createTree()
                                    .baseTree(mainTreeSha);
ghTreeBuilder.add("HelloWorld1.md", "Hello world 1 !!!", false);
ghTreeBuilder.add("HelloWorld2.md", "Hello world 2 !!!", false);

// Récupération du SHA1
String treeSha = ghTreeBuilder.create().getSha();

// Création du commit pour le tree
String commitSha = repo.createCommit()
                        .message(":tada: My global commit !")
                        .tree(treeSha)
                        .parent(mainRef.getObject().getSha())
                        .create()
                        .getSHA1();

// Positionnement de la branche main sur le dernier commit
mainRef.updateTo(commitSha);
```

Voilà maintenant il ne vous reste plus qu'à imaginer tous les cas d'utilisations qui vous passent par la tête !

L'ensemble des sources est disponible sur le [projet](https://github.com/philippart-s/githubApi4Java) GitHub.