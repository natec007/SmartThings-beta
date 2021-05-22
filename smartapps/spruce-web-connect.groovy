/**
 *  Spruce Web Connect
 *  v1.01 - 01/19/17 - fix variable initialization for schedules
 *  v1.02 - 01/22/17 - added state var to fix notifications for manual schedule starts that are pushed from the webapp
 *  v1.03 - 02/19/17 - edit finished time wording/remove superfluous 'at'; fixed time display errors when updating schedule times; fix state variable assignment for active schedule for proper api reporting.
 *  v1.04 - 03/15/17 - add support for rain sensor
 *  v1.041- 03/18/17 - bugfix issue from 1.04 where zone states were not sending properly
 *  v1.05 - 04/04/17 - runlist retry if not retrieved. after 2nd attempt, skip watering.
 *  v1.06 - 05/08/17 - add responses for all endpoints. update api endpoints. improve runlist response handling + fix retry. bugfix for saving new schedules to map.
 *	v1.07 - 05/11/17 - convert battery % back to voltage ln 583
 *	v1.08 - 08/10/17 - add schedule queue
 *	v1.09 - 08/15/17 - add Gen1/2 selection
 *  v1.10 - 08/28/17 - atomic state
 *	v1.11 - 09/08/17 - switches = false
 *
 *  Copyright 2017 Plaid Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Spruce Web Connect",
    namespace: "plaidsystems",
    author: "Plaid Systems",
    description: "Connect Spruce devices to the Spruce Cloud",
    category: "",
    iconUrl: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX2Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX3Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    oauth: true)
{
	appSetting "clientId"
	appSetting "clientSecret"
    appSetting "serverUrl"
}

preferences (oauthPage: "authPage") {
	page(name: "authPage")
 	page(name: "pageConnect")
	page(name: "pageController")
    page(name: "pageDevices")
    page(name: "pageUnsetKey")
}

def authPage() {
    if(!atomicState.accessToken) atomicState.accessToken = createAccessToken()	//set = so token is saved to atomicState 
    
    if(!atomicState.key)    {
		pageConnect()
    }
    else {
    	pageDevices()
    }
}

def pageConnect() {
    if(!atomicState.key)    {
		def spruce_oauth_url = "https://app.spruceirrigation.com/connect-smartthings?access_token=${atomicState.accessToken}&logout"
        dynamicPage(name: "pageConnect", title: "Connect to Spruce",  uninstall: false, install:false) { 
            section {
                href url:"https://app.spruceirrigation.com/register?gateway=smartthings", style:"embedded", required:false, title:"Register", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Spruce account is required."
            } 
            section {
                href url: spruce_oauth_url, style:"embedded", required:false, title:"Login to Spruce Cloud", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Login to grant access"
            }
        }
    }
    else if (gen != null){
    	Serial.println(gen)
    	pageDevices()
    }
    else pageController()
}

def pageController() {
    if(!atomicState.key)    {
		pageConnect()
    }
    else {
    	dynamicPage(name: "pageController", uninstall: false, install:false, nextPage: pageDevices) { 
            section("Select Spruce Controller Version") {
                input 'gen', 'enum', title: "Gen1: Black, Zigbee/SmartThings Only\nGen2: White, WiFi", required: true, multiple: false,	metadata: [values: ['Gen1','Gen2']]
            }
        }
    }    
}
            
def pageDevices() {
    if(!atomicState.key)    {
		pageConnect()
    }
    else {
    	dynamicPage(name: "pageDevices", uninstall: true, install:true) { 
            if (gen == "Gen1"){
            section("Select Spruce Controllers to connect, do not select individual zones:") {
                input "switches", "capability.switch", title: "Spruce Irrigation Controller:", required: false, multiple: false 
            }}
            section("Select Spruce Sensors to connect:") {
                input "sensors", "capability.relativeHumidityMeasurement", title: "Spruce Moisture sensors:", required: false, multiple: true 
            }/*
            section ("Edit Schedules on the Spruce Cloud"){
                href url:"https://app.spruceirrigation.com/schedule", style:"embedded", required:false, title:"Schedules", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: ""
            }*/
            section ("Tap Done to save changes to Spruce Cloud"){
                href url:"https://app.spruceirrigation.com/devices", style:"external", required:false, title:"Spruce WebApp", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Click to visit app.spruceirrigation.com"
            }      
            section {
                href page: "pageUnsetKey", title:"Reset Login", description: "Tap to forget Spruce API key and re-start login. For troubleshooting only."
            }          
        }   
    }
}

def pageUnsetKey() {
	atomicState.key = null
    dynamicPage(name: "pageUnsetKey", uninstall: false, install:false) { 
        section {
            paragraph "Spruce API key forgotten. Go back to re-start connect process."  
        }          
    }   
}

