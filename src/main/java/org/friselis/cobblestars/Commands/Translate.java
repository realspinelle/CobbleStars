package org.friselis.cobblestars.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Iterator;
import java.util.Map.Entry;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.GoogleTranslateLanguage;
import org.friselis.cobblestars.PlayerData;
import org.friselis.cobblestars.Translator;

public class Translate {
   private static final SuggestionProvider<ServerCommandSource> LANGUAGE_SUGGESTIONS = (context, builder) -> {
      Iterator var2 = Translator.knownLanguages.keySet().iterator();

      while(var2.hasNext()) {
         String key = (String)var2.next();
         builder.suggest(key);
      }

      return builder.buildFuture();
   };

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("translate").then(CommandManager.literal("set").then(CommandManager.argument("lang", StringArgumentType.string()).suggests(LANGUAGE_SUGGESTIONS).executes(Translate::executeSetLang)))).then(CommandManager.literal("get").then(CommandManager.argument("player", EntityArgumentType.player()).executes(Translate::executeGetLang)))).then(CommandManager.literal("list").executes(Translate::executeListLangs)));
   }

   private static int executeSetLang(CommandContext<ServerCommandSource> context) {
      ServerCommandSource source = (ServerCommandSource)context.getSource();
      String lang = StringArgumentType.getString(context, "lang");
      if (Translator.knownLanguages.isEmpty()) {
         source.sendFeedback(() -> {
            return Text.literal("ERROR TRANSLATION MOD BROKEN PING FRISELIS IMMEDIATELY");
         }, false);
         return 0;
      } else if (!Translator.knownLanguages.containsKey(lang)) {
         source.sendFeedback(() -> {
            return Text.literal("This language is not in the list");
         }, false);
         return 0;
      } else {
         ServerPlayerEntity player = source.getPlayer();

         assert player != null;

         PlayerData pdata = (PlayerData)CobbleStars.instance.playersData.get(player.getUuid());
         if (pdata == null) {
            pdata = new PlayerData();
            pdata.lang = lang;
            CobbleStars.instance.playersData.put(player.getUuid(), pdata);
         } else {
            pdata.lang = lang;
         }

         CobbleStars.instance.SavePlayerData();
         source.sendFeedback(() -> {
            return Text.literal("You selected " + ((GoogleTranslateLanguage)Translator.knownLanguages.get(lang)).getDisplayName());
         }, false);
         return 1;
      }
   }

   private static int executeGetLang(CommandContext<ServerCommandSource> context) {
      ServerCommandSource source = (ServerCommandSource)context.getSource();
      if (Translator.knownLanguages.isEmpty()) {
         source.sendFeedback(() -> {
            return Text.literal("ERROR TRANSLATION MOD BROKEN PING FRISELIS IMMEDIATELY");
         }, false);
         return 0;
      } else {
         ServerPlayerEntity player;
         try {
            player = EntityArgumentType.getPlayer(context, "player");
         } catch (CommandSyntaxException var4) {
            source.sendFeedback(() -> {
               return Text.literal("Could not find the specified player");
            }, false);
            return 0;
         }

         PlayerData pdata = (PlayerData)CobbleStars.instance.playersData.computeIfAbsent(player.getUuid(), (k) -> {
            return new PlayerData();
         });
         source.sendFeedback(() -> {
            return Text.literal(((GoogleTranslateLanguage)Translator.knownLanguages.get(pdata.lang)).getDisplayName());
         }, false);
         return 1;
      }
   }

   private static int executeListLangs(CommandContext<ServerCommandSource> context) {
      ServerCommandSource source = (ServerCommandSource)context.getSource();
      if (Translator.knownLanguages.isEmpty()) {
         source.sendFeedback(() -> {
            return Text.literal("ERROR TRANSLATION MOD BROKEN PING FRISELIS IMMEDIATELY");
         }, false);
         return 0;
      } else {
         StringBuilder text = new StringBuilder();
         Iterator var3 = Translator.knownLanguages.entrySet().iterator();

         while(var3.hasNext()) {
            Entry<String, GoogleTranslateLanguage> entry = (Entry)var3.next();
            String key = (String)entry.getKey();
            GoogleTranslateLanguage value = (GoogleTranslateLanguage)entry.getValue();
            text.append(key).append(" - ").append(value.getDisplayName()).append("\n");
         }

         source.sendFeedback(() -> {
            return Text.literal(text.toString().trim());
         }, false);
         return 1;
      }
   }
}
