package com.example.engineeringnotebook.DigitalInk;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Status bar for the app.
 *
 * <p>It is updated upon status changes announced by the StrokeManager.
 */
public class StatusTextView extends androidx.appcompat.widget.AppCompatTextView implements StrokeManager.StatusChangedListener {

    private StrokeManager strokeManager;

    public StatusTextView(@NonNull Context context) {
        super(context);
    }

    public StatusTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onStatusChanged() {
        this.setText(this.strokeManager.getStatus());
    }

    void setStrokeManager(StrokeManager strokeManager) {
        this.strokeManager = strokeManager;
    }
}