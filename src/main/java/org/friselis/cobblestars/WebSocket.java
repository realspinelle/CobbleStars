package org.friselis.cobblestars;

import com.mojang.authlib.GameProfile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.util.UserCache;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

public class WebSocket extends WebSocketClient {
   private static long RECONNECT_DELAY = 5L;
   public ScheduledExecutorService executorService;
   public boolean reconnecting = false;
   public boolean stopping = false;
   public static WebSocket instance;

   public WebSocket(URI serverUri) {
      super(serverUri);
   }

   public void onOpen(ServerHandshake handshakedata) {
      System.out.println("Connected to server");
      RECONNECT_DELAY = 5L;
      if (this.executorService != null && !this.executorService.isShutdown()) {
         this.executorService.shutdown();
      }

   }

   public void onMessage(String message) {
      try {
         JSONObject jsonObject = new JSONObject(message);
         String type = jsonObject.getString("type");
         Whitelist whitelist = CobbleStars.instance.server.getPlayerManager().getWhitelist();
         Optional p;
         if (Objects.equals(type, "whitelist")) {
            p = ((UserCache)Objects.requireNonNull(CobbleStars.instance.server.getUserCache())).findByName(jsonObject.getString("name"));
            if (p.isPresent()) {
               WhitelistEntry e = new WhitelistEntry((GameProfile)p.get());
               whitelist.add(e);
               CobbleStars.instance.sendGlobalMessage("§a" + jsonObject.getString("name") + " §rhas been whitelisted");
            }
         }

         if (Objects.equals(type, "unwhitelist")) {
            p = ((UserCache)Objects.requireNonNull(CobbleStars.instance.server.getUserCache())).findByName(jsonObject.getString("name"));
            if (p.isPresent()) {
               whitelist.remove((GameProfile)p.get());
               CobbleStars.instance.sendGlobalMessage("§a" + jsonObject.getString("name") + " §rhas been unwhitelisted");
            }
         }

         if (Objects.equals(type, "chat")) {
            CobbleStars.instance.sendGlobalTranslatedMessage(jsonObject.getString("name"), jsonObject.getString("text"));
         }
      } catch (Exception var7) {
         System.err.println("Failed to parse message as JSON: " + var7.getMessage());
      }

   }

   public void onClose(int code, String reason, boolean remote) {
      System.out.println("Disconnected from server. Reason: " + reason);
      this.attemptReconnect();
   }

   public void onError(Exception ex) {
      this.attemptReconnect();
   }

   private void attemptReconnect() {
      if (this.stopping) {
         this.executorService.shutdown();
         this.close();
      } else if (!this.reconnecting) {
         this.reconnecting = true;
         if (this.executorService == null || this.executorService.isShutdown()) {
            this.executorService = Executors.newSingleThreadScheduledExecutor();
         }

         this.executorService.schedule(() -> {
            System.out.println("Attempting to reconnect...");

            try {
               this.reconnect();
               RECONNECT_DELAY *= 2L;
               this.reconnecting = false;
            } catch (Exception var2) {
            }

         }, RECONNECT_DELAY, TimeUnit.SECONDS);
      }
   }

   public static void sendJoiningMessage(String name) {
      if (instance != null && !instance.isClosed()) {
         JSONObject json = new JSONObject();
         json.put("type", (Object)"joining");
         json.put("name", (Object)name);
         instance.send(json.toString());
      }
   }

   public static void sendLeavingMessage(String name) {
      if (instance != null && !instance.isClosed()) {
         JSONObject json = new JSONObject();
         json.put("type", (Object)"leaving");
         json.put("name", (Object)name);
         instance.send(json.toString());
      }
   }

   public static void sendNameChange(String oldName, String newName) {
      if (instance != null && !instance.isClosed()) {
         JSONObject json = new JSONObject();
         json.put("type", (Object)"nameChange");
         json.put("oldName", (Object)oldName);
         json.put("newName", (Object)newName);
         instance.send(json.toString());
      }
   }

   public static void sendChatMessage(String name, String text) {
      if (instance != null && !instance.isClosed()) {
         JSONObject json = new JSONObject();
         json.put("type", (Object)"chat");
         json.put("name", (Object)name);
         json.put("text", (Object)text);
         instance.send(json.toString());
      }
   }

   public static void sendNewAuctionMessage(String pokemonName, String price) {
      if (instance != null && !instance.isClosed()) {
         JSONObject json = new JSONObject();
         json.put("type", (Object)"auction");
         json.put("name", (Object)pokemonName);
         json.put("price", (Object)price);
         instance.send(json.toString());
      }
   }

   public void send(String text) {
      super.send(text);
   }

   public static void Start() {
      if (!CobbleStars.instance.config.local) {
         try {
            URI serverUri = new URI("ws://" + CobbleStars.instance.config.ip + ":" + CobbleStars.instance.config.port);
            WebSocket client = new WebSocket(serverUri);
            instance = client;
            client.connect();
         } catch (URISyntaxException var2) {
            var2.printStackTrace();
         }

      }
   }
}
