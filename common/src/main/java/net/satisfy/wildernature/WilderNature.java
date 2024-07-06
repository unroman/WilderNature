package net.satisfy.wildernature;

import dev.architectury.event.events.common.LifecycleEvent;
import net.satisfy.wildernature.registry.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.satisfy.wildernature.bountyboard.BountyEntrypoints;
import net.satisfy.wildernature.bountyboard.contract.ContractInProgress;

public class WilderNature {
    public static final String MOD_ID = "wildernature";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void info(String info, Object... objects){
        LOGGER.info(info, objects);
    }

    public static void init() {
        ObjectRegistry.init();
        EntityRegistry.init();
        RecipeRegistry.init();
        TabRegistry.init();
        SoundRegistry.init();
        BountyEntrypoints.serverEntry();
    }

    public static void commonInit() {
        LifecycleEvent.SERVER_BEFORE_START.register(instance -> {
            ContractInProgress.progressPerPlayer.clear();
        });
    }
}

