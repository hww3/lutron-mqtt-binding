package org.openhab.binding.lutronmqtt.model;

public class LutronDeviceState {
    protected Boolean powered;
    protected float level;

    public Boolean getPowered() {
        return powered;
    }

    public void setPowered(Boolean powered) {
        this.powered = powered;
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        this.level = level;
    }
}
