package br.com.passo;
public class LocationPolicyChecks {
 static int count;
 static void check(boolean condition){count++;if(!condition)throw new AssertionError("Caso "+count);}
 public static void main(String[] args){
  long now=30000000000L,good=29000000000L;
  check(LocationPolicy.ready(true,true,true,8,now,good));
  check(!LocationPolicy.ready(false,true,true,8,now,good));
  check(!LocationPolicy.ready(true,false,true,8,now,good));
  check(!LocationPolicy.ready(true,true,false,8,now,good));
  check(!LocationPolicy.ready(true,true,true,26,now,good));
  check(!LocationPolicy.ready(true,true,true,Float.NaN,now,good));
  check(!LocationPolicy.ready(true,true,true,-1,now,good));
  check(!LocationPolicy.ready(true,true,true,8,now,now+1));
  check(!LocationPolicy.ready(true,true,true,8,now,0));
  check(!LocationPolicy.ready(true,true,true,8,now,now-15000000001L));
  check(LocationPolicy.ready(true,true,true,25,now,now-15000000000L));
  System.out.println(count+" verificações de localização passaram.");
 }
}
