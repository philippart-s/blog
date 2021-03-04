---
title: "Utiliser Netlify comme environnement de staging pour un site Jekyll"
#excerpt: 
classes: wide
categories:
  - Articles
  - CI-CD
tags:
  - GitHub
  - Netlify
  - Jekyll
---
Cela fait quelque temps que je cherche un moyen d'avoir un environnement de staging pour mes sites [Jekyll](https://jekyllrb.com/){:target="_blank"} qui sont déployés sur *GitHub Pages*.
En effet, lorsque l'on est plusieurs à contribuer c'est plutôt pratique de pouvoir voir ce que cela donne pour relire avant de le faire sur la prod !
La version *prod* est déployée sur [GitHub Pages](https://pages.github.com/){:target="_blank"}, mais comme cela ne permet pas d'avoir un aperçu de ce que donne une PR / branche (visiblement cela va bientôt arriver mais je ne suis pas patient :wink:) je me suis mis à la recherche d'une solution automatisée pour faire ça !

Ma première version d'environnement de staging reposait sur un [bucket S3](https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html){:target="_blank"} à travers l'offre [cellar](https://www.clever-cloud.com/doc/deploy/addon/cellar/){:target="_blank"} de [Clever Cloud](https://www.clever-cloud.com/){:target="_blank"} : voir l'article [Déployer un site statique sur un bucket S3 avec GitHub Actions]({{ site.baseurl }}{% post_url 2020-12-30-Cellar-Actions %}){:target="_blank"}.
Si cela fonctionne il manque un élément important : la possibilité d'activer la fonctionnalité *[website](https://docs.aws.amazon.com/AmazonS3/latest/userguide/EnableWebsiteHosting.html){:target="_blank"}* qui permet une navigation plus fluide comme si c'était déployé sur un serveur HTTP.
Par exemple, si on n'active pas cette fonctionnalité, il faut spécifier une page html à chaque requête (ce qui n'est pas le cas avec un site Jekyll) sinon on obtient une erreur du genre :
```xml
<Error>
  <Code>NoSuchKey</Code>
  <BucketName>aws-clever</BucketName>
  <RequestId>tx00000000000000fb77132-00603f52e4-a4bc63c-default</RequestId>
  <HostId>a4bc63c-default-default</HostId>
</Error>
```
Après avoir contacté le support de Clever cette fonctionnalité n'est pas encore disponible avec l'addon *cellar* mais devrait arriver dans les semaines qui viennent mais comme pour GitHub pages je ne suis pas patient et cela me fait une excuse pour tester un nouveau truc !

## Ce que je veux faire :bulb:
Pour mémoire mon workflow est assez simple:
1. Si une PR est créée je veux créer un nouveau site de staging avec la version statique du site Jekyll de la branche de la PR déployée
1. A chaque push le site est à jour
1. Sur la fermeture de la PR la branche est détruite et à la destruction de la branche le site est supprimé

## Ce que je vais utiliser :factory:
Pour la création du site statique : [Jekyll](https://jekyllrb.com/){:target="_blank"}.

Pour l'hébergement de la prod : [GitHub Pages](https://pages.github.com/){:target="_blank"}.

Pour l’hébergement des environnements de staging : [Netlify](https://www.netlify.com/){:target="_blank"}.

Enfin, pour l'automatisation : [GitHub Actions](https://github.com/features/actions){:target="_blank"}.

Plutôt sympa comme stack :sunglasses: !

## Création de l'environnement de staging dans Netlify :gear:

C'est peut être bête mais je n'ai pas trouvé le moyen de créer un site *vide* dans l'interface de Netlify: il faut soit donner une URL GitHub, GitLab ou BitBucket, soit déposer un répertoire contenant un site statique.
Cela s'explique certainement qu'à l'origine un site sous Netlify est fait pour être *mis en production* (accédé depuis l'extérieur).
Hors, dans mon cas la production est dans GitHub pages donc cela ne m'intéresse pas.

J'aurai pu déposer un répertoire vide ou avec un fichier *index.html* vide mais j'ai préféré créé un site vide en utilisant l'[API REST](https://docs.netlify.com/api/get-started/){:target="_blank"} de Netlify.

Il faut donc appeler l'API suivante en POST : `https://api.netlify.com/api/v1/sites` avec comme body :
```json
{
    "name": "philippart-s-blog-staging"
}
```
:warning: il faudra pour cela utiliser une authentification avec un token généré via la console de gestion de Netlify :warning:

L'élément important dans le retour de l'appel est le **site_id** mais il est possible de le retrouver dans la console de gestion de Netlify, sauvegardez le il sera utilisé plus tard.

## Mise à jour de l'environnement de staging à la création / modification de la PR :rocket:
C'est là que GitHub Actions entre en scène.

### Initialisation du pipeline
Rien de bien original, on le déclenche sur la création / modification d'une pull request à destination de la master.
```yaml
name: Jekyll site CI

on:
  pull_request:
    branches: [ master ]

jobs:
  jekyll:
    name: Build and deploy Jekyll site
    runs-on: ubuntu-latest

    steps:
    - name: Checkout
      uses: actions/checkout@v2
```

### Build du site Jekyll
On utilise deux actions : 
1. une action pour builder le site : `lemonarc/jekyll-action@1.0.0`
1. une action pour avoir un cache des dépendances ruby pour accélérer les builds futurs : `actions/cache@v2`

{% raw %}
```yaml
### ... 
    - uses: actions/cache@v2
      with:
        path: vendor/bundle
        key: ${{ runner.os }}-gems-${{ hashFiles('**/Gemfile.lock') }}
        restore-keys: |
          ${{ runner.os }}-gems-

    - name: Build
      uses: lemonarc/jekyll-action@1.0.0
```
{% endraw %}

### Déploiement du site généré sur Netlify
Pour cela on utilise l"action `nwtgck/actions-netlify@v1.1.13` qui permet de :
- utiliser le nom de la branch comme début du nom de l'URL du site déployé
- ajouter un message à la PR avec l'URL du site déployé

{% raw %}
```yaml
    - name: Deploy to Netlify
      uses: nwtgck/actions-netlify@v1.1.13
      with:
        publish-dir: './_site'
        github-token: ${{ secrets.GITHUB_TOKEN }}
        deploy-message: "Deploy from GitHub Actions : ${{ github.event.pull_request.title }}"
        enable-pull-request-comment: true
        enable-commit-comment: true
        overwrites-pull-request-comment: true
        alias: ${{ github.head_ref }}
      env:
        NETLIFY_AUTH_TOKEN: ${{ secrets.NETLIFY_AUTH_TOKEN }}
        NETLIFY_SITE_ID: ${{ secrets.NETLIFY_SITE_ID }}
      timeout-minutes: 1
```
{% endraw %}

:warning:c'est ici que l'on réutilise le *site_id* précédemment récupéré en le positionnant dans un secret GitHub, on fait de même avec le token associé à notre site de staging dans Netlyfi. :warning:

A chaque modification de ma PR un build puis un déploiement dans Netlify de la branche est déclenché, plutôt cool :sunglasses:.

TODO image message dans PR

### Suppression de la version staging du site :wastebasket:
Netlify ne permet pas de supprimer les déploiements de type preview, il sont stockés et accessibles tant que le site est créé dans Netlify.
Il y a un [thread](https://answers.netlify.com/t/does-each-deploy-preview-stay-available-forever/12601){:target="_blank"} dans le forum de support qui indique que la fonctionnalité de pouvoir supprimer des déploiements en *preview* a été demandée et qu'elle sera disponible un jour :wink:.

A ce stade voici le pipeline complet:
{% raw %}
```yaml
name: Jekyll site CI

on:
  pull_request:
    branches: [ master ]

jobs:
  jekyll:
    name: Build and deploy Jekyll site
    runs-on: ubuntu-latest

    steps:
    - name: Checkout
      uses: actions/checkout@v2

    - uses: actions/cache@v2
      with:
        path: vendor/bundle
        key: ${{ runner.os }}-gems-${{ hashFiles('**/Gemfile.lock') }}
        restore-keys: |
          ${{ runner.os }}-gems-
    - name: Build
      uses: lemonarc/jekyll-action@1.0.0

    - name: Deploy to Netlify
      uses: nwtgck/actions-netlify@v1.1.13
      with:
        publish-dir: './_site'
        github-token: ${{ secrets.GITHUB_TOKEN }}
        deploy-message: "Deploy from GitHub Actions : ${{ github.event.pull_request.title }}"
        enable-pull-request-comment: true
        enable-commit-comment: true
        overwrites-pull-request-comment: true
        alias: ${{ github.head_ref }}
      env:
        NETLIFY_AUTH_TOKEN: ${{ secrets.NETLIFY_AUTH_TOKEN }}
        NETLIFY_SITE_ID: ${{ secrets.NETLIFY_SITE_ID }}
      timeout-minutes: 1
```
{% endraw %}


### TODO :clipboard:
Le fait que les *preview deployment* ne soient pas supprimables n'est pas quelque chose qui me dérange par rapport aux informations contenu dans mes sites (tadx et mon blog) mais je trouve cela dommage de laisser quelque chose de déployé qui consomme de l'énergie pour rien :earth_africa:.
Du coup cet article n'est pas totalement terminé, on va dire que c'est la V1 :wink:.
La V2 verra la possibilité de supprimer les sites de staging déployés et cela me permettra d'écrire mes premières actions GitHub qui auront comme fonction de créer le site au début de la PR puis de le supprimer une fois que la PR sera fermée.

To be continued ...