---
title: "Configurer la télécommande Logitech Harmony 700"
classes: wide
categories:
  - Articles
  - IOT
tags:
  - Logitech
  - Harmony
  - Ubuntu
---
Pour changer je vais partager un post qui n'a pas de rapport avec le développement (quoique je vais peut être finir par en parler :wink:).

Comme beaucoup d'entre nous je possède pas mal d'appareils audio - vidéo ... chacun avec leurs télécommandes !
Pour pallier à cela il y a déjà une bonne dizaine d'années j'ai voulu prendre une télécommande pour toutes les remplacer (_One remote to rule them all_ :laughing:).
Jusque là rien de bien passionnant. 
Mon côté _fashion geek_ et développeur ne datent pas d'aujourd'hui, je me suis donc tourné vers les télécommandes _programmables_.

Autant le dire tout suite : ici pas de code mais la notion de pouvoir enchaîner des actions en appuyant sur un seul bouton et ainsi répartir sur une seule télécommande le pilotage de plusieurs appareils sans changer de télécommande ou de _profile_ sur la télécommande universelle.
Pour illustrer mon propos : pour regarder un Blu-Ray avec une télé, un lecteur et un ampli la télécommande, une fois _programmée_, se comporte de la manière suivante:
 - les touches de son pilotent l'ampli
 - les touches play, stop, etc le lecteur
 - les touches de format d'image la télé

Ajoutons à cela que lorsque l'on sélectionne l’activité _regarder un film_ cela allume tout ce beau monde dans l'ordre et sélectionne les bonnes entrées audio / vidéo.
A ce jour le constructeur qui faisait, selon moi, les meilleures télécommandes de ce type, à un prix abordable, était Logitech.

