package com.pastya.spider.fetch;

import java.io.IOException;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pastya.spider.config.CrawlConfig;
import com.pastya.spider.utils.UrlHelper;

public class Fetcher {

   protected static final Logger logger = LoggerFactory.getLogger(Fetcher.class);
   protected final Object mutex = new Object();
   protected PoolingHttpClientConnectionManager connectionManager;
   protected CloseableHttpClient httpClient;
   protected long lastFetchTime = 0;
   protected ConnectionMonitorThread connectionMonitorThread = null;
   protected CrawlConfig config;

   public Fetcher(CrawlConfig config) {

      this.config = config;
      RequestConfig requestConfig = RequestConfig.custom().setExpectContinueEnabled(false).setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(false)
            .setSocketTimeout(config.getRunnableInfo().getSocketTimeout()).setConnectTimeout(config.getRunnableInfo().getConnectionTimeout()).build();

      RegistryBuilder<ConnectionSocketFactory> connRegistryBuilder = RegistryBuilder.create();
      connRegistryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);

      Registry<ConnectionSocketFactory> connRegistry = connRegistryBuilder.build();
      connectionManager = new PoolingHttpClientConnectionManager(connRegistry);

      HttpClientBuilder clientBuilder = HttpClientBuilder.create();
      clientBuilder.setDefaultRequestConfig(requestConfig);
      clientBuilder.setConnectionManager(connectionManager);
      clientBuilder.setUserAgent(config.getAgentInfo().getUserAgent());

      httpClient = clientBuilder.build();

      if (connectionMonitorThread == null) {
         connectionMonitorThread = new ConnectionMonitorThread(connectionManager);
      }
      connectionMonitorThread.start();
   }

   public FetchResult fetchPage(FetchItem item) throws InterruptedException, IOException {
      // Getting URL, setting headers & content
      FetchResult fetchResult = new FetchResult();
      String toFetchURL = item.toUrl;
      HttpUriRequest request = null;
      try {
         request = newHttpUriRequest(toFetchURL);
         // Applying Politeness delay
         synchronized (mutex) {
            long now = (new Date()).getTime();
            if ((now - lastFetchTime) < config.getCrawlerInfo().getPolitenessDelay()) {
               Thread.sleep(config.getCrawlerInfo().getPolitenessDelay() - (now - lastFetchTime));
            }
            lastFetchTime = (new Date()).getTime();
         }

         CloseableHttpResponse response = httpClient.execute(request);
         fetchResult.setEntity(response.getEntity());
         fetchResult.setResponseHeaders(response.getAllHeaders());

         // Setting HttpStatus
         int statusCode = response.getStatusLine().getStatusCode();

         // If Redirect ( 3xx )
         if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_MULTIPLE_CHOICES
               || statusCode == HttpStatus.SC_SEE_OTHER || statusCode == HttpStatus.SC_TEMPORARY_REDIRECT || statusCode == 308) {

            Header header = response.getFirstHeader("Location");
            if (header != null) {
               String movedToUrl = UrlHelper.getPath(UrlHelper.getDomain(toFetchURL) + header.getValue());
               fetchResult.setMovedToUrl(movedToUrl);
            }
         } if (statusCode >= 200 && statusCode <= 299) { // status is 2XX
            fetchResult.setFetchedUrl(toFetchURL);
            String uri = request.getURI().toString();
            if (!uri.equals(toFetchURL)) {
               if (!UrlHelper.getPath(uri).equals(toFetchURL)) {
                  fetchResult.setFetchedUrl(uri);
               }
            }

            // Checking maximum size
            if (fetchResult.getEntity() != null) {
               long size = fetchResult.getEntity().getContentLength();
               if (size == -1) {
                  Header length = response.getLastHeader("Content-Length");
                  if (length == null) {
                     length = response.getLastHeader("Content-length");
                  }
                  if (length != null) {
                     size = Integer.parseInt(length.getValue());
                  }
               }
               if (size > config.getCrawlerInfo().getMaxDownloadSize()) {
                  response.close();
                  throw new RuntimeException(
                        String.format("Page bigger than the configured maximum download size {}", config.getCrawlerInfo().getMaxDownloadSize()));
               }
            }
         }

         fetchResult.setStatusCode(statusCode);
         return fetchResult;

      } finally {
         if ((fetchResult.getEntity() == null) && (request != null)) {
            request.abort();
         }
      }
   }

   public synchronized void shutDown() {
      if (connectionMonitorThread != null) {
         connectionManager.shutdown();
         connectionMonitorThread.shutdown();
      }
   }

   /**
    * Creates a new HttpUriRequest for the given url.
    *
    * @param url
    *           the url to be fetched
    * @return the HttpUriRequest for the given url
    */
   protected HttpUriRequest newHttpUriRequest(String url) {
      return new HttpGet(url);
   }
}
