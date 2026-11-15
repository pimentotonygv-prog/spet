package br.com.passo;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.*;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity implements LocationListener {
 static final String[] PROFILES={"Azul","Verde","Roxo","Laranja"};
 static final int[] LIGHT={0xff155ac4,0xff17633e,0xff7338ad,0xff9b420b};
 static final int[] DARK={0xff8abcff,0xff86d9ac,0xffd2adff,0xffffbd87};
 int accent,ink,muted,surface,card,onAccent,page;boolean dark,visible,searching,permissionPending,follow=true;
 SharedPreferences prefs;LinearLayout root,body;TextView timer,distance,calories,equivalent,status;
 Button primary,finish,retry;MapView map;Marker positionMarker;Polygon accuracyCircle;
 JSONObject selected;Location preview;LocationManager locations;String locationMessage="Buscando sua localização…";
 long searchBegan,drawn=-1;float touchX,touchY;String pendingAction;
 final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable tick=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};

 @Override public void onCreate(Bundle state){
  prefs=getSharedPreferences("settings",0);dark=prefs.getBoolean("dark",false);
  setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
  super.onCreate(state);int profile=Math.max(0,Math.min(3,prefs.getInt("profile",1)));
  accent=(dark?DARK:LIGHT)[profile];ink=dark?0xffedf4ef:0xff1b2923;muted=dark?0xffb8c5be:0xff4f6157;
  surface=dark?0xff121a17:0xfff4f8f2;card=dark?0xff23332b:0xffe4eee6;onAccent=dark?0xff152019:Color.WHITE;
  locations=(LocationManager)getSystemService(LOCATION_SERVICE);
  Configuration.getInstance().setUserAgentValue("step/1.1 (Android walking app)");
  Configuration.getInstance().setOsmdroidBasePath(new File(getFilesDir(),"maps"));
  Configuration.getInstance().setOsmdroidTileCache(new File(getCacheDir(),"tiles"));
  getWindow().setStatusBarColor(surface);getWindow().setNavigationBarColor(surface);
  getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  if(state!=null){page=state.getInt("page",0);long date=state.getLong("selected",-1);JSONArray a=Store.read(this,"history.json").optJSONArray("walks");if(a!=null)for(int j=0;j<a.length();j++){JSONObject w=a.optJSONObject(j);if(w!=null&&w.optLong("date")==date)selected=w;}}
  show();recover();
 }
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView text(String label,int size,int color){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(6));return t;}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 Button button(String label,Runnable action){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(15);b.setTextColor(onAccent);b.setBackground(background(accent,16));b.setMinHeight(dp(48));b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setOnClickListener(v->action.run());return b;}
 void addButton(LinearLayout parent,String label,Runnable action){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);parent.addView(button(label,action),p);}
 void show(){
  stopPreview();if(map!=null){map.onPause();map.onDetach();map=null;}
  timer=null;status=null;primary=null;finish=null;retry=null;positionMarker=null;accuracyCircle=null;drawn=-1;
  root=column();root.setBackgroundColor(surface);root.setPadding(dp(18),dp(10),dp(18),dp(10));
  if(Build.VERSION.SDK_INT>=35)root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(18),dp(10)+bars.top,dp(18),dp(10)+bars.bottom);return i;});
  setContentView(root);LinearLayout header=new LinearLayout(this);TextView brand=text("step",30,accent);brand.setTypeface(null,Typeface.BOLD);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));Button gear=button("⚙",this::settings);gear.setContentDescription("Configurações");header.addView(gear,new LinearLayout.LayoutParams(dp(54),dp(48)));root.addView(header);
  LinearLayout tabs=new LinearLayout(this);String[] names={"Caminhada","Mapa","Histórico"};for(int n=0;n<3;n++){final int target=n;TextView t=text(names[n],14,page==n?accent:muted);t.setGravity(Gravity.CENTER);t.setTypeface(null,page==n?Typeface.BOLD:Typeface.NORMAL);t.setOnClickListener(v->navigate(target));tabs.addView(t,new LinearLayout.LayoutParams(0,dp(46),1));}root.addView(tabs);
  body=column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  if(page==0)home();else if(page==1)mapPage();else history();refresh();
  if(page==1&&selected==null&&visible)beginLocation(false,true);
 }
 void navigate(int target){page=target;selected=null;follow=true;show();}
 boolean active(){return WalkService.live!=null&&WalkService.live.walk!=null;}
 boolean recording(){return active()&&!WalkService.live.paused;}
 JSONObject current(){return selected!=null?selected:active()?WalkService.live.walk:null;}
 void home(){
  ScrollView scroll=new ScrollView(this);LinearLayout content=column();scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
  content.addView(text("UM PASSO DE CADA VEZ",12,muted));content.addView(text("Encontre seu ritmo.",28,ink));
  LinearLayout clock=column();clock.setGravity(Gravity.CENTER);clock.setPadding(dp(8),dp(28),dp(8),dp(28));clock.setBackground(background(card,26));
  LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(14);content.addView(clock,cp);clock.addView(text("TEMPO EM MOVIMENTO",12,accent));
  timer=text("00:00:00",41,ink);timer.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);clock.addView(timer);
  distance=metric(content,"DISTÂNCIA","0,00 km");calories=metric(content,"ENERGIA ESTIMADA","0 kcal");equivalent=metric(content,"EQUIVALENTE ENERGÉTICO","0,00 g");
  content.addView(text("Equivalência teórica de energia. Não representa perda de peso real medida.",12,muted));status=text("Pronto para caminhar. O GPS pode ser encontrado após o início.",14,accent);content.addView(status);
  primary=button("Iniciar",this::toggle);content.addView(primary,new LinearLayout.LayoutParams(-1,dp(54)));finish=button("Finalizar",this::finishWalk);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(50));fp.topMargin=dp(8);content.addView(finish,fp);
  content.addView(text("Deslize para a esquerda para abrir o mapa →",12,muted));
 }
 TextView metric(LinearLayout parent,String label,String value){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(text(label,11,muted),new LinearLayout.LayoutParams(0,-2,1));TextView v=text(value,24,ink);v.setTypeface(null,Typeface.BOLD);row.addView(v);parent.addView(row);return v;}
 void mapPage(){
  map=new MapView(this);map.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource("OpenStreetMap",0,19,256,".png",new String[]{"https://tile.openstreetmap.org/"},"© OpenStreetMap contributors"));
  map.setMultiTouchControls(true);map.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);map.getController().setZoom(17.0);
  if(dark)map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
  body.addView(map,new LinearLayout.LayoutParams(-1,0,1));map.setContentDescription("Mapa do percurso. Use dois dedos para ampliar e arraste para mover.");
  TextView attribution=text("© OpenStreetMap contributors · mapa online",10,muted);attribution.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/copyright"))));body.addView(attribution);
  timer=text("00:00:00",26,ink);timer.setGravity(Gravity.CENTER);body.addView(timer);
  status=text(selected==null?"Buscando sua localização…":"Percurso salvo",13,muted);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(status);
  if(selected!=null){addButton(body,"Enquadrar percurso",()->{follow=true;drawn=-1;refresh();});LinearLayout row=new LinearLayout(this);row.addView(button("Compartilhar",()->export(false)),new LinearLayout.LayoutParams(0,dp(48),1));row.addView(button("Salvar imagem",()->export(true)),new LinearLayout.LayoutParams(0,dp(48),1));body.addView(row);}
  else{
   addButton(body,"◎ Atualizar posição",()->{follow=true;beginLocation(true,true);});
   retry=button("Tentar novamente",()->{follow=true;beginLocation(true,true);});body.addView(retry,new LinearLayout.LayoutParams(-1,dp(48)));
   LinearLayout controls=new LinearLayout(this);primary=button("Iniciar",this::toggle);finish=button("Finalizar",this::finishWalk);controls.addView(primary,new LinearLayout.LayoutParams(0,dp(50),1));controls.addView(finish,new LinearLayout.LayoutParams(0,dp(50),1));body.addView(controls);
  }
 }
 boolean precisePermission(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 boolean gpsOn(){return locations.isProviderEnabled(LocationManager.GPS_PROVIDER);}
 boolean fresh(Location l){return l!=null&&l.hasAccuracy()&&LocationPolicy.fresh(SystemClock.elapsedRealtimeNanos(),l.getElapsedRealtimeNanos());}
 Location livePosition(){return recording()?WalkService.live.latest:preview;}
 boolean hasReliablePosition(){Location l=livePosition();return LocationPolicy.ready(precisePermission(),precisePermission()&&gpsOn(),l!=null&&l.hasAccuracy(),l==null?Float.NaN:l.getAccuracy(),SystemClock.elapsedRealtimeNanos(),l==null?0:l.getElapsedRealtimeNanos());}
 void beginLocation(boolean force,boolean ask){
  if(page!=1||selected!=null||!visible||permissionPending)return;
  if(!precisePermission()){
   stopPreview();locationMessage="Permissão necessária: autorize localização precisa durante o uso para encontrar sua posição.";refresh();
   if(ask&&(force||!prefs.getBoolean("locationPrompted",false))){permissionPending=true;prefs.edit().putBoolean("locationPrompted",true).apply();requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);}return;
  }
  if(!gpsOn()){
   stopPreview();preview=null;locationMessage="GPS desligado. Ative a localização do celular e toque em Tentar novamente.";refresh();
   if(force)new AlertDialog.Builder(this).setTitle("Ativar localização").setMessage("O GPS precisa estar ligado para buscar sua posição e registrar a caminhada.").setPositiveButton("Abrir localização",(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("Agora não",null).show();return;
  }
  if(recording()){follow=true;locationMessage=WalkService.live.signal;refresh();return;}
  if(searching&&!force)return;
  stopPreview();if(force)preview=null;searching=true;searchBegan=SystemClock.elapsedRealtime();locationMessage="Buscando sua localização…";
  try{
   locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
   if(locations.isProviderEnabled(LocationManager.NETWORK_PROVIDER))locations.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,0,this);
  }catch(SecurityException e){stopPreview();locationMessage="A permissão de localização foi removida. Autorize localização precisa e tente novamente.";}
  catch(IllegalArgumentException e){stopPreview();locationMessage="Localização indisponível neste aparelho. Tente novamente em uma área aberta.";}
  refresh();
 }
 void stopPreview(){if(locations!=null)locations.removeUpdates(this);searching=false;}
 @Override public void onLocationChanged(Location l){
  if(!searching||!fresh(l)||l.isFromMockProvider()||l.getAccuracy()>1000)return;
  if(preview!=null&&fresh(preview)&&LocationManager.GPS_PROVIDER.equals(preview.getProvider())&&LocationManager.NETWORK_PROVIDER.equals(l.getProvider())&&l.getAccuracy()>preview.getAccuracy())return;
  preview=new Location(l);locationMessage="Posição encontrada · margem estimada ±"+Math.round(l.getAccuracy())+" m";refresh();
 }
 @Override public void onProviderDisabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider)){preview=null;locationMessage="GPS desligado. Ative a localização e toque em Tentar novamente.";refresh();}}
 @Override public void onProviderEnabled(String provider){if(page==1&&selected==null&&LocationManager.GPS_PROVIDER.equals(provider))beginLocation(true,false);}
 @Override public void onStatusChanged(String provider,int status,Bundle extras){}
 void refresh(){
  JSONObject w=current();long elapsed=w==null?0:w.optLong("duration");double meters=w==null?0:w.optDouble("meters");if(selected==null&&active()){elapsed=WalkService.live.elapsed();meters=WalkService.live.meters;}
  double weight=w==null?prefs.getFloat("weight",70):w.optDouble("weight",70);double kcal=Store.calories(weight,elapsed,meters);
  if(timer!=null)timer.setText(Store.time(elapsed));if(page==0){distance.setText(String.format(new Locale("pt","BR"),"%.2f km",meters/1000));calories.setText(String.format(new Locale("pt","BR"),"%.0f kcal",kcal));equivalent.setText(String.format(new Locale("pt","BR"),"%.2f g",kcal/7.7));}
  if(primary!=null){boolean paused=active()&&WalkService.live.paused;primary.setText(active()?(paused?"Continuar":"Pausar"):"Iniciar");primary.setEnabled(true);primary.setAlpha(1f);finish.setVisibility(active()?View.VISIBLE:View.GONE);}
  if(page==0&&status!=null)status.setText(!active()?"Pronto para caminhar. O GPS pode ser encontrado após o início.":WalkService.live.paused?"Caminhada pausada":WalkService.live.signal);
  if(map==null)return;
  if(w!=null){JSONArray pts=w.optJSONArray("points");if(pts!=null&&drawn!=pts.length()){drawn=pts.length();renderRoute(pts);}}
  if(selected!=null){status.setText("Percurso salvo · "+Store.number(meters/1000)+" km");return;}
  Location pos=livePosition();boolean good=fresh(pos);
  if(!precisePermission())locationMessage="Permissão negada. Autorize localização precisa nas configurações do Android e tente novamente.";
  else if(!gpsOn())locationMessage="GPS desligado. Ative a localização e toque em Tentar novamente.";
  else if(recording())locationMessage=WalkService.live.signal;
  else if(good)locationMessage=(pos.getAccuracy()<=25?"Posição encontrada":"Sinal de GPS fraco · aguarde melhorar")+" · margem estimada ±"+Math.round(pos.getAccuracy())+" m";
  else if(searching&&SystemClock.elapsedRealtime()-searchBegan<25000)locationMessage="Buscando sua localização…";
  else if(searching||pos!=null)locationMessage="Localização indisponível. Vá para uma área aberta e toque em Tentar novamente.";
  status.setText(locationMessage+"\nO círculo mostra a incerteza estimada; a posição pode variar.");
  if(retry!=null)retry.setVisibility(!hasReliablePosition()&&!locationMessage.startsWith("Buscando")?View.VISIBLE:View.GONE);
  if(good)renderPosition(pos);else if(positionMarker!=null){map.getOverlays().remove(positionMarker);map.getOverlays().remove(accuracyCircle);positionMarker=null;accuracyCircle=null;map.invalidate();}
 }
 void renderRoute(JSONArray pts){
  map.getOverlays().clear();positionMarker=null;accuracyCircle=null;ArrayList<GeoPoint> part=new ArrayList<>(),all=new ArrayList<>();
  for(int j=0;j<pts.length();j++){JSONObject p=pts.optJSONObject(j);if(p==null)continue;if(p.optBoolean("break")&&!part.isEmpty()){line(part);part=new ArrayList<>();}GeoPoint g=new GeoPoint(p.optDouble("lat"),p.optDouble("lon"));part.add(g);all.add(g);}line(part);
  if(selected!=null&&!all.isEmpty()){Marker end=new Marker(map);end.setPosition(all.get(all.size()-1));end.setTitle("Fim do percurso");map.getOverlays().add(end);if(follow){if(all.size()>1)map.zoomToBoundingBox(BoundingBox.fromGeoPoints(all),false,dp(32));else map.getController().setCenter(all.get(0));follow=false;}}
  map.invalidate();
 }
 void line(ArrayList<GeoPoint> pts){if(pts.size()<2)return;Polyline p=new Polyline();p.setPoints(pts);p.getOutlinePaint().setColor(accent);p.getOutlinePaint().setStrokeWidth(dp(5));map.getOverlays().add(p);}
 void renderPosition(Location l){
  GeoPoint g=new GeoPoint(l.getLatitude(),l.getLongitude());
  if(accuracyCircle==null){accuracyCircle=new Polygon();accuracyCircle.setFillColor((accent&0x00ffffff)|0x28000000);accuracyCircle.setStrokeColor(accent);accuracyCircle.setStrokeWidth(dp(1));map.getOverlays().add(0,accuracyCircle);}
  accuracyCircle.setPoints(Polygon.pointsAsCircle(g,Math.max(1,l.getAccuracy())));
  if(positionMarker==null){positionMarker=new Marker(map);GradientDrawable dot=background(accent,30);dot.setSize(dp(22),dp(22));dot.setStroke(dp(3),dark?0xff121a17:Color.WHITE);positionMarker.setIcon(dot);positionMarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER);map.getOverlays().add(positionMarker);}
  positionMarker.setPosition(g);positionMarker.setTitle("Sua posição · margem estimada ±"+Math.round(l.getAccuracy())+" m");
  if(follow){map.getController().setCenter(g);follow=false;}map.invalidate();
 }
 void toggle(){
  if(active()&&!WalkService.live.paused){command("pause");return;}
  String action=active()?"resume":"start";
  if(!precisePermission()){pendingAction=action;permissionPending=true;requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},12);return;}
  startWithoutWaiting(action);
 }
 void startWithoutWaiting(String action){stopPreview();command(action);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);}
 void command(String action){try{Intent i=new Intent(this,WalkService.class).setAction(action);if("start".equals(action))startForegroundService(i);else startService(i);handler.postDelayed(()->{if("finish".equals(action))navigate(2);else{if(page==1&&selected==null&&!recording())beginLocation(false,false);refresh();}},350);}catch(Exception e){toast("Não foi possível alterar o registro. Confira as permissões e o GPS.");}}
 @Override public void onRequestPermissionsResult(int code,String[] p,int[] g){super.onRequestPermissionsResult(code,p,g);if(code==12){permissionPending=false;String action=pendingAction;pendingAction=null;if(precisePermission()&&action!=null)startWithoutWaiting(action);else{toast("O Android exige permissão de localização para registrar o percurso em segundo plano.");refresh();}return;}if(code==10){permissionPending=false;if(precisePermission())beginLocation(true,false);else{locationMessage="Permissão negada. Autorize localização precisa nas configurações do Android e tente novamente.";refresh();new AlertDialog.Builder(this).setTitle("Localização precisa necessária").setMessage("Autorize a localização precisa nas permissões do step para registrar o percurso.").setPositiveButton("Abrir permissões",(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setNegativeButton("Agora não",null).show();}}}
 void finishWalk(){new AlertDialog.Builder(this).setTitle("Finalizar caminhada?").setMessage("O percurso e o resumo serão salvos neste celular.").setPositiveButton("Salvar caminhada",(d,w)->command("finish")).setNegativeButton("Continuar aqui",null).show();}
 void history(){ScrollView scroll=new ScrollView(this);LinearLayout list=column();scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));list.addView(text("Suas caminhadas",26,ink));JSONArray a=Store.read(this,"history.json").optJSONArray("walks");if(a==null||a.length()==0){list.addView(text("As caminhadas finalizadas aparecerão aqui.",16,muted));return;}for(int j=a.length()-1;j>=0;j--){final JSONObject w=a.optJSONObject(j);if(w==null)continue;String date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,new Locale("pt","BR")).format(new Date(w.optLong("date")));addButton(list,date+"\n"+Store.number(w.optDouble("meters")/1000)+" km · "+Store.time(w.optLong("duration")),()->{selected=w;page=1;follow=true;show();});}}
 void settings(){
  LinearLayout form=column();form.setPadding(dp(22),dp(8),dp(22),dp(8));
  form.addView(text("Aparência",19,ink));Switch mode=new Switch(this);mode.setText("Tema escuro");mode.setTextColor(ink);mode.setChecked(dark);mode.setMinHeight(dp(48));form.addView(mode);
  form.addView(text("Perfil de cores",15,ink));RadioGroup colors=new RadioGroup(this);int chosen=prefs.getInt("profile",1);for(int j=0;j<4;j++){RadioButton r=new RadioButton(this);r.setId(View.generateViewId());r.setTag(j);r.setText(PROFILES[j]);r.setTextColor((dark?DARK:LIGHT)[j]);r.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));r.setMinHeight(dp(44));colors.addView(r);if(j==chosen)colors.check(r.getId());}form.addView(colors);
  form.addView(text("Peso para as próximas caminhadas (kg)",15,ink));EditText weight=new EditText(this);weight.setTextColor(ink);weight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);weight.setText(Store.number(prefs.getFloat("weight",70)));form.addView(weight);
  form.addView(text("O peso da caminhada em andamento permanece o mesmo. Estimamos energia para caminhada em terreno plano por peso, distância e ritmo médio do tempo ativo. A equivalência em peso não é perda real medida.",12,muted));
  addButton(form,"Abrir aplicativo de música",()->{try{startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,Intent.CATEGORY_APP_MUSIC));}catch(ActivityNotFoundException e){toast("Nenhum aplicativo de música compatível encontrado.");}});
  form.addView(text("Use os controles do aplicativo de música ou da notificação do Android. O registro continua.\n\nO histórico fica no celular. O mapa online recebe seu IP e a área consultada. Compartilhar uma imagem revela o percurso.",12,muted));
  addButton(form,"Apagar histórico",()->new AlertDialog.Builder(this).setMessage("Apagar as caminhadas salvas? Esta ação não pode ser desfeita.").setPositiveButton("Apagar",(d,w)->{deleteFile("history.json");if(selected!=null){selected=null;navigate(2);}else if(page==2)show();toast("Histórico apagado");}).setNegativeButton("Cancelar",null).show());
  ScrollView sc=new ScrollView(this);sc.addView(form);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Configurações do step").setView(sc).setPositiveButton("Salvar",null).setNegativeButton("Cancelar",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{float kg=Float.parseFloat(weight.getText().toString().replace(',','.'));if(!Float.isFinite(kg)||kg<20||kg>400)throw new IllegalArgumentException();int profile=(Integer)colors.findViewById(colors.getCheckedRadioButtonId()).getTag();boolean changed=mode.isChecked()!=dark||profile!=chosen;prefs.edit().putFloat("weight",kg).putBoolean("dark",mode.isChecked()).putInt("profile",profile).apply();dialog.dismiss();if(changed)recreate();}catch(Exception e){weight.setError("Informe um peso entre 20 e 400 kg");}}));dialog.show();
 }
 void recover(){JSONObject w=Store.read(this,"active.json");if(w.has("date")&&!active())new AlertDialog.Builder(this).setTitle("Registro interrompido").setMessage("O Android encerrou o registro anterior. Você pode salvar os pontos já registrados; o intervalo sem GPS não será reconstruído.").setPositiveButton("Salvar no histórico",(d,k)->{JSONObject h=Store.read(this,"history.json");JSONArray a=h.optJSONArray("walks");if(a==null)a=new JSONArray();a.put(w);try{h.put("walks",a);}catch(JSONException ignored){}Store.write(this,"history.json",h);deleteFile("active.json");navigate(2);}).setNegativeButton("Descartar",(d,k)->deleteFile("active.json")).setCancelable(false).show();}
 void export(boolean gallery){
  if(map==null||selected==null)return;
  try{
   int width=map.getWidth(),height=map.getHeight();if(width<1||height<1){toast("Aguarde o mapa carregar");return;}
   Bitmap image=Bitmap.createBitmap(width,height+dp(216),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(surface);map.draw(canvas);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(ink);paint.setTextSize(dp(22));canvas.drawText("step · sua caminhada",dp(12),height+dp(32),paint);paint.setTextSize(dp(12));double kcal=selected.optDouble("calories");String[] lines={DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,new Locale("pt","BR")).format(new Date(selected.optLong("date"))),Store.number(selected.optDouble("meters")/1000)+" km · "+Store.time(selected.optLong("duration")),Math.round(kcal)+" kcal estimadas · "+Store.number(kcal/7.7)+" g equivalentes","Equivalência energética, não perda real medida.","© OpenStreetMap contributors"};for(int j=0;j<lines.length;j++)canvas.drawText(lines[j],dp(12),height+dp(62+j*27),paint);
   if(gallery){ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"step_"+System.currentTimeMillis()+".png");values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/step");values.put(MediaStore.Images.Media.IS_PENDING,1);Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException();try{try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null||!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException();}values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);getContentResolver().update(uri,values,null,null);}catch(Exception e){getContentResolver().delete(uri,null,null);throw e;}toast("Resumo salvo na galeria");}
   else{try(FileOutputStream out=new FileOutputStream(new File(getCacheDir(),"resumo.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}Uri uri=Uri.parse("content://br.com.passo.images/resumo.png");Intent share=new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("Resumo da caminhada",uri));startActivity(Intent.createChooser(share,"Compartilhar caminhada"));}
   image.recycle();
  }catch(Exception e){toast("Não foi possível gerar a imagem. Tente novamente.");}
 }
 void toast(String label){Toast.makeText(this,label,Toast.LENGTH_LONG).show();}
 @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){touchX=e.getX();touchY=e.getY();}if(e.getActionMasked()==MotionEvent.ACTION_UP&&e.getPointerCount()==1){float dx=e.getX()-touchX,dy=e.getY()-touchY;if(Math.abs(dx)>dp(100)&&Math.abs(dx)>Math.abs(dy)*2&&(page==0||(page==1&&e.getY()>getWindow().getDecorView().getHeight()-dp(220)))){navigate(dx<0?1:0);return true;}}return super.dispatchTouchEvent(e);}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("page",page);if(selected!=null)out.putLong("selected",selected.optLong("date"));}
 @Override public void onResume(){super.onResume();visible=true;handler.removeCallbacks(tick);handler.post(tick);if(map!=null)map.onResume();if(page==1&&selected==null)beginLocation(false,true);}
 @Override public void onPause(){visible=false;handler.removeCallbacks(tick);stopPreview();if(map!=null)map.onPause();super.onPause();}
 @Override public void onDestroy(){handler.removeCallbacks(tick);stopPreview();if(map!=null)map.onDetach();super.onDestroy();}
}
