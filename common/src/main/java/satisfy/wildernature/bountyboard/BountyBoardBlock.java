package satisfy.wildernature.bountyboard;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import satisfy.wildernature.registry.EntityRegistry;
import satisfy.wildernature.registry.ObjectRegistry;

public class BountyBoardBlock extends BaseEntityBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    private static final VoxelShape SHAPE_BOTTOM_LEFT = makeBottomLeftShape();
    private static final VoxelShape SHAPE_BOTTOM_RIGHT = makeBottomRightShape();
    private static final VoxelShape SHAPE_TOP_LEFT = makeTopLeftShape();
    private static final VoxelShape SHAPE_TOP_RIGHT = makeTopRightShape();

    public BountyBoardBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.BOTTOM_LEFT));
    }

    private static VoxelShape makeBottomLeftShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.4375, 0.125, 1, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.125, 0.5, 0.4375, 1, 1, 0.5625), BooleanOp.OR);
        return shape;
    }

    private static VoxelShape makeBottomRightShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.875, 0, 0.4375, 1, 1, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.5, 0.4375, 0.875, 1, 0.5625), BooleanOp.OR);
        return shape;
    }

    private static VoxelShape makeTopLeftShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.4375, 1, 0.875, 0.5625), BooleanOp.OR);
        return shape;
    }

    private static VoxelShape makeTopRightShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.4375, 1, 0.875, 0.5625), BooleanOp.OR);
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
        var entity = level.getBlockEntity(blockPos);
        if(entity instanceof BountyBoardBlockEntity bountyBoardBlockEntity){
            return bountyBoardBlockEntity;
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    private static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level, BlockEntityType<T> blockEntityType, BlockEntityType<BountyBoardBlockEntity> blockEntityType2){
        return level.isClientSide ? null : createTickerHelper(blockEntityType, blockEntityType2, BountyBoardBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return createTicker(level, blockEntityType, EntityRegistry.BOUNTY_BLOCK.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level world = context.getLevel();
        Player player = context.getPlayer();

        if (!canPlaceAt(world, pos)) {
            return null;
        }

        //world.setBlock(pos, this.defaultBlockState().setValue(PART, Part.BOTTOM_LEFT), 3);
        world.setBlock(pos.above(), this.defaultBlockState().setValue(PART, Part.TOP_LEFT), 3);
        world.setBlock(pos.east(), this.defaultBlockState().setValue(PART, Part.BOTTOM_RIGHT), 3);
        world.setBlock(pos.east().above(), this.defaultBlockState().setValue(PART, Part.TOP_RIGHT), 3);

        world.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.playSound(null, pos, SoundEvents.CHERRY_WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }

        return this.defaultBlockState().setValue(PART, Part.BOTTOM_LEFT);
    }

    private boolean canPlaceAt(Level world, BlockPos pos) {
        return world.getBlockState(pos).canBeReplaced() &&
                world.getBlockState(pos.above()).canBeReplaced() &&
                world.getBlockState(pos.east()).canBeReplaced() &&
                world.getBlockState(pos.east().above()).canBeReplaced();
    }


    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            Part part = state.getValue(PART);
            BlockPos basePos = getBasePos(pos, part);
            destroyAdjacentBlocks(world, basePos);
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    private BlockPos getBasePos(BlockPos pos, Part part) {
        return switch (part) {
            case TOP_LEFT -> pos.below();
            case BOTTOM_RIGHT -> pos.west();
            case TOP_RIGHT -> pos.below().west();
            default -> pos;
        };
    }

    private void destroyAdjacentBlocks(Level world, BlockPos basePos) {
        world.removeBlock(basePos, false);
        world.removeBlock(basePos.above(), false);
        world.removeBlock(basePos.east(), false);
        world.removeBlock(basePos.east().above(), false);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Part part = state.getValue(PART);
        return switch (part) {
            case BOTTOM_LEFT -> SHAPE_BOTTOM_LEFT;
            case BOTTOM_RIGHT -> SHAPE_BOTTOM_RIGHT;
            case TOP_LEFT -> SHAPE_TOP_LEFT;
            case TOP_RIGHT -> SHAPE_TOP_RIGHT;
        };
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
        var pos = getBasePos(blockPos,blockState.getValue(PART));
        var entity = level.getBlockEntity(pos);
        assert entity instanceof BountyBoardBlockEntity;
        if(entity instanceof BountyBoardBlockEntity bountyBoardBlockEntity){
            var blockEntityTag = new CompoundTag();
            bountyBoardBlockEntity.saveAdditional(blockEntityTag);
            var tag = new CompoundTag();
            tag.put("BlockEntityTag",blockEntityTag);
            var stack = new ItemStack(ObjectRegistry.BOUNTY_BOARD.get());
            stack.setTag(tag);
            level.addFreshEntity(new ItemEntity(level,blockPos.getX(),blockPos.getY(),blockPos.getZ(),stack));
        }
        super.playerWillDestroy(level, blockPos, blockState, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        if(blockPos!=getBasePos(blockPos,blockState.getValue(PART))){
            return null;
        }
        return new BountyBoardBlockEntity(blockPos,blockState);
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos originalBlockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if(level.isClientSide())
            return InteractionResult.SUCCESS;
        final var blockPos = getBasePos(originalBlockPos,blockState.getValue(PART));
        var pr = getMenuProvider(blockState, level, blockPos);
        MenuRegistry.openExtendedMenu((ServerPlayer) player, pr, friendlyByteBuf -> {
            var entity = (BountyBoardBlockEntity)level.getBlockEntity(blockPos);
            /////
            friendlyByteBuf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.MULTI);
            friendlyByteBuf.writeShort(3);
            BountyBlockScreenHandler.s_writeUpdateContracts(friendlyByteBuf,entity.getContractsNbt());
            BountyBlockScreenHandler.s_writeBlockDataChange(friendlyByteBuf,entity.rerollsLeft,entity.rerollCooldownLeft,entity.boardId,entity.tier,entity.xp);
            BountyBlockScreenHandler.s_writeActiveContractInfo(friendlyByteBuf,(ServerPlayer) player);
        });
        return InteractionResult.SUCCESS;
    }

    public enum Part implements StringRepresentable {
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_RIGHT("bottom_right"),
        TOP_LEFT("top_left"),
        TOP_RIGHT("top_right");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
