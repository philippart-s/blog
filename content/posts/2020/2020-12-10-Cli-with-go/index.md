---
title: "Créer un CLI multiplateformes en Go"
link: /2020-12-10-Cli-with-go
tags: 
  - Go
author: wilda
---

## Mais pourquoi un CLI ?
Pour des raisons professionnelles (mais aussi personnelles) je me suis mis dans l'idée d'écrire un Command Line Interface (un CLI quoi :wink:).
L'idée est assez simple : avoir un CLI multiplateformes (en fait si il tourne sur Linux et Windows ça me va !) qui me permet d'effectuer des actions via des appels d'API REST d'une application distante.
Voilà pour les specs !

## Première question : quel langage ?
C'est la première chose qui m'est venue à l'esprit : ok mes langages où je suis à l'aise sont Java et Groovy ... 
Il me faudrait donc une JVM (sans aller plus loin dans les possibilités de génération de code natif) et ce n'est pas forcément l'image que j'ai d'un CLI :thinking:.

Alors que faire ? 

Eh bien demander à son réseau !

![twitter-question](twitter-question-CLI.png)

Et je n'ai pas été déçu ! J'en repars avec une jolie liste de courses : 
 - [Go](http://golang.org/)
 - [Rust](https://www.rust-lang.org/)
 - [Python](https://www.python.org/)
 - [Deno](https://deno.land/)
 - [GraalVM](https://www.graalvm.org/) Native + [Kotlin](https://kotlinlang.org/)
 - [Picocli](https://picocli.info/) (Java) avec [QuarkusIO](https://quarkus.io/)

C'est sûr qu'il y en a d'autres, mais là ça va il y a de quoi m'occuper :wink:. Pas sûr que je vais tout faire mais bon ça fait une bonne base de travail. Pour choisir je vais faire un POC (on aime bien faire ça : on a une excuse pour faire du code moche :laughing:).
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
L'[API de Google](https://developers.google.com/calendar/) en elle-même est très bien documentée et pour ne rien gâcher il y a des snipsets de code pour les appels.

Eh bien il n'y a plus qu'à ! Et le premier POC, vous vous en doutez ce sera du Go ! Il y avait un indice dans le titre de l'article ....

## Par où commencer ?
Je ne connais pas du tout Go, du coup il va falloir que je me documente : 
 - la [doc](https://golang.org/) officielle
 - [A Tour of Go](https://tour.golang.org/welcome/1) : tuto interactif pour faire ses premiers pas en Go
 - le très bon [article](https://dzone.com/articles/how-to-create-a-cli-in-go-in-few-minutes) d'Aurélie Vache sur comment écrire un CLI en Go (ça tombe bien !)

Je ne suis pas expert mais cela va me permettre de jeter les premières bases pour mon POC.

Je n'irai pas dans le détail car je ne ferai que paraphraser Aurélie et la doc officielle, la seul chose que je n'ai pas faite par rapport à l'article d'Aurélie est l'installation de GVM car je n'ai pas besoin, pour mes tests, de gérer plusieurs versions de Go.

Je peux juste indiquer que j'ai utilisé VsCode et le plugin [vscode-go](https://github.com/golang/vscode-go) qui est plutôt bien fait même si je n'arrive pas avoir la completion pour les fonctions issues des libs externes ... Avec Intellij cela fonctionne (est-ce étonnant :wink: ?) mais l'idée était d'avoir un IDE plus léger, bon un (petit) mauvais point.

Comme je l'ai indiqué j'ai, en suivant les bons conseils d'Aurélie, utilisé [Cobra](https://github.com/spf13/cobra) pour mes tests. C'est bien cela fait plein de trucs pour moi ! Par contre je me retrouve avec pas mal de *magic code* mais c'est le jeu en utilisant une lib externe dans un langage inconnu.

## Un peu de code ... enfin presque !
⚠️ **Je tiens à mettre un gros disclaimer ici : ne connaissant pas le Go il y a moyen que le code qui suive pique les yeux des développeurs Go !** :laughing: ⚠️ 

Alors une fois cobra installé, il suffit de suivre la doc et d'initialiser un projet, mais avant ça il faut choisir un endroit où mettre les sources :wink:, chose banale me direz-vous ... pas forcément en Go où on ne nous laisse pas trop le choix (en tout cas c'est ce que j'ai compris).
En effet, il faut commencer par déclarer la racine de l'endroit où on va mettre tous ses éléments Go en renseignant la variable *GO_PATH* (par exemple /go).
Ensuite dans ce répertoire on retrouvera les sources et les binaires dont on a besoin pour développer notre projet.
C'est ce qui s'appelle le [workspace](https://golang.org/doc/gopath_code.html).

Sur ce que j'ai vu de Go dans le développement de mon POC c'est cela qui me dérange le plus : le fait d'avoir l'impression de ne pas pouvoir choisir plein de choses. Que ce soit sur la configuration mais aussi dans mon propre code. 
Beaucoup de choses sont imposées, on les suit sinon j'ai l'impression que l'on ne peut pas faire grand chose.
J'ai peut être été trop (mal ?) habitué avec d'autres langages mais je trouve cela un peu contraignant.

Une fois ces petits états d'âme passés, on va tester un peu de code pour voir si c'est un client crédible pour écrire mon CLI.

Cobra aide beaucoup, en une commande on peut initialiser notre projet : 
```bash
$ cobra init --pkg-name github.com/philippart-s/gogli gogli
Your Cobra application is ready at
/home/stephane/Développements/go/src/github.com/philippart-s/gogli
```
:tada: On a un beau projet avec les fichiers et le squelette de code déjà présent !

```
gogli
├── cmd
│   └── root.go
├── LICENSE
└── main.go
```
Il ne reste plus qu'à le tester
```bash
$ go install

$ gogli                             
A longer description that spans multiple lines and likely contains
examples and usage of using your application. For example:

Cobra is a CLI library for Go that empowers applications.
This application is a tool to generate the needed files
to quickly create a Cobra application.
```

Reste plus qu'à ajouter une commande à mon CLI : *next-event* qui va nous afficher les 10 prochains événements de mon agenda Google.
Là encore Cobra va nous aider :
```bash
$ cobra add next-event                                       
nextEvent created at /home/stephane/Développements/go/src/github.com/philippart-s/gogli

$ go install

$ gogli nextEvent 
nextEvent called
```

Eh bien voilà merci, le POC est fini !

Non restez, on va quand même essayer d'aller jusqu'à afficher les événements et peut-être même lancer un éditeur de code quand même !

On file sur la page développeur de google pour l'API du calendrier Google (voir plus haut dans l'article) pour tout configurer et récupérer le code nécessaire à mon POC (pas sûr que l'on va coder tant que ça :wink:).
Je vous ferai grâce du code fournit car je n'ai rien touché à ce que google fournit comme exemple. 
J'ai juste collé le code dans un fichier *googleCalendar.go* et appelé la fonction dans mon code :
```go
var nextEventCmd = &cobra.Command{
	Use:   "nextEvent",
	Short: "A brief description of your command",
	Long: `A longer description that spans multiple lines and likely contains examples
and usage of using your command. For example:

Cobra is a CLI library for Go that empowers applications.
This application is a tool to generate the needed files
to quickly create a Cobra application.`,
	Run: func(cmd *cobra.Command, args []string) {
		printNextEvents()
	},
}
```

```bash
$ go install                                                   

$ gogli nextEvent
Upcoming events:
xxxxxx (2021-01-29T08:00:00+01:00)
xxxx (2021-01-30)
xxxxx (2021-02-11T18:15:00+01:00)
xxx (2021-02-13)
xxxxxx (2021-02-27)
xxx (2021-03-13)
xxxxx (2021-03-27)
xxxxx (2021-04-10)
xx (2021-04-13T20:00:00+02:00)
xxxxx (2021-04-20T14:00:00+02:00)
```

Eh bien voilà le CLI de mon POC est terminé et le peu de code que j'ai produit a été de copier / coller du code depuis Google et de faire un appel de fonction !

Le CLI en lui-même, sous Ubuntu, fait 18 Mo.

Plutôt efficace mais j'avoue qu'en essayant de comprendre un peu plus le code et de le bidouiller un peu je reste encore indécis sur le langage, on verra si je vais plus loin après mes comparaisons avec les autres langages pour découvrir un peu plus en profondeur Go.
Je trouve, pour un développeur Java comme moi, le ticket d'entrée assez important. 

La partie écriture en elle-même ne révolutionne pas le monde et, à force d'écriture, on comprends les différences.
Ce qui me perturbe est l'impression, parfois, de revenir à faire du C avec les pointeurs et d'avoir moins de *guides* pour développer. 
Les nombreuses années à faire des choses structurées avec des classes, de l'héritage, des objets ... et de manière très formelle dans l'écriture font que, je trouve, on est un peu perdu au début face à du source Go.

Je me trompe certainement par manque d'habitude, mais je ne me vois pas écrire toute une application de gestion en Go. Par contre je comprends pourquoi il séduit autant côté Ops pour des profils issus du monde du développement : on a, je trouve, beaucoup plus de possibilités qu'en bash par exemple.


##  One more things !
J'allais oublier un truc !
Je voulais un CLI multiplateformes (ou du moins qui fonctionne sur Linux et sur Windows), là, j'avoue que Go m'a bluffé, une seule commande :
```bash
$ GOOS=windows GOARCH=386 go install
```
Et le tour est joué, dans le répertoire *bin* du GO_PATH apparaît un répertoire *windows_386* et dedans un exécutable *gogli.exe* !

Ensuite il ne reste plus qu'à l'exécuter dans un command DOS ou un powershell.
```console
C:\temp> dir
    Répertoire : C:\temp
Mode                LastWriteTime         Length Name
----                -------------         ------ ----
-a----       26/01/2021     18:46            430 credentials.json
-a----       26/01/2021     13:47       15357440 gocli.exe
-a----       26/01/2021     18:47            385 token.json

C:\temp> ./gocli nextEvent
Upcoming events:
xxxxx (2021-01-29T08:00:00+01:00)
xx (2021-01-30)
xxxxxx (2021-02-11T18:15:00+01:00)
xxxxx (2021-02-13)
xx (2021-02-27)
xx (2021-03-13)
xxxx (2021-03-27)
xxxxxx (2021-04-10)
xx (2021-04-13T20:00:00+02:00)
xxxxxx (2021-04-20T14:00:00+02:00)
```
Vraiment bluffant la facilité avec laquelle on peut générer un livrable quelque soit la cible, plus d'informations dans la [documentation](https://golang.org/doc/install/source#environment) pour savoir toutes les cibles possibles.

Une dernière chose : il se peut qu'il vous manque des dépendances pour les cibles générées car par défaut Go récupère les dépendances pour l'architecture de la machine qui exécute la commande *go get* si on ne précise rien. 
Du coup pour générer un CLI avec Cobra pour Windows il faut récupérer les dépendances Cobra en indiquant l'architecture cible voulue : `$ GOOS=windows go get -u github.com/spf13/cobra`.

Voilà c'est fini, vous n'êtes pas devenu expert Go mais comme moi vous commencez à voir les possibilités de ce langage et notamment ce qu'il est possible de faire pour avoir un CLI (avec Cobra).

La suite, pour moi : dérouler la liste de langages à tester et refaire mon POC :wink:. Le prochain : Rust !

Comme d'habitude l'ensemble des sources est disponible sur le [repository](https://github.com/philippart-s/go-cli) GitHub.