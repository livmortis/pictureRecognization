package com.example.xzy.ipcrecognization;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Path;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.xzy.ipcrecognization.luban.Luban;
import com.example.xzy.ipcrecognization.luban.OnCompressListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    Button takephotoBtn;
    Button recogBtn;
    ImageView picIv;
    private boolean isCustom = true;
    private String name = null;
//    String address = "https://aip.baidubce.com/rest/2.0/image-classify/v1/object_detect";  //图像主题检测
//    String address = "https://aip.baidubce.com/rest/2.0/image-classify/v2/dish";  //菜品识别
//    String address = "https://aip.baidubce.com/rest/2.0/image-classify/v2/logo";  //logo识别
//    String address = "https://aip.baidubce.com/rest/2.0/image-classify/v1/animal";  //动物识别
//    String address = "https://aip.baidubce.com/rest/2.0/image-classify/v1/plant";  //植物识别
//    String address = "https://aip.baidubce.com/rest/2.0/image-classify/v1/car";  //汽车识别
    String address = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/classification/ipcrecognition";  //乐橙IPC识别

    String imageBase64;
    ProgressBar bar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takephotoBtn = (Button) findViewById(R.id.takephoto);
        recogBtn = (Button) findViewById(R.id.recognize);
        picIv = (ImageView) findViewById(R.id.pic);
        bar = (ProgressBar) findViewById(R.id.progress);

        if (isCustom) {
            name = "产品种类";
        }else{
            name = "物种";
        }


        String[] permission_external_storage = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE } ;
        int request_external_storage = 100 ;
        ActivityCompat.requestPermissions(this, permission_external_storage, request_external_storage);



        takephotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (Intent.ACTION_PICK , MediaStore.Images.Media.EXTERNAL_CONTENT_URI ) ;
                intent.setType("image/*");
                startActivityForResult(intent , 0);
            }
        });

        recogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pathAfterCompress != null) {
                    if (bar.getVisibility() == View.GONE) {
                        bar.setVisibility(View.VISIBLE);
                    } else {
                        bar.setVisibility(View.GONE);
                    }

                    //图片uri转base64
                    imageBase64 = recognize(pathAfterCompress);

                    //网络请求获取token的json
                    try {
                        getToken();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }else{
                    System.out.print("picture is empty~");
                }
            }
        });
    }


    Handler handler  = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String accessTokenJson = msg.obj.toString();

            //从token的json中解析出accessToken
            String accessToken =parser(accessTokenJson);

            //网络请求上传图片，获取分类结果
            if (accessToken != null && imageBase64!=null) {
                upload(imageBase64,accessToken);
            }
        }
    };

    Handler handler2 = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String resultJson = msg.obj.toString();
            bar.setVisibility(View.GONE);

            try {
                JSONObject jsonObject = new JSONObject(resultJson);
                if (jsonObject.getString("error_msg") != null) {
                    Toast.makeText(MainActivity.this,""+jsonObject.getString("error_code")+"  "+jsonObject.getString("error_msg"),Toast.LENGTH_LONG).show();
                    return;
                }

                String result = jsonObject.getString("result");
//                String color = jsonObject.getString("color_result");

                JSONArray jsonArray = new JSONArray(result);

                JSONObject first = jsonArray.getJSONObject(0);
                String firstname = first.getString("name");
                double firstscore = first.getDouble("score")*100;
                String secondname = null;
                double secondscore = 0;
                String thirdname = null;
                double thirdscore = 0;
                String forthname = null;
                double forthscore = 0;
                
                if (jsonArray.length() >= 2) {
                    JSONObject second = jsonArray.getJSONObject(1);
                    secondname = second.getString("name");
                    secondscore = second.getDouble("score")*100;
                }
                if (jsonArray.length() >= 3) {
                    JSONObject third = jsonArray.getJSONObject(2);
                    thirdname = third.getString("name");
                    thirdscore = third.getDouble("score")*100;
                }

                if (jsonArray.length() >= 4) {
                    JSONObject forth = jsonArray.getJSONObject(3);
                    forthname = forth.getString("name");
                    forthscore = forth.getDouble("score")*100;
                }

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setMessage(
//                        "颜色 - " + color +
                        "\n      "+name+" - " + firstname + "  概率 - " + firstscore  +
                        "%\n      "+name+" - " + secondname + "  概率 - " + secondscore  +
                        "%\n      "+name+" - " + thirdname + "  概率 - " + thirdscore +
                        "%\n      "+name+" - " + forthname + "  概率 - " + forthscore
                );
                dialog.show();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    private String parser(String accessTokenJson) {
        String accesstoken = null;
        try {
            JSONObject obj = new JSONObject(accessTokenJson);
            accesstoken = obj.getString("access_token");

            String time = obj.getString("expires_in");
            Log.d("the token time is :" , time);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return accesstoken;
    }


    public void getToken() throws IOException {


        String urlForToken = "https://aip.baidubce.com/oauth/2.0/token";
        String apiKey = "1abZFCsUsIBzBipBXOUIE19g";
        String secretKey = "tn6G5SBY9hIMNZ1ha7GM9cK4PPpZWlSv";
        final String urlWithParams = urlForToken + "?" + "grant_type=client_credentials&" +
                "client_id=" + apiKey + "&" + "client_secret=" + secretKey;


        new Thread(new Runnable() {
            @Override
            public void run() {
                URL finalUrl = null;


                try {
                    finalUrl = new URL(urlWithParams);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }


                try {
                    Log.d("xzy", "enter son thread");
                    HttpURLConnection conect = (HttpURLConnection) finalUrl.openConnection();
                    conect.setRequestMethod("GET");
                    conect.setConnectTimeout(8000);
                    conect.setReadTimeout(8000);
                    conect.connect();

                    InputStream is = conect.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = br.readLine())!=null) {
                        sb.append(line);
                    }
                    String token = sb.toString();

                    if (token != null) {
                        Message message = new Message();
                        message.obj = token;
                        handler.sendMessage(message);
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }



            }
        }).start();
    }





    URL url = null;
    String encoding = null;
    private void upload(final String imageBase64, String accessToken) {


        encoding = "UTF-8";
        if (address.contains("nlp")) {
            encoding = "GBK";
        }

        //添加token
        String addressWithToken = address + "?access_token=" + accessToken;

        //得到url
        try {
            url = new URL(addressWithToken);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    if (!isCustom) {
                        connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                    }else{
                        connection.setRequestProperty("Content-Type","application/json");
                    }
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setConnectTimeout(10000);
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();


                    //传输数据
                    OutputStream os = connection.getOutputStream();
                    DataOutputStream dtos = new DataOutputStream(os);
                    String iamgeURLEncoder = URLEncoder.encode(imageBase64, "UTF-8");
                    String params = "image=" + iamgeURLEncoder;
//                    + "&with_face=" + 0;
                    dtos.write(params.getBytes(encoding));
                    dtos.flush();
                    dtos.close();

                    //建立连接
//                    connection.connect();


                    //获取数据
                    InputStream is = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    Log.d("the respose is: " , sb.toString());

                    Message message2 = new Message();
                    message2.obj = sb.toString();
                    handler2.sendMessage(message2);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }


    //图片uri转base64
    private String recognize(String pathAfterCompress) {
        InputStream is = null;
        byte[] data = null;
        String result = null;

        try {
            Log.d("xzy", "picture path is : " + pathAfterCompress);

            is = new FileInputStream(pathAfterCompress);
            data = new byte[is.available()];
            is.read(data);
            result = Base64.encodeToString(data, Base64.DEFAULT);
            Log.d("xzy", "picture with base64 is : " + result);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is) {
                try {
                    is.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }




    public String getRealPathFromURI(Uri contentURI){
        String result = "";
        Cursor cursor = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            cursor = this.getContentResolver() . query (contentURI , null , null , null , null , null);
        }
        if (cursor == null ){
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
            result = cursor.getString(index);
            cursor.close();
        }
        return result;
    }






    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            if (requestCode == 0) {
                Uri picuriBeforeCompress = data.getData();

                //图片压缩
                compress(picuriBeforeCompress);



            }
        }

    }

    String pathAfterCompress = null;
    private void compress(final Uri picuriBeforeCompress) {
        String path = getRealPathFromURI(picuriBeforeCompress);
        Luban.with(this)
                .load(path)
                .ignoreBy(100)
                .setTargetDir("/storage/emulated/0")
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        pathAfterCompress = file.getPath();
                        if (pathAfterCompress != null) {
                            picIv.setImageURI(picuriBeforeCompress);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();
    }


}
