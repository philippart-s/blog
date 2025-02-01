---
title: "🧑‍💻 Aller plus loin avec les Dev Containers 🐳"
classes: wide
excerpt: "Plonger dans le terrier avec les Dev Containers 🐰"
categories:
  - Code
  - Docker
  - IDE

tags:
  - DevContainers
---

![un développeur assis à son bureau]({{ site.url }}{{ site.baseurl }}/assets/images/discover-devcontainers/illustration.png){: .align-center}
[@wildagsx](https://twitter.com/wildagsx){:style="font-size: smaller"}{:target="_blank"}{: .align-right}<br/>

> Suite de l'article précédent sur les Dev Containers, je vous conseille de lire le premier article [🧑‍💻 A la découverte des Dev Containers 🐳]({{ site.baseurl }}{% post_url 2025-01-12-discover-devcontainers %}){:target="_blank"} avant de lire celui-ci.


Suite à la mise en bouche avec l'article précédent concernant les _Dev COntainers_ je vous propose de pousser un plus loin l'utilisation de ceux-ci.
En effet, le premier exemple était assez simple puisqu'il s'agissait d'utiliser les _Dev Containers_ pour l'écriture d'articles de mon blog (celui même que vous lisez 😉).
Il y a bien des cas où l'environnement de travail ne se résume pas à _juste_ installer Ruby.

Pour illustrer les différentes façons de customiser un _Dev Container_ je vais prendre comme objectif d'avoir [SliDesk](https://slidesk.github.io/slidesk-doc/){:target="_blank"} d'installé dans mon environnement de travail.

> Si vous ne connaissez pas SliDesk je vous laisse allez le découvrir dans l'article [🖼️ Slides as code avec SliDesk 👨‍💻]({{ site.baseurl }}{% post_url 2024-03-09-slidesk-discovery %}){:target="_blank"}


## 😌 La solution simple : postcommand

On l'a vu dans l'article de découverte, le fichier de paramétrage a un champs `postCreateCommand` qui permet d'exécuter des commandes après le lancement du container.
Du coup on peut lancer l'installation de SliDesk en utilisant cette option.

```json
{
	"name": "Jekyll",
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",
	"postCreateCommand": {
		"bundle": "bundle install",
		"db": [
			"sudo",
			"wget",
			"-O",
			"/usr/bin/slidesk",
			"https://github.com/slidesk/slidesk/releases/download/2.11.1/slidesk_linux-arm"
		]
	},
}
```

Le gros avantage de cette solution est qu'elle est simple et rapide à mettre en place.

> Notez au passage les deux façons de déclarer une commande. 
> La première ne crée pas de shell alors que la deuxième oui.

Si c'est très simple à mettre en place on peut y voir quelques inconvénients :
 - il faut lister l'ensemble des commandes à exécuter pour installer les applications (même si il est possible de créer un fichier sh pour ne pas avoir à le faire dans le `devcontainer.json`),
 - si j'ai besoin de SliDesk dans un autre projet il faut que je duplique les lignes d'installation et donc que je maintienne dans l'ensemble des projets,
 - enfin à chaque démarrage de mon container, l'installation est de nouveau effectuée.

## 🧩 La feature : la solution naturelle ?

On l'a vu dans l'article précédent, les [features](https://containers.dev/features){:target="_blank"} ont le côté pratique d'être des extensions à nos images déjà pré-packagées.
Bien entendu, SliDesk n'a pas de feature au moment où j'écris cet article, et c'est tant mieux on va créer ensemble notre premiere feature.

Pour faire une feature rien de plus simple (du moins en local 😅) :
  - créer un répertoire du nom de la feature dans le répertoire `.devcontainer/`
  - créer un fichier `devcontainer-features.json`
  - créer un fichier `install.sh` avec le code d'installation souhaité

Voyons le fichier `install.sh` :

```bash
#!/bin/sh
set -e

echo "Activating feature 'slidesk'"

wget -O/usr/bin/slidesk https://github.com/slidesk/slidesk/releases/download/2.11.1/slidesk_linux-${DISTRIBUTION}

chmod +x /usr/bin/slidesk
```

> Vous constatez que je suis loin d'être un expert en bash 😅

Une fois ce fichier créé il suffit de créer le fichier `devcontainer-features.json` :

```json
{
  "name": "SliDesk",
  "id": "slidesk",
  "version": "1.0.0",
  "description": "A feature to install SliDesk",
  "options": {
    "distribution": {
      "type": "string",
      "enum": [
        "arm",
        "amd"
      ],
      "default": "arm",
      "description": "Choose your distribution type: arm or amd."
    }
  }
}
```

Enfin, il faut ajouter la feature à la liste des features dans le fichier `.devcontainer/devcontainer.json` :

```json
{
	"name": "Jekyll",
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",
	"features": {
		"./slidesk": {}
	},
	"postCreateCommand": {
		"bundle": "bundle install"
	},
}
```

> Notez la syntaxe spéciale de l'activation de feature car elle se trouve dans le même répertoire que le fichier `devcontainer.json`, à savoir `.devcontainer`.

Bien entendu ici, nous n'avons que déporté le code d'installation du fichier `.devcontainer.json` vers la feature.
Cela apporte tout de même plus de liberté sur le code d'installation et le versionning.
Cependant pour ne plus avoir à dupliquer le code en lui-même dans l'ensemble des projets il faut packager la feature et la distribuer via une registry.

### 📦 Release et distribution de la feature

Il faut avouer que c'est plutôt bien fait pour packager et releaser une feature.
La première chose à faire va être de faire un fork du repository de template : https://github.com/devcontainers/feature-starter.

Ensuite il suffit de copier le répertoire `slidesk` précédemment créé localement dans un répertoire src.
Pour voir ce que cela donne vous pouvez allez voir ici : https://github.com/philippart-s/feature-slidesk.

> Notez que pour développer une feature le template vous met à disposition un environnement de développement complet grâce aux Dev Containers bien sûr !

Et là où c'est cool c'est que tout est fournit pour faire une release grâce à la GitHub action dans le répertoire `.github` de votre repository fraîchement créé.
De plus, c'est un workflow qui peut se déclencher manuellement.

Une fois la release effectuée votre feature est disponible dans votre registry de votre organisation personnelle, dans mon cas : https://ghcr.io/philippart-s.

La feature est donc disponible avec l'url https://ghcr.io/philippart-s/feature-slidesk/slidesk:latest.

Voyons maintenant notre fichier `devcontainer.json` :

```json
{
	"name": "Jekyll",
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",
	"features": {
		"ghcr.io/philippart-s/feature-slidesk/slidesk:1.0.0": {}
	},
	"postCreateCommand": {
		"bundle": "bundle install"
	}
}
```

Avec ce mode d'installation et de distribution on a gommé nos problème de répétition de code et de lourdeur déclarative dans le fichier `devconainer.json`.

Et si on allait plus loin ? Si on faisait son propre template ?
Imaginer que SliDesk soit un de mes softs essentiels et que je doive l'activer à chaque fois dans mes configurations.
Cela a beau n'être que quelques lignes, je devrais les répéter tout de même.

## Le template : la solution ultime ?

# En conclusion

J'espère que cette découverte en ma compagnie des _Dev Containers_ vous a plus.
Dans les autres parties on plongera un plus en profondeur dans la customisation d'images pour répondre au mieux à vos besoins 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog){:target="_blank"} 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.