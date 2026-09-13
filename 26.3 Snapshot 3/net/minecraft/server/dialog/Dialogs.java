package net.minecraft.server.dialog;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DialogTags;

public class Dialogs {
   public static final ResourceKey<Dialog> SERVER_LINKS = create("server_links");
   public static final ResourceKey<Dialog> CUSTOM_OPTIONS = create("custom_options");
   public static final ResourceKey<Dialog> QUICK_ACTIONS = create("quick_actions");
   public static final int BIG_BUTTON_WIDTH = 310;
   private static final ActionButton DEFAULT_BACK_BUTTON = new ActionButton(new CommonButtonData(CommonComponents.GUI_BACK, 200), Optional.empty());

   private static ResourceKey<Dialog> create(final String id) {
      return ResourceKey.create(Registries.DIALOG, Identifier.withDefaultNamespace(id));
   }

   public static void bootstrap(final BootstrapContext<Dialog> context) {
      HolderGetter<Dialog> dialogs = context.lookup(Registries.DIALOG);
      context.register(
         SERVER_LINKS,
         new ServerLinksDialog(
            new CommonDialogData(
               Component.translatable("menu.server_links.title"),
               Optional.of(Component.translatable("menu.server_links")),
               true,
               true,
               DialogAction.CLOSE,
               List.of(),
               List.of()
            ),
            Optional.of(DEFAULT_BACK_BUTTON),
            1,
            310
         )
      );
      context.register(
         CUSTOM_OPTIONS,
         new DialogListDialog(
            new CommonDialogData(
               Component.translatable("menu.custom_options.title"),
               Optional.of(Component.translatable("menu.custom_options")),
               true,
               true,
               DialogAction.CLOSE,
               List.of(),
               List.of()
            ),
            dialogs.getOrThrow(DialogTags.PAUSE_SCREEN_ADDITIONS),
            Optional.of(DEFAULT_BACK_BUTTON),
            1,
            310
         )
      );
      context.register(
         QUICK_ACTIONS,
         new DialogListDialog(
            new CommonDialogData(
               Component.translatable("menu.quick_actions.title"),
               Optional.of(Component.translatable("menu.quick_actions")),
               true,
               true,
               DialogAction.CLOSE,
               List.of(),
               List.of()
            ),
            dialogs.getOrThrow(DialogTags.QUICK_ACTIONS),
            Optional.of(DEFAULT_BACK_BUTTON),
            1,
            310
         )
      );
   }
}
