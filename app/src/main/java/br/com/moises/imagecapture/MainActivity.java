package br.com.moises.imagecapture;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private String mCurrentPhotoPath;
    private static final int CAMERA_REQUEST = 1888;
    public static final int GELERIA_REQUEST = 1999;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private AppCompatActivity thisActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;
        this.imageView = this.findViewById(R.id.imageView);
    }

    public void verifyCameraPermission(View v) {
        if (needCameraPermission()) {
            requestCameraPemission();
        } else {
            openCameraApp();
        }
    }

    public void openGalleryApp(View v) {
        Intent galeriaIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galeriaIntent, GELERIA_REQUEST);
    }

    private boolean needCameraPermission() {
        return ContextCompat.checkSelfPermission(
                thisActivity,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPemission() {
        ActivityCompat.requestPermissions(
                thisActivity,
                new String[]{Manifest.permission.CAMERA}
                , CAMERA_PERMISSION_CODE
        );
    }

    private void openCameraApp() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri mPhotoUri = FileProvider.getUriForFile(thisActivity, getAuthority(), createImageFile());
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);

            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getAuthority() {
        return getApplicationContext().getPackageName() + ".fileprovider";
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraApp();
            } else {
                Toast.makeText(thisActivity, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            Bitmap resizedBmp = resizeAndCenterCrop(200, 84, mCurrentPhotoPath);
            replaceFile(resizedBmp, mCurrentPhotoPath);
            imageView.setImageBitmap(resizedBmp);


        } else if (requestCode == GELERIA_REQUEST && resultCode == Activity.RESULT_OK) {

            Uri selectedImageURI = data.getData();

            //imageView.setImageURI(selectedImageURI);
            //Bitmap resizedBmp = resizeAndCenterCrop(200,84,selectedImageURI);
            //replaceFile(resizedBmp,selectedImageURI);
            //imageView.setImageBitmap(resizedBmp);

            imageView.setImageURI(selectedImageURI);
        }
    }

    public Bitmap resizeAndCenterCrop(int width, int height, String path) {
        Bitmap srcBmp = Bitmap.createBitmap(fetchImage(path));
        return scaleCenterCrop(srcBmp, height, width);
    }

    public Bitmap fetchImage(String path) {
        return BitmapFactory.decodeFile(path);
    }

    public void replaceFile(Bitmap bmp, String path) {
        try {
            FileOutputStream out = new FileOutputStream(path);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);

        } catch (IOException e) {
        }
    }

    public Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }
}
