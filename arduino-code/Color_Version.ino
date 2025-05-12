#include "BluetoothSerial.h"
BluetoothSerial SerialBT;
// test deneme 1 2

#define RED_PIN 4
#define GREEN_PIN 2
#define BLUE_PIN 15

void setup() {
    Serial.begin(115200);
    SerialBT.begin("ESP32_LED");//asaas

    pinMode(RED_PIN, OUTPUT);
    pinMode(GREEN_PIN, OUTPUT);
    pinMode(BLUE_PIN, OUTPUT);

    digitalWrite(RED_PIN, HIGH);
    digitalWrite(GREEN_PIN, HIGH);
    digitalWrite(BLUE_PIN, HIGH);
}

void loop() {
    if (SerialBT.available()) {
        String cmd = SerialBT.readStringUntil('\n');
        cmd.trim();
        Serial.println("Gelen: " + cmd);

        if (cmd == "RED") {
            analogWrite(RED_PIN, 0);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 255);
        } else if (cmd == "GREEN") {
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 0);
            analogWrite(BLUE_PIN, 255);
        } else if (cmd == "BLUE") {
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 0);
        } else if (cmd == "OFF") {
            analogWrite(RED_PIN, 255);
            analogWrite(GREEN_PIN, 255);
            analogWrite(BLUE_PIN, 255);
        }

        if (cmd.startsWith("BRIGHT:")) {
            int brightness = cmd.substring(7).toInt();
            brightness = constrain(brightness, 0, 255); // 0–255 arasında sınırla

            analogWrite(RED_PIN, 255 - brightness);
            analogWrite(GREEN_PIN, 255 - brightness);
            analogWrite(BLUE_PIN, 255 - brightness);
        }
    }
}