---
title: "🖥️ Building a TUI in Java with TamboUI"
description: "Anyone can cook! ©Gusteau"
link: /2026-05-21-tamboui-tui-en
image: cover.svg
tags:
  - Java
  - TUI
  - Code
author: wildagsx
---

🇫🇷 Vous trouverez la version française de cet article [ici]({site.url}2026-05-21-tamboui-tui) 🇫🇷.

## TL;DR
> 🖥️ Discovering **[TamboUI](https://tamboui.dev/)**, a Java lib to build **T**erminal **U**ser **I**nterfaces (TUI)  
> 🎨 Building a full TUI with menus, user input, panels, and event handling  
> 🚀 Using the **Toolkit DSL** for declarative and readable code  
> 🐙 The [full source code](https://github.com/philippart-s/jarvis) from my Jarvis talk project

<br/>

# 📜 Introduction

Ever wanted to build a TUI in Java? Not just a sad `System.out.println`, but a real TUI with panels, colors, keyboard navigation... the kind of thing you see in other tech stacks but, until now, not in Java 🥹.

Good news: **[TamboUI](https://tamboui.dev/)** has landed in the Java ecosystem and finally lets us build elegant TUIs. The library is heavily inspired by [ratatui](https://ratatui.rs/), with a bonus: CSS in the terminal (yes, you read that right 🤯).

In this article, I'll walk you through TamboUI with a concrete use case: building an interactive terminal interface with a menu, chat views, and response streaming. We'll break down the key concepts using a real project.

If you follow this blog, you might already know [Jarvis](https://github.com/philippart-s/jarvis), my Java CLI project that I use for demos. I introduced it in my article [Discovering Picocli]({site.url}2023-08-03-discover-picocli), and I've been using it regularly since, notably for my articles on [AI agents]({site.url}2026-01-22-ai-agents). Well, it was time to give it a proper terminal interface! So I integrated TamboUI into Jarvis to replace the basic text output with an interactive TUI. That's what we'll dissect together.

# 🤔 What is TamboUI?

**TamboUI** is a Java library for building terminal user interfaces. It offers three API levels:

- **[Immediate Mode](https://tamboui.dev/docs/main/api-levels.html#immediate-mode)**: full control, you manage the render loop yourself
- **[TuiRunner](https://tamboui.dev/docs/main/api-levels.html#tuirunner)**: a managed event loop with callbacks
- **[Toolkit DSL](https://tamboui.dev/docs/main/api-levels.html#toolkit-dsl)**: the declarative API with a fluent DSL, focus management, and CSS (that's the one we'll use 😎)

The project supports GraalVM for native compilation ⚡ (not tested yet), works on all platforms, and handles full Unicode (emojis included 🎉).

# 🛠️ Setup

To use TamboUI in your project, add the following dependencies to your `pom.xml`. In Jarvis, I'm using version `0.3.0`:

```xml
<properties>
    <tamboui.version>0.3.0</tamboui.version>
</properties>
```

{|
```xml
<!-- PicoCLI integration: use TamboUI within an existing PicoCLI command -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-picocli</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- The Toolkit DSL: declarative API with components, focus manager, etc. -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-toolkit</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- Terminal backend based on Panama (FFM) for modern JDKs (21+) -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-panama-backend</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- Markdown rendering engine (CommonMark + GFM) -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-markdown</artifactId>
    <version>${tamboui.version}</version>
</dependency>

<!-- Toolkit element wrapping the Markdown renderer for use in the DSL -->
<dependency>
    <groupId>dev.tamboui</groupId>
    <artifactId>tamboui-toolkit-markdown</artifactId>
    <version>${tamboui.version}</version>
</dependency>
```
|}

Quick summary of what each module does:

| Module | Role |
|--------|------|
| `tamboui-picocli` | Integrates TamboUI into a PicoCLI command (my case with Jarvis) |
| `tamboui-toolkit` | The declarative DSL to build the UI (panels, columns, text, lists, events...) |
| `tamboui-panama-backend` | Terminal backend using Java 21+'s Foreign Function & Memory (Panama) API |
| `tamboui-markdown` | Markdown rendering engine for the terminal |
| `tamboui-toolkit-markdown` | Bridge between the Markdown engine and the Toolkit DSL (the `markdown(...)` element) |

> ℹ️ The `tamboui-picocli` module is what lets you add a TUI subcommand to an existing PicoCLI CLI. If you're starting from scratch without PicoCLI, `tamboui-toolkit` + a backend are enough.

# 🎬 ToolkitRunner: the entry point

Everything starts with a `ToolkitRunner`. It manages the event loop, the terminal, and calls your render function each frame:

```java
var config = TuiConfig.builder()
    .tickRate(Duration.ofMillis(100))
    .build();

try (var runner = ToolkitRunner.create(config)) {
    runner.run(this::render);
}
```

The `tickRate` defines the refresh rate. 100ms is a good trade-off between responsiveness and CPU usage. The `render()` method is called every tick to rebuild the UI (immediate mode rendering: we redraw everything each frame, TamboUI optimizes the diffs on the terminal side).

# 🎨 Building a view with the Toolkit DSL

The Toolkit DSL is **declarative**: you describe your UI as a tree of elements. Let's see how to build a menu:

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

Let's break it down:
- **`column(...)`**: stacks elements vertically
- **`panel("title", content)`**: creates a bordered block with a title
- **`.rounded()`**: rounded borders (because we're civilized people)
- **`.fill()`**: the panel takes all remaining space
- **`.id("menu").focusable()`**: enables keyboard navigation between elements
- **`.onKeyEvent(this::handleMenuKey)`**: keyboard event handling

## 📋 Lists

For the menu, we use a `ListElement`:

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

The list automatically handles up/down navigation and the selected item. The `highlightSymbol` adds a visual indicator in front of the active item.

# ⌨️ Event handling

Keyboard events are handled via typed callbacks:

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

The pattern is simple:
- **`EventResult.HANDLED`**: the event is consumed, no propagation
- **`EventResult.UNHANDLED`**: the event keeps bubbling up (useful for global shortcuts)

# 💬 An interactive chat view

Here's a more complex view with a text input, a markdown response panel, and logs:

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

Notable points:
- **`textInput(inputState)`**: a text field with state managed by a `TextInputState`
- **`.onSubmit(...)`**: callback triggered when the user presses Enter
- **`markdown(...)`**: Markdown rendering directly in the terminal! (CommonMark + GFM supported)
- **`.fill(2)`**: takes 2 shares of available space (ratios, like CSS flexbox 😏)

## 📝 TextInputState

The input field state is externalized in a `TextInputState` object. This makes it easy to manipulate and reset from anywhere in the code:

```java
private final TextInputState inputState = new TextInputState();

// Clear the content
inputState.clear();

// Get the typed text
var question = inputState.text().trim();
```

# 🪟 Modal dialogs

TamboUI lets you overlay elements with `stack(...)`. Perfect for confirmation dialogs:

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

`stack(...)` layers the main view and the dialog on top. The `focusManager()` directs focus to the dialog. The `.onConfirm()` and `.onCancel()` callbacks react to Enter and Escape respectively.

# 🔄 Reactive streaming and virtual threads

A nice touch: you can update the UI from a background thread using `runOnRenderThread()`:

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

Here we use Java 21+ **virtual threads** to launch an async call, and `runOnRenderThread()` to update the UI state in a thread-safe way. Since TamboUI redraws every tick, text appears progressively token by token.

# 🧭 Navigating between views

To manage multiple screens, a simple `enum` and a `switch` in the `render()` method are all you need:

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

No need for a complex routing framework. Change the mode, the UI rebuilds automatically on the next tick.

# 🏁 Conclusion

TamboUI is a library that just feels right. In a few lines of declarative code, you get a clean, reactive TUI with colors, markdown, modal dialogs, and keyboard navigation. All in Java, compilable to native with GraalVM.

The library is still young (version 0.3.x as of writing), but it's already very usable and the project is moving fast. If you're building CLIs in Java, give it a look — you won't regret it 😊.

The full source code is available in the GitHub repository [jarvis](https://github.com/philippart-s/jarvis).

If you made it this far, thanks for reading! If you spot any typos, feel free to open an [issue or PR](https://github.com/philippart-s/blog) 😊.

Thanks to my proofreader, Fanny, who saves your eyes from bleeding too much 😘.
