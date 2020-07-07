---
title: "Débuter avec SpringBatch"
#excerpt: "Tester c'est douter , Vraiment ? :thinking:"
classes: wide
categories:
  - Articles
  - Code
tags:
  - Java
  - SpringBatch
---
# Un peu d'histoire ... Je vous parle d'un temps où les moins de 20 ans ...
Avant de commencer à parler de SpringBatch et de comment développer un batch il me semblait utile de prendre le temps de se souvenir de ce qu'il y avait avant afin de comprendre pourquoi utiliser SpringBatch.

Et puis, cela aura aussi comme vertu de montrer au plus jeunes qu'il y avait autre chose avant Java et Spring :wink:.

## Pourquoi un batch ?
C'est peut être évident mais au final pourquoi un batch et dans quel cas c'est plus utile qu'un programme dit *classique* ?

Prenons la définition de Wikipédia:
>Batch signifie « lots » en anglais et peut faire référence à :
 - un traitement par lots, en informatique, un enchaînement automatique de commandes 

 Tout est dit ! Là où, par exemple, un service back end va permettre une action unitaire d'une application : l'ajout d'un client, un batch va permettre de faire la même action mais plusieurs fois : les lots.

 Vous allez me dire eh bien oui mais mon service pourrait faire une boucle et lui aussi il ferait un traitement par lots ? Oui c'est vrai mais là je parle de lots avec beaucoup beaucoup de données et qui prennent plusieurs minutes voir plusieurs heures ! On imagine mal un utilisateur attendre plus de quelques minutes devant son écran que le service appelé par son application ait fini son traitement ! Ce n'est pas qu'une question de temps ou de taille de donnée, c'est aussi une approche différente.

 Lorsque l'on pense *donnée first* on ne développe pas de la même façon que ce que l'on fait pour une interface Web. Il va falloir, notamment, commencer à penser ensembliste et non unitaire !

 ## Extract Transform Load : ETL
 C'est à ce moment que l'on rentre dans les paradigmes apportés par les *ETL* : raccourcis pour englober l'ensemble des logiciels mais aussi de la façon de développer lorsque traiter de manière unitaire la donnée n'est plus possible. Il faut réfléchir ensemble de données dit autrement on travaille sur des lots de données (la définition de Wikipédia n'est pas si mauvaise au final :wink:). Attention cela ne veux pas dire que l'on ne peut pas traiter de la donnée ligne à ligne mais l'apport d'un ETL sera alors assez marginale (à puissance machine équivalente). Enfin, la plupart des ETL sont aussi nativement connus pour travailler en parallèle (même si les données doivent le supporter) et là le gain devient très important !

**TODO** image pour flux ETL

 ### Extract
 Rentrons dans le détail, la première phase lorsque l'on veut traiter de la données est d'aller la chercher ! C'est ce qui se cache derrière la phase d'extraction (*Extract*) : aller lire de la donnée, plus précisément notre lot de données que ce soit dans un fichier, une base de données ou tout autre endroit ! Idéalement ce lot est chargé en mémoire afin de faciliter sa manipulation future. 

 ### Transform
 A mon sens la phase la plus importante même si parfois elle consiste à ne rien faire mais dans ce cas utilise t on correctement notre ETL :thinking: ? Cette phase consiste donc à manipuler notre lot de données précédemment charger, cela peut être du nettoyage de données, de l'enrichissement de données, de la vérification, ... Bref tout ce que le cas métier nécessite.

 Pourquoi je pense que c'est la phase la plus importante ? Déjà parceque notre intelligence métier se trouve ici et ensuite parce qu’elle si elle est mal faite les temps de traitements peuvent être catastrophiques !

 Enfin pourquoi j'indiquais que, parfois, on ne fait rien dans cette étape ? Tout simplement que certains traitements ETL se contentes d'extraire des données d'une source A pour aller simplement l'écrire dans une cible B. Si ils en sont tout à fait capables, le fait de ne mettre aucune intelligence fait que, souvent, ils vont être moins performants (et utiles ?) que de simples scripts SQL d'extraction et de chargement :wink:. Mais c'est aussi pour cela que je pense que dans ce cas on ne devrait pas utiliser d'ETL et donc que l'on n'utilise par correctement l'ETL.

 ### Load
 Dernière phase, une fois que la données a été modifiée il faut la mettre à disposition dans notre cible. Là encore fichier, base de données ou tout autre endroit est possible. L'écriture, elle aussi par lot, peux, comme le reste des étapes être effectuée en parallèle si tant est qu'elle le supporte et que la cible le supporte aussi !

 ### Les inconvénients des ETL
 Avec tout ça on se dit que c'est quelque chose de pas mal les ETL ! Et oui c'est le cas, mais ... 

 Cela reste, souvent, une technologie qui date plutôt de l'époque des gros systèmes et pas souvent orientée algorithmique. Cela peut rebuter pas mal de nos développeur *full stack* à toujours être habitués aux langages de plus en plus fun et cool à utiliser !

 Mais ça encore ce n'est qu'une question de goût et comme j'aime à le dire un développeur heureux est un développeur performant ! Cependant on commence à peiner à trouver des développeurs sur ces ETL et ceux que l'on trouve peuvent être assez chers de par leur longue expérience :wink:.

 TODO : lister ETL + pb de coût 