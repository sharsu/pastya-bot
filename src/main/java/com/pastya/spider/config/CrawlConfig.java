package com.pastya.spider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Crawler.
 *
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 */
@ConfigurationProperties(prefix = "pastya-bot", ignoreUnknownFields = false)
public class CrawlConfig {

   private UrlFilters urlFilters;

   private AgentInfo agentInfo;

   private RunnableInfo runnableInfo;

   private CrawlerInfo crawlerInfo;

   public UrlFilters getUrlFilters() {
      return urlFilters;
   }

   public void setUrlFilters(UrlFilters urlFilters) {
      this.urlFilters = urlFilters;
   }

   public AgentInfo getAgentInfo() {
      return agentInfo;
   }

   public void setAgentInfo(AgentInfo agentInfo) {
      this.agentInfo = agentInfo;
   }

   public RunnableInfo getRunnableInfo() {
      return runnableInfo;
   }

   public void setRunnableInfo(RunnableInfo runnableInfo) {
      this.runnableInfo = runnableInfo;
   }

   public CrawlerInfo getCrawlerInfo() {
      return crawlerInfo;
   }

   public void setCrawlerInfo(CrawlerInfo crawlerInfo) {
      this.crawlerInfo = crawlerInfo;
   }

   public static class RunnableInfo {

      /**
       * Socket timeout in milliseconds
       */
      private int socketTimeout = 20000;

      /**
       * Connection timeout in milliseconds
       */
      private int connectionTimeout = 30000;

      /**
       * Wait this long before checking the status of the worker threads.
       */
      private int threadMonitoringDelaySeconds = 10;

      /**
       * Wait this long to verify the craweler threads are finished working.
       */
      private int threadShutdownDelaySeconds = 10;

      /**
       * Wait this long in seconds before launching cleanup.
       */
      private int cleanupDelaySeconds = 10;
      
      private int maxThreads = 7;

      public int getMaxThreads() {
         return maxThreads;
      }

      public void setMaxThreads(int maxThreads) {
         this.maxThreads = maxThreads;
      }

      public int getSocketTimeout() {
         return socketTimeout;
      }

      public void setSocketTimeout(int socketTimeout) {
         this.socketTimeout = socketTimeout;
      }

      public int getConnectionTimeout() {
         return connectionTimeout;
      }

      public void setConnectionTimeout(int connectionTimeout) {
         this.connectionTimeout = connectionTimeout;
      }

      public int getThreadMonitoringDelaySeconds() {
         return threadMonitoringDelaySeconds;
      }

      public void setThreadMonitoringDelaySeconds(int delay) {
         this.threadMonitoringDelaySeconds = delay;
      }

      public int getThreadShutdownDelaySeconds() {
         return threadShutdownDelaySeconds;
      }

      public void setThreadShutdownDelaySeconds(int delay) {
         this.threadShutdownDelaySeconds = delay;
      }

      public int getCleanupDelaySeconds() {
         return cleanupDelaySeconds;
      }

      public void setCleanupDelaySeconds(int delay) {
         this.cleanupDelaySeconds = delay;
      }
   }

   public static class CrawlerInfo {
      /**
       * The folder which will be used by crawler for storing the intermediate crawl data. The content of this folder should not be modified manually.
       */
      private String storageFolder;

      /**
       * If this feature is enabled, you would be able to resume a previously stopped/crashed crawl. However, it makes crawling slightly slower
       */
      private boolean resumable = false;

      /**
       * Politeness delay in milliseconds (delay between sending two requests to the same host).
       */
      private int politenessDelay = 200;

      /**
       * Max allowed size of a page. Pages larger than this size will not be fetched.
       */
      private int maxDownloadSize = 1048576;

      /**
       * Maximum depth of crawling. Default value is -1 which represents no restriction.
       */
      private int maxDepth = -1;

      /**
       * Maximum number of pages to fetch. For no restriction this variable should be set to -1.
       */
      private int maxPages = -1;

      public String getStorageFolder() {
         return storageFolder;
      }

      /**
       * The folder which will be used by crawler for storing the intermediate crawl data. The content of this folder should not be modified manually.
       *
       * @param storageFolder
       *           The folder for the storage
       */
      public void setStorageFolder(String storageFolder) {
         this.storageFolder = storageFolder;
      }

