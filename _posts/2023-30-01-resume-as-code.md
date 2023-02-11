---
title: "Curriculum vitae as code 🧑‍💻"
excerpt: "Gérer son CV avec des PR ..."
classes: wide
date: 2023-01-30
categories:
  - Articles
tags:
  - code
---
<meta content="{{ {{ site.url }}{{ site.baseurl }}/assets/images/resume-as-code/resume.png" property="og:image">

![Resume example]({{ site.url }}{{ site.baseurl }}/assets/images/resume-as-code/resume.png){: .align-center}

Cela fait pas mal de temps que je cherche un moyen simple, efficace et sympa pour gérer mon CV.
Avant cet article c"était, selon les périodes, un document Word, Excel, voir même mon [profile](https://github.com/philippart-s/){:target="_blank"} GitHub.

Mais voilà je n'ai jamais été un grand amoureux des traitements de textes ou autre outils pour faire des slides.
Du coup, depuis quelques mois je cherchais une façon de générer un CV en markdown.
Les principaux avantages que j'y voyais : 
 - le markdown est simple et relativement reproductible en terme de rendu,
 - je peux gérer le CV dans des PR,
 - idéalement je veux aussi gérer les données à côté pour apporter du dynamisme,
 - je peux modifier le rendu à loisir (sous réserve de mes capacités en CSS 😅),
 - si c'est en markdown j'aimerai l'intégrer facilement à mon blog en [Jekyll](https://jekyllrb.com/){:target="_blank"}.

Les deux problèmes que je voyais arriver : 
 - la gestion d'une version immuable en PDF pour les envoi à des recruteuses / recruteurs,
 - la potentiel intégration à mon site existant.

Alors pari gagné ?
Voyons ça ensemble ⬇️.


## Tout en liquide ... 💧

![Resume example]({{ site.url }}{{ site.baseurl }}/assets/images/resume-as-code/liquid.jpg){: .align-center}
[César Couto](https://unsplash.com/photos/eIDXdlfelVE?utm_source=unsplash&utm_medium=referral&utm_content=creditShareLink){:style="font-size: smaller"}{:target="_blank"}{: .align-right}

Je vous l'ai dit, j'ai très vite exclus le fait de faire un CV statique en markdown.
Après coup je me dis que cela aurait pu être aussi une bonne solution mais je voulais quelque chose avec les données à part comme on peut trouver dans [JSON Resume](https://jsonresume.org/){:target="_blank"}.

Pour avoir déjà un peu _bidouillé_ Jekyll pour amener du dynamisme il est possible de faire des trucs sympa avec le moteur de templating [Liquid](https://github.com/Shopify/liquid/wiki){:target="_blank"}.  
En quoi cela consiste ?
Définir des templates en HTML pour y injecter du dynamisme avec des boucles par exemple ou de la donnée externe stockée dans un fichier.
Pour les plus vielles et plus vieux d'entre vous ce que l'on faisait en [JSTL](https://www.oracle.com/java/technologies/java-server-tag-library.html){:target="_blank"} 🤪.

Un petit exemple que j'utilise sur ce blog:
{% raw %}
```html
<div class="entries-{{ entries_layout }}">
  {% for post in posts %}
    {% include archive-single.html type=entries_layout %}
  {% endfor %}
</div>
```
{% endraw %}


Car oui, la bonne nouvelle est que c'est nativement inclus dans [Jekyll](https://jekyllrb.com/){:target="_blank"}, le moteur de génération utilisé pour ce site 😎.

## Mais alors comment ça marche ? ⚙️

Au final c'est assez simple : un fichier date au format YAML qui va me permettre de ne pas mettre trop de chose en statique.
Ensuite une page dédiée au CV qui ira piocher les données du CV dans le YAML.

## En conclusion

Que dire comme conclusion ?  
Tout simplement que je suis heureux d'avoir participé à cette nouvelle édition de Touraine Tech.
Que plus tard je pourrais dire que j'ai connu l'époque où c'était sur 1 jour puis la première sur 2 jours 😉.  
Que je suis fier de ma région, de cette équipe de bénévoles qui fait de merveilleuses choses.
Que je suis fier de ma co-speakeuse, de Matthieu que j'ai pu découvrir et accompagner du mieux que je pouvais.  
Et que dire de ma chérie, fier qu'elle se lance dans les conférences, les keynotes et maintenant les workshops de 2h en plus de tout ce qu'elle accomplit professionnellement et personnellement.

Bref j'ai tellement hâte d'être à l'année prochaine !

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog){:target="_blank"} 😊.

Merci à ma relectrice, Fanny, qui vous permet de lire cet article sans avoir trop les yeux qui saignent 😘.