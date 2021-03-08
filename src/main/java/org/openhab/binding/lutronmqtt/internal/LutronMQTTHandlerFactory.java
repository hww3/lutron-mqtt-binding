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
package org.openhab.binding.lutronmqtt.internal;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.*;

import java.util.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants;
import org.openhab.binding.lutronmqtt.discovery.LutronMQTTDeviceDiscoveryService;
import org.openhab.binding.lutronmqtt.handler.*;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LutronMQTTHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author William Welliver - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.lutronmqtt", name = "lutronmqtt")
@NonNullByDefault
public class LutronMQTTHandlerFactory extends BaseThingHandlerFactory {

    private final static Logger logger = LoggerFactory.getLogger(LutronMQTTHandlerFactory.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = LutronMQTTBindingConstants.SUPPORTED_THING_TYPES_UIDS;
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return THING_TYPE_MQTTHUB.equals(thingTypeUID) || SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    @Nullable
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        logger.warn("Creating a handler for " + thing.getThingTypeUID() + ", " + thing.getLabel());

        if (thingTypeUID.equals(THING_TYPE_MQTTHUB)) {
            logger.warn("Creating hub handler");
            LutronMQTTHubHandler handler = new LutronMQTTHubHandler((Bridge) thing);
            registerDeviceDiscoveryService(handler);
            return handler;
        } else if (thingTypeUID.equals(THING_TYPE_REMOTE)) {
            LutronMQTTRemoteHandler handler = new LutronMQTTRemoteHandler(thing);
            return handler;
        } else if (thingTypeUID.equals(THING_TYPE_DIMMABLE_LIGHT)) {
            LutronMQTTDimmableLightHandler handler = new LutronMQTTDimmableLightHandler(thing);
            return handler;
        } else if (thingTypeUID.equals(THING_TYPE_LIGHT)) {
            LutronMQTTLightHandler handler = new LutronMQTTLightHandler(thing);
            return handler;
        } else if (thingTypeUID.equals(THING_TYPE_VARIABLE_FAN)) {
            LutronMQTTVariableFanHandler handler = new LutronMQTTVariableFanHandler(thing);
            return handler;
        } else if (thingTypeUID.equals(THING_TYPE_SHADE)) {
            LutronMQTTShadeHandler handler = new LutronMQTTShadeHandler(thing);
            return handler;
        }

        return null;
    }

    private synchronized void registerDeviceDiscoveryService(LutronMQTTHubHandler hubHandler) {
        LutronMQTTDeviceDiscoveryService discoveryService = new LutronMQTTDeviceDiscoveryService(hubHandler);
        discoveryService.activate();
        this.discoveryServiceRegs.put(hubHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof LutronMQTTHubHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                LutronMQTTDeviceDiscoveryService service = (LutronMQTTDeviceDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                service.deactivate();
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }
}
