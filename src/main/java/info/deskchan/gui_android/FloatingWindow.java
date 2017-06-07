package info.deskchan.gui_android;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by Admin on 04.06.2017.
 */

public class FloatingWindow extends Service{

    private WindowManager wm;
    private LinearLayout ll;
    private Button stop;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        ll = new LinearLayout(this);
        stop = new Button(this);
        ViewGroup.LayoutParams btnParams = new ViewGroup.LayoutParams(32,32);
        stop.setBackground(getDrawable( R.drawable.close));
        stop.setLayoutParams(btnParams);



        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);

        ll.setBackground(getDrawable( R.drawable.happy));
        ll.setLayoutParams(llParams);
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(250,450,WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.gravity = Gravity.CENTER | Gravity.CENTER;
        ll.addView(stop);
        wm.addView(ll,layoutParams);

        ll.setOnTouchListener(new View.OnTouchListener() {
            private WindowManager.LayoutParams newParams = layoutParams;
            int x,y;
            float touchedX,touchedY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        x = newParams.x;
                        y = newParams.y;
                        touchedX = event.getRawX();
                        touchedY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        newParams.x = (int)(x+(event.getRawX()-touchedX));
                        newParams.y = (int)(y+(event.getRawY()-touchedY));
                        wm.updateViewLayout(ll,newParams);
                        break;
                }










                return false;
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wm.removeView(ll);
                stopSelf();
            }
        });

    }
}
