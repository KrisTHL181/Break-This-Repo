package net.minecraft;

import org.apache.commons.lang3.StringEscapeUtils;

public class IdentifierException extends RuntimeException {
   public IdentifierException(String p_453377_) {
      super(StringEscapeUtils.escapeJava(p_453377_));
   }

   public IdentifierException(String p_459207_, Throwable p_453497_) {
      super(StringEscapeUtils.escapeJava(p_459207_), p_453497_);
   }
}
