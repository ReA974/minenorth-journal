package fr.minenorth.journal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stock du kiosque : 4 emplacements de journaux (16 par tas), chaque tas garde son édition (NBT). */
public class KioskBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public KioskBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.KIOSK_BE.get(), pos, state); }

    public NonNullList<ItemStack> items() { return items; }

    public int total() {
        int n = 0;
        for (ItemStack s : items) n += s.getCount();
        return n;
    }

    /** Range le tas dans le kiosque (fusion avec la même édition, sinon emplacement libre). Renvoie le nombre de journaux rangés. */
    public int deposit(ItemStack stack) {
        int moved = 0;
        for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) {
                int n = Math.min(stack.getCount(), stack.getMaxStackSize());
                items.set(i, stack.copyWithCount(n));
                stack.shrink(n);
                moved += n;
            } else if (ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int n = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(n);
                stack.shrink(n);
                moved += n;
            }
        }
        if (moved > 0) setChanged();
        return moved;
    }

    /** Le journal que prendrait le prochain client (sans le retirer), ou vide. */
    public ItemStack peek() {
        for (ItemStack s : items) if (!s.isEmpty()) return s;
        return ItemStack.EMPTY;
    }

    /** Retire un journal (copie de l'édition du premier tas non vide), ou vide. */
    public ItemStack takeOne() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                ItemStack one = s.copyWithCount(1);
                s.shrink(1);
                if (s.isEmpty()) items.set(i, ItemStack.EMPTY);
                setChanged();
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
    }
}
