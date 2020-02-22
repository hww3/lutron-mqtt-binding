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
package org.openhab.binding.lutronmqtt.handler;

import com.google.gson.Gson;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.mqtt.*;
import org.eclipse.smarthome.io.transport.mqtt.reconnect.PeriodicReconnectStrategy;
import org.openhab.binding.lutronmqtt.internal.LutronMQTTConfiguration;
import org.openhab.binding.lutronmqtt.model.LutronDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.CONFIG_TOKEN;
import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.PROPERTY_URL;
import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.PROPERTY_UUID;

/**
 * The {@link LutronMQTTHubHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author William Welliver - Initial contribution
 */
public class LutronMQTTHubHandler extends BaseBridgeHandler implements MqttMessageSubscriber, MqttConnectionObserver {
    private final Logger logger = LoggerFactory.getLogger(LutronMQTTHubHandler.class);

    private Collection<DeviceStatusListener> deviceStatusListeners = new HashSet<>();

    private List<LutronDevice> deviceList = Collections.EMPTY_LIST;

    @Nullable
    private LutronMQTTConfiguration config;
    private String token;
    private MqttBrokerConnection mqttClient;
    private Gson gson = new Gson();
    private Map<Integer, LutronDevice> devicesByLinkAddress = new HashMap<>();
    private ScheduledFuture<?> onlineTimeout;
    private ScheduledFuture<?> allItemsJob;

    public LutronMQTTHubHandler(Bridge thing) {
        super(thing);
        logger.warn("LutronMQTTHubHandler create.");
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        Configuration config = getThing().getConfiguration();
        Map<String, String> properties = getThing().getProperties();
        token = (String) config.get(CONFIG_TOKEN);

        final String broker = properties.get(PROPERTY_URL);


        String clientId = "openhab-lutron-mqtt-" + (System.currentTimeMillis()/1000);

            if(mqttClient == null || mqttClient.connectionState() == MqttConnectionState.DISCONNECTED) {
                logger.warn("Attempting to connect to MQTT Broker.");
                URI brokerUri = null;
                try {
                    brokerUri = new URI(broker);
                } catch (URISyntaxException e) {
                    logger.error("Lutron-MQTT broker url was invalid: " + broker);
                    goOffline(ThingStatusDetail.CONFIGURATION_ERROR, "Lutron-MQTT broker url was invalid: " + broker);
                    return;
                }

                MqttBrokerConnection brokerConnection = new MqttBrokerConnection(brokerUri.getHost(), brokerUri.getPort(), false, clientId);
                brokerConnection.addConnectionObserver(this);
                brokerConnection.setReconnectStrategy(new PeriodicReconnectStrategy());
                updateStatus(ThingStatus.OFFLINE, "Preparing to connect to " + brokerUri.toASCIIString());

                Boolean bool = null;
                try {
                    mqttClient = brokerConnection;
                    bool = brokerConnection.start().get();
                } catch (InterruptedException|ExecutionException e) {
                    logger.warn("MQTT startup failed.", e);
                }
                logger.warn("MQTT startup returned " + bool);
            }

            //  logger.error("Unable to connect to MQTT broker at " + broker, e);
          //  goOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Unable to connect to MQTT broker at broker");

    }

    protected void updateStatus(ThingStatus status, String statusDetail) {
        this.updateStatus(status, ThingStatusDetail.NONE, (String)statusDetail);
    }

    @Override
    public void dispose() {
        logger.info("Disposing of handler " + this);
        super.dispose();

            mqttClient.stop();
            cancelJobs();

            mqttClient = null;
    }

    private void cancelJobs() {
        if(allItemsJob != null && !allItemsJob.isCancelled())
            allItemsJob.cancel(true);
        if(onlineTimeout != null && !onlineTimeout.isCancelled())
            onlineTimeout.cancel(true);
    }

