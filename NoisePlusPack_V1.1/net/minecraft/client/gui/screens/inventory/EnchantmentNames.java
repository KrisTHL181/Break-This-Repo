package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantmentNames {
   private static final FontDescription ALT_FONT = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
   private static final Style ROOT_STYLE = Style.EMPTY.withFont(ALT_FONT);
   private static final EnchantmentNames INSTANCE = new EnchantmentNames();
   private final RandomSource random = RandomSource.create();
   private final String[] words = new String[]{
      "the",
      "elder",
      "scrolls",
      "klaatu",
      "berata",
      "niktu",
      "xyzzy",
      "bless",
      "curse",
      "light",
      "darkness",
      "fire",
      "air",
      "earth",
      "water",
      "hot",
      "dry",
      "cold",
      "wet",
      "ignite",
      "snuff",
      "embiggen",
      "twist",
      "shorten",
      "stretch",
      "fiddle",
      "destroy",
      "imbue",
      "galvanize",
      "enchant",
      "free",
      "limited",
      "range",
      "of",
      "towards",
      "inside",
      "sphere",
      "cube",
      "self",
      "other",
      "ball",
      "mental",
      "physical",
      "grow",
      "shrink",
      "demon",
      "elemental",
      "spirit",
      "animal",
      "creature",
      "beast",
      "humanoid",
      "undead",
      "fresh",
      "stale",
      "phnglui",
      "mglwnafh",
      "cthulhu",
      "rlyeh",
      "wgahnagl",
      "fhtagn",
      "baguette"
   };

   private EnchantmentNames() {
   }

   public static EnchantmentNames getInstance() {
      return INSTANCE;
   }

   public FormattedText getRandomName(Font p_98738_, int p_98739_) {
      StringBuilder stringbuilder = new StringBuilder();
      int i = this.random.nextInt(2) + 3;

      for (int j = 0; j < i; j++) {
         if (j != 0) {
            stringbuilder.append(" ");
         }

         stringbuilder.append(Util.getRandom(this.words, this.random));
      }

      return p_98738_.getSplitter().headByWidth(Component.literal(stringbuilder.toString()).withStyle(ROOT_STYLE), p_98739_, Style.EMPTY);
   }

   public void initSeed(long p_98736_) {
      this.random.setSeed(p_98736_);
   }
}
