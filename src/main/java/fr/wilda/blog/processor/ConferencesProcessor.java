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
