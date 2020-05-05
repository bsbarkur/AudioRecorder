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

