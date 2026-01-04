---
title: "Déployer un site statique sur un bucket S3 avec GitHub Actions"
link: /2020-12-30-Cellar-Actions
tags: 
  - GitHub
  - S3
author: wilda
---

Cela fait quelques temps que j'utilise [GitHub Pages](https://pages.github.com/) pour le site de l'association [TADx](https://www.tadx.fr) avec [Jekyll](https://jekyllrb.com/).

Et cela fonctionne plutôt bien !

Oui mais il me manque quelque chose : un environnement de staging qui me permet de voir ce que donne une modification du site représentée par une branche.
GitHub Pages ne permet pas d'avoir plus d'une branche déployée. 
C'est là que je me suis mis à la recherche d'un endroit où déployer le code d'une branche et ce, de manière automatique.
J'ai choisi le couple [bucket S3](https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html) et [GitHub Actions](https://github.com/features/actions) !

## Bucket S3 📦
Je ne suis pas un grand spécialiste du cloud et d'AWS, alors pourquoi choisir S3 ? 
Je dois l'avouer que l'on m'a soufflé l'idée suite à une rencontre avec Laurent Doguin de [Clever Cloud](https://www.clever-cloud.com/) lors d'un [meetup](https://www.tadx.fr/2020/11/22/13-eme-event.html) TADx. 
Notamment le très bon [tuto](https://www.clever-cloud.com/blog/engineering/2020/06/24/deploy-cellar-s3-static-site/) de Laurent qui explique comment déployer un site statique sur Clever Cloud grâce à leur add-on [cellar](https://www.clever-cloud.com/doc/deploy/addon/cellar/).

Du coup, je vous laisse allez regarder tout ce qu'il faut faire pour activer cet add-on, moi je vais juste ajouter deux ou trois trucs pour automatiser tout ça :wink:.

## GitHub Actions 🚀
Une fois le bucket créé et opérationnel il faut l'alimenter, et si possible le plus automatiquement possible pour ne pas avoir à effectuer toutes les actions à la main.

### Activer GitHub Actions 
Je ne vais pas m'étendre sur ce sujet car, encore une fois, la [documentation](https://docs.github.com/en/free-pro-team@latest/actions) est très bien faite.
Il suffit d'avoir un répertoire `.github/workflows` dans le repository et d'y coller les `.yml` des différents workflows.

### Automatisation de la création du bucket ✨
L'idée est de créer un bucket temporaire à chaque fois qu'une PR est créée pour pouvoir visualiser la nouvelle version du site avant de le merger sur la master, cela se fait avec l'action suivante.
```yml
name: Crétion d'un bucket pour la PR

on:
    pull_request:
        types: [opened]
    
jobs:
  create_env:
    name: Création du bucket S3 / Cellar
    runs-on: ubuntu-latest
    steps:
      - name: Create bucket for PR
        run: |
            aws configure set aws_access_key_id ${{ secrets.AWS_ACCESS_KEY_ID }}
            aws configure set aws_secret_access_key ${{ secrets.AWS_SECRET_ACCESS_KEY }}
            aws s3api create-bucket --bucket ${{ github.head_ref}} --acl public-read --endpoint-url ${{ secrets.CLEVER_END_POINT }} > /dev/null
```
On voit que ce workflow se déclenche sur l'ouverture d'une PR.
Afin de ne pas exposer les secrets Clever ils sont sont stockés dans le repository GitHub avec l'utilisation de [GitHub Secrets](https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets).

La création du bucket va se faire avec le [CLI AWS](https://aws.amazon.com/cli/), comme on n'utilise pas le cloud AWS il faudra spécifier le cellar clever cloud dans toutes les commandes en ajoutant `--endpoint-url ${{ secrets.CLEVER_END_POINT }}`, là encore j'utilise les secrets GitHub pour ne pas indiquer l'URL en dur.

### Mise à jour du bucket lors du push de code 🔁
Ici il y a deux étapes dans le workflow : builder le site avec Jekyll et le déployer dans le bucket.

```yml
name: CI / CD des PR

on:
  pull_request:
    branches: [ master ]

jobs:
  jekyll:
    name: Build and deploy Jekyll site
    runs-on: ubuntu-latest

    steps:
    # Récupération des sources
    - name: Checkout
      uses: actions/checkout@v2
    # Acivation du cache des dépendances Ruby
    - uses: actions/cache@v2
      with:
        path: vendor/bundle
        key: ${{ runner.os }}-gems-${{ hashFiles('**/Gemfile.lock') }}
        restore-keys: |
          ${{ runner.os }}-gems-
    # Build du site Jekyll
    - name: Build
      uses: lemonarc/jekyll-action@1.0.0
    # Copie du site généré dans le bucket
    - name: Sync output to S3   
      run: |
        aws configure set aws_access_key_id ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws configure set aws_secret_access_key ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws s3 sync ./_site/ s3://${{ github.head_ref}} --endpoint-url ${{ secrets.CLEVER_END_POINT }} --acl public-read
```

Ce workflow se déclenche à chaque fois qu'une PR, qui cible la master, a du code qui est push.
Afin d'accélérer le build on utilise l'action permettant de faire du [cache de dépendance](https://github.com/actions/cache) `actions/cache@v2`.

Ensuite j'utilise une action pour [builder](https://github.com/lemonarc/jekyll-action) un site Jekyll : `lemonarc/jekyll-action@1.0.0`.
C'est un peu le réflexe avec GitHub actions : aller voir sur le [market place](https://github.com/marketplace?type=actions) si il n'existe pas déjà une action qui a été créée !
 
Enfin, on synchronise le site dans le bucket, de nouveau on utilise le CLI AWS en indiquant le cellar clever.
 
### Supprimer le bucket 🗑️

L'idée n'est pas de multiplier les versions du site mais simplement d'avoir un environnement de staging à la demande.
Du coup une fois la PR mergée je supprime le bucket.

```yml
name: Suppression du bucket

on:
    pull_request:
        types: [closed]
    
jobs:
  delete_env:
    name: Suppression du bucket de la branche
    runs-on: ubuntu-latest
    steps:
      - name: Delete bucket for branch
        run: |
            aws configure set aws_access_key_id ${{ secrets.AWS_ACCESS_KEY_ID }}
            aws configure set aws_secret_access_key ${{ secrets.AWS_SECRET_ACCESS_KEY }}
            aws s3 rb s3://${{ github.head_ref}} --force --endpoint-url ${{ secrets.CLEVER_END_POINT }}
```

Le workflow se déclenche à chaque fermeture de PR.
Rien de spécial, on reprend les mêmes ingrédients que précédemment : secrets GitHub et AWS CLI pour la partie bucket.

## En conclusion
Voilà c'est fini : avec ça on a une automatisation qui fonctionne plutôt pas mal :wink:.
Il y a des choses à améliorer, comme par exemple publier l'URL du site de test dans la PR mais on a déjà une bonne base. 
Côté S3 je n'ai pas trop creusé la chose et j'avoue qu'il y a deux ou trois trucs un peu obscurs :laughing:.