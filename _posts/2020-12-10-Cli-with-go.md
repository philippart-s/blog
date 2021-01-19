---
title: "Créer un CLI multiplateformes en GO"
date: 2022-01-17
#excerpt: 
classes: wide
categories:
  - Articles
  - Code
tags:
  - GO
---
## Mais pourquoi un CLI ?
Pour des raisons professionnelles mais aussi personnelles je me suis mis dans l'idée d'écrire un Command Line Interface (un CLI quoi :wink:).
L'idée est assez simple : avoir un CLI multi-plateformes (en fait si il tourne sur Linux et Windows ça me va !) qui me permet d'effectuer des actions via des appels d'API REST d'une application distante.
Voilà pour les specs !

## Première question : quel langage ?
C'est la première chose qui m'est venue à l'esprit : ok mes langages où je suis à l'aise sont Java et Groovy ... 
Il me faudrait donc une JVM (sans allez plus loin dans les possibilités de génération de code natif) et ce n'est pas forcément l'image que j'ai d'un CLI :thinking:.

Alors que faire ? 

Eh bien demander à son réseau !

![twitter-question]({{ site.url }}{{ site.baseurl }}/assets/images/go-cli/twitter-question-CLI.png)

Et je n'ai pas été déçu ! J'en repars avec une jolie liste de courses : 
 - [Go](http://golang.org/){:target="_blank"}
 - [Rust](https://www.rust-lang.org/){:target="_blank"}
 - [Python](https://www.python.org/){:target="_blank"}
 - [Deno](https://deno.land/){:target="_blank"}
 - [GraalVM](https://www.graalvm.org/){:target="_blank"} Native + [Kotlin](https://kotlinlang.org/){:target="_blank"}
 - [Picocli](https://picocli.info/){:target="_blank"} (Java) avec [QuarkusIO](https://quarkus.io/){:target="_blank"}

Bon c'est sûr qu'il y en a d'autres mais là ça va il y a de quoi m'occuper :wink:. Pas sûr que je vais tout faire mais bon ça fait une bonne base de travail, et pour choisir je vais faire un POC (on aime bien faire ça : on a une excuse de faire du code moche :laughing:).
Le POC est assez simple : j'affiche les dix prochains événements de mon agenda Google.

```bash
mon-cli nextEvent

Upcoming events:
blabla (2021-01-16)
blabla (2021-01-29T08:00:00+01:00)
blabla (2021-01-30)
blabla (2021-02-11T18:15:00+01:00)
blabla (2021-02-13)
blabla (2021-02-27)
blabla (2021-03-13)
blabla (2021-03-27)
blabla (2021-04-10)
blabla (2021-04-13T20:00:00+02:00)
```
L'[API de Google](https://developers.google.com/calendar/){:target="_blank"} en elle-même est très bien documentée et pour ne rien gâcher il y a des snipsets de code pour les appels.

Bon eh bien il n'y a plus qu'à ! Et le premier POC, vous vous en doutez ce sera du Go et le sujet de cet article !

## Par où commencer ?
Je ne connais pas du tout Go du coup il va falloir que je me documente : 
 - la [doc](https://golang.org/){:target="_blank"} officielle
 - [A Tour of Go](https://tour.golang.org/welcome/1){:target="_blank"} : tuto interactif pour faire ses premiers pas en Go
 - le très bon [article](https://dzone.com/articles/how-to-create-a-cli-in-go-in-few-minutes){:target="_blank"} d'Aurélie Vache sur comment écrire un CLI en Go (ça tombe bien !)

Bon je ne suis pas expert mais cela va me permettre de jeter les premières bases pour mon POC.

Je n'irai pas dans le détail car je ne ferai que paraphraser Aurélie et la doc officielle, la seul chose que je n'ai pas faite par rapport à l'article d'Aurélie est l'installation de GVM car je n'ai pas besoin, pour mes tests, de gérer plusieurs version de Go.

Je peux juste indiqué que j'ai utilisé VsCode et le plugin [vscode-go](https://github.com/golang/vscode-go){:target="_blank"} qui est plutôt bien fait même si je n'arrive pas avoir la completion pour les fonctions issues des libs externes ... Avec Intellij cela fonctionne (est étonnant :wink: ?) mais l'idée était d'avoir un IDE plus léger, bon un (petit) mauvais point.

Comme je l'ai indiqué j'ai, en suivant les bons conseils d'Aurélie, utilisé [Cobra](https://github.com/spf13/cobra){:target="_blank"} pour mes tests. C'est bien cela fait plein de trucs pour moi ! Par contre je me retrouve avec pas mal de *magic code* mais c'est le jeu en utilisant une lib externe dans un langage inconnu.

## Un peu de code
:warning: **Je tiens à mettre un gros disclaimer ici : ne connaissant pas le Go il y a moyen que le code qui vient pique les yeux des développeurs Go ! :laughing: :warning: 










Suivre le tuto d'Aurélie : https://dzone.com/articles/how-to-create-a-cli-in-go-in-few-minutes
:warning:
 - GOPATH à positionner => https://golang.org/doc/gopath_code.html
 - il faut créer le chemin destination : philippart-s/go-cli
 - il faut ajouter --package-name par rapport à l'article et le chemin destination : cobra init --pkg-name github.com/philippart-s/go-cli
 - Utiliser l'extention VScode https://github.com/golang/vscode-go et faire les mises à jours jusqu'à ce qu'il n'en reste plus (Go:Update/Install Tools)


