package com.ensate.robotixcarrc;

/**
 * Defines several constants used between {@link } and the UI.
 */
public class Constants {


    // Here are the settings of the MQTT Cloud
    public static final String MQTT_SERVER_NAME = "tcp://postman.cloudmqtt.com";
    public static final String MQTT_USERNAME = "ubfiqydu";
    public static final String MQTT_PASSWORD = "6MP3SoYEjK-s";
    public static final String MQTT_PORT = "15614";
    public static final String MQTT_SSL_PORT = "25614";
    public static final String MQTT_WEBSOCKET_PORT = "35614";

    // Topics for choose_mode, robot_state, move , speed and lights controls

    public static final String CAR_MODE_TOPIC = "RoPosix/mode";;
    public static final String CAR_STATE_TOPIC = "RoPosix/state";;
    public static final String CAR_MOVE_TOPIC = "RoPosix/move";;
    public static final String CAR_SPEED_TOPIC = "RoPosix/speed";;
    public static final String CAR_LIGHT_TOPIC = "RoPosix/light";;


    private Constants() {

    }
}