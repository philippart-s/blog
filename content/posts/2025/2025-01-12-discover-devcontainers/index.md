---
title: "🧑‍💻 A la découverte des Dev Containers 🐳"
description: "Quand la configuration des IDE est aussi simple qu'une image Docker"
link: /2025-01-12-discover-devcontainers
tags: 
  - DevContainers
image: illustration.png
figCaption: "@wildagsx"
author: wilda
---

Depuis maintenant de nombreuses années, j'ai dans ma _todo_ liste le test des [Dev Containers](https://containers.dev/).
Les _Dev Containers_ c'est quoi ?
En deux mots, cela vous permet de configurer votre environnement de développement dans une image Docker (ou toute autre solution de gestion de conteneurs).

Pour quelle raison me direz-vous 🤔 ?

Eh bien déjà parce que c'est rigolo à faire (ne jamais sous estimer la côté fun & geek 😉) et ensuite car cela peut rendre bien des services.
Je viens de changer de machine et, du coup, c'était le moment parfait : soit je réinstalle tout sur ma machine, soit je trouve un moyen de ne pas installer tout et n'importe quoi et de rentrer dans l'enfer des compatibilités des logiciels pour le dev.

C'est là, pour moi, le grand plus de _Dev Containers_ : permettre d'avoir plusieurs configurations de développement sans des problèmes de collisions ou de devoir installer des outils pour gérer plusieurs versions d'un même SDK par exemple.

Dans mon cas je peux citer Java bien sûr, mais aussi NodeJS, Python, Ruby, ...

L'objectif est le même pour toutes ces technologies : ne pas avoir à jongler entre les différentes versions selon mon besoin.

L'autre aspect, et nous y reviendrons plus tard, est la possibilité aussi d'avoir besoin de faire des modifications sur un projet sans être sur sa machine habituelle.
Là encore, les _Dev Containers_ offrent la possibilité de rendre _portable_ notre environnement de développement.
On retrouve la philosophie des CDE (Cloud Development Environments) : permettre le démarrage d'un environnement de développement prêt à l'emploi pour un projet avec la configuration _as code_ et donc qui suit parfaitement le cycle de vie du projet.
Les CDE, d'ailleurs, lorgnent de plus en plus vers la compatibilité avec les _Dev Containers_.

Le dernier exemple que l'on peut aussi donner concerne les projets Open Source.
Quel enfer de passer plusieurs heures pour avoir une configuration qui permet de compiler et tester le projet !
Les _Dev Containers_ permettent d'éviter cette douloureuse phase.

# ⚗️ Le POC : mon blog

