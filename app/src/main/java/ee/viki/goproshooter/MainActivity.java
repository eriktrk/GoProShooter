package ee.viki.goproshooter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.UpdateLayout;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.toolbox.*;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.zxing.IntentIntegrator;
import com.google.zxing.IntentResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private RequestQueue queue;
    MyWifiManager myWifiMan;
    ArrayList<MyReqInterface> mainReqList;
    LinearLayout mainLayout;
    Button addBtn;
    final String TAG = "Main activity";
    final String savedListKey = "SavedList";
    public static String RESULT_DELETE = "delete";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWifiMan = new MyWifiManager(this);
        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        queue = Volley.newRequestQueue(this);
        addBtn = (Button) findViewById(R.id.addBtn);
        mainReqList = RestoreRequestList(savedInstanceState);
        RebuildLayout(mainLayout, mainReqList);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        final Activity currentActivity = this;
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyReqInterface newReq = CreateEmptyRequest();

                Intent createIntent = new Intent(getBaseContext(),EditRequestActivity.class);
                createIntent.putExtra("RequestObject", newReq);
                currentActivity.startActivityForResult(createIntent, 2);
                newReq = null;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://ee.viki.goproshooter/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        SaveListToFile();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://ee.viki.goproshooter/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Result OK, resultcode: " + resultCode);
        String parameter = data.getStringExtra("barcodeParameter");
        int h = data.getIntExtra("hash", -1);
        Log.i(TAG, "parameter: " + parameter + "\nhash: " + h);

        if(resultCode == RESULT_OK){
            MyReqInterface newReq = data.getParcelableExtra("result");


            if (requestCode == 1) {
                //UPDATE EXISTING
                for(MyReqInterface req : mainReqList){
                    //Log.i(TAG, "Comparing to request: " + req.hashCode());
                    if(req.hashCode() == newReq.getSourceHash()) {
                        //Log.i(TAG, "Found matching request in the list: " + req.hashCode());
                        //req.PrintCurrentProperties();
                        req.UpdateFromStringArray(newReq.GetAsParcelableStringArray());
                        //Log.i(TAG, "This activity base context package: " + this.getBaseContext().getPackageName());
                        req.RefreshLayoutItems(this);
                        //req.PrintCurrentProperties();
                        Toast.makeText(MainActivity.this, "Changes saved!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            else if(requestCode == 2){
                //ADD NEW
                MyReqInterface reqToAdd = CreateEmptyRequest();
                reqToAdd.UpdateFromStringArray(newReq.GetAsParcelableStringArray());
                reqToAdd.RefreshLayoutItems(this);
                AddRequestToList(reqToAdd, mainReqList);
                AddRequestToLayout(reqToAdd, mainLayout);
                Toast.makeText(MainActivity.this, "Request added!", Toast.LENGTH_SHORT).show();
            }


            if(parameter != null){
                h = data.getIntExtra("hash", -1);
                for(MyReqInterface req : mainReqList) {
                    //Log.i(TAG, "Comparing to request: " + req.hashCode());
                    if (req.hashCode() == h) {
                        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                        ArrayList<Pair<String, String>> l = new ArrayList<>();
                        l.add(new Pair<>(parameter, scanningResult.toString()));
                        req.setParamValList(l);
                        req.SendAll();
                        return;
                    }
                }
            }
        }
        else if(data != null) {
            String res = data.getExtras().getString("result");
            Log.i(TAG, "Extra 'result': " + res);
            MyReqInterface toBeRemoved = null;

            if(res != null){
                if(res.equals(RESULT_DELETE)){
                    int hashToDelete = data.getExtras().getInt("hash");
                    Log.i(TAG, "Extra 'hash': " + hashToDelete);
                    for(MyReqInterface r : mainReqList){
                        if(r.hashCode() == hashToDelete){
                            toBeRemoved = r;
                            break;
                        }
                    }
                    if(toBeRemoved != null) {
                        mainReqList.remove(toBeRemoved);
                        RebuildLayout(mainLayout,mainReqList);
                        Toast.makeText(MainActivity.this, "Request deleted!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        Log.i(TAG, "Saving request list...");
        savedInstanceState.putParcelableArrayList(savedListKey, mainReqList);
    }



    public static Object deserializeBytes(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bytesIn);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }


    public static byte[] serializeObject(Object obj) throws IOException
    {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
        oos.writeObject(obj);
        oos.flush();
        byte[] bytes = bytesOut.toByteArray();
        bytesOut.close();
        oos.close();
        return bytes;
    }

    private MyReqInterface CreateEmptyRequest(){
        MyReqInterface r = new MyReqInterface("", "", this);
        return r;
    }

    private void AddRequestToList(MyReqInterface r, ArrayList<MyReqInterface> list){
        list.add(r);
    }


    private ArrayList<MyReqInterface> RestoreRequestList(Bundle bundle){
        ArrayList<MyReqInterface> list = new ArrayList<>();
        Log.i(TAG, "Restoring request list...");
        //TODO: read the saved mainReqList from a file
        /*MyReqInterface request1 = new MyReqInterface("FindCode", "/control/", this);
        request1.AddIpAddress("192.168.43.150");
        request1.AddIpAddress("192.168.43.151");
        request1.AddIpAddress("192.168.43.152");
        request1.AddIpAddress("192.168.43.153");
        request1.ClearParams();
        request1.AddParamValPair(new Pair<>("findcode","asd"));

        MyReqInterface request2 = new MyReqInterface("SetCode", "/control/", this);
        request2.AddIpAddress("192.168.43.150");
        request2.AddIpAddress("192.168.43.151");
        request2.AddIpAddress("192.168.43.152");
        request2.AddIpAddress("192.168.43.153");
        request2.ClearParams();
        request2.AddParamValPair(new Pair<>("setcode","asd"));

        AddRequestToList(request1, list);
        AddRequestToList(request2, list);*/
        /*if(bundle == null)
            Log.i(TAG, "BUNDLE IS NULL!");
        try {
            list = bundle.getParcelableArrayList(savedListKey);
            for(MyReqInterface r : list){
                r.RefreshLayoutItems(this);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }*/

        ArrayList<String[]> al = RestoreListFromFile();

        if (al != null) {
            for(String[] sa : al){
                MyReqInterface r = CreateEmptyRequest();
                r.UpdateFromStringArray(sa);
                r.RefreshLayoutItems(this);
                list.add(r);
            }
        }

        return list;
    }

    private void SaveListToFile(){
        String FILENAME = savedListKey;
        ArrayList<String[]> sa = new ArrayList<>();

        for(MyReqInterface r : mainReqList){
            sa.add(r.GetAsParcelableStringArray());
        }

        try {
            byte[] ba = serializeObject(sa);
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(ba);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String[]> RestoreListFromFile(){
        String FILENAME = savedListKey;
        ArrayList<String[]> list = null;

        try {
            FileInputStream fis = openFileInput(FILENAME);
            byte[] bl = new byte[(int)fis.getChannel().size()];
            fis.read(bl);
            fis.close();

            list = (ArrayList<String[]>) deserializeBytes(bl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private void RebuildLayout(LinearLayout layout, ArrayList<MyReqInterface> list){
        layout.removeAllViews();

        for(MyReqInterface req : list){
            AddRequestToLayout(req, layout);
        }
    }

    private void AddRequestToLayout(MyReqInterface r, LinearLayout layout){
        layout.addView(r.GetLayout());
    }
    private void SaveRequestList(ArrayList<MyReqInterface> list){
        //TODO: save the mainReqList to a file
    }

    public RequestQueue GetRequestQueue(){
        return queue;
    }
}


