package com.mahmoudjoe3.Hchat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;
    private static final int RC_PHOTO_PICKER_PERMISSION = 3;

    @BindView(R.id.messageListView)
    ListView mMessageListView;
    @BindView(R.id.photoPickerButton)
    ImageButton mPhotoPickerButton;
    @BindView(R.id.messageEditText)
    EditText mMessageEditText;
    @BindView(R.id.sendButton)
    ImageButton mSendButton;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;
    @BindView(R.id.image_msg)
    ImageView imageMsg;
    @BindView(R.id.remove_img_msg)
    ImageButton removeImgMsg;
    @BindView(R.id.image_msg_selctor_progressBar)
    ProgressBar imageMsgSelctorProgressBar;
    @BindView(R.id.empty_chat_msg)
    TextView emptyChatMsg;

    private MessageAdapter mMessageAdapter;
    private String mUsername;
    private Uri imageUri = null;

    FirebaseDatabase mFirebaseDatabase;
    FirebaseStorage mFirebaseStorage;
    FirebaseAuth mFirebaseAuth;

    DatabaseReference mDatabaseReference;
    StorageReference mStorageReference;
    ChildEventListener mChildEventListener;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mUsername = ANONYMOUS;
        mSendButton.setEnabled(false);
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        setTitle("H with M Forever");
        mDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mStorageReference = mFirebaseStorage.getReference().child("chat_photos");
        // Initialize message ListView and its adapter
        initAdapter();
        if(mMessageAdapter.isEmpty())
            emptyChatMsg.setVisibility(View.VISIBLE);
        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0&&!charSequence.toString().isEmpty()) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mUsername = user.getDisplayName();
                    initReadListener();
                } else {//not signed up
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.PhoneBuilder().build()))
                                    //.setAuthMethodPickerLayout(customLayout)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

    }

    private void initReadListener() {
        if (mChildEventListener == null) {
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    //called first time for all message children in first attach to
                    // the reference and then for every child added
                    FriendlyMessage message = snapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(message);
                    mProgressBar.setVisibility(ProgressBar.GONE);
                    emptyChatMsg.setVisibility(View.GONE);

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    FriendlyMessage message = snapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.remove(message);
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    mProgressBar.setVisibility(ProgressBar.GONE);
                }
            };
            mProgressBar.setVisibility(ProgressBar.GONE);
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private void onSignedOutCleanUp() {
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void photoPickerProcess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                &&ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                            ,Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_PHOTO_PICKER_PERMISSION);
        } else {
            openGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_PHOTO_PICKER_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

    }

    private void sendProcess(Uri imageUri) {
        if (imageUri != null) {
            imageMsgSelctorProgressBar.setVisibility(View.VISIBLE);
            StorageReference ref = mStorageReference.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            Uri downloadUri =task.getResult();
                            FriendlyMessage message = new FriendlyMessage(mMessageEditText.getText().toString()
                                    , mUsername, String.valueOf(downloadUri));
                            mDatabaseReference.push().setValue(message);
                            // Clear input box
                            mMessageEditText.setText("");
                            removeImage();
                        }
                    });
                }
            }).addOnProgressListener(this, new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    imageMsgSelctorProgressBar
                            .setProgress((int) (snapshot.getBytesTransferred() / (snapshot.getTotalByteCount())));
                }
            });
        } else {
            FriendlyMessage message = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
            mDatabaseReference.push().setValue(message);
            // Clear input box
            mMessageEditText.setText("");
        }
    }

    private void initAdapter() {
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);
        mMessageAdapter.setOnImageClickListener(uri -> {
            ShowImageBySheet(uri);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed In", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Signed In Canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                new Handler().postDelayed(() -> {
                    // do something...
                    imageUri = data.getData();
                    // compress image
                    //[1] convert uri to bitmap
                    Bitmap bitmap = uriToBitmap(imageUri);
                    //[2] encode image
                    String code = ImageCompressor.encode_Image_To_String(bitmap, 17);
                    //[3] decode image
                    Bitmap CodedBitmap = ImageCompressor.decode_String_To_Image(code);
                    //[4] convert bitmap to uri
                    imageUri = bitMapToUri(CodedBitmap);
                    viewImage(imageUri);

                },100);
                mSendButton.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sign_out_menu) {
            AuthUI.getInstance().signOut(this);
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @OnClick({R.id.photoPickerButton, R.id.sendButton,R.id.remove_img_msg,R.id.image_msg})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.photoPickerButton:// ImagePickerButton shows an image picker to upload a image for a message
                photoPickerProcess();
                break;
            case R.id.sendButton:// Send button sends a message and clears the EditText
                if(imageUri!=null)
                    imageMsgSelctorProgressBar.setVisibility(View.VISIBLE);
                sendProcess(imageUri);
                break;
            case R.id.remove_img_msg:
                removeImage();
                break;
            case R.id.image_msg:
                ShowImageBySheet(String.valueOf(imageUri));
                break;
        }
    }

    private void ShowImageBySheet(String uri) {
        BottomSheetDialog sheetDialog=new BottomSheetDialog(MainActivity.this);
        sheetDialog.setContentView(R.layout.view_image_sheet);
        ImageView imageView = (ImageView) sheetDialog.findViewById(R.id.imageView);
        Glide.with(imageView.getContext())
                .load(uri)
                .into(imageView);
        sheetDialog.show();
    }

    private void removeImage() {
        imageUri = null;
        imageMsgSelctorProgressBar.setVisibility(View.GONE);
        imageMsg.setVisibility(View.GONE);
        removeImgMsg.setVisibility(View.GONE);
        mSendButton.setEnabled(true);
    }

    private void viewImage(Uri imageData) {
        imageUri = imageData;
        Glide.with(imageMsg.getContext())
                .load(imageUri)
                .centerCrop()
                .into(imageMsg);
        imageMsg.setVisibility(View.VISIBLE);
        removeImgMsg.setVisibility(View.VISIBLE);
    }


    private Bitmap uriToBitmap(Uri uri) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= 29) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            try {
                bitmap = ImageDecoder.decodeBitmap(source);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public Uri bitMapToUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
///////////////////////////////////////////////////////

}