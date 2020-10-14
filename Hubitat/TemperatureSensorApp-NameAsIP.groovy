
/**
*  TemperatureSensorApp
*
*  Author: Victor Santana
*  Date: 2018-02-18
*/

definition(
    name: "Temperature Sensor SmartApp",
    namespace: "Temperature Sensor",
    author: "Victor Santana",
    description: "Temperature Sensor",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=3x",
    singleInstance: true
)

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

preferences {
    page(name: "page1")
    page(name: "addChildDevicePage")
}


def page1() {
    dynamicPage(name: "page1", install: true, uninstall: true) {
        section("Enable Debug Log at SmartThing IDE"){
            input "idelog", "bool", title: "Select True or False:", defaultValue: false, required: false
        }
        section("Add Temperature Sensor") {
            href (name: "addChildDevicePage", 
                title: "Add Temperature Sensor title", 
                description: "Add Temperature Sensor description",
                image: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
                required: false,
                page: "addChildDevicePage"
            )
        }
    } 
}

def addChildDevicePage(){
    app.updateSetting("deviceName",[type: "text", value: ""])
    // app.updateSetting("deviceID",[type: "text", value: ""])
    app.updateSetting("deviceIP",[type: "text", value: ""])
    app.updateSetting("devicePort",[type: "text", value: ""])
    dynamicPage(name: "addChildDevice", install: true, uninstall: true) {
        section("Add Temperature Sensor") {
            input "deviceName", "text", title: "DeviceName", description: "Type a unique name for the device", required: true
            //input "deviceID", "text", title: "DeviceID", description: "Type a unique DeviceID for Temperature Sensor", required: true
            input "deviceIP", "text", title: "DeviceIP", description: "Type device IP Address", required: true
            input "devicePort", "text", title: "devicePort", description: "Type device TCP Port", required: true            
        }     
    }  
}

def installed() {
    writeLog("TemperatureSensorSmartApp - TemperatureSensor Installed with settings: ${settings}")
    initialize()
    addTemperatureSensorDeviceType()
}

def updated() {
    writeLog("TemperatureSensorSmartApp - Updated with settings: ${settings}")
    //unsubscribe()
    initialize()
    def endpoint = settings.proxyAddress + ":" + settings.proxyPort
    //sendCommand('/subscribe/'+getNotifyAddress(),endpoint)
    addTemperatureSensorDeviceType()
}

def initialize() {
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
    writeLog("TemperatureSensorSmartApp - Initialize")
}

def uninstalled() {
    removeChildDevices()
}

private removeChildDevices() {
    getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
    writeLog("TemperatureSensorSmartApp - Removing all child devices")
}

def getIpFromHex(hexAddrString) {
    if (hexAddrString.length() % 2 != 0) {
        hexAddrString = "0" + hexAddrString;
    }
    if (hexAddrString.length() != 8) {
        //error..
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hexAddrString.length(); i = i + 2) {
        final String part = hexAddrString.substring(i, i + 2);
        final int ipPart = Integer.parseInt(part, 16);
        if (ipPart < 0 || ipPart > 254) {
            //Error...
        }
        sb.append(ipPart);
        if (i + 2 < hexAddrString.length()) {
            sb.append(".");
        }
    }
    return sb.toString();
}

def lanResponseHandler(evt) {
    def map = parseLanMessage(evt)
    writeLog("EVT: ${evt}")
    
    //String hex = '192.168.0.252'.tokenize( '.' )*.toInteger().asType( byte[] ).encodeHex()
    //log.debug "IP 192.168.0.252 is: ${hex.toString().toUpperCase()}"    
    
    //def IP = getIpFromHex("C0A800FC")
    //log.debug "IP C0A800FC is: ${IP}"    
    
    def headers = map.headers;
    def body = map.data;    

    if (headers.'deviceType' != 'temperaturesensor') {
        writeLog("TemperatureSensorSmartApp - Received event ${evt.stringValue} but it didn't came from TemperatureSensor")
        writeLog("TemperatureSensorSmartApp - Received event but it didn't came from TemperatureSensor headers:  ${headers}")
        writeLog("TemperatureSensorSmartApp - Received event but it didn't came from TemperatureSensor body: ${body}")      
        return
    }

    writeLog("TemperatureSensorSmartApp - Received event headers:  ${headers}")
    writeLog("TemperatureSensorSmartApp - Received event body: ${body}")
    updateTemperatureSensorceDeviceType(body.command,headers.deviceID)
}

private updateTemperatureSensorceDeviceType(String cmd,deviceID) {
    def TemperatureSensorNetworkID = deviceID
    def TemperatureSensordevice = getChildDevice(TemperatureSensorNetworkID)
    if (TemperatureSensordevice) {
        TemperatureSensordevice.TemperatureSensorparse("${cmd}")
        writeLog("TemperatureSensorSmartApp - Updating TemperatureSensor Device ${TemperatureSensorNetworkID} using Command: ${cmd}")
    }
}

private addTemperatureSensorDeviceType() {
    //def deviceId = settings.deviceID
    def deviceName = settings.deviceName
    def deviceIP = settings.deviceIP
    def devicePort = settings.devicePort
    def deviceSettings = deviceIP+":"+devicePort
    
    // Important to get the IP of the device in Hex to be the networkID of the device. Hubitat requires it to be Mac, IP:SourcePort or IP
    // Im using device Name to host IP and Port for device communication
    def deviceIdhex = deviceIP.tokenize( '.' )*.toInteger().asType( byte[] ).encodeHex().toString().toUpperCase()
    if (!getChildDevice(deviceId)) {
        def d = addChildDevice("TemperatureSensor", "Temperature Sensor", deviceIdhex, ["name": deviceSettings, label: deviceName, completedSetup: true])
        writeLog("TemperatureSensorSmartApp - device DisplayName: ${d.displayName}")
        writeLog("TemperatureSensorSmartApp - Added TemperatureSensorDeviceType device: ${deviceId}")
    }
    else{
        writeLog("TemperatureSensorSmartApp - DeviceID already exist: ${deviceId}")
    }
}

private getProxyAddress() {
    return settings.proxyAddress + ":" + settings.proxyPort
}

private getNotifyAddress() {
    //return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
    def hubitathub = location.hubs[0]
    return hubitathub.localIP + ":" + hubitathub.localSrvPortTCP
}

private sendCommand(path,endpoint) {
    def host = endpoint
    def headers = [:]
    headers.put("HOST", host)
    headers.put("Content-Type", "application/json")

    def hubAction = new hubitat.device.HubAction(
        method: "GET",
        path: path,
        headers: headers
    )
    sendHubCommand(hubAction)
}

private getHttpHeaders(headers) {
    def obj = [:]
    new String(headers.decodeBase64()).split("\r\n").each {param ->
        def nameAndValue = param.split(":")
        obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
    }
    return obj
}

private getHttpBody(body) {
    def obj = null;
    if (body) {
        def slurper = new JsonSlurper()
        obj = slurper.parseText(new String(body.decodeBase64()))
    }
    return obj
}

private String convertIPtoHex(ipAddress) {
    return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
}

private String convertPortToHex(port) {
    return port.toString().format( '%04x', port.toInteger() ).toUpperCase()
}

private writeLog(message)
{
    if(idelog){
        log.debug "${message}"
    }
}