def oauthInitUrl() /*not used*/
{
    log.debug "oauthInitUrl"    
    def oauthClientId = appSettings.clientId
	def oauthClientSecret = appSettings.clientSecret
	def oauth_url = "https://app.spruceirrigation.com/connect-smartthings?client=${oauthClientId}&secret=${oauthClientSecret}"
    
    log.debug(oauthClientId)
    log.debug(oauthClientSecret)
 	
	return oauth_url	
}

mappings {
  path("/schedule") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/schedule/:command") {
    action: [
      POST: "setSchedule"
    ]
  }
  path("/delete/:command") {
    action: [
      POST: "deleteSchedule"
    ]
  }
  path("/zonetime/:command") {
    action: [
      PUT: "zonetimes"
    ]
  }
  path("/zoneoption/:command") {
    action: [
      PUT: "zoneoptions"
    ]
  }
  path("/run/:command") {
    action: [
      POST: "runZone",
      GET: "runZone"
    ]
  }
  path("/apikey/:command") {
    action: [
      POST: "setKey"
    ]
  }
  path("/delay/:command") {
    action: [
      POST: "setDelay"
    ]
  }
}

//***************** install *******************************

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"	
	unsubscribe()
    initialize()    
}

def initialize() {	
    log.debug "initialize"  
    atomicState.active_sch = "none"
    atomicState.run_today = false
    atomicState.delay = '0'
    atomicState.que = ["none","none"]
    if (settings.switches) getSwitches()
    if (settings.sensors) getSensors()
    
    //add devices to web, check for schedules
    if(atomicState.key){
    	addDevices()
    	addSchedules()
	}
}

//set spruce api key
def setKey(){
	log.debug "setkey: " + params.command
    
	atomicState.key = params.command    
    if (atomicState.key && atomicState.key == params.command) {
    	log.debug "API key set, get schedules"
        //getSchedule()
        return [error: false, return_value: 1, data: 'key set']
        }
    else return [error: true, return_value: 0, data: 'key not set']
    //else return httpError(400, "$command is not a valid command for all switches specified")
       
}

//set pump delay
def setDelay(){
	//I'm sending deviceid and delay - still need to update to parse the 2 
    //   /delay/deviceid=' + deviceID + '&delay=' + value;
	log.debug "setdelay: " + params.command
    
    def p = params.command.split('&')
    def delay = p[1].split('=')[1]  
	
    atomicState.delay = delay 
    if (delay == 'delete') atomicState.delay = '0'
     
    if (atomicState.delay == delay) {
    	log.debug "Delay set = " + delay
        return [error: false, return_value: 1, data: 'delay set']
    }
    else if ( delay == 'delete' && atomicState.delay == '0') {
    	log.debug "Delay deleted "
        return [error: false, return_value: 1, data: 'delay deleted']
    }
    else return [error: true, return_value: 0, data: 'delay not set']
    //else return httpError(400, "$command is not a valid command for all switches specified")
       
}

//switch subscriptions
def getSwitches(){
	log.debug "getSwitches: " + settings.switches    
    
    //atomicState.switches = [:]
    def tempMap = [:]
    settings.switches.each{
    	tempMap[it]= (it.device.zigbeeId)
        }
    atomicState.switches = tempMap 
    
    subscribe(settings.switches, "switch", switchHandler)
    subscribe(settings.switches, "switch1", switchHandler)
    subscribe(settings.switches, "switch2", switchHandler)
    subscribe(settings.switches, "switch3", switchHandler)
    subscribe(settings.switches, "switch4", switchHandler)
    subscribe(settings.switches, "switch5", switchHandler)
    subscribe(settings.switches, "switch6", switchHandler)
    subscribe(settings.switches, "switch7", switchHandler)
    subscribe(settings.switches, "switch8", switchHandler)
    subscribe(settings.switches, "switch9", switchHandler)
    subscribe(settings.switches, "switch10", switchHandler)
    subscribe(settings.switches, "switch11", switchHandler)
    subscribe(settings.switches, "switch12", switchHandler)
    subscribe(settings.switches, "switch13", switchHandler)
    subscribe(settings.switches, "switch14", switchHandler)
    subscribe(settings.switches, "switch15", switchHandler)
    subscribe(settings.switches, "switch16", switchHandler)
    subscribe(settings.switches, "rainsensor", switchHandler)
    
}

