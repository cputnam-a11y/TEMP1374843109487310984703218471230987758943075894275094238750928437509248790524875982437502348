package io.github.cputnama11y.reloadableevent.test;

import com.mojang.serialization.MapCodec;

public class Speaker implements Listener {
    public static final MapCodec<Speaker> CODEC = MapCodec.unit(Speaker::new);

    @Override
    public void listenTo(String boringStuff) {
        IO.println("Now Speaking: " + boringStuff);
    }

    @Override
    public MapCodec<? extends Listener> type() {
        return CODEC;
    }
}
