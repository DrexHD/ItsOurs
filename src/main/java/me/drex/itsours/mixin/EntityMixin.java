package me.drex.itsours.mixin;

import me.drex.itsours.ItsOursMod;
import me.drex.itsours.claim.AbstractClaim;
import me.drex.itsours.user.ClaimPlayer;
import me.drex.itsours.util.Color;
import net.kyori.adventure.text.Component;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract EntityType<?> getType();

    @Shadow public abstract BlockPos getBlockPos();

    public Optional<AbstractClaim> pclaim = Optional.empty();
    protected UUID uuid;
    Vec3d ppos;

    @Inject(method = "setPos", at = @At("RETURN"))
    public void itsours$doPostPosActions(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            if (player.getBlockPos() == null) return;
            Optional<AbstractClaim> claim = ItsOursMod.INSTANCE.getClaimList().get((ServerWorld) player.world, player.getBlockPos());
            if (!pclaim.equals(claim)) {
                if (player.networkHandler != null) {
                    pclaim.ifPresent(c -> c.onLeave(claim, player));
                    claim.ifPresent(c -> {
                        if (c.getRestrictionManager().canEnter(player.getUuid())) {
                            if (pclaim.isPresent()) {
                                if (pclaim.get().getRestrictionManager().canEnter(player.getUuid())) {
                                    c.onEnter(pclaim, player);
                                } else {
                                    player.requestTeleport(ppos.getX(), ppos.getY(), ppos.getZ());
                                }
                            } else {
                                player.requestTeleport(ppos.getX(), ppos.getY(), ppos.getZ());
                            }
                        } else {
                            c.onEnter(pclaim, player);
                        }
                    });
                }
            }
            pclaim = ItsOursMod.INSTANCE.getClaimList().get((ServerWorld) player.world, player.getBlockPos());
            ppos = player.getPos();
        }
    }

    @Redirect(method = "checkBlockCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V"))
    private void itsours$onBlockCollision(BlockState blockState, World world, BlockPos pos, Entity entity) {
        ServerPlayerEntity playerEntity = null;
        if (entity instanceof ProjectileEntity && (blockState.getBlock() instanceof AbstractButtonBlock || blockState.getBlock() instanceof AbstractPressurePlateBlock)) {
            ProjectileEntity projectileEntity = (ProjectileEntity) entity;
            if (projectileEntity.getOwner() != null && projectileEntity.getOwner() instanceof ServerPlayerEntity) {
                playerEntity = (ServerPlayerEntity) projectileEntity.getOwner();
            }
        } else if (entity instanceof ServerPlayerEntity && blockState.getBlock() instanceof AbstractPressurePlateBlock) {
            playerEntity = (ServerPlayerEntity) entity;
        } else if (entity instanceof ItemEntity && blockState.getBlock() instanceof AbstractPressurePlateBlock) {
            ItemEntity item = (ItemEntity) entity;
            if (item.getThrower() != null) {
                playerEntity = ItsOursMod.server.getPlayerManager().getPlayer(item.getThrower());
            }
        }
        if (playerEntity == null) {
            blockState.onEntityCollision(world, pos, entity);
            return;
        }
        Optional<AbstractClaim> claim = ItsOursMod.INSTANCE.getClaimList().get((ServerWorld) world, pos);
        if (!claim.isPresent()) {
            blockState.onEntityCollision(world, pos, entity);
            return;
        }
        if (!claim.get().hasPermission(playerEntity.getUuid(), "interact_block." + Registry.BLOCK.getId(blockState.getBlock()).getPath())) {
            ClaimPlayer claimPlayer = (ClaimPlayer) playerEntity;
            claimPlayer.sendError(Component.text("You can't interact with that block here.").color(Color.RED));
            return;
        }
        blockState.onEntityCollision(world, pos, entity);
    }

}
