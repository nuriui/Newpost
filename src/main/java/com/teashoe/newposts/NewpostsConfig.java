package com.teashoe.newposts;

import net.minecraftforge.common.ForgeConfigSpec;

public class NewpostsConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> GALLERY_ID;
    public static final ForgeConfigSpec.BooleanValue USE_SYSTEM_CHAT;
    public static final ForgeConfigSpec.BooleanValue SHOW_IP_ADDRESS;
    public static final ForgeConfigSpec.BooleanValue SHOW_UID;
    public static final ForgeConfigSpec.IntValue GEULDETHAP;

    static {
        BUILDER.push("general");
        GALLERY_ID   = BUILDER.comment("갤러리 ID (steve가 아닌 ID로 변경하면 깡계체크가 작동하지 않음)").define("galleryId", "steve");
        USE_SYSTEM_CHAT  = BUILDER.comment("액션바에 표시 (true = 액션바, false = 채팅창)").define("useSystemChat", false);
        SHOW_IP_ADDRESS  = BUILDER.comment("유동 IP 보기").define("showIpAddress", true);
        SHOW_UID         = BUILDER.comment("식별코드 보기").define("showuid", true);
        GEULDETHAP       = BUILDER.comment("깡계 글댓합 기준값").defineInRange("geuldethap", 100, 0, Integer.MAX_VALUE);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
