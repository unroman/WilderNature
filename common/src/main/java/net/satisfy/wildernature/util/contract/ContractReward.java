package net.satisfy.wildernature.util.contract;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.satisfy.wildernature.util.WilderNatureIdentifier;
import org.intellij.lang.annotations.Identifier;

import java.util.Optional;

public record ContractReward(int blockExpReward, int playerExpReward, Optional<ResourceLocation> playerRewardLoot){
    public static Codec<ContractReward> CODEC = RecordCodecBuilder.create(contractRewardInstance -> contractRewardInstance.group(
            Codec.INT.fieldOf("blockExpReward").forGetter(ContractReward::blockExpReward),
            Codec.INT.fieldOf("playerExpReward").forGetter(ContractReward::playerExpReward),
            ResourceLocation.CODEC.optionalFieldOf("loot").forGetter(ContractReward::playerRewardLoot)
    ).apply(contractRewardInstance,ContractReward::new));

    public static ResourceLocation LOOT_EMPTY = new WilderNatureIdentifier("empty");
}
