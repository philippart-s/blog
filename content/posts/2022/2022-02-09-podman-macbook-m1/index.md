---
title: "Utiliser Podman sur un Macbook M1"
description: "Toi aussi utilise Podman avec ton beau Macbook M1"
link: /2022-02-09-podman-macbook-m1
tags: 
  - Macbook
  - Podman
  - Docker
image: podman-logo-source.svg
figCaption: "@wildagsx"
author: wilda
---

Récemment, j'ai eu la chance (?) d'avoir un Macbook M1 comme machine d'entreprise.
Très belle machine, véloce, silencieuse mais qui ne permet pas de faire du Docker nativement.  
Jusqu'à présent on pouvait utiliser [Docker Desktop](https://www.docker.com/products/docker-desktop) fournit par la société [Docker](https://www.docker.com/) mais depuis le changement du mode de licence il n'est plus possible de l'utiliser dans beaucoup d'entreprises (dont la mienne).  
C'est tout à fait normal car il faut bien vivre :wink: mais il faut bien que j'arrive à utiliser  mes images !

Du coup, il me restait la solution VM, mais là encore ça se complique un petit peu pour plusieurs raisons : 
 - je ne me voyais pas faire une VM où je me connecte pour faire du Docker mais plus quelque chose comme [Boot2Docker](https://github.com/boot2docker/boot2docker) et [docker machine](https://github.com/docker/machine) permettant de "dialoguer" avec la VM depuis mon Mac en utilisant une CLI (et pas tout faire dans la VM),
 - justement Boot2Docker et docker machine sont dépréciés et donc plus mis à jour par Docker (au profit de Docker Desktop),
 - [VirtualBox](https://www.virtualbox.org/) n'est pas compatible M1 et [Parallels Desktop](https://www.parallels.com) n'est pas gratuit.

Et cerise sur le gâteau je n'ai pas accès au sudo ...

Et là j'ai envie de dire:

![Image challenge accepted]({{ site.url }}{{ 
site.baseurl }}/assets/images/podman-macbook-m1/challenge-accepted.png)

Google est ton ami ...

## Pouvoir installer des outils en ligne de commande :computer:
Et là pas de mystère je n'ai pas dérogé à la règle de nombreux possesseurs de Mac : [Homebrew](https://brew.sh/index_fr).
Ce package manager a le gros avantage d'être rootless :smile:.

## Installer Podman 🦭
[Podman](https://podman.io/), fortement poussé par Red Hat, nous permet de refaire tout ce que l'on a l'habitude de faire avec Docker mais en rootless :tada:.  
Je ne m'étendrai pas sur son architecture mais sa principale différence avec Docker est qu'il est daemonless.

Il faut comprendre que l'on ne va pas installer Podman nativement mais des CLI qui vont permettent de dialoguer et piloter une machine virtuelle (grâce à [QEMU](https://www.qemu.org/)) avec une installation de Podman dans une Fedora.
En gros c'est l'équivalent de ce que l'on connaissait avec Boot2Docker.
Celui qui nous intéresse pour créer tout ce qu'il faut est [podman machine](https://docs.podman.io/en/latest/markdown/podman-machine.1.html).

A noter que la documentation Podman indique pas mal de [commandes](https://github.com/containers/podman/blob/main/docs/tutorials/mac_experimental.md) à passer en plus du simple `brew install podman` pour des problèmes avec QUEMU.
Il apparaît que ce n'est plus nécessaire mais simplement la [documentation qui n'est pas à jour](https://github.com/containers/podman/issues/13010#issuecomment-1022347137), heureusement car il y avait besoin de sudo ...  
Ouf !

Une fois l'installation de Podman effectuée il ne reste plus qu'à le tester:

```bash
➡️ podman machine init                                                                                                                                                                     ✔  17:19:20  
Extracting compressed file

➡️ podman machine start
Error: unable to start host networking: "could not find \"gvproxy\" in one of [/usr/local/opt/podman/libexec /opt/homebrew/bin /opt/homebrew/opt/podman/libexec /usr/local/bin /opt/homebrew/Cellar/podman/3.4.4/libexec /usr/local/lib/podman /usr/libexec/podman /usr/lib/podman]"
```

Ouch fausse joie.

## Petit problème de hardcoding ✏️

Si on regarde de plus près l'erreur, cela semble simplement un problème d'un fichier non présent ou non trouvé (_gvproxy_).
En effet, dans mon cas, n'ayant pas _sudo_, j'ai installé Homebrew dans mon home et ce fameux fichier n'est donc pas présent là ou il faut, pour moi il est ici: `~/homebrew/Cellar/podman/3.4.4/libexec`
En creusant un peu on se rends compte que c'est une [partie de ping pong](https://github.com/containers/podman/issues/12161) entre les équipes de Podman et Homebrew pour savoir qui doit rendre le chemin plus permissif ou intégrer des paths qui vont bien.

## Le fichier containers.conf à la rescousse ⚙️

Podman, comme d'autres, se base sur un fichier de configuration qui se nomme _containers.conf_ et se trouve dans `~/.config/containers`.
Il permet, notamment, de positionner quelques paths.
Il m'a suffit de simplement rajouter (grâce à cette [information](https://github.com/containers/podman/issues/11960#issuecomment-953672023) trouvée dans GitHub) le path vers mon installation de _gvproxy_:
```conf
[containers]
  log_size_max = -1
  pids_limit = 2048
  userns_size = 65536

[engine]
  helper_binaries_dir=["/Users/myuser/homebrew/Cellar/podman/3.4.4/libexec"]
  image_parallel_copies = 0
  num_locks = 2048
  active_service = "podman-machine-default"
  # ... Reste du fichier  
```

## Le fichier podman-machine-default.json à la rescousse ⚙️

J'en n'en avais pas fini avec les problème de chemins en durs ...
Après la résolution de mon problème de _gvproxy_ me voilà encore avec un problème de chemin de fichier non trouvé:

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
Souvenez-vous comme je n'ai pas les droits root j'ai installé le préfixe homebrew dans mon home.
En vérifiant dedans (`~/homebrew/share/qemu/`) je retrouve bien le fichier souhaité.

Cela venait du fait que j'avais un chemin erroné dans le fichier de configuration `~/.config/containers/podman/machine/qemu/podman-machine-default.json`.
Il y avait en dur un chemin vers un prefix homebrew inexistant : `/opt/homebrew/share/` que j'ai remplacé par mon prefix à moi (`/Users/myuser/homebrew/share`).

```conf
 # ...
  "-cpu",
  "cortex-a57",
  "-M",
  "virt,highmem=off",
  "-drive",
  "file=/opt/homebrew/share/qemu/edk2-aarch64-code.fd,if=pflash,format=raw,readonly=on",
  "-drive",
# ...
```
Est devenu:
```conf
 # ...
  "-cpu",
  "cortex-a57",
  "-M",
  "virt,highmem=off",
  "-drive",
  "file=/Users/myuser/homebrew/share/qemu/edk2-aarch64-code.fd,if=pflash,format=raw,readonly=on",
  "-drive",
# ...
```

Et maintenant tout fonctionne 🎉 : 
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

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur l'[article](https://github.com/philippart-s/blog).