package fr.wilda.blog.processor;

import fr.wilda.blog.data.Talk;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Comparator;
import java.util.List;

/// Helper bean to query the conferences data (a flat array loaded from `data/conferences.yml`).
/// The conference *pages* are now generated natively by Roq (`site.collections.conferences.from-data`),
/// so this bean only provides the cross-cutting views: the "by topic" list and the year grouping.
/// Injected as `myConfs` for use in Qute.
@ApplicationScoped
@Named("myConfs")
public class ConferencesProcessor {

    // The `data/conferences.yml` file is a top-level array, so the `conferences` bean is a JsonArray.
    @Inject
    @Named("conferences")
    JsonArray conferences;

    /// Returns every occurrence of a talk (across all conferences) matching the given talk id.
    /// Used by the "Talks" page to list where each topic was given.
    /// @param id The talk id (e.g. `picocli`)
    /// @return The matching talks with their conference context
    public List<Talk> getByIds(String id) {
        return conferences.stream()
                .map(c -> (JsonObject) c)
                .flatMap(event -> event.getJsonArray("talks").stream()
                        .map(t -> (JsonObject) t)
                        .filter(talk -> id.equals(talk.getString("id")))
                        .map(talk -> new Talk(talk.getString("id"),
                                event.getString("name"),
                                talk.getString("date"),
                                event.getString("postDate"),
                                event.getString("talksUrl"))))
                .toList();
    }

    /// @return The distinct years, most recent first, for the conferences listing.
    public List<String> getYears() {
        return conferences.stream()
                .map(c -> (JsonObject) c)
                .map(c -> c.getString("year"))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /// @param year The year to filter on
    /// @return The conferences held during the given year, most recent first (sorted by `postDate`)
    public List<JsonObject> getByYear(String year) {
        return conferences.stream()
                .map(c -> (JsonObject) c)
                .filter(c -> year.equals(c.getString("year")))
                .sorted(Comparator.comparing((JsonObject c) -> c.getString("postDate")).reversed())
                .toList();
    }
}
