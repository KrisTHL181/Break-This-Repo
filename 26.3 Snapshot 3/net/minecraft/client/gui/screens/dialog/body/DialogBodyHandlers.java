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
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DialogBodyHandlers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<MapCodec<? extends DialogBody>, DialogBodyHandler<?>> HANDLERS = new HashMap<>();

   private static <B extends DialogBody> void register(final MapCodec<B> type, final DialogBodyHandler<? super B> handler) {
      HANDLERS.put(type, handler);
   }

   private static <B extends DialogBody> @Nullable DialogBodyHandler<B> getHandler(final B body) {
      return (DialogBodyHandler<B>)HANDLERS.get(body.mapCodec());
   }

   public static <B extends DialogBody> @Nullable LayoutElement createBodyElement(final DialogScreen<?> screen, final B body) {
      DialogBodyHandler<B> handler = getHandler(body);
      if (handler == null) {
         LOGGER.warn("Unrecognized dialog body {}", body);
         return null;
      } else {
         return handler.createControls(screen, body);
      }
   }

   public static void bootstrap() {
      register(PlainMessage.MAP_CODEC, new DialogBodyHandlers.PlainMessageHandler());
      register(ItemBody.MAP_CODEC, new DialogBodyHandlers.ItemHandler());
   }

   private static void runActionOnParent(final DialogScreen<?> parent, final @Nullable Style clickedStyle) {
      if (clickedStyle != null) {
         ClickEvent clickEvent = clickedStyle.getClickEvent();
         if (clickEvent != null) {
            parent.runAction(Optional.of(clickEvent));
         }
      }
   }

   private static class ItemHandler implements DialogBodyHandler<ItemBody> {
      public LayoutElement createControls(final DialogScreen<?> parent, final ItemBody item) {
         ItemStack displayStack = item.item().create();
         if (item.description().isPresent()) {
            PlainMessage description = item.description().get();
            LinearLayout layout = LinearLayout.horizontal().spacing(2);
            layout.defaultCellSetting().alignVerticallyMiddle();
            ItemDisplayWidget itemWidget = new ItemDisplayWidget(
               Minecraft.getInstance(), 0, 0, item.width(), item.height(), CommonComponents.EMPTY, displayStack, item.showDecorations(), item.showTooltip()
            );
            layout.addChild(itemWidget);
            layout.addChild(
               FocusableTextWidget.builder(description.contents(), parent.getFont())
                  .maxWidth(description.width())
                  .alwaysShowBorder(false)
                  .backgroundFill(FocusableTextWidget.BackgroundFill.NEVER)
                  .build()
                  .setComponentClickHandler(style -> DialogBodyHandlers.runActionOnParent(parent, style))
            );
            return layout;
         } else {
            return new ItemDisplayWidget(
               Minecraft.getInstance(),
               0,
               0,
               item.width(),
               item.height(),
               displayStack.getHoverName(),
               displayStack,
               item.showDecorations(),
               item.showTooltip()
            );
         }
      }
   }

   private static class PlainMessageHandler implements DialogBodyHandler<PlainMessage> {
      public LayoutElement createControls(final DialogScreen<?> parent, final PlainMessage message) {
         return FocusableTextWidget.builder(message.contents(), parent.getFont())
            .maxWidth(message.width())
            .alwaysShowBorder(false)
            .backgroundFill(FocusableTextWidget.BackgroundFill.NEVER)
            .build()
            .setCentered(true)
            .setComponentClickHandler(style -> DialogBodyHandlers.runActionOnParent(parent, style));
      }
   }
}
