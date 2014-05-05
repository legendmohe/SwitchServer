package hangzhou.kankun;

public class WifiJniC
{
  private static final String libSoName = "NDK_03";
  
  static
  {
    System.loadLibrary("NDK_03");
  }
  
  public native int add(int paramInt1, int paramInt2);
  
  public native String codeMethod(String paramString);
  
  public native String decode(byte[] paramArrayOfByte, int paramInt);
  
  public native byte[] encode(String paramString, int paramInt);
}