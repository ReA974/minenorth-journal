package fr.minenorth.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Kiosque à journaux. Un journaliste y range ses journaux (clic droit, journaux en main) ; les joueurs en prennent un (clic droit, main nue),
 * gratuitement ou au prix de la config (prixKiosqueCentimes). Accroupi + clic droit à main nue (journalistes) : tout reprendre.
 * L'état « stock » (0 vide, 1 quelques-uns, 2 plein) change l'apparence : piles de journaux visibles.
 */
public class KioskBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty STOCK = IntegerProperty.create("stock", 0, 2);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 17, 16);

    public KioskBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 6.0f).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STOCK, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, STOCK); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new KioskBlockEntity(pos, state); }

    private static void refresh(Level level, BlockPos pos, BlockState state, KioskBlockEntity be) {
        int t = be.total();
        int stock = t == 0 ? 0 : t <= 16 ? 1 : 2;
        if (state.getValue(STOCK) != stock) level.setBlock(pos, state.setValue(STOCK, stock), 3);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer p) || !(level.getBlockEntity(pos) instanceof KioskBlockEntity be)) return InteractionResult.PASS;
        ItemStack held = p.getItemInHand(hand);
        if (held.getItem() instanceof JournalItem) {
            if (!Compat.isJournalist(p)) { p.displayClientMessage(Component.literal("§cSeuls les journalistes remplissent le kiosque."), true); return InteractionResult.SUCCESS; }
            if (Edition.read(held).blank()) { p.displayClientMessage(Component.literal("§7Ce journal est vierge : rédigez-le d'abord (accroupi + clic droit)."), true); return InteractionResult.SUCCESS; }
            int n = be.deposit(held);
            refresh(level, pos, state, be);
            p.displayClientMessage(Component.literal(n > 0 ? "§a" + n + " journal(aux) rangé(s) dans le kiosque (stock : " + be.total() + ")." : "§cLe kiosque est plein."), true);
            return InteractionResult.SUCCESS;
        }
        if (!held.isEmpty()) return InteractionResult.PASS;
        if (p.isShiftKeyDown() && Compat.isJournalist(p)) {
            int n = 0;
            for (int i = 0; i < KioskBlockEntity.SLOTS; i++) {
                ItemStack s = be.items().get(i);
                if (s.isEmpty()) continue;
                n += s.getCount();
                p.getInventory().placeItemBackInInventory(s.copy());
                be.items().set(i, ItemStack.EMPTY);
            }
            be.setChanged();
            refresh(level, pos, state, be);
            p.displayClientMessage(Component.literal("§7Journaux repris : " + n + "."), true);
            return InteractionResult.SUCCESS;
        }
        if (be.total() == 0) { p.displayClientMessage(Component.literal("§7Le kiosque est vide."), true); return InteractionResult.SUCCESS; }
        int price = JournalConfig.priceCents();
        if (price > 0) {
            ItemStack next = be.peek();
            int company = Compat.pressCompanyOf(p.server, next);
            // Journal rédigé par un membre de l'entreprise de presse : le prix va sur le compte de l'entreprise ; sinon au trésor.
            Boolean paid = company == 0 ? null : Compat.payCompany(p, company, price, "Journal : " + Edition.read(next).title());
            if (paid == null) {
                if (!Compat.charge(p, price, "journal:kiosque")) return InteractionResult.SUCCESS;
            } else if (!paid) {
                return InteractionResult.SUCCESS;
            }
        }
        ItemStack one = be.takeOne();
        p.getInventory().placeItemBackInInventory(one);
        refresh(level, pos, state, be);
        p.displayClientMessage(Component.literal("§aVous prenez un journal" + (price > 0 ? " (" + (price / 100) + "," + String.format("%02d", price % 100) + " €)" : "") + "."), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof KioskBlockEntity be) {
            Containers.dropContents(level, pos, be.items());
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
