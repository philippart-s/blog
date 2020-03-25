---
title: "Jenkins, pipelines, sharedlib, ..."
excerpt: ":thinking: Mais au fait c'est quoi un pipeline Jenkins ? " 
classes: wide
categories:
  - Articles
  - Code
tags:
  - Groovy
  - Jenkins
  - Test
---
En rédigeant la [première partie de l'article]({% post_url 2020-03-15-test-shared-lib-part1 %}) sur comment faire des tests unitaires lors de développements de sharedlib pour les pipelines Jenkins je me suis rendu compte pour bien le comprendre il fallait tout d'abord expliquer rapidement ce que sons les dits pipelines et sharedlib ! :innocent:

On va donc reprendre par le début !

### CI / CD et automatisation 
En ces périodes de *DevOps* à outrance il apparaît une chose commune à toutes celles et ceux qui s'y essaient: la nécessité d'automatiser et de fludifier le processus de build, de livraison et de déploiement.

C'est là qu'interviennent la construction continue (Continuous Integration), la livraisons continues (Continuous Delivery) et le déploiment continu (Continuous Deployment), en résumé ce que l'on appelle le **CI / CD**.

A cela s'ajoute le terme de *Pipelines* où comment concevoir et représenter les processus précedemment cité, ce ne sont au final qu'un enchainement de tâches séquentielles ou parallèles permettant d'atteindre le but, à savoir la construction d'un livrable et son déploiement.

Par exemple un pipeline classique de construction d'une application (Java par exemple :wink:):
![simple pipeline ci/cd]({{ site.url }}{{ site.baseurl }}/assets/images/PipelineJenkins/JavaSimplePipeline.png)

### Outillage
On se doute bien que l'on peut faire ça à la main en script bash par exemple mais que cela risque de vite prendre du temps et ne pas être très simple à maintenir !

Il existe plétore d'outils dédiés plus ou moins à cela, le plus ou moins venant du fait que certains outils dont ce n'est pas le coeur de métier historique finissent par le proposer afin de permettre aux utlisateur de bénéficier d'une "suite" logicielle complètre pour la fabrication de leur application (entre nous cela permet aussi de garder l'utlisateur bien au chaud dans son giron sans qu'il aille voir ailleurs :wink:).

Citons quelques uns de ces outils:
 - [Jenkins](https://jenkins.io/){:target="_blank"} (1 & 2)
 - [Travis CI](https://travis-ci.org/){:target="_blank"}
 - [Circle CI](https://circleci.com){:target="_blank"}
 - [GitLab CI](https://docs.gitlab.com/ee/ci/){:target="_blank"}
 - [GitHub Actions](https://github.com/features/actions){:target="_blank"}
 - ...

Il y en a pleins d'autres, mais l'idée est juste d'illustrer que ce n'est pas le choix qui manque !

Les deux derniers (GitLab et GitHub) sont un peu particuliers car ce ne sont pas *que* des outils de CI/CD mais bien plus, mais ça je vous laisse aller voir cela par vos propres moyens (peut être que cela fera de prochains articles :wink:).

Intéressons nous à Jenkins, sujet de cet article.

### Jenkins
Deux versions existent, même si la version 1 n'est plus beaucoups utilisée (à part dans du legacy) c'est avec elle que beaucoup ont commencé (dont moi), je ne vais pas m'apesentie dessus car ce n'est pas avec cette version que je vais illustrer mes articles.

La version 2 de Jenkins est celle qui nous intéresse et celle qui sera utilisée pour mes articles, non pas que les autres soient mauvaises (j'utilise à titre perso GitHub Actions car c'est gratuit) mais je ne les utilise pas au quotidien dans mon travail.

Dans sa première mouture Jenkins permettait de définir un *job* qui était un ensemble de *steps*: tâche maven, appel sonar, bash, ...
Toute la configuration se faisait par le biais de l'interface graphique, pratique mais pas très industrialisable ni facile à généraliser: une modification transverse et c'était sur l'ensemble des pipelines jobs qu'il fallait repasser.

Jenkins 2, qui a vu le jour en 2016, a apporter une grosse nouveauté: la généralisation du pipeline as code pour définir ses jobs.

L'autre grosse nouveauté c'est que l'on peut désormait "embarqué" le pipeline directement dans l'arborescence projet sans devoir créer le job dans l'interface de Jenkins, le pipeline se définit par du code dynamiquement chargé et exécuté par le moteur de Jenkins.

### Pipeline as code
Jenkins est écrit en Java et permet d'exéécuter des pipelines écris en Groovy (je pense qu'il serait possible de le faire en Java aussi mais ce n'est clairement pas comme cela que ça a été pensé).

Pour développer son pipeline il y a deux possibilités: *[Scripted Pipeline](https://jenkins.io/doc/book/pipeline/syntax/#scripted-pipeline){:target="_blank"}* ou *[Declarative Pipeline](https://jenkins.io/doc/book/pipeline/syntax/#declarative-pipeline){:target="_blank"}*. Les deux sont basées sur Groovy et le DSL (Domain Specific Language) proposé par Jenkins. Le *Scripted Pipeline* est la première façon qui a vue le jour pour développer ses pipelines, le *Declarative Pipeline* est plus récent et permet, principalement, de simplifier le développement des pipelines en n'utilisant que du DSL qui fait plus penser à de la config as code (mais où les espaces ne sont pas représentatifs ... :devil:).

Au final nous allons essentiellement conserver la phylosophie des *Declaratives Pipelines*: ne pas mettre de code Groovy dans nos Jenkinsfile, réserver cela à des classes utilitaires ou la définition de nos propres steps.


---

Jenkins permet de coder ses pipelines de plusieurs manières, les deux principales : *Syntaxique Pipelines* et *Declarative Pipelines*.

Pour continuer je vais devoir expliquer simplement les concepts de base de la conception des pipelines Jenkins avec du code groovy et plus particulièrement ce que l'on appelle les *Declarative Pipelines*, bien entendu rien de remplacera les riches articles dans la [documentation](https://jenkins.io/doc/book/pipeline/){:target="_blank"} officielle, je vais juste essayer de résumer pour que l'on parle le même langage et comprenne le reste de l'article sur les tests unitaires et d'intégration.



### Jenkinsfile
Bon on a les notions de base maintenant il faut nous lancer et pour cela il va falloir que l'on code notre premier pipeline, par convention code le pipeline dans un fichier se nommant *Jenkinsfile* mais au final peut importe le nom tant que c'est un script groovy !

Un *Jenkinsfile* en mode *Declarative pipeline*:
```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building..'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing..'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }
}
``` 
[source](https://jenkins.io/doc/book/pipeline/jenkinsfile/){:target="_blank"}

### Les sharedlib
Une fois que l'on a commencé à codé des pipelines dans nos *Jenkinsfile* on a fait un grand pas par rapport à l'approche *click to config* de Jenkins 1 mais on se retrouve avec le même problème: toutes les actions identiques entre les projets sont dupliquées et lors d'un changement par exemple de plugin que l'on utilise on se retrouve à faire une maintenance sur tous les *Jenkinsfiles*.
Il faut donc pouvoir factoriser le code pour pouvoir le réutiliser, c'est là qu'intervienent les [sharedlib](https://jenkins.io/doc/book/pipeline/shared-libraries/): une lib qui regroupe des classes / scripts réutilisables dans les *Jenkinsfile*.  Cette lib étant récupérée directement depuis le référentiel de sources au moment de son utlisation, cela explique le mode opératoir empirique expliqué dans la première partie de l'article (devoir push son code pour tester), un exemple simple d'utilisation de sharedlib:
```groovy
@Library('utils') import org.foo.Utilities
def utils = new Utilities(this)
node {
  utils.mvn 'clean package'
}
```

>L'objectif de cet article n'est pas d'expliquer dans le détail comment construire un pipeline Jenkins avec le SDK fournit je ne vais donc pas m'apesentir sur le comment mais il était nécessaire de présenter ces notions avant de continuer (j'aurai peut-être dû le faire avant la première partie ... je suis encore en mode apprenti blogueur :wink:).