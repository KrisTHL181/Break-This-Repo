package net.minecraft.client.renderer.blockentity.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeaconRenderState extends BlockEntityRenderState {
    public float animationTime;
    public float beamRadiusScale;
    public List<BeaconRenderState.Section> sections = new ArrayList<>();

    @OnlyIn(Dist.CLIENT)
    public record Section(int color, int height) {
    }
}