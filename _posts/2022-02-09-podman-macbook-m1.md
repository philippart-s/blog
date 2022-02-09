---
title: "Utiliser Podman sur un Macbook M1"
classes: wide
excerpt: "Toi aussi utilise Podman avec ton beau Macbook M1"
categories:
  - Articles
  - Configuration
  - Containers
  
tags:
  - Macbook
  - Podman

---
TODO image Podman

Récemment j'ai eu la chance (?) d'avoir un Macbook M1 comme machine d'entreprise.
Très belle machine, veloce, silencieuse mais qui ne permet pas de faire du Docker nativement.
Jusqu'à présent on pouvait utiliser [Docker Desktop](https://www.docker.com/products/docker-desktop){:target="_blank"} fournit par la société [Docker](https://www.docker.com/){:target="_blank"} mais depuis le changement du mode de licence il n'est plus possible de l'utiliser dans beaucoup d'entreprises (dont la mienne).
C'est tout à fait normal car il faut bien vivre :wink: mais il faut bien que j'arrive à utiliser  mes images !
Du coup il me restait la solution VM mais pas mieux car [VirtualBox](https://www.virtualbox.org/){:target="_blank"} n'est pas compatible M1 et [Parallels Desktop](https://www.parallels.com){:target="_blank"} n'est pas gratuit.
Et cerise sur le gateau je n'ai pas accès au sudo ...

Et là j'ai envie de dire:

TODO image challenge accepted

Google est ton ami ...

## Pouvoir installer des outils en ligne de commande
Et là pas de mystère je n'ai pas déroger à la règle de nombreux processeurs de Mac : [Homebrew](https://brew.sh/index_fr){:target="_blank"}.
Ce package manager a le gros avantage d'être rootless :smile:.

## Installer Podman
[Podman](https://podman.io/){:target="_blank"}, fortement poussé par Red Hat, nous permet de refaire tout ce que l'on a l'habitude de faire avec Docker mais en rootless :tada:.  
Je ne m'étendrai pas sur son architecture mais sa principale différence avec Docker est qu'il est daemonless.

Il faut comprendre que l'on ne va pas installer Podman nativement mais des CLI qui vont permettent de dialoguer et piloter une machine virtuelle avec une installation de Podman dans une Fedora.
En gros c'est l'équivalent de ce que l'on connaissait avec [Boot2Docker](https://github.com/boot2docker/boot2docker){:target="_blank"}.
Celui qui nous intéresse pour créer tout ce qu'il faut [podman machine](https://docs.podman.io/en/latest/markdown/podman-machine.1.html){:target="_blank"}.

Pour utiliser Podman sur un Macbook M1 il faut suivre pas mal de [commande](https://github.com/containers/podman/blob/main/docs/tutorials/mac_experimental.md){:target="_blank"} plus ou moins obscures ...
Mais voilà si on regarde bien on voit quelques _sudo_ trainer ... :cry:.

## Une petite formule à la rescousse ?
Je vous ai dit que j'aimai l'open source et surtout la magie que cela peu apporter dans certains cas ?
Un certain [Hyeon Kim](https://github.com/simnalamburt){:target="_blank"} qui a l'air de maîtriser les Macbook M1, Podman et Homebrew propose un [fork de Podman](https://github.com/simnalamburt/podman){:target="_blank"} et une [formule Homebrew](https://github.com/simnalamburt/homebrew-x/blob/main/Formula/podman-apple-silicon.rb){:target="_blank"} basée dessus pour installer via Homebrew une version de Podman qui fonctionne avec le Macbook M1 et tout ça sans _sudo_ !

Alors ok ce ne sont plus les repos officiels pour Podman, Qemu, ... mais au moins ça me permet d'avancer.
Je continuerai à vérifier la doc de Podman ou la formule de Podman dans Homebrew pour vérifier si ils fixent les petits problèmes pour basculer sur les repos officiels.

En tout cas pour l'instant cela simplifie grandement l'installation : `brew install simnalamburt/x/podman-apple-silicon`.

Pour ma part j'ai eu des problèmes lors du lancement de la VM avec une erreur de la forme : TODO message erreur

Cela venait du fait que j'avais un chemin erroné dans le fichier de configuration `~/.config/containers/podman/machine/qemu/podman-machine-default.json`.
En effet, il y avait en dur un chemin vers un prefix hombrew inexistant : `/opt/homebrew/share/` que j'ai remplacé par mon prefix à moi.

Et maintenant tout fonctionne : 
```
podman machine init
podman machine start
podman helloworld
```
TODO ajouter les vraies sorties


## Conclusion 🧐

Et voilà, fin de cet article très court.
J'espère qu'il aura débloqué certain.e.s d'entre vous qui comme moi étaient bloqué.e.s sur l'installation de Podman sur leur beau Mabook M1.

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur l'[article](https://github.com/philippart-s/blog){:target="_blank"}. 
