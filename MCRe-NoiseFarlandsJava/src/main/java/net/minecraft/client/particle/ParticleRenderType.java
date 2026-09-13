package net.minecraft.client.particle;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ParticleRenderType(String name, String shorthand) {
    public static final ParticleRenderType SINGLE_QUADS = new ParticleRenderType("SINGLE_QUADS", "SQ");
    public static final ParticleRenderType ITEM_PICKUP = new ParticleRenderType("ITEM_PICKUP", "IP");
    public static final ParticleRenderType ELDER_GUARDIANS = new ParticleRenderType("ELDER_GUARDIANS", "EG");
    public static final ParticleRenderType NO_RENDER = new ParticleRenderType("NO_RENDER", "NR");
}