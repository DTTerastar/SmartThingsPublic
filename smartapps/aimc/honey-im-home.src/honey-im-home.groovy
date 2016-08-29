/**
* Honey, I'm Home!
*
* Author: oneaccttorulethehouse@gmail.com
* Date: 2014-01-26
*
* Heavily borrowed from Night Light/Sunrise/Sunset and Greetings Earthling.
*
*	Will turn on lights and change the mode when someone arrives home and it is dark outside.
*
*
*/


// Automatically generated. Make future change here.
definition(
    name: "Honey I'm Home",
    namespace: "aimc",
    author: "Andrew Cockburn",  
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences 
{
	page( name:"Settings", title:"Settings", nextPage:"selectPhrases", uninstall: true )
    {
        section("When one of these people arrive at home") 
        {
            input "people", "capability.presenceSensor", multiple: true
        }
        section ("Sunrise offset (optional)...") 
        {
            input "sunriseOffsetValue", "text", title: "HH:MM", required: false
            input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
        }
        section ("Sunset offset (optional)...") 
        {
            input "sunsetOffsetValue", "text", title: "HH:MM", required: false
            input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
        }
        section ("Zip code (optional, defaults to location coordinates)...") 
        {
            input "zipCode", "text", required: false
        }
        section( "Notifications" ) 
        {
            input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
            input "phone", "phone", title: "Send a Text Message?", required: false
        }
        section([mobileOnly:true]) 
        {
                label title: "Assign a name", required: false
                mode title: "Set for specific mode(s)", required: false
        }
    }
    page(name: "selectPhrases")
}

def selectPhrases() {
    dynamicPage(name: "selectPhrases", title: "Configure", uninstall:true, install:true) {        
    	def phrases = location.helloHome?.getPhrases()*.label
    	if (phrases) {
        	phrases.sort()
        	section("Run This Phrase When...") {
            	log.trace phrases
            	input "lightPhrase", "enum", title: "It's light", required: true, options: phrases,  refreshAfterSelection:true
            	input "darkPhrase", "enum", title: "It's dark", required: true, options: phrases,  refreshAfterSelection:true
            }
        }
    }
}


def installed() 
{
    log.debug "Installed with settings: ${settings}"
    log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
    subscribe(people, "presence", presence)
}

def updated() 
{
    log.debug "Updated with settings: ${settings}"
    log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
    unsubscribe()
    subscribe(people, "presence", presence)
}

private isDark(rt, st) 
{
    def dark
    def t = now()
    dark = t < rt || t > st
    dark
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"

	if (evt.value == "present")
    {
        def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
        def riseTime = s.sunrise
        def setTime = s.sunset
        log.debug "riseTime: $riseTime"
        log.debug "setTime: $setTime"

        def Phrase

        if (isDark(riseTime.time, setTime.time))
        {
            Phrase = darkPhrase
        }
        else
        {
            Phrase = lightPhrase
        }

        def person = getPerson(evt)

        def message = "${person.displayName} arrived at home, executing '${Phrase}'"
        log.info message
        send(message)
        location.helloHome.execute(Phrase)
	}
}

private send(msg) 
{
    if ( sendPushMessage != "No" ) 
    {
	    log.debug( "sending push message" )
    	sendPush( msg )
	}

	if ( phone ) 
	{
		log.debug( "sending text message" )
		sendSms( phone, msg )
	}

	log.debug msg
}

private getPerson(evt)
{
	people.find{evt.deviceId == it.id}
}

private getSunriseOffset() 
{
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() 
{
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}