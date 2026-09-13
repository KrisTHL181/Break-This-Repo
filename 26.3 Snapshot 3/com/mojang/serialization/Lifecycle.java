package com.mojang.serialization;

public class Lifecycle {
   private static final Lifecycle STABLE = new Lifecycle() {
      @Override
      public String toString() {
         return "Stable";
      }
   };
   private static final Lifecycle EXPERIMENTAL = new Lifecycle() {
      @Override
      public String toString() {
         return "Experimental";
      }
   };

   private Lifecycle() {
   }

   public static Lifecycle experimental() {
      return EXPERIMENTAL;
   }

   public static Lifecycle stable() {
      return STABLE;
   }

   public static Lifecycle deprecated(int since) {
      return new Lifecycle.Deprecated(since);
   }

   public Lifecycle add(Lifecycle other) {
      if (this == EXPERIMENTAL || other == EXPERIMENTAL) {
         return EXPERIMENTAL;
      } else if (this instanceof Lifecycle.Deprecated) {
         return other instanceof Lifecycle.Deprecated && ((Lifecycle.Deprecated)other).since < ((Lifecycle.Deprecated)this).since ? other : this;
      } else {
         return other instanceof Lifecycle.Deprecated ? other : STABLE;
      }
   }

   public static final class Deprecated extends Lifecycle {
      private final int since;

      public Deprecated(int since) {
         this.since = since;
      }

      public int since() {
         return this.since;
      }
   }
}
