package com.example.testandroid;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.examples.audiorecord.AudioTranscribeGrpc;
import io.grpc.examples.audiorecord.AudioBlob;
import io.grpc.examples.audiorecord.TranscribedText;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;

import static java.nio.ByteBuffer.allocateDirect;

public class MainActivity extends AppCompatActivity {
    private Button sendButton;
    private EditText hostEdit;
    private EditText portEdit;
    private TextView resultText;

    // audio record parameters
    private static final int SAMPLING_RATE_IN_HZ = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;

    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    private static final Logger logger = Logger.getLogger(MainActivity.class.getName());

    private AudioRecord recorder = null;
    private Thread recordingThread = null;

    private boolean flag = false;

    // Permissions
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private final int MY_PERMISSIONS_WRITE_STORAGE = 1;
    private Button stopButton;
    private Handler mHandler;

    // audio permissions
    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_STORAGE);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }


        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            flag = true;
            System.out.print("Flag is true now ");
        }
    }

    // file permissions
    private void requestFileAccess() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_STORAGE
            );
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
//                    recordAudio();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    logger.info("Permissions Denied to record audio");
                }
                return;
            }
        }
    }

    public void onRequestFilePermissionsResult(int requestCode,
                                               String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
//                    recordAudio();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record file", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void prepareForRecording() {

        requestAudioPermissions();
        requestFileAccess();

        if (flag) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE_IN_HZ,
                    CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            int initializedCorrectly = recorder.getState();
            logger.info("and status is now : " + initializedCorrectly);
//            Toast.makeText(getApplicationContext(), "Audio Recorder successfully", Toast.LENGTH_LONG).show();
            recorder.startRecording();
            recordingInProgress.set(true);

            // starts recorder as thread
            recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
            recordingThread.start();
        }
    }


    private void stopRecording() {
        if (null == recorder) {
            return;
        }
        recordingInProgress.set(false);
        recorder.stop();
//        super.onStop();
        recorder.release();
        recorder = null;
        recordingThread = null;
//        stopButton.setEnabled(true);
    }


    private class RecordingRunnable implements Runnable {
        //        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
//            final File file = new File(Environment.getExternalStorageDirectory() + "/recording.pcm");
            final File file = new File("/sdcard/recording_new.pcm");
            final ByteBuffer buffer = allocateDirect(BUFFER_SIZE);
            String host = "192.168.1.35";
            final CountDownLatch done = new CountDownLatch(1);
//            byte[] bufferarray = null;

            // This code does audio recording
            try (final FileOutputStream outStream = new FileOutputStream(file)) {

                while (recordingInProgress.get()) {

                    int result = recorder.read(buffer, BUFFER_SIZE);
//                    byte[] bufferarray = new byte[BUFFER_SIZE]; //recorded data is stored in this byte array
//                    recorder.read(bufferarray, 0, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, buffer.array().length);

//                    buffer.rewind();
//                    logger.info("Byte Buffer");
//                    while (buffer.hasRemaining()) {
//                        logger.info(buffer.position() + " -> " + buffer.get());
//
//                        String a = Integer.toString(buffer.position());
//                        ByteString bsVal = ByteString.copyFrom(buffer.array(), 0, buffer.array().length);
////                    request = HelloRequest.newBuilder().setName(a).setChunk(bsVal).build();
////                    response = stub.sayHello(request);
////                    logger.info(response.getMessage());
//                    }
//                    buffer.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress("192.168.1.35", 8080)
                    .usePlaintext()
                    .build();
            AudioTranscribeGrpc.AudioTranscribeStub stub = AudioTranscribeGrpc.newStub(channel);

            // When using manual flow-control and back-pressure on the client, the ClientResponseObserver handles both
            // request and response streams.

            ClientResponseObserver<AudioBlob, TranscribedText> clientResponseObserver =
                    new ClientResponseObserver<AudioBlob, TranscribedText>() {

                        ClientCallStreamObserver<AudioBlob> requestStream;

                        @Override
                        public void beforeStart(final ClientCallStreamObserver<AudioBlob> requestStream) {
                            this.requestStream = requestStream;
                            requestStream.disableAutoInboundFlowControl();
                            requestStream.setOnReadyHandler(new Runnable() {

                                // here pass the streaming data from audiorecorder
                                Iterator<String> iterator = names().iterator();

                                @Override
                                public void run() {
                                    // Start generating values from where we left off on a non-gRPC thread.
                                    while (requestStream.isReady()) {
                                        if (iterator.hasNext()) {
                                            // Send more messages if there are more messages to send.
                                            String name = iterator.next();
                                            logger.info("--> " + name);
                                            ByteString val = ByteString.copyFrom(buffer.array());

                                            // Proto relevant msg request send goes on here
                                            AudioBlob request = AudioBlob.newBuilder().setChunk(val).build();
                                            requestStream.onNext(request);
                                        } else {
                                            // Signal completion if there is nothing left to send.
                                            requestStream.onCompleted();
                                        }
                                    }
                                }
                            });
                        }

                        @Override
                        public void onNext(TranscribedText value) {
                            logger.info("data from server is <-- " + value.getText());
                            logger.info("final is : " + String.valueOf(value.getFinal()));
                            // Signal the sender to send one message.
                            requestStream.request(1);
                        }

                        @Override
                        public void onError(Throwable t) {
                            t.printStackTrace();
                            done.countDown();
                        }

                        @Override
                        public void onCompleted() {
                            logger.info("All Done");
//
//                            logger.info("final is : " + String.valueOf(value.getFinal()));
//                            alertDialog();
//                            HandlerThread uiThread = new HandlerThread("UIHandler");
//                            uiThread.start();
//                            uiHandler = new UIHandler((HandlerThread) uiThread.getLooper());
                            Looper.prepare(); // to be able to make toast
                            Toast.makeText(MainActivity.this, "DATA COMPLETED", Toast.LENGTH_LONG).show();
                            stopRecording();
                            done.countDown();
                            Looper.loop();
                        }
                    };

            // Handling both request and response stream processing.
            stub.speechToText(clientResponseObserver);

            try {
                done.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            channel.shutdown();
            try {
                channel.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

////           /* GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
//                System.out.println("sync started");
//                HelloRequest request;
//                    HelloReply response;
//            try (final FileOutputStream outStream = new FileOutputStream(file)) {
//                while (recordingInProgress.get()) {
//
//                    int result = recorder.read(buffer, BUFFER_SIZE);
////                    byte[] bufferarray = new byte[BUFFER_SIZE]; //recorded data is stored in this byte array
////                    recorder.read(bufferarray, 0, BUFFER_SIZE);
//                    if (result < 0) {
//                        throw new RuntimeException("Reading of audio buffer failed: " +
//                                getBufferReadFailureReason(result));
//                    }
//                    outStream.write(buffer.array(), 0, buffer.array().length);
//
//                    buffer.rewind();
//                    logger.info("Byte Buffer");
//                    while (buffer.hasRemaining()) {
//                        logger.info(buffer.position() + " -> " + buffer.get());
//
//                        String a = Integer.toString(buffer.position());
//                        ByteString bsVal=ByteString.copyFrom(buffer.array(),0,buffer.array().length);
//                        request = HelloRequest.newBuilder().setName(a).setChunk(bsVal).build();
//                        response = stub.sayHello(request);
//                        logger.info(response.getMessage());
//                    }
//                    buffer.clear();
//                }
//            } catch (IOException e) {
//                throw new RuntimeException("Writing of recorded audio failed", e);
//            }
//            stopButton.setEnabled(true);
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendButton = (Button) findViewById(R.id.send_button);
        hostEdit = (EditText) findViewById(R.id.host_edit_text);
        portEdit = (EditText) findViewById(R.id.port_edit_text);
//        messageEdit = (EditText) findViewById(R.id.message_edit_text);
        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());

//        mHandler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(Message message) {
//                // This is where you do your work in the UI thread.
//                // Your worker tells you in the message what to do.
//            }
//        };
//        stopButton = (Button) findViewById(R.id.stopbutton);

//        stopButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stopRecording();
//                sendButton.setEnabled(true);
//                stopButton.setEnabled(true);
//            }
//        });
    }

    public void stopMessage(View view) {
        stopRecording();
        sendButton.setEnabled(true);
        stopButton.setEnabled(true);
    }

    public void sendMessage(View view) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(hostEdit.getWindowToken(), 0);
        sendButton.setEnabled(false);
//        stopButton.setEnabled(false);
        resultText.setText("");
        new GrpcTask(this)
                .execute(
                        hostEdit.getText().toString(),
//                        messageEdit.getText().toString(),
                        portEdit.getText().toString());
    }

    class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;
//        private GreeterGrpc.GreeterBlockingStub blockingStub;

        private GrpcTask(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String host = params[0];
//            String message = params[1];
            String portStr = params[1];
//            String host = "192.168.1.35";
////            String message = params[1];
//            String portStr = "8080";
            final String[] msg = {""};
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);

            prepareForRecording();

//            try {
//                channel = ManagedChannelBuilder.forAddress(host, 8080).usePlaintext().build();
//                GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
//                System.out.println("sync started");
//                HelloRequest request = HelloRequest.newBuilder().setName(message).build();
//                HelloReply response = stub.sayHello(request);
//                return response.getMessage();
//            } catch (Exception e) {
//                StringWriter sw = new StringWriter();
//                PrintWriter pw = new PrintWriter(sw);
//                e.printStackTrace(pw);
//                pw.flush();
//                System.out.println(sw.toString());
//                return String.format("Failed... : %n%s", sw);
//            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            try {
//                channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
                TextView resultText = (TextView) activity.findViewById(R.id.grpc_response_text);
                Button sendButton = (Button) activity.findViewById(R.id.send_button);

                // Sets the response here
                resultText.setText(result);
                sendButton.setEnabled(true);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }

//            TextView resultText = (TextView) activity.findViewById(R.id.grpc_response_text);
//            Button sendButton = (Button) activity.findViewById(R.id.send_button);
//            resultText.setText(result);
//            sendButton.setEnabled(true);
        }
    }

    private static List<String> names() {
        return Arrays.asList(
                "Sophia",
                "Jackson",
                "Emma",
                "Aiden",
                "Olivia",
                "Lucas",
                "Ava",
                "Liam",
                "Mia",
                "Noah",
                "Isabella",
                "Ethan",
                "Riley",
                "Mason",
                "Aria",
                "Caden",
                "Zoe",
                "Oliver",
                "Charlotte",
                "Elijah",
                "Lily",
                "Grayson",
                "Layla",
                "Jacob",
                "Amelia",
                "Michael",
                "Emily",
                "Benjamin",
                "Madelyn",
                "Carter",
                "Aubrey",
                "James",
                "Adalyn",
                "Jayden",
                "Madison",
                "Logan",
                "Chloe",
                "Alexander",
                "Harper",
                "Caleb",
                "Abigail",
                "Ryan",
                "Aaliyah",
                "Luke",
                "Avery",
                "Daniel",
                "Evelyn",
                "Jack",
                "Kaylee",
                "William",
                "Ella",
                "Owen",
                "Ellie",
                "Gabriel",
                "Scarlett",
                "Matthew",
                "Arianna",
                "Connor",
                "Hailey",
                "Jayce",
                "Nora",
                "Isaac",
                "Addison",
                "Sebastian",
                "Brooklyn",
                "Henry",
                "Hannah",
                "Muhammad",
                "Mila",
                "Cameron",
                "Leah",
                "Wyatt",
                "Elizabeth",
                "Dylan",
                "Sarah",
                "Nathan",
                "Eliana",
                "Nicholas",
                "Mackenzie",
                "Julian",
                "Peyton",
                "Eli",
                "Maria",
                "Levi",
                "Grace",
                "Isaiah",
                "Adeline",
                "Landon",
                "Elena",
                "David",
                "Anna",
                "Christian",
                "Victoria",
                "Andrew",
                "Camilla",
                "Brayden",
                "Lillian",
                "John",
                "Natalie",
                "Lincoln"
        );
    }

    private void alertDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("Data completely received at Server");
        dialog.setTitle("Dialog Box");
        dialog.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Toast.makeText(getApplicationContext(), "Yes is clicked", Toast.LENGTH_LONG).show();
                    }
                });
        dialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "cancel is clicked", Toast.LENGTH_LONG).show();
            }
        });
        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }
}
