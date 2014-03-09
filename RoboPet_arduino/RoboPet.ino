
#include <SoftwareSerial.h>  
#include <ArduinoRobot.h>
#include <Servo.h>


int bluetoothTx = MISO;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = MOSI;  // RX-I pin of bluetooth mate, Arduino D3

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

//laser stuff:
int laserPosY_port = TKD4;
int laserPosX_port = LCD_CS;

//int laser_port     = D3;
int laser_port     = TKD3;


// State the laser system (ON/OFF)
int laserStatus;

Servo servoY;  // create servo object to control a servo
Servo servoX;  // create servo object to control a servo

//Safety margin for the servos
int laserBoundMinY = 45;  // Do not go below 30 degrees to avoid the vertical servo to collide with the structure
int laserBoundMaxY = 100;
int laserBoundMinX  = 5;
int laserBoundMaxX  = 170;

// Laser default position (degrees)
int laserDefaultPosY = 90;
int laserDefaultPosX  = 90;

int laserVelX = random( -5, 5 );
int laserVelY = random( -5, 5 );

int robotTimeTick=500;



//COMMANDS TABLE
int TOGGLELASER_CMD=        (int)'5';
int ROBOTRIGHT_CMD=         (int)'6';
int ROBOTLEFT_CMD=          (int)'4';
int ROBOTFORWARD_CMD=       (int)'8';
int ROBOTBACKWARD_CMD=      (int)'2';
int ROBOTSTOP_CMD=          (int)'0';




void setup()
{
  
  delay(10000);
  Robot.begin();
  
  Serial.begin(9600);  
  Serial.print("Enter setup..."); 

  bluetooth.begin(115200);  
  bluetooth.print("$");  
  bluetooth.print("$");
  bluetooth.print("$");  // Enter command mode
  delay(100);
  bluetooth.println("U,9600,N");  
  bluetooth.begin(9600);
  
  Serial.println("[END]"); 
  
  //laser stuff
  servoY.attach(laserPosY_port);
  servoX.attach(laserPosX_port);
  toggleLaser(0);
  
}

void loop(){
  
  if(bluetooth.available())  // If the bluetooth sent any characters
  {
   
    char read_fromBT=bluetooth.read();
    int command =(int) read_fromBT;
    

    Serial.print("RECEIVED FORM BT: ");
    Serial.print(read_fromBT);
    Serial.print(" ==> ");
    Serial.println(command);
    
    
    if(command==TOGGLELASER_CMD) { //laser on/off
      Serial.println("TOGGLE LASER");
      toggleLaser(1);
    }
    
    else if(command==ROBOTFORWARD_CMD) {
      Serial.println("MOVE FORWARD");
      //move fwd:
      int val=map(Robot.knobRead(),0,1023,-255,255);
      Robot.motorsWrite(val,val);   
      delay(robotTimeTick);
      Robot.motorsStop();           
      
    }
    
    else if(command==ROBOTBACKWARD_CMD) {
      Serial.println("MOVE BACKWARD");
      //move backwd:
      int val=map(Robot.knobRead(),0,1023,-255,255);
      Robot.motorsWrite(-val,-val);   
      delay(robotTimeTick);
      Robot.motorsStop();           
      
    }
    
    else if(command==ROBOTLEFT_CMD) {
      Serial.println("TURN LEFT");
      //move left:
      int val=map(Robot.knobRead(),0,1023,-255,255);
      Robot.motorsWrite(-val,val);   
      delay(robotTimeTick);
      Robot.motorsStop();           
      
    }
    
    else if(command==ROBOTRIGHT_CMD) {
      //move right:
      Serial.println("TURN RIGHT");
      int val=map(Robot.knobRead(),0,1023,-255,255);
      Robot.motorsWrite(val,-val);   
      delay(robotTimeTick);
      Robot.motorsStop();           
      
    }
    
    else if(command==ROBOTSTOP_CMD) {
      Serial.println("HALT MOTORS");
      //FAST STOP:
      Robot.motorsStop();
      delay(1000);
    }
    else{
      Serial.println("COMMAND NOT RECOGNIZED!");
    }
    
  }
  
  //manage laser movements
  if(laserStatus==HIGH) {
    randomLaserPosition();
  }

}

void randomLaserPosition() {

  if ( laserVelX > 5) {
    laserVelX = laserVelX + random( -1, 0 );
  } else if ( laserVelX < -5) {
    laserVelX = laserVelX + random( 0, 1 );
  } else {
    laserVelX = laserVelX + random( -1, 1 );
  }
  
  if ( laserVelY > 3) {
    laserVelY = laserVelY + random( -1, 0 );
  } else if ( laserVelY < -3) {
    laserVelY = laserVelY + random( 0, 1 );
  } else {
    laserVelY = laserVelY + random( -1, 1 );
  }

  int posX = servoX.read();
  int posY = servoY.read();
  
  if ( posX+laserVelX < laserBoundMinX || posX+laserVelX > laserBoundMaxX ) {
    laserVelX = -laserVelX;
  }
  if ( posY+laserVelY < laserBoundMinY || posY+laserVelY > laserBoundMaxY ) {
    laserVelY = -laserVelY;
  }
  
  servoX.write(posX+laserVelX);
  servoY.write(posY+laserVelY);
  
  int randDelay = random( 40, 300 );
  delay(randDelay);
}



void toggleLaser(int init){
  if (init!=0){
    if (laserStatus == HIGH){
      Serial.print("LaserSwitchOFF()...");
      laserStatus=LOW;
    }
    else{
      Serial.print("LaserSwitchON()...");
      laserStatus=HIGH;
    }
  }
  else{
    //useful to init the laser
    Serial.print("Initializing laser()...");
    laserStatus=LOW;
  }
  
  Robot.digitalWrite(laser_port, laserStatus);
  
  servoX.write(laserDefaultPosX);
  servoY.write(laserDefaultPosY);
  Serial.println("[DONE!]");
}



