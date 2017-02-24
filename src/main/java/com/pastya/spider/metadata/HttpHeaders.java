package com.pastya.spider.metadata;

/**
 * A collection of HTTP header names.
 * 
 * @see <a href="https://www.ietf.org/rfc/rfc2616.txt">Hypertext Transfer Protocol
 *      -- HTTP/1.1 (RFC 2616)</a>
 */
public interface HttpHeaders {

  public final static String TRANSFER_ENCODING = "Transfer-Encoding";

  public final static String CONTENT_ENCODING = "Content-Encoding";

  public final static String CONTENT_LANGUAGE = "Content-Language";

  public final static String CONTENT_LENGTH = "Content-Length";

  public final static String CONTENT_LOCATION = "Content-Location";

  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  public final static String CONTENT_MD5 = "Content-MD5";

  public final static String CONTENT_TYPE = "Content-Type";

  // public static final Text WRITABLE_CONTENT_TYPE = new Text(CONTENT_TYPE);

  public final static String LAST_MODIFIED = "Last-Modified";

  public final static String LOCATION = "Location";
}