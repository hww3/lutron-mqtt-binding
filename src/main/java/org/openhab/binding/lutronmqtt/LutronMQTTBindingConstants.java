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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

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
    public final static ThingTypeUID THING_TYPE_LIGHT = new ThingTypeUID(BINDING_ID, "light");
    public final static ThingTypeUID THING_TYPE_DIMMABLE_LIGHT = new ThingTypeUID(BINDING_ID, "dimmableLight");
    public final static ThingTypeUID THING_TYPE_VARIABLE_FAN = new ThingTypeUID(BINDING_ID, "variableFan");
    public final static ThingTypeUID THING_TYPE_REMOTE = new ThingTypeUID(BINDING_ID, "remote");
    public final static ThingTypeUID THING_TYPE_SHADE = new ThingTypeUID(BINDING_ID, "shade");

    public final static Set<ThingTypeUID> SUPPORTED_HUBS_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_MQTTHUB).collect(Collectors.toSet()));

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_DIMMABLE_LIGHT, THING_TYPE_LIGHT, THING_TYPE_SHADE,
                    THING_TYPE_VARIABLE_FAN, THING_TYPE_REMOTE).collect(Collectors.toSet()));

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
    public final static String CHANNEL_LIGHT_STATE = "state";

    public final static String CHANNEL_LIGHT_COLORTEMPERATURE = "colorTemperature";
    public final static String CHANNEL_LIGHT_COLOR = "color";

    public final static String CHANNEL_FAN_SPEED = "fanSpeed";
    public final static String CHANNEL_POWER_SWITCH = "powerSwitch";

    public final static String CHANNEL_SHADE_LEVEL = "shadeLevel";

    public final static int LUTRON_PROPERTY_LEVEL = 1;

    static final public Set<Integer> lightDeviceClasses = new HashSet<>();
    static {
        lightDeviceClasses.add(68223489); // MRF2-6ANS
        lightDeviceClasses.add(70451457); // Caseta Wall Switch
    }

    static final public Set<Integer> dimmableLightDeviceClasses = new HashSet<>();
    static {
        dimmableLightDeviceClasses.add(70713601); // GE Bulb
        dimmableLightDeviceClasses.add(70385921); // Caseta Wall Dimmer
        dimmableLightDeviceClasses.add(70516993); // Caseta Plug-In Dimmer
        dimmableLightDeviceClasses.add(70582529); // RRD-6CL
    }

    static final public Set<Integer> variableFanDeviceClasses = new HashSet<>();
    static {
        variableFanDeviceClasses.add(67567873); // RRD-2ANF
    }

    static final public Set<Integer> shadeDeviceClasses = new HashSet<>();
    static {
        shadeDeviceClasses.add(50725121); // Serena Honeycomb
        shadeDeviceClasses.add(50921729); // Serena Roller
        shadeDeviceClasses.add(50987265); // Sivoia QS Wireless Triathlon Roller
        shadeDeviceClasses.add(50594049); // RF QS Roller Shade
    }

    public final static Set<Integer> remoteDeviceClasses = new HashSet<>();
    static {
        remoteDeviceClasses.add(17235974); // 3 Button Pico with Raise/Lower
        remoteDeviceClasses.add(17235973); // 3 Button Pico
        remoteDeviceClasses.add(17235972); // 2 Button Pico with Raise/Lower
        remoteDeviceClasses.add(17235971); // 2 Button Pico
        remoteDeviceClasses.add(17235970); // 1 Button Pico
    }
}