//sensor subscriptions
def getSensors(){    
    log.debug "getSensors: " + settings.sensors    
    
    //atomicState.sensors = [:]    
    def tempMap = [:]
    settings.sensors.each{
    	tempMap[it]= (it.device.zigbeeId)
        }
    atomicState.sensors = tempMap
    
    subscribe(settings.sensors, "humidity", sensorHandler)
    subscribe(settings.sensors, "temperature", sensorHandler)
    subscribe(settings.sensors, "battery", sensorHandler)
    
}

//add devices to web
def addDevices(){
	
    //add controllers to web
    def tempSwitchMap = atomicState.switches
    tempSwitchMap.each{
    	def PUTparams = [
            uri: "https://api.spruceirrigation.com/v1/device/" + it.value,
            headers: [ 	"Authorization": atomicState.key],        
            body: [
                nickname: it.key,
                type: "CO",
                gateway: "smartthings",
                num_zones: "16"
                ]
        ]    
        log.debug PUTparams
        try{
            httpPut(PUTparams){
                resp -> //resp.data {
                    log.debug "${resp.data}"               
                }                
            } 
        catch (e) {
            log.debug "send DB error: $e"
            }
    }
    atomicState.switches = tempSwitchMap
	
    //add sensors to web
    def tempSensorMap = atomicState.sensors
    tempSensorMap.each{
    	def PUTparams = [
            uri: "https://api.spruceirrigation.com/v1/device/" + it.value,
            headers: [ 	"Authorization": atomicState.key],        
            body: [
                nickname: it.key,
                type: "MS",
                gateway: "smartthings"
                ]
        ]    
        try{
            httpPut(PUTparams){
                resp -> //resp.data {
                    log.debug "${resp.data}"               
            }                
        } 
        catch (e) {
            log.debug "send DB error: $e"
        }
    }
    atomicState.sensors = tempSensorMap


}

//***************** schedule setup commands ******************************
//check for pre-set schedules
def addSchedules(){
	def respMessage = ""
    def key = atomicState.key
    def switchID
    
    def tempSwitchesMap = atomicState.switches
    tempSwitchesMap.each{        
        switchID = it.value
        respMessage = "$switchID: "
        
        def newuri =  "https://api.spruceirrigation.com/v1/settings?deviceid="
        newuri += switchID       

        def scheduleType
        def scheduleID = []    
        def tempSchMap = [:]	//atomicState.scheduleMap = [:]
        def tempManMap = [:]	//atomicState.manualMap = [:]

        def GETparams = [
                uri: newuri,		
                headers: [ 	"Authorization": key],           
                ]

        try{ httpGet(GETparams) { resp ->	
            //get schedule list        
            scheduleID = resp.data['controller'][switchID]['schedules'].split(',')
            if (scheduleID) {
                //get schedule types
                def i = 1
                scheduleID.each{
                    if ( resp.data['schedule'][it]['sched_enabled'] == '1') {
                        scheduleType = resp.data['schedule'][it]['schedule_type']
                        if (scheduleType == 'connected' || scheduleType == 'basic'){                    	
                            //atomicState.scheduleMap[it] = ['id': i, 'deviceid' : switchID, 'start_time' : resp.data['schedule'][it]['start_time']]
                            tempSchMap[i] = ['scheduleid' : it, 'deviceid' : switchID, 'start_time' : resp.data['schedule'][it]['start_time'], 'name' : resp.data['schedule'][it]['schedule_name']]
                            }                     
                            i++                        
                    }
                    if (resp.data['schedule'][it]['sched_enabled'] == '1' && resp.data['schedule'][it]['schedule_type'] == 'manual') {
                            respMessage += " Manual Sch acquired, "
                            tempManMap = ['scheduleid' : it, 'deviceid' : switchID, 'start_time' : 'manual', 'run_list' : resp.data['schedule'][it]['run_list'][switchID]['1'], 'name' : resp.data['schedule'][it]['schedule_name']]                            
                    }
                }
                respMessage += "Schedules acquired"
                }
             else respMessage += "No schedules available for controller"   
            }
        }
        catch (e) {
        	respMessage += "No schedules set, API error: $e"
            
            }
        atomicState.scheduleMap = tempSchMap
        atomicState.manualMap = tempManMap
        log.debug respMessage        
    }   
    
    if (atomicState.scheduleMap) setScheduleTimes()    
}

//set schedules times to run
def setScheduleTimes(){
	unschedule()
    //log.debug "setting schedule times"
    def message = "";    
    def ii = 0;
    def tempMap = atomicState.scheduleMap
    tempMap.each{ 
    	ii++
        def i = it.key
        def scheduleTime = tempMap[it.key]['start_time']        
        
        def hms = scheduleTime.split(':')  

        int hh = hms[0].toInteger()
        int mm = hms[1].toInteger()
        int ss = ii * 2

        //set schedule run times            
        schedule("${ss} ${mm} ${hh} ? * *", "Check${i}")
        //log.debug "set schedule Check${i} to ${hh}:${mm}"
        //message += " set to ${hh}:${mm}, "
        if (ii != 1) { message += ", "}
        message += tempMap[it.key]['name'] + " set to ${scheduleTime}"
       
	}
    atomicState.scheduleMap = tempMap
    note("schedule", message, "d")
}

