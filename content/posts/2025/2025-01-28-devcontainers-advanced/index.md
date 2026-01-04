---
title: "🧑‍💻 Aller plus loin avec les Dev Containers 🐳"
description: "Plongez dans le terrier avec les Dev Containers 🐰"
link: /2025-01-28-devcontainers-advanced
tags: 
  - DevContainers
image: image-illustration.jpg
figCaption: "@wildagsx"
author: wilda
---

> Cet article est la deuxième partie de l'article précédent sur les Dev Containers, je vous conseille de lire le premier article [🧑‍💻 A la découverte des Dev Containers 🐳]({site.url}/2025-01-12-discover-devcontainers) avant de lire celui-ci.

Suite à la mise en bouche avec l'article précédent concernant les _Dev Containers_, je vous propose de pousser un plus loin l'utilisation de ceux-ci.
En effet, le premier exemple était assez simple puisqu'il s'agissait d'utiliser les _Dev Containers_ pour l'écriture d'articles de mon blog (celui même que vous lisez 😉).
Il y a bien des cas où l'environnement de travail ne se résume pas à _juste_ installer Ruby.

Pour illustrer les différentes façons de customiser un _Dev Container_ je vais prendre comme objectif d'avoir [SliDesk](https://slidesk.github.io/slidesk-doc/) installé dans mon environnement de travail.

> Si vous ne connaissez pas SliDesk je vous laisse allez le découvrir dans l'article [🖼️ Slides as code avec SliDesk 👨‍💻]({site.url}/2024-03-09-slidesk-discovery)


## 😌 La solution simple : postcommand

On l'a vu dans l'article de découverte, le fichier de paramétrage a un champ `postCreateCommand` qui permet d'exécuter des commandes après le lancement du container.
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
 - si j'ai besoin de SliDesk dans un autre projet il faut que je duplique les lignes d'installation et donc que je le maintienne dans l'ensemble des projets,
 - enfin, à chaque démarrage de mon container, l'installation est de nouveau effectuée.

## 🧩 La feature : la solution naturelle ?

