package com.teashoe.newposts;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "newposts")
public class ModConfig implements ConfigData {
    public String galleryId = "steve"; // 기본값
    public boolean useSystemChat = false;
    public boolean showIpAddress = true;
    public boolean showuid = true;

    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean checkgallog = true;

    public int geuldethap = 100; // 깡계 기준


    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
