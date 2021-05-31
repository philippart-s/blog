---
title: ":frog: Promotion d'image Docker dans Artifactory :whale:"
classes: wide
categories:
  - Articles
  - Dev
tags:
  - Artifactory
  - Docker
  - Promotion
---
Lorsque l'on utilise Docker (ou un de ses variants) nous manipulons des images et dans certains cas en créons.
Dans les deux cas lorsque l'on sort du POC effectué localement sur la machine vient se poser le problème du stockage des images et de leur partage avec d'autres utilisateurs.

## Faisons les présentations :handshake:

Naturellement en gratuit et simple il y a le [Docker Hub](https://hub.docker.com/){:target="_blank"}.
C'est vrai que cela fonctionne mais là encore, suite aux derniers changements sur les [conditions d'utilisations](https://www.docker.com/blog/rate-limiting-questions-we-have-answers/), cela ne va pas aller beaucoup plus loin que le POC si on ne prend pas une version entreprise.
Je ne vais pas parler de la version entreprise (je vous laisse vous faire votre propre idée) mais d'une solution qui permet d'avoir on-premise (c'est à dire utilisant votre propre infrastructure) ou dans le cloud une registry Docker : [Artifactory](https://jfrog.com/artifactory/){:target="_blank"} de chez [JFrog](https://jfrog.com/){:target="_blank"}.

Il existe deux produits pour avoir une registry Docker : 
 - [JFrog Container Registry](https://jfrog.com/container-registry/){:target="_blank"}
 - [Artifacory](https://jfrog.com/artifactory/){:target="_blank"}

JFrog Container Registry ne fournit comme service que d'être une registry Docker et Helm alors qu'Artifactory permet en plus d'être un repository d'artefacts divers et variés, on citera les deux plus célèbres que sont [Npm](https://www.npmjs.com/){:target="_blank"} et [Maven](https://maven.apache.org/){:target="_blank"}.

Je ne m’étendrai pas sur l'installation d'Artifactory, ce n'est pas le but de ce post.
Pour illustrer mes propos je vais utiliser une instance dans le cloud offerte par JFrog, les actions / manipulations sont les mêmes que si c'était on-premise.

> Disclaimer :loudspeaker:
>
> Je ne travaille pas pour JFrog, je trouve juste leurs outils pratiques et il se trouve que je les utilise depuis plusieurs années dans mon boulot.
> L'idée est de partager ce que je connais pour faciliter la vie des utilisateurs Artifactory et non de dire que c'est le meilleur produit du monde !

## Rapide tour d'horizon du propriétaire :house:
Avant toute chose, si tu es ici c'est que tu ne l'as pas trouvé, mais la doc JFrog est très bien faite alors dans ma grande bonté je te mets le lien :wink: : [https://www.jfrog.com/confluence/display/JFROG/JFrog+Artifactory](https://www.jfrog.com/confluence/display/JFROG/JFrog+Artifactory){:target="_blank"}.


Une fois créée l'instance sur Google Cloud Platform via l'option permettant de [créer gratuitement](https://jfrog.com/start-free/){:target="_blank"} une instance Artifactory j'ai mon instance qui est prête :
![accueil  artifactory]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/accueilArti.png)

L'instance Artifactory vient avec des repositories pré-configurés, dont 1 Docker, cela tombe bien :wink:.
![default artifactory repositories]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/artiDefaultRepo.png)

Pour la fin de la démo on va créer un autre repository Docker il nous servira plus tard :
![repository docker supplémentaire]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/StefDockerRepo.png)
Et comme ils sont sympas chez JFrog, ils fournissent les commandes pour utiliser notre registry nouvellement créée : 
![set me up artifactory 1/2]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/setMeUp1.png)
![set me up artifactory 2/2]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/setMeUp2.png)

## Commençons par push une image :whale:
Avant de parler de promotion, nous allons donc uploader une image dans notre registry.
Je ne vais pas utilser la fonctionnalité de proxy qui me permettrait de configurer Artifactory comme un proxy en frontal de Docker Hub.
C'est un use case pratique, notamment pour le problème de quotas précédemment cité, j'en ferai peut être un article :wink:.

Donc, commençons par récupérer une image simple :
```bash
docker pull hello-world
sing default tag: latest
latest: Pulling from library/hello-world
b8dfde127a29: Pull complete 
Digest: sha256:5122f6204b6a3596e048758cabba3c46b1c937a46b5be6225b835d091b90e46c
Status: Downloaded newer image for hello-world:latest
docker.io/library/hello-world:latest
```
Et vérifions que notre image est bien en local sur le poste : 
```bash
docker images
REPOSITORY                     TAG                 IMAGE ID            CREATED             SIZE
hello-world                                          latest              d1165f221234        2 months ago        13.3kB
```

Avec les images pour les uploader dans une registry autre que le Docker Hub il faut les _tagger_ avec le nom de la registry.
```bash
docker tag hello-world xxxx.jfrog.io/default-docker-local/hello-world:1.0.0
```
Ici on retrouve : 
 - le nom de mon instance artifactory : _xxxx.jfrog.io/_
 - le nom du repository Docker qui représente ma registry : _default-docker-local_
 - enfin le nom et la version souhaitée : _hello-world:1.0.0_

On a bien nos deux images :
```bash
hello-world                                          latest              d1165f221234        2 months ago        13.3kB
xxxx.jfrog.io/default-docker-local/hello-world   1.0.0               d1165f221234        2 months ago        13.3kB
```

Ensuite on va uploader l'image avec _push_, pour cela il faut le faire identifier :
```bash
docker login xxxx.jfrog.io
Username: xxxx@gmail.com
Password: 
WARNING! Your password will be stored unencrypted in /root/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
```
Puis on push l'image :
```bash
sudo docker push xxxx.jfrog.io/default-docker-local/hello-world:1.0.0
The push refers to repository [xxxx.jfrog.io/default-docker-local/hello-world]
f22b99068db9: Pushed 
1.0.0: digest: sha256:1b26826f602946860c279fce658f31050cff2c596583af237d971f4629b57792 size: 525
```
Et voilà ! :tada:
![image hello world dans repo Docker local]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/dockerImgLocalRepoArti.png)

## Et la promotion alors ? :label:
Bon, jusqu'à présent on a une registry qui héberge nos images que l'on construit, c'est bien mais c'est un peu fragile.
J'aimerai avoir une registry qui stocke mes images en cours de réalisation et donc potentiellement instables et une autre qui stocke mes images à destination de ma production.
De plus, pour garder le fait qu'une image soit immuable une fois construite, je veux pouvoir les _promouvoir_ d'une registry à l'autre.

Derrière le terme de promotion se cache le fait de déplacer ou copier une image d'une registry à l'autre sans la reconstruire et donc garantir que c'est bien la même image.
En réalité, Artifactory permet ce mécanisme sur n'importe quel type d'artefact qu'il stocke (npm, maven ...).

Artifactory permet d'effectuer les promotions via des appels de leur [API](https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API){:target="_blank"} mais aussi via leur [CLI](https://www.jfrog.com/confluence/display/CLI/CLI+for+JFrog+Artifactory){:target="_blank"} (qui en fait appelle l'API :wink:).

