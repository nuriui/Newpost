package com.teashoe.newposts;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Newposts.MOD_ID)
public class Newposts {

    public static final String MOD_ID = "newposts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Newposts(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[newposts] 모드 초기화 시작 (classloader: {})", Newposts.class.getClassLoader().getClass().getName());
        modContainer.registerConfig(ModConfig.Type.CLIENT, NewpostsConfig.SPEC, "newposts-client.toml");
        LOGGER.info("[newposts] 클라이언트 설정 등록 완료");

        modEventBus.addListener(this::onConfigReload);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LOGGER.info("[newposts] 클라이언트 환경 확인됨 - 설정 화면 등록 중");
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new NewpostsConfigScreen(parent));
        }
        LOGGER.info("[newposts] 모드 초기화 완료");
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            LOGGER.info("[newposts] 설정 리로드 감지됨 - 게시물 번호 재초기화");
            ClientEvents.initializePostNumbers();
        }
    }
}
