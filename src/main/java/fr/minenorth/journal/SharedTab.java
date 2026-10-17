package fr.minenorth.journal;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Le journal va dans l'onglet créatif commun « MineNorth » (fourni par minenorth_api, s'il est présent). */
@Mod.EventBusSubscriber(modid = MineNorthJournal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SharedTab {
    private SharedTab() {}

    @SubscribeEvent
    public static void fill(BuildCreativeModeTabContentsEvent e) {
        if (!e.getTabKey().location().equals(new ResourceLocation("minenorthapi", "minenorth"))) return;
        ModItems.TAB_ITEMS.accept(e.getParameters(), e);
    }
}
