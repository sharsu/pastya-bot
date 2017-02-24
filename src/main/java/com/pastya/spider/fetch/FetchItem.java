package com.pastya.spider.fetch;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchItem {

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   
   protected long itemId;
   protected String toUrl = null;
   protected URL url = null;
   protected String anchor = null;
   private FetchItem parentNode;
   private FetchStatus status = FetchStatus.QUEUED;
   private String title = null;
   private long fetchTime = 0;
   protected int linkDepth = 0;

   private FetchItem(long itemId, String toUrl) {
      this(itemId, toUrl, 0);
   }

   private FetchItem(long itemId, String toUrl, int linkDepth) {
      this.toUrl = toUrl;
      this.itemId = itemId;
      this.linkDepth = linkDepth;
   }

   public static FetchItem create(String toUrl, int linkDepth) {
      long itemId;
      URL u = null;
      try {
         u = new URL(toUrl.toString());
      } catch (Exception e) {
         LOG.warn("Cannot parse url: " + toUrl, e);
         return null;
      }
      final String protocol = u.getProtocol().toLowerCase();
      String key;
      try {
         final InetAddress addr = InetAddress.getByName(u.getHost());
         key = addr.getHostAddress();
      } catch (final UnknownHostException e) {
         LOG.warn("Unable to resolve: " + u.getHost() + ", skipping.");
         return null;
      }
      long = protocol + "://" + key.toLowerCase();
      FetchItem item = new FetchItem(itemId, toUrl, linkDepth);
      item.setUrl(u);
      return item;
   }
   
   public long getItemId() {
      return itemId;
   }
   
   public URL getUrl() {
      return url;
   }

   public void setUrl(URL url) {
      this.url = url;
   }
   
   public String getToUrl() {
      return toUrl;
   }

   public void setToUrl(String toUrl) {
      this.toUrl = toUrl;
   }   
   
   public void setAnchor(String anchor) {
      this.anchor = anchor;
   }

   public FetchItem getParentNode() {
      return parentNode;
   }

   public void setParentNode(FetchItem parentNode) {
      this.parentNode = parentNode;
   }

   public FetchStatus getStatus() {
      return status;
   }

   public void setStatus(FetchStatus status) {
      this.status = status;
   }

   public String getTitle() {
      return title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public long getFetchTime() {
      return fetchTime;
   }

   public void setFetchTime(long fetchTime) {
      this.fetchTime = fetchTime;
   }
}
