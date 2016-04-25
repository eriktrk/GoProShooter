package ee.viki.goproshooter;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.api.BooleanResult;
import com.google.zxing.IntentIntegrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Lenovo on 18.04.2016.
 */
public class MyReqInterface implements Parcelable{

    private String Name;
    private String wifiSSID;
    private String wifiPass;
    private List<String> ipAddresses;
    private String addressPath;
    private List<Pair<String, String>> paramValList;
    private boolean barcodeEveryTime;

    private int sourceHash = 0;
    private String editBtnText = "Edit";
    private String TAG = "MyReqInterface";
    public static final String newLine = "\r\n";

    private LinearLayout InterfaceLayout;
    private Button SendButton, EditButton;
    private TextView statusText;
    private Activity parentActivity;

    //myWifiMan.connectToAP("GP_ET", "paroolparool");

    private void Init(String name, String path, Activity parent){
        ipAddresses=new ArrayList<>();
        paramValList = new ArrayList<>();
        this.Name = name;
        this.addressPath = path;
        parentActivity = parent;
        this.barcodeEveryTime=false;

        if(parentActivity != null)
            CreateLayoutItems();
    }

    public void RefreshLayoutItems(Activity parent){
        this.parentActivity = parent;

        if(SendButton != null)
            this.SendButton.setText(this.Name);

        if(EditButton != null)
            this.EditButton.setText(editBtnText);
    }

