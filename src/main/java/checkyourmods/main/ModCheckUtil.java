package checkyourmods.main;

import net.neoforged.fml.ModList;
import java.util.HashMap;
import java.util.Map;

public class ModCheckUtil {

    public static Map<String, ModListPayload.ModData> generateCurrentModList() {
        Map<String, ModListPayload.ModData> mods = new HashMap<>();

        ModList.get().getMods().forEach(modInfo -> {
            String modId = modInfo.getModId();
            String version = modInfo.getVersion().toString();

            // Filtert Minecraft, NeoForge und alle leeren oder "unknown" Einträge heraus
            if (modId != null && !modId.isBlank()
                    && !modId.equals("minecraft")
                    && !modId.equals("neoforge")
                    && !modId.equalsIgnoreCase("unknown")) {

                mods.put(modId, new ModListPayload.ModData(modId, version));
            }
        });

        return mods;
    }
}