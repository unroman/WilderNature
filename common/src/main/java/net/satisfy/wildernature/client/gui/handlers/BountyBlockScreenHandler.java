package net.satisfy.wildernature.client.gui.handlers;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.satisfy.wildernature.WilderNature;
import net.satisfy.wildernature.network.BountyBlockNetworking;
import net.satisfy.wildernature.block.entity.BountyBoardBlockEntity;
import net.satisfy.wildernature.util.BountyBoardTier;
import net.satisfy.wildernature.event.EventManager;
import net.satisfy.wildernature.util.contract.Contract;
import net.satisfy.wildernature.util.contract.ContractInProgress;
import net.satisfy.wildernature.item.ContractItem;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("all")
public class BountyBlockScreenHandler extends AbstractContainerMenu {

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(WilderNature.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<BountyBlockScreenHandler>> BOUNTY_BLOCK = MENUS.register("bounty_menu",
            () -> MenuRegistry.ofExtended((id, inventory, buf) -> {
                BountyBlockScreenHandler bountyBlockScreenHandler = BountyBlockScreenHandler.c_createClient(id, inventory);
                bountyBlockScreenHandler.c_onServerUpdate(buf);
                return bountyBlockScreenHandler;
            })
    );

    public static void registerMenuTypes() {
        MENUS.register();
    }

    private BountyBlockScreenHandler(int id, Inventory inventory, BountyBoardBlockEntity s_targetEntity) {
        super(BOUNTY_BLOCK.get(), id);
        this.s_targetEntity = s_targetEntity;
        if (s_targetEntity != null) {
            s_targetEntity.onTick.subscribe(() -> {
                if (s_targetEntity.rerollCooldownLeft % 20 == 0 && inventory.player.containerMenu == this) {
                    var buf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT, 0, BountyBlockNetworking.MAX_SIZE));
                    s_writeBlockDataChange(buf, this.s_targetEntity.rerollsLeft, this.s_targetEntity.rerollCooldownLeft, this.s_targetEntity.boardId, s_targetEntity.tier, s_targetEntity.xp);
                    NetworkManager.sendToPlayer((ServerPlayer) inventory.player, BountyBlockNetworking.ID_SCREEN_UPDATE, buf);
                }
            });
            s_targetEntity.onBlockDataChange.subscribe(() -> {
                if (inventory.player.containerMenu == this) {
                    var buf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT, 0, BountyBlockNetworking.MAX_SIZE));
                    /////
                    buf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.MULTI);
                    buf.writeShort(3);
                    s_writeBlockDataChange(buf, this.s_targetEntity.rerollsLeft, this.s_targetEntity.rerollCooldownLeft, this.s_targetEntity.boardId, s_targetEntity.tier, s_targetEntity.xp);
                    s_writeUpdateContracts(buf, s_targetEntity);
                    s_writeActiveContractInfo(buf, (ServerPlayer) inventory.player);
                    NetworkManager.sendToPlayer((ServerPlayer) inventory.player, BountyBlockNetworking.ID_SCREEN_UPDATE, buf);

                }
            });
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inventory, i * 9 + j + 9, j * 18 + 8, i * 18 + 86));
            }
        }
        for (int j = 0; j < 9; j++) {
            addSlot(new Slot(inventory, j, j * 18 + 8, 144));
        }
    }

    public static AbstractContainerMenu s_createServer(int i, Inventory inventory, BountyBoardBlockEntity bountyBoardBlockEntity) {
        return new BountyBlockScreenHandler(i, inventory, bountyBoardBlockEntity);
    }

    public BountyBoardBlockEntity s_targetEntity;

    public static void s_writeBlockDataChange(FriendlyByteBuf buf, int rerolls, int time, long boardId, ResourceLocation tierId, int xp) {
        buf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.SEND_BOARD_DATA);
        buf.writeInt(time);
        buf.writeByte(rerolls);
        buf.writeLong(boardId);
        buf.writeResourceLocation(tierId);
        buf.writeFloat(BountyBoardTier.byId(tierId).get().progress(xp));
    }

    public static void s_writeActiveContractInfo(FriendlyByteBuf buf, ServerPlayer player) {
        var contractProgress = ContractInProgress.progressPerPlayer.get(player.getUUID());
        if (contractProgress == null) {
            buf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.CLEAR_ACTIVE_CONTRACT);
        } else {
            buf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.SET_ACTIVE_CONTRACT);
            buf.writeNbt((CompoundTag) ContractInProgress.SERVER_CODEC.encode(contractProgress, NbtOps.INSTANCE, new CompoundTag()).get().left().get());
            buf.writeNbt((CompoundTag) Contract.CODEC.encode(contractProgress.s_getContract(), NbtOps.INSTANCE, new CompoundTag()).get().left().get());
        }
    }

    public static void s_writeUpdateContracts(FriendlyByteBuf friendlyByteBuf, BountyBoardBlockEntity blockEntity) {
        friendlyByteBuf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.UPDATE_CONTRACTS);
        friendlyByteBuf.writeNbt(blockEntity.getContractsNbt());
    }

    public void s_handleClientAction(ServerPlayer player, FriendlyByteBuf buf) {
        var action = buf.readEnum(BountyBlockNetworking.BountyClientActionType.class);
        if (action == BountyBlockNetworking.BountyClientActionType.REROLL) {
            WilderNature.info("Player {} Rerolled", player.getScoreboardName());
            s_targetEntity.tryReroll();
            s_targetEntity.getLevel();
        }
        if (action == BountyBlockNetworking.BountyClientActionType.CONFIRM_CONTRACT) {
            var playerContract = ContractInProgress.progressPerPlayer.get(player.getUUID());
            var hasContract = playerContract == null;
            if (!hasContract) {
                player.sendSystemMessage(Component.literal("Error: you already have contract"));
                return;
            }
            var id = buf.readByte();
            var contract = s_targetEntity.getContracts()[id];
            s_targetEntity.setRandomContactInSlot(id);
            var newBuf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT, 0, BountyBlockNetworking.MAX_SIZE));
            var stack = Contract.fromId(contract).contractStack();
            stack.setTag(new CompoundTag());
            assert stack.getTag() != null;
            stack.getTag().putString(ContractItem.TAG_CONTRACT_ID, contract.toString());
            stack.getTag().putUUID(ContractItem.TAG_PLAYER, player.getUUID());
            player.spawnAtLocation(stack);
            ContractInProgress.progressPerPlayer.put(player.getUUID(), new ContractInProgress(contract, Contract.fromId(contract).count(), s_targetEntity.boardId));
            newBuf.writeEnum(BountyBlockNetworking.BountyServerUpdateType.MULTI);
            newBuf.writeShort(2);
            BountyBlockScreenHandler.s_writeActiveContractInfo(newBuf, player);
            BountyBlockScreenHandler.s_writeUpdateContracts(newBuf, s_targetEntity);
            NetworkManager.sendToPlayer(player, BountyBlockNetworking.ID_SCREEN_UPDATE, newBuf);
        }
        if (action == BountyBlockNetworking.BountyClientActionType.FINISH_CONTRACT) {
            var contract = ContractInProgress.progressPerPlayer.get(player.getUUID());
            if (contract == null) {
                player.sendSystemMessage(Component.literal("Error: player does not have a contract"));
                return;
            }
            if (!contract.isFinished()) {
                player.sendSystemMessage(Component.literal("Error: contract is not finished"));
                return;
            }
            player.sendSystemMessage(Component.translatable("text.gui.wildernature.bounty.finished", Component.translatable(contract.s_getContract().name())));
            ContractInProgress.progressPerPlayer.remove(player.getUUID());

            contract.onFinish(player);

            var newBuf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT, 0, BountyBlockNetworking.MAX_SIZE));
            BountyBlockScreenHandler.s_writeActiveContractInfo(newBuf, player);
            NetworkManager.sendToPlayer(player, BountyBlockNetworking.ID_SCREEN_UPDATE, newBuf);
        }
        s_targetEntity.setChanged();
    }

    private static BountyBlockScreenHandler c_createClient(int id, Inventory inventory) {
        return new BountyBlockScreenHandler(id, inventory, null);
    }

    public EventManager c_onContractUpdate = new EventManager();
    public ContractInProgress c_activeContractProgress;
    public Contract c_activeContract;

    public long c_boardId;
    public Contract[] c_contracts;
    public int c_time;
    public int c_rerolls;
    public float c_progress;
    public ResourceLocation c_tierId;

    public void c_onServerUpdate(FriendlyByteBuf buf) {
        var updateType = buf.readEnum(BountyBlockNetworking.BountyServerUpdateType.class);
        try {
            Platform.isDevelopmentEnvironment();
            if (updateType == BountyBlockNetworking.BountyServerUpdateType.MULTI) {
                int count = buf.readShort();
                for (int i = 0; i < count; i++) {
                    c_onServerUpdate(buf);
                }
            }
            if (updateType == BountyBlockNetworking.BountyServerUpdateType.UPDATE_CONTRACTS) {
                this.c_contracts = Contract.CODEC.listOf().decode(NbtOps.INSTANCE, Objects.requireNonNull(buf.readNbt()).get("list")).getOrThrow(false, (er) -> {
                    throw new RuntimeException(er);
                }).getFirst().toArray(new Contract[3]);
                c_onContractUpdate.invoke();
            }
            if (updateType == BountyBlockNetworking.BountyServerUpdateType.SEND_BOARD_DATA) {
                this.c_time = buf.readInt();
                this.c_rerolls = buf.readByte();
                this.c_boardId = buf.readLong();
                this.c_tierId = buf.readResourceLocation();
                this.c_progress = buf.readFloat();
            }
            if (updateType == BountyBlockNetworking.BountyServerUpdateType.SET_ACTIVE_CONTRACT) {
                if (Platform.isDevelopmentEnvironment()) {
                    Minecraft.getInstance().gui.getChat().addMessage(Component.literal("_handling SET_ACTIVE_CONTRACT"));
                }
                var nbt = buf.readNbt();
                var contractProgress = ContractInProgress.SERVER_CODEC.decode(NbtOps.INSTANCE, nbt).getOrThrow(false, (er) -> {
                    throw new RuntimeException(er);
                }).getFirst();
                nbt = buf.readNbt();
                var contract = Contract.CODEC.decode(NbtOps.INSTANCE, nbt).getOrThrow(false, (er) -> {
                    throw new RuntimeException(er);
                }).getFirst();
                if (Platform.isDevelopmentEnvironment()) {
                    Minecraft.getInstance().gui.getChat().addMessage(Component.literal("_active contract: %b".formatted(contractProgress)));
                }
                this.c_activeContractProgress = contractProgress;
                this.c_activeContract = contract;
            }
            if (updateType == BountyBlockNetworking.BountyServerUpdateType.CLEAR_ACTIVE_CONTRACT) {
                this.c_activeContractProgress = null;
                this.c_activeContract = null;
            }
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Error handling %s screen update packet: %s".formatted(updateType.toString(), e.getMessage())));
            throw new RuntimeException(e);
        }
    }
}
