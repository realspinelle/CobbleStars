package org.friselis.cobblestars.Events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.luckperms.api.model.user.User;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent.Action;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.GoogleTranslateLanguage;
import org.friselis.cobblestars.PlayerData;
import org.friselis.cobblestars.Translator;
import org.friselis.cobblestars.WebSocket;

public class ALLOW_CHAT_MESSAGE {
   public static boolean RUN(SignedMessage message, ServerPlayerEntity sender, Parameters params) {
      String originalMessage = message.getSignedContent();
      if (Permissions.check(sender, "discord.bridge.command.start_now", 4)) {
         originalMessage = originalMessage.replaceAll("&", "§");
      }

      String prefix = ((User)Objects.requireNonNull(CobbleStars.instance.luckPerms.getUserManager().getUser(sender.getUuid()))).getCachedData().getMetaData().getPrefix();
      String originalSenderName = sender.getDisplayName().getString();
      if (prefix != null) {
         originalSenderName = prefix + originalSenderName + "&r";
         originalSenderName = originalSenderName.replaceAll("&", "§");
      }

      String originalFullMessage = originalSenderName + ": " + originalMessage;
      sender.sendMessage(Text.literal(originalFullMessage));
      Map<String, String> translatedLangs = new HashMap();
      PlayerData sdata = (PlayerData)CobbleStars.instance.playersData.computeIfAbsent(sender.getUuid(), (k) -> {
         return new PlayerData();
      });
      String slang = sdata.lang;
      Iterator var10 = CobbleStars.instance.server.getPlayerManager().getPlayerList().iterator();

      ServerPlayerEntity player;
      PlayerData pdata;
      String plang;
      while(var10.hasNext()) {
         player = (ServerPlayerEntity)var10.next();
         if (player != sender) {
            pdata = (PlayerData)CobbleStars.instance.playersData.computeIfAbsent(player.getUuid(), (k) -> {
               return new PlayerData();
            });
            plang = pdata.lang;
            if (!translatedLangs.containsKey(plang)) {
               try {
                  translatedLangs.put(plang, Translator.performTranslate(originalMessage, (GoogleTranslateLanguage)Translator.knownLanguages.get(slang), (GoogleTranslateLanguage)Translator.knownLanguages.get(plang)));
               } catch (Exception var16) {
               }
            }
         }
      }

      if (!slang.equals("en")) {
         if (!translatedLangs.containsKey("en")) {
            try {
               translatedLangs.put("en", Translator.performTranslate(originalMessage, (GoogleTranslateLanguage)Translator.knownLanguages.get(slang), (GoogleTranslateLanguage)Translator.knownLanguages.get("en")));
            } catch (Exception var15) {
               throw new RuntimeException(var15);
            }
         }

         WebSocket.sendChatMessage(sender.getDisplayName().getString(), (String)translatedLangs.get("en"));
      } else {
         WebSocket.sendChatMessage(sender.getDisplayName().getString(), originalMessage);
      }

      var10 = CobbleStars.instance.server.getPlayerManager().getPlayerList().iterator();

      while(var10.hasNext()) {
         player = (ServerPlayerEntity)var10.next();
         if (player != sender) {
            pdata = (PlayerData)CobbleStars.instance.playersData.computeIfAbsent(player.getUuid(), (k) -> {
               return new PlayerData();
            });
            plang = pdata.lang;
            if (slang.equals(plang)) {
               player.sendMessage(Text.literal(originalFullMessage));
            } else {
               String formattedMessage = "[T]" + originalSenderName + ": " + (String)translatedLangs.get(plang);
               player.sendMessage(Text.literal(formattedMessage).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal("Original: " + originalMessage)))));
            }
         }
      }

      return false;
   }
}
