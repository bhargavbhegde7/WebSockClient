package android.learn.com.websocktry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    private boolean AUTO_SEND = true;
    private TextToSpeech engine;
    private EditText inputMessage;
    private TextView response;
    private TextView connectionState;
    private TextView requestMessage;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.10.1:8090/");
        } catch (URISyntaxException e) {
            System.out.println("Exception : "+e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputMessage = (EditText) findViewById(R.id.messageInput);
        response = (TextView) findViewById(R.id.responseText);
        connectionState = (TextView) findViewById(R.id.connectionState);
        requestMessage = (TextView) findViewById(R.id.requestMessage);
        TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    engine.setLanguage(Locale.UK);
                }
            }
        };
        engine = new TextToSpeech(this, listener);
        CheckBox autoSend = (CheckBox) findViewById(R.id.autoSend);
        autoSend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                @Override
                                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                    if(isChecked)
                                                        AUTO_SEND = true;
                                                    else
                                                        AUTO_SEND = false;
                                                }
                                            }
        );
    }

    private void setConnectionState(boolean state){
        connectionState.setText(state?"Connected":"Not Connected");
    }

    private void attemptSend(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        inputMessage.setText("");
        try {
            //emit the 'new-message' event
            mSocket.emit("new-message", message);
            requestMessage.setText(message);
        }catch(Exception e){
            System.out.println("Exception : "+e.getMessage());
        }
    }

    private void speech(String text) {
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public static Account getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        }
        return account;
    }

    public void onConnectClick(View view){
        try {
            //set listeners
            //handler to catch the 'connection' event from the server
            Emitter.Listener responseHandler = new Emitter.Listener() {
                String message = "";
                @Override
                public void call(final Object... args) {
                    message = (String) args[0];

                    //run the UI changes on the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            response.setText(message);
                            speech(message);
                        }
                    });
                }
            };

            //handler to catch the 'connection' event from the server
            Emitter.Listener connectionStateHandler = new Emitter.Listener() {
                String message = "";
                @Override
                public void call(final Object... args) {
                    message = (String) args[0];

                    //run the UI changes on the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if("on".equals(message.trim())) {
                                setConnectionState(true);
                                Account account = getAccount(AccountManager.get(getApplicationContext()));
                                mSocket.emit("username", account.name);
                            }
                            else
                                setConnectionState(false);
                        }
                    });
                }
            };

            mSocket.on("response", responseHandler);
            mSocket.on("connection-state", connectionStateHandler);

            mSocket.connect();
        }catch(Exception e){
            System.out.println("Exception : "+e.getMessage());
        }
    }

    public void onSendClicked(View view){
        String message = inputMessage.getText().toString().trim();
        attemptSend(message);
    }

    public void onMicClicked(View view){
        try {
            new VoiceDetector(this).promptSpeechInput();
        }catch(Exception e){
            System.out.println("Exception : "+e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //remove all listeners
            mSocket.off();
            mSocket.disconnect();
        }catch(Exception e){
            System.out.println("Exception : "+e.getMessage());
        }
    }

    /* Receiving speech input */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case VoiceDetector.REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String outputText = result.get(0);
                    if(AUTO_SEND) attemptSend(outputText);
                    else inputMessage.setText(outputText);

                }
                break;
            }
        }
    }

}
