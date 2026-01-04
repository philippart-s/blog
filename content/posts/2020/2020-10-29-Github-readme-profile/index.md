---
title: "Créer un readme pour son profile GitHub"
link: /2020-10-29-Github-readme-profile
tags: 
  - Markdown
  - GitHub
author: wilda
---

Il y a quelque temps [GitHub](https://github.com) a mis à disposition une nouvelle fonctionnalité : la [possibilité de gérer son profile utilisateur avec un readme](https://docs.github.com/en/free-pro-team@latest/github/setting-up-and-managing-your-github-profile/managing-your-profile-readme).

Ni une, ni deux, en bon geek des familles je me suis dit et pourquoi pas essayer ! :wink:

## Initialiser le readme et la fonctionnalité ##
Pour accéder à cette fonctionnalité, rien de plus simple : il suffit de créer un repository ayant comme nom votre nom d'utilisateur, dans mon cas *philippart-s*.

Et ... c'est tout ! Plutôt simple. ⚡️

Ensuite, en se connectant à la racine de votre compte, dans mon cas https://github.com/philippart-s, le contenu du readme du repository précédemment créé est affiché. :art:

## Ne plus afficher le readme comme descriptif de profile ##
Bien entendu il est possible de ne plus l'afficher, la méthode la plus simple : supprimer le repository ! :boom:

Bon si vous ne souhaitez pas tout perdre et plus mesuré :
 - rendre le repository *private*
 - renommer le fichier *readme.md* en autre chose
 - le déplacer dans un sous répertoire 


## Pour aller plus loin ##
Comme d'habitude de nombreuses personnes ont déjà fait de jolies choses plus ou moins utiles :wink:.
Il existe un repository qui liste pas mal de ressources pour s'aider à la conception mais aussi voir tout ce qu'il est possible : [https://github.com/abhisheknaiidu/awesome-github-profile-readme](https://github.com/abhisheknaiidu/awesome-github-profile-readme).

Pour ma part, mon objectif était de faire un CV rapide et concis afin d'avoir ça sous la main, à vous de voir ce que vous souhaitez en faire !

Mais on ne se refait pas et à force de regarder ici ou là j'ai voulu intégrer pleins de trucs et petit à petit commencer à aller plus loin voir devoir développer certains trucs ! :rofl:

### Se présenter
On commence par une petite présentation, pour des soucis de lisibilité j'ai choisi de mettre un petit texte qui explique qui je suis et ce que je fais à l'heure actuelle, pour plus de détails cela se passera dans une section affichée à la demande.

### Les badges des réseaux sociaux ###
Commençons par un truc simple mais qui peut toujours servir : les différents comptes des réseaux sociaux, et plutôt que de simples liens autant avoir de jolis badges / icones.
J'ai hésité entre le format badges (comme ceux dans les repositories de librairies qui indiquent les informations de CI) et des icones, un peut plus joli je trouve, mais moins geek :stuck_out_tongue:.

Du coup, je suis parti sur le badges avec [sheilds.io](https://shields.io/). 
Il existe des badges pré-définis mais il est assez simple de fabriquer ses propres badges.

```md
<img src="https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Ftwitter.com%2Fwildagsx">
<img src="https://img.shields.io/badge/linkedin--lightgrey?style=social&logo=linkedin">
```

Cela donne simplement: ![badges réseaux sociaux]({{ site.url }}{{ site.baseurl }}/assets/images/badges-social-media.png)

### Une section détails
Rien de plus simple pour cela HTML est là pour ça avec le tag `<details>`.
```html
<details>
    <summary>
        CV détaillé
    </summary>
</details>
```

### Ajouter des stats propres à GitHub 
J'aime beaucoup ce que propose le repository [github-readme-stats](https://github.com/anuraghazra/github-readme-stats) qui permet de générer des tuiles avec certaines statistiques de votre activité GitHub.

Par exemple
```md
![](https://github-readme-stats.vercel.app/api/top-langs/?username=philippart-s&theme=radical&hide_langs_below=8)
![](https://github-readme-stats.vercel.app/api?username=philippart-s&show_icons=true&theme=radical&count_private=true)
```
donnera:

![badges réseaux sociaux](/github-stats.png)

En conclusion on peut faire ce que l'on souhaite, ajouter plus ou moins de dynamisme, plus ou moins d'images, ...

La version finale du mien est ici :wink: : [https://github.com/philippart-s/](https://github.com/philippart-s/) et le source [ici](https://github.com/philippart-s/philippart-s/blob/main/README.md).