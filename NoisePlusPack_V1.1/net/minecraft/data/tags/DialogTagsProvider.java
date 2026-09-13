package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.tags.DialogTags;

public class DialogTagsProvider extends KeyTagProvider<Dialog> {
   public DialogTagsProvider(PackOutput p_410242_, CompletableFuture<HolderLookup.Provider> p_406497_) {
      super(p_410242_, Registries.DIALOG, p_406497_);
   }

   @Override
   protected void addTags(HolderLookup.Provider p_410377_) {
      this.tag(DialogTags.PAUSE_SCREEN_ADDITIONS);
      this.tag(DialogTags.QUICK_ACTIONS);
   }
}
