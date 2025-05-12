#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

#define RED_PIN 4
#define GREEN_PIN 2
#define BLUE_PIN 15

int currentBrightness = 255; // Varsayılan maksimum parlaklık
String currentColor = "OFF";

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32_LED");

  pinMode(RED_PIN, OUTPUT);
  pinMode(GREEN_PIN, OUTPUT);
  pinMode(BLUE_PIN, OUTPUT);

  // Başlangıçta tüm LED'leri kapat
  digitalWrite(RED_PIN, HIGH);
  digitalWrite(GREEN_PIN, HIGH);
  digitalWrite(BLUE_PIN, HIGH);
}

void updateLEDs() {
  if (currentColor == "OFF") {
    analogWrite(RED_PIN, 255);
    analogWrite(GREEN_PIN, 255);
    analogWrite(BLUE_PIN, 255);
  } else {
    int brightness = map(currentBrightness, 0, 255, 255, 0); // Ters çevir çünkü 0 tam parlaklık
    
    if (currentColor == "RED") {
      analogWrite(RED_PIN, brightness);
      analogWrite(GREEN_PIN, 255);
      analogWrite(BLUE_PIN, 255);
    } else if (currentColor == "GREEN") {
      analogWrite(RED_PIN, 255);
      analogWrite(GREEN_PIN, brightness);
      analogWrite(BLUE_PIN, 255);
    } else if (currentColor == "BLUE") {
      analogWrite(RED_PIN, 255);
      analogWrite(GREEN_PIN, 255);
      analogWrite(BLUE_PIN, brightness);
    }
  }
}

void loop() {
  if (SerialBT.available()) {
    String cmd = SerialBT.readStringUntil('\n');
    cmd.trim();
    Serial.println("Gelen: " + cmd);

    if (cmd.startsWith("BRIGHTNESS:")) {
      currentBrightness = cmd.substring(11).toInt();
      updateLEDs();
    } else if (cmd == "RED" || cmd == "GREEN" || cmd == "BLUE" || cmd == "OFF") {
      currentColor = cmd;
      updateLEDs();
    }
  }
}