    private void setupSubscriptions() {
        try {
            mqttClient.unsubscribeAll().get();
        } catch (InterruptedException|ExecutionException e) {
            logger.warn("An error occurred while unsubscribing.", e);
        }
        logger.info("Subscribing.");
        for(String topic : new String[] {"lutron/status", "lutron/events", "lutron/remote"}) {
            try {
                logger.info("subscribing to " + topic);
                if(!mqttClient.subscribe(topic, this).get()) {
                    logger.error("subscribe failed.");
                    goOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Unable to subscribe to events");
                }
            } catch (InterruptedException|ExecutionException e) {
                logger.warn("An error occurred while subscribing.", e);
            }
        }
        logger.info("Subscribed.");
        cancelJobs();

        logger.info("Scheduling item request.");
        allItemsJob = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                requestAllItems();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void requestAllItems() {
        logger.warn("Requesting all items");
        byte[] b = new byte[0];
        try {
            b = ("{\"cmd\": \"GetDevices\", \"args\": {}}").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unable to encode JSON.", e);
        }
        try {
            Boolean res = mqttClient.publish("lutron/commands", (b), 0, false).get();
            logger.warn("Publish returned " + res);
        } catch(Exception e) {
          logger.warn("Exception while publishing.", e);
        }

        allItemsJob = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                requestAllItems();
            }
        }, 5, TimeUnit.MINUTES);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    protected void goOnline() {
        updateStatus(ThingStatus.ONLINE);
    }

    protected void goOffline(ThingStatusDetail detail, String reason) {
        updateStatus(ThingStatus.OFFLINE, detail, reason);
        if(onlineTimeout != null) onlineTimeout.cancel(true);
        if(allItemsJob != null) onlineTimeout.cancel(true);
    }

    public Collection<LutronDevice> getDevices() {
        return ImmutableList.copyOf(deviceList);
    }

    public void registerDeviceStatusListener(DeviceStatusListener lutronDeviceDiscoveryService) {
        deviceStatusListeners.add(lutronDeviceDiscoveryService);
    }

    public void unregisterDeviceStatusListener(DeviceStatusListener lutronDeviceDiscoveryService) {
        deviceStatusListeners.remove(lutronDeviceDiscoveryService);
    }

    @Override
    public void processMessage(String s, byte[] mqttMessage) {
        logger.warn("messageArrived: " + s + " " + mqttMessage.toString());
        switch(s) {
            case "lutron/status":
                handleStatusMessage(mqttMessage);
                break;
            case "lutron/remote":
                handleRemoteEvent(mqttMessage);
                break;
            case "lutron/events":
                handleGatewayEvent(mqttMessage);
                break;
        }
    }

    private void handleGatewayEvent(byte[] mqttMessage) {
        //logger.info("gateway event: " + new String(mqttMessage.getPayload()));
        Map <String,Object> msg = gson.fromJson(new String(mqttMessage), HashMap.class);

        if(msg.containsKey("cmd")) {
            String key = (String)msg.get("cmd");
            Object arg = msg.get("args");
            switch(key) {
                case "ListDevices":
                    int i = 0;
                    logger.warn("ListDevices Response: " + arg);
                    List<LutronDevice> devices = new ArrayList<>();
                    for(Map<String,Object> objectMap : (List<Map<String,Object>>)arg) {
                        getThing().getThings();
                        final LutronDevice device = new LutronDevice(objectMap);
                        logger.info("Device: " + device);
                        devices.add(device);
                        if(devicesByLinkAddress.containsKey(device.getLinkAddress())) {
                            // TODO copy properties to new device
                        }
                        devicesByLinkAddress.put(device.getLinkAddress(), device);
                        scheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                requestUpdateForDevice(device.getLinkAddress());
                            }
                        }, i++%3, TimeUnit.SECONDS);
                    }

                    this.deviceList = devices;

                    break;
                case "RuntimePropertyUpdate":
                    int linkAddress = ((Number)(((Map)arg).get("ObjectId"))).intValue();
                    LutronDevice device = getDeviceByLinkAddress(linkAddress);
                    if(device == null) {
                        logger.warn("Unable to find LutronDevice with linkAddress="  + linkAddress);
                    }
                    List<List<Object>> l = (List<List<Object>>)(((Map)arg).get("Properties"));
                    for(List property : l) {
                        int pnum = ((Number)(property.get(0))).intValue();
                        int pval = ((Number)(property.get(1))).intValue();
                        device.putProperty(pnum, pval);
                    }
                    informDeviceListeners(device);


                    break;
                default:
                    logger.warn("Received unknown message type " + key + " with args " + arg);
                    break;
            }
        }
    }

    protected void requestUpdateForDevice(int linkAddress) {
            byte[] b = ("{\"cmd\":\"RuntimePropertyQuery\", \"args\":{\"Params\":[[" + linkAddress + ",15,[1]]]}}").getBytes();
            mqttClient.publish("lutron/commands", b);
    }

    protected void informDeviceListeners(LutronDevice device) {
        logger.info("informDeviceListeners " + device);
        for (DeviceStatusListener s : deviceStatusListeners) {
            s.onDeviceStateChanged(device);
        }
    }

    public LutronDevice getDeviceByLinkAddress(int linkAddress) {
        logger.warn("looking for device with link address =" + linkAddress);
        return devicesByLinkAddress.get(linkAddress);
    }

    private void handleRemoteEvent(byte[] mqttMessage) {
        logger.info("remote event: " + new String(mqttMessage));

        Map <String,Object> msg = gson.fromJson(new String(mqttMessage), HashMap.class);
       // {"serial" : "C726CA", "action": "down", "button": "select"}
    }

    private void handleStatusMessage(byte[] mqttMessage) {
        logger.info("status message: " + new String(mqttMessage));
        Map <String,Object> msg = gson.fromJson(new String(mqttMessage), HashMap.class);
        if(msg.containsKey("state") && "running".equals(msg.get("state"))) {
            if(onlineTimeout != null)
                onlineTimeout.cancel(true);
            goOnline();
            onlineTimeout = scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    onlineTimeoutOccurred();
                }
            },120, TimeUnit.SECONDS);
        }
    }

    protected void onlineTimeoutOccurred() {
        goOffline(ThingStatusDetail.BRIDGE_OFFLINE, "Too long between status announcements.");
        try {
            mqttClient.stop().get();
            Thread.sleep(5000);
            initialize();
        } catch(Exception e) {
            logger.warn("An error occurred while restarting the service.", e);
        }
    }

    public void setDesiredState(int deviceId, Map<String, Object> lightState) {
        byte[] bytes = new byte[0];
        try {
            bytes = (gson.toJson(lightState)).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Error encoding JSON.", e);
        }
        // MqttMessage message = new MqttMessage(bytes);
        Boolean res = null;
        try {
            res = mqttClient.publish("lutron/commands", bytes, 0, false).get();
            if(res)
                logger.info("Sent message.");
            else logger.warn("Failed to send message: " + new String(bytes));
        } catch (InterruptedException|ExecutionException e) {
            logger.warn("Error sending message.", e);
        }
    }

    @Override
    public void connectionStateChanged(MqttConnectionState mqttConnectionState, @Nullable Throwable throwable) {
        if(mqttConnectionState == MqttConnectionState.CONNECTED) {
            logger.info("MQTT connection state changed to CONNECTED.");
            goOnline();
            logger.info("Online");
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    setupSubscriptions();
                    logger.info("Subscribed.");

                }
            },1, TimeUnit.SECONDS);
        } else if (mqttConnectionState == MqttConnectionState.CONNECTING) {
            goOffline(ThingStatusDetail.BRIDGE_OFFLINE, "MQTT Reconnecting");
        } else if (mqttConnectionState == MqttConnectionState.DISCONNECTED) {
            goOffline(ThingStatusDetail.BRIDGE_OFFLINE, "MQTT Disconnected");
            logger.warn("Lost connection to MQTT server.");
            cancelJobs();
        }
    }
}
