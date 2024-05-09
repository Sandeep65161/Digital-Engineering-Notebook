package com.example.engineeringnotebook.Notebook;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.engineeringnotebook.DigitalInk.DrawingView;
import com.example.engineeringnotebook.DigitalInk.StrokeManager;
import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.utils.LogHelper;

public class OtherUtilityActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(OtherUtilityActivity.class);

    @VisibleForTesting
    final StrokeManager strokeManager = new StrokeManager();

    private Button createBitmapBtn, clearBtn;
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_utility);
        createBitmapBtn = findViewById(R.id.createBitmap_button);
        clearBtn = findViewById(R.id.button_clear);
        drawingView = findViewById(R.id.drawing_view);
        getSupportActionBar().setTitle("Your Signature");

        drawingView.setStrokeMang(strokeManager);
        final ImageView imageView = findViewById(R.id.showBitmap_imageView);
        createBitmapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap = getBitmapFromViewUsingCanvas(drawingView);
                imageView.setImageBitmap(bitmap);
            }
        });
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                strokeManager.reset();
                drawingView.clear();
            }
        });
    }

    // method that converts what the canvas view has to bitmap
    // returns that bitmap
    private Bitmap getBitmapFromViewUsingCanvas(View view) {
        // Create a new Bitmap object with the desired width and height
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a new Canvas object using the Bitmap
        Canvas canvas = new Canvas(bitmap);

        // Draw the View into the Canvas
        view.draw(canvas);

        // Return the resulting Bitmap
        return bitmap;
    }

    //Need to show X on toolbar to cancel Add Note Activity without saving note
    //override onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.close_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //Handle click of 'X' - display message when clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.closeBtn) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}


