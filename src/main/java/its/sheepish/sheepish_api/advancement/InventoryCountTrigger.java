package its.sheepish.sheepish_api.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.item.Item;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class InventoryCountTrigger extends AbstractCriterion<InventoryCountTrigger.Conditions> {

    // This is the method the compiler is looking for
    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    // Ensure we implement CriterionConditions
    public record Conditions(Optional<LootContextPredicate> player, Optional<Item> item, int count) implements AbstractCriterion.Conditions {

        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootContextPredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                Registries.ITEM.getCodec().optionalFieldOf("item").forGetter(Conditions::item),
                Codec.INT.fieldOf("count").forGetter(Conditions::count)
        ).apply(instance, Conditions::new));

        public boolean matches(ServerPlayerEntity player) {
            if (item.isEmpty()) return false;
            // Scans the whole inventory and sums up the non-stackable items
            return player.getInventory().count(item.get()) >= count;
        }
    }
}