//command to recieve schedules
def setSchedule() {  

    log.debug(params.command)
    def sch = params.command.split('&')
    def sch_id = sch[0].split('=')[1]    
    
    boolean Sch_Set
    def count = 0
    if (sch[4].split('=')[1] == 'manual'){    	
    	atomicState.manualMap = ['scheduleid' : sch[0].split('=')[1], 'start_time' : sch[1].split('=')[1], 'name' : sch[2].split('=')[1], 'type' : sch[4].split('=')[1]]        
        Sch_Set = true
       }
    else if (atomicState.scheduleMap){
    	def tempMap = atomicState.scheduleMap
    	tempMap.each{        	
       		//log.debug "key $it.key, count $count"
            //if (count == it.key.toInteger()) count++            
            count++            
            if (tempMap[it.key]['scheduleid'] == sch[0].split('=')[1]  && !Sch_Set){
            	
                tempMap[it.key]['start_time'] = sch[1].split('=')[1]
                tempMap[it.key]['name'] = sch[2].split('=')[1]
                tempMap[it.key]['type'] = sch[4].split('=')[1]
            //add deviceid    
                log.debug "Schedule updated"
                Sch_Set = true                
			}
    	}
        atomicState.scheduleMap = tempMap
    }
    else {
    	def newMap = [:]
        newMap[1] = ['scheduleid' : sch[0].split('=')[1], 'start_time' : sch[1].split('=')[1], 'name' : sch[2].split('=')[1], 'type' : sch[4].split('=')[1]]
        atomicState.scheduleMap = newMap
        Sch_Set = true
		log.debug "Schedule created"
    }
    if (!Sch_Set && count <= 6){
    	def tempMap = atomicState.scheduleMap
        for ( count = 1; count <= 6; count++){
        	//log.debug atomicState.scheduleMap[count.toString()]            
            if (tempMap[count.toString()] == null && !Sch_Set) {
            	tempMap[count.toString()] = ['scheduleid' : sch[0].split('=')[1], 'start_time' : sch[1].split('=')[1], 'name' : sch[2].split('=')[1], 'type' : sch[4].split('=')[1]]        
            	Sch_Set = true
            	log.debug "Schedule added at $count"                
                }
            }
         atomicState.scheduleMap = tempMap
        }
   	
    if (Sch_Set){
    	
        setScheduleTimes()    
        return [error: false, return_value: 1]
		//return httpError(200, "schedule set")
        }
    else return [error: true, return_value: 0, message: "schedule declined, count exceeded 6"] //return httpError(200, "schedule declined, count exceeded 6") 
    
}

//remove schedule
def deleteSchedule() {  

    log.debug(params.command)
    def sch = params.command.split('&')
    def sch_id = sch[0].split('=')[1]
	log.debug(sch_id)
    def message = "";
    def count = 0
    boolean remove = false
        
    if (atomicState.scheduleMap){
    	def tempMap = atomicState.scheduleMap
    	tempMap.each{        	
       		if (tempMap[it.key]['scheduleid'] == sch_id){            	           
            	count = it.key
                remove = true
                message += tempMap[it.key]['name']
                message += " removed"
                log.debug "Schedule removed"
                return
                }
    	}        
    
        log.debug count
        if (remove) {
            tempMap.remove(count)
            atomicState.scheduleMap = tempMap
            setScheduleTimes()
            note("schedule", message, "d")
            return [error: false, return_value: 1]
        }    
    }
    return [error: false, return_value: 1, message: "schedule not found, nothing to delete"]
}  

//***************** event handlers *******************************
def parse(description) {
	log.debug(description)
}

