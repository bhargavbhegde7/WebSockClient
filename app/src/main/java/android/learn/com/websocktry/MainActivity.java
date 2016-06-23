package android.learn.com.websocktry;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity{

    private EditText inputMessage;
    private TextView response;
    private TextView connectionState;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.10.16:8090/");
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
                    if("on".equals(message.trim()))
                        setConnectionState(true);
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
        }catch(Exception e){
            System.out.println("Exception : "+e.getMessage());
        }
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

}
