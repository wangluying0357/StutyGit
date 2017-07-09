package com.example.testtest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.testtest.R;
import com.example.webview.Html5Activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;



public class MainActivity extends Activity {

    
    public Button btn_click;
    boolean type = true;
    int mcc, mnc, lac, cellid;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        btn_click = (Button) findViewById(R.id.btn_click);
    }

    
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if(v.getId()==R.id.btn_click){
            try {
                getGSMCellLocationInfo();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    
    @Override  
    public boolean onKeyDown(int keyCode, KeyEvent event) {  
        // TODO Auto-generated method stub  
        return super.onKeyDown(keyCode, event);  
    }
    
    /** 
     * 获取手机基站信息 
     * @throws JSONException  
     * MCC，Mobile Country Code，移动国家代码（中国的为460）。
     * MNC，Mobile Network Code，移动网络号码（中国移动为0，中国联通为1，中国电信为2）。
     * LAC，Location Area Code，位置区域码。
     * CID，Cell Identity，基站编号。
     * BSSS，Base station signal strength，基站信号强度。
     */  
    public void getGSMCellLocationInfo() throws JSONException{  
          
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);  
          
        String operator = manager.getNetworkOperator();  
        /**通过operator获取 MCC 和MNC，一般是5-6位的字符串，前3位为MCC，后面的是MNC。 */  
        mcc = Integer.parseInt(operator.substring(0, 3));  
        mnc = Integer.parseInt(operator.substring(3));  
        Log.e("", "------>mcc: "+mcc+"----->mnc: "+mnc);
        
        /**电信用的是cdma制式网络，移动和联通用的gsm网络，这两种网络基站信息封装类在android中是不同的，cdma要用CdmaCellLocation，gsm要用GsmCellLocation。
从TelephonManager获取基站定位信息CellLocation，其中封装了需要的CID和LAC等信息。**/
        GsmCellLocation location = (GsmCellLocation) manager.getCellLocation();  
          
        /**通过GsmCellLocation获取中国移动和联通 LAC 和cellID */  
        lac = location.getLac();  
        cellid = location.getCid();  
        Log.e("", "------>lac: "+lac+"----->cellid: "+cellid);
        /**通过CdmaCellLocation获取中国电信 LAC 和cellID */  
//          <span style="white-space:pre">        </span> /*CdmaCellLocation location1 = (CdmaCellLocation) mTelephonyManager.getCellLocation();  
//           <span style="white-space:pre">   </span> lac = location1.getNetworkId();  
//          <span style="white-space:pre">        </span> cellId = location1.getBaseStationId();  
//          <span style="white-space:pre">        </span> cellId /= 16;*/    
          
        int strength = 0;  
        /**通过getNeighboringCellInfo获取BSSS */  
        List<NeighboringCellInfo> infoLists = manager.getNeighboringCellInfo();  
        System.out.println("infoLists:"+infoLists+"     size:"+infoLists.size());  
        for (NeighboringCellInfo info : infoLists) {  
            strength+=(-133+2*info.getRssi());// 获取邻区基站信号强度    
            //info.getLac();// 取出当前邻区的LAC   
            //info.getCid();// 取出当前邻区的CID   
            System.out.println("rssi:"+info.getRssi()+"   strength:"+strength);  
        }  
          
          
        //以下内容是把得到的信息组合成json体，然后发送给我的服务器，获取经纬度信息  

//    <span style="white-space:pre">      </span>//如果你没有服务器支持，可以发送给BaiduMap，GoogleMap等地图服务商，具体看定位相关的API格式要求  
            JSONObject item = new JSONObject();  
            item.put("cid", cellid);  
            item.put("lac", lac);  
            item.put("mnc", mnc);  
            item.put("mcc", mcc);  
            item.put("strength", strength);  
            item.put("key", "16e81bc8d9251d6692db39f6ec2b3e12");
              
            JSONArray cells = new JSONArray();  
            cells.put(0, item);  
              
            JSONObject json = new JSONObject();  
            json.put("cells", cells);  
              
            CellLocationTask task = new CellLocationTask(json);  
            task.execute();  
              
        }
    /** 
     * 异步请求，通过封装的手机基站信息json体 
     * @author Administrator 
     * 
     */  
    class CellLocationTask extends AsyncTask<String, Void, String>{  
          
        private JSONObject mJson;  
        private HttpClient mClient;  
        private HttpResponse response;  
        private String responseString;  
        public CellLocationTask(JSONObject json) {  
            this.mJson = json;  
              
        }  
        @Override  
        protected void onPreExecute() {  
            super.onPreExecute();  
            mClient = new DefaultHttpClient();  
            mClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);  
            mClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 15000);  
        }  
          
          
        @Override  
        protected String doInBackground(String... params) {  
            String url = "http://v.juhe.cn/cell/get?mnc="+mnc+"&cell="+cellid+"&lac="+lac+"&key=16e81bc8d9251d6692db39f6ec2b3e12";  
              
            try {  
                HttpPost post = new HttpPost(url);  
//                post.setEntity(new StringEntity(mJson.toString(), HTTP.UTF_8));  
//                post.addHeader("Content-Type", "application/json");  
                response = mClient.execute(post);  
                int statusCode = response.getStatusLine().getStatusCode();  
                System.out.println("doinbackground:"+statusCode);  
                if (statusCode == 200) {  
                    responseString  = EntityUtils.toString(response.getEntity());  
                    System.out.println("返回结果:"+responseString);  
                }  
                  
                  
            } catch (ClientProtocolException e) {  
                e.printStackTrace();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
              
              
              
            return responseString;  
        }  
          
        @Override  
        protected void onPostExecute(String result) {  
            super.onPostExecute(result);  
            System.out.println("onPostExecute:"+result);  
              
            JSONObject json;  
            try {  
                json = new JSONObject(result);  
                JSONObject mresult = json.getJSONObject("result");  
                JSONArray datas = mresult.getJSONArray("data");  
                JSONObject data = datas.getJSONObject(0);
                double lat = data.optDouble("LAT");  
                double lng = data.optDouble("LNG");  
                Log.e("", "------>lat: "+lat+"----->lng:"+lng);   
                fg
//                mLocationGeoPoint = new GeoPoint((int)(lat*1E6), (int)(lng*1E6));  
                  
//                CustomOverlay overlay = new CustomOverlay(mAppMain);  
//                mMapView.getOverlays().add(overlay);  
//                mMapController.animateTo(mLocationGeoPoint);  
                  
            } catch (JSONException e) {  
                e.printStackTrace();  
            }  
              
        }  
          
    }  
}