      public boolean isResumable() {
         return resumable;
      }

      /**
       * If this feature is enabled, you would be able to resume a previously stopped/crashed crawl. However, it makes crawling slightly slower
       *
       * @param resumable
       *           Should crawling be resumable between runs ?
       */
      public void setResumable(boolean resumable) {
         this.resumable = resumable;
      }

      public int getMaxDepth() {
         return maxDepth;
      }

      public void setMaxDepth(int maxDepth) {
         this.maxDepth = maxDepth;
      }

      public int getMaxPages() {
         return maxPages;
      }

      public void setMaxPages(int maxPages) {
         this.maxPages = maxPages;
      }

      public int getPolitenessDelay() {
         return politenessDelay;
      }

      public void setPolitenessDelay(int politenessDelay) {
         this.politenessDelay = politenessDelay;
      }

      public int getMaxDownloadSize() {
         return maxDownloadSize;
      }

      public void setMaxDownloadSize(int maxDownloadSize) {
         this.maxDownloadSize = maxDownloadSize;
      }
   }

   public static class AgentInfo {

      /**
       * user-agent string representing the crawler to web servers. See http://en.wikipedia.org/wiki/User_agent for more details
       */
      private String userAgent = "pastya-bot";

      public String getUserAgent() {
         return userAgent;
      }

      public void setUserAgent(String userAgent) {
         this.userAgent = userAgent;
      }
   }

   public static class UrlFilters {

      private boolean skipHttpsPages = true;
      private boolean skipImages = false;
      private boolean skipRedirects = false;
      private boolean skipFtpFiles = true;
      private boolean skipMailToUrls = true;
      private boolean skipOutgoingLinks = false;
      private int maxOutgoingLinksToFollow = 5000;

      public boolean isSkipHttpsPages() {
         return skipHttpsPages;
      }

      public void setSkipHttpsPages(boolean skipHttpsPages) {
         this.skipHttpsPages = skipHttpsPages;
      }

      public boolean isSkipImages() {
         return skipImages;
      }

      public void setSkipImages(boolean skipImages) {
         this.skipImages = skipImages;
      }

      public boolean isSkipRedirects() {
         return skipRedirects;
      }

      public void setSkipRedirects(boolean skipRedirects) {
         this.skipRedirects = skipRedirects;
      }

      public boolean isSkipFtpFiles() {
         return skipFtpFiles;
      }

      public void setSkipFtpFiles(boolean skipFtpFiles) {
         this.skipFtpFiles = skipFtpFiles;
      }

      public boolean isSkipMailToUrls() {
         return skipMailToUrls;
      }

      public void setSkipMailToUrls(boolean skipMailToUrls) {
         this.skipMailToUrls = skipMailToUrls;
      }

      public boolean isSkipOutgoingLinks() {
         return skipOutgoingLinks;
      }

      public void setSkipOutgoingLinks(boolean skipOutgoingLinks) {
         this.skipOutgoingLinks = skipOutgoingLinks;
      }

      public int getMaxOutgoingLinksToFollow() {
         return maxOutgoingLinksToFollow;
      }

      public void setMaxOutgoingLinksToFollow(int maxOutgoingLinksToFollow) {
         this.maxOutgoingLinksToFollow = maxOutgoingLinksToFollow;
      }
   }

   /**
    * Validates the configs specified by this instance.
    *
    * @throws Exception
    *            on Validation fail
    */
   public void validate() throws Exception {
      if (crawlerInfo.storageFolder == null) {
         throw new Exception("Crawl storage folder is not set in the CrawlConfig.");
      }
      if (crawlerInfo.politenessDelay < 0) {
         throw new Exception("Invalid value for politeness delay: " +crawlerInfo. politenessDelay);
      }
      if (crawlerInfo.maxDepth < -1) {
         throw new Exception("Maximum crawl depth should be either a positive number or -1 for unlimited depth" + ".");
      }
      if (crawlerInfo.maxDepth > Short.MAX_VALUE) {
         throw new Exception("Maximum value for crawl depth is " + Short.MAX_VALUE);
      }
   }
}
