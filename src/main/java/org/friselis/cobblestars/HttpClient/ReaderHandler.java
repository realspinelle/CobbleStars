package org.friselis.cobblestars.HttpClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Subscription;

public record ReaderHandler(BodySubscriber<InputStream> delegate) implements BodySubscriber<Reader> {
   public ReaderHandler(BodySubscriber<InputStream> delegate) {
      this.delegate = delegate;
   }

   public static BodyHandler<Reader> of() {
      return (responseInfo) -> {
         return new ReaderHandler(BodySubscribers.ofInputStream());
      };
   }

   public CompletionStage<Reader> getBody() {
      return this.delegate.getBody().thenApply((in) -> {
         return in == null ? null : new InputStreamReader(in);
      });
   }

   public void onSubscribe(Subscription subscription) {
      this.delegate.onSubscribe(subscription);
   }

   public void onNext(List<ByteBuffer> item) {
      this.delegate.onNext(item);
   }

   public void onError(Throwable throwable) {
      this.delegate.onError(throwable);
   }

   public void onComplete() {
      this.delegate.onComplete();
   }

   public BodySubscriber<InputStream> delegate() {
      return this.delegate;
   }
}
