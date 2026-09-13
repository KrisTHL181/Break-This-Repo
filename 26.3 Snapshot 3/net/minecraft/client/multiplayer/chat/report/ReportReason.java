package net.minecraft.client.multiplayer.chat.report;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum ReportReason {
   I_WANT_TO_REPORT_THEM("i_want_to_report_them"),
   HATE_SPEECH("hate_speech"),
   HARASSMENT_OR_BULLYING("harassment_or_bullying"),
   SELF_HARM_OR_SUICIDE("self_harm_or_suicide"),
   IMMINENT_HARM("imminent_harm"),
   DEFAMATION_IMPERSONATION_FALSE_INFORMATION("defamation_impersonation_false_information"),
   ALCOHOL_TOBACCO_DRUGS("alcohol_tobacco_drugs"),
   CHILD_SEXUAL_EXPLOITATION_OR_ABUSE("child_sexual_exploitation_or_abuse"),
   TERRORISM_OR_VIOLENT_EXTREMISM("terrorism_or_violent_extremism"),
   NON_CONSENSUAL_INTIMATE_IMAGERY("non_consensual_intimate_imagery"),
   SEXUALLY_INAPPROPRIATE("sexually_inappropriate");

   private final String backendName;
   private final Component title;
   private final Component description;

   ReportReason(final String name) {
      this.backendName = name.toUpperCase(Locale.ROOT);
      String translationKey = "gui.abuseReport.reason." + name;
      this.title = Component.translatable(translationKey);
      this.description = Component.translatable(translationKey + ".description");
   }

   public String backendName() {
      return this.backendName;
   }

   public Component title() {
      return this.title;
   }

   public Component description() {
      return this.description;
   }

   public static List<ReportReason> getIncompatibleCategories(final ReportType reportType) {
      return switch (reportType) {
         case CHAT -> List.of(SEXUALLY_INAPPROPRIATE);
         case SKIN -> List.of(IMMINENT_HARM, DEFAMATION_IMPERSONATION_FALSE_INFORMATION);
         default -> List.of();
      };
   }
}