On l'a vu dans l'article précédent : les [features](https://containers.dev/features) ont le côté pratique d'être des extensions à nos images déjà pré-packagées.
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

wget -O /usr/bin/slidesk https://github.com/slidesk/slidesk/releases/download/2.11.1/slidesk_linux-${ DISTRIBUTION}

chmod +x /usr/bin/slidesk
```

> Vous constatez que je suis loin d'être un expert en bash 😅

Une fois ce fichier créé, il suffit de créer le fichier `devcontainer-features.json` :

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

Bien entendu, ici, nous n'avons fait que déporter le code d'installation du fichier `.devcontainer.json` vers la feature.
Cela apporte tout de même plus de liberté sur le code d'installation et le versioning.
Cependant, pour ne plus avoir à dupliquer le code en lui-même dans l'ensemble des projets, il faut packager la feature et la distribuer via une registry.

### 📦 Release et distribution de la feature

Il faut avouer que c'est plutôt bien fait pour packager et releaser une feature.
La première chose à faire va être de faire un fork du repository de template : <https://github.com/devcontainers/feature-starter>.

Ensuite, il suffit de copier le répertoire `slidesk` précédemment créé localement dans un répertoire `src`.
Pour voir ce que cela donne vous pouvez allez voir ici : <https://github.com/philippart-s/feature-slidesk>.

> Notez que pour développer une feature, le template vous met à disposition un environnement de développement complet grâce aux Dev Containers bien sûr !

Et là où c'est cool c'est que tout est fournit pour faire une release grâce à la GitHub action dans le répertoire `.github` de votre repository fraîchement créé.
De plus, c'est un workflow qui peut se déclencher manuellement.

Une fois la release effectuée votre feature est disponible dans votre registry de votre organisation personnelle, dans mon cas : <https://ghcr.io/philippart-s>.

La feature est donc disponible avec l'url <https://ghcr.io/philippart-s/feature-slidesk/slidesk:latest>.

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
Imaginez que SliDesk soit un de mes logiciels essentiels et que je doive l'activer à chaque fois dans mes configurations.
Cela a beau n'être que quelques lignes, je devrais les répéter tout de même.

> L'ensemble du code de la feature est disponible dans le repository [feature-slidesk](https://github.com/philippart-s/feature-slidesk)

## 🐳 L'image custom : la solution ultime ?

Nous l'avons vu précédemment, il est possible de rajouter des éléments à votre environnement via les _features_ ou les _commands_.
Les deux fonctionnent et ont leurs avantages et leurs inconvénients.
Le problème qu'elles partagent est que, si vous voulez toujours avoir SliDesk (dans mon exemple), il faut répéter l'ajout de la configuration.  
Et si, SliDesk était déjà présent au moment de lancer le _Dev Container_ ?
Vous me voyez venir et avez certainement deviné : cela va être possible avec l'attribut _image_ du fichier de configuration.
Oui, il est possible de créer sa propre image Docker pour lancer le _Dev Container_ avec.

En dehors de fabriquer une image qui va correspondre à 100% à vos besoins, l'autre avantage va être que vous allez aussi pouvoir optimiser cette image.
On l'a vu dans l'article précédent les images peuvent assez vite grossir et peut-être que vous n'avez pas besoin de tout ce que propose une image basée sur Ubuntu par exemple.

Tout comme pour les _features_, il est possible de déclarer son image localement, pour cela il va vous suffire de créer un fichier _Dockerfile_ dans le répertoire `.devcontainer` de votre projet.

Prenons l'exemple, très simple suivant : 
```dockerfile
FROM mcr.microsoft.com/devcontainers/jekyll:2-bullseye

ADD install-slidesk.sh .
RUN ./install-slidesk.sh
```

> Nous de discuterons pas de l'optimisation de l'image ici, l'idée est de bénéficier de ce qui existe déjà dans l'image _jekyll_ en lui ajoutant l'installation de SliDesk.

Ensuite dans le fichier _devcontainer.json_ il faut déclarer l'image à utiliser en replaçant l'option image par un attribut `build`.

```json
{
	"name": "Jekyll",
	"build": {
		"dockerfile": "Dockerfile"
	},
	"postCreateCommand": {
		"bundle": "bundle install",
	}
}
```

Avec cette approche vous avez la possibilité de construire votre image spécifique qui va répondre 100% à vos besoins.
Dans une approche Open Source ou de partage de configuration à d'autres personnes c'est très pratique.
Dans le cas, où comme moi, on souhaite réutiliser une configuration entre plusieurs projets je vous conseille de construire l'image, la mettre sur une registry puis la référencer dans votre _devcontainer.json_.

### 🐳 🐋 Docker in Docker

Ce que je viens de vous proposer ne semble pas une tâche insurmontable n'est-ce pas ?
Souvenez-vous : nous sommes dans un container et je vous propose de fabriquer une image.

Inception ? Non Docker in Docker ! 

Docker in Docker (DinD de son petit nom) est quelque chose de recherché par grand nombre de développeuses et de développeurs depuis longtemps.
Il y a beaucoup d'articles de blogs qui expliquent comment faire et les risques inhérents à cette pratique.
Je vous laisse donc aller les consulter.
Il y a même une [image officielle](https://hub.docker.com/_/docker) de Docker permettant de le faire, avec pas mal de doc qui explique le concept.

Retenons juste la bone nouvelle : c'est possible 😉.

L'autre bonne nouvelle est qu'une _feature_ existe, allons-y et rajoutons cette _feature_ pour fabriquer notre image :
```json
"features": {
    "ghcr.io/devcontainers/features/docker-in-docker:2": {}
}
```

Passons à la construction de notre image et au push de celle-ci : `docker build -t wilda/jekyll:1.0.0 . && docker push wilda/jekyll:1.0.0`

Ensuite vous pouvez l'utiliser comme image dans votre `devcontainer.json` :
```json
{
	"name": "Jekyll",
	"image": "wilda/jekyll:1.0.0",
	"postCreateCommand": {
		"bundle": "bundle install",
	},
}
```

A ce stade j'arrive à quelque chose de très agréable : j'ai une image type, dans mon cas Jekyll, que je vais pouvoir utiliser dans tous mes devs Jekyll sans avoir à refaire la configuration de mon IDE.

⚠ N'oubliez pas que le Dockerfile en clair reste une bonne approche si l'objectif est d'avoir quelque chose de très spécifique pour un projet et qui n'est pas couvert simplement par des _features_ ou des _commandes_. ⚠️

Si vous utilisez Docker régulièrement vous vous demandez peut-être : et Docker Compose ?

## 🐳 🏗️ Docker Compose : l'ami complexe ?

Alors oui Docker Compose peut être utilisé.
Pour moi il répond à un besoin bien spécifique : construire un environment de développement complexe qui ne se limite pas à l'environnement de développement mais à différentes briques d'architectures qu'aurait besoin l'application.

En effet, jusqu’à présent mon but était d'offrir le meilleur environnement de développement possible, reproductible et sans installation sur ma machine.
Et si j'englobais aussi les ressources plus complexes comme par exemple une base de données pour une application.
L'idée est simple : pouvoir tester l'application sans devoir créer des mocks ou ne pas être iso production.
Exactement comme le font les [test containers](https://testcontainers.com/) au final.

> Les test containers sont eux aussi dans ma TODO list, et feront certainement l'objet d'un futur article 😉.

Nous verrons certainement dans un autre article une configuration plus poussée mais pour l'exemple je vais prendre le cas où je veux installer une base de données PostgreSQL qui ne serait pas dans mon image principale.
Les besoins peuvent être multiples mais le premier évident va être le partage et la réutilisation d'images entre différents types de développements.
Par exemple, pour ma base de données je peux très bien l'utiliser dans un développement de type Java et JavaScript.
Ce serait dommage de dupliquer le code d'installation là où on pourrait le mutualiser.

Pour avoir une base de données dans un container différent de mon environnement de développement il faut donc que je crée un fichier `docker-compose.yml` au même niveau que mon `devcontainer.json`.

```yml
version: '3.8'
services:
  devcontainer:
    image: wilda/jekyll:1.0.0
    volumes:
      - ../..:/workspaces:cached
    command: sleep infinity
    network_mode: service:db
  db:
    image: postgres:latest
    restart: unless-stopped
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_USER: postgres
```

> ℹ️ La base de données est visible depuis le service _devcontainer_ grâce à : `network_mode: service:db`

Puis, le référencer dans le fichier `devcontainer.json`:

```json
{
	"name": "Jekyll",

	"dockerComposeFile": "docker-compose.yml",
	"service": "devcontainer",
	"workspaceFolder": "/workspaces/${ localWorkspaceFolderBasename}",
	
	"features": {
		"ghcr.io/robbert229/devcontainer-features/postgresql-client:1": {}
	},
	
	"postCreateCommand": {
		"bundle": "bundle install",
	},
}

```

Notez ici la nécessité d'indiquer deux choses :

 - quel service du _Docker Compose_ fait office de container de _travail_, représenté ici par le service `devcontainer`
 - le répertoire de travail (aka _workspace_), c'est transparent lorsque vous utilisez des images toutes faites mais là il faut l'indiquer 😉

En plus de notre environnement de travail géré par le service _devcontainer_ il est aussi possible d'utiliser simplement la base de données PostgreSQL : 

```bash
$ psql -h localhost -U postgres -d postgres
psql (13.18 (Debian 13.18-1.pgdg110+1), server 17.2 (Debian 17.2-1.pgdg120+1))
WARNING: psql major version 13, server major version 17.
         Some psql features might not work.
Type "help" for help.

postgres=# 

```

Même si l'utilisation _Docker Compose_ sort un peu de mon use case avec _SliDesk_ cela me semblait pertinent de le présenter dans cet article comme alternative pour la construction d'un environnement de développement complet, autonome et reproductible.

## 💫 Le template : factoriser de la configuration

On a vu plusieurs moyens de configurer l'installation de SliDesk dans notre environnement.
Que ce soit avec le `postCreateCommand` ou la _feature_ il faut positionner du paramétrage dans le fichier `.devcontainer.json` et donc dupliquer cette configuration  pour les projets de même type.
Il existe une notion vous permettant de démarrer un projet avec le fichier _devcontainer.json_ déjà pré-renseigné des valeurs nécessaires.
Cette fonctionnalité s'appelle un [template](https://containers.dev/templates).
Ce sont des templates que vous manipulez avec VSCode lors de l'initialisation d'un _Dev Container_.

Imaginons que je veuille créer un template pour avoir un environnement permettant de faire du Jekyll tout en ayant le client SliDesk.

Pour cela, je vous conseille de partir du [template d'exemple](https://github.com/devcontainers/template-starter) qui non seulement a le code et paramétrage d'exemple mais aussi des GitHub action vous permettant de packager et mettre à disposition votre template.

Une fois forké vous allez voir que vous avez un répertoire `.devcontainer` qui va vous permettre de réouvrir le projet avec un _Dev Container_ vous permettant d'avoir tous les outils nécessaires pour développer votre template 😉.

Ensuite commence le développement de votre template.
Tout se passe dans le répertoire `src`.
A la racine il y a un répertoire `.devcontainer` et un fichier `devcontainer-template.json`.

Voyons ce que contient le répertoire `.devcontainer` : un fichier `devcontainer.json` 😅.
Eh oui c'est ce fichier qui sera utilisé pour créer le fichier `devcontainer.json` lorsque le template sera utilisé.

```json
{
	"name": "Jekyll with SliDesk",
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-${ templateOption:imageVariant}",

	// 👇 Features to add to the Dev Container. More info: https://containers.dev/implementors/features.
	 "features": {
		"ghcr.io/philippart-s/feature-slidesk/slidesk:1.0.0": {}
	 },

	// 👇 Use 'forwardPorts' to make a list of ports inside the container available locally.
	// "forwardPorts": [],

	"postCreateCommand": {
		"bundle": "bundle install",
		"sliDesk": "slidesk --version"
	}
	// 👇 Configure tool-specific properties.
	// "customizations": {},

	// 👇 Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
	// "remoteUser": "root"
}
```

On le voit ici le fichier ressemble beaucoup aux autres fichiers `devcontainer.json` que l'on a déjà créé.
Les seules différences sont que l'on va pouvoir apporter un peu de généricité, par exemple ici on veut pouvoir choisir le type de distribution Debian avec le paramètre `${ templateOption:imageVariant}`.
Sinon vous constatez que cela ressemble beaucoup à ce que l'on avait créé dans le précédent article.

Un fois ce fichier créé il ne reste plus qu'à déclarer le template avec le fichier `.devcontainer-templates.json` qui se trouve à la racine de `src`.

```json
{
    "id": "slidesk",
    "version": "1.0.0",
    "name": "SliDesk with Dev Containers",
    "description": "A Template to have SliDesk in your Dev Containers",
    "documentationURL": "https://github.com/philippart-s/template-slidesk/tree/main/src/slidesk",
    "licenseURL": "https://github.com/philippart-s/template-slidesk/blob/main/LICENSE",
    "options": {
        "imageVariant": {
            "type": "string",
            "description": "Debian version (use bullseye on local arm64/Apple Silicon):",
            "proposals": [
                "bullseye",
                "buster"
            ],
            "default": "bullseye"
        }
    },
    "platforms": [
        "Any"
    ]
}
```

On donne quelques informations utiles pour la suite comme le nom et l'identifiant du template.
Surtout on indique les valeurs possibles pour la variable `${ templateOption:imageVariant}` et sa valeur par défaut.

Une fois que tout ça est push et sur GitHub il ne reste plus qu'à packager le template avec la GitHub Action manuelle `Release Dev Container Templates & Generate Documentation` présente dans le fichier `.github/workflows/release.yaml`.

Cela met à disposition le template dans votre section package de votre organisation GitHub personnelle, par exemple : <https://github.com/philippart-s?tab=packages&repo_name=template-slidesk>

Ensuite votre template est utilisable, de la même manière que pour l'article précédent avec VSCcode : `CMD+SHIFT+P` puis `Dev Containers: Add Dev Containers configuration Files...`.
Sauf qu'ici au lieu de choisir dans la liste il faut rentrer votre image : `ghcr.io/philippart-s/template-slidesk/slidesk:latest`.

> ℹ️ Il est tout à fait possible de rajouter le template dans la liste officielle qui permet qu'il apparaisse directement dans la liste des templates. 
> Je ne l'ai pas fait dans cet exemple étant donné que le template n'est que pour moi.

Une fois que vous utilisez le template, un projet est créé avec un `devcontainer.json` pré-configuré.

```json
{
	"name": "Jekyll with SliDesk",
	"image": "mcr.microsoft.com/devcontainers/jekyll:2-bullseye",

	// 👇 Features to add to the Dev Container. More info: https://containers.dev/implementors/features.
	 "features": {
		"ghcr.io/philippart-s/feature-slidesk/slidesk:1.0.0": {}
	 },

	// 👇 Use 'forwardPorts' to make a list of ports inside the container available locally.
	// "forwardPorts": [],

	"postCreateCommand": {
		"bundle": "bundle install",
		"sliDesk": "slidesk --version"
	}
	// 👇 Configure tool-specific properties.
	// "customizations": {},

	// 👇 Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
	// "remoteUser": "root"
}
```

> L'ensemble du code du template se trouve dans le repository [template-slidesk](https://github.com/philippart-s/template-slidesk)

# En conclusion

On vient de le voir : on peut aller beaucoup plus loin avec les _Dev Containers_.
A vous de voir si vous souhaitez simplement les utiliser en mode _one shot_ dans un projet, vous faire toute une bibliothèque d'environnements de développements.
Vous pouvez aussi mettre à disposition pour les autres via des images, des features ou encore des templates.
Bref laisser libre cour à votre imagination tout en ayant quelque chose de portable et reproductible.

Pour finir, vous l'avez certainement deviné mais dans notre cas de SliDesk une feature serait le plus adapté 😉.

Dans les semaines à venir, je vais devoir créer de nombreuses démos avec des technologies très hétérogènes.
Nul doute que j'aurai des choses à partager autour des _Dev Containers_ 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.