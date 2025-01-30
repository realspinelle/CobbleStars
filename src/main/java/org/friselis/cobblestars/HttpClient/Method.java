package org.friselis.cobblestars.HttpClient;

enum Method {
   GET,
   POST,
   PUT,
   PATCH,
   DELETE;

   // $FF: synthetic method
   private static Method[] $values() {
      return new Method[]{GET, POST, PUT, PATCH, DELETE};
   }
}
