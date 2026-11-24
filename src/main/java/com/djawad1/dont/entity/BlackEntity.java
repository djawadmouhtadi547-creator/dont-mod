package com.djawad1.dont.entity;

import com.djawad1.dont.maze.MazeManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Custom-AI entity: stands and watches, or hunts. Never a normal mob. */
public class BlackEntity extends PathAwareEntity {
    public static final int WATCH = 0;
    public static final int HUNT = 1;

    private int mode = WATCH;
    private int life = -1;
    private double huntSpeed = 1.2;
    private boolean main = false;

    public BlackEntity(EntityType<? extends BlackEntity> type, World world) {
        super(type, world);
        this.setSilent(true);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    public void setMode(int mode) { this.mode = mode; }
    public void setLife(int life) { this.life = life; }
    public void setHuntSpeed(double s) { this.huntSpeed = s; }
    public void setMain(boolean m) { this.main = m; }
    public boolean isMain() { return main; }
    public int getMode() { return mode; }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;
        if (life > 0 && --life == 0) {
            this.discard();
            return;
        }
        PlayerEntity target = this.getWorld().getClosestPlayer(this, 64.0);
        if (!(target instanceof ServerPlayerEntity player)) return;

        if (mode == WATCH) {
            this.getNavigation().stop();
            this.lookAtEntity(player, 360.0f, 360.0f);
            this.setVelocity(0, this.getVelocity().y, 0);
        } else {
            if (this.age % 5 == 0) this.getNavigation().startMovingTo(player, huntSpeed);
            this.getLookControl().lookAt(player, 90.0f, 90.0f);
            if (this.squaredDistanceTo(player) < 2.6) {
                MazeManager.onCaught(this, player);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isOf(DamageTypes.OUT_OF_WORLD);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) { return false; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) { }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("DontMode", mode);
        nbt.putInt("DontLife", life);
        nbt.putDouble("DontSpeed", huntSpeed);
        nbt.putBoolean("DontMain", main);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        mode = nbt.getInt("DontMode");
        life = nbt.contains("DontLife") ? nbt.getInt("DontLife") : -1;
        huntSpeed = nbt.contains("DontSpeed") ? nbt.getDouble("DontSpeed") : 1.2;
        main = nbt.getBoolean("DontMain");
    }
}
