---
title: "Créer un opérateur Kubernetes en Java ... C'est possible !"
classes: wide
categories:
  - Articles
  - Dev
tags:
  - K8s
  - Java

---
## Mais c'est quoi un opérateur

Lorsque l'on me parle d'un opérateur Kubernetes moi je pense à ça :
![Jarvis]({{ site.url }}{{ site.baseurl }}/assets/images/java-k8s-operator/jarvis.jpg){: .align-center}

ou à ça : 

![Matrix]({{ site.url }}{{ site.baseurl }}/assets/images/java-k8s-operator/matrix.jpg){: .align-center}


Je ne vais pas me lancer sur l'explication de ce qu'est un opérateur Kubernetes mais en gros c'est un contrôleur permettant d'étendre les API de Kubernetes afin de gérer de manière plus efficace les applications déployées (installation, actions d'adminsitration, ...).

Pour définir un opérateur il faut définir une _custom resource definition_ puis créer une _resouce definition_ associée.

Ensuite l'opérateur va scruter en permanence ce qu'il a à faire (selon ce que l'on aura codé) et est accessible via la CLI _kubectl_ puisque ce n'est qu'une extension de l'API de base.

<!-- TODO image boucle de réconciliation -->

En résumé : faisons faire par un programme des actions automatisables qui n'ont pas de plus value à être faites par des humains.
En gros c'est que l'on appelle partout DevOps ... 

## Ok, je suis expert opérateur maintenant 😅, comment on en développe un ?

Alors déjà faisons le point sur les deux grands types d'opérateurs : ceux sui se chargent essentiellement de l'installation et la mise à jour des applications et ceux qui vont plus loin pour proposer des actions d'administration / ops automatisées sur les applications.

On trouve souvent cela sous la dénomination _modèle de maturité des opérateurs_, illustré par le schéma suivant :
![Operator Capability]({{ site.url }}{{ site.baseurl }}/assets/images/java-k8s-operator/operator-capability-model.png){: .align-center}

Et là il y a un truc qui me chagrine car c'est soit du Helm, soit du Ansible ... soit du Go 🙄.

## Sinon ça existe dans un vrai langage 🤡 ?

Du coup, même si les quelques docs existantes ne le mentionnent pas (notamment celle de Kubernetes) j'ai recherché si il existait pas un projet qui se serait lancé dans l'aventure.

<!-- TODO image pour "à ne pas confondre -->

J'en profite pour clarifier un point qui peut paraître évident pour les sachants mais qui ne l'était pas pour moi au début : on peut écrire un opérateur dans n'importe quel langage !

Ce sera juste plus ou moins simple pour le créer avec plus ou moins d'aide : le _scaffolding d'un projet_, l'aide dans la génération des différentes ressources ou _custom resources_, les appels des API Kubernetes, ...

Voilà une fois que ça c'est dit on peut continuer et partir à la recherche d'un langage me permettant de faire ce que je veux, je parle bien sûr de Java :xink:.

## java-operator-sdk 🛠️

J'ai trouvé mon bonheur avec le projet [java-operator](https://github.com/java-operator-sdk/java-operator-sdk){:target="_blank"}.

La documentation officielle : [https://javaoperatorsdk.io/](https://javaoperatorsdk.io/){:target="_blank"}.

Ils se sont largement inspirés de celui écrit en Go ([https://github.com/operator-framework/operator-sdk](https://github.com/operator-framework/operator-sdk){:target="_blank"}) et ne s'en cachent pas. Il reste cependant pas mal de chemin avant d'arriver au niveau de celui-ci (pour moi la fonctionnalité la plus manquante étant le _scaffolding_) mais on verra un peu plus loin que ce qui est fournit aide grandement pour la création d'un opérateur.

Une chose importante à savoir est que le projet est basé sur le client Kubernetes [fabric8](https://github.com/fabric8io/kubernetes-client){:target="_blank"} qui facilite grandement la vie pour accéder aux API Kubernetes (et Openshift 😉).

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
  <version>1.9.11</version>
</dependency>

<!-- Dépendance pour générer les CRD 😎 -->
<dependency>
  <groupId>io.fabric8</groupId>
  <artifactId>crd-generator-apt</artifactId>
  <version>5.9.0</version>
  <scope>provided</scope>
</dependency>
```

### Le squelette du projet 🦴

C'est assez simple et la [documentation](https://github.com/java-operator-sdk/java-operator-sdk#Usage){:target="_blank"} est plutôt bien faite (voir la section sample et particulièrement le projet [pure-java](https://github.com/java-operator-sdk/java-operator-sdk/tree/master/samples/pure-java){:target="_blank"}).

#### Définition de la custom resource definition 📝

Comme indiqué il est possible de définir la partie _spec_ de la _custom resource definition_ (CRD) sous forme de POJO  : 

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

`mvn compile` génère dans le _target/classes/META-INF/fabric8 deux CRD : un version beta et un version normale (ils sont identiques au moment de la génération). 

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

### Définition du contrôleur 🔄

Là encore ce n'est pas très compliqué, on peut coder des actions sur pas mal d'évènements : création, suppression ou modification de la _custom resource_ (CR).
Dans notre cas on veut juste loger _Hello world <valeur du champ name de la CR>_ :
```java
@Controller
public class HelloWorldController implements ResourceController<HelloWorldCustomResource> {

  public static final String KIND = "HelloWorldCustomResource";

  public HelloWorldController() {
  }

  @Override
  public DeleteControl deleteResource(HelloWorldCustomResource resource, Context<HelloWorldCustomResource> context) {
    System.out.println(String.format("Goodbye %s 😢", resource.getSpec().getName()));
    return DeleteControl.DEFAULT_DELETE;
  }

  @Override
  public UpdateControl<HelloWorldCustomResource> createOrUpdateResource(
    HelloWorldCustomResource resource, Context<HelloWorldCustomResource> context) {
    System.out.println(String.format("Hello %s 🎉🎉 !!", resource.getSpec().getName()));

    return UpdateControl.updateCustomResource(resource);
  }
}
```

A ce stade il ne nous reste plus qu'à enregistrer notre controller au sein de Kubernetes.

```java
public class HelloWorldRunner {
    public static void main(String[] args) {
      Operator operator = new Operator(DefaultConfigurationService.instance());
      operator.register(new HelloWorldController());

      System.out.println("🚀 Starting HelloWorld operator !!! 🚀");
      operator.start();      
    }
  }
```

### Test de l'opérateur ⚗️

A ce stade pour que notre opérateur fonctionne il va falloir créer la CRD.

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

⚠️ **Laisser tourner le main pour avoir les différents messages du contrôleur ! ⚠️

Et il ne nous reste plus qu'à créer un CR pour vois si notre bel opérateur se déclenche !

Histoire d'être un peu propre on crée un namespace `kubectl create ns test-hw-crd`

Puis on va créé la CR suivante :

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

Jetons un oeuil sur la sortie standard de l'opérateur lancé tout à l'heure : 
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

Si on supprime la CR : `kubectl delete hw hello-world -n test-hw-crd`

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

<!-- Image Wait a minute !! -->

A ce stade vous devez vous dire : 
>ok il est gentil avec son exemple mais moi je veux un opérateur qui tourne dans mon Kubernetes et pas là en mode main sur un poste de dev !

Et vous avez raison !

### Packaging et déploiement de l'opérateur 📦

En réalité un opérateur n'est rien d'autre qu'une image dans un POD !

Il faut donc juste créer une image et la déployer dans notre cluster.
Ce n'est pas forcément l'objectif de cet article, à noter que j'utilise l'image Docker [registry](https://hub.docker.com/_/registry){:target="_blank"} permettant la création d'une registry locale pour stocker les images.

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

Pour cela j'ai créé un YAML complet (à ne pas reproduire chez vous 😉). 
<!-- TODO expliquer la partie RBAC / SA --> 

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
      serviceAccount: helloworld-operator 
      containers:
      - name: operator
        image: localhost:5000/hw-operator:1.0
        imagePullPolicy: Always

---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: helloworld-operator
  namespace: helloworld-operator

---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRole
metadata:
  name: helloworld-operator
rules:
- apiGroups:
  - fr.wilda.helloworldcustomresources
  resources:
  - schemas
  verbs:
  - "*"
- apiGroups:
  - fr.wilda.helloworldcustomresources
  resources:
  - schemas/status
  verbs:
  - "*"
- apiGroups:
  - apiextensions.k8s.io
  resources:
  - customresourcedefinitions
  verbs:
  - "get"
  - "list"
- apiGroups:
  - ""
  resources:
  - secrets
  verbs:
  - "*"

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: helloworld-operator
subjects:
- kind: ServiceAccount
  name: helloworld-operator
  namespace: helloworld-operator
roleRef:
  kind: ClusterRole
  name: helloworld-operator
  apiGroup: ""
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

On vient de voir comment simplement (enfin avec un peu de YAML quand même !) on peut créer un opérateur Kubernetes en Java.
Le SDK actuel permet de faire déjà pas mal de choses, il demande à être enrichi mais cela permet de simplement créer de la logique métier dans l'opérateur et surtout le tester en local !

La suite : un opérateur qui fait  un peu plus de choses, par exemple créer un service ou un POD, ... mais aussi le faire un Quarkus et SpringBoot. 

L'ensemble des sources est disponible dans le projet GitHub [java-k8s-simple-operator](https://github.com/philippart-s/java-k8s-simple-operator){:target="_blank"}.

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur le repository des [sources](https://github.com/philippart-s/java-k8s-simple-operator){:target="_blank"} ou de l'[article](https://github.com/philippart-s/blog){:target="_blank"}.

