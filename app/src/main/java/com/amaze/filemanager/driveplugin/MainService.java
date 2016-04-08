package com.amaze.filemanager.driveplugin;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.amaze.filemanager.IMyAidlInterface;
import com.amaze.filemanager.Loadlistener;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.ui.Layoutelements;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Arpit  on 05-11-2015.
 */
public class MainService extends Service {
    DriveUtil driveUtil=new DriveUtil();
    Drive mService=null;
    Loadlistener loadlistener;
    GoogleAccountCredential mCredential;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        account=intent.getStringExtra("account");
        Toast.makeText(this,account,Toast.LENGTH_SHORT).show();
        initialize(account);
        return aidlInterface.asBinder();
    }

    IMyAidlInterface aidlInterface=new IMyAidlInterface.Stub() {
        @Override
        public void loadlist(final String id) throws RemoteException {
            if(id==null){
                if(loadlistener!=null)
                    loadlistener.error("Nothing",1);
                return;
            }
            if(mService==null && android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches())
                initialize(id);
            if(android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches())
            {
                driveUtil.listRoot(mService, new DriveUtil.listReturn() {
                    @Override
                    public void list(ArrayList<File> arrayList) {
                        List<Layoutelements> l=null;
                        try {
                            l=driveUtil.addToDrive(arrayList);

                            if(loadlistener!=null)loadlistener.load(l,id);
                        } catch (RemoteException e) {
                            throwError(e);
                        }
                    }

                    @Override
                    public void throwError(Exception e) {
                        e.printStackTrace();
                        try {
                            if(loadlistener!=null)
                                loadlistener.error(e.getMessage(),0);
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
            else{
                driveUtil.listFolder(mService, id, new DriveUtil.listReturn() {
                    @Override
                    public void list(ArrayList<File> arrayList) {
                        List<Layoutelements> l = null;
                        try {
                            l=driveUtil.addToDrive(arrayList);

                            if(loadlistener!=null)loadlistener.load(l, id);
                        } catch (RemoteException e) {
                            throwError(e);
                        }
                    }

                    @Override
                    public void throwError(Exception e) {
                        e.printStackTrace();
                        try {
                            if(loadlistener!=null)
                                loadlistener.error(e.getMessage(),0);
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void loadRoot() throws RemoteException {
            loadlist(account);
        }


        @Override
        public void goback(final String id) throws RemoteException {
            driveUtil.goBack(id, new DriveUtil.GoBackCallback() {
                @Override
                public void loadList(String id) {
                    if(id==null){
                        try {
                            loadlist(null);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    try {
                        loadlist(id);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            },mService);
        }
        @Override
        public void create()throws RemoteException{
            Log.d("DriveConnection", "Create called");
            startAuthActivity();
        }
        @Override
        public void registerCallback(Loadlistener load){
            MainService.this.loadlistener=load;

        }
    };
    private static final String[] SCOPES = { DriveScopes.DRIVE_FILE };
    void startAuthActivity(){
        Intent o=new Intent(this,MainActivity.class);
        o.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(o);
        registerReceiver(receiver, new IntentFilter("account"));
    }
    String account=null;
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getExtras()!=null)
            {   account=intent.getStringExtra("account");
                if(account!=null)initialize(account);
                if(loadlistener!=null) try {
                    loadlistener.load(null,account);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                unregisterReceiver(receiver);
            }}
    };
    void initialize(String id){
        account=id;
        mCredential = GoogleAccountCredential.usingOAuth2(
                this, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(id);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Drive API Android Quickstart")
                .build();
    }
}
