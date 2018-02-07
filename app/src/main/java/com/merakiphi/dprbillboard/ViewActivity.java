package com.merakiphi.dprbillboard;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anuragmaravi on 14/09/17.
 */

public class ViewActivity extends AppCompatActivity {

    private static final String TAG = ViewActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<BillBoard> billBoardList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("billboards");
        billBoardList = new ArrayList<>();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);


        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child: dataSnapshot.getChildren()) {
//                    Log.d("User key", child.getKey());
//                    Log.d("User val", child.getValue().toString());
                    final BillBoard billBoard = new BillBoard();
                    billBoard.setKey(child.getKey());
                    billBoard.setType("Type: " + child.child("type").getValue(String.class));
                    billBoard.setDistrict("District: " + child.child("district").getValue(String.class));
                    billBoard.setLongitude("Longitude: " + child.child("longitude").getValue(String.class));
                    billBoard.setLatitude("Latitude: " + child.child("latitude").getValue(String.class));
                    billBoard.setHeight("Size: " + child.child("height").getValue(String.class) + " X " + child.child("width").getValue(String.class));
                    billBoard.setWidth("Width: " + child.child("width").getValue(String.class));
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                    storageRef.child("images/"+ child.getKey()+".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Got the download URL for 'users/me/profile.png'
                            billBoard.setImage(String.valueOf(uri));
                            Log.i(TAG, "onSucc: " + billBoard.getImage());

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "onFailure: ", exception);
                        }
                    });

                    billBoardList.add(billBoard);
                }

                recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
                recyclerView.setHasFixedSize(true);
                layoutManager = new LinearLayoutManager(getApplicationContext());
                recyclerView.setLayoutManager(layoutManager);
                ViewBillboardAdapter adapter = new ViewBillboardAdapter(billBoardList, getApplicationContext());
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(adapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    // Calls the server to securely obtain an unguessable download Url

    private String getUrlAsync (String userId){
        // Points to the root reference
        final String[] url = new String[1];
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("images");
        StorageReference dateRef = storageRef.child(userId+ ".jpg");
        dateRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
        {
            @Override
            public void onSuccess(Uri downloadUrl)
            {
                //do something with downloadurl
                url[0] = String.valueOf(downloadUrl);
                Log.i(TAG, "onSuccess: " + url[0]);
            }
        });
        return url[0];
    }
}
