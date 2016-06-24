package android.learn.com.websocktry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private boolean AUTO_SEND = true;

    private EditText inputMessage;
    private TextView response;
    private TextView connectionState;
    private TextView requestMessage;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.10.18:8090/");
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

    private void addToView(String message){
        response.setText(message);
    }

    private void setConnectionState(boolean state){
        connectionState.setText(state?"Connected":"Not Connected");
    }

    //handler to catch the 'connection' event from the server
    private Emitter.Listener responseHandler = new Emitter.Listener() {
        String message = "";
        @Override
        public void call(final Object... args) {
            message = (String) args[0];

            //run the UI changes on the UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addToView(message);
                }
            });
        }
    };

    //handler to catch the 'connection' event from the server
    private Emitter.Listener connectionStateHandler = new Emitter.Listener() {
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
            promptSpeechInput();
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

    /* for speech input */
    /* Showing google speech input dialog */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "speak the words");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "speech not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /* Receiving speech input */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
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
    /* for speech input */

}
