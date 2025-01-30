package org.friselis.cobblestars;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.friselis.cobblestars.HttpClient.HttpClient;

public class Translator {
   private static final Pattern TRANSLATION_RESULT = Pattern.compile("class=\"result-container\">([^<]*)</div>", 8);
   private static final Pattern LANGUAGE_KEY = Pattern.compile("<div class=\"language-item\"><a href=\"\\./m\\?sl&amp;tl=([a-zA-Z\\-]+)&amp;hl=[a-zA-Z\\-]+\">([^<]+)</a></div>", 8);
   public static Map<String, GoogleTranslateLanguage> knownLanguages = new HashMap();

   public static void init() throws URISyntaxException, IOException {
      Matcher matcher = LANGUAGE_KEY.matcher(get("https://translate.google.com/m?mui=tl"));

      while(matcher.find()) {
         String id = matcher.group(1);
         String name = matcher.group(2);
         knownLanguages.put(id, new GoogleTranslateLanguage(name, id));
      }

      if (knownLanguages.isEmpty()) {
         throw new IOException("Could not detect languages, Google likely changed the site. Please inform the maintainer of LibJF");
      }
   }

   public static String performTranslate(String textToTranslate, GoogleTranslateLanguage translateFrom, GoogleTranslateLanguage translateTo) throws Exception {
      String pageSource;
      try {
         pageSource = getPageSource(textToTranslate, translateFrom.getIdentifier(), translateTo.getIdentifier());
      } catch (Exception var6) {
         throw new Exception("Could not translate string", var6);
      }

      Matcher matcher = TRANSLATION_RESULT.matcher(pageSource);
      if (matcher.find()) {
         String match = matcher.group(1);
         if (match != null && !match.isEmpty()) {
            return StringEscapeUtils.unescapeHtml4(match);
         }
      }

      throw new Exception("Could not translate \"" + textToTranslate + "\": result page couldn't be parsed");
   }

   private static String getPageSource(String textToTranslate, String translateFrom, String translateTo) throws URISyntaxException, IOException {
      if (textToTranslate == null) {
         return null;
      } else {
         String pageUrl = String.format("https://translate.google.com/m?hl=en&sl=%s&tl=%s&ie=UTF-8&prev=_m&q=%s", translateFrom, translateTo, URLEncoder.encode(textToTranslate.trim(), StandardCharsets.UTF_8));
         return get(pageUrl);
      }
   }

   private static String get(String url) throws URISyntaxException, IOException {
      return HttpClient.get(url).sendString();
   }
}
