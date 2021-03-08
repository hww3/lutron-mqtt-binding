package org.openhab.binding.lutronmqtt.model;

import java.util.HashMap;
import java.util.Map;

public class LutronDevice {
    // int id;
    String name;
    // int integrationId;
    String description;
    int objectId;
    // int linkAddress;
    int serialNumber;
    int deviceClass;

    Map<Integer, Integer> properties = new HashMap<>();
    private long lastUpdated;

    public LutronDevice(Map<String, Object> d) {
        setSerialNumber(Integer.parseInt((String) d.get("SerialNumber")));
        setDeviceClass(Integer.parseInt((String) d.get("DeviceClass")));
        setDescription((String) d.get("Description"));
        setObjectId(Integer.parseInt((String) d.get("ObjectId")));
        // setLinkAddress(Integer.parseInt((String) d.get("LinkAddress")));
        setName((String) d.get("Name"));
        // setId(Integer.parseInt((String) d.get("DeviceID")));
        // setIntegrationId(Integer.parseInt((String) d.get("IntegrationID")));
    }

    // public int getId() {
    // return id;
    // }
    //
    // public void setId(int id) {
    // this.id = id;
    // }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    // public int getLinkAddress() {
    // return linkAddress;
    // }
    //
    // public void setLinkAddress(int linkAddress) {
    // this.linkAddress = linkAddress;
    // }

    public String getName() {
        return name;
    }

    // public int getIntegrationId() {
    // return integrationId;
    // }
    //
    // public void setIntegrationId(int integrationId) {
    // this.integrationId = integrationId;
    // }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(int deviceClass) {
        this.deviceClass = deviceClass;
    }

    public int getProperty(int property) {
        return properties.get(property);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LutronDevice that = (LutronDevice) o;

        if (objectId != that.objectId)
            return false;
        // if (integrationId != that.integrationId)
        // return false;
        if (serialNumber != that.serialNumber)
            return false;
        if (deviceClass != that.deviceClass)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = objectId;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        // result = 31 * result + integrationId;
        // result = 31 * result + objectId;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + serialNumber;
        result = 31 * result + deviceClass;
        return result;
    }

    @Override
    public String toString() {
        return "LutronDevice{" + "objectId=" + objectId + ", name='" + name + '\'' + ", description='" + description
                + '\'' + ", serialNumber=" + serialNumber + ", deviceClass=" + deviceClass + '}';
    }

    public boolean hasUpdatedProperties() {
        return !properties.isEmpty();
    }

    public void putProperty(int pnum, int pval) {
        lastUpdated = System.currentTimeMillis();
        properties.put(pnum, pval);
    }
}
