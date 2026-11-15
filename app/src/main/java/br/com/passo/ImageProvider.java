package br.com.passo;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.ParcelFileDescriptor;import java.io.*;
public class ImageProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public String getType(Uri u){return "image/png";}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode)||!"/resumo.png".equals(u.getPath()))throw new FileNotFoundException();return ParcelFileDescriptor.open(new File(getContext().getCacheDir(),"resumo.png"),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){android.database.MatrixCursor c=new android.database.MatrixCursor(new String[]{"_display_name","_size"});File f=new File(getContext().getCacheDir(),"resumo.png");c.addRow(new Object[]{"caminhada.png",f.length()});return c;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