//controller evts
def switchHandler(evt) {    
    log.debug "switchHandler: ${evt.device}, ${evt.name}, ${evt.value}"
    def scheduleMap
    def scheduleid = 0
    def duration = 0
    def tempSwitchesMap = atomicState.switches
    def tempSchMap = atomicState.scheduleMap
    def tempTimeMap = atomicState.timeMap
       
    //post zone on/off to web
    if (evt.value.contains('on') || evt.value.contains('off')){
        
        def device = tempSwitchesMap["${evt.device}"]   
        def zonestate = 1
        if (evt.value.contains('off')) zonestate = 0    
        
        def EP = 0
        if (evt.name == "rainsensor") {
        	EP = 101
        }        
        else if (evt.name != "switch") {
        	EP = evt.name.replace('switch',"").toInteger()
        }
        
        
        if (atomicState.active_sch != "none" && atomicState.run_today){
            if (tempSchMap[atomicState.active_sch]) scheduleMap = tempSchMap[atomicState.active_sch]
            else if (atomicState.active_sch == 'manual') scheduleMap = atomicState.manualMap
            else if (atomicState.active_sch == 'cloudSch') scheduleMap = atomicState.cloudSchMap
                        
            if (EP == 0 || atomicState.run_today) {
            //else if (atomicState.run_today) scheduleid = scheduleMap['scheduleid']
            	scheduleid = scheduleMap['scheduleid']
            	duration = scheduleMap['run_length']
            }
        
        }
            
        if (zonestate == 0 && EP != 0) duration = 0
        else if (!atomicState.run_today) duration = settings.switches.currentValue('minutes')[0].toInteger() * 60     
        else if (EP != 0) duration = tempTimeMap[(EP+1).toString()].toInteger() * 60        
       
        log.debug "Zone ${EP} ${zonestate} for ${duration}"

        def postTime = now() / 1000

        def POSTparams = [
                        uri: "https://api.spruceirrigation.com/controller/zone",
                        headers: [ 	"Authorization": atomicState.key], 
                        body: [
                            zid: device,                        
                            zone: EP,
                            zonestate: zonestate,
                            zonetime: postTime.toInteger(),
                            duration: duration,
                            schedule_id: scheduleid
                        ]
                    ]
        sendPost(POSTparams)
    }
    
    if (atomicState.run_today && evt.value == 'off') cycleOff()
    else if (!atomicState.run_today && evt.value == 'programOn') manual_schedule()
}

//sensor evts
def sensorHandler(evt) {
    log.debug "sensorHandler: ${evt.device}, ${evt.name}, ${evt.value}"
    
    def device = atomicState.sensors["${evt.device}"]
    
    def uri = "https://api.spruceirrigation.com/controller/"
    if (evt.name == "humidity") uri += "moisture"    
    else uri += evt.name
    
    def value = evt.value
    //added for battery
    if (evt.name == "battery") value = evt.value.toInteger() * 5 + 2500
    
    def POSTparams = [
                    uri: uri,
                    headers: [ 	"Authorization": atomicState.key], 
                    body: [
                        deviceid: device,
                        value: value                        
                    ]
                ]

	sendPost(POSTparams)
}

def sendPost(POSTparams) {
	try{
        httpPost(POSTparams) { 
        	//log.debug 'Data pushed to DB' 
        }
    } 
    catch (e) {
        log.debug 'send DB error: $e'
    }
}

//***************** schedule run commands ******************************
//schedule on
def schOn(){	
    log.debug "run today: ${atomicState.run_today}"
    def sch = atomicState.active_sch
    if(atomicState.run_today){
        //settings.switches.zon()
        settings.switches.start()
        
        def schedule_map        
        if (atomicState.scheduleMap[sch]) schedule_map = atomicState.scheduleMap[sch]
    	else if (sch == 'manual' && atomicState.manualMap) schedule_map = atomicState.manualMap
        else if (sch == 'cloudSch' && atomicState.cloudSchMap) schedule_map = atomicState.cloudSchMap
        //else cycleOff()

        def sch_name = schedule_map['name']
        def run_time = schedule_map['run_length']
        run_time = (run_time / 60).toDouble().round(1)
        String finishTime = new Date(now() + (schedule_map['run_length'] * 1000).toLong()).format('h:mm a', location.timeZone)
        note("active", "${sch_name} ends at ${finishTime}", "d")
    }
    else {
    	cycleOff()
        //settings.switches.programOff()
    	//note("skip", "Skip Schedule", "d")
    }
}

//schedule finish/off notification
def cycleOff(){
	log.debug "schedule finished"
    def sch = atomicState.active_sch
    def schedule_map
    
    if (atomicState.run_today){
        if (atomicState.scheduleMap[sch]) schedule_map = atomicState.scheduleMap[sch]
        else if (sch == 'manual' && atomicState.manualMap) schedule_map = atomicState.manualMap
        else if (sch == 'cloudSch' && atomicState.cloudSchMap) schedule_map = atomicState.cloudSchMap
        def sch_name = schedule_map['name']


        //settings.switches.off()
        String finishTime = new Date(now().toLong()).format('EE @ h:mm a', location.timeZone)
        note('finished', "${sch_name} finished ${finishTime}", 'd')
    }
    else settings.switches.programOff()
    
    atomicState.run_today = false
    atomicState.active_sch = "none"
    check_que()
}

