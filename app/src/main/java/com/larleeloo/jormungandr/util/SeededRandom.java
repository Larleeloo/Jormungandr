package com.larleeloo.jormungandr.util;

import java.util.Random;

public class SeededRandom {
    private final Random random;

    public SeededRandom(long seed) {
        this.random = new Random(seed);
    }

    public static long hashSeed(int region, int roomNumber) {
        return Constants.WORLD_SEED ^ ((long) region << 32) ^ ((long) roomNumber * 2654435761L);
    }

    public static long hashSeed(String roomId) {
        int region = RoomIdHelper.getRegion(roomId);
        int number = RoomIdHelper.getRoomNumber(roomId);
        return hashSeed(region, number);
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public float nextFloat() {
        return random.nextFloat();
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public int nextIntRange(int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }

    public float nextFloatRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
