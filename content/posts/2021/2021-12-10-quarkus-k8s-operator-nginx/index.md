---
title: "Créer un opérateur Kubernetes avec Quarkus pour gérer une instance Nginx."
description: "Après l'opérateur pur Springboot, voici le même opérateur mais avec Quarkus."
link: /2021-12-10-quarkus-k8s-operator-nginx
tags: 
  - K8s
  - Java
  - Springboot
image: quarkus-operator.png
figCaption: "@wildagsx"
author: wilda
---

Quatrième et dernière partie de la série d'articles consacrés à l'écriture d'un opérateur Kubernetes en java.

Pour ne pas réécrire ce qui a déjà été fait je vous propose de parcourir les trois autres articles avant de lire celui-ci.
J'y aborde dans le détail des éléments que je n'expliquerai pas dans celui-ci 😉.

Les articles en question :
 - [Créer un opérateur _Hello World_ Kubernetes en pur Java](/2021-11-09-java-k8s-operator) 
 - [Créer un opérateur _Nginx_ Kubernetes en pur Java](/2021-11-21-java-k8s-operator-nginx) 
 - [Créer un opérateur _Nginx_ Kubernetes avec Springboot](/2021-12-02-springboot-k8s-operator-nginx) 

> Notez bien : 
> Je n'utilise pas (encore) Quarkus et ne je connais donc pas très bien son fonctionnement.
> L'objectif, ici, est de faire le tour des différentes options que permet le [SDK Java](https://javaoperatorsdk.io/) pour créer un opérateur Kubernetes.


## Rappel des épisodes précédents 💬

Si vous avez bien lu les articles précédents, vous avez pu vous rendre compte que l'on va vers de plus en plus de simplicité pour écrire nos opérateurs.
L'idée était de commencer avec du pur Java pour comprendre ce qui se tramait avant d'utiliser des aides comme Springboot ou Quarkus.

Il n'empêche que, quelque soit le mode utilisé, c'est toujours la même histoire : 
 - on définit la **C**ustom **R**esource **D**efinition (CRD) sous forme de POJOs 
 - puis on code notre contrôleur de notre opérateur
 - on lance notre opérateur en CLI ou via une image (exécutée dans un POD)
 - on crée une **C**ustom **R**esource (CR) se basant sur notre CRD 

... et notre opérateur fait son boulot !

