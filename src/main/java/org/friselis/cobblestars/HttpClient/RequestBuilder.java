package org.friselis.cobblestars.HttpClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class RequestBuilder {
   private static final Predicate<String> CURSEFORGE_API = Pattern.compile("(?:http(s)?://)?addons-ecs\\.forgesvc\\.net/api/+").asMatchPredicate();
   private final String url;
   private final Builder builder;
   private Method method;
   private String accept;
   private int sent = 0;
   private int retryAfterDefault = 5000;
   private int retryAfterMax = 15000;
   private int retryLimit = 3;
   private ResponseHandlingMode responseHandlingMode;
   private List<Exception> retryExceptions;

   protected RequestBuilder(Method method, String url) throws URISyntaxException {
      this.responseHandlingMode = ResponseHandlingMode.HANDLE_ALL;
      this.retryExceptions = null;
      this.url = url.replace(" ", "%20");
      this.builder = HttpRequest.newBuilder().uri(new URI(this.url));
      this.method = method;
      this.userAgent(HttpClient.userAgent);
   }

   public RequestBuilder bearer(String token) {
      this.builder.header("Authorization", "Bearer " + token);
      return this;
   }

   public RequestBuilder header(String name, String value) {
      this.builder.header(name, value);
      return this;
   }

   public RequestBuilder setHeader(String name, String value) {
      this.builder.setHeader(name, value);
      return this;
   }

   public RequestBuilder timeout(Duration duration) {
      this.builder.timeout(duration);
      return this;
   }

   public RequestBuilder ignoreAll() {
      this.responseHandlingMode = ResponseHandlingMode.IGNORE_ALL;
      return this;
   }

   public RequestBuilder handleRedirects() {
      this.responseHandlingMode = ResponseHandlingMode.HANDLE_REDIRECTS;
      return this;
   }

   public RequestBuilder configureRetryAfter(int defaultDelay, int maxDelay) {
      if (defaultDelay < 1) {
         throw new IllegalArgumentException("defaultDelay must be greater than zero");
      } else if (maxDelay < defaultDelay) {
         throw new IllegalArgumentException("maxDelay must be greater than or equal to defaultDelay");
      } else {
         this.retryAfterDefault = defaultDelay;
         this.retryAfterMax = maxDelay;
         return this;
      }
   }

   public RequestBuilder setRetryLimit(int limit) {
      if (limit < 0) {
         throw new IllegalArgumentException("limit must be greater than or zero");
      } else {
         this.retryLimit = limit;
         return this;
      }
   }

   public RequestBuilder accept(String value) {
      this.accept = value;
      return this;
   }

   public RequestBuilder userAgent(String value) {
      return this.setHeader("User-Agent", value);
   }

   public RequestBuilder bodyString(String string) {
      this.builder.header("Content-Type", "text/plain");
      this.builder.method(this.method.name(), BodyPublishers.ofString(string));
      this.method = null;
      return this;
   }

   public RequestBuilder bodyForm(String string) {
      this.builder.header("Content-Type", "application/x-www-form-urlencoded");
      this.builder.method(this.method.name(), BodyPublishers.ofString(string));
      this.method = null;
      return this;
   }

   public RequestBuilder bodyForm(Map<String, String> entries) {
      return this.bodyForm((String)entries.entrySet().stream().map((entry) -> {
         String var10000 = URLEncoder.encode((String)entry.getKey(), StandardCharsets.UTF_8);
         return var10000 + "=" + URLEncoder.encode((String)entry.getValue(), StandardCharsets.UTF_8);
      }).collect(Collectors.joining("&")));
   }

   public RequestBuilder bodyJson(String string) {
      this.builder.header("Content-Type", "application/json");
      this.builder.method(this.method.name(), BodyPublishers.ofString(string));
      this.method = null;
      return this;
   }

   public RequestBuilder bodySerialized(Object object) throws IOException {
      Serializer serializer = Serializer.getInstance();
      this.builder.header("Content-Type", serializer.getFormatMime());
      this.builder.method(this.method.name(), BodyPublishers.ofString(serializer.serialize(object)));
      this.method = null;
      return this;
   }

   private <T> HttpResponse<T> _sendResponse(@Nullable String accept, BodyHandler<T> responseBodyHandler) throws IOException {
      ++this.sent;
      if (this.sent > this.retryLimit) {
         IOException e = new IOException("Attempted to reconnect/redirect " + this.sent + " times, which is more than the permitted " + this.retryLimit + ". Stopping");
         if (this.retryExceptions != null) {
            Iterator var13 = this.retryExceptions.iterator();

            while(var13.hasNext()) {
               Exception ex = (Exception)var13.next();
               e.addSuppressed(ex);
            }
         }

         throw e;
      } else {
         if (this.accept != null) {
            this.builder.header("Accept", this.accept);
         } else if (accept != null) {
            this.builder.header("Accept", accept);
         }

         if (this.method != null) {
            this.builder.method(this.method.name(), BodyPublishers.noBody());
         }

         if (HttpClient.PROXY_AUTH != null) {
            this.builder.header("Proxy-Authorization", HttpClient.PROXY_AUTH);
         }

         HttpResponse res;
         try {
            res = HttpClient.CLIENT.send(this.builder.build(), responseBodyHandler);
         } catch (InterruptedException var10) {
            throw new IOException("Could not send request", var10);
         } catch (IOException var11) {
            String message = var11.getMessage();
            if (message != null && message.contains("GOAWAY received")) {
               return this.handleRetryAfter(accept, responseBodyHandler, this.retryAfterDefault);
            }

            throw new IOException("Could not send request", var11);
         }

         if (res.statusCode() == 429 && (res.uri().getHost() + res.uri().getPath()).equals("www.google.com/sorry/index")) {
            throw new IOException("Google detected the request as a bot and blocked it. Please try again later.");
         } else if (this.responseHandlingMode == ResponseHandlingMode.IGNORE_ALL) {
            return res;
         } else if (res.statusCode() / 100 == 2) {
            return res;
         } else {
            Optional<String> location = res.headers().firstValue("location");
            Optional<Integer> retryAfter = res.headers().firstValue("Retry-After").flatMap((s) -> {
               try {
                  return Optional.of(Integer.parseInt(s));
               } catch (NumberFormatException var2) {
                  return Optional.empty();
               }
            });
            String exceptionSuffix = " (URL=" + this.url + ")";
            HttpResponse var7;
            HttpResponse var10000;
            int var10002;
            switch(res.statusCode()) {
            case 302:
            case 307:
               if (location.isPresent() && this.method == Method.GET) {
                  try {
                     var7 = HttpClient.get((String)location.get())._sendResponse(accept, responseBodyHandler);
                  } catch (URISyntaxException var9) {
                     throw new IOException("Could not follow redirect" + exceptionSuffix, var9);
                  }

                  var10000 = var7;
               } else {
                  if (this.responseHandlingMode != ResponseHandlingMode.HANDLE_REDIRECTS) {
                     var10002 = res.statusCode();
                     throw new IOException("Unexpected redirect: " + var10002 + exceptionSuffix);
                  }

                  var10000 = res;
               }
               break;
            case 404:
               if (this.responseHandlingMode != ResponseHandlingMode.HANDLE_REDIRECTS) {
                  throw new FileNotFoundException("Didn't find anything under that url" + exceptionSuffix);
               }

               var10000 = res;
               break;
            case 429:
               var7 = this.handleRetryAfter(accept, responseBodyHandler, (Integer)retryAfter.map((s) -> {
                  return s * 1000;
               }).orElse(this.retryAfterDefault));
               var10000 = var7;
               break;
            case 500:
            case 502:
            case 503:
            case 504:
            case 507:
               if (this.responseHandlingMode == ResponseHandlingMode.HANDLE_REDIRECTS) {
                  var10000 = res;
               } else {
                  if (!CURSEFORGE_API.test(this.url)) {
                     var10002 = res.statusCode();
                     throw new IOException("Unexpected serverside error: " + var10002 + exceptionSuffix);
                  }

                  var7 = this.handleRetryAfter(accept, responseBodyHandler, Math.min(1000, this.retryAfterMax));
                  var10000 = var7;
               }
               break;
            default:
               if (this.responseHandlingMode != ResponseHandlingMode.HANDLE_REDIRECTS) {
                  var10002 = res.statusCode();
                  throw new IOException("Unexpected return method: " + var10002 + exceptionSuffix);
               }

               var10000 = res;
            }

            return var10000;
         }
      }
   }

   private <T> HttpResponse<T> handleRetryAfter(@Nullable String accept, BodyHandler<T> responseBodyHandler, int millis) throws IOException {
      if (millis > this.retryAfterMax) {
         throw new HttpTimeoutException("Wait time specified by Retry-After is too long: " + millis);
      } else {
         try {
            Thread.sleep((long)millis);
         } catch (InterruptedException var5) {
            throw new IOException("Could not sleep before resending request" + var5);
         }

         return this._sendResponse(accept, responseBodyHandler);
      }
   }

   private <T> T unwrap(HttpResponse<T> response) throws IOException {
      return response.body();
   }

   public void send() throws IOException {
      this.unwrap(this.sendResponse());
   }

   public HttpResponse<Void> sendResponse() throws IOException {
      return this._sendResponse((String)null, BodyHandlers.discarding());
   }

   public InputStream sendInputStream() throws IOException {
      return (InputStream)this.unwrap(this.sendInputStreamResponse());
   }

   public HttpResponse<InputStream> sendInputStreamResponse() throws IOException {
      return this._sendResponse((String)null, BodyHandlers.ofInputStream());
   }

   public Reader sendReader() throws IOException {
      return (Reader)this.unwrap(this.sendReaderResponse());
   }

   public HttpResponse<Reader> sendReaderResponse() throws IOException {
      return this._sendResponse((String)null, ReaderHandler.of());
   }

   public String sendString() throws IOException {
      return (String)this.unwrap(this.sendStringResponse());
   }

   public HttpResponse<String> sendStringResponse() throws IOException {
      return this._sendResponse((String)null, BodyHandlers.ofString());
   }

   public Stream<String> sendLines() throws IOException {
      return (Stream)this.unwrap(this.sendLinesResponse());
   }

   public HttpResponse<Stream<String>> sendLinesResponse() throws IOException {
      return this._sendResponse((String)null, BodyHandlers.ofLines());
   }

   public <T> T sendSerialized(Type type) throws IOException {
      Either<T, IOException> tmp = (Either)this.unwrap(this.sendSerializedResponse(type));
      if (tmp == null) {
         return null;
      } else if (tmp.isLeft()) {
         return tmp.left();
      } else {
         throw new IOException("Could not deserialize", (Throwable)tmp.right());
      }
   }

   public <T> HttpResponse<Either<T, IOException>> sendSerializedResponse(Type type) throws IOException {
      Serializer serializer = (Serializer)Objects.requireNonNull(Serializer.getInstance());
      return this._sendResponse(serializer.getFormatMime(), SerializedHandler.of(serializer, type));
   }
}
