package com.ytmfdw.imageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * 自定义裁剪图片控件
 */
public class MyImageView extends ImageView {

    //双击回调接口
    public interface OnDoubleClickListener {
        public void getBitmap(Bitmap bitmap);
    }

    static final String TAG = "ytmfdw";

    private static final int MIN_FRAME_WIDTH = 50; // originally 240
    private static final int MIN_FRAME_HEIGHT = 20; // originally 240
    private static final int MAX_FRAME_WIDTH = 800; // originally 480
    private static final int MAX_FRAME_HEIGHT = 600; // originally 360

    private Rect framingRect;
    private Point screenResolution;
    private final Paint paint;

    private final int maskColor;
    private final int frameColor;
    private final int cornerColor;

    private boolean initialized;
    private long lastUpTime = 0;

    /**
     * 是否平移
     */
    private boolean isTranslate = false;

    private OnDoubleClickListener clickListener;

    public MyImageView(Context context) {
        this(context, null);
    }

    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        maskColor = Color.parseColor("#60000000");
        frameColor = Color.parseColor("#ffd6d6d6");
        cornerColor = Color.parseColor("#ffffffff");
//        maskColor = getResources().getColor(R.color.viewfinder_mask);
//        frameColor = getResources().getColor(R.color.viewfinder_frame);
//        cornerColor = getResources().getColor(R.color.viewfinder_corners);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        init();
    }

    /**
     * 设置监听
     *
     * @param listener
     */
    public void setDoubleClickListener(OnDoubleClickListener listener) {
        clickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        screenResolution = new Point(width, height);
    }

    /**
     * 初始化
     */
    private void init() {

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        screenResolution = new Point(width, height);

        setOnTouchListener(new View.OnTouchListener() {
            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();

                        try {
                            Rect rect = getFramingRect();

                            final int BUFFER = 50;
                            final int BIG_BUFFER = 60;
                            if (lastX >= 0) {
                                // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
                                if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top left corner: adjust both top and left sides
                                    adjustFramingRect(2 * (lastX - currentX), 2 * (lastY - currentY));
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top right corner: adjust both top and right sides
                                    adjustFramingRect(2 * (currentX - lastX), 2 * (lastY - currentY));
                                } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom left corner: adjust both bottom and left sides
                                    adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom right corner: adjust both bottom and right sides
                                    adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                                } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
                                    adjustFramingRect(2 * (lastX - currentX), 0);
                                } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
                                    adjustFramingRect(2 * (currentX - lastX), 0);
                                } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
                                    adjustFramingRect(0, 2 * (lastY - currentY));
                                } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
                                    adjustFramingRect(0, 2 * (currentY - lastY));
                                } else if (currentX > rect.left && currentX < (rect.left + rect.width()) && currentY > rect.top && currentY < (rect.top + rect.height())) {
                                    //平移，当在框框内进行移动时，平移框框
                                    isTranslate = true;
                                    translateRect(currentX - lastX, currentY - lastY);
                                }
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Framing rect not available", e);
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;
                        isTranslate = false;