def check_que(){    
    def tempQue = atomicState.que
    if(tempQue[0] != "none"){
    	runIn(20, "${tempQue[0]}")
        tempQue[0] = "none"
    }
    else if(tempQue[1] != "none"){
    	runIn(20, "${tempQue[1]}")
        tempQue[1] = "none"
    }    
    
    atomicState.que = tempQue
}

//retrieve current runlist
def getTodaysTimes(sch){
    log.debug "get todays times for Check$sch"
    atomicState.run_today = false    
    
    def respMessage = ""
    def result = []
    def tempCloudSchMap = [:]
    //atomicState.cloudSchMap = [:]
        
    def schedule_map
    def scheduleID
    def tempSchMap = atomicState.scheduleMap
    
    if (tempSchMap[sch]) schedule_map = tempSchMap[sch]
    else if (sch == 'manual' && atomicState.manualMap) schedule_map = atomicState.manualMap
    
    //create map for schedules sent directly from cloud
    if (!schedule_map && sch != 'manual'){
    	scheduleID = sch
        sch = 'cloudSch'
        schedule_map = tempCloudSchMap
        }
    else if (!schedule_map && sch == 'manual'){
    	//no manual schedule set - exit
        result[0] = "skip"
        result[1] = 'No manual schedule set'
    	return result        
    	}
    else scheduleID = schedule_map['scheduleid']    
    
    def switchID = atomicState.switches["${settings.switches}"]
    
    atomicState.active_sch = sch    
    //atomicState.run_today = true	//leave false
    //def update = true
    
    def schedule_name
    def run_length
    def scheduleTime
    def scheduleZone = '10:' + atomicState.delay + ','
    def status_message
    def weather_message    
    
    def error
    def message
           
    def newuri =  "https://api.spruceirrigation.com/schedule/runlist?deviceid="
        newuri += switchID
        newuri += "&scheduleid="
        newuri += scheduleID
        newuri += "&run=true"
        
	//log.debug newuri
    //log.debug atomicState.key
    
    def GETparams = [
        uri: newuri,		
        headers: [ 	"Authorization": atomicState.key],           
    ]
    try{ httpGet(GETparams) { resp ->	
        //scheduleid
        schedule_name = resp.data['schedule_name']
        atomicState.run_today = resp.data['run_today']
        scheduleTime = resp.data['start_time']
        scheduleZone += resp.data['runlist']
		run_length = resp.data['run_length']
        status_message = resp.data['status_message']
        weather_message = resp.data['rain_message']
            
        error = resp.data['error']        
        if (error == true) respMessage += resp.data['message']        
        
    	}
	}
    catch (e) {        
        log.debug "send DB error: $e"
        //update = false
        error = true
        
        if (atomicState.retry == false){
    		atomicState.retry = true
        
    		if (sch == '1') runIn(300, Check1, [overwrite: false])
        	else if (sch == '2') runIn(300, Check2, [overwrite: false]) 
        	else if (sch == '3') runIn(300, Check3, [overwrite: false]) 
        	else if (sch == '4') runIn(300, Check4, [overwrite: false]) 
        	else if (sch == '5') runIn(300, Check5, [overwrite: false]) 
        	else if (sch == '6') runIn(300, Check6, [overwrite: false]) 
    		respMessage += "runlist error, retry in 5 minutes, $e"
        }
        else {
            atomicState.retry = false
            respMessage += "runlist retry failed, skipping schedule, $e"
        }
        //result[1] = respMessage
    	//return result
    }

    //log.debug scheduleZone
    
    if (error == false){
    	atomicState.retry = false
        
        respMessage += schedule_name
    	result[0] = weather_message
    
        if (atomicState.run_today){
    	result[0] = "active"
    	respMessage += " starts in 1 min\n "
        }
        else {
            //result[0] = "skip"
            respMessage += " skipping today "
        }        
        
        //save schedule time & zone settings
        if (sch == 'manual'){
        	atomicState.manualMap['run_list'] = scheduleZone
            atomicState.manualMap.put ('run_length', run_length)
            }
        else if (sch == 'cloudSch'){
        	tempCloudSchMap = ['scheduleid' : scheduleID, 'run_list' : scheduleZone, 'name' : schedule_name, 'run_length' : run_length] 
        	}
        else {
        	schedule_map.put ('name', schedule_name)
            tempSchMap[sch]['start_time'] = scheduleTime
            
            //only update if run today
            if (atomicState.run_today){
            	tempSchMap[sch].put ('run_list', scheduleZone)
            	tempSchMap[sch].put ('run_length', run_length)                
			}

            def hms = scheduleTime.split(':')    

            int whh = 23
            int wmm = 0
            int wtime = 0

            int hh = hms[0].toInteger()
            int mm = hms[1].toInteger()
            int ss = 0

            if ( (hh*60 + mm) <= (whh * 60 + wmm) ){
                wtime = hh*60 + mm - 5
                whh = wtime / 60
                wmm = ((wtime / 60 * 100) - (whh * 100)) * 60 /100
                //log.debug "set schedule to ${hh}:${mm}"
            }
            //set schedule run time
            schedule("${ss} ${mm} ${hh} ? * *", "Check${sch}")
		}        
        respMessage += status_message
    }
    else if (sch == 'manual') respMessage += "schedule error, check settings"
    
    atomicState.cloudSchMap = tempCloudSchMap
    atomicState.scheduleMap = tempSchMap
    log.debug "runlist response $error : $respMessage"   
    result[1] = respMessage
    return result    
}

