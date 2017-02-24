package com.pastya.spider.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;

import com.pastya.spider.config.CrawlConfig;
import com.pastya.spider.crawl.Page;
import com.pastya.spider.fetch.FetchItem;

public class Parser {
   protected static final Logger logger = LoggerFactory.getLogger(Parser.class);

   private final CrawlConfig config;
   private final HtmlParser htmlParser;
   private final ParseContext parseContext;
   private static final Pattern pattern = initializePattern();

   public Parser(CrawlConfig config) throws InstantiationException, IllegalAccessException {
      this.config = config;
      htmlParser = new HtmlParser();
      parseContext = new ParseContext();
   }

   public void parse(Page page, String contextURL) throws Exception {
      if (Parser.hasBinaryContent(page.getContentType())) { // Images
         ParseData parseData = new ParseData();
         if (config.getUrlFilters().isSkipImages()) {
            parseData.setContent("");
            page.setParseData(parseData);
         }
      } else if (Parser.hasPlainTextContent(page.getContentType())) { // plain Text
         try {
            ParseData parseData = new ParseData();
            if (page.getContentCharset() == null) {
               parseData.setContent(new String(page.getContentData()));
            } else {
               parseData.setContent(new String(page.getContentData(), page.getContentCharset()));
            }
            parseData.setOutgoingUrls(Parser.extractUrls(parseData.getContent()));
            page.setParseData(parseData);
         } catch (Exception e) {
            logger.error("{}, while parsing: {}", e.getMessage(), page.getFetchItem().getToUrl());
            throw new ParseException(e);
         }
      } else { // isHTML
         Metadata metadata = new Metadata();
         DefaultHandler contentHandler = new DefaultHandler();
         try (InputStream inputStream = new ByteArrayInputStream(page.getContentData())) {
            htmlParser.parse(inputStream, contentHandler, metadata, parseContext);
         } catch (Exception e) {
            logger.error("{}, while parsing: {}", e.getMessage(), page.getFetchItem().getToUrl());
            throw e;
         }

         if (page.getContentCharset() == null) {
            page.setContentCharset(metadata.get("Content-Encoding"));
         }

         ParseData parseData = new ParseData();
         parseData.setText(contentHandler.getBodyText().trim());
         Set<FetchItem> outgoingUrls = new HashSet<FetchItem>();

         String baseURL = contentHandler.getBaseUrl();
         if (baseURL != null) {
            contextURL = baseURL;
         }

         int urlCount = 0;
         for (ExtractedUrlAnchorPair urlAnchorPair : contentHandler.getOutgoingUrls()) {

            String href = urlAnchorPair.getHref();
            if ((href == null) || href.trim().isEmpty()) {
               continue;
            }

            String hrefLoweredCase = href.trim().toLowerCase();
            if (!hrefLoweredCase.contains("javascript:") && !hrefLoweredCase.contains("mailto:") && !hrefLoweredCase.contains("@")) {
               String url = URLCanonicalizer.getCanonicalURL(href, contextURL);
               if (url != null) {
                  FetchItem fetchItem = FetchItem.create(url, 0);
                  fetchItem.setTag(urlAnchorPair.getTag());
                  fetchItem.setAnchor(urlAnchorPair.getAnchor());
                  outgoingUrls.add(fetchItem);
                  urlCount++;
                  if (urlCount > config.getUrlFilters().getMaxOutgoingLinksToFollow()) {
                     break;
                  }
               }
            }
         }
         parseData.setOutgoingUrls(outgoingUrls);

         try {
            if (page.getContentCharset() == null) {
               parseData.setContent(new String(page.getContentData()));
            } else {
               parseData.setContent(new String(page.getContentData(), page.getContentCharset()));
            }

            page.setParseData(parseData);
         } catch (UnsupportedEncodingException e) {
            logger.error("error parsing the html: " + page.getFetchItem().getToUrl(), e);
            throw new ParseException(e);
         }
      }
   }

   private static boolean hasBinaryContent(String contentType) {
      String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

      return typeStr.contains("image") || typeStr.contains("audio") || typeStr.contains("video") || typeStr.contains("application");
   }

   private static boolean hasPlainTextContent(String contentType) {
      String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

      return typeStr.contains("text") && !typeStr.contains("html");
   }

   private static Set<String> extractUrls(String input) {
       Set<String> extractedUrls = new HashSet<>();

       if (input != null) {
           Matcher matcher = pattern.matcher(input);
           while (matcher.find()) {
               String urlStr = matcher.group();
               if (!urlStr.startsWith("http")) {
                   urlStr = "http://" + urlStr;
               }
               extractedUrls.add(urlStr);
           }
       }

       return extractedUrls;
   }

   // Pattern shamelessly borrowed from Another project
   private static Pattern initializePattern() {
       return Pattern.compile("\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
                              "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
                              "|mil|biz|info|mobi|name|aero|jobs|museum" +
                              "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
                              "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
                              "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                              "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
                              "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
                              "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
                              "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
   }   
}
