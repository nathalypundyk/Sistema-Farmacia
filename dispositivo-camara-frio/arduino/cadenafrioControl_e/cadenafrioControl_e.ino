#include <DallasTemperature.h>
#include <OneWire.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// DEFINICIONES DE VARIABLES
LiquidCrystal_I2C lcd(0x27,20,4); 
OneWire ourWire(2);                
DallasTemperature sensors(&ourWire); 
int pausa = 1000; 

const int pinFinCarrera = 6; 
const int pinLed = 13;       
const int pinBuzzer = 4;     // Pin digital conectado al buzzer

float umbralMax = 22;
float umbralMin = 18;

// Variables para el control del temporizador
unsigned long tiempoInicio = 0;
unsigned long segundosTranscurridos = 0;
bool temporizadorActivo = false;

// Variable de estado para alertas
String estadoalert = "";

void setup()
{ 
  pinMode(pinFinCarrera, INPUT_PULLUP); 
  pinMode(pinLed, OUTPUT);       
  pinMode(pinBuzzer, OUTPUT);    
  
  lcd.backlight();
  lcd.init();
  sensors.begin();   
  Serial.begin(9600);
}

void loop()
{  
  int estado = digitalRead(pinFinCarrera); 
  
  // 1. LEER COMUNICACIÓN DESDE PYTHON (Si hay comandos pendientes)
  if (Serial.available() > 0) {
    String comando = Serial.readStringUntil('\n');
    comando.trim();
    if (comando == "ALARMA_INTRUSO") {
      if (estado == LOW) {
        estadoalert = "ALERTA INTRUSO!";
        alerta();
      }
    }
    if (comando == "ACCESO_OK") {
      estadoalert = "ACCESO AUTORIZADO";
    }
    
  }

  sensors.requestTemperatures();   
  float temp = sensors.getTempCByIndex(0); 
  String puertaEst = "";

  if (estado == LOW) {
    digitalWrite(pinLed, HIGH); 
    puertaEst = "ABIERTA";
    
    if (!temporizadorActivo) {
      temporizadorActivo = true;
      tiempoInicio = millis();
    }
    segundosTranscurridos = (millis() - tiempoInicio) / 1000;

  } else {
    digitalWrite(pinLed, LOW);  
    puertaEst = "CERRADA";
    
    temporizadorActivo = false;
    segundosTranscurridos = 0;
  }
  
  // CONTROL DE TEMPERATURA Y ALARMA
  if (temp > umbralMax) {
    digitalWrite(pinBuzzer, HIGH); 
    estadoalert = "alert temp";
    alerta();
  } else if (estadoalert != "ALERTA INTRUSO!") {
    digitalWrite(pinBuzzer, LOW);  
    estadoalert = "ok";
  }

  // Conversión de segundos totales a Horas, Minutos y Segundos
  unsigned long horas = segundosTranscurridos / 3600;
  unsigned long minutos = (segundosTranscurridos % 3600) / 60;
  unsigned long segundos = segundosTranscurridos % 60;

  char bufferTiempo[9];
  sprintf(bufferTiempo, "%02lu:%02lu:%02lu", horas, minutos, segundos);

  if (strcmp(bufferTiempo, "00:05:00") >= 0) {
    estadoalert = "tiempo max-exed";
    alerta(); 
  }
  
  // --- ENVÍO DE DATOS ESTRUCTURADOS HACIA PYTHON ---
  // Formato: TEMP:xx.xx,PUERTA:ESTADO,TIEMPO:HH:MM:SS,EST:ESTADO
  Serial.print("TEMP:");
  Serial.print(temp);
  Serial.print(",PUERTA:");
  Serial.print(puertaEst);
  Serial.print(",TIEMPO:");
  Serial.print(bufferTiempo);
  Serial.print(",EST:");
  Serial.println(estadoalert);

  // Actualización de pantalla LCD física del Arduino
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("PUERTA: ");
  lcd.print(puertaEst);   

  lcd.setCursor(0,1);
  lcd.print("TEMP:");
  lcd.print(temp);   
  lcd.print(" 'C");

  lcd.setCursor(0,2);
  lcd.print("Tiempo:");
  lcd.print(bufferTiempo);   

  lcd.setCursor(0,3);
  lcd.print("Estado:");
  lcd.print(estadoalert);

  delay(pausa);
}

void alerta()
{
  unsigned long tiempoInicioAlerta = millis();
  while (millis() - tiempoInicioAlerta < 3000) {
    tone(pinBuzzer, 1000); 
    delay(400);          
    noTone(pinBuzzer);     
    delay(400);          
  }
}