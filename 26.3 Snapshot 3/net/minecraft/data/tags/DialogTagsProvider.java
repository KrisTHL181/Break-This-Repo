package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.tags.DialogTags;

public class DialogTagsProvider extends TagsProvider<Dialog> {
   public DialogTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, Registries.DIALOG, lookupProvider);
   }

   @Override
   protected void addTags(final HolderLookup.Provider registries) {
      this.tag(DialogTags.PAUSE_SCREEN_ADDITIONS);
      this.tag(DialogTags.QUICK_ACTIONS);
   }
}
