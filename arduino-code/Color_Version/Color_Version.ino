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
    int currentRed;
    int currentGreen;
    int currentBlue;
    bool isOn;
};

RoomLED rooms[3] = {
    {ROOM1_RED_PIN, ROOM1_GREEN_PIN, ROOM1_BLUE_PIN, 255, 255, 255, 255, false},
    {ROOM2_RED_PIN, ROOM2_GREEN_PIN, ROOM2_BLUE_PIN, 255, 255, 255, 255, false},
    {ROOM3_RED_PIN, ROOM3_GREEN_PIN, ROOM3_BLUE_PIN, 255, 255, 255, 255, false}
};

bool emergencyMode = false;
unsigned long lastEmergencyToggle = 0;
bool emergencyLedState = false;

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
    if (!room.isOn) {
        analogWrite(room.redPin, 255);
        analogWrite(room.greenPin, 255);
        analogWrite(room.bluePin, 255);
    } else {
        int brightness = map(room.currentBrightness, 0, 255, 255, 0);
        int redValue = map(room.currentRed, 0, 255, 255, 0);
        int greenValue = map(room.currentGreen, 0, 255, 255, 0);
        int blueValue = map(room.currentBlue, 0, 255, 255, 0);
        
        // Apply brightness to RGB values
        redValue = map(redValue, 0, 255, 255, brightness);
        greenValue = map(greenValue, 0, 255, 255, brightness);
        blueValue = map(blueValue, 0, 255, 255, brightness);
        
        analogWrite(room.redPin, redValue);
        analogWrite(room.greenPin, greenValue);
        analogWrite(room.bluePin, blueValue);
    }
}

void activateEmergencyMode() {
    emergencyMode = true;
    emergencyLedState = true;
    lastEmergencyToggle = millis();
    
    // Turn all LEDs red
    for (int i = 0; i < 3; i++) {
        analogWrite(rooms[i].redPin, 0);
        analogWrite(rooms[i].greenPin, 255);
        analogWrite(rooms[i].bluePin, 255);
    }
    
    // Activate buzzer
    digitalWrite(BUZZER_PIN, HIGH);
}

void updateEmergencyState() {
    if (emergencyMode && (millis() - lastEmergencyToggle >= 1000)) {
        emergencyLedState = !emergencyLedState;
        lastEmergencyToggle = millis();
        
        for (int i = 0; i < 3; i++) {
            if (emergencyLedState) {
                analogWrite(rooms[i].redPin, 0);
                analogWrite(rooms[i].greenPin, 255);
                analogWrite(rooms[i].bluePin, 255);
            } else {
                analogWrite(rooms[i].redPin, 255);
                analogWrite(rooms[i].greenPin, 255);
                analogWrite(rooms[i].bluePin, 255);
            }
        }
    }
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
                
                if (cmd.startsWith("RGB:")) {
                    // Parse RGB values: RGB:red:green:blue:roomId
                    int firstColon = cmd.indexOf(':');
                    int secondColon = cmd.indexOf(':', firstColon + 1);
                    int thirdColon = cmd.indexOf(':', secondColon + 1);
                    
                    if (firstColon != -1 && secondColon != -1 && thirdColon != -1) {
                        int red = cmd.substring(firstColon + 1, secondColon).toInt();
                        int green = cmd.substring(secondColon + 1, thirdColon).toInt();
                        int blue = cmd.substring(thirdColon + 1, cmd.length() - 2).toInt();
                        
                        rooms[roomIndex].currentRed = red;
                        rooms[roomIndex].currentGreen = green;
                        rooms[roomIndex].currentBlue = blue;
                        rooms[roomIndex].isOn = true;
                        updateLED(roomIndex);
                    }
                } else if (cmd.startsWith("OFF")) {
                    rooms[roomIndex].isOn = false;
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
    
    updateEmergencyState();
}
