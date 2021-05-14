---
title: "VS Code en mode remote avec SSH"
classes: wide
categories:
  - Articles
  - IDE
tags:
  - VSCode
  - SSH
---
Un tout petit article pour partager une configuration que j'utilise dans VS Code pour me simplifier la vie lors de l'utilisation de logiciels que je ne peux pas installer sur ma machine.
En effet, tous les jours au travail je dispose d'une machine Windows 10 qui a WSL 1 ([Windows Subsystem for Linux](https://docs.microsoft.com/fr-fr/windows/wsl/install-win10){:target="_blank"}).

Au final, il est assez classique que nos machines fournies par nos employeurs ne soient pas les dernières du moment ou celles qui nous feraient le plus plaisir :wink:.
C'est tout à fait compréhensible lorsque l'on doit gérer un parc de plusieurs milliers de machines : on utilise ce qui est le plus connus de tous et le plus pratique à administrer. Je ne rentre donc pas dans le débat : "mais pourquoi ne pas pouvoir choisir sa machine la plus adaptée" (même si de mon point de vue, espérer avoir un type de machine / OS pour tout le monde dans une société me parait utopique ! :thinking:).

Mon problème est le suivant : au quotidien j'utilise [Docker](https://www.docker.com/){:target="_blank"}, [Podman](https://podman.io/){:target="_blank"}, [Buildah](https://buildah.io/){:target="_blank"}, ... qui sont très bien pensés pour Linux et beaucoup moins pour Windows :laughing: !
De plus, ma version de Windows est bloquée sur la version 1 de WSL ce qui me prive aussi d'utiliser WSL 2 qui me simplifierait la tâche (même si ce n'est pas parfait).

## :bulb: La solution :bulb:
Au quotidien j'utilise [VS Code](https://code.visualstudio.com/){:target="_blank"} comme éditeur, hormis pour le Java ou Groovy où le je trouve moins performant qu'[Intellij IDEA](https://www.jetbrains.com/idea/){:target="_blank"} de Jetbrains.
VS Code a une galaxie d'[extensions](https://marketplace.visualstudio.com/){:target="_blank"} qui permettent d'enrichir de manière très importante l'outil.
Dans mon cas c'est l'extension [Remote Development](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.vscode-remote-extensionpack){:target="_blank"} qui va m'aider à solutionner mes problèmes d'OS.
C'est notamment la partie [Remote SSH](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-ssh){:target="_blank"} qui m'intéresse.

## :hammer_and_wrench: Configuration :hammer_and_wrench:

Alors la première chose est pourquoi cette extension ?

La documentation permet de le comprendre : ![architecture remote SSH VS Code](https://code.visualstudio.com/assets/docs/remote/ssh/architecture-ssh.png)
En résumé on va faire du bon vieux client / serveur :laughing: !
L'idée étant de profiter de VS Code sur ma machine Windows en ayant un terminal et des extensions (des plugins quoi) qui tournent sur un Linux distant et le tout échangeant des données en SSH.
Cela va donc me permettre d'utiliser un des serveurs de développement (en [REHL](https://www.redhat.com/en/technologies/linux-platforms/enterprise-linux){:target="_blank"} chez nous) à ma disposition qui a les outils que j'utilise, par exemple Podman.

L'installation est très simple et pour le coup il suffit de suivre la [documentation](https://code.visualstudio.com/docs/remote/ssh#_installation){:target="_blank"}.

Et voilà :tada: !

Notre VS Code a sa partie graphique qui s'exécute sur ma machine Windows mais le terminal et les extensions que j'utilise pour développer s'exécutent sur ma session Linux.
C'est plutôt très fluide et, pour mon utilisation qui est essentiellement la manipulation d'images avec Podman, cela rempli très bien le contrat !

## :warning: Espace disque :warning:
Par défaut la partie serveur s'installe dans `~/.vscode-server`, cela représente environ 150 Mo sans aucune extension !
Ensuite les différentes extensions serveur vont s'installer dans ce dossier et ça peut grossir très vite ...

Dans mon cas, la machine sur laquelle je me connecte ne permet pas d'avoir des _home_ utilisateurs ayant une grosse taille.
Il a donc fallut que je trouve le moyen de ne pas tout stocker dans le _home_ : en standard VS Code ne permet pas de choisir le chemin d'installation de la partie serveur de cette extension.

Il y a une [issue](https://github.com/microsoft/vscode-remote-release/issues/472){:target="_blank"} ouverte chez Microsoft mais pour l'instant elle n'est pas prise en compte :slightly_frowning_face:.
En la parcourant je suis tombé sur l'[astuce](https://github.com/microsoft/vscode-remote-release/issues/940#issuecomment-510232416){:target="_blank"} qui permet de contourner le problème : créer un lien symbolique vers un endroit qui a plus de place ! 

C'est tellement évident que j'ai honte de ne pas y avoir pensé tout seul ! :scream:

Pour ceux qui ont la flemme de cliquer sur les liens : 
```bash
mv ~/.vscode-server /my/big/disk/
ln -s /my/big/disk/.vscode-server ~/.vscode-server
```

:tada: Maintenant je peux tout installer sans avoir peur de saturer le _/home_ de mon serveur ! 

A moi les joies de la ligne de commande Podman !