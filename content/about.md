---
title: About
layout: page
---

# About Me

Developer Advocate chez OVHcloud. Développeur Java depuis … trop longtemps 😅.  
Baigne dans le cloud et l’Intelligence Artificielle.  
Speaker à de nombreuses conférences.

## Authors

<div class="authors">
  <!-- authors.yml is in the data/ -->
  {#for id in cdi:authors.fields}
    {#let author=cdi:authors.get(id)}
    <!-- the roq/authorCard tag is defined in the default Roq theme -->
    {#roq/authorCard name=author.name avatar=author.avatar nickname=author.nickname profile=author.profile /}
  {/for}
</div>

