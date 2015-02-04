package com.example.jcobreros.luces;

import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Luces extends ActionBarActivity {

    private Handler handler = new Handler();

    int udpServerPort = 48899;
    int maxUdpDatagramLen = 1024;
    int udpClientPort = 8899;

    boolean needToSendData = false;
    float H = 0;
    float S = 0;
    float B = 0;

    public static String myIp;

    Button bSearch;
    TextView textInfo;
    ListView clientListView;

    SeekBar seekBarH;
    SeekBar seekBarS;
    SeekBar seekBarB;

    List<String> rawClientList = new ArrayList<String>();
    List<String> clientListIP = new ArrayList<String>();
    List<String> clientListName = new ArrayList<String>();
    List<String> clientListFullName = new ArrayList<String>();
    int currentClientIndex = 0;

    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_luces);
        bSearch = (Button)findViewById(R.id.bSearch);
        textInfo = (TextView)findViewById(R.id.textInfo);
        clientListView = (ListView)findViewById(R.id.clientListView);

        seekBarH = (SeekBar) findViewById(R.id.seekBarH);
        seekBarS = (SeekBar) findViewById(R.id.seekBarS);
        seekBarB = (SeekBar) findViewById(R.id.seekBarB);
        seekBarH.setMax(360);

        arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                clientListFullName );

        bSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //new UDPClientThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                textInfo.setText("Searching Wifi Lights");
                new FindLightsThread().execute();
            }
        });

        clientListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                currentClientIndex = position;
                textInfo.setText("Controlling " + clientListIP.get(currentClientIndex));
            }
        });

        seekBarH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                H = seekBar.getProgress();
                needToSendData=true;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {            }
        });
        seekBarS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                S = seekBar.getProgress();
                needToSendData=true;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {            }
        });
        seekBarB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                B = seekBar.getProgress();
                needToSendData=true;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {            }
        });

        new FindLightsThread().execute();
        handler.postDelayed(runnable, 20);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        myIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(needToSendData)
            {
                sendData();
                needToSendData=false;
            }
            handler.postDelayed(this, 20);
        }
    };

    public void sendData()
    {
        new UDPClientThread().execute();
        TextView consola = (TextView) findViewById(R.id.consola);
        consola.setText(udpResult + counter);
    }
    String udpResult = "";
    int counter = 0;

    public class UDPClientThread extends AsyncTask<String, Integer, Boolean>
    {

        @Override
        protected Boolean doInBackground(String... arg0)
        {
            float[] hsv = new float[]{H, S/100f, B/100f};
            int colorcillo = Color.HSVToColor(hsv);

            float redC = Color.red(colorcillo)/2.5f;
            float greenC = Color.green(colorcillo)/2.5f;
            float blueC = Color.blue(colorcillo)/2.5f;

            String udpMsgR = "PWM 2 30000 " + (int)redC;
            String udpMsgG = "PWM 3 30000 " + (int)greenC;
            String udpMsgB = "PWM 1 30000 " + (int)blueC;
            DatagramSocket ds = null;
            try {

                String address = clientListIP.get(currentClientIndex);
                Log.i("Sending",address);
                ds = new DatagramSocket();
                InetAddress clientAddress = InetAddress.getByName(address);
                DatagramPacket dp;
                dp = new DatagramPacket(udpMsgR.getBytes(), udpMsgR.length(), clientAddress, udpClientPort);
                ds.send(dp);
                dp = new DatagramPacket(udpMsgG.getBytes(), udpMsgG.length(), clientAddress, udpClientPort);
                ds.send(dp);
                dp = new DatagramPacket(udpMsgB.getBytes(), udpMsgB.length(), clientAddress, udpClientPort);
                ds.send(dp);
                udpResult = "Sent ";
                counter++;

            } catch (SocketException e) {
                e.printStackTrace();
                udpResult = e.toString();
            }catch (UnknownHostException e) {
                e.printStackTrace();
                udpResult = e.toString();
            } catch (IOException e) {
                e.printStackTrace();
                udpResult = e.toString();
            } catch (Exception e) {
                e.printStackTrace();
                udpResult = e.toString();
            } finally {
                if (ds != null) {
                    ds.close();
                }
            }
            return false;
        }
    }

    public class FindLightsThread extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... arg0) {
            DatagramSocket ds = null;

            byte[] rawMessage = new byte[maxUdpDatagramLen];
            String incomingMessage;
            DatagramPacket recvDatagramPacket = new DatagramPacket(rawMessage, rawMessage.length);

            try {
                String[] split = myIp.split("\\.");
                String UDPAddr0 = "192.168.0.255";
                String UDPAddr1 = "192.168.1.255";
                String usrPassword = "HF-A11ASSISTHREAD";

                ds = new DatagramSocket();
                ds.setSoTimeout(1000);

                rawClientList.clear();

                InetAddress serverAddress = InetAddress.getByName(UDPAddr0);
                DatagramPacket sendDatagramPacket = new DatagramPacket(usrPassword.getBytes(), usrPassword.length(), serverAddress, udpServerPort);
                ds.send(sendDatagramPacket);
                Log.i("UDP", "Packet Sent");

                serverAddress = InetAddress.getByName(UDPAddr1);
                sendDatagramPacket = new DatagramPacket(usrPassword.getBytes(), usrPassword.length(), serverAddress, udpServerPort);
                ds.send(sendDatagramPacket);
                Log.i("UDP", "Packet Sent");



                while (true) {      //Will keep going until timeOut occurs
                    ds.receive(recvDatagramPacket);
                    incomingMessage = new String(rawMessage, 0, recvDatagramPacket.getLength());
                    Log.i("UDP Packet Received", incomingMessage);
                    rawClientList.add(incomingMessage);
                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ds != null) {
                    ds.close();
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean r)
        {
            clientListName.clear();
            clientListIP.clear();
            clientListFullName.clear();

            for (String i: rawClientList)
            {
                String[] split = i.split(",");
                clientListIP.add(split[0]);
                clientListName.add(split[2]);
                clientListFullName.add(split[2] + " @ " + split[0]);
            }

            if (!clientListIP.isEmpty())
            {
                String[] split = clientListIP.get(0).split("\\.");
                String addr = split[0]+"."+split[1]+"."+split[2]+".255";
                clientListIP.add(0,addr);
                clientListName.add(0,"Todas");
                clientListFullName.add(0,"Todas @ " + addr);
            }

            clientListView.setAdapter(arrayAdapter);
            textInfo.setText("All cool");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_luces, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
