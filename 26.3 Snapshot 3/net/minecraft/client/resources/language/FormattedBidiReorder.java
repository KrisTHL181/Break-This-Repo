package net.minecraft.client.resources.language;

import com.google.common.collect.Lists;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import java.util.List;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.util.FormattedCharSequence;

public class FormattedBidiReorder {
   public static FormattedCharSequence reorder(final FormattedText text, final boolean defaultRightToLeft) {
      SubStringSource source = SubStringSource.create(text, UCharacter::getMirror, FormattedBidiReorder::shape);
      Bidi bidi = new Bidi(source.getPlainText(), defaultRightToLeft ? 127 : 126);
      bidi.setReorderingMode(0);
      List<FormattedCharSequence> result = Lists.newArrayList();
      int runCount = bidi.countRuns();

      for (int i = 0; i < runCount; i++) {
         BidiRun run = bidi.getVisualRun(i);
         result.addAll(source.substring(run.getStart(), run.getLength(), run.isOddRun()));
      }

      return FormattedCharSequence.composite(result);
   }

   private static String shape(final String text) {
      try {
         return new ArabicShaping(8).shape(text);
      } catch (Exception e) {
         return text;
      }
   }
}
