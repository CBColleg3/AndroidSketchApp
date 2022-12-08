package com.example.sketchapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {
    int defaultColor;
    SignaturePad signaturePad;
    ImageButton imgEraser, imgColor, imgSave;
    SeekBar seekBar;
    TextView txtPenSize;

    SeekBar seekBarAlpha;
    TextView txtPenSizeAlpha;

    private static String fileName;
    File path = new File(System.getenv("EXTERNAL_STORAGE"));

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    public Uri saveBitmap(@NonNull final Context context, @NonNull final Bitmap bitmap,
                          @NonNull final Bitmap.CompressFormat format,
                          @NonNull final String mimeType,
                          @NonNull final String displayName) throws IOException {

        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);

        final ContentResolver resolver = context.getContentResolver();
        Uri uri = null;

        try {
            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, values);

            if (uri == null)
                throw new IOException("Failed to create new MediaStore record.");

            try (final OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null)
                    throw new IOException("Failed to open output stream.");

                if (!bitmap.compress(format, 95, stream))
                    throw new IOException("Failed to save bitmap.");
            }

            return uri;
        }
        catch (IOException e) {
            if (uri != null) {
                // Don't leave an orphan entry in the MediaStore
                resolver.delete(uri, null, null);
            }

            throw e;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signaturePad = findViewById(R.id.signature_pad);
        seekBar = findViewById(R.id.penSize);
        txtPenSize = findViewById(R.id.txtPenSize);
        imgColor = findViewById(R.id.btnColor);
        imgEraser = findViewById(R.id.btnEraser);
        imgSave = findViewById(R.id.btnSave);

        seekBarAlpha = findViewById(R.id.penAlpha);
        txtPenSizeAlpha = findViewById(R.id.txtPenAlpha);

        defaultColor = ContextCompat.getColor(MainActivity.this, R.color.black);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txtPenSize.setText("Brush Size:" + i + "dp");
                signaturePad.setMaxWidth(i);
                seekBar.setMax(50);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        imgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openColorPicker();
            }
        });

        imgEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signaturePad.clear();
            }
        });

        imgSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!signaturePad.isEmpty()) {
                    try {
                        saveImage();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Couldn't save!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        seekBarAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txtPenSizeAlpha.setText("Alpha:" +i + "dp");
                signaturePad.setAlpha((float)i/100f);
                seekBarAlpha.setMax(100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void saveImage() throws IOException {

        Bitmap bitmap = signaturePad.getSignatureBitmap();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBitmap(MainActivity.this, bitmap, Bitmap.CompressFormat.PNG, "image/png", fileName);
        }
        Toast.makeText(MainActivity.this, "Painting Saved!", Toast.LENGTH_SHORT).show();
    }

    private void openColorPicker() {
        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {

                defaultColor = color;
                signaturePad.setPenColor(color);
            }
        });
        ambilWarnaDialog.show();
    }

    private void askPermission() {
        Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener((new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                Toast.makeText(MainActivity.this, "Granted!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        })).check();
    }
}