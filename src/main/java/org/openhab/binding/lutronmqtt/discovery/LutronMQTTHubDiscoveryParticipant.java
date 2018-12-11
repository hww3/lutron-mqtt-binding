package org.openhab.binding.lutronmqtt.discovery;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

@Component(service = MDNSDiscoveryParticipant.class, immediate = true, configurationPid = "binding.lutronmqtt", name = "org.openhab.binding.lutronmqtt.discovery.hub")
public class LutronMQTTHubDiscoveryParticipant implements MDNSDiscoveryParticipant, ExtendedDiscoveryService {
    private Logger logger = LoggerFactory.getLogger(LutronMQTTHubDiscoveryParticipant.class);
    private DiscoveryServiceCallback discoveryServiceCallback;

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(LutronMQTTBindingConstants.THING_TYPE_MQTTHUB);
    }

    @Override
    public String getServiceType() {
        return "_lutron_mqtt._tcp.local.";
    }

    @Override
    public DiscoveryResult createResult(ServiceInfo service) {
logger.warn("Discovery result: " + service.toString());
            ThingUID uid = getThingUID(service);

            logger.warn("text: " + service.getTextString());
            logger.warn("server: " + service.getServer());
            logger.warn("name: " + service.getName());
            logger.warn("qname: " + service.getQualifiedName());
            logger.warn("nice: " + service.getNiceTextString());
            Enumeration<String> pn = service.getPropertyNames();
            while(pn.hasMoreElements())
                logger.warn("prop: " + pn.nextElement());

            logger.debug("Got discovered device.");
            if (true /*getDiscoveryServiceCallback() != null*/) {
              /*  logger.debug("Looking to see if this thing exists already.");
                Thing thing = getDiscoveryServiceCallback().getExistingThing(uid);
                if (thing != null) {
                    logger.debug("Already have thing with ID=<" + uid + ">");
                    return null;
                } else {
                    logger.debug("Nope. This should trigger a new inbox entry.");
                }
            } else {
                logger.warn("DiscoveryServiceCallback not set. This shouldn't happen!");
                return null;
            }

*/
                if (uid != null) {
                    Map<String, Object> properties = new HashMap<>(2);
                    String u = service.getPropertyString("uuid");
                    String s = service.getServer();

                    if(u == null || s == null || u.isEmpty() || s.isEmpty()) // incomplete discovery result
                    {
                        logger.warn("Found lutron-mqtt service but missing data. Try restarting device.");
                        return null;
                    }

                        properties.put(LutronMQTTBindingConstants.PROPERTY_URL, "tcp://" + s +
                                ":" + service.getPort());
                    properties.put(LutronMQTTBindingConstants.PROPERTY_UUID, u);

                    return DiscoveryResultBuilder.create(uid).withProperties(properties)
                            .withRepresentationProperty(uid.getId()).withLabel(service.getName() + " Lutron-MQTT Gateway").build();
                }
            }
        return null;

    }

    @Override
    public ThingUID getThingUID(ServiceInfo service) {

        if (service != null) {
            if (service.getType() != null) {
                if (service.getType().equals(getServiceType())) {
                    logger.trace("Discovered a Lutron-MQTT gateway thing with name '{}'", service.getName());
                    return new ThingUID(LutronMQTTBindingConstants.THING_TYPE_MQTTHUB, service.getName().replace(" ", "_"));
                }
            }
        }

        return null;
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        logger.warn(discoveryServiceCallback.toString());
        this.discoveryServiceCallback = discoveryServiceCallback;
        // log.debug(discoveryServiceCallback.toString());
    }

    public DiscoveryServiceCallback getDiscoveryServiceCallback() {
        return discoveryServiceCallback;
    }
}
