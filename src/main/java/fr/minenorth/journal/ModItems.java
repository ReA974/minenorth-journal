package fr.minenorth.journal;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineNorthJournal.MOD_ID);
    public static final RegistryObject<Item> JOURNAL = ITEMS.register("journal", JournalItem::new);
    public static final CreativeModeTab.DisplayItemsGenerator TAB_ITEMS = (params, out) -> out.accept(JOURNAL.get());

    public static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