A noter que toutes les manipulations Kubernetes se font via la lib fournie par  [fabric8](https://github.com/fabric8io/kubernetes-client/) 

## Quarkus a-t-il tué le game ? 💀

La simplicité avec laquelle l'extension Quarkus permet de créer et initialiser les classes nécessaires pour développer les opérateurs est presque indécente !

### Initialisation du projet 🛠️

Rien de plus simple, les équipes de Quarkus ont tout prévu: se connecter au site [https://code.quarkus.io](https://code.quarkus.io) puis choisir l'extension _quarkus-operator-sdk_.

Avec ça on a un beau projet tout neuf avec les bonnes dépendances dans notre _pom.xml_ prêt à faire feu !

Extrait du _pom.xml_ : 
```xml
<dependency>
  <groupId>io.quarkiverse.operatorsdk</groupId>
  <artifactId>quarkus-operator-sdk</artifactId>
  <version>2.0.1</version>
</dependency>
 ```
### Développement de l'opérateur 📝

Eh bien c'est certainement là que Quarkus tue le game ... 
On ne se concentre que sur les éléments de l'opérateur en lui-même:

 - le contrôleur
 - la définition de la custom resource

Et c'est tout, l'extension se débrouille pour enregistrer notre opérateur et l'injection de dépendances ([CDI](https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html)) fait le reste.

>Bien sûr, il est possible d'ajouter des configurations propres à Quarkus ou les différents éléments utilisés pour générer les images par exemple.
>Pour cela c'est dans le fichier _application.properties_ que tout se passe.

La custom resource ne change pas par rapport aux autres articles :
```java
@Group("fr.wilda")
@Version("v1")
@ShortNames("ngi")
public class NginxInstallerResource extends CustomResource<NginxInstallerSpec, Void> implements Namespaced {
    
}
```

```java
public class NginxInstallerSpec {

    private Integer replicas;

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
```

Et le contrôleur non plus ... 😎
```java
@Controller
public class NginxInstallerController implements ResourceController<NginxInstallerResource> {
  
    // K8S API utility
    private KubernetesClient k8sClient;
    // Watcher to do some actions when events occurs
    private Watch watch = null;
    
    public NginxInstallerController(KubernetesClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    @Override
    public UpdateControl<NginxInstallerResource> createOrUpdateResource(NginxInstallerResource resource,
            Context<NginxInstallerResource> context) {
        System.out.println("🛠️  Create / update Nginx resource operator ! 🛠️");

        String namespace = resource.getMetadata().getNamespace();

        // Load the Nginx deployment
        Deployment deployment = loadYaml(Deployment.class, "/k8s/nginx-deployment.yml");

        // Apply the number of replicas
        deployment.getSpec().setReplicas(resource.getSpec().getReplicas());
        deployment.getMetadata().setNamespace(namespace);
        // Create or update the modifications
        k8sClient.apps().deployments().inNamespace(namespace).createOrReplace(deployment);

        // Watch events on the Nginx deployment
        watch = k8sClient.apps().deployments().withName(deployment.getMetadata().getName())
                .watch(new Watcher<Deployment>() {
                    @Override
                    public void eventReceived(Action action, Deployment resource) {
                        System.out.println("⚡ Event receive on watcher ! ⚡ ➡️ " + action.name());

                        if (action == Action.DELETED) {
                            System.out.println("🗑️  Deployment deleted, recreate it ! 🗑️");
                            k8sClient.apps().deployments().inNamespace(resource.getMetadata().getNamespace())
                                    .createOrReplace(deployment);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        System.out.println("☠️ Watcher closed due to unexpected error : " + cause);
                    }
                });

        // Create service
        Service service = loadYaml(Service.class, "/k8s/nginx-service.yml");
        k8sClient.services().inNamespace(namespace).createOrReplace(service);

        return UpdateControl.updateCustomResource(resource);
    }

    @Override
    public DeleteControl deleteResource(NginxInstallerResource resource, Context<NginxInstallerResource> context) {
        System.out.println("💀 Delete Nginx resource operator ! 💀");

        // Avoid the automatic recreation
        if (watch != null) watch.close();
        // Delete deployment and its PODs
        k8sClient.apps().deployments().inNamespace(resource.getMetadata().getNamespace()).delete();
        // Delete the service
        k8sClient.services().inNamespace((resource.getMetadata().getNamespace())).delete();

        return ResourceController.super.deleteResource(resource, context);
    }

    /**
     *  Load a YAML file and transform it to a Java class.
     * 
     * @param clazz The java class to create
     * @param yamlPath The yaml file path in the classpath
     */
    private <T> T loadYaml(Class<T> clazz, String yamlPath) {
        try (InputStream is = getClass().getResourceAsStream(yamlPath)) {
          return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
          throw new IllegalStateException("Cannot find yaml on classpath: " + yamlPath);
        }
    }
}
```
Comme je l'ai indiqué, CDI se charge de nous créer l'instance de la classe fabric8 permettant la manipulation des ressources et commandes Kubernetes.
La vie est bien faite quand même !

### Exécution de l'opérateur 🤖

#### Dans l'IDE 💻

Comme pour les autres articles l'exécution dans l'IDE va nous permettre de tester plus rapidement mais aussi de mettre des points d'arrêt !
```bash
mvn compile quarkus:dev

Listening for transport dt_socket at address: 5005
2021-12-10 11:19:25,071 INFO  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) Registered 'fr.wilda.resource.NginxInstallerResource' for reflection
2021-12-10 11:19:25,093 INFO  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) Registered 'fr.wilda.resource.NginxInstallerSpec' for reflection
2021-12-10 11:19:25,094 INFO  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) Registered 'java.lang.Void' for reflection
2021-12-10 11:19:25,221 WARN  [io.fab.crd.gen.CustomResourceInfo] (build-15) Cannot reliably determine status types for fr.wilda.resource.NginxInstallerResource because it isn't parameterized with only spec and status types. Status replicas detection will be deactivated.
2021-12-10 11:19:25,230 INFO  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) Processed 'fr.wilda.controller.NginxInstallerController' controller named 'nginxinstallercontroller' for 'nginxinstallerresources.fr.wilda' CR (version 'fr.wilda/v1')
2021-12-10 11:19:25,240 INFO  [io.fab.crd.gen.CRDGenerator] (build-15) Generating 'nginxinstallerresources.fr.wilda' version 'v1' with fr.wilda.resource.NginxInstallerResource (spec: fr.wilda.resource.NginxInstallerSpec / status undetermined)...

2021-12-10 11:19:26,240 WARN  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) 'nginxinstallercontroller' controller is configured to watch all namespaces, this requires a ClusterRoleBinding for which we MUST specify the namespace of the operator ServiceAccount. However, at this information is not known at build time, we are leaving it blank and needs to be provided by the user by editing the 'nginxinstallercontroller-cluster-role-binding' ClusterRoleBinding to provide the namespace in which the operator will be deployed.
2021-12-10 11:19:26,381 WARN  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) 'nginxinstallercontroller' controller is configured to watch all namespaces, this requires a ClusterRoleBinding for which we MUST specify the namespace of the operator ServiceAccount. However, at this information is not known at build time, we are leaving it blank and needs to be provided by the user by editing the 'nginxinstallercontroller-cluster-role-binding' ClusterRoleBinding to provide the namespace in which the operator will be deployed.
2021-12-10 11:19:26,487 WARN  [io.qua.ope.dep.OperatorSDKProcessor] (build-15) 'nginxinstallercontroller' controller is configured to watch all namespaces, this requires a ClusterRoleBinding for which we MUST specify the namespace of the operator ServiceAccount. However, at this information is not known at build time, we are leaving it blank and needs to be provided by the user by editing the 'nginxinstallercontroller-cluster-role-binding' ClusterRoleBinding to provide the namespace in which the operator will be deployed.
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2021-12-10 11:19:28,068 INFO  [io.jav.ope.Operator] (Quarkus Main Thread) Registered Controller: 'nginxinstallercontroller' for CRD: 'class fr.wilda.resource.NginxInstallerResource' for namespace(s): [all namespaces]

2021-12-10 11:19:28,069 INFO  [io.qua.ope.run.AppEventListener] (Quarkus Main Thread) Quarkus Java Operator SDK extension 2.0.1 (commit: eac130a on branch: eac130ab44f3686189f252b021e8f2ba572c98ca) built on Mon Nov 15 15:09:13 CET 2021
2021-12-10 11:19:28,070 INFO  [io.jav.ope.Operator] (Quarkus Main Thread) Operator SDK 1.9.11 (commit: 0287c0c) built on Fri Oct 29 18:12:51 CEST 2021 starting...
2021-12-10 11:19:28,070 INFO  [io.jav.ope.Operator] (Quarkus Main Thread) Client version: 5.10.1
2021-12-10 11:19:28,306 INFO  [io.jav.ope.Operator] (Quarkus Main Thread) Server version: 1.21
2021-12-10 11:19:28,512 INFO  [io.quarkus] (Quarkus Main Thread) nginxoperator 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.5.1.Final) started in 5.962s. Listening on: http://localhost:8080
2021-12-10 11:19:28,515 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2021-12-10 11:19:28,515 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, kubernetes, kubernetes-client, openshift-client, operator-sdk, smallrye-context-propagation, smallrye-health, vertx] 
```
Et là on voit bien comment le SDK, couplé à l'extension Quarkus, nous mâche le travail pour enregistrer les différents éléments auprès de Kubernetes (ils m'indiquent aussi que j'ai fait des trucs pas très sécure 😅).

Puis on peut jouer avec nos créations / mises à jour ou suppressions de _custom resource_ : `kubectl apply -f ./src/test/resources/test_nginx.yml -n test-nginx-operator` et `kubectl delete ngi/nginx-installer  -n test-nginx-operator`.

```bash
🛠️ Create / update Nginx resource operator ! 🛠️
⚡ Event receive on watcher ! ⚡ ➡️ ADDED
⚡ Event receive on watcher ! ⚡ ➡️ MODIFIED
⚡ Event receive on watcher ! ⚡ ➡️ MODIFIED
💀 Delete Nginx resource operator ! 💀
```

#### Dans Kubernetes 🐳

Là encore, merci Quarkus puisque la fabrication de mon image se résume par la commande : `mvn clean package -Dquarkus.container-image.build=true`.
Ensuite il suffit d'appliquer simplement le _deployment.yml_ pour déployer notre opérateur : 
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: nginx-operator
---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-operator
  namespace: nginx-operator
spec:
  selector:
    matchLabels:
      app: nginx-operator
  replicas: 1 
  strategy:
    type: Recreate 
  template:
    metadata:
      labels:
        app: nginx-operator
    spec:
      containers:
      - name: operator
        image: localhost:5000/nginx-operator
        imagePullPolicy: Always
```

Et c'est fini, l'opérateur est déployé et n'attends plus que vos _custom resources_ pour agir.

## Conclusion 🧐

Comme je l'ai indiqué en préambule je n'avais jamais _joué_ avec Quarkus (ce n'est pas l'envie qui manquait) et j'ai été vraiment bluffé par la simplicité pour créer notre opérateur.
Clairement on sent que l'extension Quarkus est plus suivie et a plus d'activité que les autres (pur java / Springboot) mais comment leur en vouloir 😉.

Je n'ai pas été dans les méandres de toutes les configurations qu'offrent Quarkus et l'extension.
Elles sont nombreuses et permettent vraiment d'aller plus loin sur ce qui est fait en automatique par le SDK (par exemple au niveau des CRD ou encore de l'image en elle-même) et je vous laisse aller voir les documentations qui sont plutôt complètes !

Pour ma part, cela m'a vraiment donné envie d'aller plus loin et de suivre les évolutions de ce projet avec une v2 qui s'annonce très prometteuse !

Ma série d'articles sur comment écrire un opérateur en Java est terminée.
Merci d'avoir pris le temps de me lire et peut être que de nouveaux articles verront le jour avec la v2 du SDK ... qui sait !

L'ensemble des sources est disponible dans le projet GitHub [quarkus-k8s-nginx-operator](https://github.com/philippart-s/quarkus-k8s-nginx-operator).

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur le repository des [sources](https://github.com/philippart-s/quarkus-k8s-nginx-operator) ou de l'[article](https://github.com/philippart-s/blog).