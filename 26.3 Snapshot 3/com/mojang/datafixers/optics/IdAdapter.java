package com.mojang.datafixers.optics;

class IdAdapter<S, T> implements Adapter<S, T, S, T> {
   static final IdAdapter<?, ?> INSTANCE = new IdAdapter();

   private IdAdapter() {
   }

   @Override
   public S from(S s) {
      return s;
   }

   @Override
   public T to(T b) {
      return b;
   }

   @Override
   public String toString() {
      return "id";
   }
}
