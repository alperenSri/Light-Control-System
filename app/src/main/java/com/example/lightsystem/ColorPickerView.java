package com.example.lightsystem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    private Paint paint;
    private Paint centerPaint;
    private Paint selectorPaint;
    private RectF rectF;
    private int[] colors;
    private float currentAngle = 0;
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectF = new RectF();

        // Define colors for the wheel
        colors = new int[] {
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.RED
        };

        selectorPaint.setColor(Color.WHITE);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(5);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int size = Math.min(w, h);
        int offset = 50; // Padding from edges
        rectF.set(offset, offset, size - offset, size - offset);

        // Create gradient shader
        SweepGradient gradient = new SweepGradient(rectF.centerX(), rectF.centerY(), colors, null);
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw color wheel
        canvas.drawArc(rectF, 0, 360, true, paint);

        // Draw selector
        float radius = (rectF.width() / 2);
        float selectorX = rectF.centerX() + radius * (float) Math.cos(Math.toRadians(currentAngle));
        float selectorY = rectF.centerY() + radius * (float) Math.sin(Math.toRadians(currentAngle));
        canvas.drawCircle(selectorX, selectorY, 20, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float dx = x - rectF.centerX();
                float dy = y - rectF.centerY();
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (angle < 0)
                    angle += 360;

                currentAngle = angle;
                int color = getColorFromAngle(angle);

                if (listener != null) {
                    listener.onColorSelected(color);
                }

                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int getColorFromAngle(float angle) {
        float unit = (angle / 360f);
        if (unit <= 0.16666f) {
            return interpolateColor(Color.RED, Color.YELLOW, unit * 6);
        } else if (unit <= 0.33333f) {
            return interpolateColor(Color.YELLOW, Color.GREEN, (unit - 0.16666f) * 6);
        } else if (unit <= 0.5f) {
            return interpolateColor(Color.GREEN, Color.CYAN, (unit - 0.33333f) * 6);
        } else if (unit <= 0.66666f) {
            return interpolateColor(Color.CYAN, Color.BLUE, (unit - 0.5f) * 6);
        } else if (unit <= 0.83333f) {
            return interpolateColor(Color.BLUE, Color.MAGENTA, (unit - 0.66666f) * 6);
        } else {
            return interpolateColor(Color.MAGENTA, Color.RED, (unit - 0.83333f) * 6);
        }
    }

    private int interpolateColor(int color1, int color2, float unit) {
        float unit1 = 1 - unit;

        int r = (int) (Color.red(color1) * unit1 + Color.red(color2) * unit);
        int g = (int) (Color.green(color1) * unit1 + Color.green(color2) * unit);
        int b = (int) (Color.blue(color1) * unit1 + Color.blue(color2) * unit);

        return Color.rgb(r, g, b);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }
}