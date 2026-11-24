package com.djawad1.dont.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public class DontNet {
    public static final Identifier JUMPSCARE = new Identifier("dont", "jumpscare");
    public static final Identifier BLACKOUT = new Identifier("dont", "blackout");
    public static final Identifier GLITCH = new Identifier("dont", "glitch");
    public static final Identifier EYES = new Identifier("dont", "eyes");
    public static final Identifier CRASH = new Identifier("dont", "crash");

    public static void jumpscare(ServerPlayerEntity p, int ms) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeInt(ms);
        ServerPlayNetworking.send(p, JUMPSCARE, b);
    }

    public static void blackout(ServerPlayerEntity p, int fadeInMs, int holdMs, int fadeOutMs) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeInt(fadeInMs);
        b.writeInt(holdMs);
        b.writeInt(fadeOutMs);
        ServerPlayNetworking.send(p, BLACKOUT, b);
    }

    public static void glitch(ServerPlayerEntity p, int ms, float volume) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeInt(ms);
        b.writeFloat(volume);
        ServerPlayNetworking.send(p, GLITCH, b);
    }

    public static void eyes(ServerPlayerEntity p, List<Integer> ids, int ms) {
        PacketByteBuf b = PacketByteBufs.create();
        b.writeInt(ms);
        b.writeInt(ids.size());
        for (int id : ids) b.writeInt(id);
        ServerPlayNetworking.send(p, EYES, b);
    }

    public static void crash(ServerPlayerEntity p) {
        ServerPlayNetworking.send(p, CRASH, PacketByteBufs.create());
    }
}
