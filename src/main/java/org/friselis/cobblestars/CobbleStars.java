package org.friselis.cobblestars;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.Text;
import org.friselis.cobblestars.Commands.Auction;
import org.friselis.cobblestars.Commands.Translate;
import org.friselis.cobblestars.Datas.Config;
import org.friselis.cobblestars.Datas.PlayerData;
import org.friselis.cobblestars.Datas.SafariAuctionData;
import org.friselis.cobblestars.Events.ALLOW_CHAT_MESSAGE;
import org.friselis.cobblestars.Events.DISCONNECT;
import org.friselis.cobblestars.Events.JOIN;
import org.friselis.cobblestars.SafariAuction.SafariAuction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleStars implements ModInitializer {
   public static final String MOD_ID = "cobblestars";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   public static CobbleStars instance;
   public MinecraftServer server;
   public LuckPerms luckPerms;

   public void onInitialize() {
      instance = this;
      ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(ALLOW_CHAT_MESSAGE::RUN);
      ServerPlayConnectionEvents.JOIN.register(JOIN::RUN);
      ServerPlayConnectionEvents.DISCONNECT.register(DISCONNECT::RUN);
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
         Translate.register(dispatcher);
         Auction.register(dispatcher);
      });
      ServerLifecycleEvents.SERVER_STARTED.register((s) -> {
         this.server = s;
         this.luckPerms = LuckPermsProvider.get();
         WebSocket.Start();
         SafariAuction.Start();
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((s) -> {
         WebSocket.instance.stopping = true;
         WebSocket.instance.close();
      });
      ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

      try {
         Translator.init();
      } catch (Exception var2) {
      }
      Config.Load();
      PlayerData.Load();
   }

   private void onServerTick(MinecraftServer server) {
      if (SafariAuction.instance != null) {
         SafariAuction.instance.tick();
      }
   }


   public void sendGlobalMessage(String message) {
      this.server.getPlayerManager().getPlayerList().forEach((player) -> {
         player.sendMessage(Text.of(message));
      });
   }

   public void sendGlobalTranslatedMessage(String name, String message) {
      Map<String, String> translatedLangs = new HashMap();
      Iterator var4 = this.server.getPlayerManager().getPlayerList().iterator();

      ServerPlayerEntity player;
      PlayerData pdata;
      String plang;
      while(var4.hasNext()) {
         player = (ServerPlayerEntity)var4.next();
         pdata = PlayerData.get(player);
         plang = pdata.lang;
         if (!translatedLangs.containsKey(plang)) {
            try {
               translatedLangs.put(plang, Translator.performTranslate(message, GoogleTranslateLanguage.AUTO_DETECT, Translator.knownLanguages.get(plang)));
            } catch (Exception var9) {
            }
         }
      }

      var4 = this.server.getPlayerManager().getPlayerList().iterator();

      while(var4.hasNext()) {
         player = (ServerPlayerEntity)var4.next();
         pdata = PlayerData.get(player);
         plang = pdata.lang;
         String formattedMessage = "[D]" + name + ": " + translatedLangs.get(plang);
         player.sendMessage(Text.literal(formattedMessage).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal("From Discord\nOriginal: " + message)))));
      }

   }
}
