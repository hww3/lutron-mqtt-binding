package org.openhab.binding.lutronmqtt.handler;

import org.openhab.binding.lutronmqtt.model.LutronDevice;

public interface DeviceStatusListener {

    public void onDeviceFound(LutronDevice d);

    public void onDeviceRemoved(LutronDevice d);

    public void onDeviceStateChanged(LutronDevice light);
}
