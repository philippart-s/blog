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
![Logo Podman]({{ site.url }}{{ site.baseurl }}/assets/images/podman-macbook-m1/podman-logo-source.svg){: .align-center}

Récemment, j'ai eu la chance (?) d'avoir un Macbook M1 comme machine d'entreprise.
Très belle machine, véloce, silencieuse mais qui ne permet pas de faire du Docker nativement.  
Jusqu'à présent on pouvait utiliser [Docker Desktop](https://www.docker.com/products/docker-desktop){:target="_blank"} fournit par la société [Docker](https://www.docker.com/){:target="_blank"} mais depuis le changement du mode de licence il n'est plus possible de l'utiliser dans beaucoup d'entreprises (dont la mienne).  
C'est tout à fait normal car il faut bien vivre :wink: mais il faut bien que j'arrive à utiliser  mes images !
Du coup, il me restait la solution VM, mais pas mieux car [VirtualBox](https://www.virtualbox.org/){:target="_blank"} n'est pas compatible M1 et [Parallels Desktop](https://www.parallels.com){:target="_blank"} n'est pas gratuit.

Et cerise sur le gâteau je n'ai pas accès au sudo ...

Et là j'ai envie de dire:

![Image challenge accepted]({{ site.url }}{{ site.baseurl }}/assets/images/podman-macbook-m1/challenge-accepted.png){: .align-center}

Google est ton ami ...

## Pouvoir installer des outils en ligne de commande :computer:
Et là pas de mystère je n'ai pas dérogé à la règle de nombreux possesseurs de Mac : [Homebrew](https://brew.sh/index_fr){:target="_blank"}.
Ce package manager a le gros avantage d'être rootless :smile:.

## Installer Podman 🦭
[Podman](https://podman.io/){:target="_blank"}, fortement poussé par Red Hat, nous permet de refaire tout ce que l'on a l'habitude de faire avec Docker mais en rootless :tada:.  
Je ne m'étendrai pas sur son architecture mais sa principale différence avec Docker est qu'il est daemonless.

Il faut comprendre que l'on ne va pas installer Podman nativement mais des CLI qui vont permettent de dialoguer et piloter une machine virtuelle (grâce à [QEMU](https://www.qemu.org/){:target="_blank"}) avec une installation de Podman dans une Fedora.
En gros c'est l'équivalent de ce que l'on connaissait avec [Boot2Docker](https://github.com/boot2docker/boot2docker){:target="_blank"}.
Celui qui nous intéresse pour créer tout ce qu'il faut est [podman machine](https://docs.podman.io/en/latest/markdown/podman-machine.1.html){:target="_blank"}.

Pour utiliser Podman sur un Macbook M1 il faut passer pas mal de [commandes](https://github.com/containers/podman/blob/main/docs/tutorials/mac_experimental.md){:target="_blank"} plus ou moins obscures ...  
Mais voilà si on regarde bien on voit quelques _sudo_ trainer ... :cry:.

## Une petite formule à la rescousse ? :beers:
Je vous ai dit que j'aimais l'open source et surtout la magie que cela peu apporter dans certains cas ?
Je suis tombé sur le travail d'un certain [Hyeon Kim](https://github.com/simnalamburt){:target="_blank"} qui a l'air de maîtriser les Macbook M1, Podman et Homebrew 😲.  
Il propose un [fork de Podman](https://github.com/simnalamburt/podman){:target="_blank"} et une [formule Homebrew](https://github.com/simnalamburt/homebrew-x/blob/main/Formula/podman-apple-silicon.rb){:target="_blank"} basée dessus pour installer via Homebrew une version de Podman qui fonctionne avec le Macbook M1 et tout ça sans _sudo_ !

Alors ok, ce ne sont plus les repos officiels pour Podman, QEMU, ... mais au moins ça me permet d'avancer.
Je continuerai à vérifier la doc de Podman ou la formule de Podman dans Homebrew pour vérifier si ils fixent les petits problèmes pour basculer sur les repos officiels.

En tout cas pour l'instant cela simplifie grandement celle-ci : `brew install simnalamburt/x/podman-apple-silicon`.

Pour ma part j'ai eu des problèmes lors du lancement de la VM avec une erreur de la forme : 
```bash
➡️ podman machin start
INFO[0000] waiting for clients...                       
INFO[0000] listening tcp://0.0.0.0:7777                 
INFO[0000] new connection from  to /var/folders/f4/jlws_dq92sg9c3jxvh4pvvdw0000gn/T/podman/qemu_podman-machine-default.sock 
Waiting for VM ...
qemu-system-aarch64: -drive file=/opt/homebrew/share/qemu/edk2-aarch64-code.fd,if=pflash,format=raw,readonly=on: Could not open '/opt/homebrew/share/qemu/edk2-aarch64-code.fd': No such file or directory
Error: dial unix /var/folders/f4/jlws_dq92sg9c3jxvh4pvvdw0000gn/T/podman/podman-machine-default_ready.sock: connect: connection refused
ERRO[0003] cannot receive packets from , disconnecting: cannot read size from socket: EOF 
ERRO[0003] cannot read size from socket: EOF   
```
Ce qui m'a interpellé est le path: `/opt/homebrew/share/qemu/edk2-aarch64-code.fd`.
En effet, comme je n'ai pas les droits root j'ai installé le préfixe homebrew dans mon home.
En vérifiant dedans je retrouve bien le fichier souhaité.

Cela venait du fait que j'avais un chemin erroné dans le fichier de configuration `~/.config/containers/podman/machine/qemu/podman-machine-default.json`.
Il y avait en dur un chemin vers un prefix homebrew inexistant : `/opt/homebrew/share/` que j'ai remplacé par mon prefix à moi.

Et maintenant tout fonctionne : 
```bash
➡️ podman machine init                                                                                                                                                                     ✔  17:19:20  
Extracting compressed file

➡️ podman machine start                                                                                                                                                                    ✔  18:18:45  
INFO[0000] waiting for clients...                       
INFO[0000] listening tcp://0.0.0.0:7777                 
INFO[0000] new connection from  to /var/folders/f4/jlws_dq92sg9c3jxvh4pvvdw0000gn/T/podman/qemu_podman-machine-default.sock 
Waiting for VM ...

➡️ podman machine list                                                                                                                                                                     ✔  18:26:38  
NAME                     VM TYPE     CREATED             LAST UP
podman-machine-default*  qemu        About a minute ago  Currently running

➡️ podman run -it --rm docker.io/hello-world                                                                                                                                               ✔  18:26:42  
Trying to pull docker.io/library/hello-world:latest...
Getting image source signatures
Copying blob sha256:93288797bd35d114f2d788e5abf4fae518a5bd299647daf4ede47acc029d66c5
Copying blob sha256:93288797bd35d114f2d788e5abf4fae518a5bd299647daf4ede47acc029d66c5
Copying config sha256:18e5af7904737ba5ef7fbbd7d59de5ebe6c4437907bd7fc436bf9b3ef3149ea9
Writing manifest to image destination
Storing signatures

Hello from Docker!
This message shows that your installation appears to be working correctly.

To generate this message, Docker took the following steps:
 1. The Docker client contacted the Docker daemon.
 2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
    (arm64v8)
 3. The Docker daemon created a new container from that image which runs the
    executable that produces the output you are currently reading.
 4. The Docker daemon streamed that output to the Docker client, which sent it
    to your terminal.

To try something more ambitious, you can run an Ubuntu container with:
 $ docker run -it ubuntu bash

Share images, automate workflows, and more with a free Docker ID:
 https://hub.docker.com/

For more examples and ideas, visit:
 https://docs.docker.com/get-started/
 ```

⚠️ Cela reste un mode VM et donc cela prends pas mal de ressources, par défaut 1 CPU, 2 GO de RAM et 10 GO de disque.
Il est possible sur le `podman machine init` de positionner des valeurs plus ou moins hautes. ⚠️

## Conclusion 🧐

Et voilà, fin de cet article très court.
J'espère qu'il aura débloqué certain.e.s d'entre vous qui comme moi étaient bloqué.e.s sur l'installation de Podman sur leur beau Mabook M1.

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur l'[article](https://github.com/philippart-s/blog){:target="_blank"}. 
