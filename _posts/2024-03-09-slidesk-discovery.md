---
title: "🖼️ Slides as code avec SliDesk 👨‍💻"
classes: wide
excerpt: "Coder ses slides simplement."
categories:
  - Articles
  - Dev
  - Documentation
  
tags:
  - SliDesk

---
![Image sliDesk]({{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/slidesk-logo.png){: .align-center}
[© Gouz](https://twitter.com/GouZ){:style="font-size: smaller"}{:target="_blank"}{: .align-right}

<br/>

---
>**📜 TL;DR**
>
>Projet de slide as code simple, léger et personnalisable.
>
> - 🔗 [https://slidesk.github.io/slidesk-doc/](https://slidesk.github.io/slidesk-doc/){:target="_blank"}
> - 🔗 [https://github.com/slidesk/slidesk](https://github.com/slidesk/slidesk){:target="_blank"}
> - 🔗 [https://slidesk.github.io/slidesk/](https://slidesk.github.io/slidesk/){:target="_blank"}

--- 

Il y a quelques temps je me suis [déjà essayé]({{ site.baseurl }}{% post_url 2020-05-11-Revealjs %}{:target="_blank"}), avec peu de succès, à coder mes slides.
A cette époque j'ai testé [Reveal.js](https://revealjs.com/){:target="_blank"} et je n'ai pas réussi à aller au bout de mon test pour plusieurs raisons.
Les principales : un niveau trop faible en CSS, la complexité de Reveal.js, un niveau trop faible en CSS 😅.  
Mais, alors, pourquoi réessayer ?
Tout simplement parce que, depuis, l'envie de coder mes slides plutôt que de les _dessiner_ ne m'a pas quitté.
Voyez-y de la geekerie (que j'assume) mais aussi pour les raisons suivantes : 
 - je ne peux pas forcément utiliser facilement les outils classiques que sont Office ou Google Slides,
 - je trouve le suivi de version de ces outils pas assez abouti,
 - le travail collaboratif par PR me semble plus approprié lorsque l'on travaille à plusieurs,
 - la gestion multilingues où il faut tout refaire ne me convient pas,
 - j'aime coder 😉

Il n'empêche que lorsque j'ai, de nouveau, franchi le pas en début d'année dernière, je me suis confronté aux mêmes problèmes que la dernière fois en réessayant Reveal.js et consorts.
Autre élément qui m'a aussi ennuyé le côté monolithe du dev alors que mon envie était la possibilité de traiter mes slides comme un dev : factorisable, générique et composable.

# 🤔 Pourquoi SliDesk ?

Comme souvent, tout part d'une discussion 😉.
Dans ce cas, avec [Sylvain](https://twitter.com/GouZ){:target="_blank"}, l'auteur de SliDesk.
Je lui indiquai, que je galérai avec Reveal.js et qu'en plus, nativement, le multilingues n'était pas super bien géré (et toujours le côté monolithe qui ne me plaisait pas).

De fil en aiguille, Sylvain, qui bossait sur le sujet SliDesk me propose d'intégrer cette fonctionnalité dans ses devs 🤩.
L'idée, aussi, de l'aider dans la mise au point de son projet, avec mes retours de débutant de CSS et JS étaient aussi l'occasion de participer avec lui à son aventure.

Enfin, Sylvain, me l'avait indiqué, son objectif était de faire un slide as code simple et personnalisable !

> Je ne suis pas aussi calé qu'Hubert Sablonière qui code carrément tout un site pour les slides des ses talks 🤩.
> Allez jeter un coup d'oeil à son [site](https://www.hsablonniere.com/talks/){:target="_blank"}.

# 🏁 Démarrer avec SliDesk

Les premières choses à faire : aller faire un tour du côté du [repository](https://github.com/slidesk/slidesk){:target="_blank"} GitHub, de la [documentation](https://slidesk.github.io/slidesk-doc/){:target="_blank"} et de l'[exemple]([https://slidesk.github.io/slidesk/){:target="_blank"}.

Ensuite vient le temps _d'installer_ SliDesk.
Ou plus précisément, la [CLI](https://slidesk.github.io/slidesk-doc/docs/usage/intro){:target="_blank"} de SliDesk qui va être votre compagnon pour tirer pleinement partie de SliDesk.

J'en parlerai plus tard mais la CLI vous permettra aussi de _servir_ vos pages pour afficher vos slides.

Avant d'aller plus loin il vous faut donc installer SliDesk pour cela suivez la [documentation](https://slidesk.github.io/slidesk-doc/docs/category/installation){:target="_blank"} qui explique les différentes façons de procéder.
pour ma part j'ai utilisé brew : `brew install gouz/tools/slidesk`.

## ✨ Initialisation d'un projet

Pour initialiser un projet,rien de plus simple : `slidesk create slidesk-discovery`.  
```bash
$ slidesk create slidesk-discovery
 ____(•)-
(SliDesk) v 2.4.3

Creation of your talk: slidesk-discovery
> What is the title of talk?
SliDesk discovery
> Do you want to customize the presentation? [yN]
y
```
> le fait d'indiquer que l'on souhaite personnaliser la présentation permet d'avoir un fichier _custom.css_ de créé.

Une fois la commande exécutée, l'arborescence suivante est créée : 
```bash
slidesk-discovery
├── custom.css
└── main.sdf
```

On y retrouve le fichier _custom.css_ dont je vous parlais et le fichier _main.sdf_ qui va être le fichier _point d'entrée_ de notre présentation.

> l'extension _.sdf_ est propre à SliDesk mais au final c'est un format texte où l'on va mélanger syntaxe SliDesk, Markdown et HTML.

Voyons le contenu du fichier _main.sdf_ : 
```markdown
/::
custom_css: custom.css
::/

# SliDesk discovery 

## My first Slide
```

> J'utilise ici le markdown pour la coloration syntaxique mais Sylvain a développé une [extension VSCode](https://marketplace.visualstudio.com/items?itemName=gouz.sdf){:target="_blank"} qui permet d'avoir la coloration des fichiers _sdf_

On voit ici la structure d'un slide SliDesk : 
 - `# SliDesk discovery ` : titre principal d'une présentation, représente le premier slide
 - `## My first Slide` : un slide simple dans SliDesk.

Voyons ce que cela donne de manière visuelle.
Pour cela il faut _exécuter_ notre présentation : `slidesk`.
```bash
$ slidesk                         
 ____(•)-
(SliDesk) v 2.4.3

Take the control of your presentation direct from here. 
 
Press Enter to go to the next slide. 
Press P + Enter to go to the previous slide. 
Press Q to quit the program. 

Your presentation is available on: http://localhost:1337
```

Lorsque l'on exécute _slidesk_, si un fichier _main.sdf_ est présent cela lance automatiquement a présentation à partir de celui-ci.
On voit aussi qu'il est possible de naviguer via le terminal en utilisant le clavier et de se connecter à la présentation pour l'afficher sur le port `1337` (paramétrage par défaut facilement changeable).

<table>
  <tr >
    <td style="border-bottom: 0px">
      <img src="{{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/first-slide.png" alt="">
    </td>
    <td style="border-bottom: 0px">
      <img src="{{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/slide-example.png" alt="">
    </td>
  </tr>
  <tr>
    <td colspan=2 style="border-bottom: 0px; text-align: center">
      <i>Rendu des slides lors de la génération d'un projet</i>
    </td>
  </tr>
</table>

## 🏗️ Organisation de ses sources

Je vous l'ai dit, une des choses que je n'aimais pas avec les autres Frameworks c'était l'obligation de tout mettre dans un seul fichier.
Je trouvais ça contre productif et contre intuitif (et accessoirement impossible de travailler efficacement à plusieurs).

SliDesk, au contraire, permet une totale liberté d'organisation de slides, d'images, de CSS, JS, ...
A noter, qu'il vous est aussi possible de tout mettre dans un seul fichier si vous préférez 😊.

Voici un exemple d'organisation de sources : 
```bash
.
├── assets
│  ├── css
│  │  └── custom.css
│  └── images
│     └── prez
│        ├── ours.png
│        ├── tadx.png
│        └── whoami.png
├── LICENSE
├── main.sdf
├── README.md
└── slides
   └── speaker.sdf
```
On retrouve le ficher _main.sdf_ à la racine.
Les ressources dans un répertoire _assets_ avec ses sous répertoires selon les ressources.
Et enfin, un répertoire _slides_ avec les différents fichiers _sdf_.
> A noter qu'il est possible de créer d'autres sous répertoires afin de classer comme bon vous semble les différents fichiers.

Mais comment cela fonctionne au final l'assemblage ?
Grâce à la directive `include`.
Le fichier _main.sdf_ devient donc : 
```markdown
/::
custom_css: assets/css/custom.css
::/

# SliDesk discovery 

!include(slides/speaker.sdf)
```

On voit ici l'inclusion du slide _speaker.sdf_ mais aussi de la feuille de style _custom.css_ qui se trouvent tous les deux dans des sous arborescences.

C'est cette liberté dans l'organisation et dans la syntaxe (on le verra plus tard) que j'aime dans SliDesk.

## 🎨 Un peu de personnalisation

Comme je vous l'ai déjà indiqué, il est possible de personnaliser SliDesk avec sa propre feuille de style.
En effet, SliDesk se veut simple et vient avec un style épuré et simple.
Nul doute, que quelque soit votre niveau de CSS (et croyez moi, je sais de quoi je parle 😅) vous aurez besoin de personnaliser le rendu de vos slides.
Il est possible de tout faire en HTML, oui j'ai essayé tellement je n'aime pas le CSS 😂.
Force est de constaté, qu'il est plus simple et plus dans l'esprit de SliDesk de le faire en CSS.
Je ne suis pas devenu un pro de CSS mais avec un peu d'habitude, beaucoup d'inspecteur de code et encore plus de demande d'aide de Sylvain (merci à toi) on y arrive !

Dans mon cas c'est assez simple car mon template favori pour faire des slides : un fond blanc, une police noire et des listes à puces.
Quelques mages et emojis et on a fait le tour !
Là où j'ai le plus eu de difficultés c'est avec les images pour les positionner comme il faut.
De plus, SliDesk est responsive, l'idée est donc que les images suivent aussi lorsque la résolution ou la place changent 😉.

SliDesk vient à notre secours avec une directive `image` mais il possible que vous ayez besoin de gérer une ou deux images pour des besoins spécifiques.
Mais revenons à nos mouton et voyons un peu ce que donne la personnalisation.
```css
:root {
  --sd-heading1-size: 8.5vw;
  --sd-heading1-line-height: 1;
  --sd-heading2-size: 5vw;
  --sd-heading2-line-height: 1;
  --sd-text-size: 2.2vw;
  --sd-text-line-height: 1.2;

  --sd-background-color: #fff;
  --sd-heading-color: #000;
  --sd-text-color: #000;
  --sd-primary-color: rgb(37, 186, 146);

  /* SpeakerView */
  --sd-sv-timer-size: 80px;
  --sd-sv-text-size: 40px;
  --sd-sv-text-line-height: 1.2;
  --sd-sv-background-color: #242424;
  --sd-sv-text-color: rgba(255, 255, 255, 0.87);
}

.slide-text {
  display: block;
}
.slide-text h2 {
  text-align: left;
  margin-bottom: 5rem;
}

.slide-text ul {
  margin: 2rem;
  width: 62%;
}

.slide-text ul li {
  list-style: none;
  line-height: 1.6em;
}

.slide-text .twitter {
  color: var(--sd-primary-color);
}

.slide-text p {
  text-align: left;
  line-height: 1.6em;
}
```
Je reviendrai sur la _speaker view_ présente dans le CSS plus tard 😉

## 🖼️ Coder ses slides

Il est, maintenant, grand temps de coder son premier slide.
```markdown
## Stéphane Philippart .[slide-text]

!image(assets/images/prez/whoami.png,, 494, 155, float: right; margin-top: -16vh)

- 🏷️ 🥑 DeveloperAdvocate@OVHCloud 🦄
- 🏷️ Co-creator of TADx (Agile, Dev, DevOps meetups in Tours)

!image(assets/images/prez/tadx.png, tadx, 304, 165, float: right; margin-top: -9vh)

- ☕️ A Java developer in the cloud ☁️

!image(assets/images/prez/ours.png, ours, 437, 414, float: right;)

- 🐦 <span class="twitter">@wildagsx</span>
- 🔗 https://philippart-s.github.io/blog
- 🐙 https://github.com/philippart-s/
- 💬 https://www.linkedin.com/in/philippartstephane/
```

Ici on voit plusieurs choses.
Commençons par la syntaxe markdown : on retrouve les titres de sections `##` et les listes à puce `-`.
On peut appliquer un style CSS ç l'ensemble de la slide `.[slide-text]`.
On découvre aussi la directive [image](https://slidesk.github.io/slidesk-doc/docs/syntax/Image/){:target="_blank"} en action.

Le CSS précédent avec le code ci-dessous nous donne maintenant un joli slide de présentation de speaker.

![Slide speaker]({{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/speaker-slide.png){: .align-center}

## 🧩 Les plugins

SliDesk vient avec des [plugins](https://slidesk.github.io/slidesk-doc/docs/plugins/intro){:target="_blank"} pré-définis.
Vous en avez déjà pas mal et plutôt cool comme piloter sa prez avec un gamepad, avoir des listes à puces animées, une bare de progression, ...
Je vous laisse allez voir la vingtaine de plugins disponibles.

Et si vous ne trouvez pas votre bonheur vous pouvez en créer vous même.
C'est, avec mon petit niveau front, ce que j'ai fait.
J'avais besoin de rajouter un footer avec logo et autres informations, du coup j'ai créer un plugin.
Pour cea il faut créer un répertoire `plugins` à la racine de votre projet puis un sous répertoire contenant votre plugin.
Dans mon cas ce sera `footer`.
Ensuite il faut créer quelques fichiers.

### ⚙️ Plugin.json

C'est le fichier principal qui va contenir les paramètres du plugin.
```json
{
  "addHTMLFromFiles": ["./plugins/footer/footer.html"],
  "addStyles": ["./plugins/footer/footer.css"]
}
```
Il y a [plusieurs champs](https://slidesk.github.io/slidesk-doc/docs/plugins/intro){:target="_blank"} que vous pouvez renseigner.
Dans mon cas j'ai utiliser deux champs :
 - le premier pour indiquer le code HTML de mon plugin
 - le deuxième pour indiquer le CSS propre à mon plugin

### 📜 footer.html

Fichier qui contient simplement le code HTML du plugin.
```html
<div class="footer">
  <div class="ovhcloud-footer">
    <img src="assets/images/OVHcloud_logo.png" alt="OVHcloud right logo" style="width: 20%" />
  </div>
  <div class="author-footer">@wildagsx</div>
  <div class="footer">
    <img src="assets/images/footer.png" alt="Centered footer image" style="width: 25%;margin: auto;" />
  </div>
</div>
```

### 🎨 footer.css

Fichier qui contient la feuille de style du plugin.
```css
/****
 * CSS for center element
 ****/
.footer {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  color: rgb(0, 0, 0);
  text-align: center;
  transition: all var(--animationTimer) ease;
}

/****
  * CSS for OVHcloud logo
  ****/
.ovhcloud-footer {
  position: absolute;
  left: 0%;
  top: 50%;
  transform: translateY(-50%);
}

.ovhcloud-footer img {
  padding: 2px;
  transition: all var(--animationTimer) ease;
}

/****
  * CSS for logo
  ****/
.img-footer {
  margin: 0 auto;
  width: 100px;
  text-align: center;
}

/****
 * CSS for author element
 ****/
.author-footer {
  position: absolute;
  right: 0%;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.4em;
}
```

Au final, nos slides ont un beau footer !

![Exemple footer]({{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/footer-slide.png){: .align-center}

## 👀 La speaker view

Arrêtons-nous à la fonctionnalité qui va en intéresser plus d'un•es : la possibilité d'afficher les [notes speakers](https://slidesk.github.io/slidesk-doc/docs/syntax/Comments/speaker-notes){:target="_blank"}.
A titre personnel, je ne l'utilise pas : mon cerveaux ne sait pas faire deux choses en même temps (parler et lire) 😅.
La speaker view de SliDesk permet :
 - d'afficher les note speakers
 - d'afficher un timer pour savoir combien de temps on doit passer sur le slide ou à combien de temps on doit être sur la présentation

Pour avoir comme speaker note rien de plus facile : tout commentaire dans les slides sera affiché dans les speaker notes, les commentaires doivent être au format `/* commentaire */` (bien sûr il peut être multi-lignes).

Une feature que je trouve super intéressante avec les speaker notes : vous pouvez [les afficher sur un device autre](https://slidesk.github.io/slidesk-doc/docs/usage/options/notes){:target="_blank"}, par exemple un iPad, et c'est automatiquement synchronisé avec ce que vous projetez.
Pratique en conférence qd il n'y a pas de déport speaker 😉.

## 🏴󠁧󠁢󠁥󠁮󠁧󠁿 L'internationalisation

Une des raisons qui me fait regarder du côte des slides as code est la réutilisation.
Notamment lorsque l'on doit donner la même conférence dans une langue différente.
Avec les outils classiques il faut dupliquer la prez et tout modifier et lorsqu'il y a des modifications les synchroniser.
SliDesk me permet de gérer les libellé et autres éléments de phrase dans [un fichier JSON externe](https://slidesk.github.io/slidesk-doc/docs/internationalisation/intro){:target="_blank"} pour chaque langue, puis de choisir lequel utiliser.

On a un fichier, `FR.lang.json` par défaut.
```json
{
  "default": true,
  "translations": {
    "title": "Découverte de SliDesk"
  }
}
```

Puis autant de fichiers que de langues souhaitées, par exemple pour l'anglais : 
```json
{
  "translations": {
    "title": "SliDesk discovery"
  }
}
```

Et cela donne.
<table>
  <tr >
    <td style="border-bottom: 0px">
      <img src="{{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/french-main-slide.png" alt="Main slide in French">
    </td>
    <td style="border-bottom: 0px">
      <img src="{{ site.url }}{{ site.baseurl }}/assets/images/slidesk-discovery/english-main-slide.png" alt="Main slide in English">
    </td>
  </tr>
  <tr>
    <td colspan=2 style="border-bottom: 0px; text-align: center">
      <i>Activation de l'option multi-lingues</i>
    </td>
  </tr>
</table>

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog){:target="_blank"} 😊.