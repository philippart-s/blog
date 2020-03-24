---
title: "Tester une classe utilitaire dans une Sharedlib Jenkins : partie 2"
excerpt: "Tester c'est douter , Vraiment ?"
classes: wide
categories:
  - Articles
  - Code
tags:
  - Groovy
  - Jenkins
  - Test
---
Suite de l'[article]({% post_url 2020-03-15-test-shared-lib-part1 %}) qui expliquait comment mettre en place un projet maven lors du développement d'une sharedlib Jenkins 2,(mais au final plus généralement d'un projet Groovy) l'objectif maintenant va être de tester son code, notamment en local, afin d'éviter de devoir push son code sur le repo git ou monter une instance de tests. 

Pour continuer je vais devoir expliquer simplement les concepts de base de la conception des pipelines Jenkins avec du code groovy et plus particulièrement ce que l'on appelle les *Declarative Pipelines*, bien entendu rien de remplacera les riches articles dans la [documentation](https://jenkins.io/doc/book/pipeline/){:target="_blank"} officielle, je vais juste essayer de résumer pour que l'on parle le même langage et comprenne le reste de l'article sur les tests unitaires et d'intégration.

### Un pipeline ... 
Ici on ne parle pas de pétrole ou d'eau mais bien d'enchainement d'étapes de nécessaire à la construction, déploiment ou autre livraison d'une application.
Ces étapes peuvent êtres séquentielles ou parallèles. Comme une image vaut 100 mots voici ce que cela donne:
![jenkins-pipline]({{ site.url }}{{ site.baseurl }}/assets/images/jenkins-pipeline.png)
Jusqu'à Jenkins 2 pour définir des pipelines on passait par l'interface graphique de Jenkins, facile mais pas de réutilisabilité et très rébarbatif à faire. C'est là où Jenkins 2 et ses pipelines as code nous apportent la solution !

### ... et du code !
Pour développer son pipeline il y a plusieurs possibilités, tout faire en code, dans ce cas on est plutôt dans une approche *Scripted Pipeline* ou utiliser le DSL (Domain Specific Language) proposé par Jenkins et dans ce cas on plutôt dans l'approche *Declarative Pipeline*.

Pour ce qui nous interresse nous allons partir sur l'approche *Declarative Pipeline* qui est plus simple, plus récente et qui semble être ce vers quoi poussent les équipes Jenkins.

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

### Les tests ... une nécessité 
Ca y est on a lu la doc en entier (2 fois parceque la première fois ce n'était pas clair :wink:), on a fait quelques aller / retour sur Stack Overflow mais oui ça y on a un premier pipeline assez clair avec du code factorisé dans une belle sharedlib ! Et tout ça avec simplement 25O commits et 150 push sur le référentiel GitHub pour pouvoir tester au sein de l'instance Jenkins.

Et se pose alors mes deux questions d'origne:
 - que se passe t il si je touche une partie du code de la shared lib ? Je test tous les pipelines pour m'assurer qu'il n'y a pas de régression ? 🤔
 - il n'y a pas plus simple que de push mon code pour le tester ? Cela fait un peu je teste sur la prod (même si le mécanisme de branche me permet d'éviter cela) :scream:.
 
Comme vous vous en doutez c'est là que les tests unitaires vont répondre à ces deux problématiques.

### Outillage
Comme déjà indiqué je vais utiliser le framework de tests [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit){:target="_blank"} qui a été créé à l'origine par l'équipe des furets.com. Après quelques mois d'inactivé une nouvelle équipe l'a repris en main et de nombreuses nouvelles fonctionnalités ont été ajoutées ainsi que quelques corrections de bugs.

Il faut un certain temps pour le prendre en main mais il y a de nombreux exemples en lien dans la documentation.

En résumé ce qu'il faut comprendre:
 - le framework permet d'exécuter les piplines comme si on était dans une instance Jenkins
 - le framework permet de mocker les commandes qui sont proprent à Jenkins (que cela vienne du moteur ou de plugins installés.)
Un petit mot sur le mocking, il est tout à fait possible d'utiliser d'autres frameworks pour mocker comme *PowerMock* ou *Mockito* par exemple mais *JenkinsPipelineUnit* permet de moker simplement les méthodes qui sont appelées par les pipelines, par exemple la commande `echo`qui permet d'afficher du texte dans les logs d'exécution du pipeline.

Le framework vient avec plein de méthodes déjà mockées mais il est tout à fait possible de de surcharger le mock afin qu'il fasse ce que l'on souhaite.

### Notre premier test: une classe utilitaire dans une sharedlib
Ouf la présentation est terminée (certe un peu long pour une présentation mais il falleit bien ça :innocent:) notre premier test arrive.

Notre classe à tester:
```groovy
package fr.ourson

/**
 * Wonderfull class to say Hello !
 */
class SayHello {
    // Jenkins steps
    def steps

    /**
     * Constructor with Jenkins steps.
     * @param steps Jenkin's steps.
     */
    SayHello(Script steps) {
        this.steps = steps
    }

    /**
     * Say hello to the world !
     * @param name Who say hello ?
     */
    void sayHelloToTheWorld(String name) {
        steps.echo "Hello World by ${name} !!!"
    }
}
```
Le script permettant de définir le custom step:
```groovy
import fr.ourson.SayHello

def call(String name) {
    SayHello sayHello = new SayHello(this)
    sayHello.sayHelloToTheWorld(name)
}
```
Enfin le jenkinsfile qui l'utilise:
```groovy
hello 'Ourson'
```

Le [pom complet](https://github.com/philippart-s/groovy-examples/blob/master/pom.xml){:target="_blank"} et le [projet complet](https://github.com/philippart-s/groovy-examples/){:target="_blank"}.

Voilà à ce stade on a un projet qui compile, la complétion qui marche bien dans Intellij et ... la possibilité de faire des tests (ce sera l'objet du prochain article).

Cerise sur le gâteau notre développement peut lui même être buildé par Jenkins 2 mais dans mon cas ce sera sur [GitHub Actions](https://github.com/philippart-s/groovy-examples/actions?query=workflow%3A%22Java+CI+with+Maven%22) car je n'ai pas d'instances Jenkins 2 gratuites :wink:.