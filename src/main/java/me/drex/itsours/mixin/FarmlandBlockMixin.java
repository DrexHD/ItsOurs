package me.drex.itsours.mixin;

import me.drex.itsours.ItsOursMod;
import me.drex.itsours.claim.AbstractClaim;
import me.drex.itsours.user.ClaimPlayer;
import me.drex.itsours.util.Color;
import net.kyori.adventure.text.Component;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {

    @Inject(method = "onLandedUpon", at = @At(value = "HEAD", target = "Lnet/minecraft/block/FarmlandBlock;onLandedUpon(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;F)V"), cancellable = true)
    private void dontYouDareFarmMe(World world, BlockPos pos, Entity entity, float distance, CallbackInfo ci) {
        Optional<AbstractClaim> claim = ItsOursMod.INSTANCE.getClaimList().get((ServerWorld) world, pos);
        if (claim.isPresent() && entity instanceof ServerPlayerEntity && !claim.get().hasPermission(entity.getUuid(), "mine.farmland")) {
            ClaimPlayer claimPlayer = (ClaimPlayer) (ServerPlayerEntity) entity;
            claimPlayer.sendError(Component.text("You can't break that block here.").color(Color.RED));
            ci.cancel();
        }
    }

}