package com.mojang.authlib;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.Validate;

public abstract class HttpDiscoveryService implements DiscoveryService {
   private final Proxy proxy;

   protected HttpDiscoveryService(Proxy proxy) {
      Validate.notNull(proxy);
      this.proxy = proxy;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public static URL constantURL(String url) {
      try {
         return new URL(url);
      } catch (MalformedURLException ex) {
         throw new Error("Couldn't create constant for " + url, ex);
      }
   }

   public static String buildQuery(Map<String, Object> query) {
      if (query == null) {
         return "";
      }

      StringBuilder builder = new StringBuilder();

      for (Entry<String, Object> entry : query.entrySet()) {
         if (builder.length() > 0) {
            builder.append('&');
         }

         builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
         if (entry.getValue() != null) {
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
         }
      }

      return builder.toString();
   }

   public static URL concatenateURL(URL url, String query) {
      try {
         return url.getQuery() != null && url.getQuery().length() > 0
            ? new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query)
            : new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
      } catch (MalformedURLException ex) {
         throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
      }
   }
}
