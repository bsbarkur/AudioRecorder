# AudioRecorder

## Running

1. Install Android Studio
2. Import the project to the android studio
3. Click on `Build` in the navigation side bar
4. Start `Run`.
5. When running, connect to the physical device which has USB debugging enabled
6. Once the physical device is connected via USB port, studio discovers it via device manager.
7. Run the android app by selecting the physical device name when running.

## Operation of App

1. When the app starts, press on the Start button
2. A toast appears asking for file permissions and audio record permissions. Grant permission by pressing on Yes.
3. Now we can start recording the audio by pressing on the `START` button.
4. When you are done press the `STOP` button. Toast that says `Recoding ended` appears.
5. The recording gets saved to `/sdcard/recording.pcm` on the device filesystem.

## SyncHelloClient

1. This app contains the GRPC oriented synchronous channel based client using TCP port that is specified.
2. This app can start either both on the physical and virtual devices.
3. The GRPC server code will be committed to seperate repo.
4. App can be installed on the device using either android studio (USB) or via the .apk generated in the build directory and shared to the physical device via the bluetooth sharing.
5. Input to be given on App is: Server IP of the GRPC server, Port number it is running on and the Message to be sent.
6. Once connection is established the GRPC server returns the message that is displayed on a TextView area in the app.

## Streaming Android Client

1. This does streaming channel connection using RPC service defined in teh record.proto file as per the requirements.
2. It makes connection to the IP/PORT specified in the Android Client as per the proto requirements
3. The server once it receives chunks replies back on the channel with "Data recieved" and final = 0
4. When all data is completed server sends final = 1 and "Data completed" back to the client
5. Ensure the manifest has the legacy file permission to store the recorded bytes
6. Ensure the manifest has permission to store access the device recoder device

## Server
1. The script audio-record-server is used to run the server java class compiled using gradle on my system.
2. The server is run on the specified port
3. Ensure firewall is turned off so that the server tcp port 8080 / 50051 (Default) are reachable from the mobile phone ip.
4. The server is in this code AudioRecorderServer.java
5. The proto is in `audiorecord.proto` which should be same as the proto used by the streaming android client above.
