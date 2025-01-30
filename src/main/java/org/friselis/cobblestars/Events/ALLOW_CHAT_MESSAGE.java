package org.friselis.cobblestars.Events;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent.Action;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.Datas.PlayerData;
import org.friselis.cobblestars.Translator;
import org.friselis.cobblestars.WebSocket;

public class ALLOW_CHAT_MESSAGE {
   public static boolean RUN(SignedMessage message, ServerPlayerEntity sender, Parameters params) {
      String originalMessage = message.getSignedContent();
      if (Permissions.check(sender, "discord.bridge.command.start_now", 4)) {
         originalMessage = originalMessage.replaceAll("&", "§");
      }

      String prefix = (Objects.requireNonNull(CobbleStars.instance.luckPerms.getUserManager().getUser(sender.getUuid()))).getCachedData().getMetaData().getPrefix();
      String originalSenderName = sender.getDisplayName().getString();
      if (prefix != null) {
         originalSenderName = prefix + originalSenderName + "&r";
         originalSenderName = originalSenderName.replaceAll("&", "§");
      }

      String originalFullMessage = originalSenderName + ": " + originalMessage;
      sender.sendMessage(Text.literal(originalFullMessage));
      Map<String, String> translatedLangs = new HashMap<>();
      PlayerData sdata = PlayerData.get(sender);
      String slang = sdata.lang;

      for (var player : CobbleStars.instance.server.getPlayerManager().getPlayerList()){
         if (player != sender) {
            var pdata = PlayerData.get(player);
            if (!translatedLangs.containsKey(pdata.lang)) {
               try {
                  translatedLangs.put(pdata.lang, Translator.performTranslate(originalMessage, Translator.knownLanguages.get(slang), Translator.knownLanguages.get(pdata.lang)));
               } catch (Exception var16) {
               }
            }
         }
      }

      if (!slang.equals("en")) {
         if (!translatedLangs.containsKey("en")) {
            try {
               translatedLangs.put("en", Translator.performTranslate(originalMessage, Translator.knownLanguages.get(slang), Translator.knownLanguages.get("en")));
            } catch (Exception var15) {
               throw new RuntimeException(var15);
            }
         }

         WebSocket.sendChatMessage(sender.getDisplayName().getString(), translatedLangs.get("en"));
      } else {
         WebSocket.sendChatMessage(sender.getDisplayName().getString(), originalMessage);
      }

      for (var player : CobbleStars.instance.server.getPlayerManager().getPlayerList()){
         if (player != sender) {
            var pdata = PlayerData.get(player);
            if (slang.equals(pdata.lang)) {
               player.sendMessage(Text.literal(originalFullMessage));
            } else {
               String formattedMessage = "[T]" + originalSenderName + ": " + translatedLangs.get(pdata.lang);
               player.sendMessage(Text.literal(formattedMessage).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal("Original: " + originalMessage)))));
            }
         }
      }

      return false;
   }
}
