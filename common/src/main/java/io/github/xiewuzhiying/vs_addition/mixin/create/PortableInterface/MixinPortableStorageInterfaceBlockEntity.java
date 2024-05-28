package io.github.xiewuzhiying.vs_addition.mixin.create.PortableInterface;

import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.xiewuzhiying.vs_addition.compats.create.foundation.behaviour.IPSIBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PortableStorageInterfaceBlockEntity.class)
public abstract class MixinPortableStorageInterfaceBlockEntity extends SmartBlockEntity implements IPSIBehavior {
    @Shadow protected float distance;
    @Shadow public abstract void startConnecting();
    @Shadow abstract boolean isConnected();
    @Shadow protected abstract Integer getTransferTimeout();

    @Shadow @Final public static int ANIMATION;
    @Shadow public int keepAlive;
    @Shadow protected int transferTimer;
    @Shadow protected boolean powered;
    @Shadow protected LerpedFloat connectionAnimation;
    private boolean isPassive;
    private PortableStorageInterfaceBlockEntity connectedPI;


    public MixinPortableStorageInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.connectedPI = null;
        this.isPassive = false;
    }



    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    public void tick(CallbackInfo ci) {
        if(false) {
            ci.cancel();
            super.tick();
            boolean wasConnected = isConnected();
            int timeUnit = getTransferTimeout();
            int animation = ANIMATION;

            if (keepAlive > 0) {
                keepAlive--;
                if (keepAlive == 0 && !level.isClientSide) {
                    stopTransferring();
                    transferTimer = ANIMATION - 1;
                    sendData();
                    return;
                }
            }

            transferTimer = Math.min(transferTimer, ANIMATION * 2 + timeUnit);

            boolean timerCanDecrement = transferTimer > ANIMATION || transferTimer > 0 && keepAlive == 0
                    && (isVirtual() || !level.isClientSide || transferTimer != ANIMATION);

            if (timerCanDecrement && (!isVirtual() || transferTimer != ANIMATION)) {
                transferTimer--;
                if (transferTimer == ANIMATION - 1)
                    sendData();
                if (transferTimer <= 0 || powered)
                    stopTransferring();
            }

            boolean isConnected = isConnected();
            if (wasConnected != isConnected && !level.isClientSide)
                setChanged();

            float progress = 0;
            if (isConnected)
                progress = 1;
            else if (transferTimer >= timeUnit + animation)
                progress = Mth.lerp((transferTimer - timeUnit - animation) / (float) animation, 1, 0);
            else if (transferTimer < animation)
                progress = Mth.lerp(transferTimer / (float) animation, 0, 1);
            connectionAnimation.setValue(progress);
        }
    }




    @Override
    public void startTransferringTo(PortableStorageInterfaceBlockEntity pi, float distance) {
        if (connectedPI == pi)
            return;
        this.distance = Math.min(2, distance);
        connectedPI = pi;
        startConnecting();
        notifyUpdate();
    }

    @Override
    public void stopTransferring() {
        connectedPI = null;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Override
    public boolean canTransfer() {
        if (connectedPI != null)
            stopTransferring();
        return connectedPI != null && isConnected();
    }

    @Unique
    protected ScrollOptionBehaviour<IPSIBehavior.WorkigMode> workingMode;

    @Inject(
            method = "addBehaviours",
            at = @At("RETURN"),
            remap = false
    )
    public void behaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        this.workingMode = new ScrollOptionBehaviour<>(IPSIBehavior.WorkigMode.class, Lang.translateDirect("psi.working_mode"), (PortableStorageInterfaceBlockEntity)(Object) this, getMovementModeSlot());
        behaviours.add(this.workingMode);
    }

    private ValueBoxTransform getMovementModeSlot() {
        return new DirectionalExtenderScrollOptionSlot((state, d) -> {
            Direction.Axis axis = d.getAxis();
            Direction.Axis bearingAxis = state.getValue(PortableStorageInterfaceBlock.FACING)
                    .getAxis();
            return bearingAxis != axis;
        });
    }

    @Override
    public ScrollOptionBehaviour<IPSIBehavior.WorkigMode> getWorkingMode() {
        return workingMode;
    }
}
