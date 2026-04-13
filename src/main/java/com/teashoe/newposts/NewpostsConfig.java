package com.teashoe.newposts;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class NewpostsConfig {

    private static Configuration config;

    public static String galleryId = "steve";
    public static boolean useSystemChat = false;
    public static boolean showIpAddress = true;
    public static boolean showUid = true;
    public static int geuldethap = 100;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        load();
    }

    public static void load() {
        config.load();
        galleryId    = config.get("general", "galleryId", "steve",
                "갤러리 ID (steve가 아닌 ID로 변경하면 깡계체크가 작동하지 않음)").getString();
        useSystemChat = config.get("general", "useSystemChat", false,
                "액션바에 표시 (1.7.10에서는 항상 채팅창에 표시됨)").getBoolean();
        showIpAddress = config.get("general", "showIpAddress", true,
                "유동 IP 보기").getBoolean();
        showUid       = config.get("general", "showuid", true,
                "식별코드 보기").getBoolean();
        Property geulProp = config.get("general", "geuldethap", 100,
                "깡계 글댓합 기준값");
        geulProp.setMinValue(0);
        geuldethap = geulProp.getInt();
        if (config.hasChanged()) config.save();
    }

    public static void save() {
        if (config == null) return;
        config.get("general", "galleryId", "steve").set(galleryId);
        config.get("general", "useSystemChat", false).set(useSystemChat);
        config.get("general", "showIpAddress", true).set(showIpAddress);
        config.get("general", "showuid", true).set(showUid);
        config.get("general", "geuldethap", 100).set(geuldethap);
        config.save();
    }
}
