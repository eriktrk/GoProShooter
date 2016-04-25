package ee.viki.goproshooter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.*;

import org.w3c.dom.Text;


public class EditRequestActivity extends Activity {

    private EditText nameText, ipsText, pathText, varsText, valsText, ssidText, passText;
    private TextView previewText;
    private Button scanValBtn;
    private CheckBox barcodeCheck;
    private static MyReqInterface RequestInterface;
    private String TAG = "EditActivity";
    private boolean isNewRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editfields);
        isNewRequest = false;



        Intent i = getIntent();
        RequestInterface = i.getParcelableExtra("RequestObject");
        if(RequestInterface == null){
            Log.e(TAG, "Could not recreate Request object from Parcel!");
        }

        nameText = (EditText) findViewById(R.id.nameText);
        ipsText = (EditText) findViewById(R.id.ipsText);
        pathText = (EditText) findViewById(R.id.pathText);
        varsText = (EditText) findViewById(R.id.varsText);
        valsText = (EditText) findViewById(R.id.valsText);
        ssidText = (EditText) findViewById(R.id.ssidText);
        passText = (EditText) findViewById(R.id.passText);
        previewText = (TextView) findViewById(R.id.previewText);
        barcodeCheck= (CheckBox) findViewById(R.id.barcodeCheck);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String vars = varsText.getText().toString();
                int i = vars.indexOf("\r");
                if(i > -1)
                    vars = vars.substring(0,i);

                String vals = valsText.getText().toString();
                i = vals.indexOf("\r");
                if(i > -1)
                    vals = vals.substring(0,i);

                String ips = ipsText.getText().toString();
                i = ips.indexOf("\r");
                if(i > -1)
                    ips = ips.substring(0,i);

                String s = "http://" + ips + pathText.getText() + "?" + vars + "=" + vals;
                previewText.setText(s);
            }
        };

        ipsText.addTextChangedListener(tw);
        pathText.addTextChangedListener(tw);
        valsText.addTextChangedListener(tw);
        varsText.addTextChangedListener(tw);

        String[] data = RequestInterface.GetAsParcelableStringArray();
        nameText.setText(data[1]);
        ipsText.setText(data[4]);
        pathText.setText(data[5]);
        varsText.setText(data[6]);
        valsText.setText(data[7]);
        ssidText.setText(data[2]);
        passText.setText(data[3]);
        barcodeCheck.setChecked(Boolean.parseBoolean(data[8]));
        if(data[1].length() < 1) isNewRequest = true;


        Button mClose = (Button)findViewById(R.id.close);
        mClose.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result","cancel");
                setResult(Activity.RESULT_CANCELED,returnIntent);
                finish();
            }
        });

        Button mSave = (Button)findViewById(R.id.save);
        mSave.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if(saveChanges())
                    finish();
                else
                    Toast.makeText(EditRequestActivity.this, "Please fill in required fields!", Toast.LENGTH_SHORT).show();
            }
        });

        Button mDelete = (Button)findViewById(R.id.deleteBtn);
        mDelete.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                new AlertDialog.Builder(EditRequestActivity.this)
                        .setMessage("Are You really sure?")
                        .setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("result",MainActivity.RESULT_DELETE);
                                returnIntent.putExtra("hash",RequestInterface.getSourceHash());
                                setResult(Activity.RESULT_CANCELED,returnIntent);
                                finish();
                            }})
                        .setNegativeButton(R.string.NO, null).show();
            }
        });

        scanValBtn = (Button) findViewById(R.id.btnScan);
        scanValBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator scanIntegrator = new IntentIntegrator(EditRequestActivity.this);
                scanIntegrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
                scanIntegrator.setOrientationLocked(true);
                scanIntegrator.initiateScan();
            }
        });
    }


    /*private void RemoveListeners(){
        ipsText.removeTextChangedListener();
        pathText.addTextChangedListener(tw);
        valsText.addTextChangedListener(tw);
        varsText.addTextChangedListener(tw);
    }*/

    private boolean saveChanges(){
        String[] a = new String[]{
            Integer.toString(RequestInterface.getSourceHash()),
            nameText.getText().toString(),
            ssidText.getText().toString(),
            passText.getText().toString(),
            ipsText.getText().toString(),
            pathText.getText().toString(),
            varsText.getText().toString(),
            valsText.getText().toString(),
            Boolean.toString(barcodeCheck.isChecked())};

        //Log.i(TAG, "Saving using array, len:" + a.length);
        //for(String s : a)
            //Log.i(TAG, s);

        if(a[1].length() < 1 || a[4].length() < 1 || a[6].length() < 1 || (a[7].length() < 1 && !barcodeCheck.isChecked()))
            return false;

        RequestInterface.UpdateFromStringArray(a);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result",RequestInterface);
        setResult(Activity.RESULT_OK,returnIntent);
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(scanningResult != null){
            String scanContent = scanningResult.getContents();
            String valsCurrent = valsText.getText().toString();
            Toast.makeText(EditRequestActivity.this, "Last newline @ " + valsCurrent.lastIndexOf(MyReqInterface.newLine)
                    + "\n\r" + "Must be greater than " + (valsCurrent.length()-1-MyReqInterface.newLine.length()), Toast.LENGTH_SHORT).show();
            if(valsCurrent.length() > 0)
            {
                valsCurrent += MyReqInterface.newLine;
            }
            String code = valsCurrent + scanContent;
            valsText.setText(code);
        }
    }
}

