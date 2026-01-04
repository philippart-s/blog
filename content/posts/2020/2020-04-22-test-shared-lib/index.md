---
title: "Tester une Sharedlib (et ses classes utilitaires) Jenkins"
description: "Tester c'est douter , Vraiment ? :thinking:"
link: /2020-04-22-test-shared-lib
tags: 
  - Groovy
  - Jenkins
  - Test
author: wilda
---

Suite de l'[article](/2020-03-15-maveniser-sharedlib) qui expliquait comment mettre en place un projet maven lors du développement d'une sharedlib Jenkins 2.

L'objectif maintenant va être de tester son code, notamment en local, afin d'éviter de devoir push son code sur le repo git ou de monter une instance de tests. 

Pour mémoire il est, je pense, nécessaire de se rafraîchir la mémoire sur le développement de pipelines Jenkins avec l'[article](/2020-03-14-PipelinesJenkins) qui en parle sur ce même blog :wink:.

Pour la suite nous allons utiliser la sharedlib déjà développée pour l'article sur les sharedlib, pour mémoire:
 - elle se compose d'une classe utilitaire qui a, comme seul but, d'exécuter une commande maven:

```groovy
 package fr.ourson.utils

/**
* Classe d'exemple représentant une classe utilitaire pour un pipeline.
*/
class Utilities implements Serializable{
    Script steps

    Utilities(Script steps) {
        this.steps = steps
    }

    /**
    * Méthode permettant d'exécuter une commande Maven.
    * @param args Maven arguments.
    */
    void mvn(String args) {
        steps.sh "mvn ${ args}"
    }
}
```
 [source](https://github.com/philippart-s/jenkins-examples/blob/master/src/fr/ourson/utils/Utilities.groovy)
 - d'un script représentant notre step:

```groovy
import fr.ourson.utils.Utilities

def call(Map config) {

    Utilities util = new Utilities(this)

    node() {
        stage('Build') {
            echo 'Building..'
            util.mvn config.mvnArgs
        }
        stage('Test') {
            echo 'Testing..'
        }
        stage('Deploy') {
            echo 'Deploying....'
        }
    }
} 
```
[source](https://github.com/philippart-s/jenkins-examples/blob/master/vars/myMavenStep.groovy)
 - et enfin le code du Jenkinsfile qui l'utilise:
 
```groovy
@Library('ourson-lib') _

myMavenStep mvnArgs: 'clean compile'
```

### Les tests ... une nécessité 
Ca y est! On a lu la doc en entier (2 fois parce que la première fois ce n'était pas clair :wink:), on a fait quelques allers / retours sur Stack Overflow, et oui ça y est: on a un premier pipeline assez clair avec du code factorisé dans une belle sharedlib ! Et tout ça avec simplement 25O commits et 150 pushs sur le référentiel Git pour pouvoir tester au sein de l'instance Jenkins.

Se posent alors mes deux questions d'origine:
 - que se passe-t-il si je touche une partie du code de la shared lib ? Je teste tous les pipelines pour m'assurer qu'il n'y a pas de régression ? 
 - n'y a-t-il pas plus simple que de push mon code pour le tester ? Cela fait un peu "je teste sur la prod"  :scream: (même si le mécanisme de branche me permet d'éviter cela).
 
Comme vous vous en doutez, c'est là que les tests unitaires vont répondre à ces deux problématiques.

### Outillage
La première chose qui m'est venu à l'esprit c'est ... Docker ! :sunglasses:

La vraie fausse bonne idée: je n'ai pas besoin de me connecter à mon instance distante mais il reste tout de même le problème de push le code sur le repo git pour la prise en compte par Jenkins. Il existe bien un moyen de référencer une lib de manière locale sur le file system mais ce n'est pas simple, pas très fiable, peu performant et quand bien même, le process reste lourd pour tester mon développement: il faut que je lance un pipeline via l'interface de Jenkins.

Non, définitivement, je veux pouvoir faire des test unitaires et d'intégration directement dans mon IDE et sur ma PIC (attention inception inside: un job Jenkins qui build / test mon développement de sharedlib !).

Il y a plusieurs façons de faire des tests et avec elles les frameworks associés: moi je viens du monde Java donc forcément cela va se ressentir sur mes choix :wink:.

J'utilise [JUnit](https://junit.org/) comme "ordonnanceur" de tests. 

J'adosse à JUnit un framework qui va me permettre de mocker Jenkins. Ce fameux framework est  [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit) qui a été créé à l'origine par l'équipe des furets.com. 
Après quelques mois d'inactivé, une nouvelle équipe l'a repris en main et de nombreuses nouvelles fonctionnalités ont été ajoutées ainsi que quelques corrections de bugs.
C'est [officiellement](https://jenkins.io/doc/book/pipeline/development/#unit-test) le framework de tests pour les pipelines et il fait partie de l'organisation GitHub Jenkinsci.

Il faut un certain temps pour le prendre en main mais il y a de [nombreux exemples](https://github.com/jenkinsci/JenkinsPipelineUnit/blob/master/README.md#demos-and-examples) en lien dans la [documentation](https://github.com/jenkinsci/JenkinsPipelineUnit/blob/master/README.md).

En résumé ce qu'il faut comprendre:
 - le framework permet d'exécuter les pipelines comme si on était dans une instance Jenkins
 - le framework permet de mocker les steps qui sont propres à Jenkins

Un petit mot sur le mocking: *JenkinsPipelineUnit* permet de mocker simplement les méthodes qui sont appelées par les pipelines.
Le framework vient avec plein de méthodes déjà mockées, mais il est tout à fait possible de surcharger le mock afin qu'il fasse ce que l'on souhaite.

On peut aussi utiliser d'autres frameworks pour mocker comme *PowerMock*, *Mockito* ou tout simplment le [mocking intégré](https://groovy-lang.org/testing.html#_mocking_and_stubbing) à Groovy. Dans certains cas cela peut faciliter la vie :smile:.

### Ce que l'on va tester
L'idée ici n'est pas de réécrire la documentation qui est fournie avec le framework *JenkinsPipelineUnit*. Je ne vais pas non plus expliquer comment faire pour tester des pipelines en mode *ScriptedPipeline* ou *Declartive Pipelines* (voir l'[article](/2020-03-14-PipelinesJenkins) pour la différence entre les *Scripted Pipelines* et *Declarative Pipelines*) mais plutôt comment tester des sharedlib qui sont faites pour écrire des pipelines avec des [customs steps](https://jenkins.io/doc/book/pipeline/shared-libraries/#defining-custom-steps).

Et on va voir que cela nécessite quelques adaptations.

### Tester la classe utilitaire
Pour découvrir comment faire, nous allons partir d'un test unitaire d'une méthode pour aller jusqu'à tester le custom step.
Notre premier test unitaire concerne donc la classe utilitaire *Utilities* qui permet d'exécuter une commande maven avec, notamment, le step *sh* fourni par Jenkins.

Heureusement le framework *JenkinsPipelineUnits* nous mock une grande partie des steps de Jenkins et dans notre cas *sh*.

```groovy
package fr.ourson.utils

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtilitiesTest extends BasePipelineTest {
    def steps

    @BeforeEach
    void setUp() {
        // Positionnement de l'endroit où on trouve les sources et les différentes ressources
        this.scriptRoots += "test/resources"
        this.scriptRoots += "src"

        // Initialisation de JenkinsPipelineUnit
        super.setUp()
        
        // Astuce pour pouvoir exécuter le code dans un "context" Jenkins
        steps = loadScript('foo.groovy')
    }

    @Test
    void should_execute_maven_command() {
        Utilities utilities = new Utilities(steps)

        utilities.mvn 'clean compile'

        assertCallStackContains('mvn clean compile')
    }
}
```
[source](https://github.com/philippart-s/jenkins-examples/blob/master/test/groovy/fr/ourson/utils/UtilitiesTest.groovy)

Quelques explications sont nécessaires il me semble !
 - `extends BasePipelineTest`: nécessaire pour bénéficier du framework *JenkinsPipelineUnit* 
 - `def steps`: variable qui va stocker le *contexte* Jenkins et accéder aux steps qu'il met à disposition
 - `setup`: nécessaire pour positionner quelques paramétrages pour la bonne exécution du test
   - `this.scriptRoots += 'test/resources'`et `this.scriptRoots += 'src'`: positionnement des racines de paths où se trouvent les scripts et classes des pipelines
   - `steps = loadScript('foo.groovy')`: c'est cette instruction qui va nous permettre de charger le contexte Jenkins émulé par le framework *JenkinsPipelineUnit* mais pour cela, rappelez-vous: à l'origine le framework a été pensé pour du *Scripted Pipeline*. Il faut donc charger un script et c'est ce que l'on fait, le script en lui-même ne fait rien d'autre, il est donc vide:

```groovy
    def version = '1.0'

    return this
```
[source](https://github.com/philippart-s/jenkins-examples/blob/master/test/resources/foo.groovy)   
 - `Utilities utilities = new Utilities(steps)`: il est maintenant possible d'instancier la classe pour pouvoir l'appeler et la tester
 - `assertCallStackContains('mvn clean compile')`: le framework propose des méthodes utilitaires: celle-ci permet de rechercher dans l'arbre d'appel une commande en particulier, dans mon cas je veux vérifier que l'on appelle bien la commande *mvn* avec les paramètres passés: *clean compile*

### Tester le custom step
A ce stade nous savons donc tester des classes utilitaires mais il nous manque notre custom steps qui est utilisé dans les Jenkinsfiles.

Pour cela il suffit de tester le script représentant le custom step, dans notre cas *myMavenStep.groovy*:
 ```groovy
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Classe permettant de tester le script représentant le custom step myMavenStepTest
 */
class MyMavenStepTest extends BasePipelineTest{

    @Override
    @BeforeEach
    void setUp() {
        this.scriptRoots += 'test/resources'
        this.scriptRoots += 'src'
        this.scriptRoots += 'vars'
        super.setUp()
    }

    @Test
    void should_run_custom_step() {
        // Chargement du custom step
        def customStepToTest = loadScript('myMavenStep.groovy')

        Map params = [mvnArgs:'clean compile']
        
        // Appel de la méthode principale du custom step
        customStepToTest.call(params)

        assertCallStackContains('mvn clean compile')
    }
}
 ```
[source](https://github.com/philippart-s/jenkins-examples/blob/master/test/groovy/vars/MyMavenStepTest.groovy)  
Quelques explications:
 - `this.scriptRoots += 'vars'`: on ajoute le répertoire *vars* dans la liste des endroits où trouver des scripts car le custom step est dedans
 - `def customStepToTest = loadScript('myMavenStep.groovy')`: plutôt que de charger un script *bidon* on charge le script qui représente le custom step
 - `customStepToTest.call(params)`: il ne reste plus qu'à appeler la méthode *call* de notre script pour tester son bon fonctionnement (comme le ferait Jenkins lors de l'exécution du Jenkinsfile)
 - `assertCallStackContains('mvn clean compile')`: ensuite, on utilise la méthode *assertCallStackContains* pour tester que la commande maven est bien appelée

#### Une limitation: ne pas pouvoir utiliser un Jenkinsfile avec le custom step
On vient de le voir: à ce stade on a testé de manière unitaire une méthode utilisant des steps Jenkins, puis on testé un custom step qui utilise cette méthode.

Même si on couvre l'ensemble de ce que l'on veut tester, j'aurai aimé pouvoir tester l'utilisation du custom step en situation réelle.
Ce que j'entends par "réelle" c'est tester un Jenkinsfile du genre:
```groovy
@Library('ourson-lib') _

myMavenStep mvnArgs: 'clean compile'
```
Malheureusement, un test unitaire qui charge ce genre de fichier ne fonctionne pas: les mocks proposés par le frameworks ne sont pas utilisés.
C'est peut-être moi qui n'ai pas compris comment faire ou le framework qui ne le permet pas (encore :wink:).


Voilà, c'est la fin de cet article autour des tests unitaires qui permettent (doivent) accompagner le développement des sharedlib Jenkins.
J'espère que cela vous aura donné quelques astuces pour tester vos sharedlib et améliorer le process de développement de celles-ci.

L'ensemble des sources est disponible [ici](https://github.com/philippart-s/jenkins-examples).