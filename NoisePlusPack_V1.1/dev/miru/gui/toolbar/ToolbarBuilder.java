package dev.miru.gui.toolbar;

import dev.miru.gui.screens.test.ExceptionThrowTestScreen;
import dev.miru.gui.screens.utils.PositionLocateScreen;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public class ToolbarBuilder {
   private Screen parent;
   private Minecraft mc;
   public Button singlePlayerJumpButton;
   public Button multiPlauerJumpButton;
   public Button optionsJumpButton;
   public Button createWorldJumpButton;
   public Button fastQuitButton;
   public Button toolBarExpandControlButton;
   public Button throwTestButton;
   public Button positionLocatorButton;
   public List<Button> toolBarButtonList = new ArrayList<>();

   public ToolbarBuilder(Screen parent, Minecraft mc) {
      this.parent = parent;
      this.mc = mc;
   }

   public void buildToolbar() {
      this.clearToolBar();
      if (ModMain.getOptions().getExpd_EnableToolBarValue()) {
         int h = this.parent.height - 20;
         this.singlePlayerJumpButton = KitUtil.button(
            Component.translatable("menu.singleplayer"),
            Component.literal(ModMain.getI18N("toolbar.open.singleplayer.title")),
            btn -> this.mc.setScreen(new SelectWorldScreen(this.parent)),
            80,
            20,
            20,
            h
         );
         this.multiPlauerJumpButton = KitUtil.button(
            Component.translatable("menu.multiplayer"),
            Component.literal(ModMain.getI18N("toolbar.open.multiplayer.title")),
            btn -> this.mc.setScreen(new JoinMultiplayerScreen(this.parent)),
            80,
            20,
            100,
            h
         );
         this.optionsJumpButton = KitUtil.button(
            Component.translatable("menu.options"),
            Component.literal(ModMain.getI18N("toolbar.open.options.title")),
            btn -> this.mc.setScreen(new OptionsScreen(this.parent, this.mc.options)),
            80,
            20,
            180,
            h
         );
         this.createWorldJumpButton = KitUtil.button(
            Component.translatable("selectWorld.create"),
            Component.literal(ModMain.getI18N("toolbar.create.world.title")),
            btn -> CreateWorldScreen.openFresh(this.mc, () -> this.mc.setScreen(this.parent)),
            80,
            20,
            260,
            h
         );
         this.fastQuitButton = KitUtil.button(
            Component.translatable("menu.quit"),
            Component.translatable("menu.quit"),
            btn -> this.mc
               .setScreen(
                  new ConfirmScreen(
                     selected -> {
                        if (selected) {
                           this.mc.stop();
                        } else {
                           this.mc.setScreen(this.parent);
                        }
                     },
                     Component.translatable("menu.quit").append(Component.literal("?")),
                     Component.literal(""),
                     Component.translatable("gui.yes"),
                     Component.translatable("gui.no")
                  )
               ),
            80,
            20,
            340,
            h
         );
         this.throwTestButton = KitUtil.button(
            Component.literal(ModMain.getI18N("toolbar.throw.test.title")),
            Component.literal(ModMain.getI18N("toolbar.throw.test.hint")),
            btn -> this.mc.setScreen(new ExceptionThrowTestScreen(this.parent)),
            80,
            20,
            420,
            h
         );
         this.positionLocatorButton = KitUtil.button(
            Component.literal(ModMain.getI18N("toolbar.pos.locator.title")),
            Component.literal(ModMain.getI18N("toolbar.pos.locator.hint")),
            btn -> this.mc.setScreen(new PositionLocateScreen(this.parent, ModMain.getOptions())),
            80,
            20,
            500,
            h
         );
         this.toolBarExpandControlButton = KitUtil.button(
            Component.literal(!Screen.toolBarExpanded ? "→" : "←"),
            Component.literal(!Screen.toolBarExpanded ? ModMain.getI18N("toolbar.expand.title") : ModMain.getI18N("toolbar.fold.title")),
            btn -> {
               Screen.toolBarExpanded = !Screen.toolBarExpanded;
               btn.setMessage(Component.literal(!Screen.toolBarExpanded ? "→" : "←"));
               btn.setTooltip(
                  Tooltip.create(Component.literal(!Screen.toolBarExpanded ? ModMain.getI18N("toolbar.expand.title") : ModMain.getI18N("toolbar.fold.title")))
               );
               Button[] list = new Button[]{this.singlePlayerJumpButton, this.multiPlauerJumpButton, this.optionsJumpButton, this.fastQuitButton};

               for (Button button : list) {
                  button.visible = !Screen.toolBarExpanded;
                  button.active = !Screen.toolBarExpanded;
               }
            },
            20,
            20,
            0,
            h
         );
         this.toolBarButtonList.add(this.toolBarExpandControlButton);
         this.toolBarButtonList.add(this.singlePlayerJumpButton);
         this.toolBarButtonList.add(this.multiPlauerJumpButton);
         this.toolBarButtonList.add(this.optionsJumpButton);
         this.toolBarButtonList.add(this.createWorldJumpButton);
         this.toolBarButtonList.add(this.fastQuitButton);
         this.toolBarButtonList.add(this.throwTestButton);
         this.toolBarButtonList.add(this.positionLocatorButton);
         this.toolBarButtonList.forEach(this.parent::addRenderableWidget);
      }
   }

   public void clearToolBar() {
      this.toolBarButtonList.forEach(this.parent::removeWidget);
      this.toolBarButtonList.clear();
   }
}
