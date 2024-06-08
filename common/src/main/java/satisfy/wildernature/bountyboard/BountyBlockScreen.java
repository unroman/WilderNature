package satisfy.wildernature.bountyboard;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import satisfy.wildernature.bountyboard.contract.Contract;
import satisfy.wildernature.bountyboard.contract.ContractButton;
import satisfy.wildernature.util.WilderNatureIdentifier;

import java.util.ArrayList;

public class BountyBlockScreen extends AbstractContainerScreen<BountyBlockScreenHandler> {

    private WilderNatureIdentifier TEX_BACKGROUND = new WilderNatureIdentifier("textures/gui/bounty_board/background.png");
    private WilderNatureIdentifier TEX_REROLL = new WilderNatureIdentifier("textures/gui/bounty_board/reroll.png");
    private WilderNatureIdentifier TEX_ACCEPT = new WilderNatureIdentifier("textures/gui/bounty_board/accept.png");
    private WilderNatureIdentifier TEX_BAR = new WilderNatureIdentifier("textures/gui/bounty_board/bar.png");
    private Tooltip getTooltip;
    private ImageButton rerollButton;
    private ImageButton acceptButton;
    private Button finishButton;
    private ContractButton contractButtons[] = new ContractButton[3];
    private ContractButton selectedContractButton;

