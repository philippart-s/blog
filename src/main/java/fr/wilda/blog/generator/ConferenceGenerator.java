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

    /// Called once the QUarkus application is ready
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
