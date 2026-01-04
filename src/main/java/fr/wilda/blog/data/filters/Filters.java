package fr.wilda.blog.data.filters;

import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class Filters {

  /// Replace return character by `</br>`
  static RawString addBr(String text) {
    return new RawString(text.replace("\n", "</br>"));
  }
}
