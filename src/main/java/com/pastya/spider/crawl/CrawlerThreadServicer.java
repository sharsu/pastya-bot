package com.pastya.spider.crawl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pastya.spider.config.CrawlConfig;
import com.pastya.spider.fetch.FetchItemQueue;
import com.pastya.spider.fetch.Fetcher;

public class CrawlerThreadServicer {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerThreadServicer.class);

   private CrawlConfig config;
   private Fetcher pageFetcher;
   private FetchItemQueue queue;
   private boolean finished;
   private boolean shuttingDown;

   protected final Object waitingLock = new Object();

   public CrawlerThreadServicer(CrawlConfig config, Fetcher fetcher) throws Exception {
      this.config = config;
      this.pageFetcher = fetcher;

      File folder = new File(config.getCrawlerInfo().getStorageFolder());
      if (!folder.exists()) {
         if (folder.mkdirs()) {
            logger.debug("Created folder: " + folder.getAbsolutePath());
         } else {
            throw new Exception("Couldn't create the storage folder: " + folder.getAbsolutePath());
         }
      }
      queue = new FetchItemQueue(config.getRunnableInfo().getMaxThreads(), config.getCrawlerInfo().getPolitenessDelay());
      finished = false;
      shuttingDown = false;
   }

   protected <T extends WebCrawler> void start(final WebCrawlerFactory<T> crawlerFactory, final int numberOfCrawlers, boolean isBlocking) {
      try {
         finished = false;
         final List<Thread> threads = new ArrayList<Thread>();
         final List<T> crawlers = new ArrayList<T>();

         for (int i = 1; i <= numberOfCrawlers; i++) {
            T crawler = crawlerFactory.newInstance();
            Thread thread = new Thread(crawler, "CrawlerThread " + i);
            crawler.setCrawlerThread(thread);
            crawler.init(i, this);
            thread.start();
            crawlers.add(crawler);
            threads.add(thread);
            logger.info("Crawler {} started", i);
         }

         final CrawlerThreadServicer servicer = this;
         final CrawlConfig config = this.getConfig();
         final int shutdownDelaySeconds = config.getRunnableInfo().getThreadShutdownDelaySeconds();

         Thread monitorThread = new Thread(new Runnable() {

            public void run() {
               try {
                  synchronized (waitingLock) {

                     while (true) {
                        sleep(shutdownDelaySeconds);
                        boolean aThreadIsAlive = false;
                        for (int i = 0; i < threads.size(); i++) {
                           Thread thread = threads.get(i);
                           if (!thread.isAlive()) {
                              if (!shuttingDown) {
                                 T crawler = crawlerFactory.newInstance();
                                 thread = new Thread(crawler, "Crawler " + (i + 1));
                                 threads.remove(i);
                                 threads.add(i, thread);
                                 crawler.setCrawlerThread(thread);
                                 crawler.init(i + 1, servicer);
                                 thread.start();
                                 crawlers.remove(i);
                                 crawlers.add(i, crawler);
                              }
                           } else {
                              aThreadIsAlive = true;
                           }
                        }
                        if (!aThreadIsAlive) {
                           sleep(shutdownDelaySeconds);

                           aThreadIsAlive = false;
                           for (int i = 0; i < threads.size(); i++) {
                              Thread thread = threads.get(i);
                              if (thread.isAlive()) {
                                 aThreadIsAlive = true;
                              }
                           }
                           if (!aThreadIsAlive) {
                              if (!shuttingDown) {
                                 long queueLength = queue.getQueueSize();
                                 if (queueLength > 0) {
                                    continue;
                                 }
                                 sleep(shutdownDelaySeconds);
                                 queueLength = queue.getQueueSize();
                                 if (queueLength > 0) {
                                    continue;
                                 }
                              }
                              sleep(config.getRunnableInfo().getCleanupDelaySeconds());

                              pageFetcher.shutDown();

                              finished = true;
                              waitingLock.notifyAll();
                              return;
                           }
                        }
                     }
                  }
               } catch (Exception e) {
                  logger.error("Unexpected Error", e);
               }
            }
         });

         monitorThread.start();

         if (isBlocking) {
            waitUntilFinish();
         }

      } catch (Exception e) {
         logger.error("Error happened", e);
      }
   }

   /**
    * Wait until this crawling session finishes.
    */
   public void waitUntilFinish() {
      while (!finished) {
         synchronized (waitingLock) {
            if (finished) {
               return;
            }
            try {
               waitingLock.wait();
            } catch (InterruptedException e) {
               logger.error("Error occurred", e);
            }
         }
      }
   }

   protected static void sleep(int seconds) {
      try {
         Thread.sleep(seconds * 1000);
      } catch (InterruptedException ignored) {
         // Do nothing
      }
   }

   public CrawlConfig getConfig() {
      return config;
   }

   public Fetcher getPageFetcher() {
      return pageFetcher;
   }

   public FetchItemQueue getFetchItemQueue() {
      return queue;
   }   

   public boolean isFinished() {
      return finished;
   }

   public boolean isShuttingDown() {
      return shuttingDown;
   }

   public void shutdown() {
      logger.info("Shutting down...");
      this.shuttingDown = true;
      pageFetcher.shutDown();
   }

   public interface WebCrawlerFactory<T extends WebCrawler> {
      T newInstance() throws Exception;
   }

   private static class DefaultWebCrawlerFactory<T extends WebCrawler> implements WebCrawlerFactory<T> {
      final Class<T> clazz;

      DefaultWebCrawlerFactory(Class<T> clazz) {
         this.clazz = clazz;
      }

      public T newInstance() throws Exception {
         try {
            return clazz.newInstance();
         } catch (ReflectiveOperationException e) {
            throw e;
         }
      }
   }
}