//parse zone times/cycles from runlist
def zoneCycles(sch) {    
    int i = 0
    
    if (atomicState.run_today){
        def schedule_map
        def tempSchMap = atomicState.scheduleMap
        if (tempSchMap[sch]) schedule_map = tempSchMap[sch]
    	else if (sch == 'manual' && atomicState.manualMap) schedule_map = atomicState.manualMap
        else if (sch == 'cloudSch' && atomicState.cloudSchMap) schedule_map = atomicState.cloudSchMap
        else atomicState.run_today = false
        
        def scheduleID = schedule_map['scheduleid']    
        def zoneArray = schedule_map['run_list']
        log.debug zoneArray

        def newMap = zoneArray.split(',')

        def tempCycleMap = [:]	//atomicState.cycleMap = [:]
        def tempTimeMap = [:]	//atomicState.timeMap = [:]
        def option = []
    //need to add additional settings? added above at scheduleZone = '10:1,'  
        for(i = 0; i < 17; i++){    	
            option = newMap[i].toString().split(':')
            if (i == 0) tempTimeMap."${i+1}" = option[0]
            else if (option[1] != "0") tempTimeMap."${i+1}" = (Math.round(option[0].toInteger() / option[1].toInteger())).toString()
            else tempTimeMap."${i+1}" = option[0]
            tempCycleMap."${i+1}" = option[1]
        }
        atomicState.cycleMap = tempCycleMap
        atomicState.timeMap = tempTimeMap
        //log.debug "cycleMap: ${atomicState.cycleMap}"
        //log.debug "timeMap: ${atomicState.timeMap}"

        //send settings to controller
        settings.switches.settingsMap(tempCycleMap, 4001)
        runIn(20, sendTimeMap)
    }
    else runIn(10, schOn)	//sch end
}

//send runlist times to spruce controller
def sendTimeMap(){
	def tempTimeMap = atomicState.timeMap
	settings.switches.settingsMap(tempTimeMap, 4002)
    }

//**************** scheduled times ********************
def Check1(){
	def tempQue = atomicState.que
    if(atomicState.active_sch == "none"){								 
        runIn(10, zoneCycles1)
        runIn(60, schOn)
        settings.switches.programWait()
        def result = getTodaysTimes('1')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        log.debug "Starting Check 1 in 1 minute"
    }
    else if (tempQue[0] == "none") tempQue[0] = Check1
    else if (tempQue[1] == "none") tempQue[1] = Check1
    else check_que()
    atomicState.que = tempQue
}
def zoneCycles1() {zoneCycles('1')}

def Check2(){
	def tempQue = atomicState.que
    if(atomicState.active_sch == "none"){									
        runIn(10, zoneCycles2)
        runIn(60, schOn)
        settings.switches.programWait()
        def result = getTodaysTimes('2')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        log.debug "Starting Check 2 in 1 minute"
    }
    else if (tempQue[0] == "none") tempQue[0] = Check2
    else if (tempQue[1] == "none") tempQue[1] = Check2
    else check_que()
    atomicState.que = tempQue
}
def zoneCycles2() {zoneCycles('2')}

def Check3(){
	def tempQue = atomicState.que
    if(atomicState.active_sch == "none"){									
        runIn(10, zoneCycles3)
        runIn(60, schOn)
        settings.switches.programWait()
        def result = getTodaysTimes('3')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        log.debug "Starting Check 3 in 1 minute"
    }
    else if (tempQue[0] == "none") tempQue[0] = Check3
    else if (tempQue[1] == "none") tempQue[1] = Check3
    else check_que()
    atomicState.que = tempQue
}
def zoneCycles3() {zoneCycles('3')}

