package com.example.oorstory;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Locale;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static com.example.oorstory.Notification.CHANNEL_1_ID;

/*
임시적으로 사용하지 않는 서비스

 */
public class StopWatchService extends Service  {

    private WindowManager windowManager;
    private View floatingView;
    private String title;
    private TextView textTime;

    private int seconds = 0;
    private boolean running = true;
    private String time;

    @Override
    public void onCreate(){
        super.onCreate();
         /*
            움직일 수 있는 플로팅 뷰 띄우기
         */
        setView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId)
    {
        if(intent == null){
            return Service.START_STICKY;
        }else {

            title = intent.getStringExtra("title");
            runTimer();

            ManageView();

            /*
            도착 버튼 클릭 시, 서비스 종료 및 기존 앱으로 돌아가기
             */
            floatingView.findViewById(R.id.notiArrived).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    BackToApp();
                }
            });
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runTimer()
    {
        handler.post(runnable);
    }

    public void NotificationUpdate(String time){

        Intent notificationIntent = new Intent(this, StoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this, CHANNEL_1_ID)
                .setContentTitle(title)
                .setContentText("Time elapsed : " + time)
                .setSmallIcon(R.drawable.ic_timer)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

    }

    public void setView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.activity_stop_watch_floating_view, null);
        floatingView.setBackgroundColor(Color.TRANSPARENT);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT,
                LAYOUT_FLAG,
                FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE
        );

        params.x = 0;
        params.y = 0;
        params.gravity = Gravity.CENTER | Gravity.CENTER;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        //움직일 수 있는 뷰
        floatingView.setOnTouchListener(new View.OnTouchListener(){

            private int x, y;
            private float touchedX, touchedY;
            private WindowManager.LayoutParams updatedParams = params;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:

                        x = updatedParams.x;
                        y = updatedParams.y;

                        touchedX = event.getRawX();
                        touchedY = event.getRawY();

                    case MotionEvent.ACTION_MOVE:

                        updatedParams.x = (int) (x + event.getRawX() - touchedX);
                        updatedParams.y = (int) (x + event.getRawY() - touchedY);

                        windowManager.updateViewLayout(floatingView, updatedParams);

                    default :
                        return true;
                }
            }


        });
    }

    public void ManageView(){

        //set Text
        TextView storyTitle = (TextView)floatingView.findViewById(R.id.storyTitle);
        storyTitle.setText(title);

        //remove the view by cancel button
        floatingView.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                windowManager.removeView(floatingView);
                //stopSelf();
            }
        });
    }

    public void BackToApp(){
        //게임 시작 버튼 활성화
        final Intent intentLocal = new Intent();
        intentLocal.setAction("activateButton");
        sendBroadcast(intentLocal);

        //Tmap 실행 후 기본앱으로 돌아오기 -- tmap은 종료 API x => 강제종료?밖에 답이 없나?
        Intent dialogIntent  = new Intent(getApplicationContext(), LocationCheckingActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);

        // 창닫기
        windowManager.removeView(floatingView);

        stopForeground(true);
        stopSelf();
        handler.removeCallbacks(runnable);
    }



    //메인 스레드
    final Handler handler = new Handler();
    //워킹 스레드
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            int secs = seconds % 60;

            time = String.format(Locale.getDefault(), "%d:%02d:%02d",
                    hours, minutes, secs);

            if (running) {
                seconds++;
            }

            NotificationUpdate(time);

            textTime = (TextView)floatingView.findViewById(R.id.textViewTime) ;
            textTime.setText(time);

            // Post the code again
            // with a delay of 1 second.
            handler.postDelayed(this, 1000);

               /* intentLocal.putExtra("elapsedTime", time);
                sendBroadcast(intentLocal);*/
                /*final Intent intentLocal = new Intent();
                intentLocal.setAction("StopWatch");*/
        }

    };
}
