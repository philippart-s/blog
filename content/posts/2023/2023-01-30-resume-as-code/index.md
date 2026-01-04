---
title: "Curriculum vitae as code 🧑‍💻"
description: "Gérer son CV avec des PR ..."
link: /2023-01-30-resume-as-code
tags: 
  - code
image: resume.png
figCaption: "@wildagsx"
author: wilda
---

Cela fait pas mal de temps que je cherche un moyen simple, efficace et sympa pour gérer mon CV.
Avant cet article c'était, selon les périodes, un document Word, Excel, voir même mon [profile](https://github.com/philippart-s/) GitHub.

Mais voilà, je n'ai jamais été un grand amoureux des traitements de textes ou autre outils pour faire des slides.
Du coup, depuis quelques mois je cherchais une façon de générer un CV en markdown.
Les principaux avantages que j'y voyais : 
 - le markdown est simple et relativement reproductible en terme de rendu,
 - je peux gérer le CV dans des PR,
 - idéalement je veux aussi gérer les données à côté pour apporter du dynamisme,
 - je peux modifier le rendu à loisir (sous réserve de mes capacités en CSS 😅),
 - si c'est en markdown j'aimerai l'intégrer facilement à mon blog en [Jekyll](https://jekyllrb.com/).

Les deux problèmes que je voyais arriver : 
 - la gestion d'une version immuable en PDF pour les envois à des recruteuses / recruteurs,
 - la potentielle intégration à mon site existant.

Alors pari gagné ?
Voyons ça ensemble ⬇️.


## Tout en liquide ... 💧

