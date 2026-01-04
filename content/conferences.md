---
title: Conferences
layout: :theme/page
tags:
  - Conférences
  - Slides
  - Replays
---
Ici, vous trouverez l'ensemble des conférences auxquelles j'ai participé et que j'ai présentées.

Vous pouvez aussi retrouver la [liste des sujets]({site.url}/talks) que j'ai donné en conférence.

<style>
  table {
    width: 100%;
    height: 100%;
    display: table;
  }

  th,
  td {
    border: 0 solid #000 !important;
    padding: 10px !important;
    text-align: center !important;
    vertical-align: middle !important;
  }

  th {
    background-color: #f2f2f2 !important;
  }
</style>


<table>
{#let conferences=cdi:conferences}
{#for year in conferences.fieldNames.stream().sorted().toList().reversed()}
  <tr>
    <th colspan="3">
      <h1> {year} </h1>
    </th>
  </tr>
{#for conf in conferences.get(year)}
  <tr>
    <td style="width: 35%; text-align: left;">
      {conf.name}
    </td>
    <td style="width: 35%;">
      🗓️ {conf.date} 🗓️
    </td>
    <td style="width: 30%;">
      🎤 <a href="{site.url}/{conf.talksUrl}">
        Liste des talks
      </a> 🎤
    </td>
  </tr>
{/for}
{/for}

</table>