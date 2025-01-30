package org.friselis.cobblestars.HttpClient;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class SerializerHolder {
   private static Serializer instance;

   @NotNull
   public static Serializer getInstance() {
      return instance;
   }

   public static void setInstance(@NotNull Serializer instance) {
      SerializerHolder.instance = (Serializer)Objects.requireNonNull(instance);
   }
}
