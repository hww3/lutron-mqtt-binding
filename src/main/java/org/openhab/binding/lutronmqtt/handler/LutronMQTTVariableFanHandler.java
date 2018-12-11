/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lutronmqtt.handler;

import org.eclipse.smarthome.core.thing.Thing;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CHANNEL_FAN_SPEED;
import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CHANNEL_POWER_SWITCH;

/**
 * The {@link LutronMQTTVariableFanHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author William Welliver - Initial contribution
 */
public class LutronMQTTVariableFanHandler extends PowerLevelDeviceHandler {

    public LutronMQTTVariableFanHandler(Thing thing) {
        super(thing, CHANNEL_FAN_SPEED, CHANNEL_POWER_SWITCH);
    }
}
