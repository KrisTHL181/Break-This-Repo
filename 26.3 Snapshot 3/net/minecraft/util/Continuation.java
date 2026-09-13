package net.minecraft.util;

public enum Continuation {
   CONTINUE,
   ABORT;

   public boolean shouldAbort() {
      return this == ABORT;
   }

   public static Continuation abortIf(final boolean abortIf) {
      return abortIf ? ABORT : CONTINUE;
   }

   public static Continuation continueIf(final boolean continueIf) {
      return abortIf(!continueIf);
   }
}
