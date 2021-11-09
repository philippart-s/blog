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

Du coup, même si les quelques docs existantes ne le mentionnent pas (notamment celle de Kubernetes) j'i recherché si il existait pas un projet qui se serait lancé dans l'aventure.

Ayant entendu que Red Hat travaillait sur la possibilité d'écrire un opérateur en Java (avec Quarkus) j'avais bon espoir !



<!-- TODO image pour "à ne pas confondre -->

