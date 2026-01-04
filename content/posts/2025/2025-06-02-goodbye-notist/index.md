---
title: "🧪 Remplacer Noti.st par du \"as code\" avec Jekyll 💎"
description: "Originalement géré dans noti.st, j'ai décidé de géré la liste des confs que j'ai données avec Jekyll. Mais pourquoi 🤨 et comment ?"
link: /2025-06-02-goodbye-notist
tags: 
  - Ruby
  - Jekyll
image: main-image.webp
figCaption: "@wildagsx"
author: wilda
---

## 🤗 Non je ne quitte pas Noti.st car c'est un mauvais outil

Cela me semblait utile de commencer par indiquer que ce blog post n'est pas pour dire que [Noti.st](https://noti.st/) est un mauvais outil.
Bien au contraire, après 3 ans d'utilisation je n'ai pas grand chose à lui reprocher, il fait très bien le travail.
Pour vous expliquer ce qu'est Noti.st je pense que le texte d'accueil du site résume parfaitement son but:

>Notist is a place to build your public speaking portfolio. A place to share your slides, but also to collate feedback, tweets, photos, resources and everything to create a permanent history of your speaking events.

Si vous voulez voir ce que cela donne voici mon compte publique : [https://noti.st/philippart-s](https://noti.st/philippart-s)

## 🤨 Mais alors pourquoi partir de Noti.st ?

Pour plusieurs raisons, mais la principale est le fameux "vendor locking".
En effet, tout ce que j'ai mis dans Noti.st n'est pas récupérable, je peux récupérer la liste de mes talks mais pas les éléments que j'ai uploadés (titres, localisations, slides, liens, ...).
Ce qui, le jour où je ne souhaite plus payer mon abonnement (> 100$ par an) ou tout simplement arrêter peut être problématique.
Soit j'ai tout perdu, soit je vais galérer avec pleins d'actions manuelles pour le faire (spoiler on en parlera lors de la partie migration 😱).

Et autre raison : cela faisait un petit moment que je me disais qu'il y avait moyen de développer un truc au sein même de mon blog au final ... 
Oui même pour moi qui suit très loin d'être un développeur front (c'est pour cela que j'utilise [Jekyll](https://jekyllrb.com/) 😇).

## 📚 Bon on y va ? C'est quoi les specs ?

Mon but : faire le moins d'actions possibles lorsque je dois rajouter un talk.
J'aurai donc un fichier de données global où je retrouverai mes informations de base pour un talk : nom de la conférence, date, talk, slides, replay, source, ...
Des templates pour générer un maximum de choses, idéalement je ne veux pas à avoir à créer un blog post après chaque conférence mais juste une nouvelle entrée dans mon fichier de données.
Ce dernier point va être important dans certains choix par la suite.

Bien entendu, le tout intégré dans Jekyll afin de facilement l'avoir dans mon blog.

Bonus attendus : 
 - une gestion as code
 - une gestion du cycle de vie dans Git
 - une modification simple car via des templates
 - la découverte d'une utilisation avancée de Jekyll

## 💿 Les données 

Le choix pour mes données de conférences a été assez rapide, YAML.
Parce que on ne va pas se mentir, même si on dit que l'on n'aime pas ça, c'est quand même bien pratique et pas trop verbeux.
Jekyll permet de manipuler et d'[accéder nativement](https://jekyllrb.com/docs/step-by-step/06-data-files/) aux données dans un fichier YAML avec le moteur de templating [Liquid](https://jekyllrb.com/docs/liquid/).

A ce jour la structure d'une entrée YAML pour une conférence est la suivante:
```yaml
conf-2025:
 name: conf
 post-date: 2025-01-22
 date: 22 au 25 janvier 2025 
 excerpt: "Liste des talks donnés lors de Conf 2025"
 categories: Conférences
 tags:
   - Slides
   - Replays
 talks:
  - title: "blabla"
    location: Ville (🇫🇷)
    language: Français
    date: Mercredi 22 janvier 2025
    time: 14h00 - 17h00
    duration: 3h mins
    abstract: |
      blabla
    source: xxx
    slides: xxx
    replay: xxx
```

ℹ️ Pour que Jekyll puisse les utiliser il vous suffit de les déposer dans le répertoire [_data](.https://github.com/philippart-s/blog/tree/master/_data) de votre blog.

## 🏞️ Le layout, éviter de réinventer la roue

Une fois ces données créées, il faut maintenant les afficher, pour cela rien de plus simple avec Jekyll et le moteur de templating [Liquid](https://jekyllrb.com/docs/liquid/), il est très simple d'ajouter du dynamisme et de la généricité dans les blog posts.

Et cerise sur le gateau, Jekyll vient avec un mécanisme de [layouts](https://jekyllrb.com/docs/layouts/) pour faciliter la mise en page de vos articles.
Et il est aussi possible d'utiliser [Liquid](https://jekyllrb.com/docs/liquid/) dans un layout 😉.

Du coup, pour créer un nouveau layout, il suffit de créer mon fichier [conference.html](https://github.com/philippart-s/blog/blob/master/_layouts/conference.html) dans le répertoire [_layouts](https://github.com/philippart-s/blog/tree/master/_layouts) de mon blog:


```markdown
---
layout: archive
classes: wide
categories:
- Conférences
tags:
- Slides
- Replays
---

{{ content }}

{% for conferences in site.data.conferences %}
{% assign conferenceFile = conferences[0] %}
{% assign conference = site.data.conferences[conferenceFile] %}

{% if conference[page.conference-name] %}
{% for talk in conference[page.conference-name].talks %}

<h2> {{ talk.title}} </h2>

📍{{ talk.location}} / 🗣️ {{ talk.language}} / 🗓️ {{ talk.date}} / ⏰ {{ talk.time}} / 🕒 {{ talk.duration}}

{% capture notice-2 %}
{{ talk.abstract}}
{% endcapture %}

<div class="notice">{{ notice-2 | markdownify }}</div>

{% if talk.source %}📚 <a href="{{ talk.source}}">Code source</a> / {% endif %} {% if talk.slides %}🌠
<a href="{{ talk.slides}}">Slides</a> 
{% endif %} {% if talk.replay %}/ 🎥 <a href="{{ talk.replay}}">Replay</a>{% endif %}

{% endfor %}
{% endif %}
{% endfor %}
```

Ce qu'il faut retenir : 
 - la première partie (entre le `---`) permet d'utiliser un layout de base si on le souhaite,
 - les `{{ }}` permettent d'accéder à des variables prédéfinies, comme `content` ou créées, comme `talk`
 - `{{ content }}` représente le contenu du blog post créé qui utilisera ce layout et sera injecté à cet endroit du layout
 - les `{% %}` permettent d'ajouter des instructions de flow

## 📝 Créer les articles à partir du layout

A ce stade je peux utiliser mon layout dans un article fraîchement créé, par exemple pour Devoxx France:

```markdown
---
layout: conference
title: Talks donnés à Devoxx France
date: 2025-04-16
permalink: /talks/devoxx-fr-2025
excerpt: Liste des talks donnés lors de Devoxx France 2025
categories: Conférences
tags: 
 - ["Slides", "Replays"]
conference-name: devoxx-fr-2025
---
```

Cela me génère l'[article suivant](https://philippart-s.github.io/blog/talks/devoxx-fr-2025): 
![](devoxx-france-blog-post.png)
<br/>

C'est bien ... Mais c'est dommage de devoir créer un fichier à chaque talk pour qu'au final il soit vide avec juste l'en-tête renseignée.

Il doit bien avoir un moyen d'automatiser ça, d'autant que les données sont présentes dans le fichier de données YAML précédemment créé.

## ⚙️ Générer les articles avec un plugin

Jekyll utilise des [plugins](https://jekyllrb.com/docs/plugins/), soit communautaires, soit locaux.
C'est le deuxième type qui va nous intéresser, créer un plugin local.
En effet, pour générer des pages, on va créer un plugin de type [generator](https://jekyllrb.com/docs/plugins/generators/) (logique 😉).
Et comme il est petit, pas besoin de le packager, pour cela il suffit de le mettre dans le répertoire [_plugins](https://github.com/philippart-s/blog/tree/master/_plugins) de notre site.

C'est du [Ruby](https://www.ruby-lang.org/fr/) et voyons à quoi il ressemble :

```ruby
require 'jekyll'
require 'fileutils'

module Jekyll
  class ConferenceGenerator < Generator
    safe true

    def generate(site)
      puts "---------- ConferenceGenerator started ----------"
       if (!defined?@render_count)
          @render_count = 1
       end

       if @render_count < 1
         Jekyll.logger.info('already fetched data')
         return
       end

       conferencesFilesKey = site.data['conferences'].keys.select { |key| key.include?('conferences') }
       conferencesFilesKey.each do |key|
         Jekyll.logger.info("Processing #{ key}")

          conferences = site.data['conferences'][key]

          if conferences
            conferences.each do |conference_id, details|
              post_path = File.join('_posts', "#{ details['post-date']}-#{ conference_id}-talks.markdown")

              post_data = {
                'layout' => 'conference',
                'title' => "Talks donnés à #{ details['name']}",
                'date' => details['post-date'],
                'permalink' => "/talks/#{ conference_id}",
                'excerpt' => details['excerpt'],
                'categories' => "#{ details['categories']}",
                'tags' => "\n - #{ details['tags']}",
                'conference-name' => conference_id
              }
              
              post_content = "---\n"
              post_content += post_data.map { |key, value| "#{ key}: #{ value}" }.join("\n") + "\n"
              post_content += "---\n"

              # Write the post file
              FileUtils.mkdir_p(File.dirname(post_path))
              File.write(post_path, post_content)

              puts "Generated post: #{ post_path}"
            end
          else
            puts "No conferences data found in site.data"
          end
          Jekyll.logger.info('Data fetched successfully.')
          @render_count = @render_count - 1
          puts "---------- ConferenceGenerator finished ----------"
        end
      end
  end
end
```

Rien de bien compliqué ici :
 - l10 -> l17 : c'est juste un garde fou (pas très élégant) pour éviter à Jekyll de boucler indéfiniment en mode watch. Le mode watch régénère l'ensemble du site et appelle les plugins à chaque modification de fichier et comme le plugin à chaque exécution génère des fichiers c'est un cercle sans fin 😉. Il faudra que je trouve une manière plus élégante de le gérer.
 - l19 : j'utilise uniquement les fichier YAML qui ont des données sur les conférences (ces fichiers commencent par `conferences`)
 - l20 -> l42: j'itère pour chaque conférence et je fabrique l'en-tête
 - l45 -> l46: création physique du fichier avec les données préparées (pour me repérer, les articles générés ont l'extension `markdown` et ceux écrits à la main `md`)

Et voilà avec ce script à chaque modification dans un fichier YAML de données de conférences, les articles correspondants sont créés 😎.

## 🍰 Cerise sur le gâteau, une page pour lister les talks

Comme j'ai les données qui servent à créer les pages unitaires, il est assez facile de créer [une page qui liste l'ensemble de mes conférences](https://philippart-s.github.io/blog/conferences/) regroupées par année:

```markdown
---
title: "Conférences"
permalink: /conferences/
classes: wide
excerpt: "Liste des conférences avec les ressources (slides, vidéos, ...)"
#author_profile: false
categories:
- Conférences
tags:
- Slides
- Replays
---

Ici vous trouverez l'ensemble des conférences auxquelles j'ai participé et que j'ai présentées. 

Vous pouvez aussi retrouver la [liste des sujets]({{ site.baseurl }}/talks) que j'ai donnés en conférence.

<style>
  table {
    width: 100%;
    height: 100%;
    display: table;
  }

  th,
  td {
    border: 0px solid #000;
    padding: 10px;
    text-align: center;
    vertical-align: middle;
  }

  th {
    background-color: #f2f2f2;
  }
</style>

<table>
  {% for conferences in site.data.conferences reversed %}
    {% assign conferenceFile = conferences[0] %}

  <tr>
    <th colspan="3">
      <h1>  {{ conferenceFile | split: "-" | last }} </h1>
    </th>
  </tr>

  {% for conference in site.data.conferences[conferenceFile] %}
  <tr>
    <td style="width: 35%; text-align: left;">
      {{ conference[1].name}}
    </td>
    <td style="width: 35%;">
      🗓️ {{ conference[1].date }} 🗓️
    </td>
    <td style="width: 30%;">
      🎤 <a href="{{ site.baseurl }}/talks/{{ conference[0] }}">
        Liste des talks
      </a> 🎤
    </td>
  </tr>
  {% endfor %}
  {% endfor %}
</table>
```

## 📋 Il en reste une ...

J'ai aussi créé une page qui liste les talks que j'ai donné et pour chaque talk la conférence avec le lien vers cette conférence.
Si j'ai toutes les données, elles se trouvent dans l'ensemble des fichiers YAML.
Cela demanderait à faire une analyse croisée et des `group by` entre les données des fichiers, pas infaisable mais créer la page à la main a été moins long 😜.

Cette page se trouve [ici](https://philippart-s.github.io/blog/talks/) et son code [ici](https://github.com/philippart-s/blog/blob/master/_pages/conferences/talks.md).

Peut-être une amélioration dans le futur ...

## 🥳 En conclusion

Ce qui m'a pris le plus de temps a été de reporter l'ensemble de mes talks depuis Noti.st vers mon blog.
Pas d'export des données donc tout à la main talk par talk, pour plus de 50 talks j'ai dû y passer 3 à 4h.

Maintenant que c'est fait, rajouter un talk c'est ajouter une entrée dans le fichier YAML, soit pas plus de 2-3 minutes !

Pour voir ce que cela donne :
  - la [page](https://philippart-s.github.io/blog/conferences/) qui liste les conférences avec un lien vers chaque article qui indique les talks avec leurs détails
  - la [page](https://philippart-s.github.io/blog/conferences/) qui liste les talks avec la liste des conférences où ils ont été donnés (avec un lien vers l'article de la dite conférence)
  - un [exemple](https://philippart-s.github.io/blog/talks/devoxx-fr-2025) d'article généré

La suite ?

Certainement ajouter la partie réseau social / photos / la possibilité de mettre plusieurs liens vers du code source / une carte / ...
Au final j'ai juste à modifier mes templates, rajouter les données dans les YAML et ce sera effectif 😉.

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.