package br.com.passo;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class Store {
 static JSONObject read(Context c,String name) {
  try(FileInputStream in=c.openFileInput(name);ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
   return new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
  }catch(Exception e){return new JSONObject();}
 }
 static void write(Context c,String name,JSONObject data) {
  android.util.AtomicFile file=new android.util.AtomicFile(new File(c.getFilesDir(),name));FileOutputStream out=null;
  try{out=file.startWrite();out.write(data.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
  catch(Exception e){if(out!=null)file.failWrite(out);throw new IllegalStateException("Não foi possível salvar os dados",e);}
 }
 static double calories(double kg,long ms,double meters){if(ms<=0||meters<=0)return 0;double hours=ms/3600000.0;return kg*0.5*((meters/1000.0)/hours)*hours;}
 static String time(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
 static String number(double n){return String.format(new Locale("pt","BR"),"%.2f",n);}
}
