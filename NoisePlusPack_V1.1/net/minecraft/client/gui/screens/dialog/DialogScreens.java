package net.minecraft.client.gui.screens.dialog;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.ServerLinksDialog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DialogScreens {
   private static final Map<MapCodec<? extends Dialog>, DialogScreens.Factory<?>> FACTORIES = new HashMap<>();

   private static <T extends Dialog> void register(MapCodec<T> p_408218_, DialogScreens.Factory<? super T> p_408705_) {
      FACTORIES.put(p_408218_, p_408705_);
   }

   public static <T extends Dialog> @Nullable DialogScreen<T> createFromData(T p_406293_, @Nullable Screen p_406264_, DialogConnectionAccess p_407821_) {
      DialogScreens.Factory<T> factory = (DialogScreens.Factory<T>)FACTORIES.get(p_406293_.codec());
      return factory != null ? factory.create(p_406264_, p_406293_, p_407821_) : null;
   }

   public static void bootstrap() {
      register(ConfirmationDialog.MAP_CODEC, SimpleDialogScreen::new);
      register(NoticeDialog.MAP_CODEC, SimpleDialogScreen::new);
      register(DialogListDialog.MAP_CODEC, DialogListDialogScreen::new);
      register(MultiActionDialog.MAP_CODEC, MultiButtonDialogScreen::new);
      register(ServerLinksDialog.MAP_CODEC, ServerLinksDialogScreen::new);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Factory<T extends Dialog> {
      DialogScreen<T> create(@Nullable Screen var1, T var2, DialogConnectionAccess var3);
   }
}
