package br.com.passo;
final class LocationPolicy {
 static boolean fresh(long nowNanos,long fixNanos){long age=nowNanos-fixNanos;return fixNanos>0&&age>=0&&age<=15000000000L;}
 static boolean ready(boolean precise,boolean gps,boolean hasAccuracy,float accuracy,long nowNanos,long fixNanos){return precise&&gps&&hasAccuracy&&Float.isFinite(accuracy)&&accuracy>=0&&accuracy<=25&&fresh(nowNanos,fixNanos);}
}
