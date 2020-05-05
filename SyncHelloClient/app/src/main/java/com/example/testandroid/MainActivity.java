package com.example.testandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.ByteString;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;

public class MainActivity extends AppCompatActivity {
    private Button sendButton;
    private EditText hostEdit;
    private EditText portEdit;
    private EditText messageEdit;
    private TextView resultText;

    private static final Logger logger = Logger.getLogger(MainActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendButton = (Button) findViewById(R.id.send_button);
        hostEdit = (EditText) findViewById(R.id.host_edit_text);
        portEdit = (EditText) findViewById(R.id.port_edit_text);
        messageEdit = (EditText) findViewById(R.id.message_edit_text);
        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
    }

    public void sendMessage(View view) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(hostEdit.getWindowToken(), 0);
        sendButton.setEnabled(false);
        resultText.setText("");
        new GrpcTask(this)
                .execute(
                        hostEdit.getText().toString(),
                        messageEdit.getText().toString(),
                        portEdit.getText().toString());
    }

    class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;
        private GreeterGrpc.GreeterBlockingStub blockingStub;

        private GrpcTask(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String host = params[0];
            String message = params[1];
            String portStr = params[2];
//            String host = "192.168.1.35";
//            String message = params[1];
//            String portStr = "8080";
            final String[] msg = {""};
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);
            try {
                channel = ManagedChannelBuilder.forAddress(host, 8080).usePlaintext().build();
                GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
                System.out.println("sync started");
                HelloRequest request = HelloRequest.newBuilder().setName(message).build();
                HelloReply response = stub.sayHello(request);
                return response.getMessage();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                System.out.println(sw.toString());
                return String.format("Failed... : %n%s", sw);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            try {
                channel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
                TextView resultText = (TextView) activity.findViewById(R.id.grpc_response_text);
                Button sendButton = (Button) activity.findViewById(R.id.send_button);

                // Sets the response here
                resultText.setText(result);
                sendButton.setEnabled(true);
            } catch (InterruptedException e) {
                Thread.currentThread ().interrupt();
            }

//            TextView resultText = (TextView) activity.findViewById(R.id.grpc_response_text);
//            Button sendButton = (Button) activity.findViewById(R.id.send_button);
//            resultText.setText(result);
//            sendButton.setEnabled(true);
        }
    }
}
