package org.friselis.cobblestars.HttpClient;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.util.Base64;

public class HttpClient {
   protected static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
   protected static final String PROXY_AUTH;
   protected static final java.net.http.HttpClient CLIENT;

   public static void setUserAgent(String hostname) {
      if (hostname != null && !hostname.isEmpty()) {
         userAgent = hostname;
      } else {
         throw new IllegalArgumentException("Hostname cannot be empty");
      }
   }

   public static RequestBuilder get(String url) throws URISyntaxException {
      return new RequestBuilder(Method.GET, url);
   }

   public static RequestBuilder post(String url) throws URISyntaxException {
      return new RequestBuilder(Method.POST, url);
   }

   public static RequestBuilder put(String url) throws URISyntaxException {
      return new RequestBuilder(Method.PUT, url);
   }

   public static RequestBuilder patch(String url) throws URISyntaxException {
      return new RequestBuilder(Method.PATCH, url);
   }

   public static RequestBuilder delete(String url) throws URISyntaxException {
      return new RequestBuilder(Method.DELETE, url);
   }

   static {
      System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
      Builder clientBuilder = java.net.http.HttpClient.newBuilder().followRedirects(Redirect.ALWAYS);
      String host = System.getProperty("http.proxyHost");
      String port = System.getProperty("http.proxyPort");
      if (host != null && port != null) {
         if (port.matches("\\d+")) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(host, Integer.parseInt(port))));
         } else {
            System.err.println("Could not parse proxy settings: Port is not a number");
         }
      }

      String name = System.getProperty("http.proxyUserName");
      String pass = System.getProperty("http.proxyUserPassword");
      if (name != null && pass != null) {
         String var10000 = new String(Base64.getEncoder().encode((name + ":" + pass).getBytes()));
         PROXY_AUTH = "Basic " + var10000;
      } else {
         PROXY_AUTH = null;
      }

      CLIENT = clientBuilder.build();
   }
}
