---
title: "⚡️ Optimiser les Dev Containers 🐳"
description: "Accélérer et spécialiser les dev containers 🎯"
link: /2025-08-22-devcontainers-optimisations
tags: 
  - DevContainers
image: image-illustration.jpg
figCaption: "@wildagsx"
author: wilda
---

> **TL;DR**  
> Cet article est la troisième partie de la série d'articles sur les Dev Containers, je vous conseille de lire les premiers articles [🧑‍💻 A la découverte des Dev Containers 🐳](2025-01-12-discover-devcontainers) et [🧑‍💻 Aller plus loin avec les Dev Containers 🐳]({site.url}/2025-01-28-devcontainers-advanced) avant de lire celui-ci.  
> L'ensemble des exemples et des définitions des images pour les dev containers se trouvent dans le repository [bear-corp/the-cave](https://github.com/bear-corp/the-cave).

Si vous avez débuté votre voyage dans les dev containers, que ce soit en me lisant ou par votre propre envie, vous êtes certainement arrivé•es à la même conclusion que moi : les dev containers c'est bien, mais niveau performances cela peut, parfois, ne pas être top.

En effet, pour moi, il y a deux problèmes majeurs en utilisant les templates et autres features dans un `devcontainer.json` :
 - la taille des images construites, on verra que l'on ne peut pas faire de magie mais quand même on peut tenter de faire maigrir notre baleine 🐳
 - le temps de build des images lors de modifications ou pour démarrer un nouvel environnement.

C'est principalement le deuxième point qui va m’intéresser : l'optimisation de l'image utilisée pour mon environnement de dev et sa distribution.

> ⚠️ Ce qui suit n'est pas forcément adapté pour un projet OSS car vous allez vous rendre compte que l'on va masquer certains éléments de configuration, ce qui peut faire peur de ne pas savoir ce que l'on installe mais comme c'est dans un container 🤷

# 🏗️ L'approche "prebuilds"

