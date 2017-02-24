package com.pastya.spider.crawl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import com.pastya.spider.fetch.FetchItem;
import com.pastya.spider.parser.ParseData;

public class Page {

   /**
    * Item fetched in this page object
    */
   protected FetchItem fetchItem;

   /**
    * Status of the fetch
    */
   protected int statusCode;

   /**
    * The content of this page in binary format.
    */
   protected byte[] contentData;

   /**
    * The ContentType of this page. For example: "text/html; charset=UTF-8"
    */
   protected String contentType;

   /**
    * The charset of the content. For example: "UTF-8"
    */
   protected String contentCharset;

   /**
    * Language of the Content.
    */
   private String language;

   /**
    * Headers which were present in the response of the fetch request
    */
   protected Header[] fetchResponseHeaders;

   /**
    * The parsed data populated by parsers
    */
   protected ParseData parseData;

   public Page(FetchItem fetchItem) {
      this.fetchItem = fetchItem;
   }

   /**
    * Loads the content of this page from a fetched HttpEntity.
    *
    * @param entity
    *           HttpEntity
    * @param maxBytes
    *           The maximum number of bytes to read
    * @throws Exception
    *            when load fails
    */
   public void load(HttpEntity entity, int maxBytes) throws Exception {

      contentType = null;
      Header type = entity.getContentType();
      if (type != null) {
         contentType = type.getValue();
      }

      Charset charset = ContentType.getOrDefault(entity).getCharset();
      if (charset != null) {
         contentCharset = charset.displayName();
      }

      contentData = toByteArray(entity, maxBytes);
   }

   /**
    * Read contents from an entity, with a specified maximum. This is a replacement of EntityUtils.toByteArray because that function does not impose a maximum
    * size.
    *
    * @param entity
    *           The entity from which to read
    * @param maxBytes
    *           The maximum number of bytes to read
    * @return A byte array containing maxBytes or fewer bytes read from the entity
    *
    * @throws IOException
    *            Thrown when reading fails for any reason
    */
   protected byte[] toByteArray(HttpEntity entity, int maxBytes) throws IOException {
      if (entity == null) {
         return new byte[0];
      }

      InputStream is = entity.getContent();
      int size = (int) entity.getContentLength();
      if (size <= 0 || size > maxBytes) {
         size = maxBytes;
      }

      int actualSize = 0;

      byte[] buf = new byte[size];
      while (actualSize < size) {
         int remain = size - actualSize;
         int readBytes = is.read(buf, actualSize, Math.min(remain, 1500));

         if (readBytes <= 0) {
            break;
         }
         actualSize += readBytes;
      }

      // If the actual size matches the size of the buffer, do not copy it
      if (actualSize == buf.length) {
         return buf;
      }

      // Return the subset of the byte buffer that was used
      return Arrays.copyOfRange(buf, 0, actualSize);
   }

   public FetchItem getFetchItem() {
      return fetchItem;
   }

   public void setFetchItem(FetchItem fetchItem) {
      this.fetchItem = fetchItem;
   }

   public int getStatusCode() {
      return statusCode;
   }

   public void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
   }

   /**
    * Returns headers which were present in the response of the fetch request
    *
    * @return Header Array, the response headers
    */
   public Header[] getFetchResponseHeaders() {
      return fetchResponseHeaders;
   }

   public void setFetchResponseHeaders(Header[] headers) {
      fetchResponseHeaders = headers;
   }

   /**
    * @return parsed data generated for this page by parsers
    */
   public ParseData getParseData() {
      return parseData;
   }

   public void setParseData(ParseData parseData) {
      this.parseData = parseData;
   }

   /**
    * @return content of this page in binary format.
    */
   public byte[] getContentData() {
      return contentData;
   }

   public void setContentData(byte[] contentData) {
      this.contentData = contentData;
   }

   /**
    * @return ContentType of this page. For example: "text/html; charset=UTF-8"
    */
   public String getContentType() {
      return contentType;
   }

   public void setContentType(String contentType) {
      this.contentType = contentType;
   }

   /**
    * @return charset of the content. For example: "UTF-8"
    */
   public String getContentCharset() {
      return contentCharset;
   }

   public void setContentCharset(String contentCharset) {
      this.contentCharset = contentCharset;
   }

   public String getLanguage() {
      return language;
   }

   public void setLanguage(String language) {
      this.language = language;
   }
}
