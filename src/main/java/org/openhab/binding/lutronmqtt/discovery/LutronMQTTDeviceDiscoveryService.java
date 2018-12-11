package org.openhab.binding.lutronmqtt.discovery;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants;
import org.openhab.binding.lutronmqtt.handler.DeviceStatusListener;
import org.openhab.binding.lutronmqtt.handler.LutronMQTTHubHandler;
import org.openhab.binding.lutronmqtt.model.LutronDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.lutronmqtt.internal.LutronMQTTHandlerFactory.SUPPORTED_THING_TYPES_UIDS;

public class LutronMQTTDeviceDiscoveryService extends AbstractDiscoveryService implements DeviceStatusListener {
    private final static int SEARCH_TIME = 60;
    private final Logger logger = LoggerFactory.getLogger(LutronMQTTDeviceDiscoveryService.class);

    private LutronMQTTHubHandler hubHandler;

    public LutronMQTTDeviceDiscoveryService(LutronMQTTHubHandler hubHandler) {
        super(SEARCH_TIME);
        this.hubHandler = hubHandler;
    }

    public void activate() {
        logger.warn("activate");
        hubHandler.registerDeviceStatusListener(this);
        startScan();
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
        hubHandler.unregisterDeviceStatusListener(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                doScan();
            }
        }, 5, TimeUnit.SECONDS);
    }

    protected void doScan() {
        logger.warn("starting scan");
        Collection<LutronDevice> devices = hubHandler.getDevices();

        for (LutronDevice d : devices) {
            onDeviceFound(d);
        }
    }

    @Override
    public void onDeviceFound(LutronDevice d) {
        ThingTypeUID thingTypeUID = getThingTypeUID(d);
        if (thingTypeUID == null) {
            return;
        }

        ThingUID thingUID = getThingUID(d);

        if (thingUID != null) {
            if (hubHandler.getThingByUID(thingUID) != null) {
                logger.debug("ignoring already registered device of name '{}' with id {}", d.getName(), d.getId());
            }
            ThingUID bridgeUID = hubHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(LutronMQTTBindingConstants.PROPERTY_OBJECT_ID, "" + d.getId());
            properties.put(LutronMQTTBindingConstants.PROPERTY_INTEGRATION_ID, "" + d.getIntegrationId());
            properties.put(LutronMQTTBindingConstants.PROPERTY_LINK_ADDRESS, "" + d.getLinkAddress());
            properties.put(LutronMQTTBindingConstants.PROPERTY_OBJECT_NAME, d.getName());

            logger.warn("discovery result " + d.getName() + " " + d.getIntegrationId());

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withBridge(bridgeUID).withLabel(d.getName()).build();

            thingDiscovered(discoveryResult);
        } else {
            logger.debug("discovered unsupported device of name '{}' with id {}", d.getName(), d.getId());
        }

    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    private ThingUID getThingUID(LutronDevice device) {
        ThingUID bridgeUID = hubHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = getThingTypeUID(device);

        if (thingTypeUID != null && getSupportedThingTypes().contains(thingTypeUID)) {
            return new ThingUID(thingTypeUID, bridgeUID, "" + device.getId());
        } else {
            return null;
        }
    }

    private ThingTypeUID getThingTypeUID(LutronDevice device) {
        ThingTypeUID thingTypeId = null;
        String name = device.getName();
            if (name.endsWith("Remote")) {
                thingTypeId = LutronMQTTBindingConstants.THING_TYPE_REMOTE;
            } else if (name.contains("Fan")) {
                thingTypeId = LutronMQTTBindingConstants.THING_TYPE_VARIABLE_FAN;
            } else {
                thingTypeId = LutronMQTTBindingConstants.THING_TYPE_DIMMABLE_LIGHT;
            }
        return thingTypeId;
    }

    @Override
    public void onDeviceRemoved(LutronDevice d) {
        // TODO Remove devices

    }

    @Override
    public void onDeviceStateChanged(LutronDevice light) {
        // Do nothing
    }
}
