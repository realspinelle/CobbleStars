package org.friselis.cobblestars.HttpClient;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public interface Serializer {
   static Serializer getInstance() {
      return SerializerHolder.getInstance();
   }

   static void setInstance(Serializer instance) {
      SerializerHolder.setInstance(instance);
   }

   String serialize(Object var1) throws IOException;

   default void serialize(Object object, Appendable writer) throws IOException {
      writer.append(this.serialize(object));
   }

   default <T> T deserialize(Reader source, Class<T> typeOfT) throws IOException {
      return this.deserialize((Reader)source, (Type)typeOfT);
   }

   <T> T deserialize(Reader var1, Type var2) throws IOException;

   default <T> T deserialize(String source, Class<T> typeOfT) throws IOException {
      return this.deserialize((String)source, (Type)typeOfT);
   }

   <T> T deserialize(String var1, Type var2) throws IOException;

   String getFormatMime();

   public static class SerializeException extends IOException {
      public SerializeException(String message) {
         super(message);
      }

      public SerializeException(String message, Throwable cause) {
         super(message, cause);
      }

      public SerializeException(Throwable cause) {
         super(cause);
      }
   }
}
