package com.mojang.blaze3d;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GraphicsWorkarounds {
   private static final List<String> INTEL_GEN11_CORE = List.of(
      "i3-1000g1",
      "i3-1000g4",
      "i3-1000ng4",
      "i3-1005g1",
      "i3-l13g4",
      "i5-1030g4",
      "i5-1030g7",
      "i5-1030ng7",
      "i5-1034g1",
      "i5-1035g1",
      "i5-1035g4",
      "i5-1035g7",
      "i5-1038ng7",
      "i5-l16g7",
      "i7-1060g7",
      "i7-1060ng7",
      "i7-1065g7",
      "i7-1068g7",
      "i7-1068ng7"
   );
   private static final List<String> INTEL_GEN11_ATOM = List.of("x6211e", "x6212re", "x6214re", "x6413e", "x6414re", "x6416re", "x6425e", "x6425re", "x6427fe");
   private static final List<String> INTEL_GEN11_CELERON = List.of("j6412", "j6413", "n4500", "n4505", "n5095", "n5095a", "n5100", "n5105", "n6210", "n6211");
   private static final List<String> INTEL_GEN11_PENTIUM = List.of("6805", "j6426", "n6415", "n6000", "n6005");
   private static @Nullable GraphicsWorkarounds instance;
   private final WeakReference<GpuDevice> gpuDevice;
   private final boolean alwaysCreateFreshImmediateBuffer;
   private final boolean isGlOnDx12;
   private final boolean isAmd;

   private GraphicsWorkarounds(GpuDevice p_410827_) {
      this.gpuDevice = new WeakReference<>(p_410827_);
      this.alwaysCreateFreshImmediateBuffer = isIntelGen11(p_410827_);
      this.isGlOnDx12 = isGlOnDx12(p_410827_);
      this.isAmd = isAmd(p_410827_);
   }

   public static GraphicsWorkarounds get(GpuDevice p_410829_) {
      GraphicsWorkarounds graphicsworkarounds = instance;
      if (graphicsworkarounds == null || graphicsworkarounds.gpuDevice.get() != p_410829_) {
         instance = graphicsworkarounds = new GraphicsWorkarounds(p_410829_);
      }

      return graphicsworkarounds;
   }

   public boolean alwaysCreateFreshImmediateBuffer() {
      return this.alwaysCreateFreshImmediateBuffer;
   }

   public boolean isGlOnDx12() {
      return this.isGlOnDx12;
   }

   public boolean isAmd() {
      return this.isAmd;
   }

   private static boolean isIntelGen11(GpuDevice p_410828_) {
      String s = GLX._getCpuInfo().toLowerCase(Locale.ROOT);
      String s1 = p_410828_.getRenderer().toLowerCase(Locale.ROOT);
      if (!s.contains("intel") || !s1.contains("intel") || s1.contains("mesa")) {
         return false;
      } else if (s1.endsWith("gen11")) {
         return true;
      } else {
         return !s1.contains("uhd graphics") && !s1.contains("iris")
            ? false
            : s.contains("atom") && INTEL_GEN11_ATOM.stream().anyMatch(s::contains)
               || s.contains("celeron") && INTEL_GEN11_CELERON.stream().anyMatch(s::contains)
               || s.contains("pentium") && INTEL_GEN11_PENTIUM.stream().anyMatch(s::contains)
               || INTEL_GEN11_CORE.stream().anyMatch(s::contains);
      }
   }

   private static boolean isGlOnDx12(GpuDevice p_422971_) {
      boolean flag = Util.getPlatform() == Util.OS.WINDOWS && Util.isAarch64();
      return flag || p_422971_.getRenderer().startsWith("D3D12");
   }

   private static boolean isAmd(GpuDevice p_451834_) {
      return p_451834_.getRenderer().contains("AMD");
   }
}
