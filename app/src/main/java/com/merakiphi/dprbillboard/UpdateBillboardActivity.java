package com.merakiphi.dprbillboard;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by anuragmaravi on 14/09/17.
 */

public class UpdateBillboardActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LocationListener {
    private static final String TAG = UpdateBillboardActivity.class.getSimpleName();

    private TextView textViewLocation;
    private EditText editTextLenght, editTextBreadth, editTextNotes;
    private Button update, delete;
    private RadioGroup radioGrp;

    //Location
    LocationManager locationManager;
    double latitude, longitude;

    //Image Picker
    private final int SELECT_PHOTO = 1;
    private ImageView imageView;

    //Spinners
    private Spinner spinner, spinnerType;
    private String spinnerDistrictString, spinnerTypeString, backlightString;

    private FirebaseDatabase database;
    private DatabaseReference ref;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri filePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_billboard);
        database = FirebaseDatabase.getInstance();
        ref = database.getReference("billboards");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference("images");

        //UI Elements
        editTextLenght = (EditText) findViewById(R.id.length);
        editTextBreadth = (EditText) findViewById(R.id.breadth);
        editTextNotes = (EditText) findViewById(R.id.notes);
        textViewLocation = (TextView) findViewById(R.id.textViewLocation);
        update = (Button) findViewById(R.id.update);
        delete = (Button) findViewById(R.id.delete);

        // Spinner element
        spinner = (Spinner) findViewById(R.id.spinner);
        spinnerType = (Spinner) findViewById(R.id.spinnerType);
        // Spinner click listener
        spinner.setOnItemSelectedListener(UpdateBillboardActivity.this);
        spinnerType.setOnItemSelectedListener(UpdateBillboardActivity.this);
        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("Durg");
        categories.add("Raipur");
        categories.add("Korba");
        categories.add("Bilaspur");

        List<String> types = new ArrayList<String>();
        types.add("Unipole");
        types.add("Tripole");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        ArrayAdapter<String> dataAdapterType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, types);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataAdapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinnerType.setAdapter(dataAdapterType);

        //Initialize activity with the current data
        ref.child(getIntent().getStringExtra("key")).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("User val", dataSnapshot.child("type").getValue().toString());
                editTextLenght.setText(dataSnapshot.child("height").getValue().toString());
                editTextBreadth.setText(dataSnapshot.child("width").getValue().toString());
                editTextNotes.setText(dataSnapshot.child("notes").getValue().toString());
                textViewLocation.setText(dataSnapshot.child("latitude").getValue().toString() + ", " + dataSnapshot.child("longitude").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

        //Location request
        requestPermission();

        //Image Picker ******************************************************
        imageView = (ImageView)findViewById(R.id.imageView);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        storageRef.child("images/"+ getIntent().getStringExtra("key") +".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                Log.i(TAG, "onSucc: " + String.valueOf(uri));
                Glide.with(getApplicationContext()).load(String.valueOf(uri)).into(imageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "onFailure: ", exception);
            }
        });

        Button pickImage = (Button) findViewById(R.id.btn_pick);
        pickImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                User user = new User(spinnerDistrictString, String.valueOf(latitude), String.valueOf(longitude),"null",editTextLenght.getText().toString(),editTextBreadth.getText().toString(),spinnerTypeString, backlightString,editTextNotes.getText().toString(),"null","null","null",getIntent().getStringExtra("key")  + ".jpg");
                ref.child(getIntent().getStringExtra("key")).setValue(user);
                uploadFile(getIntent().getStringExtra("key"));
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ref.child(getIntent().getStringExtra("key")).removeValue();
                Intent intent = new Intent(getApplicationContext(), ViewActivity.class);
                startActivity(intent);
            }
        });

    }

    /**
     * Spinners ****************************************************************************
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


        switch(parent.getId()){
            case R.id.spinner:
                // On selecting a spinner item
                spinnerDistrictString = parent.getItemAtPosition(position).toString();
                break;
            case R.id.spinnerType:
                // On selecting a spinner item
                spinnerTypeString = parent.getItemAtPosition(position).toString();
                break;
        }
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * Current Location ****************************************************************************
     */
    private void requestPermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 223);
            } else {
                lock_on();
            }
        } else {
            lock_on();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 223: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    lock_on();

                } else {
                    Toast.makeText(getApplicationContext(), "Unable to get location", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    void lock_on() {
        final String provider;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(provider,1000, 1,UpdateBillboardActivity.this);

        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String providera : providers) {
            Location l = locationManager.getLastKnownLocation(providera);
            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            Toast.makeText(this, "Unable to find the current location.", Toast.LENGTH_SHORT).show();
        }
        latitude=bestLocation.getLatitude();
        longitude=bestLocation.getLongitude();
        textViewLocation.setText(String.valueOf(latitude) + ", " + String.valueOf(longitude));

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Image Picker ****************************************************************************
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        imageView.setImageBitmap(selectedImage);

                        filePath = imageReturnedIntent.getData();


                        // CALL THIS METHOD TO GET THE ACTUAL PATH
//                        File finalFile = new File(getRealPathFromURI(imageUri));
//                        Toast.makeText(this, finalFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//                        Log.i("Image Picker", finalFile.getAbsolutePath());

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

    //this method will upload the file
    private void uploadFile(String userId) {
        //if there is a file to upload
        if (filePath != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            StorageReference riversRef = storageRef.child(userId + ".jpg");
            riversRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying a success toast
                            Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying error message
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            //you can display an error toast
        }
    }
}
