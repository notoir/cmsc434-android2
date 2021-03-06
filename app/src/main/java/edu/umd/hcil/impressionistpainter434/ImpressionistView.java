package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    /*public class PaintPoint {
        public float _x;
        public float _y;
        public float _radius;
        public Paint _paint = new Paint();

        public PaintPoint(float x, float y, int radius, Paint p) {
            _x = x;
            _y = y;
            _radius = radius;
            _paint.set(p);
        }
    }*/

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();
    private VelocityTracker _vTracker = null;

    //private ArrayList<PaintPoint> _listPaintPoints = new ArrayList<PaintPoint>();

    private float _scaleX;
    private float _scaleY;

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private int _radiusOffset = 0;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //_listPaintPoints.clear();

        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        }
        invalidate();
    }

    public Bitmap getPainting() {
        return this._offScreenBitmap;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }
        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    public void drawTriangle(float x, float y) {
        // adapted from:
        // http://stackoverflow.com/questions/20544668/how-to-draw-filled-triangle-on-android-canvas
        Point a = new Point((int)x, (int)y + _defaultRadius);
        Point b = new Point((int)x + _defaultRadius, (int)y - _defaultRadius);
        Point c = new Point((int)x - _defaultRadius, (int)y - _defaultRadius);
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(b.x, b.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(a.x, a.y);
        path.close();

        _offScreenCanvas.drawPath(path, _paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();

        // obtain necessary information for the velocity tracker
        int index = motionEvent.getActionIndex();
        int action = motionEvent.getActionMasked();
        int pointerId = motionEvent.getPointerId(index);

        BitmapDrawable d = ((BitmapDrawable)_imageView.getDrawable());
        if(d != null) {
            Bitmap b = d.getBitmap();
            float[] f = new float[9];
            _imageView.getImageMatrix().getValues(f);

            // obtain the scale factor, since each image that will be loaded into the image view on
            // the left has different dimensions (found this out through trial and error)
            _scaleX = f[Matrix.MSCALE_X];
            _scaleY = f[Matrix.MSCALE_Y];

            final int origW = b.getWidth();
            final int origH = b.getHeight();
            final int widthActual = Math.round(origW * _scaleX);
            final int heightActual = Math.round(origH * _scaleY);
            int top = (_imageView.getHeight() - heightActual)/2;
            int left = (_imageView.getWidth() - widthActual)/2;

            // check boundaries, multiplied by their respective scale factors so that the boundaries
            // of both sides can be treated as effectively the same
            if((int)curTouchX > left && (int)curTouchX < left + widthActual && (int)curTouchY > top &&
                    (int)curTouchY < top + heightActual) {
                // pull the color from the corresponding pixel in the image view
                int color = b.getPixel((int)((curTouchX - left)/_scaleX),
                        (int)((curTouchY - top )/ _scaleY));
                _paint.setColor(color);
            } else {
                return true;
            }
        }
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // initialize the velocity tracker
                if(_vTracker == null) {
                    _vTracker = VelocityTracker.obtain();
                } else {
                    _vTracker.clear();
                }
                _vTracker.addMovement(motionEvent);

                //_offScreenCanvas.drawCircle(curTouchX, curTouchY, _defaultRadius, _paint);
                break;
            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();

                _vTracker.addMovement(motionEvent);
                // compute the current velocity, in 10 pixels per second for the units, and
                // set 50 as the highest possible value, to ensure the shapes drawn don't blow up
                _vTracker.computeCurrentVelocity(10, 50);
                // we use the product of the x and y velocities to calculate a radius offset for
                // drawing the shapes, and divide it by 20 (found to work through trial and error)
                _radiusOffset = (int)((VelocityTrackerCompat.getXVelocity(_vTracker, pointerId) *
                        VelocityTrackerCompat.getYVelocity(_vTracker, pointerId)) / 20);

                for(int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if(_brushType.equals(BrushType.Square)) {
                        _offScreenCanvas.drawRect(touchX - _defaultRadius, touchY - _defaultRadius,
                                touchX + _defaultRadius, touchY + _defaultRadius, _paint);
                    } else if(_brushType.equals(BrushType.VelocityCircle)) {
                        _offScreenCanvas.drawCircle(curTouchX, curTouchY, _defaultRadius + _radiusOffset, _paint);
                    } else {
                        this.drawTriangle(curTouchX, curTouchY);
                    }
                }
                if(_brushType.equals(BrushType.Square)) {
                    // square brush
                    _offScreenCanvas.drawRect(curTouchX - _defaultRadius - _radiusOffset, curTouchY - _defaultRadius - _radiusOffset,
                            curTouchX + _defaultRadius, curTouchY + _defaultRadius, _paint);
                } else if(_brushType.equals(BrushType.VelocityCircle)) {
                    // circle brush
                    _offScreenCanvas.drawCircle(curTouchX, curTouchY, _defaultRadius + _radiusOffset, _paint);
                } else {
                    // triangle brush
                    this.drawTriangle(curTouchX, curTouchY);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        invalidate();
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();
        //System.out.println("Original sin: " + origW + ", " + origH);
        //System.out.println("Also: " + scaleX + ", " + scaleY);

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();
        //System.out.println("Here's the deal: " + imgViewW + ", " + imgViewH);
        //System.out.println("And this deal: " + widthActual + ", " + heightActual);

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    public void convertToBW() {

        // algorithm from:
        // http://www.johndcook.com/blog/2009/08/24/algorithms-convert-color-grayscale/
        int[] pixels = new int[_offScreenBitmap.getWidth()*_offScreenBitmap.getHeight()];
        _offScreenBitmap.getPixels(pixels, 0, _offScreenBitmap.getWidth(), 0, 0,
                _offScreenBitmap.getWidth(), _offScreenBitmap.getHeight());

        for(int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];

            int red = Color.red(pixel);
            int blue = Color.blue(pixel);
            int green = Color.green(pixel);


            int mean = (red + blue + green) / 3;
            pixels[i] = Color.rgb(mean, mean, mean);
        }

        _offScreenBitmap.setPixels(pixels, 0, _offScreenBitmap.getWidth(), 0, 0,
                _offScreenBitmap.getWidth(), _offScreenBitmap.getHeight());


        invalidate();
    }
}

