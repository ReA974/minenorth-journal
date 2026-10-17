package fr.minenorth.journal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MineNorthJournal.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MineNorthJournal.MOD_ID);

    public static final RegistryObject<Block> KIOSK = BLOCKS.register("kiosque", KioskBlock::new);
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<KioskBlockEntity>> KIOSK_BE = BLOCK_ENTITIES.register("kiosque",
            () -> BlockEntityType.Builder.of(KioskBlockEntity::new, KIOSK.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
