package starduster.circuitmod.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import starduster.circuitmod.block.machines.BloomeryBlock;
import starduster.circuitmod.recipe.BloomeryRecipe;
import starduster.circuitmod.recipe.BloomeryRecipeInput;
import starduster.circuitmod.recipe.ModRecipes;
import starduster.circuitmod.screen.BloomeryScreenHandler;
import starduster.circuitmod.util.ImplementedInventory;

import java.util.Optional;

public class BloomeryBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);

    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    protected final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 200; //default max time
    private int burnTime = 0;
    private int maxBurnTime = 50;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOMERY_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0 -> {return BloomeryBlockEntity.this.progress;}
                    case 1 -> {return BloomeryBlockEntity.this.maxProgress;}
                    case 2 -> {return BloomeryBlockEntity.this.burnTime;}
                    case 3 -> {return BloomeryBlockEntity.this.maxBurnTime;}
                    default -> {return 0;}
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> BloomeryBlockEntity.this.progress = value;
                    case 1 -> BloomeryBlockEntity.this.maxProgress = value;
                    case 2 -> BloomeryBlockEntity.this.burnTime = value;
                    case 3 -> BloomeryBlockEntity.this.maxBurnTime = value;
                }
            }

            @Override
            public int size() {
                return 4;
            }
        };
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.circuitmod.bloomery");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BloomeryScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt("bloomery.progress", progress);
        nbt.putInt("bloomery.max_progress", maxProgress);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.readNbt(nbt, inventory, registryLookup);
        progress = nbt.getInt("bloomery.progress").get();
        maxProgress = nbt.getInt("bloomery.max_progress").get();
        super.readNbt(nbt, registryLookup);
    }


    public void tick(World world, BlockPos pos, BlockState state, BloomeryBlockEntity blockEntity) {
        boolean burningBefore = blockEntity.isBurning();

        if(burnTime > 0) {
            burnTime = burnTime - 1;
        }
        if(burnTime <= 0 && hasRecipe() && hasFuel()) {
            consumeFuel();
            burnTime = maxBurnTime;
        }

        if(hasRecipe() && burnTime > 0) {
            increaseSmeltProgress();
            markDirty(world, pos, state);

            if(hasSmeltingFinished()) {
                craftItem();
                resetProgress();
            }
        } else {
            resetProgress();
        }

        if (burningBefore != blockEntity.isBurning()) {
            world.setBlockState(pos, world.getBlockState(pos).with(BloomeryBlock.LIT, blockEntity.isBurning()), Block.NOTIFY_ALL);
        }
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    private void consumeFuel() {
        this.removeStack(FUEL_SLOT, 1);
    }

    private boolean hasFuel() {
        Item fuelType1 = Items.CHARCOAL;
        Item fuelType2 = Items.COAL;

        return this.getStack(FUEL_SLOT).isOf(fuelType1) || this.getStack(FUEL_SLOT).isOf(fuelType2);
    }

//    private boolean isFuel(ItemStack stack) {
//        // For now, just coal and charcoal
//        return stack.isIn(net.minecraft.registry.tag.ItemTags.COALS);
//    }

    private void resetProgress() {
        this.progress = 0;
        this.maxProgress = 200; //default max time
    }

    private void craftItem() {
        Optional<RecipeEntry<BloomeryRecipe>> recipe = getCurrentRecipe();
        ItemStack output = recipe.get().value().output();
        this.removeStack(INPUT_SLOT, 1);
        this.setStack(OUTPUT_SLOT, new ItemStack(output.getItem(), this.getStack(OUTPUT_SLOT).getCount() + output.getCount()));
    }

    private boolean hasSmeltingFinished() {
        return this.progress >= this.maxProgress;
    }


    private void increaseSmeltProgress() {
        this.progress++;
    }

    private boolean hasRecipe() {
        Optional<RecipeEntry<BloomeryRecipe>> recipe = getCurrentRecipe();
        if(recipe.isEmpty()) {
            return false;
        }
        ItemStack output = recipe.get().value().output();
        return canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
    }

    private Optional<RecipeEntry<BloomeryRecipe>> getCurrentRecipe() {
        return ((ServerWorld) this.getWorld()).getRecipeManager()
                .getFirstMatch(ModRecipes.BLOOMERY_TYPE, new BloomeryRecipeInput(inventory.get(INPUT_SLOT)), this.getWorld());
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack output) {
        return this.getStack(OUTPUT_SLOT).isEmpty() || this.getStack(OUTPUT_SLOT).getItem() == output.getItem();
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        int maxCount = this.getStack(OUTPUT_SLOT).isEmpty() ? 64 : this.getStack(OUTPUT_SLOT).getMaxCount();
        int currentCount = this.getStack(OUTPUT_SLOT).getCount();

        return maxCount >= currentCount + count;
    }


    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
