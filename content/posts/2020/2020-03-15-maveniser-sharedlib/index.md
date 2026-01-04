---
title: "Maveniser le développement d'une sharedlib Jenkins"
description: "🚀 Rendre un développement d'une sharedlib industrialisable et ready to test. 🚀"
link: /2020-03-15-maveniser-sharedlib
tags: 
  - Groovy
  - Jenkins
  - Test
author: wilda
---

Depuis maintenant pas mal de temps j'ai en charge le développement des pipelines de notre CI/CD sous Jenkins 2 (voir l'[article](/2020-03-14-PipelinesJenkins) qui en dit plus long sur ce sujet).

J'ai bien dit développement car après avoir joué à *clique pour configurer* dans Jenkins 1, le passage à Jenkins 2 nous a permis de faire du *pipeline as code* ... ouf !

Cependant on ne se refait pas et durant ces mois de développements un truc me manquait: les tests unitaires et l'industrialisation du développement ! 

L'approche script de groovy et le côté *amateur* que l'on avait me dérangeait.
En effet le mode de développement était un peu empirique:
 1. je code 
 1. je commit
 1. je push
 1. je lance le pipeline 
 1. le code ne passe pas car erreur de dev (merci le non typage de groovy ...)
 1. correction
 1. retour en 1 :confounded:

Tout d'abord autant le dire tout de suite, si on m'avait posé la question il y a deux ans concernant le développement des pipelines le premier truc que j'aurais répondu : 
> Mais pourquoi Groovy ? Pourquoi un truc au dessus de Java pas typé qui détecte rien à la compilation !

Mais ça c'était avant, je n'irai pas dire que c'est mon langage préféré mais j'avoue commencer à apprécier certains choix faits et les closures c'est assez sympa (même si le scoping des variables peu prendre du temps à débuguer).

