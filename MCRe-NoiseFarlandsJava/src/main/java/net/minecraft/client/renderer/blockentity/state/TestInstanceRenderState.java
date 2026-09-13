package net.minecraft.client.renderer.blockentity.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestInstanceRenderState extends BlockEntityRenderState {
    public BeaconRenderState beaconRenderState;
    public BlockEntityWithBoundingBoxRenderState blockEntityWithBoundingBoxRenderState;
    public final List<TestInstanceBlockEntity.ErrorMarker> errorMarkers = new ArrayList<>();
}