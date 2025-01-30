package org.friselis.cobblestars.Datas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.friselis.cobblestars.CobbleStars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class PlayerData {
    public static HashMap<UUID, PlayerData> data = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final File file = FabricLoader.getInstance().getConfigDir().resolve("CobbleStarsPlayerData.json").toFile();

    public static PlayerData get(UUID id) {
        return data.putIfAbsent(id, new PlayerData());
    }

    public static PlayerData get(ServerPlayerEntity player) {
        return get(player.getUuid());
    }

    public static void Save() {
        try {
            String json = mapper.writeValueAsString(data);

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
                StringBuilder builder = new StringBuilder();

                while (myReader.hasNextLine()) {
                    builder.append(myReader.nextLine());
                }

                myReader.close();
                data = mapper.readValue(builder.toString(), new TypeReference<HashMap<UUID, PlayerData>>() {
                });
            } catch (FileNotFoundException var5) {
                CobbleStars.LOGGER.error("An error occurred.", var5);
            }
        } catch (IOException var6) {
            CobbleStars.LOGGER.error("An error occurred.", var6);
        }

    }

    public String lang = "en";
    public String username = "";
}
