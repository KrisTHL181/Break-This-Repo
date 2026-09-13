package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinCustomizationScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.skinCustomisation.title");

   public SkinCustomizationScreen(Screen p_343566_, Options p_344917_) {
      super(p_343566_, p_344917_, TITLE);
   }

   @Override
   protected void addOptions() {
      List<AbstractWidget> list = new ArrayList<>();

      for (PlayerModelPart playermodelpart : PlayerModelPart.values()) {
         list.add(
            CycleButton.onOffBuilder(this.options.isModelPartEnabled(playermodelpart))
               .create(playermodelpart.getName(), (p_357682_, p_357683_) -> this.options.setModelPart(playermodelpart, p_357683_))
         );
      }

      list.add(this.options.mainHand().createButton(this.options));
      this.list.addSmall(list);
   }
}
