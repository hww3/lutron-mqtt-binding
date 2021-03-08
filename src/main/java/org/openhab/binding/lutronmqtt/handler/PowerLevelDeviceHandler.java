/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lutronmqtt.handler;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.*;

import java.util.Map;

import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lutronmqtt.model.LutronDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link PowerLevelDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author William Welliver - Initial contribution
 */
public class PowerLevelDeviceHandler extends BaseThingHandler implements DeviceStatusListener {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected int objectId;
    // protected int deviceId; //
    // protected int integrationId;
    protected LutronDevice device; // last update received for this device.
    // protected int linkAddress;

    boolean setAsIs = false;
    protected LutronMQTTHubHandler hubHandler;

    protected final String powerLevelChannelName;
    // protected final String powerSwitchChannelName;

    public PowerLevelDeviceHandler(Thing thing, String powerLevelChannel, String powerSwitchChannel) {
        super(thing);
        this.powerLevelChannelName = powerLevelChannel;
        // this.powerSwitchChannelName = powerSwitchChannel;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Map<String, Object> lightState = null;
        String ch = channelUID.getId();
        log.warn("Got a command for channel id=" + ch + ", command=" + command);

        log.info("command= " + command + ", last device reading=" + getDevice().getProperty(LUTRON_PROPERTY_LEVEL));

        if (log.isTraceEnabled()) {
            log.trace(
                    "command= " + command + ", last device reading=" + getDevice().getProperty(LUTRON_PROPERTY_LEVEL));
        }
        if (powerLevelChannelName.equals(ch)) {
            if (command instanceof PercentType) {
                lightState = LightStateConverter.toLightState((PercentType) command, getDevice());
            } else if (command instanceof OnOffType) {
                lightState = LightStateConverter.toLightState((OnOffType) command, getDevice());
            } else if (command instanceof IncreaseDecreaseType) {
                lightState = LightStateConverter.toLightState((IncreaseDecreaseType) command, getDevice());
            } else if (command instanceof UpDownType) {
                lightState = LightStateConverter.toLightState((UpDownType) command, getDevice());
            } else if (command == StopMoveType.STOP) {
                setAsIs = true;
                log.warn("STOPPING");
                scheduleUpdateForDevice(objectId);
                // lightState = LightStateConverter.toLightState((Sto) command, getDevice());
            }
        }

        /*
         * else if (powerSwitchChannelName.equals(ch)) {
         * if (command instanceof OnOffType) {
         * lightState = LightStateConverter.toOnOffLightState((OnOffType) command, getDevice());
         * }
         * }
         */
        Gson gson = new Gson();
        log.info("converted " + command + " to " + gson.toJson(lightState));

        if (lightState != null) {
            if (log.isTraceEnabled()) {
                // Gson gson = new Gson();
                log.trace("converted " + command + " to " + gson.toJson(lightState));
            }
            updateDeviceState(lightState);
        } else {
            log.warn("Got a command for an unhandled channel: " + ch);
        }
    }

    protected void updateDeviceState(Map<String, Object> lightState) {
        log.warn("updateDeviceState: " + lightState);
        getHubHandler().setDesiredState(lightState);
    }

    @Override
    public void initialize() {
        log.debug("Initializing power level device handler.");
        initializeThing((getBridge() == null) ? null : getBridge().getStatus());
    }

