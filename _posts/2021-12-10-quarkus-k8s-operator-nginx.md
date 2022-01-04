---
title: "Créer un opérateur Kubernetes avec Quarkus pour gérer une instance Nginx."
classes: wide
excerpt: "Après l'opérateur pur Springboot, voici le même opérateur mais avec Quarkus."
categories:
  - Articles
  - Dev
tags:
  - K8s
  - Java
  - Springboot

---

![quarkus hub]({{ site.url }}{{ site.baseurl }}/assets/images/quarkus-k8s-operator/quarkus-operator.png){: .align-center}

Quatrième et denière partie de la série d'articles consacrés à l'écriture d'un opérateur Kubernetes en java.

Pour ne pas réécrire ce qui a déjà été fait je vous propose de parcourir les troisautres articles avant de lire celui-ci.
J'y abirde dans le détails des éléments que je n'expliquerai pas danscelui-ci 😉.

Les articles en question :
 - [Créer un opérateur _Hello World_ Kubernetes en pur Java]({{ site.baseurl }}{% post_url 2021-11-09-java-k8s-operator %}) 
 - [Créer un opérateur _Nginx_ Kubernetes en pur Java]({{ site.baseurl }}{% post_url 2021-11-21-java-k8s-operator-nginx %}) 
 - [Créer un opérateur _Nginx_ Kubernetes avec Springboot]({{ site.baseurl }}{% post_url 2021-12-02-springboot-k8s-operator-nginx %}) 

> Notez bien : 
> Je n'utilise pas (encore) Quarkus et ne connais donc pas très bien son fonctionnement.
> L'objectif, ici, est de faire le tour complet de ce que permet de [SDK Java](https://javaoperatorsdk.io/){:target="_blank"} pour créer un opérateur Kubernetes.


## Rappel des épisodes précédents 💬

Si vous avez bien lu les articles précédents vous avez pu vous rendre compte que l'on va vers de plus en plus de simplicité pourécrire nos opérteurs.
L'idée était de commencer avec du pur Java pour comprendre ce qui se tramait avant d'utiliser des aides comme Springboot ou Quarkus.

Il n'empêche que quelque soit le mode utilisé c'est toujours la même histoire : 
 - on définit la **C**ustom **R**esource **D**efinition (CRD) sous forme de POJOs 
 - puis on code notre controleur dans notre opérateur
 - on lance notre opérateur en CLI ou via une image (exécutée dans un POD)
 - on crée une **C**ustom **R**esource se basant sur notre CRD 

... et notre opérateur fait son boulot !

A noter que toutes les manipulations Kubernetes se font via la lib fourni par  [fabric8](https://github.com/fabric8io/kubernetes-client/){:target="_blank"} 

## Quarkus a t il tué le game ? 💀

La simplicité avec laquelle l'extension Quarkus permet de créer et initialiser les classes nécessaires pour développer les opérateurs est presque indécente !

### Initialisation du projet 🛠️

Rien de plus simple, les équipes de Quarkus ont tout prévu: se connecter au site [https://code.quarkus.io](https://code.quarkus.io){:target="_blank"} puis choisir l'extension _quarkus-operator-sdk_.

Avec ça on a un beau projet tout neuf avec les bonnes dépendances dans notre _pom.xml_ prêt à faire deu !

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
On ne se concentre que sur les éléments de l'opérateur en lui même:

 - le contrôleur
 - la définition de la custom resource

Et c'est tout, l'extension se débrouille pour enregistrer notre opérateur et l'injection de dépendances ([CDI](https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html){:target="_blank"}) fait le reste.

> Bien sûr il est possible d'ajouter des configurations propres à Quarkus ou les différents éléments utilisés pour générer les images par exemple.
Pour cela c'est dans le fichier _application.properties_ que tout se passe.

La custom resource ne change pas par rapport aux autre articles :
```java
@Group("fr.wilda")
@Version("v1")
@ShortNames("ngi")
public class NginxInstallerResource extends CustomResource<NginxInstallerSpec, Void> implements Namespaced {
    
}
```

```java
ublic class NginxInstallerSpec {

    private Integer replicas;

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
```

## Conclusion 🧐

L'ensemble des sources est disponible dans le projet GitHub [quarkus-k8s-nginx-operator](https://github.com/philippart-s/quarkus-k8s-nginx-operator){:target="_blank"}.

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur le repository des [sources](https://github.com/philippart-s/quarkus-k8s-nginx-operator){:target="_blank"} ou de l'[article](https://github.com/philippart-s/blog){:target="_blank"}.

 