package org.friselis.cobblestars;

public record GoogleTranslateLanguage(String name, String id) implements Language {
   public static final GoogleTranslateLanguage AUTO_DETECT = new GoogleTranslateLanguage("Auto-Detect", "auto");

   public GoogleTranslateLanguage(String name, String id) {
      this.name = name;
      this.id = id;
   }

   public String toString() {
      return this.name;
   }

   public String getDisplayName() {
      return this.name;
   }

   public String getIdentifier() {
      return this.id;
   }

   public String name() {
      return this.name;
   }

   public String id() {
      return this.id;
   }
}
