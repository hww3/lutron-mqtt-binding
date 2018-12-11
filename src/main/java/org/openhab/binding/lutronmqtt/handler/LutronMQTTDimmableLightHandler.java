/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lutronmqtt.handler;

import org.eclipse.smarthome.core.thing.Thing;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CHANNEL_LIGHT_LEVEL;
import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CHANNEL_LIGHT_STATE;

/**
 * The {@link LutronMQTTDimmableLightHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author William Welliver - Initial contribution
 */
public class LutronMQTTDimmableLightHandler extends PowerLevelDeviceHandler {

    public LutronMQTTDimmableLightHandler(Thing thing) {
        super(thing, CHANNEL_LIGHT_LEVEL, CHANNEL_LIGHT_STATE);
    }

}
