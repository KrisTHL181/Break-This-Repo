package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.color.ColorLerper;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepRenderState extends LivingEntityRenderState {
    public float headEatPositionScale;
    public float headEatAngleScale;
    public boolean isSheared;
    public DyeColor woolColor = DyeColor.WHITE;
    public boolean isJebSheep;

    public int getWoolColor() {
        return this.isJebSheep ? ColorLerper.getLerpedColor(ColorLerper.Type.SHEEP, this.ageInTicks) : ColorLerper.Type.SHEEP.getColor(this.woolColor);
    }
}