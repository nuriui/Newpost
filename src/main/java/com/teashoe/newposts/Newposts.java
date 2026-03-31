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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Newposts.MOD_ID)
public class Newposts {

    public static final String MOD_ID = "newposts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Newposts() {
        LOGGER.info("[newposts] 모드 초기화 시작 (classloader: {})", Newposts.class.getClassLoader().getClass().getName());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NewpostsConfig.SPEC, "newposts-client.toml");
        LOGGER.info("[newposts] 클라이언트 설정 등록 완료");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onConfigReload);

        // ClientEvents는 @Mod.EventBusSubscriber가 자동 등록하므로 중복 등록하지 않음

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LOGGER.info("[newposts] 클라이언트 환경 확인됨 - 설정 화면 등록 중");
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new NewpostsConfigScreen(parent)
                )
            );
        });
        LOGGER.info("[newposts] 모드 초기화 완료");
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            LOGGER.info("[newposts] 설정 리로드 감지됨 - 게시물 번호 재초기화");
            ClientEvents.initializePostNumbers();
        }
    }
}
