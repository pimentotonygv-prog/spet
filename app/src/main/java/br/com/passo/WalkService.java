package br.com.passo;
import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import org.json.*;

public class WalkService extends Service implements LocationListener {
 static WalkService live;
 JSONObject walk;JSONArray points;boolean paused=true;long accumulated,started,lastFix,lastSaved;
 double meters,weight;Location anchor,latest;boolean breakLine=true;LocationManager gps;
 String signal="Buscando sua localização…";
 final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable tick=new Runnable(){public void run(){
  if(!paused&&SystemClock.elapsedRealtime()-lastFix>15000){signal="GPS fraco · aguardando uma posição confiável";anchor=null;breakLine=true;}
  if(walk!=null&&SystemClock.elapsedRealtime()-lastSaved>10000)persist();handler.postDelayed(this,3000);
 }};
 public void onCreate(){super.onCreate();live=this;gps=(LocationManager)getSystemService(LOCATION_SERVICE);handler.post(tick);}
 public int onStartCommand(Intent i,int flags,int id){
  if(i==null){stopSelf();return START_NOT_STICKY;}
  String action=i.getAction();
  if("start".equals(action)&&walk==null){walk=new JSONObject();points=new JSONArray();weight=getSharedPreferences("settings",0).getFloat("weight",70);put("date",System.currentTimeMillis());put("weight",weight);lastFix=SystemClock.elapsedRealtime();}
  if(walk==null){stopSelf();return START_NOT_STICKY;}
  if("pause".equals(action))pause();
  else if("finish".equals(action)){
   pause();JSONObject h=Store.read(this,"history.json");JSONArray a=h.optJSONArray("walks");if(a==null)a=new JSONArray();a.put(walk);
   try{h.put("walks",a);}catch(JSONException ignored){}Store.write(this,"history.json",h);deleteFile("active.json");walk=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
  }else if("start".equals(action)||"resume".equals(action)){
   notification();if(paused){paused=false;started=SystemClock.elapsedRealtime();lastFix=started;anchor=null;breakLine=true;
    try{gps.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,0,this);}catch(SecurityException e){signal="Permissão de localização necessária";pause();}
   }persist();
  }
  return START_NOT_STICKY;
 }
 void notification(){NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("walk","Caminhada em andamento",NotificationManager.IMPORTANCE_LOW));PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);startForeground(1,new Notification.Builder(this,"walk").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("step · caminhada em andamento").setContentText("Toque para pausar ou finalizar").setContentIntent(open).setOngoing(true).build());}
 long elapsed(){return accumulated+(paused?0:SystemClock.elapsedRealtime()-started);}
 void pause(){if(!paused){accumulated=elapsed();paused=true;}gps.removeUpdates(this);anchor=null;breakLine=true;persist();}
 void put(String key,Object value){try{walk.put(key,value);}catch(JSONException ignored){}}
 void persist(){if(walk==null)return;put("duration",elapsed());put("meters",meters);put("points",points);put("calories",Store.calories(weight,elapsed(),meters));put("paused",true);Store.write(this,"active.json",walk);lastSaved=SystemClock.elapsedRealtime();}
 public void onLocationChanged(Location l){
  if(paused)return;long age=(SystemClock.elapsedRealtimeNanos()-l.getElapsedRealtimeNanos())/1000000;
  if(!l.hasAccuracy()||l.getAccuracy()>25||age>10000||age<0||l.isFromMockProvider()){signal="GPS fraco · ponto descartado";return;}
  latest=new Location(l);lastFix=SystemClock.elapsedRealtime();signal="GPS ativo · margem estimada ±"+Math.round(l.getAccuracy())+" m";
  if(anchor!=null){float d=anchor.distanceTo(l);double dt=(l.getElapsedRealtimeNanos()-anchor.getElapsedRealtimeNanos())/1e9;
   if(dt<=0)return;if(dt>20){anchor=null;breakLine=true;}
   else if(d/dt>4.5){signal="Oscilação do GPS · ponto descartado";latest=null;return;}
   else if(d<Math.max(4,Math.min(12,(anchor.getAccuracy()+l.getAccuracy())*0.35)))return;
   else meters+=d;
  }
  JSONObject p=new JSONObject();try{p.put("lat",l.getLatitude());p.put("lon",l.getLongitude());p.put("break",breakLine);p.put("accuracy",l.getAccuracy());p.put("time",l.getTime());points.put(p);}catch(JSONException ignored){}
  breakLine=false;anchor=new Location(l);persist();
 }
 public void onProviderDisabled(String provider){signal="GPS desligado · ative a localização";anchor=null;latest=null;breakLine=true;}
 public void onProviderEnabled(String provider){}
 public void onStatusChanged(String provider,int status,Bundle extras){}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){handler.removeCallbacks(tick);gps.removeUpdates(this);if(walk!=null)pause();live=null;super.onDestroy();}
}
