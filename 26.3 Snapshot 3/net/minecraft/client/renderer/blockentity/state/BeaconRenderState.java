package net.minecraft.client.renderer.blockentity.state;

import java.util.ArrayList;
import java.util.List;

public class BeaconRenderState extends BlockEntityRenderState {
   public float animationTime;
   public float beamRadiusScale;
   public List<BeaconRenderState.Section> sections = new ArrayList<>();

   public record Section(int color, int height) {
   }
}
