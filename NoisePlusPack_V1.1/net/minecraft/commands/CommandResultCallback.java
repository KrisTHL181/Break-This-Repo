package net.minecraft.commands;

@FunctionalInterface
public interface CommandResultCallback {
   CommandResultCallback EMPTY = new CommandResultCallback() {
      @Override
      public void onResult(boolean p_310694_, int p_309781_) {
      }

      @Override
      public String toString() {
         return "<empty>";
      }
   };

   void onResult(boolean var1, int var2);

   default void onSuccess(int p_312969_) {
      this.onResult(true, p_312969_);
   }

   default void onFailure() {
      this.onResult(false, 0);
   }

   static CommandResultCallback chain(CommandResultCallback p_312991_, CommandResultCallback p_310583_) {
      if (p_312991_ == EMPTY) {
         return p_310583_;
      } else {
         return p_310583_ == EMPTY ? p_312991_ : (p_311372_, p_312527_) -> {
            p_312991_.onResult(p_311372_, p_312527_);
            p_310583_.onResult(p_311372_, p_312527_);
         };
      }
   }
}
