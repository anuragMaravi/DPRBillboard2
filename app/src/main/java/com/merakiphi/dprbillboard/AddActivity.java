package com.merakiphi.dprbillboard;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by anuragmaravi on 14/09/17.
 */

public class AddActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LocationListener {

    private static final String TAG = AddActivity.class.getSimpleName();


    private Spinner spinner, spinnerType;
    private TextView textViewLocation;
    private DatePickerDialog datePickerDialog;
    private int mYear, mMonth, mDay;
    private Button save, buttonCamera;
    private TextView textViewDate;
    private ImageView calendar;
    private RadioGroup radioGrp;
    private RadioButton radioY, radioN;
    private EditText editTextLenght, editTextBreadth, editTextNotes;


    //Location
    LocationManager locationManager;
    double latitude, longitude;

    //Image Picker
    private final int SELECT_PHOTO = 1;
    private ImageView imageView;

    //Spinners
    private String spinnerDistrictString, spinnerTypeString, backlightString;

    //Firebase
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private String userId;

    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri filePath;

    static final int REQUEST_IMAGE_CAPTURE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        //EditTexts
        editTextLenght = (EditText) findViewById(R.id.length);
        editTextBreadth = (EditText) findViewById(R.id.breadth);
        editTextNotes = (EditText) findViewById(R.id.notes);

        textViewLocation = (TextView) findViewById(R.id.textViewLocation);


        // Spinner element
        spinner = (Spinner) findViewById(R.id.spinner);
        spinnerType = (Spinner) findViewById(R.id.spinnerType);

        radioGrp=(RadioGroup)findViewById(R.id.radioGrp);
        radioGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(checkedId == R.id.radioY) {
                    backlightString = "True";
                } else {
                    backlightString = "False";
                }
            }
        });

        // Spinner click listener
        spinner.setOnItemSelectedListener(AddActivity.this);
        spinnerType.setOnItemSelectedListener(AddActivity.this);


        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabase = mFirebaseInstance.getReference("billboards");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference("images");

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

        //Location request
        requestPermission();

        //Image Picker ******************************************************
        imageView = (ImageView)findViewById(R.id.imageView);

        Button pickImage = (Button) findViewById(R.id.btn_pick);
        pickImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        });


        buttonCamera = (Button) findViewById(R.id.buttonCamera);
        buttonCamera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        save = (Button) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(editTextLenght.getText().toString()!=null && editTextBreadth.getText().toString()!=null && editTextNotes.getText().toString()!=null && backlightString!=null) {
                    if(uploadFile(userId)) {
                        userId = mFirebaseDatabase.push().getKey();
                        User user = new User(spinnerDistrictString, String.valueOf(latitude), String.valueOf(longitude), "null", editTextLenght.getText().toString(), editTextBreadth.getText().toString(), spinnerTypeString, backlightString, editTextNotes.getText().toString(), "null", "null", "null", userId + ".jpg");
                        mFirebaseDatabase.child(userId).setValue(user);
                    }
                }
                else
                {
                    Toast.makeText(AddActivity.this, "Some entry is missing.", Toast.LENGTH_SHORT).show();
                }

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
        locationManager.requestLocationUpdates(provider,1000, 1,AddActivity.this);

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

        //ToDo: Check the code for location updates
//        latitude=bestLocation.getLatitude();
//        longitude=bestLocation.getLongitude();
//        textViewLocation.setText(String.valueOf(latitude) + ", " + String.valueOf(longitude));

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
                        Log.i(TAG, "onActivityResult: " + filePath);
//                        Toast.makeText(this, String.valueOf(filePath), Toast.LENGTH_SHORT).show();


                        // CALL THIS METHOD TO GET THE ACTUAL PATH
//                        File finalFile = new File(getRealPathFromURI(imageUri));
//                        Toast.makeText(this, finalFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//                        Log.i("Image Picker", finalFile.getAbsolutePath());

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            case REQUEST_IMAGE_CAPTURE:
                if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                    Bundle extras = imageReturnedIntent.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");

//                    Bitmap new1 = rotateImage(imageBitmap, 90);
                    imageView.setImageBitmap(imageBitmap);

                    saveToInternalStorage(imageBitmap);

                    Log.i(TAG, "onActivityResult: " + Uri.fromFile(new File("/data/user/0/com.merakiphi.dprbillboard/app_imageDir/profile.jpg")));

                    filePath = Uri.fromFile(new File("/data/user/0/com.merakiphi.dprbillboard/app_imageDir/profile.jpg"));
                }
        }
    }

    //this method will upload the file
    private Boolean uploadFile(String userId) {
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

                            AlertDialog.Builder mBuilder = new AlertDialog.Builder(AddActivity.this);
                            View mView = getLayoutInflater().inflate(R.layout.dialog_add_success, null);
                            Button mLogin = (Button) mView.findViewById(R.id.btnLogin);
                            mBuilder.setView(mView);
                            final AlertDialog dialog = mBuilder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                            mLogin.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(getApplicationContext(), AddActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
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
            return true;
        }
        //if there is not any file
        else {
            Toast.makeText(this, "No image found.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Camera Intent ****************************************************************************
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");
        Log.i(TAG, "saveToInternalStorage: " + String.valueOf(mypath));
        Toast.makeText(cw, String.valueOf(mypath), Toast.LENGTH_SHORT).show();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
}