![Resume example](liquid.jpg)
[César Couto](https://unsplash.com/photos/eIDXdlfelVE?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

Je vous l'ai dit, j'ai très vite exclu le fait de faire un CV statique en markdown.
Après coup, je me dis que cela aurait pu être aussi une bonne solution mais je voulais quelque chose avec les données à part comme on peut trouver dans [JSON Resume](https://jsonresume.org/).

Pour avoir déjà un peu _bidouillé_ Jekyll pour amener du dynamisme il est possible de faire des trucs sympas avec le moteur de templating [Liquid](https://github.com/Shopify/liquid/wiki).  
En quoi cela consiste ?
Définir des templates en HTML pour y injecter du dynamisme avec des boucles par exemple ou de la donnée externe stockée dans un fichier.
Pour les plus vielles et plus vieux d'entre vous ce que l'on faisait en [JSTL](https://www.oracle.com/java/technologies/java-server-tag-library.html) 🤪.

Un petit exemple que j'utilise sur ce blog:

```html
<div class="entries-{{ entries_layout }}">
  {% for post in posts %}
    {% include archive-single.html type=entries_layout %}
  {% endfor %}
</div>
```



Car oui, la bonne nouvelle est que c'est nativement inclus dans [Jekyll](https://jekyllrb.com/), le moteur de génération utilisé pour ce site 😎.

## Mais alors comment ça marche ? ⚙️

![Architecture de Jekyll](jekyll-archi.png)
[@wildagsx](https://twitter.com/wildagsx)


Au final, il y a deux choses principales pour faire un site avec Jekyll : 
  - la partie design : de manière classique les compétences vont être axées sur le HTML, CSS et Javascript,
  - la partie génération / dynamisme : là ce sont plus des compétences algorithmiques classiques et la connaissance de [Liquid](https://github.com/Shopify/liquid/wiki) qui vont être importantes.

Autant vous le dire tout suite : CSS / JS ne font pas bon ménage avec moi 😅, plutôt que de repartir de zéro et faire un truc moche, j'ai donc choisi de partir d'un [thème Jekyll](https://jekyllrb.com/docs/themes/#pick-up-a-theme) existant.

> Note 📝 :  
> Un thème Jekyll est composé de sa partie _statique_ (html, css, js) mais aussi _dynamique_ (liquid).
> C'est en fait un site opérationnel.  
> La seule différence est que vous pouvez le _référencer_ comme votre site _parent_ et surcharger et / ou configurer les éléments de votre choix.
> Comme c'est un site opérationnel, vous pouvez très bien choisir de tout simplement en faire un _fork_ puis de l'adapter à vos besoins.  
> A titre d'exemple, ce blog utilise la première option en référençant le thème parent _minimal-mistakes_ dans le _config.yml_ : `remote_theme: mmistakes/minimal-mistakes`.

Dans mon cas, je savais que j'aurai des modifications structurelles à apporter au thème d'origine.
J'ai donc choisi de faire un fork.
J'en ai profité pour proposer des PR avec les modifications que j'apportais au fur et à mesure 🤝.

Les modifications que j'ai apportées au thème d'origine : 
 - choix de la localisation de la _sidebar_ (droite ou gauche),
 - support du multilingues pour avoir un CV en français et un en anglais.

## Codons notre CV 👨‍💻

![Image code](code.jpg)
[Chris Ried](https://unsplash.com/fr/photos/ieic5Tq8YMk?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink)

Je vous l'ai dit, je suis parti d'un thème existant : [online CV](https://jekyllthemes.io/theme/online-cv) de [Sharath Kumar](https://github.com/sharu725).
J'ai beaucoup aimé le look général du CV et comme je vous l'ai dit c'était bien au delà de ce que je pouvais rêver arriver comme résultat si j'étais parti de rien 🤩.  
Comme déjà indiqué dans le paragraphe précédent, j'ai customisé le thème pour qu'il colle parfaitement à mes besoins.

Dans le détail quelles sont les étapes pour arriver à mon CV final ?

### Fork du repository 🔀

Rien de bien compliqué, dans ce cas Github est votre ami pour faire directement un fork 😉.
Ensuite l'idée était de pouvoir continuer à participer au projet tout en créant mon CV sans envoyer mes données dans le thème d'origine 😅.
Pour cela, j'ai simplement créé une branche _my-resume_ qui me sert de branche de production dans Github pour la configuration de Github pages.
Cette branche contient le code _upstream_, le code des PR que j'ai proposées en attente de validation (ou pas) et le code propre à mon CV à moi.

Tout ceci se trouve ici : [online-cv](https://github.com/philippart-s/online-cv).

### Ajout de fonctionnalités ✨

La première fonctionnalité que j'ai rajoutée permet de mettre la barre latérale d'informations (nom, prénom, téléphone, ...) à gauche ou à droite.
Pour cela j'ai rajouté un champ de configuration dans le fichier _data.yml_ : 
```yaml
sidebar:
    position: left # position of the sidebar : left or right
```

> Note 📝 :  
> Avec Jekyll et Liquid vos données sont stockées dans un fichier YAML dans le répertoire \_data.
> Ensuite pour y accéder dans des tags Liquid : `site.data.<nom du fichier>`

Ensuite j'ai modifié le CSS (ouh là 😱) pour la prise en compte du choix : 

```css
// define variable with front matter
{% if site.data.data.sidebar.position == 'left' %}
$resume-order: 2;
$sidebar-order: 1;
{% else %}
$resume-order: 1;
$sidebar-order: 2;
{% endif %}
```



Ensuite il ne me restait _plus_ qu'à utiliser les variables dans le SCSS :
```scss
.sidebar-wrapper {
  grid-column: span 3;
  order: $sidebar-order;
  // ...
}
```

Le détail de cette modification est dans la [PR#359](https://github.com/sharu725/online-cv/pull/359) sur le repo d'origine. 

L'autre modification consistait à pouvoir avoir mon CV en anglais en plus du français.
Dans les issues du repository d'origine il est indiqué qu'il faut créer autant de repositories que de versions du CV.
Cela me dérangeait par rapport à la duplication de code alors que techniquement, seul le fichier _data.yml_ change puisqu'il contient l'ensemble des données : titres de sections et informations du CV.

La première chose a été de créer un nouveau fichier de données en rajoutant la possibilité de spécifier la langue : `data-fr.yml`.  
Ensuite j'ai créé un layout par langue : 

```html
---
#Add "layout: compress" here to compress the html code
---
<!DOCTYPE html>
<!--[if IE 8]> <html lang="en" class="ie8"> <![endif]-->
<!--[if IE 9]> <html lang="en" class="ie9"> <![endif]-->
<!--[if !IE]><!--> <html lang="en"> <!--<![endif]-->
{% include head.html %}
{% assign data = site.data.data-fr %}
<body>
  <div class="wrapper">
    {% include sidebar.html %}

    <div class="main-wrapper">
        {{ content }}
    </div>
  </div>

  {% include footer.html %}

{% include scripts.html %}
</body>

</html>
```

La principale chose à retenir est la récupération du bon fichier de data : `{% assign data = site.data.data-fr %}`.


Enfin, créer un nouvel index utilisant ce layout : 

```html
---
layout: default-fr
---
{% assign data = site.data.data-fr %}

{% include index-common.html %}
```


Le détail de l'ensemble de ces modifications se trouve dans la [PR#360](https://github.com/sharu725/online-cv/pull/360) dans le repository d'origine.

### Ajout des données 🔠

C'est la partie la plus simple, remplir le _data.yml_ avec mes données que je souhaitais voir apparaître dans mon CV.
```yaml
sidebar:
    about: False # set to False or comment line if you want to remove the "how to use?" in the sidebar
    education: True # set to False if you want education in main section instead of in sidebar

    # Profile information
    name: Stéphane Philippart
    tagline: Developer Relation
    avatar: stephane_philippart.jpg 
# ...
```

Je vous laisse aller voir le [détail](https://github.com/philippart-s/online-cv/blob/my-resume/_data/data.yml) sur le repository.

### L'impression 🖨️

Le thème permet d'avoir une [page](https://philippart-s.github.io/online-cv/print) dédiée à l'impression.
C'est bien, mais l'idée est aussi d'avoir un PDF afin de pouvoir le fournir à des personnes souhaitant avoir le CV au format fichier.
La problématique est d'arriver à générer un PDF _propre_ à partir d'une page Web.
La version _imprimable_ n'est pas géniale et au final je pense qu'un peu de CSS pourrait arranger ça mais vous me connaissez le CSS et moi 😅.

Du coup, je suis parti sur utiliser un convertisseur en ligne qui fait ça très bien : [https://www.freeconvert.com/webpage-to-pdf](https://www.freeconvert.com/webpage-to-pdf).
Cerise sur le gâteau il génère une [version PDF](https://philippart-s.github.io/online-cv/assets/pdf/cv-fr-stephane-philippart.pdf) à partir de la page principale très propre 😎.

## En conclusion

Voilà il reste des choses à faire et à peaufiner, par exemple : 
 - gérer la langue par défaut en configuration plutôt qu'en dure dans les fichiers,
 - ne pas avoir de duplication de code,
 - optimiser le CSS,
 - ...
 
Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.