---
title: "Créer un opérateur Kubernetes en Java ... C'est possible !"
description: "Il n'y a pas que GO pour créer un opérateur Kubernetes, c'est aussi possible en Java :coffee:!"
link: /2021-11-09-java-k8s-operator
tags: 
  - K8s
  - Java
image: jarvis.jpg
figCaption: "@wildagsx"
author: wilda
---

> 💡 Mise à jour : suite à la release 2.x du SDK j'ai mis à jour l'article et le code 😉 

## Mais c'est quoi un opérateur ?

Lorsque l'on me parle d'un opérateur Kubernetes moi je pense à ça :


ou à ça : 

![Matrix](matrix.jpg)


Je ne vais pas me lancer sur l'explication de ce qu'est un [opérateur Kubernetes](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/) mais en gros c'est un contrôleur permettant d'étendre les API de Kubernetes afin de gérer de manière plus efficace les applications déployées (installation, actions d'administration, ...).

Pour définir un opérateur, il faut définir une _custom resource definition_ puis créer la _custom resource_ associée. C'est cette création / modification qui va permettre, notamment, de déclencher des actions (utiles pour automatiser des installations par exemple).

Ensuite, l'opérateur va scruter en permanence la ressource pour agir en cas de modification.
Il est aussi possible d'accéder à ces _custom resources_ via la CLI _kubectl_ puisque ce n'est qu'une extension de l'API de base.

En résumé : faisons faire par un programme des actions automatisables qui n'ont pas de plus-value à être faites par des humains.
En gros c'est que l'on appelle partout DevOps ... 

## Ok, je suis expert opérateur maintenant 😅, comment on en développe un ?

Alors déjà faisons le point sur les deux grands types d'opérateurs : ceux qui se chargent essentiellement de l'installation et la mise à jour des applications et ceux qui vont plus loin pour proposer des actions d'administration / ops automatisées sur les applications.

On trouve souvent cela sous la dénomination _modèle de maturité des opérateurs_, illustré par le schéma suivant :
![Operator Capability](operator-capability-model.png)

Et là il y a un truc qui me chagrine car c'est soit du Helm, soit du Ansible ... soit du Go 🙄.

## Sinon ça existe dans un vrai langage 🤡 ?

Du coup, même si les quelques docs existantes ne le mentionnent pas (notamment celle de Kubernetes), j'ai recherché si il existait un projet qui se serait lancé dans l'aventure.

J'en profite pour clarifier un point qui peut paraître évident pour les sachants mais qui ne l'était pas pour moi au début : on peut écrire un opérateur dans n'importe quel langage !

Ce sera juste plus ou moins simple pour le créer avec plus ou moins d'aide : le _scaffolding_ d'un projet, l'aide dans la génération des différentes ressources ou _custom resources_, les appels des API Kubernetes, ...

Voilà, une fois que ça c'est dit, on peut continuer et partir à la recherche d'un langage me permettant de faire ce que je veux, je parle bien sûr de Java :wink:.

## java-operator-sdk 🛠️

J'ai trouvé mon bonheur avec le projet [java-operator](https://github.com/java-operator-sdk/java-operator-sdk).

La documentation officielle : [https://javaoperatorsdk.io/](https://javaoperatorsdk.io/).

Ils se sont largement inspirés de celui écrit en Go ([https://github.com/operator-framework/operator-sdk](https://github.com/operator-framework/operator-sdk)) et ne s'en cachent pas. 
Il reste cependant pas mal de chemin avant d'arriver au niveau de celui-ci (pour moi la fonctionnalité la plus manquante étant le _scaffolding_) mais on verra un peu plus loin que ce qui est fourni aide grandement pour la création d'un opérateur.

Une chose importante à savoir est que le projet est basé sur le client Kubernetes Java proposé par [fabric8](https://github.com/fabric8io/kubernetes-client) qui facilite grandement la vie pour accéder aux API Kubernetes (et Openshift 😉).

## Hello world ! 👋

Bon c'est parti pour écrire notre premier _hello world !_.
On va faire quelque chose d'assez simple : un opérateur qui log les attributs positionnés dans la _custom resource_, dingue non 😎 ?

Bon on y va ?

### Configuration du projet ⚙️

Rien de plus simple on ajoute 2 dépendances :
```xml
<!-- Dépendance principale -->
<dependency>
  <groupId>io.javaoperatorsdk</groupId>
  <artifactId>operator-framework</artifactId>
  <version>2.0.2</version>
</dependency>

<!-- Dépendance pour générer les CRD 😎 -->
<dependency>
  <groupId>io.fabric8</groupId>
  <artifactId>crd-generator-apt</artifactId>
  <version>5.11.2</version>
  <scope>provided</scope>
</dependency>
```

### Le squelette du projet 🦴

C'est assez simple et la [documentation](https://javaoperatorsdk.io/docs/getting-started) est plutôt bien faite (voir la section _How to use samples_ et particulièrement le projet [pure-java](https://github.com/java-operator-sdk/java-operator-sdk/tree/main/smoke-test-samples/pure-java)).

#### Définition de la _custom resource definition_ 📝

Il est possible de définir la partie _spec_ de la _custom resource definition_ (CRD) sous forme de POJO  : 

```java
public class HelloWorldSpec {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

Ensuite il suffit de créer la classe qui représentera la CRD : 
```java
@Group("fr.wilda")
@Version("v1")
@ShortNames("hw")
public class HelloWorldCustomResource extends CustomResource<HelloWorldSpec, Void> implements Namespaced {

}
```

`mvn compile` génère dans le _target/classes/META-INF/fabric8_ deux CRD : un version beta et un version normale (ils sont identiques au moment de la génération). 

Voici à quoi ressemble le fichier _helloworldcustomresources.fr.wilda-v1.yml_ généré : 
```yaml
# Generated by Fabric8 CRDGenerator, manual edits might get overwritten!
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: helloworldcustomresources.fr.wilda
spec:
  group: fr.wilda
  names:
    kind: HelloWorldCustomResource
    plural: helloworldcustomresources
    shortNames:
    - hw
    singular: helloworldcustomresource
  scope: Namespaced
  versions:
  - name: v1
    schema:
      openAPIV3Schema:
        properties:
          spec:
            properties:
              name:
                type: string
            type: object
          status:
            type: object
        type: object
    served: true
    storage: true
```

Plutôt sympa 😉.

### Définition du reconciler 🔄

Là encore ce n'est pas très compliqué, on peut coder des actions sur pas mal d'évènements : création, suppression ou modification de la _custom resource_ (CR).
Dans notre cas on veut juste loger _Hello world \<valeur du champ name de la CR\>_ :
```java
@ControllerConfiguration
public class HelloWorldReconciler implements Reconciler<HelloWorldCustomResource> {

  public static final String KIND = "HelloWorldCustomResource";

  public HelloWorldReconciler() {
  }

  @Override
  public DeleteControl cleanup(HelloWorldCustomResource resource, Context context) {
    System.out.println(String.format("Goodbye %s 😢", resource.getSpec().getName()));
    return DeleteControl.defaultDelete();
  }

  @Override
  public UpdateControl<HelloWorldCustomResource> reconcile(
    HelloWorldCustomResource resource, Context context) {
    System.out.println(String.format("Hello %s 🎉🎉 !!", resource.getSpec().getName()));

    return UpdateControl.updateResource(resource);
  }
}
```

A ce stade il ne nous reste plus qu'à _enregistrer_ notre reconciler au sein de Kubernetes.

```java
public class HelloWorldRunner {
    public static void main(String[] args) {
      Operator operator = new Operator(DefaultConfigurationService.instance());
      operator.register(new HelloWorldReconciler());

      System.out.println("🚀 Starting HelloWorld operator !!! 🚀");
      operator.start();
    }
  }
```

### Test de l'opérateur ⚗️

Pour que notre opérateur fonctionne il va falloir créer la CRD.

Créer la CRD : `kubectl apply -f ./target/classes/META-INF/fabric8/helloworldcustomresources.fr.wilda-v1.yml`
```
kubectl get crd --all-namespaces

NAME                                 CREATED AT
helloworldcustomresources.fr.wilda   2021-11-10T08:50:00Z
```

Ensuite lancer l'opérateur en local : 
```
mvn exec:java -Dexec.mainClass=fr.wilda.HelloWorldRunner
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< fr.wilda:simple-java-k8s-operator >------------------
[INFO] Building simple-java-k8s-operator 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ simple-java-k8s-operator ---
🚀 Starting HelloWorld operator !!! 🚀
```

⚠️ **Laisser tourner le main pour avoir les différents messages du reconciler !** ⚠️

Et il ne nous reste plus qu'à créer une CR pour voir si notre bel opérateur se déclenche !

Histoire d'être un peu propre on crée un namespace `kubectl create ns test-hw-crd`

Puis on va créer la CR suivante :

```yaml
apiVersion: "fr.wilda/v1"
kind: HelloWorldCustomResource
metadata:
  name: hello-world
  namespace: test-hw-crd
spec:
  name: stef
```

`kubectl apply -f ./src/test/resources/test_helloworld.yml -n test-hw-crd`

On vérifie que tout est ok :
```
kubectl get hw -n test-hw-crd

NAME          AGE  
hello-world   2m12s
```

Jetons un oeil sur la sortie standard de l'opérateur lancé tout à l'heure : 
```
mvn exec:java -Dexec.mainClass=fr.wilda.HelloWorldRunner
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< fr.wilda:simple-java-k8s-operator >------------------
[INFO] Building simple-java-k8s-operator 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ simple-java-k8s-operator ---
🚀 Starting HelloWorld operator !!! 🚀
Hello stef 🎉🎉 !!
```

Et voilà 😎 !

On supprime la CR : `kubectl delete hw hello-world -n test-hw-crd`

Et de nouveau sur la sortie standard de l'opérateur : 
```
mvn exec:java -Dexec.mainClass=fr.wilda.HelloWorldRunner
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< fr.wilda:simple-java-k8s-operator >------------------
[INFO] Building simple-java-k8s-operator 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:3.0.0:java (default-cli) @ simple-java-k8s-operator ---
🚀 Starting HelloWorld operator !!! 🚀
Hello stef 🎉🎉 !!
Goodbye stef 😢
```

![Wait a minute](wait-a-minute-393x295.jpg)

A ce stade vous devez vous dire : 
>ok il est gentil avec son exemple mais moi je veux un opérateur qui tourne dans mon Kubernetes et pas là en mode main sur un poste de dev !

Et vous avez raison !

### Packaging et déploiement de l'opérateur 📦

En réalité un opérateur n'est rien d'autre qu'une image dans un POD !

Il faut donc juste créer une image et la déployer dans notre cluster.
Ce n'est pas forcément l'objectif de cet article, du coup je ne vais pas m'étendre sur les différentes actions.
A noter que j'utilise l'image Docker [registry](https://hub.docker.com/_/registry) permettant la création d'une registry locale pour stocker les images.

#### Construction et push de l'image 🐳

Tout d'abord il nous faut un _Dockerfile_ tout simple : 
```dockerfile
FROM fabric8/java-alpine-openjdk11-jre

ENTRYPOINT ["java", "-jar", "/usr/share/operator/operator.jar"]

ARG JAR_FILE
ADD target/simple-java-k8s-operator-1.0-SNAPSHOT.jar /usr/share/operator/operator.jar
```

Il faut modifier notre packaging pour créer un _fat jar_ : 
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-assembly-plugin</artifactId>
  <version>3.3.0</version>
  <configuration>
    <descriptorRefs>
      <descriptorRef>jar-with-dependencies</descriptorRef>
    </descriptorRefs>
    <archive>
      <manifest>
      <mainClass>fr.wilda.HelloWorldRunner</mainClass>
      </manifest>
    </archive>
  </configuration>
  <executions>
    <execution>
    <id>make-assembly</id>
    <phase>package</phase> 
    <goals>
      <goal>single</goal>
    </goals>
    </execution>
  </executions>
</plugin>      
```

Ensuite construction et push dans la registry : 
```bash
docker build  -t localhost:5000/hw-operator:1.0 .

docker push localhost:5000/hw-operator:1.0
```

### Déploiement de l'opérateur 🤖

Pour cela j'ai créé un YAML complet très simple (à ne pas reproduire chez vous 😉). 

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: helloworld-operator

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: helloworld-operator
  namespace: helloworld-operator
spec:
  selector:
    matchLabels:
      app: helloworld-operator
  replicas: 1 
  strategy:
    type: Recreate 
  template:
    metadata:
      labels:
        app: helloworld-operator
    spec:
      containers:
      - name: operator
        image: localhost:5000/hw-operator:1.0
        imagePullPolicy: Always
```

Déploiement de l'opérateur : `kubectl apply -f ./src/k8s/operator.yml`

```
kubectl get pods -n helloworld-operator
NAME                                   READY   STATUS    RESTARTS   AGE

helloworld-operator-6ddf57b9bc-nwb5n   1/1     Running   0          15m
```

Retestons notre operator en affichant ses logs : 
```bash
kubectl logs helloworld-operator-6ddf57b9bc-nwb5n -n helloworld-operator

🚀 Starting HelloWorld operator !!! 🚀
```

Si on crée une CR (`kubectl apply -f ./src/test/resources/test_helloworld.yml -n test-hw-crd`) alors on obtient :
```bash
kubectl logs helloworld-operator-6ddf57b9bc-nwb5n -n helloworld-operator

🚀 Starting HelloWorld operator !!! 🚀
Hello stef 🎉🎉 !!
```

## En conclusion 🧐

On vient de voir comment, simplement (enfin avec un peu de YAML quand même !), on peut créer un opérateur Kubernetes en Java.
Le SDK actuel permet de faire déjà pas mal de choses, il demande à être enrichi mais cela permet de simplement créer de la logique métier dans l'opérateur et surtout le tester en local !

La suite : un opérateur qui fait  un peu plus de choses, par exemple créer un service ou un POD, ... mais aussi le faire en Quarkus et SpringBoot. 

L'ensemble des sources est disponible dans le projet GitHub [java-k8s-simple-operator](https://github.com/philippart-s/java-k8s-simple-operator).

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur le repository des [sources](https://github.com/philippart-s/java-k8s-simple-operator) ou de l'[article](https://github.com/philippart-s/blog).