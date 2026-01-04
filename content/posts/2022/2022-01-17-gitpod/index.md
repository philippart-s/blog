---
title: "Gitpod à la place d'Intellij ou de VSCode ?"
description: ""
link: /2022-01-17-gitpod
tags: 
  - Gitpod
  - VSCode
  - Intellij
author: wilda
---

Depuis maintenant deux ou trois ans, on entend de plus en plus parler d'IDE dans le cloud.
Je dois reconnaître que ce n'était pas quelque chose qui me faisait envie, le côté navigateur : si on rafraîchit la page, si son appuie sur précédent, ...
Bref je n'étais pas prêt et en toute honnêteté je ne trouvais pas les produits mûrs.

Mais comme on dit il n'y a que les imbéciles qui ne changent pas d'avis, et heureusement je commence à changer d'avis 😅.
A tel point que je me pose la question si je ne vais pas commencer à me passer de mon installation Intellij tellement choyée depuis plus de 15 ans !

# Les use cases 🎯

Les raisons pour lesquelles je me suis intéressé aux IDE dans le cloud sont multiples, plus ou moins utiles !

## La maturité des produits ✅
Que ce soit [Code Ready Workspaces](https://developers.redhat.com/products/codeready-workspaces/overview), [Code Spaces](https://github.com/features/codespaces) ou [Gitpod](https://gitpod.io/) force est de reconnaître que les produits sont de plus en plus aboutis.
On sent moins le côté navigateur et expérimental.

## La maturité de VSCode 👍
Je suis tombé, comme beaucoup, sous le charme de VSCode et je l'utilisais au quotidien comme éditeur de code / texte pour tout ce qui est autre que le Java.
En effet, je n'arrivais pas à retrouver la facilité / fluidité d'utilisation que j'avais dans Intellij.
Du coup, lorsque je testais les IDE dans le cloud je me retrouvais toujours frustré de ne pas pouvoir tester simplement un développement Java !

Mais tout a changé avec la release 1.0 de l'[extension VSCode Java](https://github.com/redhat-developer/vscode-java) fournie par Red Hat !

En effet, à partir de la 1.0.0, développer en Java avec VSCode était vraiment une expérience agréable, à tel point que j'utilise aussi VSCode pour développer en Java sur mon poste local.
En sachant que ces outils utilisent une version cloud de VSCode (basé sur [eclipse Theia](https://github.com/eclipse-theia/theia)) il n'en fallait pas plus pour enfin sauter le pas !

## L'utilisation de plusieurs machines 🖥️
Forcément, lorsque l'on parle de cloud cela permet d'envisager l'accès au code sur plusieurs machines sans avoir à réinstaller l'ensemble de la configuration à chaque fois.
Cela permet aussi d'envisager de travailler sur des machines plus exotiques comme un iPad par exemple.

L'autre bonus est de pouvoir bénéficier d'un environnement possédant des outils non disponibles ou non optimisés pour notre machine de tous les jours. 
A titre d'exemple ma machine professionnelle utilise Windows 10 mais je peux avoir besoin d'un Linux pour, par exemple, utiliser Docker sans virtualisation.

Enfin, lorsque l'on parle de machine on peut bénéficier d'une configuration beaucoup plus musclée que ce que l'on aurait eu en tant que machine physique (et nous évite ainsi de dépenser énormément d'argent !).

## Initialiser un environnement de développement 🛠️
Bien entendu cela permet d'éviter des actions fastidieuses de configuration pour :
 - la création d'une formation
 - l'on boarding d'une ou un développeuse / développeur
 - partager une configuration pour un projet open source 

## Parceque c'est cool 😎
Comme d'habitude je n'ai pas résisté à l'attrait de la nouveauté et du coup je me suis pris au jeu de tester cette nouvelle approche de développer.
Il faut aussi reconnaître qu'à force de voir des tutos ou talks de Philippe Charrière ou Horacio Gonzalez j'ai fini par craquer.

# Le choix du roi ? 👑
Je l'ai rapidement indiqué, mais à ce jour il y a plusieurs solutions disponibles.
Je vais expliquer pourquoi je suis parti sur Gitpod: 
 - Code Ready Workspace (Red Hat) nécessite un cluster Openshift, cela limite forcément l'utilisation perso 😅 ! 
 - Code Spaces (GitHub) était un bon candidat mais est devenu payant suite à l'ouverture de la beta et à ce jour je ne crois pas qu'il soit ouvert à titre personnel mais simplement accessible lorsque l'on a une licence business.

Du coup il restait Gitpod, qui en plus est vraiment cool en termes de fonctionnalités et offre 50h d'utilisation par mois gratuitement !
C'est largement suffisant pour ce que j'en fais.
Je vous laisse aller voir les [prix](https://www.gitpod.io/pricing/) qui sont tout à fait raisonnables.

On va y venir dans les sections suivantes mais l'autre chose très sympathique avec Gitpod est qu'il est énormément customisable et ça pour tout geek qui se respecte ça n'a pas de prix.
Les autres le sont aussi certainement mais ne les ayant pas utilisé je ne peux pas l'affirmer.

## Installation de Gitpod 🚫
Eh bien rien à faire, c'est le but hein !
Juste se connecter sur [https://gitpod.io](https://gitpod.io) et créer un compte ou utiliser un de vos comptes GitHub, GitLab, BitBucket.

## ConfigurationS
Gitpod est donc un IDE basé sur Eclipse Theia sous forme d'image Docker.
L'image par [défaut](https://github.com/gitpod-io/workspace-images/blob/master/full/Dockerfile) est très complète et permet déjà de faire pas mal de choses : python, java, node, go, ...
Si c'est largement suffisant pour une utilisation rapide sans trop se poser de questions nous allons voir que nous pouvons un peu (beaucoup 😉) améliorer _l'expérience_ développeur.

### Configuration du workspace avec .gitpod.yml ⚙️
Gitpod vient avec la notion de workspaces, qui représente en gros une instance de Gitpod sur un repository.
Gitpod gère le cycle de vie de ces workspaces de manière assez transparente et permet de rouvrir un workspace fermé pendant 14 jours (sur la version gratuite) avant de le supprimer.

On peut avoir besoin de configurer de manière spécifique un projet et donc son workspace associé, pour cela tout va se passer dans le fichier `.gitpod.yml` à placer à la racine du projet.
On peut y configurer de nombreuses choses, comme des actions d'initialisations, des extensions VSCode, exposer des ports, ... 
Je vous laisse aller voir la [documentation](https://www.gitpod.io/docs/references/gitpod-yml) qui explique l'ensemble des configurations possibles.

Voici un exemple d'initialisation que l'on peut faire, dans mon cas je récupère les différentes dépendances pour Jekyll :
```yml
tasks:
  - name: Init
    init: bundle install
```
On peut aussi ajouter des actions à effectuer avant l'ouverture du workspace :
```yml
tasks:
  - name: Init
    before: printf "\n[settings]\napi_key = $WAKATIME_API_KEY\n" > ~/.wakatime.cfg
```
Ici l'exemple de la configuration de [wakatime](https://wakatime.com/) en récupérant mon API Key positionnée dans la [configuration de mon workspace](https://www.gitpod.io/docs/environment-variables#using-the-account-settings) dans Gitpod.

Il est aussi possible d'indiquer les extensions que l'on souhaite activer par défaut dans notre VSCode : 
```yml
vscode:
  extensions:
    - streetsidesoftware.code-spell-checker-french
    - streetsidesoftware.code-spell-checker
    - github.vscode-pull-request-github
    - eamodio.gitlens
```

Avec ces trois exemples on voit que l'on peut paramétrer des éléments techniques du projet à faire avant chaque accès au code, des éléments de configuration techniques ou propres à VSCode.

Pour éviter de dupliquer le `.gitpod.yml` dans tous les projets et de devoir le maintenir à la main c'est là que l'on va commencer à regarder du côté de l'image customisable !

### Configuration du workspace avec une image Docker 🐳

Comme indiqué précédemment Gitpod, se base sur une image qui initialise tout un écosystème avec les bons outils. 
Pour l'exemple je vais prendre l'image [full](https://github.com/gitpod-io/workspace-images/blob/master/full/Dockerfile) mais je pense qu'il serait plus sage et optimal (niveau performances) d'utiliser l'image de [base](https://github.com/gitpod-io/workspace-images/blob/master/base/Dockerfile).
Il y a deux moyens d'indiquer quelle image prendre à la création du workspace : 

 - positionner un fichier `.gitpod.Dockerfile` à la racine du projet puis indiquer dans le `.gitpod.yml` que c'est cette image qui sera utilisée : 
```yml
image:
  file: .gitpod.Dockerfile
```
 - créer une image, la publier sur une registry et la référencer dans le `.gitpod.yml` : 
```yml
image: wildagsx/gitpod:latest
```

A titre d'exemple voici une image permettant l'utilisation de [Jekyll](https://jekyllrb.com/) :
 - configuration de Wakatime
 - installation du bundler et de Jekyll
 - installation de [Oh My Zsh](https://ohmyz.sh/) (primordial !) et de ses thèmes / plugins

Cela donne un Dockerfile assez simple : 
```dockerfile
FROM gitpod/workspace-full

USER gitpod

# Wakatime config
ARG WAKATIME_API_KEY
ENV ZSH=/home/gitpod/.oh-my-zsh

# Set configuration for wakatime
RUN printf "\n[settings]\napi_key = $WAKATIME_API_KEY\n" > ~/.wakatime.cfg \
    # Install Jekyll
    && bash -lc "gem install bundler jekyll" \
    && sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" \
    # Powerlevel10k theme
    && git clone https://github.com/romkatv/powerlevel10k.git $ZSH/custom/themes/powerlevel10k --depth=1 \
    # zsh-autosuggestions plugin
    git clone https://github.com/zsh-users/zsh-autosuggestions $ZSH/custom/plugins/zsh-autosuggestions

# ADD zsh & oh my zsh config file
ADD .zshrc .
# ADD Powerlevel10k theme
ADD .p10k.zsh .
```

Pour le tester c'est assez simple : `docker build -f .gitpod.Dockerfile -t wilda/gitpod .` & `docker run -it wilda/gitpod zsh`
```zsh
philipparts-gitpodimages-d3pjrj224nx% docker run -it wilda/gitpod zsh
[powerlevel10k] fetching gitstatusd .. [ok]                                                                                                                       
~                                        with gitpod@5af5c0c59dfa at 16:32:39
❯
```
Plutôt cool.

Maintenant pour pouvoir utiliser cette image n'importe où : `docker push wilda/gitpod`

Ensuite, on l'a déjà vu, son utilisation va être assez simple: il suffit d'avoir un `.gitpod.yml` de la forme :
```yaml
image: wilda/gitpod
```

Du coup, lorsque j'ouvre mon projet dans Gitpod avec ce paramétrage dans le `.gitpod.yaml` j'ai ce genre configuration d'activée : 
```zsh
❯ jekyll -version
jekyll 4.2.1
/workspace/gitpod-images on main                             Ruby 2.7.4 at 16:55:52
❯  
```

Pour plus de documentation sur comment customiser les images : [documentation GitPod](https://www.gitpod.io/docs/config-docker#custom-docker-image)

### Configuration de VSCode 🛠️
A ce stade on a configuré une image qui nous convient.
Il reste à configurer notre VSCode et, notamment, les extensions.
Il y a deux moyens : via [Gitpod](https://www.gitpod.io/docs/vscode-extensions/) ou via la [synchronisation](https://code.visualstudio.com/docs/editor/settings-sync) de VSCode.

Personnellement j'utilise la synchronisation via VSCode mais la solution fournie par Gitpod est intéressante car elle permet de choisir si c'est partagé par tous (propre au projet), pour tous les workspaces de l'utilisateur ou pour un workspace en particulier.
Je vous laisse aller voir la documentation pour plus de détails.
Je vais juste faire un zoom sur un exemple dans un `.gitpod.yml`:
```yaml
image: wilda/gitpod 

vscode:
  extensions:
    - vscjava.vscode-maven
```
Dans cet exemple le projet installera automatiquement l'extension _vscode-maven_ dans le cas où on voudrait être sûr de l'avoir si on est sur un projet Java.
Pour savoir quel est le nom de l'extension à mettre dans le fichier `.gitpod.yml` c'est assez simple car VSCode nous le propose directement sur l'extension (roue crantée, ⚙️, puis _Add to .gitpod.yml_).

## Pour aller plus loin 🔬
Comme je vous l'ai indiqué, pour des raisons de simplicité, j'ai utilisé l'image `gitpod/workspace-full` dans cet article mais il est plutôt conseillé de partir sur l'image `gitpod/workspace-base` pour vraiment créer des images optimisées.
C'est ce que j'ai fait dans ce [repository](https://github.com/philippart-s/gitpod-images) où j'ai ma propre [base image](https://github.com/philippart-s/gitpod-images/tree/main/default-image) (basée sur _gitpod/workspace-base_) qui me permet de construire d'autres images dédiées par technos.
N'hésitez pas à aller jeter un oeil et si vous voyez des coquilles n'hésitez pas !

Je vous conseille aussi les ressources suivantes : 
 - l'excellent [talk](https://youtu.be/868Gq7QUdME) d'Horacio Gonzalez et Philippe Charrière
 - de jeter un oeil sur le [compte GitLab](https://gitlab.com/k33g_org/k33g_org.gitlab.io/-/issues) de Philippe Charrière
 - la série de [CALMS](https://www.youtube.com/c/ThierryChantier/search?query=gitpod) de Thierry Chantier consacrés à Gitpod

## Conclusion 🧐

J'espère vous avoir donné envie de jeter un oeil à ce type d'IDE.
J'étais le premier septique mais j'avoue que j'ai vraiment été bluffé par la facilité d'utilisation et de customisation.
Je n'en suis pas à mettre à la poubelle tous mes IDE mais j'avoue que pour plein de situations je passe de plus en plus par Gitpod !

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur l'[article](https://github.com/philippart-s/blog).