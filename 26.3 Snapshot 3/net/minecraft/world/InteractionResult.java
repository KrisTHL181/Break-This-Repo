package net.minecraft.world;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public sealed interface InteractionResult
   permits InteractionResult.Success,
   InteractionResult.Fail,
   InteractionResult.Pass,
   InteractionResult.TryEmptyHandInteraction {
   InteractionResult.Success SUCCESS = new InteractionResult.Success(InteractionResult.SwingSource.CLIENT, InteractionResult.ItemContext.DEFAULT);
   InteractionResult.Success SUCCESS_SERVER = new InteractionResult.Success(InteractionResult.SwingSource.SERVER, InteractionResult.ItemContext.DEFAULT);
   InteractionResult.Success CONSUME = new InteractionResult.Success(InteractionResult.SwingSource.NONE, InteractionResult.ItemContext.DEFAULT);
   InteractionResult.Fail FAIL = new InteractionResult.Fail();
   InteractionResult.Pass PASS = new InteractionResult.Pass();
   InteractionResult.TryEmptyHandInteraction TRY_WITH_EMPTY_HAND = new InteractionResult.TryEmptyHandInteraction();

   default boolean consumesAction() {
      return false;
   }

   record Fail() implements InteractionResult {
   }

   record ItemContext(boolean wasItemInteraction, @Nullable ItemStack heldItemTransformedTo) {
      public static final InteractionResult.ItemContext NONE = new InteractionResult.ItemContext(false, null);
      public static final InteractionResult.ItemContext DEFAULT = new InteractionResult.ItemContext(true, null);
   }

   record Pass() implements InteractionResult {
   }

   record Success(InteractionResult.SwingSource swingSource, InteractionResult.ItemContext itemContext) implements InteractionResult {
      @Override
      public boolean consumesAction() {
         return true;
      }

      public InteractionResult.Success heldItemTransformedTo(final ItemStack itemStack) {
         return new InteractionResult.Success(this.swingSource, new InteractionResult.ItemContext(true, itemStack));
      }

      public InteractionResult.Success withoutItem() {
         return new InteractionResult.Success(this.swingSource, InteractionResult.ItemContext.NONE);
      }

      public boolean wasItemInteraction() {
         return this.itemContext.wasItemInteraction;
      }

      public @Nullable ItemStack heldItemTransformedTo() {
         return this.itemContext.heldItemTransformedTo;
      }
   }

   enum SwingSource {
      NONE,
      CLIENT,
      SERVER;
   }

   record TryEmptyHandInteraction() implements InteractionResult {
   }
}
