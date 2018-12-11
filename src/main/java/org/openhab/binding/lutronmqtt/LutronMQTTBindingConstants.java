/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lutronmqtt;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import java.util.Set;

/**
 * The {@link LutronMQTTBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author William Welliver - Initial contribution
 */
 @NonNullByDefault
public class LutronMQTTBindingConstants {

    private static final String BINDING_ID = "lutronmqtt";

    public final static ThingTypeUID THING_TYPE_MQTTHUB = new ThingTypeUID(BINDING_ID, "hub");
    public final static ThingTypeUID THING_TYPE_DIMMABLE_LIGHT = new ThingTypeUID(BINDING_ID, "dimmableLight");
    public final static ThingTypeUID THING_TYPE_VARIABLE_FAN = new ThingTypeUID(BINDING_ID, "variableFan");
    public final static ThingTypeUID THING_TYPE_REMOTE = new ThingTypeUID(BINDING_ID, "remote");

    public final static Set<ThingTypeUID> SUPPORTED_HUBS_UIDS = ImmutableSet.of(THING_TYPE_MQTTHUB);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_DIMMABLE_LIGHT,
             THING_TYPE_VARIABLE_FAN, THING_TYPE_REMOTE);

    // List of all Channel ids
    public final static String PROPERTY_UUID = "uuid";
    public final static String PROPERTY_URL = "url";

    public final static String PROPERTY_OBJECT_ID = "objectId";
    public final static String PROPERTY_OBJECT_NAME = "name";
    public final static String PROPERTY_INTEGRATION_ID = "integrationId";
    public final static String PROPERTY_LINK_ADDRESS = "linkAddress";

    public final static String CONFIG_TOKEN = "token";

    // List of all Channel ids
    public final static String CHANNEL_LIGHT_LEVEL = "lightlevel";
    public final static String CHANNEL_LIGHT_STATE = "lightstate";

    public final static String CHANNEL_LIGHT_COLORTEMPERATURE = "colorTemperature";
    public final static String CHANNEL_LIGHT_COLOR = "color";

    public final static String CHANNEL_FAN_SPEED = "fanSpeed";
    public final static String CHANNEL_POWER_SWITCH = "powerSwitch";

    public final static int LUTRON_PROPERTY_LEVEL = 1;
}
