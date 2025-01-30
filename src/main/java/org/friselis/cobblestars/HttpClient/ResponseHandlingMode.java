package org.friselis.cobblestars.HttpClient;

enum ResponseHandlingMode {
   IGNORE_ALL,
   HANDLE_REDIRECTS,
   HANDLE_ALL;

   // $FF: synthetic method
   private static ResponseHandlingMode[] $values() {
      return new ResponseHandlingMode[]{IGNORE_ALL, HANDLE_REDIRECTS, HANDLE_ALL};
   }
}
