package b.my.audioplayer.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class GradientTextView extends AppCompatTextView {

    private LinearGradient gradient;
    private Matrix gradientMatrix;
    private int[] gradientColors = new int[]{
            Color.WHITE,
            Color.YELLOW,
            Color.parseColor("#D0964B")
    };

    public GradientTextView(Context context) {
        super(context);
        init();
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GradientTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gradientMatrix = new Matrix();
    }

    public void setGradientColors(int... colors) {
        this.gradientColors = colors;
        gradient = null;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient = null;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        gradient = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        CharSequence text = getText();
        if (text == null || text.length() == 0) {
            super.onDraw(canvas);
            return;
        }

        int width = getWidth();
        if (width <= 0) {
            super.onDraw(canvas);
            return;
        }

        if (gradient == null) {
            gradient = new LinearGradient(
                    0, 0, width, 0,
                    gradientColors,
                    null,
                    Shader.TileMode.CLAMP
            );
        }

        gradientMatrix.reset();
        gradient.setLocalMatrix(gradientMatrix);
        getPaint().setShader(gradient);

        super.onDraw(canvas);
    }
}