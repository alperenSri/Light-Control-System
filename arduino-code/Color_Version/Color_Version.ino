#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

// LED pins for each room
#define ROOM1_RED_PIN 4
#define ROOM1_GREEN_PIN 2
#define ROOM1_BLUE_PIN 15

#define ROOM2_RED_PIN 16
#define ROOM2_GREEN_PIN 17
#define ROOM2_BLUE_PIN 5

#define ROOM3_RED_PIN 18
#define ROOM3_GREEN_PIN 19
#define ROOM3_BLUE_PIN 21

#define BUZZER_PIN 23

struct RoomLED {
    int redPin;
    int greenPin;
    int bluePin;
    int currentBrightness;
    String currentColor;
};

RoomLED rooms[3] = {
    {ROOM1_RED_PIN, ROOM1_GREEN_PIN, ROOM1_BLUE_PIN, 255, "OFF"},
    {ROOM2_RED_PIN, ROOM2_GREEN_PIN, ROOM2_BLUE_PIN, 255, "OFF"},
    {ROOM3_RED_PIN, ROOM3_GREEN_PIN, ROOM3_BLUE_PIN, 255, "OFF"}
};

bool emergencyMode = false;

void setup() {
    Serial.begin(115200);
    SerialBT.begin("ESP32_LED");

    // Initialize all LED pins
    for (int i = 0; i < 3; i++) {
        pinMode(rooms[i].redPin, OUTPUT);
        pinMode(rooms[i].greenPin, OUTPUT);
        pinMode(rooms[i].bluePin, OUTPUT);
        
        digitalWrite(rooms[i].redPin, HIGH);
        digitalWrite(rooms[i].greenPin, HIGH);
        digitalWrite(rooms[i].bluePin, HIGH);
    }

    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);
}

void updateLED(int roomIndex) {
    if (emergencyMode) return;

    RoomLED &room = rooms[roomIndex];
    if (room.currentColor == "OFF") {
        analogWrite(room.redPin, 255);
        analogWrite(room.greenPin, 255);
        analogWrite(room.bluePin, 255);
    } else {
        int brightness = map(room.currentBrightness, 0, 255, 255, 0);
        
        if (room.currentColor == "RED") {
            analogWrite(room.redPin, brightness);
            analogWrite(room.greenPin, 255);
            analogWrite(room.bluePin, 255);
        } else if (room.currentColor == "GREEN") {
            analogWrite(room.redPin, 255);
            analogWrite(room.greenPin, brightness);
            analogWrite(room.bluePin, 255);
        } else if (room.currentColor == "BLUE") {
            analogWrite(room.redPin, 255);
            analogWrite(room.greenPin, 255);
            analogWrite(room.bluePin, brightness);
        }
    }
}

void activateEmergencyMode() {
    emergencyMode = true;
    
    // Turn all LEDs red
    for (int i = 0; i < 3; i++) {
        analogWrite(rooms[i].redPin, 0);
        analogWrite(rooms[i].greenPin, 255);
        analogWrite(rooms[i].bluePin, 255);
    }
    
    // Activate buzzer
    digitalWrite(BUZZER_PIN, HIGH);
}

void deactivateEmergencyMode() {
    emergencyMode = false;
    digitalWrite(BUZZER_PIN, LOW);
    
    // Restore previous LED states
    for (int i = 0; i < 3; i++) {
        updateLED(i);
    }
}

void loop() {
    if (SerialBT.available()) {
        String cmd = SerialBT.readStringUntil('\n');
        cmd.trim();
        Serial.println("Received: " + cmd);

        if (cmd == "EMERGENCY") {
            activateEmergencyMode();
        } else if (cmd == "EMERGENCY_OFF") {
            deactivateEmergencyMode();
        } else {
            // Handle room-specific commands
            if (cmd.endsWith("1") || cmd.endsWith("2") || cmd.endsWith("3")) {
                int roomIndex = (cmd.charAt(cmd.length() - 1) - '1');
                String command = cmd.substring(0, cmd.length() - 1);

                if (command == "RED" || command == "GREEN" || command == "BLUE" || command == "OFF") {
                    rooms[roomIndex].currentColor = command;
                    updateLED(roomIndex);
                }
            } else if (cmd.startsWith("BRIGHTNESS:")) {
                int separatorIndex = cmd.lastIndexOf(':');
                if (separatorIndex != -1 && separatorIndex < cmd.length() - 1) {
                    int roomIndex = (cmd.charAt(cmd.length() - 1) - '1');
                    int brightness = cmd.substring(11, separatorIndex).toInt();
                    rooms[roomIndex].currentBrightness = brightness;
                    updateLED(roomIndex);
                }
            }
        }
    }
}