Dans les deux cas les informations à apporter à la promotion sont : 
 - la registry de départ
 - la registry d'arrivée
 - l'image de départ et son tag
 - l'image d'arrivée et son tag
 - si on copie ou déplace l'image


### Promotion avec l'API en REST :link:
Au final c'est assez simple, on fournit un token et les informations nécessaires et le tour est joué.
```bash
curl -X POST -H "X-JFrog-Art-Api:$ARTI_API_KEY" \
          -H "Content-Type: application/json" \
          -d '{"targetRepo" : "stef-docker-local",  
               "dockerRepository" : "hello-world",
               "targetDockerRepository" : "hello-world", 
               "tag" : "1.0.0",
               "targetTag" : "prod",
               "copy": false    }' \
           https://xxxx.jfrog.io/artifactory/api/docker/default-docker-local/v2/promote

Promotion ended successfully%
```
> ARTI_API_KEY est la clef représentant le couple login / password associé à mon compte, plus d'informations [ici](https://www.jfrog.com/confluence/display/JFROG/User+Profile#UserProfile-APIKey){:target="_blank"}.

Une fois la commande passée notre image a bien été promue dans la registry _stef-docker-local_ avec le nouveau tag _prod_ et n'est plus présente dans la registry de départ.
![image promue dans la registry prod]({{ site.url }}{{ site.baseurl }}/assets/images/docker-promote-arti/dockerAfterPromote.png)

### Promotion avec le CLI
Le CLI de JFrog n'est ni plus ni moins qu'un appel de l'API mais packagé dans un programme GO.
Cela peut paraître plus simple à utiliser mais il faut installer et configurer un binnaire là où l'API est utilisable dans n'importe quelles conditions.
Le CLI Artifactory est disponible sur le [site](https://www.jfrog.com/confluence/display/CLI/JFrog+CLI){:target="_blank"} de JFrog.
Une fois installé et configuré (la configuration se passe lors du lancement de la première commande), il suffit de lancer : 
```bash
./jfrog rt docker-promote hello-world default-docker-local stef-docker-local \
                          --source-tag=1.0.0 \
                          --target-docker-image=hello-world \
                          --target-tag=prod

To avoid this message in the future, set the JFROG_CLI_OFFER_CONFIG environment variable to false.
The CLI commands require the URL and authentication details
Configuring JFrog CLI with these parameters now will save you having to include them as command options.
You can also configure these parameters later using the 'jfrog c' command.
Configure now? (y/n) [n]? y
Server ID: xxxx-arti
JFrog platform URL: https://xxxx.jfrog.io/
JFrog access token (Leave blank for username and password/API key): 
JFrog username: xxxx@xxx.com
JFrog password or API key: 
Is the Artifactory reverse proxy configured to accept a client certificate? (y/n) [n]? n

[Info] Promoted image hello-world to: stef-docker-local repository.
```
Certes c'est plus simple mais c'est moins drôle :wink:
