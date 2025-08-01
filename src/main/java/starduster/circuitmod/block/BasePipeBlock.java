package starduster.circuitmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import starduster.circuitmod.Circuitmod;

import java.util.Map;

public abstract class BasePipeBlock extends BlockWithEntity {
    // Connection properties for each direction
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");

    // Add a map of directions to properties for easier access
    public static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = Map.of(
        Direction.NORTH, NORTH,
        Direction.EAST, EAST,
        Direction.SOUTH, SOUTH,
        Direction.WEST, WEST,
        Direction.UP, UP,
        Direction.DOWN, DOWN
    );

    // Base shape for the center of the pipe
    protected static final VoxelShape CORE_SHAPE = Block.createCuboidShape(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);

    // Shapes for each connection
    protected static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    protected static final VoxelShape EAST_SHAPE = Block.createCuboidShape(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    protected static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    protected static final VoxelShape UP_SHAPE = Block.createCuboidShape(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    protected static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);
    
    // Special shape for when a pipe is above a chest - only use horizontal connections and center to allow chest to open
    protected static final VoxelShape ABOVE_CHEST_SHAPE = VoxelShapes.union(
        CORE_SHAPE, NORTH_SHAPE, EAST_SHAPE, SOUTH_SHAPE, WEST_SHAPE
    );

    public BasePipeBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
            .with(NORTH, false)
            .with(EAST, false)
            .with(SOUTH, false)
            .with(WEST, false)
            .with(UP, false)
            .with(DOWN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected boolean isTransparent(BlockState state) {
        return true;
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F; // Full light level - no shadow casting
    }

    @Override
    protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        // Hide faces between adjacent pipe blocks to reduce visual clutter
        return stateFrom.getBlock() instanceof BasePipeBlock ? true : super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE_SHAPE;
        
        // Add shapes for each connection
        if (state.get(NORTH)) {
            shape = VoxelShapes.union(shape, NORTH_SHAPE);
        }
        if (state.get(EAST)) {
            shape = VoxelShapes.union(shape, EAST_SHAPE);
        }
        if (state.get(SOUTH)) {
            shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        }
        if (state.get(WEST)) {
            shape = VoxelShapes.union(shape, WEST_SHAPE);
        }
        if (state.get(UP)) {
            shape = VoxelShapes.union(shape, UP_SHAPE);
        }
        if (state.get(DOWN)) {
            shape = VoxelShapes.union(shape, DOWN_SHAPE);
        }
        
        return shape;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Check if this pipe is above a chest - if so, use a shape that won't block chest opening
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);
        
        if (belowState.getBlock() instanceof ChestBlock) {
            // Use the shape that doesn't extend downward
            return ABOVE_CHEST_SHAPE;
        }
        
        // Otherwise use the normal outline shape
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = getDefaultState();
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        
        // Check each direction for connectable blocks
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            
            // Connect if the neighbor can connect to this pipe
            boolean canConnect = canConnectTo(world, neighborPos, direction.getOpposite());
            
            if (canConnect) {
                state = state.with(DIRECTION_PROPERTIES.get(direction), true);
            }
        }
        
        return state;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
        BlockState state,
        WorldView world,
        ScheduledTickView tickView,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        Random random
    ) {
        // Update connection state when a neighbor changes
        boolean canConnect = false;
        
        // Case 1: Neighbor is another pipe
        if (neighborState.getBlock() instanceof BasePipeBlock) {
            canConnect = true;
            // Check if the neighboring pipe is connected back to us
            BooleanProperty neighborProperty = DIRECTION_PROPERTIES.get(direction.getOpposite());
            boolean neighborConnected = neighborState.get(neighborProperty);
            
            // If the neighbor isn't connected back to us, we need to update it
            if (!neighborConnected && world instanceof World realWorld) {
                // Force update the neighbor's connection state immediately
                realWorld.setBlockState(neighborPos, 
                    neighborState.with(neighborProperty, true), 
                    Block.NOTIFY_ALL);
            }
        } 
        // Case 2: Neighbor is a block entity with inventory
        else if (world instanceof WorldAccess && 
                ((WorldAccess)world).getBlockEntity(neighborPos) instanceof net.minecraft.inventory.Inventory) {
            canConnect = true;
        }
        
        // If this is a real world and not just a view, check for additional processing
        if (world instanceof World && canConnect) {
            BlockEntity be = ((World) world).getBlockEntity(pos);
            if (be != null) {
                // Force the pipe to validate its connections next tick
                ((World) world).scheduleBlockTick(pos, this, 1);
            }
        }
        
        return state.with(DIRECTION_PROPERTIES.get(direction), canConnect);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null) {
            // Check connections and force update if needed
            for (Direction direction : Direction.values()) {
                if (state.get(DIRECTION_PROPERTIES.get(direction))) {
                    BlockPos neighborPos = pos.offset(direction);
                    BlockState neighborState = world.getBlockState(neighborPos);
                    
                    // If neighbor is a pipe, ensure it's connected back to us
                    if (neighborState.getBlock() instanceof BasePipeBlock) {
                        BooleanProperty neighborProperty = DIRECTION_PROPERTIES.get(direction.getOpposite());
                        boolean neighborConnected = neighborState.get(neighborProperty);
                        
                        if (!neighborConnected) {
                            world.setBlockState(neighborPos, 
                                neighborState.with(neighborProperty, true), 
                                Block.NOTIFY_ALL);
                        }
                    }
                }
            }
            
            // Allow subclasses to handle their specific tick logic
            handleScheduledTick(state, world, pos, random, be);
        }
    }
    
    /**
     * Subclasses can override this to handle their specific tick logic
     */
    protected void handleScheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random, BlockEntity blockEntity) {
        // Default implementation does nothing
    }
    
    protected boolean canConnectTo(WorldAccess world, BlockPos pos, Direction face) {
        BlockState state = world.getBlockState(pos);
        // Connect to inventories or other pipes
        return state.getBlock() instanceof BasePipeBlock || 
               hasInventory(world, pos, face);
    }
    
    protected boolean hasInventory(WorldAccess world, BlockPos pos, Direction face) {
        // Check if the block has an inventory we can connect to
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity instanceof net.minecraft.inventory.Inventory;
    }

    /**
     * Get the direction property for a given direction
     */
    public static BooleanProperty getDirectionProperty(Direction direction) {
        return DIRECTION_PROPERTIES.get(direction);
    }
} 