package org.friselis.cobblestars.Datas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.fabricmc.loader.api.FabricLoader;
import org.friselis.cobblestars.CobbleStars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Config {
   private static final File file = FabricLoader.getInstance().getConfigDir().resolve("CobbleStars.json").toFile();
   private static final ObjectMapper mapper = new ObjectMapper();
   public static Config config = new Config();
   public static void Save() {
      try {
         String json = mapper.writeValueAsString(config);
         try {
            FileWriter myWriter = new FileWriter(file);
            myWriter.write(json);
            myWriter.close();
         } catch (IOException var3) {
            CobbleStars.LOGGER.error(var3.getMessage());
         }
      } catch (JsonProcessingException var4) {
         CobbleStars.LOGGER.error(var4.getMessage());
      }
   }
   public static void Load() {
      try {
         if (file.createNewFile()) {
            CobbleStars.LOGGER.info("File created: {}", file.getName());

            Save();
         }
         try {
            Scanner myReader = new Scanner(file);
            StringBuilder data = new StringBuilder();
            while(myReader.hasNextLine()) {
               data.append(myReader.nextLine());
            }
            myReader.close();
            config = mapper.readValue(data.toString(), Config.class);
         } catch (FileNotFoundException var4) {
            CobbleStars.LOGGER.error("An error occurred.", var4);
         }
      } catch (IOException var5) {
         CobbleStars.LOGGER.error("An error occurred.", var5);
      }
   }
   public String ip = "";
   public String port = "";
   public boolean local = false;
}
