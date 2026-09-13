package net.minecraft.world.inventory;

import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface RemoteSlot {
   RemoteSlot PLACEHOLDER = new RemoteSlot() {
      @Override
      public void receive(final HashedStack incoming) {
      }

      @Override
      public void force(final ItemStack outgoing) {
      }

      @Override
      public boolean matches(final ItemStack local) {
         return true;
      }
   };

   void force(ItemStack outgoing);

   void receive(HashedStack incoming);

   boolean matches(ItemStack local);

   class Synchronized implements RemoteSlot {
      private final HashedPatchMap.HashGenerator hasher;
      private @Nullable ItemStack remoteStack = null;
      private @Nullable HashedStack remoteHash = null;

      public Synchronized(final HashedPatchMap.HashGenerator hasher) {
         this.hasher = hasher;
      }

      @Override
      public void force(final ItemStack outgoing) {
         this.remoteStack = outgoing.copy();
         this.remoteHash = null;
      }

      @Override
      public void receive(final HashedStack incoming) {
         this.remoteStack = null;
         this.remoteHash = incoming;
      }

      @Override
      public boolean matches(final ItemStack local) {
         if (this.remoteStack != null) {
            return ItemStack.matches(this.remoteStack, local);
         } else if (this.remoteHash != null && this.remoteHash.matches(local, this.hasher)) {
            this.remoteStack = local.copy();
            return true;
         } else {
            return false;
         }
      }

      public void copyFrom(final RemoteSlot.Synchronized other) {
         this.remoteStack = other.remoteStack;
         this.remoteHash = other.remoteHash;
      }
   }
}