//                        currentDx = 0;
//                        currentDy = 0;
                        //判断是否双击
                        if (System.currentTimeMillis() - lastUpTime < 300) {
                            //800毫秒内算双击
                            if (clickListener != null) {
                                Bitmap bitmap = clip();
                                clickListener.getBitmap(bitmap);
                            }
                            return true;
                        }
                        lastUpTime = System.currentTimeMillis();
                        return true;
                }
                return false;
            }
        });

        initialized = true;

    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            int width = screenResolution.x * 3 / 5;
            if (width < MIN_FRAME_WIDTH) {
                width = MIN_FRAME_WIDTH;
            } else if (width > MAX_FRAME_WIDTH) {
                width = MAX_FRAME_WIDTH;
            }
            int height = screenResolution.y * 1 / 5;
            if (height < MIN_FRAME_HEIGHT) {
                height = MIN_FRAME_HEIGHT;
            } else if (height > MAX_FRAME_HEIGHT) {
                height = MAX_FRAME_HEIGHT;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        }
        return framingRect;
    }

    private int currentDx = 0;
    private int currentDy = 0;

    /**
     * 平移框框
     *
     * @param dx
     * @param dy
     */
    public synchronized void translateRect(int dx, int dy) {
        Log.d(TAG, "dx=" + dx);
        Log.d(TAG, "dy=" + dy);
        if (initialized) {
            int newWidth = framingRect.width();
            int newHeight = framingRect.height();
            currentDx += dx;
            currentDy += dy;
            int leftOffset = (screenResolution.x - newWidth) / 2 + currentDx;
            int topOffset = (screenResolution.y - newHeight) / 2 + currentDy;
            if (leftOffset < 0) {
                leftOffset = 0;
            }
            if (topOffset < 0) {
                topOffset = 0;
            }
            if (leftOffset + newWidth > screenResolution.x) {
                leftOffset = screenResolution.x - newWidth;
            }
            if (topOffset + newHeight > screenResolution.y) {
                topOffset = screenResolution.y - newHeight;
            }
            framingRect = new Rect(leftOffset, topOffset, leftOffset + newWidth, topOffset + newHeight);
        }
    }

    /**
     * Changes the size of the framing rect.
     *
     * @param deltaWidth  Number of pixels to adjust the width
     * @param deltaHeight Number of pixels to adjust the height
     */
    public synchronized void adjustFramingRect(int deltaWidth, int deltaHeight) {
        if (initialized && !isTranslate) {
            // Set maximum and minimum sizes
            if ((framingRect.width() + deltaWidth > screenResolution.x - 4) || (framingRect.width() + deltaWidth < 50)) {
                deltaWidth = 0;
            }
            if ((framingRect.height() + deltaHeight > screenResolution.y - 4) || (framingRect.height() + deltaHeight < 50)) {
                deltaHeight = 0;
            }

            int newWidth = framingRect.width() + deltaWidth;
            int newHeight = framingRect.height() + deltaHeight;
            int leftOffset = (screenResolution.x - newWidth) / 2 + currentDx;
            int topOffset = (screenResolution.y - newHeight) / 2 + currentDy;
            if (leftOffset < 0) {
                leftOffset = 0;
            }
            if (topOffset < 0) {
                topOffset = 0;
            }
            if (leftOffset + newWidth > screenResolution.x) {
                leftOffset = screenResolution.x - newWidth;
            }
            if (topOffset + newHeight > screenResolution.y) {
                topOffset = screenResolution.y - newHeight;
            }
            framingRect = new Rect(leftOffset, topOffset, leftOffset + newWidth, topOffset + newHeight);
//            framingRectInPreview = null;
        } else {
//            requestedFramingRectWidth = deltaWidth;
//            requestedFramingRectHeight = deltaHeight;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        {
            Rect frame = getFramingRect();
            if (frame == null) {
                return;
            }
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            // Draw the exterior (i.e. outside the framing rect) darkened
            paint.setColor(maskColor);
            canvas.drawRect(0, 0, width, frame.top, paint);
            canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
            canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
            canvas.drawRect(0, frame.bottom + 1, width, height, paint);
            // Draw a two pixel solid border inside the framing rect
            paint.setAlpha(0);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(frameColor);
            canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
            canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
            canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
            canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

            // Draw the framing rect corner UI elements
            paint.setColor(cornerColor);
            canvas.drawRect(frame.left - 15, frame.top - 15, frame.left + 15, frame.top, paint);
            canvas.drawRect(frame.left - 15, frame.top, frame.left, frame.top + 15, paint);
            canvas.drawRect(frame.right - 15, frame.top - 15, frame.right + 15, frame.top, paint);
            canvas.drawRect(frame.right, frame.top - 15, frame.right + 15, frame.top + 15, paint);
            canvas.drawRect(frame.left - 15, frame.bottom, frame.left + 15, frame.bottom + 15, paint);
            canvas.drawRect(frame.left - 15, frame.bottom - 15, frame.left, frame.bottom, paint);
            canvas.drawRect(frame.right - 15, frame.bottom, frame.right + 15, frame.bottom + 15, paint);
            canvas.drawRect(frame.right, frame.bottom - 15, frame.right + 15, frame.bottom + 15, paint);
        }
    }


    /**
     * 剪切图片，返回剪切后的bitmap对象
     *
     * @return
     */
    public Bitmap clip() {
        Rect frame = getFramingRect();
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return Bitmap.createBitmap(bitmap, frame.left + 1,
                frame.top + 1, frame.width() - 2,
                frame.height() - 2);
    }
}
