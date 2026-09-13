package net.minecraft.client;

import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.dialog.DialogScreens;
import net.minecraft.client.gui.screens.dialog.body.DialogBodyHandlers;
import net.minecraft.client.gui.screens.dialog.input.InputControlHandlers;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientBootstrap {
    private static volatile boolean isBootstrapped;

    public static void bootstrap() {
        if (!isBootstrapped) {
            isBootstrapped = true;
            ItemModels.bootstrap();
            SpecialModelRenderers.bootstrap();
            ItemTintSources.bootstrap();
            SelectItemModelProperties.bootstrap();
            ConditionalItemModelProperties.bootstrap();
            RangeSelectItemModelProperties.bootstrap();
            SpriteSources.bootstrap();
            DialogScreens.bootstrap();
            InputControlHandlers.bootstrap();
            DialogBodyHandlers.bootstrap();
        }
    }
}