package com.pastya.spider.fetch;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pastya.spider.crawl.Page;

public class FetchResult {

   protected static final Logger logger = LoggerFactory.getLogger(FetchResult.class);

   protected int statusCode;
   protected HttpEntity entity = null;
   protected Header[] responseHeaders = null;
   protected String fetchedUrl = null;
   protected String movedToUrl = null;
   
   public int getStatusCode() {
      return statusCode;
  }

  public void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
  }

  public HttpEntity getEntity() {
      return entity;
  }

  public void setEntity(HttpEntity entity) {
      this.entity = entity;
  }

  public Header[] getResponseHeaders() {
      return responseHeaders;
  }

  public void setResponseHeaders(Header[] responseHeaders) {
      this.responseHeaders = responseHeaders;
  }

  public String getFetchedUrl() {
      return fetchedUrl;
  }

  public void setFetchedUrl(String fetchedUrl) {
      this.fetchedUrl = fetchedUrl;
  }

  public boolean fetchContent(Page page, int maxBytes) {
      try {
          page.load(entity, maxBytes);
          page.setFetchResponseHeaders(responseHeaders);
          return true;
      } catch (Exception e) {
          logger.info("Exception while fetching content for: {} [{}]", page.getFetchItem().toUrl,
                      e.getMessage());
      }
      return false;
  }

  public void discardContentIfNotConsumed() {
      try {
          if (entity != null) {
              EntityUtils.consume(entity);
          }
      } catch (IOException ignored) {
          // We can EOFException (extends IOException) exception. It can happen on compressed
          // streams which are not
          // repeatable
          // We can ignore this exception. It can happen if the stream is closed.
      } catch (Exception e) {
          logger.warn("Unexpected error occurred while trying to discard content", e);
      }
  }

  public String getMovedToUrl() {
      return movedToUrl;
  }

  public void setMovedToUrl(String movedToUrl) {
      this.movedToUrl = movedToUrl;
  }
}