Vous trouverez une explication de ce qui se cache derrière le terme *prebuilds* sur le site des dev containers dans le guide [Speed Up Your Workflow with Prebuilds](https://containers.dev/guide/prebuild).  
Pour résumer ce que c'est en quelques mots : 
 - l'objectif est de créer une image avec l'ensemble des configurations dont vous avez besoin pour vous éviter de devoir builder à chaque fois votre image
 - un autre gain : commencer à optimiser votre image en créant des images depuis un Dockerfile ou en utilisant Docker Compose. Cette étape n'est pas obligatoire comme on le verra 😉.

> 💡 On retrouve la notion de prebuilds aussi dans les CDE comme Codespaces, GitPod ou encore Coders. Ce qui n'est pas étonnant puisque ils sont tous compatibles dev containers 😉.

# 🐳 Construction de l'image de base

> 💡 L'ensemble des fichiers et l'arborescence du projet qui les contient est disponible dans le repository GitHub [bear-corp/the-cave](https://github.com/bear-corp/the-cave).

Une des premières choses que j'ai constatées dans mon utilisation des dev containers : c'est la tendance à faire un copier-coller des fichiers de configuration entre les projets pour retrouver mes outils préférés.
Avec les multiples maintenances que cela implique et le côté rébarbatif de la chose.
J'ai donc, décidé de démarrer par une image *générique* qui sera la base de toutes mes configurations des dev containers.
Avec le versioning qui sera apporté à chaque modification, je ne devrai pas trop peiner à m'en sortir 😅.

Pour cette étape j'ai donc créé une image à partir d'un [Dockerfile](https://github.com/bear-corp/the-cave/blob/main/common/Dockerfile) dans le répertoire [common](https://github.com/bear-corp/the-cave/tree/main/common) de mon projet.

```dockerfile
FROM mcr.microsoft.com/devcontainers/base:ubuntu

USER vscode

#### 🎨 ZSH & OhMyZsh configuration 🖥️  ####
ENV ZSH_CUSTOM=/home/vscode/.oh-my-zsh/custom
COPY .zshrc /home/vscode/.zshrc
RUN git clone https://github.com/zsh-users/zsh-autosuggestions $ZSH_CUSTOM/plugins/zsh-autosuggestions
RUN git clone https://github.com/spaceship-prompt/spaceship-prompt.git "$ZSH_CUSTOM/themes/spaceship-prompt" --depth=1
RUN ln -s "$ZSH_CUSTOM/themes/spaceship-prompt/spaceship.zsh-theme" "$ZSH_CUSTOM/themes/spaceship.zsh-theme"
```

Quelques explications s'imposent je pense 😉 : 
 - la ligne 1 correspond à l'image de base que je vais utiliser. Il y a un repository qui permet d'avoir la liste des images utilisées dans les templates : [devcontainers/images](https://github.com/devcontainers/images). Et dans ce repository vous avez la liste de toutes les base images utilisées dans les templates (elles commencent par `base-`). Dans mon cas j'ai choisi Ubuntu car c'est la distribution Linux que je "connais" le mieux 😅.
  - la ligne 3 me permet de positionner l'utilisateur qui sera l'utilisateur du container une fois lancé.
  - ensuite le reste des lignes me permet d'installer [Zsh](https://www.zsh.org/) + [Oh My Zsh](). Je n'ai pas utilisé la [feature existante](https://github.com/cirolosapio/devcontainers-features/tree/main/src/alpine-ohmyzsh) car je voulais pouvoir pré-configurer des éléments dans un `.zshrc` et avoir le choix des plugins et thèmes à utiliser.

> 💡 Je ne suis pas un expert Docker et Linux, il y a moyen d'optimiser ces Dockerfiles et d'utiliser Docker Compose pour celles et ceux qui excellent dans ce domaine 😉.

# 🧩 La structure du dev container common

Une fois que j'ai ce Dockerfile il ne me reste plus qu'à créer mon fichier [devcontainer.json](https://github.com/bear-corp/the-cave/blob/main/common/.devcontainer/devcontainer.json) pour créer mon template common de base.

```json
{
	"name": "Dev Container Commons",
	"build": {
		"dockerfile": "../Dockerfile"
	},
	"features": {
		"ghcr.io/jsburckhardt/devcontainer-features/bat:1": {},
		"ghcr.io/devcontainers/features/docker-in-docker:2": {}
	},
	"mounts": [
		{ "source": "zsh-history", "target": "/home/vscode/commandhistory", "type": "volume" }
	],
	"remoteEnv": {
		"HISTFILE": "/home/vscode/commandhistory/.zsh_history"
	},
	"postCreateCommand": {
		"chown": "sudo chown -R vscode:vscode /home/vscode/commandhistory"	
	},

}
```

Bon, j'ai quelques explications à donner 😅.

  - les lignes 2 à 5 sont la partie construction du l'image à partir du Dockerfile précédemment créé.
	- la ligne 7 est l'installation d'un outil que j'adore : [bat](https://github.com/sharkdp/bat), c'est un remplaçant de *cat* mais plus optimisé (merci Philippe Charrière pour la découverte 😘)
	- la ligne 8 est l'installation de la feature me permettant de faire du Docker dans un container. J'en ai besoin pour créer les images de me dev containers (si vous regardez la configuration du projet elle utilise ce template) mais aussi pour mes devs Java avec Quarkus ou Kubernetes.
	- à partir de la ligne 10 ce sont des astuces pour gérer mon historique de commandes zsh qui va être utilisable avec le plugin Oh My Zsh [zsh-autosuggestions](https://github.com/zsh-users/zsh-autosuggestions) dont je ne peux plus me passer 😉. Par défaut, étant dans un container, à chaque fois que j'arrête le container, je perds mon historique de commandes. Là je vais le persister sur la machine hôte et le partager avec l'ensemble de mes containers pour en bénéficier partout: 
	   - la ligne 10 est le mount du volume Docker `zsh-history` avec un répertoire dans mon container qui va stocker mon historique (il n'est pas possible de créer un volume Docker vers autre chose qu'un répertoire). 
		 > 💡 cela implique donc qu'au préalable il faut créer un volume Docker: `docker volume create --name zsh-history`
		 - la ligne 14 permet d'indiquer à Zsh le nouveau répertoire qui va stocker les historiques de commandes
		 - la ligne 17 me permet de rendre le répertoire accessible en écriture au user `vscode` utilisé dans le container

Vous allez me dire beaucoup de choses pour juste un historique de commandes ? C'est pas faux mais on ne se refait pas 😉.

# 🐳 Construction de l'image du template common

Maintenant que l'on a défini notre dev container de base, il faut le rendre disponible pour mes autres configurations grâce à la notion de *prebuilds*.
Pour cela, je vais installer la CLI des dev containers, mais comme je pousse l'exercice jusqu'au bout, je n'installe rien sur ma machine en dehors de Docker, donc je passe aussi par une configuration dev containers pour fabriquer mes dev containers 😉.

```json
{
	"name": "Dev Container CLI",
	"image": "wilda/common-devcontainer:0.0.2",
	"features": {
		"ghcr.io/eliises/devcontainer-features/devcontainers-cli:1": {
			"version": "latest",
			"nodeVersion": "latest"
		}
	}
}
```

Alors oui, je triche un peu, et vous voyez qu'en ligne 3 j'utilise une image que j'ai déjà faite ... l'histoire de l'oeuf et de la poule 🐓.
Ce qu'il s'est passé est qu'à l'origine j'ai simplement utilisé le [template de base Ubuntu](https://github.com/devcontainers/templates/tree/main/src/ubuntu) pour créer ma première configuration et une fois que j'ai eu une image satisfaisante, j'ai créé mon template common pour à la fin remplacer le template Ubuntu par mon template common.

Une fois que l'on a la CLI c'est assez simple pour construire son template en mode *prebuilds* : `devcontainer build --workspace-folder . --push true --image-name wilda/common-devcontainer:0.0.2`.

> 💡 pour tester l'image sans la push et pull vous pouvez utiliser la commande suivante dans le répertoire contenant le répertoire `.devcontainer` : `devcontainer up --remove-existing-container --workspace-folder .` puis lancer une commande dans le container créé : `devcontainer exec --workspace-folder . ls -lart`


# 🧰 Utiliser l'image prebuild

Je vous ai déjà un peu (beaucoup) spoilé mais ensuite vous pouvez simplement utiliser l'image telle quelle :

```json
{
	"name": "Dev Containers configuration",
	"image": "wilda/common-devcontainer:0.0.2"
}
```

Mais aussi comme n'importe quel template en ajoutant des configurations (feature, extensions, ...).
Voici par exemple mon [devcontainer.json](https://github.com/bear-corp/the-cave/blob/main/java/.devcontainer/devcontainer.json) pour un projet de développement Java : 

```json
{
	"name": "Dev Container Java tools",
	"image": "wilda/common-devcontainer:0.0.2",
	"postCreateCommand": {
		"chown": "sudo chown -R vscode:vscode /home/vscode/.m2"	
	},
	"features": {
		"ghcr.io/devcontainers/features/java:1": {
      "installMaven": true,
			"mavenVersion": "3.9.11",
      "version": "24.0.2",
      "jdkDistro": "tem"
    },
		"ghcr.io/devcontainers-extra/features/jbang-sdkman:2": {
			"jdkDistro": "tem",
			"jdkVersion": "24.0.2-tem"
		},
		"ghcr.io/devcontainers-extra/features/quarkus-sdkman:2": {
			"jdkDistro": "tem",
			"jdkVersion": "24.0.2-tem"
		}
	},
	"mounts": [
		{ "source": "maven-repo", "target": "/home/vscode/.m2", "type": "volume" }
	],
	"customizations": {
		"vscode": {
			"settings": {
				"java.configuration.runtimes": [
					{
						"name": "Java24-tem",
						"path": "/usr/local/sdkman/candidates/java/current/"
					}
				]
			},
			"extensions": ["jbangdev.jbang-vscode"]
		}
	}
}
```

Ce fichier est un peu plus long mais je reprends des notions que l'on vient de voir : 
  - la ligne 3 est l'utilisation du template common 
  - les lignes 5 et 24 me permettent via un volume Docker de centraliser mes dépendances Maven sur la machine hôte
  - de la ligne 7 à 22 j'installe les outils via des features : JDK, Maven, JBang, Quarkus 
  - de la ligne 26 à 38 j'installe des extensions VSCode pour me faciliter la vie dans mes développements Java

Bien entendu, encore une fois l'idée est de construire un template *prebuild* avec cette configuration pour ne pas avoir le temps de construction lorsque je crée un nouveau projet Java : `devcontainer build --workspace-folder . --push true --image-name wilda/java-devcontainer:1.2.0`.

Ensuite, je l'utilise de manière très épurée dans mes projets Java avec un temps de démarrage rapide : 

```json
{
	"name": "Dev Containers configuration",
	"image": "wilda/java-devcontainer:1.2.0"
}
```

# 🤗 En conclusion

Voilà pour cet article vous permettant d'optimiser vos dev containers avec la notion de *prebuilds*.
Au final l'image Java construite fait 1.8 Go non compressée.
Cela reste une certaine taille et je suis persuadé que l'on peut optimiser tout cela mais j'ai la chance que cela ne soit pas un problème pour l'instant 😉.

Par contre une fois pull, le temps de démarrage est très rapide et la partie changement de machine est encore facilitée (si je dispose d'une bonne connexion Internet 😉).

Ma prochaine étape sera de tester mes images avec Intellij IDEA Ultimate mais j'avoue être un peu déçu du support des dev containers par celui-ci.
Je pense aussi voir le comportement de mes images avec les CDE qui utilisent ou sont compatibles avec les dev containers.

Si vous êtes arrivé•es jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.