Le billet de blog que vous êtes en train de lire a comme base [Jekyll](https://jekyllrb.com/), lui-même basé sur [Ruby](https://www.ruby-lang.org/en/).

Ruby, je n'en fait pas et le seul moment où j'en ai besoin c'est pour mon blog.
Clairement, je n'ai pas un gros besoin d'avoir tout le tooling continuellement installé et à jour sur ma machine.
C'est un exemple parfait, pour moi, de l'utilisation des _Dev Containers_.
J'ai besoin d'installer Ruby, puis un _bundler_, et enfin _Jekyll_ lui-même.
Rien de bien compliqué, ce qui m'arrange pour commencer mon exploration des _Dev Containers_ 🧑‍💻.

# 🧑‍💻 L'IDE : VSCode

On ne va pas se mentir, utiliser les _Dev Containers_ en dehors de VSCode n'est pas une chose aisée ...
C'est peut être le premier warning à noter : si vous aimer utiliser d'autres IDE vous allez pouvoir faire des choses avec la [CLI](https://github.com/devcontainers/cli) par exemple, mais pour moi l'expérience développeur est la meilleure dans VSCode.

> A noter que Intellij de JetBrains permet une intégration des [Dev Container dans sa version ultimate](https://www.jetbrains.com/help/idea/connect-to-devcontainer.html), je ne l'ai pas encore testée.

Cette restriction d'intégration ou plutôt cette facilité d'intégration peut être un premier choix d'utiliser ou non les _Dev Containers_.
Je pense que le mieux est de voir si le jeu en vaut la chandelle en termes de use case.
A titre d'exemple, je donne pas mal de conférences ou fait pas mal de démos, c'est une bonne chose de pouvoir figer son environnement de développement et qu'il soit reproductible à loisir.

L'intégration des _Dev Container_ à VSCode est native, il n'y a rien à installer.
Si votre VSCode n'a pas l'extension d'installée, il suffit de l'installer via ce [lien](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).

Ensuite, rien à faire, l'extension reconnaîtra automatiquement les fichiers _Dev Container_ lorsqu'ils sont présents dans le projet.

# ⚙ Initialisation de la configuration

Pour initialiser votre configuration autant utiliser VSCode et son extension : `CMD+SHIFT+P` puis `Dev Containers: Add Dev Containers configuration Files...`.
Ensuite, je vous conseille de choisir de mettre la configuration dans le workspace du projet afin d'avoir une configuration propre au projet (souvenez-vous : la reproductibilité d'un environnement 😉).
Et là, il vous suffit de choisir une configuration (en fait une image Docker), correspondant à votre besoin, dans mon cas : _Jekyll_.
Ensuite vous pouvez avoir le choix d'une version de l'image en particulier, dans le cas de Jekyll quelle version de Debian.
Puis, potentiellement des [features](https://containers.dev/implementors/features/) à activer.

> Une feature, en deux mots, est une portion de configuration avec ou non un outil déjà pré-configuré dans votre image.
> A titre d'exemple : si vous souhaitez avoir la [CLI de GitHub](https://github.com/devcontainers/features/tree/main/src/github-cli), il existe une feature qui vous permettra qu'elle soit présente dans votre image sans avoir à le faire manuellement et à gérer ses potentielles mises à jour (les features sont versionnées).
> Nous verrons comment les utiliser plus en avant dans cet article.

Dans mon cas je vais commencer simple : juste la configuration pour faire du Jekyll.
Et à la fin, VSCode vous propose de ré-ouvrir votre projet avec la configuration du _Dev Container_ qu'il vient de créer.

> ⚠ A ce stade je dois vous indiquer un nouveau warning : Dev Container demande d'avoir un logiciel de run et construction d'images de conteneurs, dans mon cas j'utilise Docker mais n'oubliez pas que c'est un pré-requis fort 😉

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
On reviendra sur certaines d'entre elles plus loin dans cet article ou dans les suivants.

# ✍️ Utilisation

VSCode vous propose de ré-ouvrir votre projet en mode _Dev Container_. 
Si ce n'est pas le cas c'est possible via l'extension : `CMP+SHIFT+P` puis _Dev Containers: Open Container Configuration File..._ vous vous retrouvez ... dans votre VSCode comme si rien n'avait changé !

Vous remarquez peut-être un changement dans votre terminal : `vscode ➜ /workspaces/blog (dev-container) $`.
Et aussi en bas à gauche de VSCode vous voyez, par exemple `Dev Container: Jekyll @ desktop-linux`.
En réalité, les _Dev Containers_ ne sont (avec des gros guillemets) qu'un [vscode server](https://code.visualstudio.com/docs/remote/vscode-server) local.
On comprends mieux pourquoi les CDE s'y intéressent et, pour certains, offrent une compatibilité pour les réutiliser.

Ensuite, je peux utiliser comme bon me semble cet environnement en installant mes outils et autres SDK dont j'ai besoin, sans polluer ma machine :

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

Une chose très intéressante avec les _Dev Containers_ et l'extension de VSCode : le _forward_ de ports est automatiquement géré !
Pas de configuration compliquée pour accéder à ce qui est exposé par mon container, comme on le voit mon blog est disponible sur l'URL locale `http://127.0.0.1:4000` 😍.

## 💻 L'avantage du mode local

Tout ce que vous faîtes dans vos fichiers n'est pas une copie de vos fichiers dans votre container mais bien sur vos fichiers directement de votre machine, pas de synchro à prévoir et pas de lock sur l'environnement _Dev Container_ (c'est un volume monté sur l'image Docker).
Vous pouvez toujours éditer localement vos fichiers.
C'est une grosse différence avec le mode SSH et un vscode server remote.

Autre chose très appreciable : c'est votre machine, votre VSCode mais aussi votre configuration Git qui est utilisée, pas de synchro à faire là non plus.
Les commandes git et aussi les push utilisent vos configurations locales, dans mon cas, par exemple, la signature des commits et les credentials de mon compte GitHub 😎.

## ⚙️ Le paramétrage

### ⚡️ Exécution de commandes au démarrage

Les _Dev Containers_ permettent d'aller plus loin dans le paramétrage de votre _environnmenet_.
Imaginons que je souhaite lancer _Jekyll_ au démarrage de mon environnement.
Il y a plusieurs façons de le faire, l'une d'elle est d'utiliser l'option `postCreateCommand` dans le `devcontainer.json` précédemment créé.

```json
// For format details, see https://aka.ms/devcontainer.json. For config options, see the
// README at: https://github.com/devcontainers/templates/tree/main/src/jekyll
{
  "name": "Jekyll",
  // Or use a Dockerfile or Docker Compose file. More info: https://containers.dev/guide/dockerfile
  "image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",

  // Features to add to the dev container. More info: https://containers.dev/features.
  // "features": {},

  // Use 'forwardPorts' to make a list of ports inside the container available locally.
  // "forwardPorts": [],

  // Uncomment the next line to run commands after the container is created.
  "postCreateCommand": "bundle exec jekyll serve"

  // Configure tool-specific properties.
  // "customizations": {},

  // Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
  // "remoteUser": "root"
}
```

> J'en profite pour faire une petite digression à ce stade : si vous avez suivi les étapes de cet article au moment d'enregistrer le fichier `devcontainer.json` l'extension VSCode vous propose de reconstruire votre environnement et de le relancer. Pratique non ? 😎
> Comme toujours si cela n'apparaît pas ou si vous n'avez pas le temps de cliquer : `CMD+SHIFT+P` puis `Dev Containers: Rebuild container...`

### ✨ Customisation de l'IDE

Une des choses sympas dans les options possibles est la partie permettant la customisation propre au type d'IDE que vous utilisez.
Dans mon cas, pour cet exemple, c'est VSCode mais vous avez plusieurs supports (voir la [documentation](https://containers.dev/supporting)).
Imaginons, que dans mon cas, je souhaite avoir [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode), une extension permettant le formatage de code source dans de nombreux langages.
Je prends cet exemple à dessein car pour faciliter les diffs de code source, cela peut être avantageux de partager le même outil et la même configuration de mise en forme 😉.  
Je pourrai très bien, à chaque lancement de mon _Dev Containers_, aller dans la vue extensions de VSCode et activer l'extension.
Cela fonctionne mais cela fait des cliques et si une autre personne utilise mon projet je suis dépendant qu'elle l'active, dans la bonne version et de la façon que je souhaite.
C'est là où on va retrouver ce qui a fait ses preuves depuis pas mal d'années maintenant : la configuration as code 😉.

```json
// For format details, see https://aka.ms/devcontainer.json. For config options, see the
// README at: https://github.com/devcontainers/templates/tree/main/src/jekyll
{
  "name": "Jekyll",
  // Or use a Dockerfile or Docker Compose file. More info: https://containers.dev/guide/dockerfile
  "image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",

  // Features to add to the dev container. More info: https://containers.dev/features.
  // "features": {},

  // Use 'forwardPorts' to make a list of ports inside the container available locally.
  // "forwardPorts": [],

  // Uncomment the next line to run commands after the container is created.
  "postCreateCommand": "bundle exec jekyll serve",

  // Configure tool-specific properties.
  "customizations": {
    // Configure properties specific to VS Code.
    "vscode": {
      "settings": { "editor.formatOnSave": true },
      "extensions": ["esbenp.prettier-vscode"]
    }
  }

  // Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
  // "remoteUser": "root"
}
```

> 🔎 Vous retrouverez l'identifiant unique de l'extension dans le market place dans les informations de l'extension.

Dans cet exemple, j'ai activé l'extension _Prettier_ mais aussi modifié le comportement de VSCode pour qu'il appelle le formateur à chaque sauvegarde.

## 🛠️ Outillage

Je vous l'ai déjà dis, les images pré-packagée viennent avec leur outils respectifs (Ruby par exemple dans mon cas).
Cependant il peut vous manquer des outils qui vous sont nécessaires et là encore, les installer manuellement à chaque fois n'est pas une chose agréable et optimisée.
Les _Dev Containers_ viennent avec deux manières de le faire : créer sa propre image ou utiliser la notion de _features_.
Nous verrons la création d'image dans un autre article sur l'utilisation plus avancée des _Dev Containers_, concentrons nous sur la notion de _features_.
Les _features_ permettent donc d'enrichir votre _Dev Container_ via un [catalogue](https://containers.dev/features) de _features_.
Nous allons voir comment les utiliser et comment les paramétrées au mieux pour répondre à nos besoins 😉.

Imaginons que je veuille installer l'outil [JQ](https://jqlang.github.io/jq/):
```json
// For format details, see https://aka.ms/devcontainer.json. For config options, see the
// README at: https://github.com/devcontainers/templates/tree/main/src/jekyll
{
	"name": "Jekyll",
	// Or use a Dockerfile or Docker Compose file. More info: https://containers.dev/guide/dockerfile
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",

	// Features to add to the dev container. More info: https://containers.dev/features.
	"features": {
    "ghcr.io/eitsupi/devcontainer-features/jq-likes:1": {
        "jqVersion": "latest"
    }
	},
	// Use 'forwardPorts' to make a list of ports inside the container available locally.
	// "forwardPorts": [],

	// Uncomment the next line to run commands after the container is created.
	"postCreateCommand": "bundle exec jekyll serve",

	// Configure tool-specific properties.
	"customizations": {
		// Configure properties specific to VS Code.
		"vscode": {
			"settings": {"editor.formatOnSave": false},
			"extensions": ["esbenp.prettier-vscode"]
		}
	}

	// Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
	// "remoteUser": "root"
}
```

Dans ce cas on active une _feature_ avec son nom et on peut, si elle le supporte, lui ajouter des paramètres, dans mon cas la version de JQ.

A ce stade de la découverte des _Dev Containers_ je n'irai pas plus loin sur l'explication des paramètres.
Ce sont ceux proposés par défaut mais il en existe une longue liste en fonction de vos besoins.
La [documentation](https://containers.dev/implementors/json_reference/#general-properties) sera votre meilleure alliée 😉.

## 🪫 Et la performance ?

Pour l'écriture de cet article, j'utilise un Mac Book M3 avec 24 Go de mémoire.
Pour être honnête ne je vois pas du tout de différence avec une installation locale.
Encore une fois, ce premier besoin est simple et ne demande pas forcément une grosse configuration et beaucoup d'outillage.
A voir ce que cela donne quand je vais faire du Java 😉.

Autre chose que l'on peut se poser comme question est la taille des images versus la taille de tout installer sur une machine.
Pour l'image que j'utilise pour la partie Jekyll elle fait 2.4 Go !
C'est plutôt imposant si on compare ce que l'on doit installer qui se résume simplement à Ruby et quelques dépendances !

C'est une chose à prendre en compte : cela va forcément vous ajouter ce genre de sur-consommation mémoire et disque.
Il est toujours possible de faire régulièrement le ménage des images non utilisées mais dans tous les cas cela ne sera pas adapté aux petites configurations mémoires et disques.

Pour minimiser l'impact sur votre machine vous pouvez vous tourner vers les Cloud Development Environments qui sont compatibles avec les _Dev Containers_.
Un des plus connus : [Code Spaces](https://docs.github.com/fr/codespaces) de GitHub.
Cela va vous permettre de n'utiliser que votre navigateur ou votre VSCode local (mais simplement pour la partie IDE et non pas le shell qui va utiliser les ressources distantes).
Comme d'habitude, si cela vous permet de bénéficier de configurations supérieures à votre machine attention à la facturation !

# En conclusion

J'espère que cette découverte en ma compagnie des _Dev Containers_ vous a plus.
Dans les autres parties on plongera un plus en profondeur dans la customisation d'images pour répondre au mieux à vos besoins 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.