A l'époque j'ai donc acheté le modèle [Harmony 700](https://support.myharmony.com/fr-fr/700){:target="_blank"}.

:warning: Récemment ils ont annoncé qu'ils arrêtaient la commercialisation de ce type d'appareil, c'est bien dommage. :warning:

## :roll_eyes: C'est bien gentil mais pourquoi parle-t-on de tout ça ?
J'y viens !

La chose à savoir est que Logitech fourni une application permettant la configuration de la télécommande par USB, uniquement compatible Windows ou Mac.
Ce qui ne me posait pas de problème il y a 10 ans mais maintenant oui !
En effet, depuis maintenant quelques années je suis passé sous Ubuntu et ne possède plus d'ordinateur perso sous Windows.
Impossible donc de mettre à jour ma télécommande, ce qui devenait problématique depuis quelques changements sur ma configuration audio / vidéo.
C'est que l'on s’habitue à n'avoir qu'une seule télécommande à la maison et c'est tellement [WAF](https://en.wikipedia.org/wiki/Wife_acceptance_factor){:target="_blank"} !

## :thinking: Alors comment faire pour mettre à jour cette télécommande ?
:x: La première chose que j'ai essayé : [Wine](https://www.winehq.org/){:target="_blank"} : un échec.
J'ai réussi à l'installer mais impossible qu'il détecte ma télécommande.

:x: Ayant un poste pro avec Windows 10 je me suis dit allez, juste pour la modif je l'installe puis ensuite je le retire (on sens déjà le modop pas supper mais si ça pouvait me dépanner).
Le logiciel de synchronisation est écrit en [Sylverligth](https://www.microsoft.com/Silverlight/){:target="_blank"} ... et comment vous dire, entre les détections aléatoires et les problèmes de plantages je n'étais pas plus avancé ...

Google étant ton ami (enfin parfois :yum:) je me suis mis à la recherche d'une façon de faire sous Ubuntu, quitte  à faire quelques commandes en Shell.

:white_check_mark: Et c'est là que vient la bonne surprise je tombe sur deux projets écrits en C et en Python : 
 - [Concordance](hhttps://phildev.net/concordance/){:target="_blank"} qui permet de communiquer avec la télécommande et de faire toutes les actions nécessaires : flash de firmeware, apprentissage par IR, ... Tout se base sur des fichiers propriétaires issus du site Web de Logitech permetttant la création de la configuration (j'y reviendrai plus tard)
 - [Congruity](https://sourceforge.net/projects/congruity/){:target="_blank"} est une interface graphique au dessus de concordance qui permet de simplifier grandement l'utilisation de celui-ci :wink:

## :hammer_and_wrench: Installation de Concordance & Congruity

La partie communication, Concordance,  est écrite en C ([repository](https://github.com/jaymzh/concordance){:target="_blank"} GitHub), la partie interface en Pyton ([repository](https://github.com/congruity/congruity){:target="_blank"} GitHub).
Chacun des deux outils peuvent s'installer sur les trois plate-formes Windows, Linux et MacOs. 
Je n'ai testé que la partie Linux :wink:.

L'installation de concordance est simple:
```bash
sudo apt-get install concordance
```
Il est possible de le faire en rebuildant les sources, je vous laisse aller voir cela pour la plate-forme de votre choix, moi je préfère la simplicité !

L'installation de Congruity n'est pas plus compliquée mais demande quelques étapes :
 - vérifier les prérequis :
  - python d'installé, pour ma part je suis en 3.9.4
  - libconcord, normalement vient avec l'installation de concordance mais il est possible de l'installer à part : )
  ```bash
  sudo apt-get install python3-libconcord
  ```
  - pip : [https://pypi.org/project/pip/](https://pypi.org/project/pip/)
  - setuptools : [https://pypi.org/project/setuptools/](https://pypi.org/project/setuptools/)
 - récupérer la dernière version de Congruity sur [sourceforge](https://sourceforge.net/projects/congruity/files/congruity/20/congruity-20.tar.bz2/download){:target="_blank"} ou cloner le repository. 
 - puis, une fois dans le répertoire principal, lancer l'installation :
 ```bash
 # Installation / mise à jour des setuptools
 pip install --upgrade setuptools
 # Intallation à proprement parlé de congruity
 python3 setup.py install
 ```

Et voilà c'est terminé, normalement à ce stade tout est installé et fonctionnel :

```bash
concordance --version
Concordance 1.4
Copyright 2007 Kevin Timmerman and Phil Dibowitz
This software is distributed under the GPLv3.

congruity --version
20
```

## :gear: Mise à jour de la télécommande
La partie configuration va se faire sur un site mis à disposition tout droit sorti des années 2000 :laughing:, mais il a le mérite de faire le boulot !
Je vous laisse le prendre en main, on y trouve vite ses petits, le site en question : [http://members.harmonyremote.com/EasyZapper/](http://members.harmonyremote.com/EasyZapper/).
Ensuite les interractions vont se dérouler toujours en suivant le cheminement suivant :
![architecture communication]({{ site.url }}{{ site.baseurl }}/assets/images/harmony-logitech/archi.png)

Donc on résume : 
 - toute la configuration de la télécommande s'effectue sur le site [http://members.harmonyremote.com/EasyZapper/](http://members.harmonyremote.com/EasyZapper/)
 - on lance la mise à jour de la télécommande : _Update My Remote_
 - un premier fichier est généré : _Connectivity.EZHex_
 - lancer la commande `congruity Connectivity.EZHex` qui a comme conséquence d'afficher l'interface graphique, dérouler (_forward_) jusqu'à la fin. L'objectif de cette étape est de valider que la communication avec votre télécommande se passe bien
 - si tout se passe bien le site se rafraîchit et propose la mise à jour de la télécommande en générant le deuxième fichier : _Update.EZHex_.
 - lancer la commande _congruity Update.EZHex_, de nouveau cela va afficher l'interface graphique permettant cette fois de flasher la télécommande. Dérouler tout jusqu'à la fin.

 :tada: La télécommande est mise à jour !

 En plus, pour moi c'est beaucoup plus rapide qu'avec l'application officielle logitech.

## En conclusion

Comme souvent, grâce à des passionnés de développement j'ai pu me sortir d'un problème technique de manière _assez_ simple (en tout cas c'est plus simple que d'installer un Windows :yum:).
Les projets ne vivent plus beaucoup mais, comme je l'ai indiqué, Logitech arrête la commercialistion des ses télécommandes donc il y a peu de chance qu'il y ait besoin de mises à jour.