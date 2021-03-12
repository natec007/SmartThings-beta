# Spruce-SmartThings-beta

beta device, smartapps for SmartThings

# Versions

## Version v3.4
 * update presentation with 'patch' to rename 'valve' to 'Zone x'
 * remove commands on, off
 * add command setValveDuration
 * update settings order and description
 * fix controllerStatus -> status

 ## Version v3.3
 * change to remotecontrol with components

 ## Version v3.2
 * add zigbee constants
 * update to zigbee commands
 * tabs and trim whitespace

 ## Version v3.1
 * Change to work with standard ST automation options
 * use standard switch since custom attributes still don't work in automations
 * Add schedule minute times to settings
 * Add split cycle to settings
 * deprecate Spruce Scheduler compatibility

 ## Version v3.0
 * Update for new Samsung SmartThings app
 * update vid with status, message, rainsensor
 * maintain compatibility with Spruce Scheduler
 * Requires Spruce Valve as child device

 ## Version v2.7
 * added Rain Sensor = Water Sensor Capability
 * added Pump/Master
 * add "Dimmer" to Spruce zone child for manual duration


# CLI Command Reference
The main cli commands used so I don't have to remember them in the future

- smartthings presentation:device-config:create -j -i controllerSinglePresentationConfig.json
- smartthings capabilities:presentation:update heartreturn55003.valveDuration 1 -y -i valveDuration.json
