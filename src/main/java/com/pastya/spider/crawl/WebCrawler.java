package com.pastya.spider.crawl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pastya.spider.fetch.FetchException;
import com.pastya.spider.fetch.FetchItem;
import com.pastya.spider.fetch.FetchItemQueue;
import com.pastya.spider.fetch.FetchResult;
import com.pastya.spider.fetch.FetchStatus;
import com.pastya.spider.fetch.Fetcher;
import com.pastya.spider.parser.Parser;

public class WebCrawler implements Runnable {

   protected static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

   /**
    * The id associated to the thread running this instance
    */
   protected int crawlerThreadId;

   private Thread crawlerThread;

   protected CrawlerThreadServicer servicer;

   private Fetcher fetcher;

   private Parser parser;

   private FetchItemQueue queue;

   private boolean isWaiting;

   public void init(int crawlerThreadId, CrawlerThreadServicer servicer) throws InstantiationException, IllegalAccessException {
      this.crawlerThreadId = crawlerThreadId;
      this.servicer = servicer;
      this.fetcher = servicer.getPageFetcher();
      this.queue = servicer.getFetchItemQueue();
      this.parser = new Parser(servicer.getConfig());
      this.isWaiting = false;
   }

   public void onStart() {
      // Do nothing by default
      // Sub-classed can override this to add their custom functionality
   }

   public void onBeforeExit() {
      // Do nothing by default
      // Sub-classed can override this to add their custom functionality
   }

   public void run() {
      while (true) {
         isWaiting = true;
         FetchItem fetchItem = queue.getPageFetchItem();
         isWaiting = false;
         if (fetchItem == null) {
            if (queue.getQueueSize() == 0) {
               return;
            }
            try {
               Thread.sleep(3000);
            } catch (InterruptedException e) {
               logger.error("Error occurred", e);
            }
         } else {
            if (servicer.isShuttingDown()) {
               logger.info("Exiting because of controller shutdown.");
               return;
            }
            if (fetchItem != null) {
               fetchItem.setStatus(FetchStatus.INPROGRESS);
               processPage(fetchItem);
               fetchItem.setStatus(FetchStatus.FETCHED);
               queue.finishPageFetchItem(fetchItem, true);
            }
         }
      }
   }

   private void processPage(FetchItem fetchItem) {
      FetchResult fetchResult = null;
      try {
         if (fetchItem == null) {
            return;
         }

         fetchResult = fetcher.fetchPage(fetchItem);
         int statusCode = fetchResult.getStatusCode();

         Page page = new Page(fetchItem);
         page.setFetchResponseHeaders(fetchResult.getResponseHeaders());
         page.setStatusCode(statusCode);
         if (statusCode < 200 || statusCode > 299) { // Not 2XX: 2XX status codes indicate success
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_MULTIPLE_CHOICES
                  || statusCode == HttpStatus.SC_SEE_OTHER || statusCode == HttpStatus.SC_TEMPORARY_REDIRECT || statusCode == 308) { // is 3xx todo

               // Need to provide support for redirection
            } else {
               String description = EnglishReasonPhraseCatalog.INSTANCE.getReason(fetchResult.getStatusCode(), Locale.ENGLISH); // Finds
               // the status reason for all known statuses
               String contentType = fetchResult.getEntity() == null ? ""
                     : fetchResult.getEntity().getContentType() == null ? "" : fetchResult.getEntity().getContentType().getValue();
               onUnexpectedStatusCode(fetchItem.getUrl(), fetchResult.getStatusCode(), contentType, description);
            }

         } else { // if status code is 200
            if (!fetchItem.getUrl().equals(fetchResult.getFetchedUrl())) {
               if (docIdServer.isSeenBefore(fetchResult.getFetchedUrl())) {
                  logger.debug("Redirect page: {} has already been seen", fetchItem.getToUrl());
                  return;
               }
               fetchItem.setToUrl(fetchResult.getFetchedUrl());
               fetchItem.setDocid(docIdServer.getNewDocID(fetchResult.getFetchedUrl()));
            }

            if (!fetchResult.fetchContent(page, servicer.getConfig().getCrawlerInfo().getMaxDownloadSize())) {
               throw new FetchException();
            }
            
            parser.parse(page, fetchItem.getToUrl());

            if (shouldFollowLinksIn(page.getWebURL())) {
               ParseData parseData = page.getParseData();
               List<WebURL> toSchedule = new ArrayList<>();
               int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
               for (WebURL webURL : parseData.getOutgoingUrls()) {
                  webURL.setParentDocid(curURL.getDocid());
                  webURL.setParentUrl(curURL.getURL());
                  int newdocid = docIdServer.getDocId(webURL.getURL());
                  if (newdocid > 0) {
                     // This is not the first time that this Url is visited. So, we set the
                     // depth to a negative number.
                     webURL.setDepth((short) -1);
                     webURL.setDocid(newdocid);
                  } else {
                     webURL.setDocid(-1);
                     webURL.setDepth((short) (curURL.getDepth() + 1));
                     if ((maxCrawlDepth == -1) || (curURL.getDepth() < maxCrawlDepth)) {
                        if (shouldVisit(page, webURL)) {
                           if (robotstxtServer.allows(webURL)) {
                              webURL.setDocid(docIdServer.getNewDocID(webURL.getURL()));
                              toSchedule.add(webURL);
                           } else {
                              logger.debug("Not visiting: {} as per the server's \"robots.txt\" " + "policy", webURL.getURL());
                           }
                        } else {
                           logger.debug("Not visiting: {} as per your \"shouldVisit\" policy", webURL.getURL());
                        }
                     }
                  }
               }
               frontier.scheduleAll(toSchedule);
            } else {
               logger.debug("Not looking for links in page {}, " + "as per your \"shouldFollowLinksInPage\" policy", page.getWebURL().getURL());
            }
            visit(page);
         }
      } catch (PageBiggerThanMaxSizeException e) {
         onPageBiggerThanMaxSize(curURL.getURL(), e.getPageSize());
      } catch (ParseException pe) {
         onParseError(curURL);
      } catch (ContentFetchException cfe) {
         onContentFetchError(curURL);
      } catch (NotAllowedContentException nace) {
         logger.debug("Skipping: {} as it contains binary content which you configured not to crawl", curURL.getURL());
      } catch (Exception e) {
         onUnhandledException(curURL, e);
      } finally {
         if (fetchResult != null) {
            fetchResult.discardContentIfNotConsumed();
         }
      }
   }

   public int getCrawlerThreadId() {
      return crawlerThreadId;
   }

   public Thread getCrawlerThread() {
      return crawlerThread;
   }

   public void setCrawlerThread(Thread crawlerThread) {
      this.crawlerThread = crawlerThread;
   }

   public CrawlerThreadServicer getCrawlServicer() {
      return servicer;
   }

}
