package its.sheepish.sheepish_api.mixin;

import its.sheepish.API;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class InventoryTriggerMixin {
    @Shadow @Final public PlayerEntity player;

    // 1. Catches manual changes and slot logic
    @Inject(method = "markDirty", at = @At("TAIL"))
    private void onMarkDirty(CallbackInfo ci) {
        this.sheepish$triggerAdvancement();
    }

    // 2. Catches direct slot setting (commands, specific logic)
    @Inject(method = "setStack", at = @At("TAIL"))
    private void onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        this.sheepish$triggerAdvancement();
    }

    // 3. Catches picking up items from the ground
    // insertStack is called by ItemEntity when a player walks over it
    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("RETURN"))
    private void onInsertStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Only trigger if the item was actually added (Return value is true)
        if (cir.getReturnValue()) {
            this.sheepish$triggerAdvancement();
        }
    }

    @Unique
    private void sheepish$triggerAdvancement() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            API.INVENTORY_COUNT.trigger(serverPlayer);
        }
    }
}