def Check4(){
	def tempQue = atomicState.que
    if(atomicState.active_sch == "none"){
        runIn(10, zoneCycles4)
        runIn(60, schOn)
        settings.switches.programWait()
        def result = getTodaysTimes('4')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        log.debug "Starting Check 4 in 1 minute"
    }
    else if (tempQue[0] == "none") tempQue[0] = Check4
    else if (tempQue[1] == "none") tempQue[1] = Check4
    else check_que()
    atomicState.que = tempQue
}
def zoneCycles4() {zoneCycles('4')}

def Check5(){
	def tempQue = atomicState.que
    if(atomicState.active_sch == "none"){
        runIn(10, zoneCycles5)
        runIn(60, schOn)
        settings.switches.programWait()
        def result = getTodaysTimes('5')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        log.debug "Starting Check 5 in 1 minute"
    }
    else if (tempQue[0] == "none") tempQue[0] = Check5
    else if (tempQue[1] == "none") tempQue[1] = Check5
    else check_que()
    atomicState.que = tempQue
}
def zoneCycles5() {zoneCycles('5')}

def Check6(){
	def tempQue = atomicState.que
    if(atomicState.active_sch == "none"){
        runIn(10, zoneCycles6)
        runIn(60, schOn)
        settings.switches.programWait()
        def result = getTodaysTimes('6')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        log.debug "Starting Check 6 in 1 minute"
    }
    else if (tempQue[0] == "none") tempQue[0] = Check6
    else if (tempQue[1] == "none") tempQue[1] = Check6
    else check_que()
    atomicState.que = tempQue
}
def zoneCycles6() {zoneCycles('6')}

def manual_schedule(){
	//if(atomicState.active_sch == "none"){
        //log.debug "Manual Schedule starting"
        runIn(10, zoneCyclesM)
        runIn(60, schOn)

        def result = getTodaysTimes('manual')
        def status = result[0]
        def message = result[1]
        note(status, message, "d")
        settings.switches.programWait()

        log.debug "Starting Check M in 1 minute"
   /*}
    else if (atomicState.que[0] == "none") atomicState.que[0] = manual_schedule
    else if (atomicState.que[1] == "none") atomicState.que[1] = manual_schedule*/
}
def zoneCyclesM() {zoneCycles('manual')}

def zoneCyclesC() {zoneCycles('cloudSch')}


//************* notifications to device, pushed if requested ******************
def note(status, message, type){
	log.debug "${status}:  ${message}"
    settings.switches.notify("${status}", "${message}")
    if(notify)
    {
      if (notify.contains('Daily') && type == "d"){       
        sendPush "${message}"
      }
      if (notify.contains('Weather') && type == "f"){     
        sendPush "${message}"
      }
      if (notify.contains('Warnings') && type == "w"){     
        sendPush "${message}"
      }
      if (notify.contains('Moisture') && type == "m"){        
        sendPush "${message}"
      }      
    }
}

//**************************** device commands **************************************
def runZone(){
	log.debug(params.command)
    // use the built-in request object to get the command parameter
    def command = params.command
    def zoneonoff = command.split(',')    
    
    switch(zoneonoff[0]) {
        case "zon":
            //set turn on time
            def runT = zoneonoff[2].toInteger() / 60
    		settings.switches.manualTime(runT)
         	//pumpOn()
            zoneOn(zoneonoff[1])            
            return [error: false, return_value: 1]
            //def response = 'callback({"error":false,"return_value":1})'
            //render contentType: "application/jsonp", data: response, status: 200
            break
        case "zoff":
            zoneOff(zoneonoff[1])           
            //def response = 'callback({"error":false,"return_value":1})'
            //render contentType: "application/jsonp", data: response, status: 200
            return [error: false, return_value: 1]
            break
        case "son":        	
            //use manual table to store run values
            runIn(30, zoneCyclesC)
            runIn(60, schOn)
            
            //send scheduleID
            def result = getTodaysTimes(zoneonoff[1])
            def status = result[0]
            def message = result[1]

            log.debug "$status, $message"
            note(status, message, "d")
            settings.switches.programWait()

            log.debug "Starting Check cloudSch in 1 minute"
            return [error: false, return_value: 1]
            break
        case "soff": 
        	settings.switches.off()
         	cycleOff()
            return [error: false, return_value: 1]
            break
        default:
            return [error: true, return_value: 0, message: "no command found"] //httpError(400, "$command is not a valid command for all switches specified")
    }    
}

def zoneOn(zone){	
	//settings.switches."z${zone}on"()
    //log.debug settings.switches
    settings.switches.zoneon("${settings.switches.deviceNetworkId}.${zone}")
}

def zoneOff(zone){
	//settings.switches."z${zone}off"()
    settings.switches.zoneoff("${settings.switches.deviceNetworkId}.${zone}")
}