Les différents exemples que je vais utiliser seront basés sur le fait d'utiliser une sharedlib Jenkins, en gros on factorise du code dans une lib permettant de ne pas copier coller du code à outrance dans les Jenkinsfile. Plus d'informations dans la [doc](https://jenkins.io/doc/book/pipeline/shared-libraries/) (je ferai peut être un article sur les sharedlib et leur utilisation dans le futur).

Revenons à nos moutons, améliorer mon process de développement, premier réflexe ammener un peu de compilation et de contrôles sur le poste de développement.

Pour cela je reviens aux bases: Maven et tests unitaires (voir d'intégration), pourquoi Maven plutôt que Graddle ? Tout simplement parceque je maitrise plus Maven (un jour je me mettrai à graddle :wink:).

Après pas mal de recherches et de lecture de doc (la doc officielle de Jenkins est plutôt bien faite si on prends le temps de la lire) le choix a été d'utiliser le plugin [groovy-eclipse-compiler](https://github.com/groovy/groovy-eclipse/wiki), l'activation est assez simple:
 ```xml
    <plugin>
      <groupId>org.codehaus.groovy</groupId>
      <artifactId>groovy-eclipse-compiler</artifactId>
      <version>3.3.0-01</version>
      <extensions>true</extensions>
  </plugin>
  <plugin>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.8.1</version>                <!-- 3.6.2 is the minimum -->
      <configuration>
          <source>1.8</source>
          <target>1.8</target>
          <compilerId>groovy-eclipse-compiler</compilerId>
          <compilerArguments>
              <indy />
              <!-- optional; supported by batch 2.4.12-04+ -->
              <configScript>config.groovy</configScript> 
          </compilerArguments>
      </configuration>
      <dependencies>
          <!-- voir https://github.com/groovy/groovy-eclipse/wiki/Groovy-Eclipse-Maven-plugin -->
          <dependency>
              <groupId>org.codehaus.groovy</groupId>
              <artifactId>groovy-eclipse-compiler</artifactId>
              <version>3.3.0-01</version>
          </dependency>
          <dependency>
              <groupId>org.codehaus.groovy</groupId>
              <artifactId>groovy-eclipse-batch</artifactId>
              <version>2.5.6-01</version>
          </dependency>
      </dependencies>
  </plugin>
 ```
 
⚠️ Le développement des sharedlib impose une arborescence de sources, qui, en résumé doit avoir un `src` qui contient directement les classes utilitaires et un `vars` pour les variables directement utilisables dans des Jenkinsfiles, plus d'informations dans la [doc](https://jenkins.io/doc/book/pipeline/shared-libraries/#directory-structure).

Du coup il faut modifier les arborescences où trouver les sources car par défaut le plugin s'attends à trouver les sources dans `src/main/groovy` et `src/test/groovy`:
 ```xml
 <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>build-helper-maven-plugin</artifactId>
      <version>3.0.0</version>
      <executions>
          <execution>
              <id>add-source</id>
              <phase>generate-sources</phase>
              <goals>
                  <goal>add-source</goal>
              </goals>
              <!-- Ajout arborescence propre à Jenkins-->
              <configuration>
                  <sources>
                      <source>src</source>
                      <!--<source>vars</source>-->
                  </sources>
              </configuration>
          </execution>
          <execution>
              <id>add-test-source</id>
              <phase>generate-test-sources</phase>
              <goals>
                  <goal>add-test-source</goal>
              </goals>
              <configuration>
                  <sources>
                      <source>test/groovy</source>
                  </sources>
              </configuration>
          </execution>
      </executions>
  </plugin>
 ```
Une fois que l'on a tout ça il nous reste les dépendances, afin que la compilation se passe bien et qu'accessoirement on accède à toute la sandbox de Jenkins, aux API Groovy, ... il va falloir ajouter les bonnes dépendances:
```xml
 <!-- Dépendances pour le build-->
    <dependencies>
        <!-- Groovy ...-->
        <dependency>
            <groupId>org.codehaus.groovy</groupId>
            <artifactId>groovy-all</artifactId>
            <version>2.4.6</version>
        </dependency>

        <!-- Lib Jenkins -->
        <dependency>
            <groupId>org.jenkins-ci.main</groupId>
            <artifactId>jenkins-core</artifactId>
            <version>2.150</version>
            <scope>provided</scope>
        </dependency>

        <!-- Gestion de l'annotation @NonCPS -->
        <dependency>
            <groupId>com.cloudbees</groupId>
            <artifactId>groovy-cps</artifactId>
            <version>1.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin -->
        <dependency>
            <groupId>org.jenkins-ci.plugins.workflow</groupId>
            <artifactId>workflow-step-api</artifactId>
            <version>2.1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```
Pour les tests je me suis basé sur le framework de tests [JenkinsPipelineUnits](https://github.com/jenkinsci/JenkinsPipelineUnit) à l'origine créé par lesfurets.com et intégré dans la communeauté Jenkins, là encore quelques dépendances à ajouter:
 ```xml
         <!-- Testing -->
        <dependency>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
            <version>${ junit.jupiter.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.lesfurets</groupId>
            <artifactId>jenkins-pipeline-unit</artifactId>
            <version>1.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>3.4.1</version>
            <scope>test</scope>
        </dependency>

 ```

Enfin pour éviter de chercher trop longtemps les repos où trouver tout ça:
 ```xml
 <repositories>
      <repository>
          <id>Maven central</id>
          <url>http://central.maven.org/maven2/</url>
      </repository>
      <repository>
          <id>Jenkins</id>
          <url>http://repo.jenkins-ci.org/releases/</url>
      </repository>
  </repositories>

  <pluginRepositories>
      <pluginRepository>
          <id>bintray</id>
          <name>Groovy Bintray</name>
          <url>https://dl.bintray.com/groovy/maven</url>
          <releases>
              <updatePolicy>never</updatePolicy>
          </releases>
          <snapshots>
              <enabled>false</enabled>
          </snapshots>
      </pluginRepository>
  </pluginRepositories>
 ```

Le [pom complet](https://github.com/philippart-s/jenkins-examples/blob/master/pom.xml) et le [projet complet](https://github.com/philippart-s/jenkins-examples/).

Voilà à ce stade on a un projet qui compile, la complétion qui marche bien dans Intellij et ... la possibilité de faire des tests (ce sera l'objet du prochain article).

Cerise sur le gâteau notre développement peut lui même être buildé par Jenkins 2 mais dans mon cas ce sera sur [GitHub Actions](https://github.com/philippart-s/jenkins-examples/actions?query=workflow%3A%22Java+CI+with+Maven%22) car je n'ai pas d'instances Jenkins 2 gratuites :wink:.