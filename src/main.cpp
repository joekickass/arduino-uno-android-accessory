#include <Usb.h>
#include <adk.h>

#define LED_PIN 3
#define LONG_BLINK 2000
#define SHORT_BLINK 500

int ledStatus = HIGH;

USB Usb;
ADK adk(&Usb,"JoeKickass",
            "AndroidApkTakeTwo",
            "ADK example application",
            "1.0",
            "http://joekickass.se",
            "0000000012345678");

void toggleLed() {
    digitalWrite(LED_PIN, ledStatus);
    ledStatus = (ledStatus == LOW) ? HIGH : LOW;
}

void blinkLed(int delayFactor) {
    toggleLed();
    delay(delayFactor);
    toggleLed();
    delay(delayFactor);
}

void errorBlink() {
    blinkLed(SHORT_BLINK);
}

void readMessage() {
    uint8_t msg[2] = { 0x00 };
    uint16_t len = sizeof(msg);
    adk.RcvData(&len, msg);
    if(len > 0) {
        if (msg[0] == 0xf) {
            toggleLed();
        }
    }
}

void setup() {
    pinMode(LED_PIN, OUTPUT);
    if (Usb.Init() == -1) {
        while(1) {
            errorBlink();
        }
    }
}

void loop() {
    Usb.Task();

    if (!adk.isReady()) {
        return;
    }
    
    readMessage();
}

int main(void) {
    init();
    setup();
    for (;;) {
        loop();
    }
}
