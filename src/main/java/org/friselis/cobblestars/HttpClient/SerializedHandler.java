package org.friselis.cobblestars.HttpClient;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;

public record SerializedHandler<T>(BodySubscriber<Reader> delegate, Serializer serializer, Type type) implements BodySubscriber<Either<T, IOException>> {
   public SerializedHandler(BodySubscriber<Reader> delegate, Serializer serializer, Type type) {
      this.delegate = delegate;
      this.serializer = serializer;
      this.type = type;
   }

   public static <T> BodyHandler<Either<T, IOException>> of(Serializer serializer, Type type) {
      return (responseInfo) -> {
         return new SerializedHandler(new ReaderHandler(BodySubscribers.ofInputStream()), serializer, type);
      };
   }

   public CompletionStage<Either<T, IOException>> getBody() {
      return this.delegate.getBody().thenApply((in) -> {
         try {
            return Either.left(in == null ? null : this.serializer.deserialize(in, this.type));
         } catch (IOException var3) {
            return Either.right(var3);
         }
      });
   }

   public void onSubscribe(Subscription subscription) {
      this.delegate.onSubscribe(subscription);
   }

   public void onNext(List<ByteBuffer> byteBuffers) {
      this.delegate.onNext(byteBuffers);
   }

   public void onError(Throwable throwable) {
      this.delegate.onError(throwable);
   }

   public void onComplete() {
      this.delegate.onComplete();
   }

   public BodySubscriber<Reader> delegate() {
      return this.delegate;
   }

   public Serializer serializer() {
      return this.serializer;
   }

   public Type type() {
      return this.type;
   }
}
