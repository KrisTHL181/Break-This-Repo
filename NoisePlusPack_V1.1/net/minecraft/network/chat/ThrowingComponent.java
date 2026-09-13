package net.minecraft.network.chat;

public class ThrowingComponent extends Exception {
   private final Component component;

   public ThrowingComponent(Component p_237304_) {
      super(p_237304_.getString());
      this.component = p_237304_;
   }

   public ThrowingComponent(Component p_237306_, Throwable p_237307_) {
      super(p_237306_.getString(), p_237307_);
      this.component = p_237306_;
   }

   public Component getComponent() {
      return this.component;
   }
}
