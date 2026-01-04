---
title: "Créer un opérateur Kubernetes en Springboot pour gérer une instance Nginx."
description: "Après l'opérateur pur Java, voici le même opérateur mais avec Springboot."
link: /2021-12-02-springboot-k8s-operator-nginx
tags: 
  - K8s
  - Java
  - Springboot
image: springboot-logo.png
figCaption: "@wildagsx"
author: wilda
---

> 💡 Mise à jour : suite à la release 2.x du SDK j'ai mis à jour l'article et le code 😉 



Troisième partie de la série d'articles consacrés à l'écriture d'un opérateur Kubernetes en java.
Afin de ne pas faire trop de redites je vais passer sur certaines choses, je vous propose donc, de lire les premiers articles pour commencer :
 - [Créer un opérateur _Hello World_ Kubernetes en pur Java](/2021-11-09-java-k8s-operator) 
 - [Créer un opérateur _Nginx_ Kubernetes en pur Java](/2021-11-21-java-k8s-operator-nginx) 

## Springboot et la magie de l'auto configuration 💫

Depuis quelques années je pense que peu de personnes utilisant Java ne connaissent pas [Spring](https://spring.io/) et son éco-système.
Le nombre de sous projets étant particulièrement important on a vite fait de se perdre.
C'est là où [Springboot](https://spring.io/projects/spring-boot) nous aide dans l'utilisation de ces différents projets.
Mon objectif n'est pas d'expliquer ce qu'est Springboot mais simplement de marier l'auto configuration Spring avec le développement d'un opérateur Kubernetes en Java.

## On prend les mêmes 

Je ne vais donc aborder que les changements entre le code source d'un opérateur pur Java et celui codé avec Springboot.
Tout d'abord, le code source d'origine : [https://github.com/philippart-s/java-k8s-nginx-operator](https://github.com/philippart-s/java-k8s-nginx-operator) (voir l'[article](/2021-11-21-java-k8s-operator-nginx) en rapport pour son explication).

On repart aussi avec le [SDK Java](https://javaoperatorsdk.io/) des articles précédents qui met à disposition un [starter Springboot](https://github.com/java-operator-sdk/operator-framework-spring-boot-starter).

> On voit clairement que la partie Springboot n'est pas une priorité de l'équipe projet : 
 - elle date de plusieurs mois
 - n'est quasiment pas documentée
 - pas totalement fonctionnelle (pour les tests mais là ça peut venir de moi 😅)

Cela peut s'expliquer, qu'à contrario, la partie [Quarkus](https://github.com/quarkiverse/quarkus-operator-sdk) semble plus active (on verra ça dans un prochain article 😉).

### Un peu de configuration 🛠️

Springboot a beau faire de la magie il faut tout de même déclarer quelques dépendances :
```xml
<!-- Core Springboot dependencies -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-test</artifactId>
	<scope>test</scope>
</dependency>

<!-- Springboot starter for SDK -->
<dependency>
	<groupId>io.javaoperatorsdk</groupId>
	<artifactId>operator-framework-spring-boot-starter</artifactId>
	<version>2.0.1</version>
</dependency>
<dependency>
	<groupId>io.javaoperatorsdk</groupId>
	<artifactId>operator-framework-spring-boot-starter-test</artifactId>
	<version>2.0.1</version>
</dependency>
```

Et c'est tout, le reste ne change pas !

Il est possible de configurer certains éléments avec un `application.yml`, par exemple le nombre de fois où le contrôleur essaie d'effectuer ses actions en cas d'erreurs : 
```yaml
javaoperatorsdk:
  controllers:
    customservicecontroller:
      retry:
        maxAttempts: 3
```
Personnellement pour mon test je n'ai pas positionné de configuration particulière, d'autant qu'aucune documentation explique quel type de configuration on peut positionner.
On peut positionner les _credentials_ pour le client Kubernetes par exemple, mais pour avoir le nom des clefs il faut directement aller voir dans le code ... 

Quand je parlais de priorité : pas de documentation ni de commit depuis des mois.
Du coup ça me donne une idée, je vais certainement leur proposer de rédiger un peu de doc.
Je verrai bien si cela intéresse l'équipe de documenter la partie Springboot.
### Les adaptations à effectuer dans le code 📝

#### Le point d'entrée principal

La classe `NginxInstallerRunner` est remplacée par la classe décorée par l'annotation `@SpringBootApplication`: 
```java
@SpringBootApplication
public class NginxOperatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(NginxOperatorApplication.class, args);
	}
}
```

#### Le controller 🤖

Là c'est encore plus simple, on prend exactement la même classe que l'on annote comme étant un composant (`@Component`) Springboot:
```java
@ControllerConfiguration
@Component
public class NginxInstallerReconciler implements Reconciler<NginxInstallerResource> {
  // Le reste du code ne change pas !
}
```

Et c'est tout ! 

Tout le reste ne change pas et l'opérateur fonctionne toujours de la même façon.

On crée la CRD et la CR : 
```bash
kubectl apply -f ./target/classes/META-INF/fabric8/nginxinstallerresources.fr.wilda-v1.yml

kubectl apply -f ./src/test/resources/test_nginx.yml -n test-nginx-operator

kubectl delete ngi/nginx-installer  -n test-nginx-operator
```

```bash
mvn exec:java -Dexec.mainClass=fr.wilda.nginxoperator.NginxOperatorApplication


  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.6.1)

2021-12-02 14:13:12.458  INFO 3135 --- [lication.main()] f.w.n.NginxOperatorApplication           : Starting NginxOperatorApplication using Java 11.0.11 on NB101738SPC3700 with PID 
2021-12-02 14:13:12.464  INFO 3135 --- [lication.main()] f.w.n.NginxOperatorApplication           : No active profile set, falling back to default profiles: default
2021-12-02 14:13:15.727  INFO 3135 --- [lication.main()] io.javaoperatorsdk.operator.Operator     : Registered Controller: 'nginxinstallercontroller' for CRD: 'class fr.wilda.nginxoperator.resource.NginxInstallerResource' for namespace(s): [all namespaces]
2021-12-02 14:13:15.814  INFO 3135 --- [lication.main()] f.w.n.NginxOperatorApplication           : Started NginxOperatorApplication in 4.405 seconds (JVM running for 22.302)
🛠️   Create / update Nginx resource operator ! 🛠️
⚡ Event receive on watcher ! ⚡ ➡️ ADDED
⚡ Event receive on watcher ! ⚡ ➡️ MODIFIED
⚡ Event receive on watcher ! ⚡ ➡️ MODIFIED
⚡ Event receive on watcher ! ⚡ ➡️ MODIFIED
💀 Delete Nginx resource operator ! 💀
```

Je ne m'étendrai pas sur la partie création de l'image et déploiement dans Kubernetes car c'est exactement la même chose que dans l'[article](/2021-11-21-java-k8s-operator-nginx) précédent, cela utilise le plugin maven [jib](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

## Conclusion 🧐

Cet article est beaucoup plus court, mais c'était aussi beaucoup plus simple (merci à la magie de l'auto configuration Springboot).

Je reste mitigé sur l'apport de Springboot pour un tel projet.
Le projet ne semble pas très actif (mais a t on besoin de plus ?) et je ne sais pas trop ce que cela va devenir.
L'autre problématique est que je n'ai pas fait fonctionner la partie tests avec, notamment, l'utilisation de _fabric8_.
Je pense que dans ce cas c'est certainement qu'il faut que je mocke cette partie, j'essaierai de faire ça un peu plus tard.

En résumé, je conseillerai aux fans de Spring de l'utiliser afin de retrouver leur framework préféré.
Quant aux autres, du pur Java ou du Quarkus devrait les combler.

L'ensemble des sources est disponible dans le projet GitHub [springboot-k8s-nginx-operator](https://github.com/philippart-s/springboot-k8s-nginx-operator).

Merci de m'avoir lu et si vous avez vu des coquilles n'hésitez pas à me l'indiquer sur le repository des [sources](https://github.com/philippart-s/springboot-k8s-nginx-operator) ou de l'[article](https://github.com/philippart-s/blog).