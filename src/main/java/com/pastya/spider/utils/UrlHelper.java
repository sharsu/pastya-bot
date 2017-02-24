package com.pastya.spider.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class UrlHelper {
   private static final Logger LOG = LoggerFactory.getLogger(UrlHelper.class.getName());
   private static final Pattern URL_PROTOCOL_REGEX = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
   
   public static String getDomain(String url) {
      try {
         return getDomainChecked(url);
      } catch (URISyntaxException e) {
         LOG.info("Malformed url: " + url);
         return null;
      }
   }

   public static String getDomainChecked(String url) throws URISyntaxException {
      Preconditions.checkNotNull(url);
      url = addProtocol(url);
      return new URI(url).getHost();
   }

   public static String getPath(String url) {
      Preconditions.checkNotNull(url);
      url = addProtocol(url);
      try {
         return new URI(url).getPath();
      } catch (URISyntaxException e) {
         LOG.info("Malformed url: " + url);
         return null;
      }
   }

   public static String stripUrlParameters(String url) {
      Preconditions.checkNotNull(url);
      int paramStartIndex = url.indexOf("?");
      if (paramStartIndex == -1) {
         return url;
      } else {
         return url.substring(0, paramStartIndex);
      }
   }

   public static String stripUrlParameters(URL url) {
      return stripUrlParameters(url.toString());
   }

   public static String addProtocol(String url) {
      Preconditions.checkNotNull(url);
      Matcher matcher = URL_PROTOCOL_REGEX.matcher(url);
      if (!matcher.find()) {
         url = "http://" + url;
      }
      return url;
   }

   public static List<String> getDomainLevels(String host) {
      Preconditions.checkNotNull(host);

      // Automatically include www prefix if not present.
      if (!host.startsWith("www")) {
         host = "www." + host;
      }

      Joiner joiner = Joiner.on(".");
      List<String> domainParts = Lists.newLinkedList(Arrays.asList(host.split("\\.")));
      List<String> levels = Lists.newLinkedList();

      while (!domainParts.isEmpty()) {
         levels.add(joiner.join(domainParts));
         domainParts.remove(0);
      }

      return levels;
   }
   
   public static long byteArray2Long(byte[] b) {
      int value = 0;
      for (int i = 0; i < 8; i++) {
          int shift = (8 - 1 - i) * 8;
          value += (b[i] & 0x000000FF) << shift;
      }
      return value;
  }   
}
