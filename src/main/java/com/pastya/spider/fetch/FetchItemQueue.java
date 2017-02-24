package com.pastya.spider.fetch;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchItemQueue {
   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   List<FetchItem> queue = Collections.synchronizedList(new LinkedList<FetchItem>());
   List<Long> seenPages = Collections.synchronizedList(new LinkedList<Long>());
   List<Long> pagesInProgess = Collections.synchronizedList(new LinkedList<Long>());
   AtomicInteger inProgress = new AtomicInteger();
   AtomicLong nextFetchTime = new AtomicLong();
   AtomicInteger exceptionCounter = new AtomicInteger();
   long crawlDelay;
   int maxThreads;
   private final Object mutex = new Object();
   private int lastItemId;
   
   public FetchItemQueue(int maxThreads, long crawlDelay) {
      this.maxThreads = maxThreads;
      this.crawlDelay = crawlDelay;
      setEndTime(System.currentTimeMillis() - crawlDelay);
   }

   public synchronized int emptyQueue() {
      int presize = queue.size();
      queue.clear();
      seenPages.clear();
      return presize;
   }

   public int getQueueSize() {
      return queue.size();
   }

   public int getInProgressSize() {
      return inProgress.get();
   }

   public int incrementExceptionCounter() {
      return exceptionCounter.incrementAndGet();
   }

   public void finishPageFetchItem(FetchItem item, boolean asap) {
      if (item != null) {
         inProgress.decrementAndGet();
         pagesInProgess.remove(item.getItemId());
         setEndTime(System.currentTimeMillis(), asap);
      }
   }

   public void addPageFetchItem(FetchItem item) {
      if (item == null)
         return;
      queue.add(item);
   }

   public void addInProgressPageFetchItem(FetchItem item) {
      if (item == null)
         return;
      inProgress.incrementAndGet();
   }

   public FetchItem getPageFetchItem() {
      if (inProgress.get() >= maxThreads)
         return null;
      long now = System.currentTimeMillis();
      if (nextFetchTime.get() > now)
         return null;
      FetchItem item = null;
      if (queue.size() == 0)
         return null;
      try {
         item = queue.remove(0);
         inProgress.incrementAndGet();
      } catch (Exception e) {
         LOG.error("Cannot remove FetchItem from queue or cannot add it to inProgress queue", e);
      }
      return item;
   }

   public synchronized void dump() {
      LOG.info("  maxThreads    = " + maxThreads);
      LOG.info("  inProgress    = " + inProgress.get());
      LOG.info("  crawlDelay    = " + crawlDelay);
      LOG.info("  nextFetchTime = " + nextFetchTime.get());
      LOG.info("  now           = " + System.currentTimeMillis());
      for (int i = 0; i < queue.size(); i++) {
         FetchItem item = queue.get(i);
         LOG.info("  " + i + ". " + item.toUrl);
      }
   }

   private void setEndTime(long endTime) {
      setEndTime(endTime, false);
   }

   private void setEndTime(long endTime, boolean asap) {
      if (!asap)
         nextFetchTime.set(endTime + crawlDelay);
      else
         nextFetchTime.set(endTime);
   }
}
