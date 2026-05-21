---
title: "🖥️ Construire une TUI en Java avec TamboUI"
description: "Tout le monde peut cuisiner ! ©Gusteau"
link: /2026-05-21-tamboui-tui
image: cover.svg
tags:
  - Java
  - TUI
  - Code
author: wildagsx
---

🏴󠁧󠁢󠁥󠁮󠁧󠁿 You can find the English version of this article [here]({site.url}2026-05-21-tamboui-tui-en) 🏴󠁧󠁢󠁥󠁮󠁧󠁿.

## TL;DR
> 🖥️ Découverte de **[TamboUI](https://tamboui.dev/)**, une lib Java pour construire des **T**erminal **U**ser **I**nterface (TUI)  
> 🎨 Construction d'une TUI complète avec menu, saisie utilisateur, panels et gestion d'événements  
> 🚀 Utilisation du **Toolkit DSL** pour un code déclaratif et lisible  
> 🐙 Le [code complet](https://github.com/philippart-s/jarvis) d'un exemple dans mon projet pour le talk Jarvis

<br/>

# 📜 Introduction

Vous avez déjà eu envie de faire une TUI en Java ? Pas juste un `System.out.println` triste, non, une vraie TUI avec des panels, des couleurs, de la navigation au clavier... le genre de chose que l'on voit dans les autres technos mais, jusqu'à présent, pas en Java 🥹.

Bonne nouvelle : **[TamboUI](https://tamboui.dev/)** débarque dans l'écosystème Java et nous permet enfin de construire des TUI élégantes. La lib s'inspire fortement de [ratatui](https://ratatui.rs/), avec en bonus du CSS dans le terminal (oui oui, vous avez bien lu 🤯).

Dans cet article, je vous propose de découvrir TamboUI à travers un cas concret : la construction d'une interface terminal interactive avec un menu, des vues de chat, et du streaming de réponses. On va décortiquer les concepts clés en s'appuyant sur un projet réel.

Si vous suivez ce blog, vous connaissez peut-être déjà [Jarvis](https://github.com/philippart-s/jarvis), mon projet de CLI en Java que j'utilise pour mes démos. Je l'avais présenté dans mon article [À la découverte de Picocli]({site.url}2023-08-03-discover-picocli), et je m'en sers régulièrement depuis, notamment pour mes articles sur [les agents IA]({site.url}2026-01-22-ai-agents). Eh bien, il était temps de lui offrir une vraie interface terminal digne de ce nom ! J'ai donc intégré TamboUI à Jarvis pour remplacer la sortie texte basique par une TUI interactive. C'est ce résultat qu'on va décortiquer ensemble.

# 🤔 C'est quoi TamboUI ?

**TamboUI** est une bibliothèque Java pour construire des TUl. Elle propose trois niveaux d'API :

- **[Immediate Mode](https://tamboui.dev/docs/main/api-levels.html#immediate-mode)** : contrôle total, vous gérez la boucle de rendu vous-même
- **[TuiRunner](https://tamboui.dev/docs/main/api-levels.html#tuirunner)** : une boucle d'événements gérée pour vous, avec des callbacks
- **[Toolkit DSL](https://tamboui.dev/docs/main/api-levels.html#toolkit-dsl)** : l'API déclarative avec un DSL déclaratif, la gestion du focus, et le CSS (c'est celle qu'on va utiliser 😎)

Le projet supporte GraalVM pour la compilation native ⚡ (pas encore testé), fonctionne sur toutes les plateformes, et gère l'Unicode complet (emoji inclus 🎉).

# 🛠️ Mise en place

Pour utiliser TamboUI dans votre projet, ajoutez les dépendances suivantes dans votre `pom.xml`. Dans Jarvis, j'utilise la version `0.3.0` :

```xml
<properties>
    <tamboui.version>0.3.0</tamboui.version>
</properties>
```

{|
```xml
<!-- Intégration PicoCLI : permet d'utiliser TamboUI dans une commande PicoCLI existante -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-picocli</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- Le Toolkit DSL : l'API déclarative avec les composants, le focus manager, etc. -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-toolkit</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- Le backend terminal basé sur Panama (FFM) pour les JDK modernes (21+) -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-panama-backend</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- Le moteur de rendu Markdown (CommonMark + GFM) -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-markdown</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- L'élément Toolkit qui wrappe le rendu Markdown pour l'utiliser dans le DSL -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-toolkit-markdown</artifactId>
    <version>${tamboui.version}</version>
</dependency>
```
|}

Petit récapitulatif de ce que fait chaque module :

| Module | Rôle |
|--------|------|
| `tamboui-picocli` | Intègre TamboUI dans une commande PicoCLI (c'est mon cas avec Jarvis) |
| `tamboui-toolkit` | Le DSL déclaratif pour construire l'UI (panels, columns, text, lists, events...) |
| `tamboui-panama-backend` | Le backend terminal utilisant l'API Foreign Function & Memory (Panama) de Java 21+ |
| `tamboui-markdown` | Le moteur de rendu Markdown dans le terminal |
| `tamboui-toolkit-markdown` | Le bridge entre le moteur Markdown et le Toolkit DSL (l'élément `markdown(...)`) |

> ℹ️ Le module `tamboui-picocli` est ce qui permet d'ajouter une sous-commande TUI à une CLI PicoCLI existante. Si vous partez de zéro sans PicoCLI, `tamboui-toolkit` + un backend suffisent.

# 🎬 Le ToolkitRunner : le point d'entrée

Tout commence par un `ToolkitRunner`. C'est lui qui gère la boucle d'événements, le terminal, et appelle votre fonction de rendu à chaque frame :

```java
var config = TuiConfig.builder()
    .tickRate(Duration.ofMillis(100))
    .build();

try (var runner = ToolkitRunner.create(config)) {
    runner.run(this::render);
}
```

Le `tickRate` définit la fréquence de rafraîchissement. 100ms c'est un bon compromis entre réactivité et consommation CPU. La méthode `render()` sera appelée à chaque tick pour reconstruire l'UI (immediate mode rendering : on redessine tout à chaque frame, c'est TamboUI qui optimise les diffs côté terminal).

# 🎨 Construire une vue avec le Toolkit DSL

Le Toolkit DSL est **déclaratif** : vous décrivez votre UI comme un arbre d'éléments. Voyons comment construire un menu :

```java
private Element menuView() {
    return column(
        panel(
            text("🤖 Jarvis TUI 🤖").bold().cyan()
        ).rounded().borderColor(Color.CYAN).length(3),

        panel("Jarvis", menuList)
            .rounded().borderColor(Color.GREEN).fill()
            .id("menu").focusable()
            .focusedBorderColor(Color.CYAN)
            .onKeyEvent(this::handleMenuKey),

        helpBar(
            "↑/↓", "Navigate",
            "Enter", "Select",
            "q/Ctrl+C", "Quit"
        )
    );
}
```

Décortiquons :
- **`column(...)`** : empile les éléments verticalement
- **`panel("titre", contenu)`** : crée un bloc avec un titre et une bordure
- **`.rounded()`** : bordures arrondies (parce qu'on est des gens civilisés)
- **`.fill()`** : le panel prend tout l'espace restant
- **`.id("menu").focusable()`** : permet la navigation au clavier entre éléments
- **`.onKeyEvent(this::handleMenuKey)`** : gestion des événements clavier

## 📋 Les listes

Pour le menu, on utilise un `ListElement` :

```java
private static final List<String> MENU_ITEMS = List.of(
    "Chat bot",
    "RAG demo",
    "MCP demo",
    "Agent with human workflow demo"
);

private final ListElement<?> menuList = list(MENU_ITEMS.toArray(new String[0]))
    .highlightColor(Color.CYAN)
    .highlightSymbol("▶ ")
    .autoScroll();
```

La liste gère automatiquement la navigation haut/bas et l'élément sélectionné. Le `highlightSymbol` ajoute un indicateur visuel devant l'élément actif.

# ⌨️ Gestion des événements

Les événements clavier se gèrent via des callbacks typés :

```java
private EventResult handleMenuKey(KeyEvent event) {
    if (event.isConfirm() || event.isSelect()) {
        var selected = menuList.selected();
        if (selected >= 0 && selected < MENU_ITEMS.size()) {
            currentMode = Mode.values()[selected + 1];
            inputState.clear();
            response = "";
            return EventResult.HANDLED;
        }
    }
    return EventResult.UNHANDLED;
}
```

Le pattern est simple :
- **`EventResult.HANDLED`** : l'événement est consommé, pas de propagation
- **`EventResult.UNHANDLED`** : l'événement continue à remonter (utile pour les raccourcis globaux)

# 💬 Une vue de chat interactive

Voici une vue plus complexe avec un champ de saisie, un panneau de réponse en markdown, et des logs :

```java
private Element chatView() {
    return column(
        chatHeader(),

        panel("Question",
            textInput(inputState)
                .placeholder(processing ? "Waiting for response..." : "Ask a question...")
                .id("chat-input")
                .onSubmit(this::submitQuestion)
        ).rounded().borderColor(Color.YELLOW).focusedBorderColor(Color.CYAN).length(3),

        panel("Response",
            markdown(buildResponseText())
                .overflow(Overflow.WRAP_WORD))
            .rounded().borderColor(Color.GREEN).fill(2)
            .id("chat-response").focusable()
            .onKeyEvent(this::handleChatKey),

        panel("Logs", textLines(logs.isEmpty() ? "No logs yet." : logs))
            .rounded().borderColor(Color.DARK_GRAY).fill()
            .id("chat-logs"),

        chatFooter()
    );
}
```

Quelques points notables :
- **`textInput(inputState)`** : un champ de saisie avec état géré par un `TextInputState`
- **`.onSubmit(...)`** : callback déclenché quand l'utilisateur appuie sur Entrée
- **`markdown(...)`** : rendu Markdown directement dans le terminal ! (CommonMark + GFM supportés)
- **`.fill(2)`** : prend 2 parts de l'espace disponible (les ratios, comme en CSS flexbox 😏)

## 📝 Le TextInputState

L'état du champ de saisie est externalisé dans un objet `TextInputState`. 
Ça permet de le manipuler facilement et de le mettre à jour / réinitialiser depuis n'importe où dans le code :

```java
private final TextInputState inputState = new TextInputState();

// Effacer le contenu
inputState.clear();

// Récupérer le texte saisi
var question = inputState.text().trim();
```

# 🪟 Les dialogues modaux

TamboUI permet de superposer des éléments avec `stack(...)`. Parfait pour les dialogues de confirmation :

```java
if (tuiToolApproval.hasPendingApproval()) {
    runner.focusManager().setFocus("approval-dialog");
    return stack(
        view,
        dialog("⚠️  Tool Approval",
            text("Tool: " + tuiToolApproval.pendingToolName()).bold().cyan(),
            text(""),
            text("Do you want to allow this tool execution?"),
            text(""),
            text("[Enter] Approve    [Esc] Reject").dim()
        ).rounded().borderColor(Color.YELLOW).width(60)
            .id("approval-dialog").focusable()
            .onConfirm(tuiToolApproval::approve)
            .onCancel(tuiToolApproval::reject)
    );
}
```

Le `stack(...)` empile la vue principale et le dialogue par-dessus. Le `focusManager()` permet de diriger le focus vers le dialogue. Les callbacks `.onConfirm()` et `.onCancel()` réagissent respectivement à Entrée et Échap.

# 🔄 Streaming réactif et virtual threads

Un point sympa : on peut mettre à jour l'UI depuis un thread en arrière-plan grâce à `runOnRenderThread()` :

```java
private void streamResponse(Function<String, Multi<String>> serviceCall, String question) {
    Thread.startVirtualThread(() -> serviceCall.apply(question)
        .subscribe().with(
            token -> runner.runOnRenderThread(() -> response += token),
            error -> runner.runOnRenderThread(() -> {
                logs += "⚠️ Error: " + error.getMessage() + "\n";
                processing = false;
            }),
            () -> runner.runOnRenderThread(() -> processing = false)
        ));
}
```

Ici on utilise les **virtual threads** de Java 21+ pour lancer un appel asynchrone, et `runOnRenderThread()` pour mettre à jour l'état de l'UI de manière thread-safe. 
Comme TamboUI redessine à chaque tick, le texte apparaît progressivement token par token. 

# 🧭 Navigation entre vues

Pour gérer plusieurs écrans, un simple `enum` et un `switch` dans la méthode `render()` suffisent :

```java
enum Mode {
    MENU, CHAT, RAG, MCP, MANUAL_WORKFLOW, WORKFLOW, AGENT
}

private Mode currentMode = Mode.MENU;

private Element render() {
    return switch (currentMode) {
        case MENU -> menuView();
        case RAG -> ragDocumentsLoaded ? chatView() : ragPathView();
        default -> chatView();
    };
}
```

Pas besoin de framework de routing complexe. On change le mode, l'UI se reconstruit automatiquement au prochain tick. 

# 🏁 Conclusion

TamboUI est une lib qui fait du bien. En quelques lignes de code déclaratif, on obtient une TUI propre, réactive, avec de la couleur, du markdown, des dialogues modaux et de la navigation au clavier. Le tout en Java, compilable en natif avec GraalVM.

La lib est encore jeune (version 0.3.x au moment où j'écris), mais elle est déjà très utilisable et le projet avance vite. Si vous faites du CLI en Java, jetez-y un œil, vous ne le regretterez pas 😊.

L'ensemble du code source est disponible dans le dépôt GitHub [jarvis](https://github.com/philippart-s/jarvis).

Si vous êtes arrivés jusque là merci de m'avoir lu et si il y a des coquilles n'hésitez pas à me faire une [issue ou PR](https://github.com/philippart-s/blog) 😊.
