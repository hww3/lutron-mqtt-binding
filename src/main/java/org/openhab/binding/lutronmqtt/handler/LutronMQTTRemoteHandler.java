/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lutronmqtt.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lutronmqtt.model.LutronDevice;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CHANNEL_LIGHT_LEVEL;
import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CHANNEL_LIGHT_STATE;

/**
 * The {@link LutronMQTTRemoteHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author William Welliver - Initial contribution
 */
public class LutronMQTTRemoteHandler extends BaseThingHandler implements DeviceStatusListener {

    public LutronMQTTRemoteHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void onDeviceFound(LutronDevice d) {

    }

    @Override
    public void onDeviceRemoved(LutronDevice d) {

    }

    @Override
    public void onDeviceStateChanged(LutronDevice light) {

    }
}
