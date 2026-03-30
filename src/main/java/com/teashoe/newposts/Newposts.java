package com.teashoe.newposts;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;

@Mod(Newposts.MOD_ID)
public class Newposts {

    public static final String MOD_ID = "newposts";

    public Newposts() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NewpostsConfig.SPEC, "newposts-client.toml");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onConfigReload);

        // ClientEvents는 @Mod.EventBusSubscriber가 자동 등록하므로 중복 등록하지 않음

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new NewpostsConfigScreen(parent)
                )
            )
        );
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            ClientEvents.initializePostNumbers();
        }
    }
}
