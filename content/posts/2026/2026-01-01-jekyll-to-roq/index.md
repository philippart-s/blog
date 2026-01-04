---
title: "👹 Migrer son blog Jekyll vers Quarkus Roq ⚡️"
description: Comment et pourquoi migrer son blog Jekyll vers Quarkus Roq
link: /2026-01-01-jekyll-to-roq
image: roq-logo.jpg
figCaption: © ROQ
tags: 
  - Code
  - Blog
author: wildagsx
---

## 📖 TL;DR
>💡 [ROQ](https://iamroq.com/) est une extension Quarkus qui permet de générer un site statique (comme [Jekyll](https://jekyllrb.com/) par exemple) à partir de Markdown et Java  
>👤 Son créateur est [Andy Damevin](https://github.com/ia3andy).     
>🐙 Son [repository](https://github.com/quarkiverse/quarkus-roq) GitHub et sa [documentation officielle](https://iamroq.com/).   
>🎯 Dans ce post, vous trouverez comment j'ai migré mon blog Jekyll sous ROQ.  
>🐙 Le [code source](https://github.com/philippart-s/blog) de mon blog avec ROQ

<br/>

# 🤔 Mais pourquoi migrer son blog Jekyll vers ROQ ?

La première raison est que c'est en Java et j'avoue que je suis une _fashion victim_ des nouveautés Java 🤩.
L'autre raison est que j'avais développé la génération de mes pages liées à mes conférences en Ruby pour Jekyll (voir [🧪 Remplacer Noti.st par du as code avec Jekyll 💎]({site.url}/2025-06-02-goodbye-notist)), pouvoir le faire et le maintenir en Java était un vrai plus pour moi.
On le verra plus tard mais cela m'a permis aussi de rentrer dans le code d'une extension Quarkus 🧑‍💻.

Et puis, a-t-on vraiment besoin d'une raison valable pour démarrer un side project ? 🙃

## ✅ Ce que devait permettre ROQ

Qui dit migration, dit fonctionnalités à reprendre !
Je n'avais pas beaucoup customisé mon blog Jekyll mails fallait absolument retrouver : 
 * 📜 la possibilité de générer les pages de blog pour une conférence donnée à partir de données stockées dans un YAML
 * 🗂️ générer les pages _chapeaux_ pour les talks (liste des conférences où le talk a été donné) et les conférences (liste des conférences par années)
 * 🧩 permettre d'avoir du templating pour préparer des squelettes de blog posts
 * 📝 permettre l'utilisation de Markdown

# 🧑‍💻 Faire une PR sur une extension Quarkus

Eh oui, ce serait trop simple si tout se déroulait comme prévu 😅.
Pour ma version Jekyll, j'utilisais un fichier YAML par année de conférences. 
Mais ROQ ne permettait que d'utiliser qu'un seul fichier par type de données (par exemple un fichier pour toutes les données sur les conférences et non un fichier par année de conférence).  
Après des échanges avec les committers du projet pour valider qu'avoir ce genre d'évolution rentrait bien dans ce qu'ils souhaitaient pour ROQ, me voila à récupérer le code de ROQ pour proposer ma PR.  
Bon, on ne va pas se mentir : rentrer dans une extension Quarkus avec du Qute pour la gestion du templating n'est pas forcément une chose aisée 😅.  
Mais avec de la persévérance et de l'aide précieuse des membres du projet j'ai fini par proposer la PR et elle a été accéptée et intégrée dans ROQ 🥳.

Du coup maintenant, on peut avoir dans ROQ ce genre d'arborescence pour les data 🥳 :
```bash
data
  | conferences
    |_ 2022.yml
    |_ 2023.yml
    |_ 2024.yml
    |_ 2025.yml
```

Premier problème réglé ✅, on peut retourner à la migration.

# 🗺️ Migration de l'architecture du site

Là, c'est plutôt une bonne surprise : ROQ est assez permissif et pour les choses à respecter c'est très proche de Jekyll.
J'ai choisi d'avoir un répertoire par post, le répertoire ayant la date du post 🗓️ : `DD-MM-YYYY-titre`.

J'aurais pu conserver la façon de Jekyll et que ce soit le nom du fichier Markdown mais faire un répertoire me permet aussi de mettre les ressources statiques 🖼️ (images, vidéos, ...) au plus proche du post.
J'ai créé ensuite un répertoire par année, pour avoir une lecture plus claire des sources.
Cette sous arborescence n'a pas d'incidence sur le rendu final, seule la date du nom de répertoire impacte la façon dont l'article est rendu.
Toujours dans un souci de lecture des sources, j'ai créé un sous répertoire `conferences` qui sera le réceptacle des pages conférences générées.

Pour le reste, c'est assez simple : 
 - 🎨 il y a un répertoire `public` où l'on peut mettre les scripts JavaScript, CSS et autres ressources statiques globales au site.
 - 🧑‍💻 un répertoire `src` contenant tout le code Java

Pour plus de détails sur l'organisation type d'un projet ROQ je vous conseille la section [directory structure](https://iamroq.com/docs/basics/#directory-structure) de la documentation.

# 🎨 Front matter

La bonne nouvelle est que ROQ, comme beaucoup de générateurs de sites statiques, utilise la notion de `front matter` pour rajouter des métadonnées dans les fichiers Markdowns.
Si la syntaxe est différente pour certains champs, l'esprit reste le même.
J'ai juste dû faire le mapping entre certains champs comme `excerpt` qui devient `description` par exemple.

## 🧩 Qute pour le templating

En plus du front matter ROQ vient avec le moteur de templating [Qute](https://quarkus.io/guides/qute-reference). 
Les deux réunis permettent d'ajouter toute la généricité nécessaire pour ne pas trop avoir à faire de copier/coller dans les articles.

Pour plus de détails sur ces notions vous pouvez consulter la section [Qute and FontMatter](https://iamroq.com/docs/basics/#qute-and-frontmatter) de la documentation.

Tant que l'on parle de Qute et templating, j'ai donc dû porter le template que j'avais créé pour Jekyll : le template qui permet de mettre le détail des talks que j'ai donnés à une conférence.  
Le résultat final de ce template 🧩 :

{|
```html
---
layout: :theme/post
tags:
    - Conférences
    - Slides
    - Replay
---

{#for conference in cdi:myConfs.getByUrl(page.data.conference-name)}
{#for talk in conference.talks}
<h2> <a href="#{talk.id}" id="{talk.title}">{talk.title}</a> </h2>
📍{talk.location} / 🗣️ {talk.language} / 🗓️ {talk.date} / ⏰ {talk.time} /
🕒 {talk.duration}
</br>
</br>

{talk.pitch.addBr}

</br>
</br>
{#if talk.source}📚 <a href="{talk.source}">Code source</a> / {/if} {#if talk.slides}🌠
<a href="{talk.slides}">Slides</a> {/if} {#if talk.replay}/ 🎥 <a href="{talk.replay}">Replay</a>{/if}

{/for}
{/for}
```
|}

> J'ai choisi de faire un template au format HTML, mais je crois que j'aurai pu le faire en Markdown.

La première partie du template positionne le front matter.
Ce sont les informations par défaut, d'autres seront ajoutées lors de la création du post (par exemple `conference-name`).
Ensuite, on boucle, grâce à Qute, sur la représentation Java des fichiers YAML contenant les données de mes talks. 
Puis il suffit d'accéder aux objets Java avec leurs champs (pas de getter ici juste le nom du champ).

Pour que tout ça se passe bien, il faut que je vous explique la ligne `\{#for conference in cdi:myConfs.getByUrl(page.data.conference-name)\}
` ☝️.

Vous voyez que je référence un objet `myConfs` injecté dans le contexte CDI de Quarkus... mais il est où cet objet ?
C'est là où la partie développement Java m'a été utile, car il fallait que je prépare les données pour être compatibles avec le template.
Comme je vous l'ai dit les données sont au format YAML et chargées en format JSON par l'extension Quarkus, plus précisément en [JSONObject Vertex](https://access.redhat.com/webassets/avalon/d/red_hat_build_of_eclipse_vert.x/3.9.1/vertx_javadocs/io/vertx/core/json/JsonObject.html).

> Il est possible d'avoir un mapping fort en créant un record qui map la structure du YAML et qui, du coup, est beaucoup plus simple à utiliser ensuite dans les templates avec Qute.
> Mais cette partie s'est révélée trop complexe dans la PR que j'ai faite et pour l'instant dans le cas d'un répertoire avec une liste de YAML il n'y a pas le support du mapping objet 🫤

Ceci étant dit, pour me simplifier la vie, j'ai créé un petit helper pour manipuler les objets mappés ([ConferencesProcessor](https://github.com/philippart-s/blog/blob/main/src/main/java/fr/wilda/blog/processor/ConferencesProcessor.java)) : 
```java
package fr.wilda.blog.processor;

import fr.wilda.blog.data.Talk;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.Map;

/// Class to manipulate JSON objects created from YAML data.
/// The bean is injected as `myConfs` bean to be used in Qute.
@ApplicationScoped
@Named("myConfs")
public class ConferencesProcessor {


    // This field represents the ./data/conferences folder. This field has a JSONArray with the directory files content (2022.yml, ...
    @Inject
    @Named("conferences")
    JsonObject talks;

    /// This method takes an id (`picocli` for example) and give the given talks that have this id.
    /// @param id The id that the talk must have
    /// @return Talks list with the right id
    public List<Talk> getByIds(String id) {
        List<Talk> filtered = talks.stream() // stream sur les années
                .map(entry -> (Map.Entry<String, Object>) entry)
                .map(Map.Entry::getValue)
                .map(v -> (JsonArray) v) // chaque valeur = JsonArray d'events
                .flatMap(JsonArray::stream)
                .map(event -> (JsonObject) event)
                .flatMap(event -> event.getJsonArray("talks").stream()
                        .map(t -> (JsonObject) t)
                        .filter(talk -> id.equals(talk.getString("id")))
                        .map(talk -> new Talk(talk.getString("id"),
                                event.getString("name"),
                                talk.getString("date"),
                                event.getString("postDate"),
                                event.getString("talksUrl")))
                )
                .toList();

        return filtered;
    }

    /// This method returns the corresponding JSONObject given the talk url post.
    /// @param url The unique URL for a conference
    /// @return The given JSONObject for a URL
    public List<JsonObject> getByUrl(String url) {
        List<JsonObject> filtered = talks.stream()
                .map(entry -> (Map.Entry<String, Object>) entry)
                .map(Map.Entry::getValue)
                .map(v -> (JsonArray) v)
                .flatMap(events -> events.stream())
                .map(event -> (JsonObject) event)
                .filter(event -> url.equals(event.getString("talksUrl")))
                .toList();
        return filtered;
    }
}
```
 
Comme vous le constatez, c'est dans cette classe que l'on va retrouver la création du bean CDI `myConfs`.
Ensuite, ce sont deux méthodes utilitaires pour manipuler les objets JSONObject : 
 - 🪪 `getByIds` : qui permet d'avoir la liste des talks d'un certain type (par exemple `picocli`), cette méthode me sera utile pour la page qui liste pour chaque type de conférence à quel endroit elles ont été données,
 - 🔗 `getByUrl` : qui permet d'avoir la liste des talks d'une conférence (par son URL qui est unique dans les data), c'est cette méthode qui est utilisée dans le template [conference.html](https://github.com/philippart-s/blog/blob/main/templates/layouts/conference.html).

## 📝 Qute et Java pour la génération automatique de posts

Une fois le template créé, passons à la création des posts correspondants.
Plutôt que de tout créer à la main, j'ai repris le principe de créer les posts en dev comme je l'avais fait pour la version Jekyll.
C'est la classe [ConferenceGenerator](https://github.com/philippart-s/blog/blob/main/src/main/java/fr/wilda/blog/generator/ConferenceGenerator.java) qui s'en charge : 
```java
package fr.wilda.blog.generator;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/// Class to generate the conference blog posts.
/// This class is called on each Quarkus start to generate the conferences posts.
@ApplicationScoped
public class ConferenceGenerator {
    @Inject
    @Named("conferences")
    JsonObject allConferences;

    // front matter with dynamics values from the YAML conferences data.
    String frontMatter = """
---
title: "🎤 Talks donnés à %s 🎤"
description: Liste de talks donnés lors de la conférence %s
image: %s
layout: conference
author: wilda
conference-name: %s
link: %s
---""";

    /// Called once the Quarkus application is ready
    void onStart(@Observes StartupEvent ev) throws IOException {
        Log.info("🚀 Conference pages generation...");
        Map<String, Object> mapOfAllConferences = allConferences.getMap();

        // Conferences posts generation, only non existing posts must be generated.
        for (var entry : mapOfAllConferences.entrySet()) {
            JsonArray conferences = (JsonArray) entry.getValue();
            for (var conference : conferences) {
                JsonObject jsonConf = (JsonObject) conference;
                Path dir = Path.of("./content/posts/conferences/" + jsonConf.getString("postDate") + "-" + jsonConf.getString("talksUrl"));
                if (!Files.isDirectory(dir)) {
                    Files.createDirectories(dir);
                }
                Path file = Path.of(dir + "/index.markdown");
                if (!Files.exists(file)) {
                    Files.createDirectories(dir);
                    Files.write(file,
                            frontMatter.formatted(jsonConf.getString("name"),
                                    jsonConf.getString("name"),
                                    (Files.exists(Path.of("./public/images/conferences/" + jsonConf.getString("talksUrl") + ".png")) ?
                                            "conferences/" + jsonConf.getString("talksUrl") + ".png" :
                                            "conferences/conference.jpg"),
                                    jsonConf.getString("talksUrl"),
                                    jsonConf.getString("talksUrl")).getBytes(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                }
            }
        }

        Log.info("✅ Conference pages generated ✅");
    }
}
```

Une fois de plus, je vais réutiliser la version Java des data YAML pour générer tous les blog posts nécessaires.
La génération se fait au moment du démarrage de l'application Quarkus.
J'aurai certainement pû optimiser cela pour éviter que cela se fasse à chaque fois, mais plus à la demande.
À défaut, je ne génère pas de blog post s'il existe déjà, c'est déjà ça de gagné 😉.

J'ai ensuite créé deux posts spéciaux : 
 - 👥 [conferences.md](https://github.com/philippart-s/blog/blob/main/content/conferences.md) qui liste toutes les conférences auxquelles j'ai participé triées par années. Pour chaque conférence un lien vers le blog post généré correspondant,
 - 🎤 [talks.md](https://github.com/philippart-s/blog/blob/main/content/talks.md) qui liste l'ensemble des talks que j'ai donnés. Pour chaque talk, il y a la liste des conférences où je l'ai donné avec un lien vers le blog post correspondant.

Les deux posts utilisent Qute et les données des fichiers YAML pour m'éviter de les modifier à chaque nouvelle conférence.

# 📜 La migration des posts non conférences

Bon, à ce stade, j'ai migré la partie conférences.
Pourquoi commencer par ça ?
Tout simplement parce qu'elle nécessitait obligatoirement d'avoir du développement spécifique.
Je ne voulais pas régresser par rapport à Jekyll et devoir gérer mes conférences à la main.

Maintenant que c'est fait, il me reste les posts dits classiques.
J'ai déjà mentionné au début la partie front matter à migrer et avec cela 2-3 choses.
La liste de migration pour mon site revient donc à : 
- ⚙ migrer les éléments de front matter qui ne sont pas compatibles ROQ
- 🗑️ retirer la syntaxe Kramdown, ROQ ne le supporte pas (il supporte Markdown et AsciiDoc)
- 📂 gérer la création d'un répertoire à partir du nom de post Jekyll
- 📜 y copier l'article et ses images
- 🖼️ gérer l'image d'en-tête qui sera aussi la vignette

## ❗️#️⃣ JBang à la rescousse

Bien entendu, je n'allais pas faire tout ça à la main, cela représente tout de même plus de 50 posts à migrer 😅.
Pour cela, j'ai dégainé mon arme secrète : [JBang](https://www.jbang.dev/).

Tout se passe dans le script [JekyllToRoq](https://github.com/philippart-s/blog/blob/main/scripts/JekyllToRoq.java).

La base est l'utilisation massive de regexp pour faire du search and replace depuis le fichier Jekyll vers le fichier ROQ.
Étant donné que c'est du one-shot le code n'est clairement pas joli et optimisé 🫣.

# 🚀 Publication du blog

Eh bien si vous me lisez, c'est que j'ai bien tout migré.
Et en cela, merci ROQ car le template de projet vient avec une github action qui n'attend plus que vous pour l'utiliser pour publier sur GitHub Pages.
Si vous voulez utiliser d'autres plateformes cibles, je vous laisse aller voir la section [publishing](https://iamroq.com/docs/publishing/) de la documentation.

# 🤗 En conclusion

Il me reste tout de même quelques éléments qu'il va falloir gérer suite à cette migration :
- 🤝 essayer d'avoir le mapping objet pour les data multiples, pour cela, il va falloir que je me lance dans une nouvelle PR,
- ⚡️ optimiser le code pour mes pages de talks (une fois le mapping objet fait),
- 🎨 la gestion de highlightJS qui ne me convient pas (il manque notamment les numéros de lignes),
- 🎨 avoir un style propre à moi, car là, c'est le style par défaut (mais bon moi et le CSS ...),
- ⚡️ avoir une version plus simple de génération qui ne se lance pas à chaque démarrage, mais à la demande (mais j'attends de voir ce que vont faire les devs de ROQ sur la dev UI 🤫),
- ♻️ refactorer mon code qui a les terminologies Jekyll et parfois cela rend la lecture compliquée pour la maintenance

Et tout ce qu'il me passera par la tête pour m'amuser à développer mon blog.

Si vous êtes arrivé•es jusque-là merci de m'avoir lu et s'il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