    public BountyBlockScreen(BountyBlockScreenHandler abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();
        var centerX = width/2;
        var centerY = height/2;
        var guiX = width/2-176/2;
        var guiY = height/2-169/2;
        rerollButton = new ImageButton(centerX-74,centerY-52,14,14,0,0,14, TEX_REROLL,14,42,this::onReroll);
        acceptButton = new ImageButton(centerX-176/2+135,centerY-169/2+51,14,14,0,0,14, TEX_ACCEPT,14,42,this::onAccept);
        finishButton = new Button.Builder(Component.translatable("text.gui.wildernature.bounty.finish"),(button)->{
            onFinish();
        }).pos(centerX+176/2+4,centerY-169/2+14).width(120).build();

        addRenderableWidget(rerollButton);
        addRenderableWidget(acceptButton);
        addRenderableWidget(finishButton);
        acceptButton.setTooltip(Tooltip.create(Component.translatable("text.gui.wildernature.bounty.accept")));
        for(int i=0;i<3;i++){
            var contract = menu.c_contracts[i];
            contractButtons[i] = addRenderableWidget(new ContractButton(centerX-176/2+25+i*18,centerY-169/2+49,contract,(button)->{
                this.setSelectedContract(((ContractButton)button).getContract());
            }));
        }
        selectedContractButton = new ContractButton(centerX-176/2+97,centerY-169/2+49,null,(button)->{});
        addRenderableWidget(selectedContractButton);
        menu.c_onContractUpdate.subscribe(()->{
            selectedContractButton.setContract(null);
            finishButton.visible = menu.c_activeContract!=null && menu.c_activeContract.isFinished();
            for(int i=0;i<3;i++){
                contractButtons[i].setContract(menu.c_contracts[i]);
            }
        });
        addRenderableOnly((guiGraphics,mx,my,f)->{
            Tooltip.create(Component.literal("123"));
            int xPos = centerX+176/2-4;
            int yPos = centerY-169/2+30-1+(menu.c_activeContract !=null&&menu.c_activeContract.isFinished()?24:0);
            if(menu.c_activeContract !=null){
                var tooltipBorders = 9;
                var tooltipTextWidth = 120-tooltipBorders;
                if(menu.c_boardId != menu.c_activeContract.boardId) {
                    var tip = minecraft.font.split(Component.translatable("text.gui.wildernature.bounty.finish.warning"), tooltipTextWidth);
                    guiGraphics.renderTooltip(minecraft.font, tip, xPos, yPos);
                    var rowsHeight = (tip.size() * 10);
                    var row1 = (tip.size() == 1 ? 0 : 2); // for some reason gap between first and second line is 5 px, but after that gap is 3 px
                    yPos += rowsHeight + tooltipBorders + row1;
                }

                var contractSplit = minecraft.font.split(Component.translatable("text.gui.wildernature.bounty.currentcontract"),tooltipTextWidth);
                var nameSplit = minecraft.font.split(Component.translatable(menu.c_activeContract.getS_contract().name()),tooltipTextWidth);
                var descriptionSplit = minecraft.font.split(Component.translatable(menu.c_activeContract.getS_contract().description()),tooltipTextWidth);
                var progressSplit = minecraft.font.split(Component.translatable("text.gui.wildernature.bounty.progress", menu.c_activeContract.getS_contract().count()-menu.c_activeContract.count, menu.c_activeContract.getS_contract().count()),tooltipTextWidth);

                var list = new ArrayList<FormattedCharSequence>();
                list.addAll(contractSplit);
                list.addAll(nameSplit);
                list.addAll(descriptionSplit);
                list.addAll(progressSplit);
                guiGraphics.renderTooltip(minecraft.font,list,xPos,yPos);
            }
        });
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            guiGraphics.drawCenteredString(minecraft.font,Component.translatable(menu.c_tierId.toLanguageKey()),centerX,guiY-15,0xFFFFFFFF);
            var progress = menu.c_progress;
            guiGraphics.blit(TEX_BAR,guiX+12,guiY+5,0f,0f, (int) (153*progress),12, 153,12);
        }));
    }

    private void onFinish() {
        var buf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT,0,2048));
        buf.writeEnum(BountyBlockNetworking.BountyClientActionType.FINISH_CONTRACT);
        NetworkManager.sendToServer(BountyBlockNetworking.ID_SCREEN_ACTION,buf);
    }


    private void onAccept(Button button) {
        for(int i=0;i<3;i++){
            if(contractButtons[i].getContract() == selectedContractButton.getContract()){
                var buf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT,0,64));
                buf.writeEnum(BountyBlockNetworking.BountyClientActionType.CONFIRM_CONTRACT);
                buf.writeByte(i);
                NetworkManager.sendToServer(BountyBlockNetworking.ID_SCREEN_ACTION,buf);
                return;
            }
        }
        throw new RuntimeException("not found contract");
    }

    private void setSelectedContract(Contract contract) {
        selectedContractButton.setContract(contract);
    }

    private void onReroll(Button button) {
        var buf = new FriendlyByteBuf(new UnpooledHeapByteBuf(ByteBufAllocator.DEFAULT,0,64));
        buf.writeEnum(BountyBlockNetworking.BountyClientActionType.REROLL);
        NetworkManager.sendToServer(BountyBlockNetworking.ID_SCREEN_ACTION,buf);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mx, int my, float f) {
        finishButton.visible = menu.c_activeContract!=null && menu.c_activeContract.isFinished();
        updateTooltip();
        acceptButton.visible = menu.c_activeContract == null;
        acceptButton.active = menu.c_activeContract == null;
        super.render(guiGraphics, mx, my, f);
        renderTooltip(guiGraphics, mx, my);

//        for(int i=0;i<menu.c_contracts.length;i++){
//            guiGraphics.drawString(minecraft.font,menu.c_contracts[i].reward().playerRewardLoot().toString(),16,16*i,0xFFFFFFFF);
//        }
//        guiGraphics.drawString(minecraft.font,menu.c_time+"",16,16*6,0xFFFFFFFF);

    }

    private void updateTooltip() {
        var comp = Component.translatable("text.gui.wildernature.bounty.reroll.left",menu.c_rerolls);
        if(menu.c_time>0){
            comp = comp.append("\n").append(Component.translatable("text.gui.wildernature.bounty.reroll.time",menu.c_time/20/60,menu.c_time/20%60));
        }
        Tooltip tooltip = Tooltip.create(comp);
        rerollButton.setTooltip(tooltip);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        guiGraphics.fill(0,0,width,height, FastColor.ARGB32.color(64,0,0,0));
        guiGraphics.blit(TEX_BACKGROUND,
                width/2 - (176 / 2),
                height/2 - (169 / 2),
                0,
                0,
                176,
                169);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
    }
}
