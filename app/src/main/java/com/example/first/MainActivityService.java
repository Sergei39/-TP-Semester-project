package com.example.first;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class MainActivityService extends Service {
    private static final String NAME_BRANCH = "ProfilesSergei";
    private final long ONE_MEGABYTE = 1024 * 1024;

    // For data
    private DatabaseReference myRef;
    private FirebaseUser user;

    // For Image
    private StorageReference storageRef;
    Bitmap bmpImage;

    private Profile userProfile;
    private ArrayList<String> idDogs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(MainActivity.INF, "ServiceOnCreate");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        storageRef = FirebaseStorage.getInstance().getReference().child(NAME_BRANCH); //.child(user.getUid()).child("AvatarImage");

        idDogs = new ArrayList<>();


        if (user != null) {
            Log.d(MainActivity.INFORMATION_PROCESS, "User +");
            DatabaseReference childRef = myRef.child(NAME_BRANCH).child(user.getUid());
            childRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(dataSnapshot.getValue(Profile.class).getSeen() != null) {

                        idDogs = dataSnapshot.getValue(Profile.class).getSeen();

                        Log.d(MainActivity.INFORMATION_PROCESS, "Id dog with seen in ArrayList");
                        Log.d(MainActivity.INFORMATION_PROCESS, Integer.toString(idDogs.size()) + " In ArrayList Id");


                        if (idDogs.size() != 0) {
                            myRef.child(NAME_BRANCH).child(idDogs.get(0))
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            userProfile = dataSnapshot.getValue(Profile.class);

                                            Log.d(MainActivity.INFORMATION_PROCESS, "First Profile We Have");
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                            storageRef.child(idDogs.get(0)).child("AvatarImage").
                                    getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    bmpImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                    Log.d(MainActivity.INFORMATION_PROCESS, "First Image in Bitmap");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), "No Such file or Path found!!", Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String inf;
        inf = intent.getAction();

        Log.d(MainActivity.INFORMATION_PROCESS, "New Service");

        setNewProfile();

        if (inf != null) {
            Log.d(MainActivity.INF, inf);
            switch (inf) {
                case "left": {
                    //remove with likes
                    //Profile profile = getDataOfNewDog();

                    Log.d(MainActivity.INFORMATION_PROCESS, "Stop Service");
                    stopSelf(startId);
                }

                case "right": {
                    //remove with likes, put in likes or matches
                    //Profile profile = getDataOfNewDog();

                    Log.d(MainActivity.INFORMATION_PROCESS, "Stop Service");
                    stopSelf(startId);
                }

                default: {
                }

            }
        }

        super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    private void getNewProfile() {

        if(idDogs.size() != 0) {
            idDogs.remove(0);
            myRef.child(NAME_BRANCH).child(user.getUid()).child("seen").setValue(idDogs);

            myRef.child(NAME_BRANCH).child(idDogs.get(0))
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            userProfile = dataSnapshot.getValue(Profile.class);

                            Log.d(MainActivity.INFORMATION_PROCESS, "We have new Profile");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

            storageRef.child(idDogs.get(0)).child("AvatarImage").
                    getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    bmpImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    Log.d(MainActivity.INFORMATION_PROCESS, "Next Image in Bitmap");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "No Such file or Path found!!", Toast.LENGTH_LONG).show();
                }
            });

        }
        else {
            userProfile = null;

            Log.d(MainActivity.INFORMATION_PROCESS, "Next Profile Not");
        }
    }

    private MyBinder mBinder = new MyBinder();
    private ProfileListener listener = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void listenEvents(ProfileListener profileListener) {
        listener = profileListener;
    }

    class MyBinder extends Binder {
        MainActivityService getService() {
            return MainActivityService.this;
        }
    }

    interface ProfileListener {
        void newProfile(Profile profile, Bitmap bmpImage);
    }

    public void setNewProfile () {
        Log.d(MainActivity.INFORMATION_PROCESS, "Service Start New Profile");
        if (userProfile != null) {
            Log.d(MainActivity.INFORMATION_PROCESS, "Transport Profile");

            listener.newProfile(userProfile, bmpImage);
        }
        else {
            Log.d(MainActivity.INFORMATION_PROCESS, "Not Profile");
        }
        //listener.newProfile(new Profile("Test2", "", "", "", "6", "", "city", "", null, null, null));
        getNewProfile();
    }
}
