/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.putme2yourheart.qrcodereaderview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.ResultPoint;

import java.util.Collection;
import java.util.HashSet;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 */
public final class ScannerCoverLayout extends ViewGroup {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 10L;
    private static final int OPAQUE = 0xFF;
    private static final int CORNER_RECT_WIDTH = 8;  // The width of the corner of the scanning area
    private static final int CORNER_RECT_HEIGHT = 40; // The height of the corner of the scanning area
    private static final int SCANNER_LINE_MOVE_DISTANCE = 5;  // Scan line moving distance
    private static final int SCANNER_LINE_HEIGHT = 10;  // Scan line width

    private View topView;
    private View bottomView;

    private final Paint paint;
    private Bitmap resultBitmap;
    // Blur area color
    private final int maskColor;
    private final int resultColor;
    // Scan area border color
    private final int frameColor;
    // Scanline color
    private final int laserColor;
    // Four-corner color
    private final int cornerColor;
    // Scan point color
    private final int resultPointColor;
    private int scannerAlpha;

    public static int scannerStart = 0;
    public static int scannerEnd = 0;

    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;

    private CameraManager mCameraManager;

    // This constructor is used when the class is built from an XML resource.
    public ScannerCoverLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize custom attribute information
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ScannerCoverLayout);
        laserColor = array.getColor(R.styleable.ScannerCoverLayout_laser_color, 0x00FF00);
        cornerColor = array.getColor(R.styleable.ScannerCoverLayout_corner_color, 0x00FF00);
        frameColor = array.getColor(R.styleable.ScannerCoverLayout_frame_color, 0xFFFFFF);
        resultPointColor =
                array.getColor(R.styleable.ScannerCoverLayout_result_point_color, 0xC0FFFF00);
        maskColor = array.getColor(R.styleable.ScannerCoverLayout_mask_color, 0x60000000);
        resultColor = array.getColor(R.styleable.ScannerCoverLayout_result_color, 0xB0000000);

        // Initialize these once for performance rather than calling them every time in onDraw().
        mCameraManager = CameraManager.getCameraManager(context);
        paint = new Paint();
        paint.setAntiAlias(true);
        scannerAlpha = 0;
        possibleResultPoints = new HashSet<>(5);

        array.recycle();

        // onDraw in the view group
        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            // Measure the width and height of a sub-control
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }

        Rect rect = mCameraManager.getFramingRect();

        if (topView != null) {
            LayoutParams layoutParams = topView.getLayoutParams();
            if (rect != null) {
                layoutParams.height = rect.top;
            }
            topView.setLayoutParams(layoutParams);
        }
        if (bottomView != null) {
            LayoutParams layoutParams = bottomView.getLayoutParams();
            if (rect != null) {
                layoutParams.height = rect.top;
            }
            bottomView.setLayoutParams(layoutParams);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (topView != null) {
            LayoutParams layoutParams = topView.getLayoutParams();
            Rect rect = mCameraManager.getFramingRect();
            if (rect != null) {
                layoutParams.height = rect.top;
                layoutParams.width = r;
                bottomView.setLayoutParams(layoutParams);
                topView.layout(0, 0, r, rect.top);
            }
        }

        if (bottomView != null) {
            LayoutParams layoutParams = bottomView.getLayoutParams();
            Rect rect = mCameraManager.getFramingRect();
            if (rect != null) {
                layoutParams.height = b - rect.bottom;
                layoutParams.width = r;
                bottomView.setLayoutParams(layoutParams);
                bottomView.layout(0, rect.bottom, r, b);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int count = getChildCount();
        if (count > 2) {
            throw new InflateException(
                    "cannot inflate ScannerCoverLayout, because there are more than 2 child views");
        }
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (i == 0) {
                topView = view;
            }
            if (i == 1) {
                bottomView = view;
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = mCameraManager.getFramingRect();
        if (frame == null) {
            return;
        }
        if (scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top;
            scannerEnd = frame.bottom;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas, frame, width, height);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {
            // Draw a two pixel solid black border inside the framing rect
            drawFrame(canvas, frame);
            // Draw a corner
            drawCorner(canvas, frame);
            // Draw a red "laser scanner" line through the middle to show decoding is active
            drawLaserScanner(canvas, frame);

            Collection<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new HashSet<>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            // Specify a redraw region, this method will be executed in the child thread
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    // Draw a corner
    private void drawCorner(Canvas canvas, Rect frame) {
        paint.setColor(cornerColor);
        // Top left corner
        canvas.drawRect(frame.left, frame.top, frame.left + CORNER_RECT_WIDTH,
                frame.top + CORNER_RECT_HEIGHT, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + CORNER_RECT_HEIGHT,
                frame.top + CORNER_RECT_WIDTH, paint);
        // Top right corner
        canvas.drawRect(frame.right - CORNER_RECT_WIDTH, frame.top, frame.right,
                frame.top + CORNER_RECT_HEIGHT, paint);
        canvas.drawRect(frame.right - CORNER_RECT_HEIGHT, frame.top, frame.right,
                frame.top + CORNER_RECT_WIDTH, paint);
        // Lower left corner
        canvas.drawRect(frame.left, frame.bottom - CORNER_RECT_WIDTH, frame.left + CORNER_RECT_HEIGHT,
                frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - CORNER_RECT_HEIGHT, frame.left + CORNER_RECT_WIDTH,
                frame.bottom, paint);
        // Lower right corner
        canvas.drawRect(frame.right - CORNER_RECT_WIDTH, frame.bottom - CORNER_RECT_HEIGHT, frame.right,
                frame.bottom, paint);
        canvas.drawRect(frame.right - CORNER_RECT_HEIGHT, frame.bottom - CORNER_RECT_WIDTH, frame.right,
                frame.bottom, paint);
    }

    // Draw a scan line
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        paint.setColor(laserColor);
        // Scanning line flashing effect
        //    paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        //    scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        //    int middle = frame.height() / 2 + frame.top;
        //    canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
        // Linear gradient
        LinearGradient linearGradient =
                new LinearGradient(frame.left, scannerStart, frame.left, scannerStart + SCANNER_LINE_HEIGHT,
                        shadeColor(laserColor), laserColor, Shader.TileMode.MIRROR);

        RadialGradient radialGradient = new RadialGradient((float) (frame.left + frame.width() / 2),
                (float) (scannerStart + SCANNER_LINE_HEIGHT / 2), 360f, laserColor, shadeColor(laserColor),
                Shader.TileMode.MIRROR);

        SweepGradient sweepGradient = new SweepGradient((float) (frame.left + frame.width() / 2),
                (float) (scannerStart + SCANNER_LINE_HEIGHT), shadeColor(laserColor), laserColor);

        ComposeShader composeShader =
                new ComposeShader(radialGradient, linearGradient, PorterDuff.Mode.ADD);

        paint.setShader(radialGradient);
        if (scannerStart <= scannerEnd) {
            // rectangle
            //      canvas.drawRect(frame.left, scannerStart, frame.right, scannerStart + SCANNER_LINE_HEIGHT, paint);
            // oval
            RectF rectF = new RectF(frame.left + 2 * SCANNER_LINE_HEIGHT, scannerStart,
                    frame.right - 2 * SCANNER_LINE_HEIGHT, scannerStart + SCANNER_LINE_HEIGHT);
            canvas.drawOval(rectF, paint);
            scannerStart += SCANNER_LINE_MOVE_DISTANCE;
        } else {
            scannerStart = frame.top;
        }
        paint.setShader(null);
    }

    // Handle color blur
    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "20" + hax.substring(2);
        return Integer.valueOf(result, 16);
    }

    // Draw a two pixel solid black border inside the framing rect
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
        canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
        canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);
    }

    // Draw the exterior (i.e. outside the framing rect) darkened
    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

    public View getTopView() {
        return topView;
    }

    public View getBottomView() {
        return bottomView;
    }
}
