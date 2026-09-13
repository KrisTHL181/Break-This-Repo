package net.minecraft.client.gui.screens.dialog.body;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DialogBodyHandlers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<MapCodec<? extends DialogBody>, DialogBodyHandler<?>> HANDLERS = new HashMap<>();

   private static <B extends DialogBody> void register(MapCodec<B> p_406762_, DialogBodyHandler<? super B> p_409072_) {
      HANDLERS.put(p_406762_, p_409072_);
   }

   private static <B extends DialogBody> @Nullable DialogBodyHandler<B> getHandler(B p_407699_) {
      return (DialogBodyHandler<B>)HANDLERS.get(p_407699_.mapCodec());
   }

   public static <B extends DialogBody> @Nullable LayoutElement createBodyElement(DialogScreen<?> p_406596_, B p_407924_) {
      DialogBodyHandler<B> dialogbodyhandler = getHandler(p_407924_);
      if (dialogbodyhandler == null) {
         LOGGER.warn("Unrecognized dialog body {}", p_407924_);
         return null;
      } else {
         return dialogbodyhandler.createControls(p_406596_, p_407924_);
      }
   }

   public static void bootstrap() {
      register(PlainMessage.MAP_CODEC, new DialogBodyHandlers.PlainMessageHandler());
      register(ItemBody.MAP_CODEC, new DialogBodyHandlers.ItemHandler());
   }

   static void runActionOnParent(DialogScreen<?> p_409938_, @Nullable Style p_409083_) {
      if (p_409083_ != null) {
         ClickEvent clickevent = p_409083_.getClickEvent();
         if (clickevent != null) {
            p_409938_.runAction(Optional.of(clickevent));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ItemHandler implements DialogBodyHandler<ItemBody> {
      public LayoutElement createControls(DialogScreen<?> p_409486_, ItemBody p_410433_) {
         if (p_410433_.description().isPresent()) {
            PlainMessage plainmessage = p_410433_.description().get();
            LinearLayout linearlayout = LinearLayout.horizontal().spacing(2);
            linearlayout.defaultCellSetting().alignVerticallyMiddle();
            ItemDisplayWidget itemdisplaywidget = new ItemDisplayWidget(
               Minecraft.getInstance(),
               0,
               0,
               p_410433_.width(),
               p_410433_.height(),
               CommonComponents.EMPTY,
               p_410433_.item(),
               p_410433_.showDecorations(),
               p_410433_.showTooltip()
            );
            linearlayout.addChild(itemdisplaywidget);
            linearlayout.addChild(
               FocusableTextWidget.builder(plainmessage.contents(), p_409486_.getFont())
                  .maxWidth(plainmessage.width())
                  .alwaysShowBorder(false)
                  .backgroundFill(FocusableTextWidget.BackgroundFill.NEVER)
                  .build()
                  .setComponentClickHandler(p_407925_ -> DialogBodyHandlers.runActionOnParent(p_409486_, p_407925_))
            );
            return linearlayout;
         } else {
            return new ItemDisplayWidget(
               Minecraft.getInstance(),
               0,
               0,
               p_410433_.width(),
               p_410433_.height(),
               p_410433_.item().getHoverName(),
               p_410433_.item(),
               p_410433_.showDecorations(),
               p_410433_.showTooltip()
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class PlainMessageHandler implements DialogBodyHandler<PlainMessage> {
      public LayoutElement createControls(DialogScreen<?> p_405971_, PlainMessage p_408125_) {
         return FocusableTextWidget.builder(p_408125_.contents(), p_405971_.getFont())
            .maxWidth(p_408125_.width())
            .alwaysShowBorder(false)
            .backgroundFill(FocusableTextWidget.BackgroundFill.NEVER)
            .build()
            .setCentered(true)
            .setComponentClickHandler(p_409257_ -> DialogBodyHandlers.runActionOnParent(p_405971_, p_409257_));
      }
   }
}
