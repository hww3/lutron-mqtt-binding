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

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lutronmqtt.internal.LutronMQTTConfiguration;
import org.openhab.binding.lutronmqtt.model.LutronDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
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
public class LutronMQTTHubHandler extends BaseBridgeHandler implements MqttCallback {

    private final Logger logger = LoggerFactory.getLogger(LutronMQTTHubHandler.class);

    private Collection<DeviceStatusListener> deviceStatusListeners = new HashSet<>();

    private List<LutronDevice> deviceList = Collections.EMPTY_LIST;

    @Nullable
    private LutronMQTTConfiguration config;
    private String token;
    private MqttClient mqttClient;
    private Gson gson = new Gson();
    private Map<Integer, LutronDevice> devicesByLinkAddress = new HashMap<>();
    private ScheduledFuture<?> reconnectJob;
    private ScheduledFuture<?> onlineTimeout;
    private ScheduledFuture<?> allItemsJob;

    public LutronMQTTHubHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        Configuration config = getThing().getConfiguration();
        Map<String, String> properties = getThing().getProperties();
        token = (String) config.get(CONFIG_TOKEN);

        final String uuid = properties.get(PROPERTY_UUID);
        final String broker = properties.get(PROPERTY_URL);

        updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE);

        MemoryPersistence persistence = new MemoryPersistence();
        String clientId = "openhab-lutron-mqtt-" + (System.currentTimeMillis()/1000);

        try {
            if(mqttClient == null || !mqttClient.isConnected()) {
                logger.info("Attempting to connect to MQTT Broker.");
                MqttClient client = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
//                connOpts.setCleanSession(true);
//                connOpts.setKeepAliveInterval(30);
//                connOpts.setConnectionTimeout(90);
                client.connect(connOpts);
                mqttClient = client;
                setupSubscriptions();
            }
        } catch (MqttException e) {
            logger.error("Unable to connect to MQTT broker at " + broker, e);
            goOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Unable to connect to MQTT broker at broker");
        }
    }



    @Override
    public void dispose() {
        logger.info("Disposing of handler " + this);
        super.dispose();
        try {
            MqttClient cl = mqttClient;
            cl.setCallback(null);
            cl.unsubscribe(new String[] {"lutron/status", "lutron/events", "lutron/remote"});
            cl.disconnectForcibly(3000);
            cl.close();
            mqttClient = null;
            cancelJobs();

        } catch (MqttException e) {
            logger.warn("Error while disconnecting", e);
        }
    }

    private void cancelJobs() {
        if(reconnectJob != null && !reconnectJob.isCancelled())
            reconnectJob.cancel(true);
        if(allItemsJob != null && !allItemsJob.isCancelled())
            allItemsJob.cancel(true);
        if(onlineTimeout != null && !allItemsJob.isCancelled())
            onlineTimeout.cancel(true);
    }

    private void setupSubscriptions() {
        mqttClient.setCallback(this);
        try {
            mqttClient.subscribe(new String[] {"lutron/status", "lutron/events", "lutron/remote"});
        } catch (MqttException e) {
            logger.error("subscribe failed.", e);
            goOffline(ThingStatusDetail.COMMUNICATION_ERROR, "Unable to subscribe to events");
        }

        cancelJobs();

        allItemsJob = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                requestAllItems();
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void requestAllItems() {
        try {
            byte[] b = ("{\"cmd\": \"GetDevices\", \"args\": {}}").getBytes("UTF-8");
            mqttClient.publish("lutron/commands", (b), 0, false);
        } catch (MqttException e) {
            logger.warn("Error Sending message.", e);
        } catch (UnsupportedEncodingException e) {
            logger.warn("Error encoding message.", e);
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
    public void connectionLost(Throwable throwable) {
        goOffline(ThingStatusDetail.BRIDGE_OFFLINE, throwable.getMessage());
        logger.warn("Lost connection to MQTT server. Will rety in 20 seconds.", throwable);
        if(mqttClient != null) scheduleReconnect(20);
    }

    private void scheduleReconnect(int seconds) {
       if(reconnectJob == null || reconnectJob.isDone()) {
           reconnectJob = scheduler.schedule(new Runnable() {
               @Override
               public void run() {
                   reconnect();
               }
           }, seconds, TimeUnit.SECONDS);
       }
    }

    private void reconnect() {
        if(mqttClient == null) {
            logger.info("Skipping reconnect because MQTT client is null.");
            return;
        }
        logger.info("Attempting to reconnect to MQTT server");
            initialize();
        ThingStatusInfo info = getThing().getStatusInfo();
        if(info.getStatus() != ThingStatus.ONLINE &&
                (info.getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE ||
                        info.getStatusDetail() == ThingStatusDetail.COMMUNICATION_ERROR
                )) // we're not online but have a retryable status
            scheduleReconnect(20);
        else {
            reconnectJob = null;
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        logger.debug("messageArrived: " + s + " " + mqttMessage.toString());
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

    private void handleGatewayEvent(MqttMessage mqttMessage) {
        //logger.info("gateway event: " + new String(mqttMessage.getPayload()));
        Map <String,Object> msg = gson.fromJson(new String(mqttMessage.getPayload()), HashMap.class);

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
        try {
            byte[] b = ("{\"cmd\":\"RuntimePropertyQuery\", \"args\":{\"Params\":[[" + linkAddress + ",15,[1]]]}}").getBytes();
            MqttMessage message = new MqttMessage(b);
            mqttClient.publish("lutron/commands", message);
        } catch (MqttException e) {
            logger.error("Error publishing message", e);
        }
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

    private void handleRemoteEvent(MqttMessage mqttMessage) {
        logger.info("remote event: " + new String(mqttMessage.getPayload()));

        Map <String,Object> msg = gson.fromJson(new String(mqttMessage.getPayload()), HashMap.class);
       // {"serial" : "C726CA", "action": "down", "button": "select"}

    }

    private void handleStatusMessage(MqttMessage mqttMessage) {
        logger.info("status message: " + new String(mqttMessage.getPayload()));
        Map <String,Object> msg = gson.fromJson(new String(mqttMessage.getPayload()), HashMap.class);
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
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        // We really don't care too much about this status.
    }

    public void setDesiredState(int deviceId, Map<String, Object> lightState) {
        try {
            byte[] bytes = (gson.toJson(lightState)).getBytes("UTF-8");
           // MqttMessage message = new MqttMessage(bytes);
            mqttClient.publish("lutron/commands", bytes, 0, false);
            logger.info("Sent message.");
        } catch (MqttException|UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
