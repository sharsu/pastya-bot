package com.pastya.spider.parser;

import java.util.HashSet;
import java.util.Set;

public class ParseData {

   private Set<String> outgoingUrls = new HashSet<String>();
   private String content = null;

   public String getContent() {
      return content;
   }

   public void setContent(String content) {
      this.content = content;
   }

   public Set<String> getOutgoingUrls() {
      return outgoingUrls;
   }

   public void setOutgoingUrls(Set<String> outgoingUrls) {
      this.outgoingUrls = outgoingUrls;
   }
}
