package net.minecraft.server.dialog.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.functions.StringTemplate;

public class ParsedTemplate {
   public static final Codec<ParsedTemplate> CODEC = Codec.STRING.comapFlatMap(ParsedTemplate::parse, p_407073_ -> p_407073_.raw);
   public static final Codec<String> VARIABLE_CODEC = Codec.STRING
      .validate(
         p_406209_ -> StringTemplate.isValidVariableName(p_406209_)
            ? DataResult.success(p_406209_)
            : DataResult.error(() -> p_406209_ + " is not a valid input name")
      );
   private final String raw;
   private final StringTemplate parsed;

   private ParsedTemplate(String p_406318_, StringTemplate p_410691_) {
      this.raw = p_406318_;
      this.parsed = p_410691_;
   }

   private static DataResult<ParsedTemplate> parse(String p_406260_) {
      StringTemplate stringtemplate;
      try {
         stringtemplate = StringTemplate.fromString(p_406260_);
      } catch (Exception exception) {
         return DataResult.error(() -> "Failed to parse template " + p_406260_ + ": " + exception.getMessage());
      }

      return DataResult.success(new ParsedTemplate(p_406260_, stringtemplate));
   }

   public String instantiate(Map<String, String> p_406879_) {
      List<String> list = this.parsed.variables().stream().map(p_407488_ -> p_406879_.getOrDefault(p_407488_, "")).toList();
      return this.parsed.substitute(list);
   }
}
