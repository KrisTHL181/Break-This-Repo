package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;

public class DialogTags {
   public static final TagKey<Dialog> PAUSE_SCREEN_ADDITIONS = create("pause_screen_additions");
   public static final TagKey<Dialog> QUICK_ACTIONS = create("quick_actions");

   private DialogTags() {
   }

   private static TagKey<Dialog> create(String p_410551_) {
      return TagKey.create(Registries.DIALOG, Identifier.withDefaultNamespace(p_410551_));
   }
}
