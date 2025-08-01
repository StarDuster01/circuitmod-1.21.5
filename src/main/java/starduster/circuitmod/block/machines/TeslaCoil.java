package starduster.circuitmod.block.machines;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import starduster.circuitmod.Circuitmod;
import starduster.circuitmod.block.entity.TeslaCoilBlockEntity;
import starduster.circuitmod.block.entity.ModBlockEntities;

public class TeslaCoil extends BlockWithEntity {
    public static final MapCodec<TeslaCoil> CODEC = createCodec(TeslaCoil::new);
    public static final BooleanProperty RUNNING = BooleanProperty.of("running");
//    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0,0.0,0.0,16.0,1.0,16.0);
    private static final VoxelShape SHAPE = VoxelShapes.union(Block.createCuboidShape(0.0,0.0,0.0,16,16,16), Block.createCuboidShape(5.0,16.0,5.0,11.0,28.0,11.0), Block.createCuboidShape(1,28,1,15,32,15));

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public MapCodec<TeslaCoil> getCodec() {
        return CODEC;
    }

    public TeslaCoil(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH));
        setDefaultState(getDefaultState().with(RUNNING, false));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        Circuitmod.LOGGER.info("Tesla Coil placed with facing direction: " + facing);
        return this.getDefaultState().with(HorizontalFacingBlock.FACING, facing);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HorizontalFacingBlock.FACING);
        builder.add(RUNNING);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TeslaCoilBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.TESLA_COIL_BLOCK_ENTITY, TeslaCoilBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof TeslaCoilBlockEntity teslaCoil) {
                // Display tesla coil status when right-clicked
                String status = teslaCoil.isActive() ? "§aActive" : "§cInactive";
                player.sendMessage(net.minecraft.text.Text.literal("§6Tesla Coil Status: " + status), false);
                
                if (teslaCoil.getNetwork() != null) {
                    player.sendMessage(net.minecraft.text.Text.literal("§7Energy demand: §e20§7 energy/tick"), false);
                    player.sendMessage(net.minecraft.text.Text.literal("§7Damage range: §e5§7 blocks"), false);
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("§cNot connected to any network!"), false);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    // Add method to connect to network when placed
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        // The block entity will handle network connection in its tick method
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!moved) {
            // Handle network updates when this block is removed
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof TeslaCoilBlockEntity teslaCoil) {
                // Use the new onRemoved method to properly handle network cleanup
                teslaCoil.onRemoved();
            }
        }
        
        super.onStateReplaced(state, world, pos, moved);
    }
} 