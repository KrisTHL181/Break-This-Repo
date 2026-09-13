package net.minecraft.client.resources.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleSoundInstance extends AbstractSoundInstance {
   public SimpleSoundInstance(SoundEvent p_235109_, SoundSource p_235110_, float p_235111_, float p_235112_, RandomSource p_235113_, BlockPos p_235114_) {
      this(p_235109_, p_235110_, p_235111_, p_235112_, p_235113_, p_235114_.getX() + 0.5, p_235114_.getY() + 0.5, p_235114_.getZ() + 0.5);
   }

   public static SimpleSoundInstance forUI(SoundEvent p_119753_, float p_119754_) {
      return forUI(p_119753_, p_119754_, 0.25F);
   }

   public static SimpleSoundInstance forUI(Holder<SoundEvent> p_263418_, float p_263405_) {
      return forUI(p_263418_.value(), p_263405_);
   }

   public static SimpleSoundInstance forUI(SoundEvent p_119756_, float p_119757_, float p_119758_) {
      return new SimpleSoundInstance(
         p_119756_.location(),
         SoundSource.UI,
         p_119758_,
         p_119757_,
         SoundInstance.createUnseededRandom(),
         false,
         0,
         SoundInstance.Attenuation.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
   }

   public static SimpleSoundInstance forMusic(SoundEvent p_119746_) {
      return new SimpleSoundInstance(
         p_119746_.location(),
         SoundSource.MUSIC,
         1.0F,
         1.0F,
         SoundInstance.createUnseededRandom(),
         false,
         0,
         SoundInstance.Attenuation.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
   }

   public static SimpleSoundInstance forJukeboxSong(SoundEvent p_249575_, Vec3 p_249600_) {
      return new SimpleSoundInstance(
         p_249575_,
         SoundSource.RECORDS,
         4.0F,
         1.0F,
         SoundInstance.createUnseededRandom(),
         false,
         0,
         SoundInstance.Attenuation.LINEAR,
         p_249600_.x,
         p_249600_.y,
         p_249600_.z
      );
   }

   public static SimpleSoundInstance forLocalAmbience(SoundEvent p_119767_, float p_119768_, float p_119769_) {
      return new SimpleSoundInstance(
         p_119767_.location(),
         SoundSource.AMBIENT,
         p_119769_,
         p_119768_,
         SoundInstance.createUnseededRandom(),
         false,
         0,
         SoundInstance.Attenuation.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
   }

   public static SimpleSoundInstance forAmbientAddition(SoundEvent p_119760_) {
      return forLocalAmbience(p_119760_, 1.0F, 1.0F);
   }

   public static SimpleSoundInstance forAmbientMood(SoundEvent p_235128_, RandomSource p_235129_, double p_235130_, double p_235131_, double p_235132_) {
      return new SimpleSoundInstance(
         p_235128_, SoundSource.AMBIENT, 1.0F, 1.0F, p_235129_, false, 0, SoundInstance.Attenuation.LINEAR, p_235130_, p_235131_, p_235132_
      );
   }

   public SimpleSoundInstance(
      SoundEvent p_235100_,
      SoundSource p_235101_,
      float p_235102_,
      float p_235103_,
      RandomSource p_235104_,
      double p_235105_,
      double p_235106_,
      double p_235107_
   ) {
      this(p_235100_, p_235101_, p_235102_, p_235103_, p_235104_, false, 0, SoundInstance.Attenuation.LINEAR, p_235105_, p_235106_, p_235107_);
   }

   private SimpleSoundInstance(
      SoundEvent p_235116_,
      SoundSource p_235117_,
      float p_235118_,
      float p_235119_,
      RandomSource p_235120_,
      boolean p_235121_,
      int p_235122_,
      SoundInstance.Attenuation p_235123_,
      double p_235124_,
      double p_235125_,
      double p_235126_
   ) {
      this(p_235116_.location(), p_235117_, p_235118_, p_235119_, p_235120_, p_235121_, p_235122_, p_235123_, p_235124_, p_235125_, p_235126_, false);
   }

   public SimpleSoundInstance(
      Identifier p_453143_,
      SoundSource p_235088_,
      float p_235089_,
      float p_235090_,
      RandomSource p_235091_,
      boolean p_235092_,
      int p_235093_,
      SoundInstance.Attenuation p_235094_,
      double p_235095_,
      double p_235096_,
      double p_235097_,
      boolean p_235098_
   ) {
      super(p_453143_, p_235088_, p_235091_);
      this.volume = p_235089_;
      this.pitch = p_235090_;
      this.x = p_235095_;
      this.y = p_235096_;
      this.z = p_235097_;
      this.looping = p_235092_;
      this.delay = p_235093_;
      this.attenuation = p_235094_;
      this.relative = p_235098_;
   }
}
