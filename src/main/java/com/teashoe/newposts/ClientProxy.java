package com.teashoe.newposts;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {}

    @Override
    public void init(FMLInitializationEvent event) {
        // MinecraftForge 버스: 일반 게임 이벤트 (tick 등)
        MinecraftForge.EVENT_BUS.register(ClientEvents.INSTANCE);
        // FML 버스: 네트워크 연결/해제 이벤트
        FMLCommonHandler.instance().bus().register(ClientEvents.INSTANCE);
    }
}
