package com.example.toshi.applausometer;


import android.app.Activity;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.os.Bundle;



import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;
import android.widget.ArrayAdapter;

import android.widget.ImageView;
import android.widget.ListView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    public class myTimerTask extends TimerTask {
        @Override
        public void run() {
            TimerMethod();
        }
    }

    public class myTimerTask2 extends TimerTask{
        @Override
        public void run() { TimerMethod2();


        }

    }

    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    public boolean firstTime;
    private String s;
    public Timer myTimer,myTimer2;
    public boolean isRecording = false;
    public double progress = 0;
    public double ticks = 0;
    private ImageView myImage;
    public myTimerTask TimerTask1;
    public myTimerTask2 TimerTask2;
    public ListView lista1;
    public int num = 1;
    public String punteggio;

    public void TimerMethod2()
    {
        this.runOnUiThread(Timer_Tick2);
    };

    public void TimerMethod()

    {
        this.runOnUiThread(Timer_Tick);
    };


    private Runnable Timer_Tick = new Runnable() {
        long iu;
        String su;
        int RR;
        public void run() {

            //This method runs in the same thread as the UI.

            //Do something to the UI thread here
            if (isRecording) {

                iu=0;

            iu = myAudioRecorder.getMaxAmplitude();

                if (iu>0)
                        if (iu<=200) { RR=R.drawable.lights0; } else
                        if (iu<=2400) { RR=R.drawable.lights1; } else
                        if (iu<=5600) { RR=R.drawable.lights2; } else
                        if (iu<=11800) { RR=R.drawable.lights3; } else
                        if (iu<=15000) { RR=R.drawable.lights4; } else
                        if (iu<=19200) { RR=R.drawable.lights5; } else
                        if (iu<=22400) { RR=R.drawable.lights6; } else
                        if (iu<=25600) { RR=R.drawable.lights7; } else
                        if (iu<=29800) { RR=R.drawable.lights8; } else
                        if (iu<=31800) { RR=R.drawable.lights9; } else
                                        RR=R.drawable.lights10;

                myImage.setImageResource(RR);


                TextView myTextViewLOC = (TextView) findViewById(R.id.textView2);

                if (progress++==10) {
                    final Animation animation3 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_stop);
                    ImageButton myImageButtonLOC =(ImageButton) findViewById(R.id.myImageButton);
                    myImageButtonLOC.startAnimation(animation3);
                    //qua
                    myTextViewLOC.startAnimation(animation3);


                }
                ticks += iu;
                DecimalFormat df = new DecimalFormat("00");
                su = String.valueOf(df.format((float)(iu/330)));
                TextView tv = (TextView) findViewById(R.id.textView);
                tv.setText("sto ascoltando    "+su);
                df = new DecimalFormat("0");
                myTextViewLOC.setText(" "+String.valueOf(df.format(8 - ((progress - 1) / 10)))+" sec");


            }

        }



    };

    public Runnable Timer_Tick2 = new Runnable() {

        double score;

        public void run() {

            final Animation animation2 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_down);

            ImageButton myImageButtonLOC =(ImageButton) findViewById(R.id.myImageButton);
            myImageButtonLOC.startAnimation(animation2);
            //qua

            final TextView myTextViewLOC = (TextView) findViewById(R.id.textView2);
            myTextViewLOC.startAnimation(animation2);

            animation2.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                }

                @Override
                public void onAnimationEnd(Animation arg0) {
                    ImageButton myImageButtonLOC = (ImageButton) findViewById(R.id.myImageButton);
                    final Animation animation4 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_stop_down);
                    myImageButtonLOC.startAnimation(animation4);
                    myTextViewLOC.startAnimation(animation4);
                    myTextViewLOC.setText("<----   Click me!");
                }
            });



            isRecording = false;
            score= ((ticks/progress)/660)+50;
            double iu= ticks/progress;
            int RR=0;
            if (iu>=0)
                if (iu<=200) { RR=R.drawable.lights0; } else
                if (iu<=2400) { RR=R.drawable.lights1; } else
                if (iu<=5600) { RR=R.drawable.lights2; } else
                if (iu<=11800) { RR=R.drawable.lights3; } else
                if (iu<=15000) { RR=R.drawable.lights4; } else
                if (iu<=19200) { RR=R.drawable.lights5; } else
                if (iu<=22400) { RR=R.drawable.lights6; } else
                if (iu<=25600) { RR=R.drawable.lights7; } else
                if (iu<=29800) { RR=R.drawable.lights8; } else
                if (iu<=31800) { RR=R.drawable.lights9; } else
                    RR=R.drawable.lights10;

            myImage.setImageResource(RR);


            DecimalFormat df = new DecimalFormat("00.00");


            punteggio = String.valueOf(df.format(score));
            if (num>7)
                adapter.remove(adapter.getItem(0));

            TextView tv = (TextView) findViewById(R.id.textView);
            tv.setText("Applause!            "+punteggio+"!");

            adapter.add(String.valueOf(num++) + ": " + punteggio);

            Toast.makeText(getApplicationContext(), "Ha totalizzato "+punteggio+" punti!" , Toast.LENGTH_LONG).show();
            progress = 0;
            ticks = 0;
            lista1.setVisibility(View.VISIBLE);
            myTimer2.cancel();
            myAudioRecorder.stop();
            myAudioRecorder.release();
            /*myAudioRecorder = null;
            myAudioRecorder = new MediaRecorder();
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myAudioRecorder.setOutputFile(outputFile);
            try {
                myAudioRecorder.prepare();
                myAudioRecorder.start();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/


        }
    };

    public void reggistra() {


        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);
        try {
            myAudioRecorder.prepare();
        myAudioRecorder.start();
    } catch (IllegalStateException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }





    myTimer2 = new Timer();
    TimerTask2 = new myTimerTask2();

    myTimer2.schedule(TimerTask2, 7000, 7000);
        lista1.setVisibility(View.INVISIBLE);
    isRecording = true;


    Toast.makeText(getApplicationContext(), "Sto Ascoltando...", Toast.LENGTH_LONG).show();
};



    @Override
    public void onSaveInstanceState(Bundle outState)
    {
       // Toast.makeText(getApplicationContext(), "save", Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myImage = (ImageView) findViewById(R.id.meter);
        myTimer = new Timer();
        TimerTask1 = new myTimerTask();
        TimerTask2 = new myTimerTask2();
        lista1 = (ListView) findViewById(R.id.listView);
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList);
        lista1.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        myTimer.schedule(TimerTask1, 0, 100);

        ImageButton myImageButton = (ImageButton) findViewById(R.id.myImageButton);
        final Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.anim_x);
        //qua

        TextView myTextViewLOC = (TextView) findViewById(R.id.textView2);
        myTextViewLOC.startAnimation(animation1);




        myImageButton.setOnTouchListener(
                new View.OnTouchListener(){
                    @Override
                    public boolean onTouch (View v, MotionEvent event)
                    {

                        if ((event.getActionMasked() == MotionEvent.ACTION_DOWN) && (isRecording==false))
                        {
                            reggistra();
                            v.startAnimation(animation1);

                            //qua

                            TextView myTextViewLOC = (TextView) findViewById(R.id.textView2);
                            myTextViewLOC.startAnimation(animation1);

                            return false;}

                return true;    }

                }


        );




        if (firstTime) {
            firstTime = false;
        }

        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";
        myAudioRecorder = new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);
        final Animation animation4 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_stop_down);
        myImageButton.startAnimation(animation4);
//qua

        //TextView myTextViewLOC = (TextView) findViewById(R.id.textView2);
        myTextViewLOC.startAnimation(animation4);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //  return true;
        //}
        return super.onOptionsItemSelected(item);
    }
}