    private void initializeThing(ThingStatus bridgeStatus) {
        log.debug("initializeThing thing {} bridge status {}", getThing().getUID(), bridgeStatus);
        final Integer _objectId = Integer.valueOf(getThing().getProperties().get(PROPERTY_OBJECT_ID));
        log.warn("intializeThing " + _objectId);

        if (_objectId != null) {
            this.objectId = _objectId;
            if (getHubHandler() != null) {
                if (bridgeStatus == ThingStatus.ONLINE) {
                    getHubHandler().requestUpdateForDevice(objectId);
                    LutronDevice device = getHubHandler().getDeviceByObjectId(objectId);
                    if (device == null) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                        return;
                    }
                    updateStatus(ThingStatus.ONLINE);

                    // receiving a response to the request update method above should trigger a state change.
                    // onDeviceStateChanged(getHubHandler().getDeviceByIntegrationId(integrationId));
                    // initializeProperties();
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    @Override
    public void dispose() {
        log.debug("Handler disposed. Unregistering listener.");
        if (objectId != 0) { // technically 0 is a valid device id but it appears to be reserved for the hub
            LutronMQTTHubHandler hubHandler = getHubHandler();
            if (hubHandler != null) {
                hubHandler.unregisterDeviceStatusListener(this);
                this.hubHandler = null;
            }
            objectId = 0;
        }
    }

    protected synchronized LutronMQTTHubHandler getHubHandler() {
        if (this.hubHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof LutronMQTTHubHandler) {
                this.hubHandler = (LutronMQTTHubHandler) handler;
                this.hubHandler.registerDeviceStatusListener(this);
            } else {
                return null;
            }
        }
        return this.hubHandler;
    }

    protected void scheduleUpdateForDevice(int objectId) {

        log.info("Scheduling an update request for deviceId=" + objectId);
        scheduler.submit(new Runnable() {
            @Override
            public void run() {
                LutronMQTTHubHandler handler = getHubHandler();
                if (handler != null) {
                    onDeviceStateChanged(handler.getDeviceByObjectId(objectId));
                }
            }
        });
    }

    public LutronDevice getDevice() {
        if (device != null) {
            return device;
        }

        LutronMQTTHubHandler handler = getHubHandler();
        device = handler.getDeviceByObjectId(objectId);
        return device;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatus) {
        super.bridgeStatusChanged(bridgeStatus);
        if (bridgeStatus.getStatus() == ThingStatus.ONLINE) {
            scheduleUpdateForDevice(objectId);
        }
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        if (this.getBridge().getStatus() == ThingStatus.ONLINE) {
            // TODO really only need 1 for each device, no matter the channelse.
            scheduleUpdateForDevice(objectId);
        } else {
            log.info("Channel Linked but hub is not online.");
        }
    }

    @Override
    public void onDeviceFound(LutronDevice d) {
        if (d.getObjectId() == objectId) {
            updateStatus(ThingStatus.ONLINE);
            onDeviceStateChanged(d);
        }
    }

    @Override
    public void onDeviceRemoved(LutronDevice d) {
        if (d.getObjectId() == objectId) {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onDeviceStateChanged(LutronDevice d) {
        log.trace("onDeviceStateChanged my id=" + objectId + ", update is for " + d.getObjectId() + ", name="
                + d.getName());
        if (d.getObjectId() != objectId) {
            return;
        }

        if (setAsIs) {
            log.warn("AS IS: " + d.getProperty(LUTRON_PROPERTY_LEVEL));
            setAsIs = false;
        }

        log.debug("Go device status change for " + this.getThing().getLabel());

        if (d.hasUpdatedProperties()) {
            log.info("Received notice of pending state change.");
        }

        if (false && device != null
                && d.getProperty(LUTRON_PROPERTY_LEVEL) == (device.getProperty(LUTRON_PROPERTY_LEVEL))) {
            log.info("Lutron Device: " + d.getName() + " Received State Changed but no difference");
            return;
        }

        device = d;

        log.info("Lutron Device: " + d.getName() + " State Changed: " + d.getProperty(LUTRON_PROPERTY_LEVEL));

        // TODO we should keep the previous state so that we don't send unnecessary updates.

        PercentType percentType = LightStateConverter.toBrightnessPercentType(d);

        log.info("Lutron: " + d.getName() + " Light Level: " + percentType.intValue());
        updateState(powerLevelChannelName, percentType);
    }

    protected Integer getCurrentLevel(LutronDevice light) {
        int brightness = light.getProperty(LUTRON_PROPERTY_LEVEL);

        return brightness;
    }
}
