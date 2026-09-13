package net.minecraft;

import org.apache.commons.lang3.StringEscapeUtils;

public class IdentifierException extends RuntimeException {
   public IdentifierException(final String message) {
      super(StringEscapeUtils.escapeJava(message));
   }

   public IdentifierException(final String message, final Throwable cause) {
      super(StringEscapeUtils.escapeJava(message), cause);
   }
}
