package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FramePass {
   <T> ResourceHandle<T> createsInternal(String var1, ResourceDescriptor<T> var2);

   <T> void reads(ResourceHandle<T> var1);

   <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> var1);

   void requires(FramePass var1);

   void disableCulling();

   void executes(Runnable var1);
}
