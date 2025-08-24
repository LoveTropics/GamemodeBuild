package com.lovetropics.gamemodebuild.client;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import com.lovetropics.gamemodebuild.container.BuildContainer;
import com.lovetropics.gamemodebuild.message.ListUpdateMessage;
import com.lovetropics.gamemodebuild.message.SetActiveMessage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@Mod(value = GamemodeBuild.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GamemodeBuild.MODID, value = Dist.CLIENT)
public class GBClient {
    public GBClient(IEventBus bus) {
        bus.addListener((final RegisterMenuScreensEvent event) -> {
            event.register(BuildContainer.TYPE.get(), BuildScreen::new);
        });
    }

    @SubscribeEvent
    public static void onPayloadHandlerRegister(RegisterClientPayloadHandlersEvent event) {
        event.register(ListUpdateMessage.TYPE, ListUpdateMessage::handle);
        event.register(SetActiveMessage.TYPE, SetActiveMessage::handle);
    }
}
