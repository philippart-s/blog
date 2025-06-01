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
# sidebar:
# title: "Listes des conférences"
# nav: conferences-nav
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