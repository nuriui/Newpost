package com.teashoe.newposts;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Newposts.MOD_ID, name = "Newposts", version = "@VERSION@",
        dependencies = "required-after:Forge@[10.13.4,)",
        guiFactory = "com.teashoe.newposts.NewpostsGuiFactory")
public class Newposts {

    public static final String MOD_ID = "newposts";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SidedProxy(clientSide = "com.teashoe.newposts.ClientProxy",
                serverSide = "com.teashoe.newposts.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[newposts] 모드 초기화 시작");
        NewpostsConfig.init(event.getSuggestedConfigurationFile());
        proxy.preInit(event);
        LOGGER.info("[newposts] preInit 완료");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        LOGGER.info("[newposts] 모드 초기화 완료");
    }
}
