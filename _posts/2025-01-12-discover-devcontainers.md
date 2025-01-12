---
title: "🧑‍💻 A la découverte des Dev Containers 🐳"
classes: wide
excerpt: "Quand la configuration des IDE est aussi simple qu'une image Docker"
categories:
  - Code
  - Docker
  - IDE
  
tags:
  - DevContainers

---
![un développeur assis à son bureau]({{ site.url }}{{ site.baseurl }}/assets/images/discover-devcontainers/illustration.png){: .align-center}
[@wildagsx](https://twitter.com/wildagsx){:style="font-size: smaller"}{:target="_blank"}{: .align-right}<br/>


Depuis maintenant de nombreuses années j'ai dans ma _todo_ liste le test des [Dev Containers](https://containers.dev/){:target="_blank"}.
Les _Dev Containers_ c'est quoi ?
En deux mots, cela vous permet de configurer votre environnement de développement dans une image Docker (ou toute autre solution de gestion de conteneurs).

Pour quelle raison me direz-vous 🤔 ?

Eh bien déjà parce que c'est rigolo à faire (ne jamais sous estimé la côté fun & geek 😉) et ensuite car cela peut rendre bien des services.
Je viens de changer de machine et, du coup, c'était le moment parfait : soit je réinstalle tout sur ma machine, soit je trouve un moyen de ne pas installer tout et n'importe quoi et de rentrer dans l'enfer des compatibilités des logiciels pour le dev.

C'est là, pour moi, le grand plus de _Dev Containers_ : permettre d'avoir plusieurs configurations de développement sans problèmes de collisions ou de devoir installer des outils pour gérer plusieurs versions d'un même SDK par exemple.

Dans mon cas je peux citer Java bien sûr, mais aussi NodeJS, Python, Ruby, ...

L'objectif est le même pour toutes ces technologies : ne pas avoir à jongler entre les différentes versions selon mon besoin.

L'autre aspect, et on y reviendra plus tard, est la possibilité aussi d'avoir besoin de faire des modifications sur un projet sans être sur sa machine habituelle.
Là encore, les _Dev Containers_ offres la possibilité de rendre _portable_ notre environnement de développement.
On retrouve la philosophie des CDE (Cloud Development Environments) : permettre le démarrage d'un environnement de développement prêt à l'emploi pour un projet avec la configuration _as code_ et donc qui suit parfaitement le cycle de vie du projet.
Les CDE, d'ailleurs, lorgnent de plus en plus vers la compatibilité avec les _Dev Containers_.

# Le POC : mon blog 

Le billet de blog que vous êtes en train de lire a comme base [Jekyll](https://jekyllrb.com/){:target="_blank"} lui-même basé sur [Ruby](https://www.ruby-lang.org/en/){:target="_blank"}.

Ruby, je n'en fait pas et le seul moment où j'en ai besoin c'est pour mon blog, clairement je n'ai pas un gros besoin d'avoir tout le tooling continuellement installé et à jour sur ma machine.
C'est un exemple parfait, pour moi, de l'utilisation des _Dev Containers_.
J'ai besoin d'installer Ruby puis un _bundler_ et enfin _Jekyll_ en lui-même.
Rien de bien compliqué, ce qui m'arrange pour commencer mon exploration des _Dev Containers_ 🧑‍💻.

# L'IDE : VSCode

On ne va pas se mentir, utiliser les _Dev Containers_ en dehors de VSCode n'est pas une chose aisée ... 
C'est peut être le premier warning à noter, si vous aimer utiliser d'autres IDE vous allez pouvoir faire des choses ave la [CLI](https://github.com/devcontainers/cli){:target="_blank"} par exemple mais pour moi l'expérience développeur est la meilleure dans VSCode.

> A noter que Intellij de JetBrains permet une intégration des [Dev Container dans sa version ultimate](https://www.jetbrains.com/help/idea/connect-to-devcontainer.html){:target="_blank"}, je ne l'ai pas encore testée.

Cette restriction d'intégration ou plutôt cette facilité d'intégration doit vous guider vers le fait que cette solution et pour tout remplacer ou dans certains cas uniquement.
A titre d'exemple, je donne pas mal de conférences ou fait pas mal de démos, c'est une bonne chose de pouvoir figer son environnement de développement et qu'il soit reproductible à loisir.

L'intégration des _Dev Container_ à VSCode est native, il n'y a rien à installer.
Si votre VSCode n'a pas l'extension d'installée, il suffit de l'installer via ce [lien](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers){:target="_blank"}.

Ensuite, rien à faire l'extension reconnaîtra automatiquement les fichiers _Dev Container_ lorsqu'ils sont présents dans le projet.

# Initialisation de la configuration

Pour initialiser votre configuration, autant utiliser VSCode et son extension : CMD+SHIFT+P puis `Dev Containers: Add Dev Containers configuration Files...`.
Ensuite, je vous conseille de choisir de mettre la configuration dans le workspace du projet afin d'avoir une configuration propre au projet (souvenez-vous la reproductibilité d'un environnement 😉).
Et là il vous suffit de choisir une configuration (en fait une image Docker), correspondant à votre besoin, dans mon cas : _Jekyll_.
Ensuite vous pouvez avoir le choix d'une version de l'image en particulier, dans le cas de Jekyll quelle version de Debian.
Puis, potentiellement de [features](https://containers.dev/implementors/features/){:target="_blank"} à activer.

> Une feature, en deux mots, est une portion de configuration avec ou non un outil déjà pré-configurée dans votre image.
> A titre d'exemple : si vous souhaiter avoir la [CLI de GitHub](https://github.com/devcontainers/features/tree/main/src/github-cli){:target="_blank"}, il existe une feature qui vous permettra qu'elle soit présente dans votre image sans avoir à le faire manuellement et à gérer ses potentielles mises à jour (les features sont versionnées).

Dans mon cas je vais commencer simple : juste la configuration pour faire de Jekyll.
Et à la fin, VSCode vous propose de ré-ouvrir votre projet avec la configuration du _Dev Container_ qu'il vient de créer.

> ⚠ A ce stade je dois vous indiquer un nouveau warning : Dev Container demande d'avoir un logiciel de run et construction d'images de conteneurs, dans mon cas j'utilise Docker mais n'oubliez pas que c'est un pré-requis fort 😉


TODO: vidéo

```json
// For format details, see https://aka.ms/devcontainer.json. For config options, see the
// README at: https://github.com/devcontainers/templates/tree/main/src/jekyll
{
	"name": "Jekyll",
	// Or use a Dockerfile or Docker Compose file. More info: https://containers.dev/guide/dockerfile
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye"

	// Features to add to the dev container. More info: https://containers.dev/features.
	// "features": {},

	// Use 'forwardPorts' to make a list of ports inside the container available locally.
	// "forwardPorts": [],

	// Uncomment the next line to run commands after the container is created.
	// "postCreateCommand": "jekyll --version"

	// Configure tool-specific properties.
	// "customizations": {},

	// Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
	// "remoteUser": "root"
}
```
Voici ce que donne le fichier fraîchement créé par l'extension VSCode.
On voit ici qu'il y a des options que l'on peut paramétrer et / ou activer.
On revriendra sur certaines d'entre elles plus loin dans cet article.

# Utilisation 

Une fois que vous avez accpeter d'ouvrir votre projet en mode _Dev Container_ ou si ce n'est pas le cas ouvert via l'extension : CMP+SHIFT+P puis _Dev Containers: Open Container Configuration File..._ vous vous retrouvez ... dans votre VSCode comme si rien n'avait changé !

Vous remarquez peut-être un changement dans votre terminal : `vscode ➜ /workspaces/blog (dev-container) $`.
Et aussi en bas à gauche de VSCode vous voyez, par exemple `Dev Container: Jekyll @ desktop-linux`.
En réalité, au fond les _Dev Containers_ ne sont (avec des gros guillemets) qu'un [vscode server](https://code.visualstudio.com/docs/remote/vscode-server){:target="_blank"} local.
On comprends mieux pourquoi les CDE s'y intéressent et, pour certains, offrent une compatibilité pour les réutiliser.

Ensuite je peux utiliser comme bon me semble cet environnement en installant mes outils et autres SDK dont j'ai besoin, sans polluer ma machine : 
```bash
$ gem install jekyll bundler
Successfully installed jekyll-4.3.4
Fetching bundler-2.6.2.gem
Successfully installed bundler-2.6.2
2 gems installed

A new release of RubyGems is available: 3.5.23 → 3.6.2!
Run `gem update --system 3.6.2` to update your installation.

$ bundle exec jekyll serve
To use retry middleware with Faraday v2.0+, install `faraday-retry` gem
/usr/local/rvm/gems/default/gems/httpclient-2.8.3/lib/httpclient.rb:19: warning: mutex_m was loaded from the standard library, but will no longer be part of the default gems starting from Ruby 3.4.0.
You can add mutex_m to your Gemfile or gemspec to silence this warning.
Configuration file: /workspaces/blog/_config.yml
            Source: /workspaces/blog
       Destination: /workspaces/blog/_site
 Incremental build: disabled. Enable with --incremental
      Generating... 
      Remote Theme: Using theme mmistakes/minimal-mistakes
       Jekyll Feed: Generating feed for posts
                    done in 3.679 seconds.
 Auto-regeneration: enabled for '/workspaces/blog'
    Server address: http://127.0.0.1:4000
  Server running... press ctrl-c to stop.

```

Et là une chose très intéressante avec les _Dev Containers_ et l'extension de VSCode : le _forward_ de ports est automatiquement géré !
Pas de configuration compliquée pour accéder à ce qui est exposé par mon container, comme on le voit mon blog est disponible sur l'URL locale `http://127.0.0.1:4000` 😍.

## L'avantage du mode local

Tout ce que vous faîtes dans vos fichiers n'est pas sur une copie de vos fichiers dans votre container mais bien sur vos fichiers directement de votre machine, pas de synchro à prévoir et pas de lock sur l'environnement _Dev Container_.
Vous pouvez toujours éditer localement vos fichiers.
C'est une grose différence avec le mode SSH et un vscode server remote.

Autre chose très apreciable : c'est votre machine, votre VSCode mais aussi votre configuration Git qui est utilisée, pas de synchro à faire là non plus.
Les commandes git et aussi les push utilisent vos configurations locales, dans mon cas par exemple la signature des commits et les credentials de mon compte GitHub 😎.

## La customisation

TODO : action post run / maj de version / features


# En conclusion

J'espère que vous avez pu, simplement, vous rendre compte comme il est assez facile avec Quarkus et LangChain4j d'ajouter des capacités à notre application de _chat bot_.
Je vais continuer à ajouter quelques autres choses plus ou moins utiles dans les articles suivants 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog){:target="_blank"} 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.

L'ensemble des sources des exemples est disponible dans le repository GitHub [langchain4j-discovery](https://github.com/philippart-s/discover-langchain4j){:target="_blank"}.