---
title: "Revealjs un truc de geek ?"
link: /2020-05-11-Revealjs
tags: 
  - Slides 
  - Revealjs
author: wilda
---

Lors d'une conf du Devoxx 2018 (souvenez-vous dans l'ancien monde on faisait ça: se réunir pour échanger :wink:) j'avais été assez bluffé par les supports de présentation du speaker fait sur [revealjs](https://revealjs.com/#/).
En bon geek que je suis je m'étais dit qu'il fallait que j'essaie ça un jour et je n'ai pas eu l'occasion ni le temps de le faire.

Je dois donner un talk en interne dans ma société sous forme de BBL et donc pas besoin d'utiliser le modèle d'entreprise, liberté sur le support ! 
Ajouter à ça que c'est un [talk](https://2019.touraine.tech/talk/rz7osZfhbITMlI5B5vK5) que j'avais donné lors du [Touraine Tech](https://touraine.tech/) 2019, l'occasion était trop belle pour tenter de s'y mettre: j'ai le contenu (que je dois mettre à jour mais le plus gros existe) et je vais donc pouvoir calculer le temps que je mets à utiliser revealjs pour faire les slides.
La présentation contient 80 slides ... il y a du boulot !

## Initialisation et configuration du projet
Bon on va coder, donc on commence par initialiser un repo git.
Il existe un [éditeur](https://slides.com/) mais non seulement il est payant et l'idée est de faire du code donc on le laisse de côté.

Lors de l'installation on a le choix entre une installation *simple* (on dézippe la dernière release) ou *avancée* (on clone puis on utilise nodejs pour lancer un serveur web).
Je vais commencer humblement et utiliser le mode simple.

Bon plutôt efficace: 5 mins chrono pour avoir un *hello world* fonctionnel :sunglasses:.

Je n'utiliserai pas la version avancée, je n'ai pas besoin des fonctionnalités proposées et je trouve ça un peu too much de devoir installer node et lancer un serveur web juste pour faire des slides.

## Premiers slides
Assez d'autosatisfaction, j'ai juste décompressé un zip après tout :wink:. 
Passons à l'écriture de nos premiers slides.
Revealjs vient avec une architecture assez simple: tout dans le même fichier HTML !

Ensuite c'est simple:
```html
<div class="reveal">
	<div class="slides">
		<section>Slide 1</section>
		<section>Slide 2</section>
		</div>
	</div>
</div>
```

Il faut une *div* englobante avec la classe CSS *reveal*, une autre avec la classe CSS *slides* et enfin chaque section représente un slide de niveau horizontal. 
C'est une des choses qui m'avaient plues avec revealjs, à savoir la possibilité d'avoir une présentation à deux dimensions: horizontale, défilement normal d'une présentation et verticale, dans ce cas un deuxième niveau de slides est disponible (avec la touche curseur bas).
Pour activer les slides verticaux il suffit d'ajouter une nouvelle *section* enfant à la section:
```html
<div class="reveal">
	<div class="slides">
		<section>Slide 1</section>
		<section>
			<section>Slide 2</section>
			<section>Slide 2.1</section>
		</section>
	</div>
</div>
```

Voilà, c'est à peut près tout, la présentation est initialisée et maintenant on va pouvoir commencer à (essayer) de refaire les slides.

Git oblige je vais commit après chaque slide, l'idée est d'avoir un historique simple à lire mais aussi un retour arrière facilité.

Ce qui marque la première fois que l'on utilise revealjs et que, si comme moi, on n'est pas un as en HTML et CSS, c'est que l'on va être humble sur le rendu. Pour ma part  ce n'est pas un inconvénient car j'avoue être plutôt séduit par les slides épurés.

Il est possible d'utiliser des thèmes pré-packagés, d'autres fournis par la communauté ou le faire soi-même.

J'ai choisi de faire les slides *au kilomètre* afin de me concentrer sur la partie style plus tard (notamment en fonction de mes capacités :wink:).

## Derniers slides
En réalité je n'ai pas été beaucoup plus loin, le premier slide où j'ai voulu mettre du texte et deux images de deux tailles différentes, positionnées de façon pas forcément alignées j'ai vite compris que j'allais devenir fou.

En effet, je suis vite arrivé à mes limites CSS / HTML et le temps que représente la création d'un slide par rapport aux outils classiques m'a fait comprendre que je n'irai pas plus loin.

C'est la conclusion, plus rapide que je ne le pensais, de mon essai de revealjs: j'ai été attiré par le côté geek de pouvoir coder mes slides et au final j'ai eu plus de frustration de ne pas pouvoir faire ce que je voulais simplement et de ne pas faire passer le message que je souhaitais.

Alors cela vient peut être du fait que je ne suis pas (et de loin) un expert en développement front, mais pour moi, passer beaucoup de temps (en plus du temps sur le sujet en lui-même) à faire des slides à la fin de l'étude d'un sujet ne me semble pas valoir le coup.

J'ai jeté un oeil à l'éditeur on-line, qui est très bien fait, mais qui au final ressemble à un Google Slides payant.
Dans ce cas autant utiliser Google Slides.

Donc on inaugure un article qui explique (un peu) comment faire et surtout pourquoi je n'utiliserai pas cet outil :laughing:.

Je mets tout de même ce que j'ai trouvé en points positifs / négatifs sur ma *petite* utilisation de revealjs:

 - les plus:
    - slide as code
    - git, PR, diff, merge
    - beaucoup de thèmes disponibles
 - les moins:
    - tout mettre dans le même fichier HTML, pas super pour la réutilisabilité du code :confused:
    - avoir un style graphique avancé demande des connaissances CSS
    - nécessaire d'avoir chrome (ou chromium) pour pouvoir générer un PDF acceptable
    - la gestion de choses simples comme les images peuvent vite décourager d'utiliser le produit