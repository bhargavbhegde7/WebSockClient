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

import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private EditText mInputMessageView;
    private TextView responseView;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.10.16:3000/");
        } catch (URISyntaxException e) {
            System.out.println("Exception : "+e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInputMessageView = (EditText) findViewById(R.id.messageInput);
        responseView = (TextView) findViewById(R.id.responseText);
    }

    private Emitter.Listener onResponse = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            String message = (String) args[0];
            System.out.println("Response : "+message);
            //responseView.setText(message);
        }
    };

    private void attemptSend(String message) {

        if (TextUtils.isEmpty(message)) {
            return;
        }

        mInputMessageView.setText("");
        mSocket.emit("new message", message);
    }

    public void onConnectClick(View view){
        mSocket.on("response", onResponse);
        mSocket.connect();
    }

    public void onSendClicked(View view){
        String message = mInputMessageView.getText().toString().trim();
        attemptSend(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off("response", onResponse);
    }
}