    public void CreateLayoutItems(){
        //Log.i(TAG, "Creating layout items and layout for request " + this.hashCode());
        //Buttons
        this.SendButton = new Button(parentActivity);
        this.SendButton.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT,1f));
        this.SendButton.setWidth(0);
        this.SendButton.setText(this.Name);
        this.SendButton.setTextSize(18);
        this.SendButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Toast.makeText(parentActivity, "Click and hold to send request!", Toast.LENGTH_SHORT).show();
            }
        });
        this.SendButton.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                if(barcodeEveryTime){
                    Pair<String, String> p = paramValList.get(0);
                    String var = p.first;
                    IntentIntegrator scanIntegrator = new IntentIntegrator(parentActivity);
                    scanIntegrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
                    scanIntegrator.setOrientationLocked(true);
                    Log.d(TAG, "first parameter: " + p.first);
                    scanIntegrator.addExtra("barcodeParameter", var);
                    scanIntegrator.addExtra("hash", this.hashCode());
                    paramValList.clear();
                    scanIntegrator.initiateScan();
                }
                else
                    SendAll();
                return true;
            }
        });

        this.EditButton = new Button(parentActivity);
        this.EditButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.EditButton.setText(editBtnText);
        this.EditButton.setTextSize(18);
        final MyReqInterface currentInstance = this;
        this.EditButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                currentInstance.setSourceHash();
                Intent editIntent = new Intent(parentActivity.getBaseContext(),EditRequestActivity.class);
                editIntent.putExtra("RequestObject", currentInstance);
                parentActivity.startActivityForResult(editIntent, 1);
            }
        });

        //Layout
        this.InterfaceLayout = new LinearLayout(parentActivity);
        this.InterfaceLayout.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.InterfaceLayout.setLayoutParams(params);
        this.InterfaceLayout.addView(SendButton);
        this.InterfaceLayout.addView(EditButton);

        //Log.i(TAG, "Request layout created!");
    }
    public MyReqInterface(String name, String path, Activity parent){
        Init(name, path, parent);
    }
    public MyReqInterface(String name, String path, String ssid , String pass, Activity parent ){
        this.wifiSSID = ssid;
        this.wifiPass = pass;
        Init(name, path, parent);
    }
    public MyReqInterface(Parcel in){
        String[] data = in.createStringArray();
        UpdateFromStringArray(data);
    }

    public void PrintCurrentProperties(){
        //Log.i(TAG, "Hash: " + this.hashCode());
        //Log.i(TAG, "Source hash: " + this.sourceHash);
        //Log.i(TAG, "Path: " + this.addressPath);
        //Log.i(TAG, "SSID: " + this.wifiSSID);
        //Log.i(TAG, "Pass: " + this.wifiPass);

        for(String ip : ipAddresses)
            Log.i(TAG, "IP: " + ip);

        for(Pair<String,String> p : paramValList)
            Log.i(TAG, "Parameter: " + p.first + "; Value: " + p.second);
    }

    public void UpdateFromStringArray(String[] data){
        //[0]:  Hash
        //[1]:  Name
        //[2]:  wifiSSID;
        //[3]:  wifiPass;
        //[4]:  ipAddresses;
        //[5]:  addressPath;
        //[6, 7]:  paramValList;
        //[8]: always scan barcode for value
        //Log.i(TAG, "Updating from a string array with length: " + data.length);
        this.sourceHash = Integer.parseInt(data[0]);
        this.Name = data[1];
        this.wifiSSID = data[2];
        this.wifiPass = data[3];
        Init(data[1], data[5], null);

        ClearIps();
        Scanner scanner = new Scanner(data[4]);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            //if(line.length() > 6)
                this.ipAddresses.add(line);
        }
        scanner.close();
        //Log.i(TAG, "Added " + ipAddresses.size() + " IPs!");

        ClearParams();
        //Log.i(TAG, "Parameter string: " + data[6]);
        //Log.i(TAG, "Value string: " + data[7]);

        Scanner scannerParam = new Scanner(data[6]);
        Scanner scannerVal = new Scanner(data[7]);
        while (scannerParam.hasNextLine() && scannerVal.hasNextLine()) {
            String p = scannerParam.nextLine();
            String v = scannerVal.nextLine();
            this.paramValList.add(new Pair<>(p,v));
        }
        scannerParam.close();
        scannerVal.close();
        //Log.i(TAG, "Added " + paramValList.size() + " param-val Pairs!");
        if(data.length > 8)
            this.barcodeEveryTime = Boolean.parseBoolean(data[8]);
        else
            this.barcodeEveryTime = false;
    }

    public LinearLayout GetLayout(){
        return InterfaceLayout;
    }
    public void SetStatusText(TextView st){
        statusText = st;
    }
    public void AddParamValPair(Pair<String, String> p){
        paramValList.add(p);
    }
    public void AddParamValPairList(List<Pair<String, String>> list){
        paramValList.addAll(list);
    }

    public void ClearParams(){
        paramValList = new ArrayList<>();
    }

    public void AddIpAddress(String ip){
        ipAddresses.add(ip);
    }

    public void AddIpAddressList(List<String> ips){
        ipAddresses.addAll(ips);
    }

    public void ClearIps(){
        ipAddresses = new ArrayList<>();
    }

    public void SendAll(){
        String allRequests = "";

        for(String req : GetRequestList()){
            Log.i(TAG, "Sending http request: " + req);
            allRequests += req + newLine;
            SendHttpRequest(req);
        }
        Toast.makeText(parentActivity, "Sent: " + newLine + allRequests.trim(), Toast.LENGTH_SHORT).show();
    }

    private List<String> GetRequestList(){
        List<String> rqs = new ArrayList<String>();
        String pathAndParam = "?" + paramValList.get(0).first + "=" + paramValList.get(0).second;

        for(int i=1;i<paramValList.size();i++){
            pathAndParam += "&" + paramValList.get(i).first + "=" + paramValList.get(i).second;
        }

        pathAndParam = addressPath + pathAndParam;
        //Log.i(TAG, "Path+Parameters: " + pathAndParam);

        for(String address : ipAddresses ){
            String rq = "http://" + address + pathAndParam;
            rqs.add(rq);
        }

        return rqs;
    }

    private void SendHttpRequest(String request) {
        //Log.d("Http request to be sent", request);

        // Request a string response from the provided URL.
        StringRequest stringRequest = null;
        try {
            stringRequest = new StringRequest(Request.Method.GET, request,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            String s = response.trim();
                            ReceiveHttpResponse(s, "");
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if(error.networkResponse != null){
                                ReceiveHttpResponse("Error " + error.networkResponse.statusCode, "");
                            }
                            else ReceiveHttpResponse("No response!", "");
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            ReceiveHttpResponse("Connection error!", "");
        }

        // Add the request to the RequestQueue.
        if(parentActivity != null) {
            ((MainActivity)parentActivity).GetRequestQueue().add(stringRequest);
        } else
            Log.e(TAG, "Parent activity is null!");
    }

    public void ReceiveHttpResponse(String respStr, String src) {
        Log.d("Http response", respStr);
        Toast.makeText(parentActivity, "Response (" + src + "):\r\n" + respStr, Toast.LENGTH_SHORT).show();
        if(statusText != null){
            statusText.setText(respStr);
        }
    }

    public String getName() {
        return Name;
    }
    public void setName(String name) {
        Name = name;
    }
    public String getWifiSSID() {
        return wifiSSID;
    }
    public void setWifiSSID(String wifiSSID) {
        this.wifiSSID = wifiSSID;
    }
    public String getWifiPass() {
        return wifiPass;
    }
    public void setWifiPass(String wifiPass) {
        this.wifiPass = wifiPass;
    }
    public List<String> getIpAddresses() {
        return ipAddresses;
    }
    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }
    public String getAddressPath() {
        return addressPath;
    }
    public void setAddressPath(String addressPath) {
        this.addressPath = addressPath;
    }
    public List<Pair<String, String>> getParamValList() {
        return paramValList;
    }
    public void setParamValList(List<Pair<String, String>> paramValList) {
        this.paramValList = paramValList;
    }
    public int getSourceHash() {
        return sourceHash;
    }
    public void setSourceHash() {
        this.sourceHash = this.hashCode();
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Log.i(TAG, "Writing object to parcel: " + GetAsParcelableStringArray());
        parcel.writeStringArray(GetAsParcelableStringArray());
    }

    public String[] GetAsParcelableStringArray(){
        //Log.i(TAG, "Converting object into parcelable string array...");

        String[] data = new String[9];
        //[1]:  Name
        //[2]:  wifiSSID;
        //[3]:  wifiPass;
        //[4]:  ipAddresses;
        //[5]:  addressPath;
        //[6, 7]:  paramValList;
        data[0] = Integer.toString(this.sourceHash);
        data[1] = this.Name;
        data[2] = this.wifiSSID;
        data[3] = this.wifiPass;
        data[5] = this.addressPath;
        data[4] = "";
        for(int i=0;i<ipAddresses.size();i++ ){
            data[4] += ipAddresses.get(i);
            if(i<ipAddresses.size()-1) data[4] += newLine;
            //Log.i(TAG, "Creating parcel string: IP nr" + i + ": " + ipAddresses.get(i));
            //Log.i(TAG, "IP String so far: " + data[4]);
        }
        ////Log.i(TAG, "Wrote " + ipAddresses.size() + " IPs!");

        data[6] = data[7] = "";
        for(int j=0;j< paramValList.size();j++){
            Pair<String,String> pv = this.paramValList.get(j);
            data[6] += pv.first;
            data[7] += pv.second;
            if(j<paramValList.size()-1){
                data[6] +=  newLine;
                data[7] +=  newLine;
            }
        }

        data[8] = Boolean.toString(barcodeEveryTime);
        Log.i(TAG, "data[8]: " + data[8]);

        return data;
    }

    public static final Parcelable.Creator<MyReqInterface> CREATOR = new Parcelable.Creator<MyReqInterface>() {

        @Override
        public MyReqInterface createFromParcel(Parcel source) {
            return new MyReqInterface(source);  //using parcelable constructor
        }

        @Override
        public MyReqInterface[] newArray(int size) {
            return new MyReqInterface[size];
        }
    };
}
